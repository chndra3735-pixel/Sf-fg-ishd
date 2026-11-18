package com.example.data.repository

import android.util.Log
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.net.URLEncoder
import java.net.UnknownHostException
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

  val sharedOkHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .dns(dohFallbackDns)
      .connectTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(60, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build()
  }

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

  fun cacheRemoteMediaFile(cacheDir: java.io.File, remoteUrl: String): java.io.File? {
    return runCatching {
      val dir = java.io.File(cacheDir, "swar_media_cache").apply { mkdirs() }
      val rawExt = remoteUrl.substringBefore('?').substringBefore('#')
        .substringAfterLast('/', "")
        .substringAfterLast('.', "")
        .lowercase()
        .filter { it.isLetterOrDigit() }
        .take(5)
      val ext = if (rawExt in setOf("mp4", "m4v", "mov", "webm", "3gp", "mkv", "gif", "jpg", "jpeg", "png", "webp")) {
        rawExt
      } else {
        "mp4"
      }
      val key = remoteUrl.hashCode().toUInt().toString(16)
      val target = java.io.File(dir, "media_$key.$ext")
      if (target.exists() && target.length() > 0L) {
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
        val tmp = java.io.File(dir, "media_$key.tmp")
        body.byteStream().use { input ->
          tmp.outputStream().use { output ->
            input.copyTo(output)
          }
        }
        if (tmp.length() > 0L) {
          tmp.renameTo(target)
          target
        } else {
          tmp.delete()
          null
        }
      }
    }.getOrNull()
  }

  fun evictCachedMedia(cacheDir: java.io.File?, remoteUrl: String) {
    if (cacheDir == null || remoteUrl.isBlank()) return
    runCatching {
      val dir = java.io.File(cacheDir, "swar_media_cache")
      if (!dir.exists()) return@runCatching
      val key = remoteUrl.hashCode().toUInt().toString(16)
      dir.listFiles()?.forEach { file ->
        if (file.name.startsWith("media_$key.") || file.name == "media_$key.tmp") {
          file.delete()
        }
      }
    }
  }
}
