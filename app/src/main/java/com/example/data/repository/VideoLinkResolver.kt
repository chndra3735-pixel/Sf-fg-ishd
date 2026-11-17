package com.example.data.repository

import android.util.Log
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap

sealed interface VideoLinkResolution {
  data class Success(
    val playableUrl: String,
    val thumbnailUrl: String = "",
    val isHlsStream: Boolean = false,
  ) : VideoLinkResolution

  data class Failure(
    val reasonHindi: String,
    val fallbackImageUrl: String = "",
  ) : VideoLinkResolution
}

/**
 * Resolves direct video URLs and social/short-link video pages (such as Pinterest `pin.it/...`)
 * into playable MP4 or HLS (`.m3u8`) streams for full-screen gift playback.
 */
object VideoLinkResolver {
  private const val TAG = "VideoLinkResolver"
  private const val CACHE_TTL_MS = 15 * 60 * 1000L

  private data class CachedResolution(
    val result: VideoLinkResolution,
    val expiresAtMs: Long,
  )

  private val resolvedCache = ConcurrentHashMap<String, CachedResolution>()

  private val directVideoExtRegex = Regex(
    """\.(mp4|m4v|mov|webm|3gp|mkv)(\?.*)?$""",
    RegexOption.IGNORE_CASE,
  )
  private val hlsExtRegex = Regex(
    """\.m3u8(\?.*)?$""",
    RegexOption.IGNORE_CASE,
  )
  private val pinimgMp4Regex = Regex(
    """https://v[0-9]*\.pinimg\.com/videos/[^\s"'<>\\]+\.mp4(?:\?[^\s"'<>\\]*)?""",
    RegexOption.IGNORE_CASE,
  )
  private val pinimgM3u8Regex = Regex(
    """https://v[0-9]*\.pinimg\.com/videos/[^\s"'<>\\]+\.m3u8(?:\?[^\s"'<>\\]*)?""",
    RegexOption.IGNORE_CASE,
  )
  private val genericMp4Regex = Regex(
    """https?://[^\s"'<>\\]+\.mp4(?:\?[^\s"'<>\\]*)?""",
    RegexOption.IGNORE_CASE,
  )
  private val genericM3u8Regex = Regex(
    """https?://[^\s"'<>\\]+\.m3u8(?:\?[^\s"'<>\\]*)?""",
    RegexOption.IGNORE_CASE,
  )
  private val ogVideoRegex1 = Regex(
    """<meta[^>]+property=["']og:video(?::secure_url|:url)?["'][^>]+content=["']([^"']+)["']""",
    setOf(RegexOption.IGNORE_CASE),
  )
  private val ogVideoRegex2 = Regex(
    """<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:video(?::secure_url|:url)?["']""",
    setOf(RegexOption.IGNORE_CASE),
  )
  private val videoSrcRegex = Regex(
    """<(?:video|source)[^>]+src=["']([^"']+)["']""",
    setOf(RegexOption.IGNORE_CASE),
  )
  private val ogImageRegex1 = Regex(
    """<meta[^>]+property=["']og:image(?::secure_url)?["'][^>]+content=["']([^"']+)["']""",
    setOf(RegexOption.IGNORE_CASE),
  )
  private val ogImageRegex2 = Regex(
    """<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:image(?::secure_url)?["']""",
    setOf(RegexOption.IGNORE_CASE),
  )
  private val pinimgThumbRegex = Regex(
    """https://i\.pinimg\.com/(?:originals|736x|564x)/[^\s"'<>\\]+\.(?:jpg|jpeg|png|webp)""",
    RegexOption.IGNORE_CASE,
  )

  fun resolvePlayableVideo(rawUrl: String): VideoLinkResolution {
    val cleanUrl = rawUrl.trim()
    if (cleanUrl.isBlank()) {
      return VideoLinkResolution.Failure("वीडियो लिंक खाली है।")
    }
    if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
      !cleanUrl.startsWith("https://", ignoreCase = true)
    ) {
      return VideoLinkResolution.Success(playableUrl = cleanUrl)
    }

    if (directVideoExtRegex.containsMatchIn(cleanUrl)) {
      return VideoLinkResolution.Success(playableUrl = cleanUrl)
    }
    if (hlsExtRegex.containsMatchIn(cleanUrl)) {
      return VideoLinkResolution.Success(playableUrl = cleanUrl, isHlsStream = true)
    }

    val now = System.currentTimeMillis()
    resolvedCache[cleanUrl]?.let { cached ->
      if (cached.expiresAtMs > now && cached.result is VideoLinkResolution.Success) {
        return cached.result
      }
    }

    val result = runCatching {
      val request = Request.Builder()
        .url(cleanUrl)
        .get()
        .header(
          "User-Agent",
          "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
        )
        .header(
          "Accept",
          "text/html,application/xhtml+xml,application/xml;q=0.9,video/mp4,video/*;q=0.8,*/*;q=0.7",
        )
        .header("Accept-Language", "en-US,en;q=0.9,hi;q=0.8")
        .build()

      SupabaseNetworkClient.sharedOkHttpClient.newCall(request).execute().use { response ->
        val finalUrl = response.request.url.toString()
        val contentType = response.header("Content-Type").orEmpty().lowercase()

        if (!response.isSuccessful) {
          return@use VideoLinkResolution.Failure(
            "वीडियो लिंक खोलने में सर्वर त्रुटि (HTTP ${response.code})।"
          )
        }

        if (contentType.startsWith("video/") || directVideoExtRegex.containsMatchIn(finalUrl)) {
          return@use VideoLinkResolution.Success(playableUrl = finalUrl)
        }
        if (contentType.contains("mpegurl") || hlsExtRegex.containsMatchIn(finalUrl)) {
          return@use VideoLinkResolution.Success(playableUrl = finalUrl, isHlsStream = true)
        }

        val htmlBody = response.body?.string().orEmpty()
        if (htmlBody.isBlank()) {
          return@use VideoLinkResolution.Failure("इस लिंक से कोई वीडियो डेटा प्राप्त नहीं हुआ।")
        }

        extractMediaFromHtml(htmlBody, finalUrl)
      }
    }.getOrElse { error ->
      Log.w(TAG, "Failed to resolve video link: $cleanUrl", error)
      VideoLinkResolution.Failure(
        "लिंक से वीडियो लोड नहीं हो सका (${error.message ?: "नेटवर्क त्रुटि"})।"
      )
    }

    if (result is VideoLinkResolution.Success) {
      resolvedCache[cleanUrl] = CachedResolution(result, now + CACHE_TTL_MS)
    }
    return result
  }

  internal fun extractMediaFromHtml(html: String, resolvedPageUrl: String): VideoLinkResolution {
    val unescaped = html
      .replace("\\u002F", "/", ignoreCase = true)
      .replace("\\/", "/")
      .replace("&amp;", "&")

    val thumbUrl = ogImageRegex1.find(unescaped)?.groupValues?.getOrNull(1)
      ?: ogImageRegex2.find(unescaped)?.groupValues?.getOrNull(1)
      ?: pinimgThumbRegex.find(unescaped)?.value
      ?: ""

    val pinMp4Matches = pinimgMp4Regex.findAll(unescaped)
      .map { it.value }
      .distinct()
      .toList()
    if (pinMp4Matches.isNotEmpty()) {
      val preferred = pinMp4Matches.sortedByDescending { url ->
        when {
          url.contains("720p", ignoreCase = true) -> 4
          url.contains("480p", ignoreCase = true) -> 3
          url.contains("360p", ignoreCase = true) -> 2
          else -> 1
        }
      }.first()
      return VideoLinkResolution.Success(
        playableUrl = preferred,
        thumbnailUrl = thumbUrl,
        isHlsStream = false,
      )
    }

    val ogVideo = ogVideoRegex1.find(unescaped)?.groupValues?.getOrNull(1)
      ?: ogVideoRegex2.find(unescaped)?.groupValues?.getOrNull(1)
    if (!ogVideo.isNullOrBlank() && (ogVideo.startsWith("http://") || ogVideo.startsWith("https://"))) {
      return VideoLinkResolution.Success(
        playableUrl = ogVideo,
        thumbnailUrl = thumbUrl,
        isHlsStream = hlsExtRegex.containsMatchIn(ogVideo),
      )
    }

    val videoTagSrc = videoSrcRegex.findAll(unescaped)
      .mapNotNull { it.groupValues.getOrNull(1) }
      .firstOrNull { src ->
        (src.startsWith("http://") || src.startsWith("https://")) && !src.startsWith("blob:")
      }
    if (!videoTagSrc.isNullOrBlank()) {
      return VideoLinkResolution.Success(
        playableUrl = videoTagSrc,
        thumbnailUrl = thumbUrl,
        isHlsStream = hlsExtRegex.containsMatchIn(videoTagSrc),
      )
    }

    val genericMp4 = genericMp4Regex.findAll(unescaped)
      .map { it.value }
      .firstOrNull()
    if (!genericMp4.isNullOrBlank()) {
      return VideoLinkResolution.Success(
        playableUrl = genericMp4,
        thumbnailUrl = thumbUrl,
        isHlsStream = false,
      )
    }

    val hlsUrl = pinimgM3u8Regex.find(unescaped)?.value
      ?: genericM3u8Regex.find(unescaped)?.value
    if (!hlsUrl.isNullOrBlank()) {
      return VideoLinkResolution.Success(
        playableUrl = hlsUrl,
        thumbnailUrl = thumbUrl,
        isHlsStream = true,
      )
    }

    val isPinterestPage = resolvedPageUrl.contains("pinterest.", ignoreCase = true) ||
      resolvedPageUrl.contains("pin.it", ignoreCase = true)
    val reason = if (isPinterestPage) {
      "Pinterest लिंक पर सीधा MP4 वीडियो स्ट्रीम नहीं मिला (यह केवल फोटो/लॉगिन-प्रोटेक्टेड पिन हो सकता है)। कृपया सीधा .mp4 वीडियो लिंक या गैलरी से वीडियो चुनें।"
    } else {
      "इस वेब लिंक में सीधा MP4 या HLS वीडियो स्ट्रीम नहीं मिला।"
    }
    return VideoLinkResolution.Failure(reasonHindi = reason, fallbackImageUrl = thumbUrl)
  }

  private val directImageExtRegex = Regex(
    """\.(jpg|jpeg|png|webp|gif|bmp)(\?.*)?$""",
    RegexOption.IGNORE_CASE,
  )

  fun resolveImageUrl(rawUrl: String): Result<String> {
    val cleanUrl = rawUrl.trim()
    if (cleanUrl.isBlank()) {
      return Result.failure(IllegalArgumentException("DP लिंक खाली है।"))
    }
    if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
      !cleanUrl.startsWith("https://", ignoreCase = true)
    ) {
      return Result.failure(IllegalArgumentException("कृपया https:// या http:// से शुरू होने वाला मान्य DP लिंक दर्ज करें।"))
    }
    if (directImageExtRegex.containsMatchIn(cleanUrl)) {
      return Result.success(cleanUrl)
    }

    return runCatching {
      val request = Request.Builder()
        .url(cleanUrl)
        .get()
        .header(
          "User-Agent",
          "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
        )
        .header("Accept", "text/html,application/xhtml+xml,image/*,*/*;q=0.8")
        .header("Accept-Language", "en-US,en;q=0.9,hi;q=0.8")
        .build()

      SupabaseNetworkClient.sharedOkHttpClient.newCall(request).execute().use { response ->
        val finalUrl = response.request.url.toString()
        val contentType = response.header("Content-Type").orEmpty().lowercase()
        if (!response.isSuccessful) {
          throw IllegalStateException("DP लिंक लोड नहीं हुआ (HTTP ${response.code})।")
        }
        if (contentType.startsWith("image/") || directImageExtRegex.containsMatchIn(finalUrl)) {
          return@use finalUrl
        }
        val htmlBody = response.body?.string().orEmpty()
        extractImageFromHtml(htmlBody)
          ?: throw IllegalStateException("इस लिंक में DP फोटो नहीं मिली — सीधा फोटो लिंक (.jpg/.png) या गैलरी फोटो चुनें।")
      }
    }
  }

  internal fun extractImageFromHtml(html: String): String? {
    if (html.isBlank()) return null
    val unescaped = html
      .replace("\\u002F", "/", ignoreCase = true)
      .replace("\\/", "/")
      .replace("&amp;", "&")
    return ogImageRegex1.find(unescaped)?.groupValues?.getOrNull(1)
      ?: ogImageRegex2.find(unescaped)?.groupValues?.getOrNull(1)
      ?: pinimgThumbRegex.find(unescaped)?.value
  }
}
