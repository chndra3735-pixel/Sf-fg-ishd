package com.example

import com.example.data.model.GiftEvent
import com.example.data.model.RoomMessage
import com.example.data.model.SwarCatalogDefaults
import com.example.data.model.SwarUserIdentity
import com.example.data.repository.AuditDateLocationHelper
import com.example.data.repository.GitHubUpdateChecker
import com.example.data.repository.VideoLinkResolution
import com.example.data.repository.VideoLinkResolver
import com.example.ui.viewmodel.SwarRoomViewModel
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.Date

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
  fun builtInGifts_isCompletelyEmptyForFreshLinkSetup() {
    assertTrue(SwarCatalogDefaults.builtInGifts.isEmpty())
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
  fun videoLinkResolver_buildsInstagramEmbedUrlForReels() {
    assertEquals(
      "https://www.instagram.com/reel/Dfqs1NiPAe3/embed/",
      VideoLinkResolver.instagramEmbedUrl("https://www.instagram.com/reel/Dfqs1NiPAe3/?rpxt=MWE0dmtxcHdlaWNubg=="),
    )
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

  @Test
  fun unlockedTiersAndHighestConfiguredEffects_resolveHighestUnlockedReward() {
    val unlockedLv25 = SwarUserIdentity.unlockedTiersForLevel(25)
    assertEquals(listOf(1, 10, 20), unlockedLv25.map { it.badgeLevel })

    val configs = mapOf(
      1 to com.example.data.model.LevelTierConfig(
        badgeLevel = 1L,
        avatarFrameUrl = "https://cdn.example.com/frame_lv1.gif",
        entryVideoUrl = "https://cdn.example.com/entry_lv1.mp4",
      ),
      20 to com.example.data.model.LevelTierConfig(
        badgeLevel = 20L,
        avatarFrameUrl = "https://cdn.example.com/frame_lv20.webp",
      ),
      50 to com.example.data.model.LevelTierConfig(
        badgeLevel = 50L,
        avatarFrameUrl = "https://cdn.example.com/frame_lv50.gif",
        entryVideoUrl = "https://cdn.example.com/entry_lv50.mp4",
      ),
    )

    assertEquals(
      "https://cdn.example.com/frame_lv20.webp",
      SwarUserIdentity.highestUnlockedFrameUrl(25, configs),
    )
    val entry = SwarUserIdentity.highestUnlockedEntryVideo(25, configs)
    assertNotNull(entry)
    assertEquals(1, entry?.first?.badgeLevel)
    assertEquals("https://cdn.example.com/entry_lv1.mp4", entry?.second)
  }

  @Test
  fun filterLiveRoomMessages_hidesExitedUsersAndOldJoinHistoryOnReentry() {
    val rejoinedAt = 100_000L
    val oldJoinBeforeReentry = RoomMessage(
      id = "m_old",
      senderUid = "u_old",
      senderName = "Old Visitor",
      text = "Old Visitor का कमरे में स्वागत है",
      messageType = "join",
      createdAt = Timestamp(Date(rejoinedAt - 20_000L)),
    )
    val leftUserJoin = RoomMessage(
      id = "m_left",
      senderUid = "u_left",
      senderName = "Gone User",
      text = "Gone User का कमरे में स्वागत है",
      messageType = "join",
      createdAt = Timestamp(Date(rejoinedAt + 2_000L)),
    )
    val liveUserJoin = RoomMessage(
      id = "m_live",
      senderUid = "u_live",
      senderName = "Active Member",
      text = "Active Member का कमरे में स्वागत है",
      messageType = "join",
      createdAt = Timestamp(Date(rejoinedAt + 4_000L)),
    )
    val normalChat = RoomMessage(
      id = "m_chat",
      senderUid = "u_live",
      senderName = "Active Member",
      text = "Hello room!",
      messageType = "text",
      createdAt = Timestamp(Date(rejoinedAt + 5_000L)),
    )

    val filtered = SwarRoomViewModel.filterLiveRoomMessages(
      messages = listOf(oldJoinBeforeReentry, leftUserJoin, liveUserJoin, normalChat),
      blockedUids = emptySet(),
      liveMemberUids = setOf("u_live"),
      roomJoinedAtMs = rejoinedAt,
    )
    assertEquals(listOf("m_live", "m_chat"), filtered.map { it.id })
  }

  @Test
  fun isTimestampFresh_marksStaleHeartbeatsOfflineAfterThreshold() {
    val now = 1_000_000L
    assertTrue(SwarRoomViewModel.isTimestampFresh(now - 7_000L, now, 25_000L))
    assertFalse(SwarRoomViewModel.isTimestampFresh(now - 30_000L, now, 25_000L))
    assertTrue(SwarRoomViewModel.isTimestampFresh(null, now, 25_000L))
  }

  @Test
  fun githubUpdateChecker_extractsOwnerAndRepoFromAnyLinkOrSlug() {
    assertEquals(
      "chndra/SwarRoom",
      GitHubUpdateChecker.extractRepoSlug("https://github.com/chndra/SwarRoom"),
    )
    assertEquals(
      "chndra/SwarRoom",
      GitHubUpdateChecker.extractRepoSlug("https://github.com/chndra/SwarRoom.git"),
    )
    assertEquals(
      "chndra/SwarRoom",
      GitHubUpdateChecker.extractRepoSlug("Check repo: https://github.com/chndra/SwarRoom/releases/latest"),
    )
    assertEquals(
      "chndra/SwarRoom",
      GitHubUpdateChecker.extractRepoSlug("chndra/SwarRoom"),
    )
    assertNull(GitHubUpdateChecker.extractRepoSlug("https://github.com/owner/repo"))
    assertNull(GitHubUpdateChecker.extractRepoSlug("not-a-repo"))
  }

  @Test
  fun githubUpdateChecker_comparesReleaseVersionsNumerically() {
    assertTrue(GitHubUpdateChecker.isNewerVersion("v1.0.10", "1.0.2", 2))
    assertTrue(GitHubUpdateChecker.isNewerVersion("v1.0.6", "1.0", 5))
    assertFalse(GitHubUpdateChecker.isNewerVersion("v1.0.5", "1.0.5", 5))
    assertFalse(GitHubUpdateChecker.isNewerVersion("v1.0.5", "1.0", 5))
    assertFalse(GitHubUpdateChecker.isNewerVersion("v1.0.3", "1.0.8", 8))
  }

  @Test
  fun shouldPlayRoomGiftEvent_deliversInRoomRegardlessOfServerDelayOrClockSkewAndBlocksAfterExit() {
    val deviceClockJoinedAtMs = 2_000_000L
    // Simulate receiver device clock running 45 seconds ahead of Firestore server clock
    val serverClockOffsetMs = -45_000L
    val serverJoinApproxMs = deviceClockJoinedAtMs + serverClockOffsetMs

    val liveGiftInRoom = GiftEvent(
      id = "evt_live_1",
      roomId = "room_alpha",
      createdAt = Timestamp(Date(serverJoinApproxMs + 1_000L)),
    )
    val delayedGiftInRoom = GiftEvent(
      id = "evt_delayed_2",
      roomId = "room_alpha",
      createdAt = Timestamp(Date(deviceClockJoinedAtMs - 180_000L)),
    )
    val ancientGiftFromBeforeJoin = GiftEvent(
      id = "evt_old_history",
      roomId = "room_alpha",
      createdAt = Timestamp(Date(serverJoinApproxMs - 120_000L)),
    )
    val otherRoomGift = GiftEvent(
      id = "evt_other_room",
      roomId = "room_beta",
      createdAt = Timestamp(Date(serverJoinApproxMs + 2_000L)),
    )

    // Calibrated clock allows gift on initial snapshot even when receiver phone clock is 45s ahead
    assertTrue(
      SwarRoomViewModel.shouldPlayRoomGiftEvent(
        event = liveGiftInRoom,
        activeRoomId = "room_alpha",
        joinedAtLocalMs = deviceClockJoinedAtMs,
        serverClockOffsetMs = serverClockOffsetMs,
        hasServerClockCalibration = true,
        isPostInitialRoomSnapshot = false,
      )
    )

    // Once user is inside the room (post-initial snapshot), NO gift in active room is dropped regardless of delay/skew
    assertTrue(
      SwarRoomViewModel.shouldPlayRoomGiftEvent(
        event = delayedGiftInRoom,
        activeRoomId = "room_alpha",
        joinedAtLocalMs = deviceClockJoinedAtMs,
        serverClockOffsetMs = serverClockOffsetMs,
        hasServerClockCalibration = true,
        isPostInitialRoomSnapshot = true,
      )
    )

    // Pre-existing historical gift from 2 minutes before join does NOT replay on initial room load
    assertFalse(
      SwarRoomViewModel.shouldPlayRoomGiftEvent(
        event = ancientGiftFromBeforeJoin,
        activeRoomId = "room_alpha",
        joinedAtLocalMs = deviceClockJoinedAtMs,
        serverClockOffsetMs = serverClockOffsetMs,
        hasServerClockCalibration = true,
        isPostInitialRoomSnapshot = false,
      )
    )

    // Other room or exited room (activeRoomId = null) never plays
    assertFalse(
      SwarRoomViewModel.shouldPlayRoomGiftEvent(
        event = otherRoomGift,
        activeRoomId = "room_alpha",
        joinedAtLocalMs = deviceClockJoinedAtMs,
        serverClockOffsetMs = serverClockOffsetMs,
        hasServerClockCalibration = true,
        isPostInitialRoomSnapshot = true,
      )
    )
    assertFalse(
      SwarRoomViewModel.shouldPlayRoomGiftEvent(
        event = liveGiftInRoom,
        activeRoomId = null,
        joinedAtLocalMs = 0L,
        serverClockOffsetMs = serverClockOffsetMs,
        hasServerClockCalibration = true,
        isPostInitialRoomSnapshot = true,
      )
    )
  }

  @Test
  fun auditDateLocationHelper_calculatesAgeAndFormatsDob() {
    val refCal = Calendar.getInstance().apply {
      clear()
      set(2026, Calendar.NOVEMBER, 21, 12, 0, 0)
    }
    val nowMs = refCal.timeInMillis
    assertEquals(26, AuditDateLocationHelper.calculateAgeYears("2000-05-10", nowMs))
    assertEquals(25, AuditDateLocationHelper.calculateAgeYears("2000-12-25", nowMs))
    assertEquals(0, AuditDateLocationHelper.calculateAgeYears("2030-01-01", nowMs))
    assertEquals("10/05/2000", AuditDateLocationHelper.formatDobDisplay("2000-05-10"))
  }

  @Test
  fun auditDateLocationHelper_sevenMonthRetentionAndDurationFormatting() {
    val nowMs = 1_760_000_000_000L
    val sevenMonthsOut = AuditDateLocationHelper.sevenMonthsExpiryTimestamp(nowMs)
    assertTrue(AuditDateLocationHelper.isWithinSevenMonths(sevenMonthsOut, Timestamp(Date(nowMs)), nowMs))

    val expiredTimestamp = Timestamp(Date(nowMs - 1_000L))
    assertFalse(AuditDateLocationHelper.isWithinSevenMonths(expiredTimestamp, null, nowMs))

    val oldCreated = Calendar.getInstance().apply {
      timeInMillis = nowMs
      add(Calendar.MONTH, -8)
    }.time
    assertFalse(AuditDateLocationHelper.isWithinSevenMonths(null, Timestamp(oldCreated), nowMs))

    assertEquals("2 घंटा 15 मिनट", AuditDateLocationHelper.formatDurationHindi(2 * 3600L + 15 * 60L))
    assertEquals("45 सेकंड", AuditDateLocationHelper.formatDurationHindi(45L))
    assertTrue(AuditDateLocationHelper.dateKeyFromMillis(nowMs).matches(Regex("""^\d{4}-\d{2}-\d{2}$""")))
  }
}
