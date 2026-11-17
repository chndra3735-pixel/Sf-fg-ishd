package com.example

import com.example.data.model.SwarCatalogDefaults
import com.example.data.model.SwarUserIdentity
import com.example.data.repository.VideoLinkResolution
import com.example.data.repository.VideoLinkResolver
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for core SwarRoom utilities.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun numericIdForUid_isAlwaysTenDigitsAndUniquePerUser() {
    val ids = (1..200).map { i -> SwarUserIdentity.numericIdForUid("uid_user_$i") }
    assertTrue(ids.all { it.length == 10 && it.all(Char::isDigit) && it.first() != '0' })
    assertEquals(ids.size, ids.toSet().size)
    assertEquals(
      SwarUserIdentity.numericIdForUid("uid_alpha"),
      SwarUserIdentity.numericIdForUid("uid_alpha"),
    )
  }

  @Test
  fun levelForCoins_calculatesLevelAndProgress() {
    val lvl1 = SwarUserIdentity.levelForCoins(500L)
    assertEquals(1, lvl1.level)
    assertEquals(0.5f, lvl1.progress, 0.01f)
    assertEquals(500L, lvl1.nextLevelRemainingCoins)

    val lvl15 = SwarUserIdentity.levelForCoins(14_250L)
    assertEquals(15, lvl15.level)
    assertEquals("गोल्डन वॉइस", lvl15.title)
  }

  @Test
  fun builtInGifts_includesPinterestVideoGiftLink() {
    val pinGift = SwarCatalogDefaults.builtInGifts.firstOrNull {
      it.mediaUrl == "https://pin.it/5wrohsACd"
    }
    assertNotNull(pinGift)
    assertEquals("video", pinGift?.mediaType)
  }

  @Test
  fun videoLinkResolver_extractsPinterestMp4AndThumbnailFromEscapedHtml() {
    val sampleHtml = """
      <html>
        <head>
          <meta property="og:image" content="https://i.pinimg.com/736x/ab/cd/ef/thumb.jpg" />
        </head>
        <body>
          <script>
            {"video_list":{"V_360P":{"url":"https:\/\/v1.pinimg.com\/videos\/mc\/360p\/sample.mp4"},"V_720P":{"url":"https:\/\/v1.pinimg.com\/videos\/mc\/720p\/sample.mp4"}}}
          </script>
        </body>
      </html>
    """.trimIndent()

    val res = VideoLinkResolver.extractMediaFromHtml(sampleHtml, "https://www.pinterest.com/pin/12345/")
    assertTrue(res is VideoLinkResolution.Success)
    val ok = res as VideoLinkResolution.Success
    assertEquals("https://v1.pinimg.com/videos/mc/720p/sample.mp4", ok.playableUrl)
    assertEquals("https://i.pinimg.com/736x/ab/cd/ef/thumb.jpg", ok.thumbnailUrl)
    assertFalse(ok.isHlsStream)
  }

  @Test
  fun videoLinkResolver_handlesHlsFallbackAndImageOnlyFailure() {
    val hlsHtml = """
      <html><script>{"hls":{"url":"https://v1.pinimg.com/videos/iht/hls/sample.m3u8"}}</script></html>
    """.trimIndent()
    val hlsRes = VideoLinkResolver.extractMediaFromHtml(hlsHtml, "https://www.pinterest.com/pin/999/")
    assertTrue(hlsRes is VideoLinkResolution.Success)
    assertTrue((hlsRes as VideoLinkResolution.Success).isHlsStream)

    val imageOnlyHtml = """
      <html><head><meta property="og:image" content="https://i.pinimg.com/originals/img.jpg" /></head></html>
    """.trimIndent()
    val failRes = VideoLinkResolver.extractMediaFromHtml(imageOnlyHtml, "https://www.pinterest.com/pin/888/")
    assertTrue(failRes is VideoLinkResolution.Failure)
    assertEquals("https://i.pinimg.com/originals/img.jpg", (failRes as VideoLinkResolution.Failure).fallbackImageUrl)
  }

  @Test
  fun videoLinkResolver_extractsDpImageUrlFromHtml() {
    val html = """
      <html><head><meta content="https:\/\/i.pinimg.com\/736x\/12\/34\/56\/dp.jpg" property="og:image" /></head></html>
    """.trimIndent()
    val dpUrl = VideoLinkResolver.extractImageFromHtml(html)
    assertEquals("https://i.pinimg.com/736x/12/34/56/dp.jpg", dpUrl)
  }
}
