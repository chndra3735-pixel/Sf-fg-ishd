package com.example.data.repository

import android.util.Log
import okhttp3.Cache
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.net.URLEncoder
import java.net.UnknownHostException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Shared OkHttp client & DNS-over-HTTPS (DoH) fallback for Supabase Storage and Coil image loading.
 * Some mobile networks / ISPs fail system DNS lookups for `*.supabase.co`; falling back to
 * Cloudflare (1.1.1.1) and Google (8.8.8.8) DoH JSON endpoints resolves the host directly over HTTPS.
 */
object SupabaseNetworkClient {
  private const val TAG = "SupabaseNetwork"
  private const val CACHE_TTL_MS = 5 * 60 * 1000L

  private data class CachedEntry(
    val addresses: List<InetAddress>,
    val expiresAtMs: Long,
  )

  private val dohCache = ConcurrentHashMap<String, CachedEntry>()
  private val nxDomainHosts = ConcurrentHashMap<String, Boolean>()

  private val dohBootstrapClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(8, TimeUnit.SECONDS)
      .readTimeout(8, TimeUnit.SECONDS)
      .build()
  }

  val dohFallbackDns: Dns = object : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
      try {
        val systemResults = Dns.SYSTEM.lookup(hostname)
        if (systemResults.isNotEmpty()) {
          nxDomainHosts.remove(hostname)
          return systemResults
        }
      } catch (systemError: UnknownHostException) {
        val dohResults = resolveViaDoh(hostname)
        if (dohResults.isNotEmpty()) {
          return dohResults
        }
        throw systemError
      }
      val dohResults = resolveViaDoh(hostname)
      if (dohResults.isNotEmpty()) {
        return dohResults
      }
      throw UnknownHostException("Unable to resolve host \"$hostname\"")
    }
  }

  private const val HTTP_DISK_CACHE_BYTES = 120L * 1024L * 1024L

  @Volatile
  private var cachedHttpClient: OkHttpClient? = null

  private val mediaCacheControlInterceptor = Interceptor { chain ->
    val request = chain.request()
    val response = chain.proceed(request)
    val url = request.url.toString()
    val isCacheableGet = request.method.equals("GET", ignoreCase = true) &&
      response.isSuccessful &&
      (url.contains("/storage/v1/object/public/", ignoreCase = true) ||
        url.endsWith(".gif", ignoreCase = true) ||
        url.endsWith(".png", ignoreCase = true) ||
        url.endsWith(".jpg", ignoreCase = true) ||
        url.endsWith(".jpeg", ignoreCase = true) ||
        url.endsWith(".webp", ignoreCase = true) ||
        url.endsWith(".mp4", ignoreCase = true))
    if (isCacheableGet && response.header("Cache-Control").isNullOrBlank()) {
      response.newBuilder()
        .removeHeader("Pragma")
        .header("Cache-Control", "public, max-age=604800")
        .build()
    } else {
      response
    }
  }

  private val fallbackClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .dns(dohFallbackDns)
      .connectTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(60, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build()
  }

  fun initialize(cacheDir: File) {
    if (cachedHttpClient != null) return
    synchronized(this) {
      if (cachedHttpClient != null) return
      val httpCacheDir = File(cacheDir, LocalMediaCacheManager.HTTP_CACHE_DIR_NAME).apply { mkdirs() }
      cachedHttpClient = OkHttpClient.Builder()
        .cache(Cache(httpCacheDir, HTTP_DISK_CACHE_BYTES))
        .dns(dohFallbackDns)
        .addNetworkInterceptor(mediaCacheControlInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    }
  }

  val sharedOkHttpClient: OkHttpClient
    get() = cachedHttpClient ?: fallbackClient

  fun wasHostNxDomain(hostname: String): Boolean = nxDomainHosts[hostname] == true

  private fun resolveViaDoh(hostname: String): List<InetAddress> {
    val now = System.currentTimeMillis()
    val cached = dohCache[hostname]
    if (cached != null && cached.expiresAtMs > now && cached.addresses.isNotEmpty()) {
      return cached.addresses
    }

    val encoded = URLEncoder.encode(hostname, "UTF-8")
    val endpoints = listOf(
      "https://1.1.1.1/dns-query?name=$encoded&type=A",
      "https://8.8.8.8/resolve?name=$encoded&type=A",
    )

    var sawNxDomain = false
    for (endpoint in endpoints) {
      try {
        val request = Request.Builder()
          .url(endpoint)
          .get()
          .addHeader("Accept", "application/dns-json")
          .build()
        dohBootstrapClient.newCall(request).execute().use { response ->
          if (!response.isSuccessful) return@use
          val body = response.body?.string().orEmpty()
          if (body.isBlank()) return@use
          val json = JSONObject(body)
          val status = json.optInt("Status", -1)
          if (status == 3) {
            sawNxDomain = true
            return@use
          }
          val answers = json.optJSONArray("Answer") ?: return@use
          val addresses = mutableListOf<InetAddress>()
          for (i in 0 until answers.length()) {
            val item = answers.optJSONObject(i) ?: continue
            val recordType = item.optInt("type", -1)
            if (recordType == 1 || recordType == 28) {
              val ip = item.optString("data", "").trim()
              if (ip.isNotEmpty()) {
                runCatching { InetAddress.getByName(ip) }.getOrNull()?.let { addresses.add(it) }
              }
            }
          }
          if (addresses.isNotEmpty()) {
            nxDomainHosts.remove(hostname)
            dohCache[hostname] = CachedEntry(addresses, now + CACHE_TTL_MS)
            Log.i(TAG, "Resolved $hostname via DoH ($endpoint)")
            return addresses
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "DoH lookup failed via $endpoint for $hostname: ${e.message}")
      }
    }

    if (sawNxDomain) {
      nxDomainHosts[hostname] = true
    }
    return emptyList()
  }

  private fun sha256ShortKey(url: String): String {
    return runCatching {
      val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
      digest.take(12).joinToString("") { "%02x".format(it) }
    }.getOrElse {
      url.hashCode().toUInt().toString(16)
    }
  }

  private fun resolveMediaStorageDir(baseDir: File): File {
    return if (baseDir.name == "files") {
      File(baseDir, LocalMediaCacheManager.PERSISTENT_MEDIA_DIR_NAME).apply { mkdirs() }
    } else {
      File(baseDir, LocalMediaCacheManager.TEMP_MEDIA_DIR_NAME).apply { mkdirs() }
    }
  }

  private fun extractSafeExtension(remoteUrl: String): String {
    val rawExt = remoteUrl.substringBefore('?').substringBefore('#')
      .substringAfterLast('/', "")
      .substringAfterLast('.', "")
      .lowercase()
      .filter { it.isLetterOrDigit() }
      .take(5)
    return if (rawExt in setOf("mp4", "m4v", "mov", "webm", "3gp", "mkv", "gif", "jpg", "jpeg", "png", "webp")) {
      rawExt
    } else {
      "mp4"
    }
  }

  fun findCachedMediaFile(baseDir: File?, remoteUrl: String): File? {
    if (baseDir == null || remoteUrl.isBlank()) return null
    val ext = extractSafeExtension(remoteUrl)
    val shaKey = sha256ShortKey(remoteUrl)
    val legacyKey = remoteUrl.hashCode().toUInt().toString(16)
    val candidates = listOf(
      File(baseDir, "${LocalMediaCacheManager.PERSISTENT_MEDIA_DIR_NAME}/media_$shaKey.$ext"),
      File(baseDir, "${LocalMediaCacheManager.TEMP_MEDIA_DIR_NAME}/media_$shaKey.$ext"),
      File(baseDir, "${LocalMediaCacheManager.PERSISTENT_MEDIA_DIR_NAME}/media_$legacyKey.$ext"),
      File(baseDir, "${LocalMediaCacheManager.TEMP_MEDIA_DIR_NAME}/media_$legacyKey.$ext"),
    )
    return candidates.firstOrNull { it.exists() && it.length() > 0L }?.also {
      runCatching { it.setLastModified(System.currentTimeMillis()) }
    }
  }

  fun cacheRemoteMediaFile(
    cacheDir: File,
    remoteUrl: String,
    maxCacheBytes: Long = LocalMediaCacheManager.DEFAULT_MAX_MEDIA_CACHE_BYTES,
  ): File? {
    return runCatching {
      findCachedMediaFile(cacheDir, remoteUrl)?.let { existing ->
        return@runCatching existing
      }
      val dir = resolveMediaStorageDir(cacheDir)
      val ext = extractSafeExtension(remoteUrl)
      val key = sha256ShortKey(remoteUrl)
      val target = File(dir, "media_$key.$ext")
      if (target.exists() && target.length() > 0L) {
        runCatching { target.setLastModified(System.currentTimeMillis()) }
        return@runCatching target
      }
      val requestBuilder = Request.Builder()
        .url(remoteUrl)
        .header(
          "User-Agent",
          "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
        )
        .get()
      if (remoteUrl.contains("cdninstagram.com", ignoreCase = true) ||
        remoteUrl.contains("fbcdn.net", ignoreCase = true)
      ) {
        requestBuilder.header("Referer", "https://www.instagram.com/")
      } else if (remoteUrl.contains("pinimg.com", ignoreCase = true)) {
        requestBuilder.header("Referer", "https://www.pinterest.com/")
      }
      val request = requestBuilder.build()
      sharedOkHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@runCatching null
        val contentType = response.header("Content-Type").orEmpty().lowercase()
        if (contentType.startsWith("text/") || contentType.contains("html") || contentType.contains("mpegurl")) {
          return@runCatching null
        }
        val body = response.body ?: return@runCatching null
        val tmp = File(dir, "media_$key.tmp")
        var moved = false
        try {
          body.byteStream().use { input ->
            tmp.outputStream().use { output ->
              input.copyTo(output)
            }
          }
          if (tmp.length() > 0L && tmp.renameTo(target)) {
            moved = true
            LocalMediaCacheManager.enforceLruLimit(dir, maxCacheBytes)
            target
          } else {
            null
          }
        } finally {
          if (!moved && tmp.exists()) {
            tmp.delete()
          }
        }
      }
    }.getOrNull()
  }

  fun evictCachedMedia(cacheDir: File?, remoteUrl: String) {
    if (cacheDir == null || remoteUrl.isBlank()) return
    runCatching {
      val shaKey = sha256ShortKey(remoteUrl)
      val legacyKey = remoteUrl.hashCode().toUInt().toString(16)
      listOf(
        File(cacheDir, LocalMediaCacheManager.PERSISTENT_MEDIA_DIR_NAME),
        File(cacheDir, LocalMediaCacheManager.TEMP_MEDIA_DIR_NAME),
      ).forEach { dir ->
        if (dir.exists()) {
          dir.listFiles()?.forEach { file ->
            if (file.name.startsWith("media_$shaKey.") ||
              file.name == "media_$shaKey.tmp" ||
              file.name.startsWith("media_$legacyKey.") ||
              file.name == "media_$legacyKey.tmp"
            ) {
              file.delete()
            }
          }
        }
      }
    }
  }
}
