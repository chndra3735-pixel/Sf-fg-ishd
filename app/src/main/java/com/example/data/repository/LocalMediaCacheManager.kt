package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.LevelTierConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class LocalCacheStats(
  val mediaBytes: Long = 0L,
  val imageCacheBytes: Long = 0L,
  val httpCacheBytes: Long = 0L,
  val cachedMediaFiles: Int = 0,
) {
  val totalBytes: Long
    get() = (mediaBytes + imageCacheBytes + httpCacheBytes).coerceAtLeast(0L)

  val formattedSize: String
    get() = formatBytesReadable(totalBytes)

  companion object {
    fun formatBytesReadable(bytes: Long): String {
      val b = bytes.coerceAtLeast(0L)
      return when {
        b < 1024L -> "$b B"
        b < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", b / 1024.0)
        b < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", b / (1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.2f GB", b / (1024.0 * 1024.0 * 1024.0))
      }
    }
  }
}

/**
 * Manages app-scoped persistent phone storage caching for videos, frames, typing banners,
 * level badges, and voice server bandwidth saver preferences — saving server egress without
 * requesting broad storage permissions.
 */
object LocalMediaCacheManager {
  private const val TAG = "SwarLocalCache"
  private const val PREFS_NAME = "swarroom_local_media_prefs"
  private const val KEY_ENTRY_VIDEO_MUTED = "entry_video_muted"
  private const val KEY_SERVER_SAVER_ENABLED = "server_saver_enabled"

  const val PERSISTENT_MEDIA_DIR_NAME = "swar_local_media"
  const val TEMP_MEDIA_DIR_NAME = "swar_media_cache"
  const val IMAGE_CACHE_DIR_NAME = "swar_image_cache"
  const val HTTP_CACHE_DIR_NAME = "swar_http_cache"

  const val DEFAULT_MAX_MEDIA_CACHE_BYTES = 350L * 1024L * 1024L // 350 MB local phone budget

  private fun prefs(context: Context) =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun isEntryVideoMuted(context: Context): Boolean {
    return prefs(context).getBoolean(KEY_ENTRY_VIDEO_MUTED, false)
  }

  fun setEntryVideoMuted(context: Context, muted: Boolean) {
    prefs(context).edit().putBoolean(KEY_ENTRY_VIDEO_MUTED, muted).apply()
  }

  fun isServerSaverEnabled(context: Context): Boolean {
    return prefs(context).getBoolean(KEY_SERVER_SAVER_ENABLED, true)
  }

  fun setServerSaverEnabled(context: Context, enabled: Boolean) {
    prefs(context).edit().putBoolean(KEY_SERVER_SAVER_ENABLED, enabled).apply()
  }

  /**
   * When Server Saver mode is ON, uses app-scoped persistent `filesDir` so downloaded entry
   * videos, frames, and gift clips stay saved on phone storage across system cache trims.
   */
  fun preferredMediaBaseDir(context: Context): File {
    val appCtx = context.applicationContext
    return if (isServerSaverEnabled(appCtx)) {
      appCtx.filesDir ?: appCtx.cacheDir
    } else {
      appCtx.cacheDir
    }
  }

  fun persistentMediaFolder(baseDir: File): File {
    return File(baseDir, PERSISTENT_MEDIA_DIR_NAME).apply { mkdirs() }
  }

  fun calculateCacheStats(context: Context): LocalCacheStats {
    val appCtx = context.applicationContext
    val persistentDir = File(appCtx.filesDir, PERSISTENT_MEDIA_DIR_NAME)
    val tempMediaDir = File(appCtx.cacheDir, TEMP_MEDIA_DIR_NAME)
    val imageCacheDir = File(appCtx.cacheDir, IMAGE_CACHE_DIR_NAME)
    val httpCacheDir = File(appCtx.cacheDir, HTTP_CACHE_DIR_NAME)

    val (pBytes, pCount) = dirSizeAndCount(persistentDir)
    val (tBytes, tCount) = dirSizeAndCount(tempMediaDir)
    val (iBytes, _) = dirSizeAndCount(imageCacheDir)
    val (hBytes, _) = dirSizeAndCount(httpCacheDir)

    return LocalCacheStats(
      mediaBytes = pBytes + tBytes,
      imageCacheBytes = iBytes,
      httpCacheBytes = hBytes,
      cachedMediaFiles = pCount + tCount,
    )
  }

  fun clearAllLocalCaches(context: Context): Long {
    val appCtx = context.applicationContext
    val before = calculateCacheStats(appCtx).totalBytes
    listOf(
      File(appCtx.filesDir, PERSISTENT_MEDIA_DIR_NAME),
      File(appCtx.cacheDir, TEMP_MEDIA_DIR_NAME),
      File(appCtx.cacheDir, IMAGE_CACHE_DIR_NAME),
      File(appCtx.cacheDir, HTTP_CACHE_DIR_NAME),
    ).forEach { dir ->
      runCatching {
        if (dir.exists()) {
          dir.deleteRecursively()
        }
      }
    }
    return before
  }

  /**
   * Enforces an LRU cap on a media cache directory so phone memory stays fast and bounded.
   */
  fun enforceLruLimit(dir: File, maxBytes: Long = DEFAULT_MAX_MEDIA_CACHE_BYTES) {
    runCatching {
      if (!dir.exists() || maxBytes <= 0L) return
      val files = dir.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") } ?: return
      var total = files.sumOf { it.length() }
      if (total <= maxBytes) return
      val oldestFirst = files.sortedBy { it.lastModified() }
      for (f in oldestFirst) {
        if (total <= maxBytes) break
        val len = f.length()
        if (f.delete()) {
          total -= len
        }
      }
    }
  }

  /**
   * Prefetches non-empty static/animated cosmetic assets (frames, typing banners, badges,
   * and direct MP4 entry videos) onto local device storage once so subsequent opens use 0 server egress.
   */
  suspend fun prefetchLevelConfigMedia(
    context: Context,
    configs: Map<Int, LevelTierConfig>,
    maxAssetsPerPass: Int = 18,
  ): Int = withContext(Dispatchers.IO) {
    if (!isServerSaverEnabled(context) || configs.isEmpty()) return@withContext 0
    val baseDir = preferredMediaBaseDir(context)
    val urls = LinkedHashSet<String>()
    configs.values.forEach { cfg ->
      listOf(
        cfg.avatarFrameUrl,
        cfg.typingBannerUrl,
        cfg.profileBadgeUrl,
      ).forEach { raw ->
        val clean = raw.trim()
        if (clean.startsWith("http", ignoreCase = true)) {
          urls.add(clean)
        }
      }
      val entryUrl = cfg.entryVideoUrl.trim()
      if (entryUrl.startsWith("http", ignoreCase = true) &&
        (entryUrl.contains(".mp4", ignoreCase = true) || entryUrl.contains("supabase", ignoreCase = true))
      ) {
        urls.add(entryUrl)
      }
    }

    var cachedCount = 0
    for (url in urls.take(maxAssetsPerPass)) {
      val file = runCatching {
        SupabaseNetworkClient.cacheRemoteMediaFile(baseDir, url)
      }.getOrNull()
      if (file != null && file.exists()) {
        cachedCount++
      }
    }
    if (cachedCount > 0) {
      Log.d(TAG, "Prefetched/verified $cachedCount level cosmetic media files on phone storage")
    }
    cachedCount
  }

  private fun dirSizeAndCount(dir: File): Pair<Long, Int> {
    if (!dir.exists()) return 0L to 0
    var bytes = 0L
    var count = 0
    dir.walkTopDown().forEach { f ->
      if (f.isFile) {
        bytes += f.length()
        count++
      }
    }
    return bytes to count
  }
}
