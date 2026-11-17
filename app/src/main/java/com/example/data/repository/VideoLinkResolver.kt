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
    val embedFallbackUrl: String = "",
  ) : VideoLinkResolution
}

/**
 * Resolves direct video URLs and social/short-link video pages (such as Instagram Reels and
 * Pinterest `pin.it/...`) into playable MP4 or HLS (`.m3u8`) streams for full-screen gift playback.
 */
object VideoLinkResolver {
  private const val TAG = "VideoLinkResolver"
  private const val CACHE_TTL_MS = 15 * 60 * 1000L
  private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

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
  private val instagramShortcodeRegex = Regex(
    """https?://(?:www\.|m\.)?instagram\.com/(?:reel|reels|p|tv)/([A-Za-z0-9_-]+)""",
    RegexOption.IGNORE_CASE,
  )
  private val instagramVideoUrlRegex = Regex(
    """"video_url"\s*:\s*"(https?://[^"]+)"""",
    RegexOption.IGNORE_CASE,
  )
  private val instagramVideoVersionsRegex = Regex(
    """"video_versions"\s*:\s*\[\s*\{[^}]*"url"\s*:\s*"(https?://[^"]+)"""",
    RegexOption.IGNORE_CASE,
  )
  private val instagramPlayableUrlRegex = Regex(
    """"(?:playable_url_quality_hd|playable_url|contentUrl)"\s*:\s*"(https?://[^"]+)"""",
    RegexOption.IGNORE_CASE,
  )
  private val instagramThumbRegex = Regex(
    """"(?:display_url|thumbnail_src)"\s*:\s*"(https?://[^"]+)"""",
    RegexOption.IGNORE_CASE,
  )
  private val cdnInstagramMp4Regex = Regex(
    """https://[^\s"'<>\\]*(?:cdninstagram\.com|fbcdn\.net)/[^\s"'<>\\]+\.mp4(?:\?[^\s"'<>\\]*)?""",
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

  fun instagramEmbedUrl(rawUrl: String): String? {
    val shortcode = instagramShortcodeRegex.find(rawUrl.trim())?.groupValues?.getOrNull(1)
      ?: return null
    return "https://www.instagram.com/reel/$shortcode/embed/"
  }

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

    val igShortcode = instagramShortcodeRegex.find(cleanUrl)?.groupValues?.getOrNull(1)
    val igEmbedUrl = igShortcode?.let { "https://www.instagram.com/reel/$it/embed/" }.orEmpty()

    val candidateRequests: List<Pair<String, Map<String, String>>> = buildList {
      if (igShortcode != null) {
        add(
          "https://www.instagram.com/reel/$igShortcode/embed/captioned/" to mapOf(
            "User-Agent" to BROWSER_USER_AGENT,
          )
        )
        add(
          "https://www.instagram.com/reel/$igShortcode/embed/" to mapOf(
            "User-Agent" to BROWSER_USER_AGENT,
          )
        )
        add(
          "https://www.instagram.com/reel/$igShortcode/?__a=1&__d=dis" to mapOf(
            "User-Agent" to BROWSER_USER_AGENT,
            "X-IG-App-ID" to "936619743392459",
          )
        )
        add(
          cleanUrl to mapOf(
            "User-Agent" to "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)",
          )
        )
      }
      add(cleanUrl to mapOf("User-Agent" to BROWSER_USER_AGENT))
    }

    var lastFailure: VideoLinkResolution.Failure? = null
    for ((targetUrl, extraHeaders) in candidateRequests) {
      val attempt = fetchAndExtract(targetUrl, igEmbedUrl, extraHeaders)
      if (attempt is VideoLinkResolution.Success) {
        resolvedCache[cleanUrl] = CachedResolution(attempt, now + CACHE_TTL_MS)
        return attempt
      }
      if (attempt is VideoLinkResolution.Failure) {
        if (lastFailure == null || (attempt.fallbackImageUrl.isNotBlank() && lastFailure.fallbackImageUrl.isBlank())) {
          lastFailure = attempt
        }
      }
    }

    return lastFailure ?: VideoLinkResolution.Failure(
      reasonHindi = "लिंक से वीडियो लोड नहीं हो सका।",
      embedFallbackUrl = igEmbedUrl,
    )
  }

  private fun fetchAndExtract(
    targetUrl: String,
    igEmbedUrl: String,
    extraHeaders: Map<String, String> = emptyMap(),
  ): VideoLinkResolution {
    return runCatching {
      val requestBuilder = Request.Builder()
        .url(targetUrl)
        .get()
        .header("User-Agent", extraHeaders["User-Agent"] ?: BROWSER_USER_AGENT)
        .header(
          "Accept",
          "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,video/mp4,video/*;q=0.8,*/*;q=0.7",
        )
        .header("Accept-Language", "en-US,en;q=0.9,hi;q=0.8")
      extraHeaders.forEach { (k, v) ->
        if (!k.equals("User-Agent", ignoreCase = true)) {
          requestBuilder.header(k, v)
        }
      }
      val request = requestBuilder.build()

      SupabaseNetworkClient.sharedOkHttpClient.newCall(request).execute().use { response ->
        val finalUrl = response.request.url.toString()
        val contentType = response.header("Content-Type").orEmpty().lowercase()

        if (!response.isSuccessful) {
          return@use VideoLinkResolution.Failure(
            reasonHindi = "वीडियो लिंक खोलने में सर्वर त्रुटि (HTTP ${response.code})।",
            embedFallbackUrl = igEmbedUrl,
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
          return@use VideoLinkResolution.Failure(
            reasonHindi = "इस लिंक से कोई वीडियो डेटा प्राप्त नहीं हुआ।",
            embedFallbackUrl = igEmbedUrl,
          )
        }

        val extracted = extractMediaFromHtml(htmlBody, finalUrl)
        if (extracted is VideoLinkResolution.Failure && igEmbedUrl.isNotBlank() && extracted.embedFallbackUrl.isBlank()) {
          extracted.copy(embedFallbackUrl = igEmbedUrl)
        } else {
          extracted
        }
      }
    }.getOrElse { error ->
      Log.w(TAG, "Failed to resolve video link: $targetUrl", error)
      VideoLinkResolution.Failure(
        reasonHindi = "लिंक से वीडियो लोड नहीं हो सका (${error.message ?: "नेटवर्क त्रुटि"})।",
        embedFallbackUrl = igEmbedUrl,
      )
    }
  }

  private fun unescapeHtmlAndJson(raw: String): String {
    return raw
      .replace("\\\\u0026", "&", ignoreCase = true)
      .replace("\\u0026", "&", ignoreCase = true)
      .replace("\\\\u002F", "/", ignoreCase = true)
      .replace("\\u002F", "/", ignoreCase = true)
      .replace("\\\\u003D", "=", ignoreCase = true)
      .replace("\\u003D", "=", ignoreCase = true)
      .replace("\\/", "/")
      .replace("&#x26;", "&", ignoreCase = true)
      .replace("&#38;", "&")
      .replace("&amp;", "&")
  }

  internal fun extractMediaFromHtml(html: String, resolvedPageUrl: String): VideoLinkResolution {
    val unescaped = unescapeHtmlAndJson(html)

    val thumbUrl = ogImageRegex1.find(unescaped)?.groupValues?.getOrNull(1)
      ?: ogImageRegex2.find(unescaped)?.groupValues?.getOrNull(1)
      ?: instagramThumbRegex.find(unescaped)?.groupValues?.getOrNull(1)
      ?: pinimgThumbRegex.find(unescaped)?.value
      ?: ""

    val igVideoUrl = instagramVideoUrlRegex.find(unescaped)?.groupValues?.getOrNull(1)
      ?: instagramVideoVersionsRegex.find(unescaped)?.groupValues?.getOrNull(1)
      ?: instagramPlayableUrlRegex.find(unescaped)?.groupValues?.getOrNull(1)
      ?: cdnInstagramMp4Regex.find(unescaped)?.value
    if (!igVideoUrl.isNullOrBlank()) {
      return VideoLinkResolution.Success(
        playableUrl = igVideoUrl,
        thumbnailUrl = thumbUrl,
        isHlsStream = hlsExtRegex.containsMatchIn(igVideoUrl),
      )
    }

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

    val isInstagramPage = resolvedPageUrl.contains("instagram.com", ignoreCase = true)
    val isPinterestPage = resolvedPageUrl.contains("pinterest.", ignoreCase = true) ||
      resolvedPageUrl.contains("pin.it", ignoreCase = true)
    val igEmbed = if (isInstagramPage) instagramEmbedUrl(resolvedPageUrl).orEmpty() else ""

    val reason = when {
      isInstagramPage ->
        "Instagram रील से सीधा MP4 स्ट्रीम नहीं मिला; एम्बेड प्लेयर से चलाया जा रहा है।"
      isPinterestPage ->
        "Pinterest लिंक पर सीधा MP4 वीडियो स्ट्रीम नहीं मिला (यह केवल फोटो/लॉगिन-प्रोटेक्टेड पिन हो सकता है)। कृपया सीधा .mp4 वीडियो लिंक उपयोग करें।"
      else ->
        "इस वेब लिंक में सीधा MP4 या HLS वीडियो स्ट्रीम नहीं मिला।"
    }
    return VideoLinkResolution.Failure(
      reasonHindi = reason,
      fallbackImageUrl = thumbUrl,
      embedFallbackUrl = igEmbed,
    )
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
        .header("User-Agent", BROWSER_USER_AGENT)
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
    val unescaped = unescapeHtmlAndJson(html)
    return ogImageRegex1.find(unescaped)?.groupValues?.getOrNull(1)
      ?: ogImageRegex2.find(unescaped)?.groupValues?.getOrNull(1)
      ?: instagramThumbRegex.find(unescaped)?.groupValues?.getOrNull(1)
      ?: pinimgThumbRegex.find(unescaped)?.value
  }
}
