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

  @Test
  fun builtInGifts_includesInstagramReelVideoGiftLink() {
    val igGift = SwarCatalogDefaults.builtInGifts.firstOrNull {
      it.mediaUrl == "https://www.instagram.com/reel/Dfqs1NiPAe3/?rpxt=MWE0dmtxcHdlaWNubg=="
    }
    assertNotNull(igGift)
    assertEquals("video", igGift?.mediaType)
    assertEquals("https://www.instagram.com/reel/Dfqs1NiPAe3/embed/", VideoLinkResolver.instagramEmbedUrl(igGift!!.mediaUrl))
  }

  @Test
  fun videoLinkResolver_extractsInstagramVideoUrlWithEscapedAmpersand() {
    val igHtml = """
      <html><script>{"video_url":"https:\/\/scontent-bom1-2.cdninstagram.com\/v\/reel.mp4?efg=abc\u0026_nc_ht=scontent","display_url":"https:\/\/scontent-bom1-2.cdninstagram.com\/v\/thumb.jpg?stp=dst\u0026ccb=7-5"}</script></html>
    """.trimIndent()
    val res = VideoLinkResolver.extractMediaFromHtml(igHtml, "https://www.instagram.com/reel/Dfqs1NiPAe3/embed/captioned/")
    assertTrue(res is VideoLinkResolution.Success)
    val ok = res as VideoLinkResolution.Success
    assertEquals("https://scontent-bom1-2.cdninstagram.com/v/reel.mp4?efg=abc&_nc_ht=scontent", ok.playableUrl)
    assertEquals("https://scontent-bom1-2.cdninstagram.com/v/thumb.jpg?stp=dst&ccb=7-5", ok.thumbnailUrl)
  }

  @Test
  fun videoLinkResolver_extractsPlayableQualityHdAndFbcdnMp4() {
    val html = """
      <html><script>{"playable_url_quality_hd":"https:\/\/video-bom1-2.xx.fbcdn.net\/v\/t42.1790-2\/clip.mp4?efg=123&#x26;oe=456"}</script></html>
    """.trimIndent()
    val res = VideoLinkResolver.extractMediaFromHtml(html, "https://www.instagram.com/reel/Dfqs1NiPAe3/embed/")
    assertTrue(res is VideoLinkResolution.Success)
    val ok = res as VideoLinkResolution.Success
    assertEquals("https://video-bom1-2.xx.fbcdn.net/v/t42.1790-2/clip.mp4?efg=123&oe=456", ok.playableUrl)
  }

  @Test
  fun videoLinkResolver_handlesShareTextProfileReelsAndSocialPageDetection() {
    val pastedShareText = "ये मज़ेदार रील देखो 👉 https://www.instagram.com/creator_name/reel/Dfqs1NiPAe3/?igsh=abc123!"
    val extracted = VideoLinkResolver.extractFirstHttpUrl(pastedShareText)
    assertEquals("https://www.instagram.com/creator_name/reel/Dfqs1NiPAe3/?igsh=abc123!", extracted)
    assertEquals(
      "https://www.instagram.com/reel/Dfqs1NiPAe3/embed/",
      VideoLinkResolver.instagramEmbedUrl(pastedShareText),
    )
    assertEquals(
      "https://www.instagram.com/reel/Dfqs1NiPAe3/",
      VideoLinkResolver.instagramCanonicalReelUrl(pastedShareText),
    )
    // /share/reel/<token> must not mistake "share" for a username
    assertNull(VideoLinkResolver.instagramEmbedUrl("https://www.instagram.com/share/reel/BAcDef123/"))
    assertTrue(VideoLinkResolver.isSocialOrPageVideoUrl(extracted))
    assertFalse(VideoLinkResolver.isSocialOrPageVideoUrl("https://cdn.example.com/gifts/dragon.mp4?token=1"))
  }

  @Test
  fun levelTiers_coversAllThirteenTiersFromLv1ToLv120() {
    val tiers = SwarUserIdentity.levelTiers
    assertEquals(13, tiers.size)
    assertEquals("LV.1~LV.9", tiers.first().rangeLabel)
    assertEquals("LV.120", tiers.last().rangeLabel)
    assertEquals("LV.50", SwarUserIdentity.tierForLevel(54).badgeLabel)
    assertEquals("LV.110~LV.119", SwarUserIdentity.tierForLevel(115).rangeLabel)
    assertEquals(120, SwarUserIdentity.levelForCoins(250_000L).level)
  }
}
