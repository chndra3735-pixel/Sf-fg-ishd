package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SwarUserIdentity
import com.example.data.model.UserActivityLog
import com.example.data.model.UserAuditProfile
import com.example.data.repository.AuditDateLocationHelper
import com.example.data.repository.MediaCompressor
import com.example.ui.theme.SwarRoomTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SwarRoom", appName)
  }

  @Test
  fun `main activity launches without crashing`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertEquals("SwarRoom", activity.getString(R.string.app_name))
    controller.pause().stop().destroy()
  }

  @Test
  fun `root composable shows setup screen instead of crashing when firebase is uninitialized`() {
    var attempts = 0
    composeTestRule.setContent {
      SwarRoomTheme {
        SwarRoomAppRoot(
          firebaseAuthProvider = {
            attempts++
            null
          },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("retry_firebase_init_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("retry_firebase_init_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(2, attempts)
  }

  @Test
  fun `media compressor prepares jpg and transparent png gift photos`() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val sampleJpg = File(context.cacheDir, "sample_gift_dp.jpg")
      val samplePng = File(context.cacheDir, "sample_gift_dp.png")
      val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
      bmp.eraseColor(android.graphics.Color.argb(180, 255, 80, 120))
      try {
        sampleJpg.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        samplePng.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val preparedJpg = MediaCompressor.prepareForUpload(
          context = context,
          uri = Uri.fromFile(sampleJpg),
          folder = "gifts",
          isVideo = false,
        )
        assertTrue(preparedJpg.file.exists() && preparedJpg.finalBytes > 0L)
        assertEquals("image/jpeg", preparedJpg.mimeType)
        preparedJpg.file.delete()

        val preparedPng = MediaCompressor.prepareForUpload(
          context = context,
          uri = Uri.fromFile(samplePng),
          folder = "gifts",
          isVideo = false,
        )
        assertTrue(preparedPng.file.exists() && preparedPng.finalBytes > 0L)
        assertTrue(preparedPng.mimeType == "image/webp" || preparedPng.mimeType == "image/jpeg")
        preparedPng.file.delete()
      } finally {
        bmp.recycle()
        sampleJpg.delete()
        samplePng.delete()
      }
    }
  }

  @Test
  fun `clicking profile level opens all 13 large level cards showcase and admin configurator`() {
    val profile = com.example.data.model.UserProfile(
      id = "u1",
      userId = "u1",
      displayName = "Test Singer",
      coins = 14_500L,
    )
    var savedBadgeLevel = -1
    var savedCoins = -1L
    var savedTypingBannerLink = ""
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.screens.ProfileScreen(
          profileState = com.example.ui.viewmodel.UiState.Success(profile),
          isUploadingMedia = false,
          isSupabaseConfigured = true,
          onUploadAvatarDp = {},
          onUpdateProfile = { _, _, _, _, _ -> },
          onSignOut = {},
          isAdminUser = true,
          onSaveLevelTierConfig = { lvl, _, _, _, _, coinsReward, _, _, typLink, _, onDone ->
            savedBadgeLevel = lvl
            savedCoins = coinsReward
            savedTypingBannerLink = typLink
            onDone()
          },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_unlocked_badges_line").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_level_hp_card").assertIsDisplayed()
    composeTestRule.onAllNodesWithText("वर्तमान HP:", substring = true).onFirst().assertIsDisplayed()
    composeTestRule.onAllNodesWithText("HP चाहिए", substring = true).onFirst().assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_level_row").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("level_cards_showcase_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("level_card_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("level_tier_detail_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tier_entry_video_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("tier_frame_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("tier_coins_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("tier_badge_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("tier_typing_banner_input").performScrollTo()
      .performTextInput("https://cdn.example.com/royal_patti.gif")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("typing_banner_live_preview").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("save_level_config_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, savedBadgeLevel)
    assertEquals(0L, savedCoins)
    assertEquals("https://cdn.example.com/royal_patti.gif", savedTypingBannerLink)
    composeTestRule.onNodeWithTag("close_level_cards_button").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `voice room renders sender unlocked typing patti banner behind typed chat messages`() {
    val room = com.example.data.model.VoiceRoom(
      id = "room_patti",
      ownerId = "host_1",
      ownerName = "Host",
      title = "Patti Test Room",
      roomCode = "778899",
    )
    val msg = com.example.data.model.RoomMessage(
      id = "msg_patti_1",
      roomId = "room_patti",
      senderUid = "u_vip",
      senderName = "राजा स्वर",
      text = "शानदार गायकी!",
      messageType = "text",
      badgeTag = "VIP",
    )
    val configs = mapOf(
      1 to com.example.data.model.LevelTierConfig(
        badgeLevel = 1L,
        typingBannerUrl = "https://cdn.example.com/vip_banner.png",
      )
    )
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.screens.VoiceRoomScreen(
          room = room,
          seats = emptyList(),
          messages = listOf(msg),
          currentUserId = "u_vip",
          isAdminUser = false,
          isUploadingMedia = false,
          userCoins = 2500L,
          emojiCatalog = emptyList(),
          giftCatalog = emptyList(),
          latestMarqueeGift = null,
          activeFullScreenGift = null,
          isMicMuted = true,
          isSpeakerOn = true,
          isMonitorLoopbackOn = false,
          micAudioLevel = 0f,
          levelConfigs = configs,
          onLeaveRoom = {},
          onSeatTapped = { _, _, _ -> },
          onLeaveSeat = {},
          onToggleSeatLock = {},
          onToggleMic = { _, _ -> },
          onAudioPermissionGranted = {},
          onToggleSpeaker = {},
          onToggleMonitorLoopback = {},
          onSendMessage = { _, _ -> },
          onSendEmoji = {},
          onAddCustomEmoji = { _, _, _, _, _, _ -> },
          onUpdateCustomEmoji = { _, _, _, _, _, _, _, _ -> },
          onDeleteCustomEmoji = { _, _ -> },
          onSendGift = { _, _, _, _ -> },
          onAddCustomGift = { _, _, _, _, _, _, _, _ -> },
          onUpdateCustomGift = { _, _, _, _, _, _, _, _, _, _, _ -> },
          onDeleteCustomGift = { _, _ -> },
          onDismissFullScreenGift = {},
          onClaimBonusCoins = {},
          onUpdateRoomSettings = { _, _, _ -> },
          onStartDirectChat = { _, _, _ -> },
          onShowToast = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("room_message_typing_banner_msg_patti_1").assertIsDisplayed()
    composeTestRule.onNodeWithText("शानदार गायकी!", substring = true, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun `empty gift panel shows empty catalog state with admin add CTA`() {
    val room = com.example.data.model.VoiceRoom(
      id = "room_empty_gift",
      ownerId = "admin_1",
      ownerName = "Chandra",
      title = "Empty Gift Room",
      roomCode = "102030",
    )
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.GiftPanelSheet(
          room = room,
          seats = emptyList(),
          gifts = emptyList(),
          userCoins = 1000L,
          isAdminUser = true,
          isUploading = false,
          onClaimBonusCoins = {},
          onSendGift = { _, _, _, _ -> },
          onAddCustomGift = { _, _, _, _, _, _, _, onDone -> onDone() },
          onUpdateCustomGift = { _, _, _, _, _, _, _, _, _, _, onDone -> onDone() },
          onDeleteCustomGift = { _, onDone -> onDone() },
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("empty_gift_catalog_state").assertExists().assertIsDisplayed()
    composeTestRule.onNodeWithTag("empty_add_gift_button").assertExists().assertIsDisplayed()
  }

  @Test
  fun `voice room hides full screen gift playback belonging to another room`() {
    val roomB = com.example.data.model.VoiceRoom(
      id = "room_B",
      ownerId = "host_B",
      ownerName = "Host B",
      title = "Room B",
      roomCode = "223344",
    )
    val giftFromRoomA = com.example.data.model.GiftEvent(
      id = "evt_from_room_A",
      roomId = "room_A",
      roomTitle = "Room A",
      senderUid = "u2",
      senderName = "Sender A",
      recipientUid = "host_A",
      recipientName = "Host A",
      giftId = "gift_1",
      giftName = "Royal Gift",
      giftEmoji = "🎁",
      giftMediaUrl = "https://example.com/video.mp4",
      giftMediaType = "video",
      coinCost = 50L,
    )
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.screens.VoiceRoomScreen(
          room = roomB,
          seats = emptyList(),
          messages = emptyList(),
          currentUserId = "u_viewer",
          isAdminUser = false,
          isUploadingMedia = false,
          userCoins = 1000L,
          emojiCatalog = emptyList(),
          giftCatalog = emptyList(),
          latestMarqueeGift = null,
          activeFullScreenGift = giftFromRoomA,
          isMicMuted = true,
          isSpeakerOn = true,
          isMonitorLoopbackOn = false,
          micAudioLevel = 0f,
          onLeaveRoom = {},
          onSeatTapped = { _, _, _ -> },
          onLeaveSeat = {},
          onToggleSeatLock = {},
          onToggleMic = { _, _ -> },
          onAudioPermissionGranted = {},
          onToggleSpeaker = {},
          onToggleMonitorLoopback = {},
          onSendMessage = { _, _ -> },
          onSendEmoji = {},
          onAddCustomEmoji = { _, _, _, _, _, _ -> },
          onUpdateCustomEmoji = { _, _, _, _, _, _, _, _ -> },
          onDeleteCustomEmoji = { _, _ -> },
          onSendGift = { _, _, _, _ -> },
          onAddCustomGift = { _, _, _, _, _, _, _, _ -> },
          onUpdateCustomGift = { _, _, _, _, _, _, _, _, _, _, _ -> },
          onDeleteCustomGift = { _, _ -> },
          onDismissFullScreenGift = {},
          onClaimBonusCoins = {},
          onUpdateRoomSettings = { _, _, _ -> },
          onStartDirectChat = { _, _, _ -> },
          onShowToast = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fullscreen_gift_overlay").assertDoesNotExist()
  }

  @Test
  fun `profile app update center shows available GitHub release and saves admin repo link`() {
    val profile = com.example.data.model.UserProfile(
      id = "admin_1",
      userId = "admin_1",
      displayName = "Chandra",
      coins = 5_000L,
    )
    val updateInfo = com.example.data.repository.AppUpdateInfo(
      hasUpdate = true,
      currentVersionName = "1.0.1",
      currentVersionCode = 1,
      latestVersionTag = "v1.0.9",
      releaseTitle = "SwarRoom APK v1.0.9",
      releaseNotes = "नया रीयल-टाइम अपडेट फीचर",
      downloadUrl = "https://github.com/chndra/SwarRoom/releases/download/v1.0.9/SwarRoom.apk",
      releasePageUrl = "https://github.com/chndra/SwarRoom/releases/tag/v1.0.9",
      repoSlug = "chndra/SwarRoom",
    )
    var savedRepoLink = ""
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.screens.ProfileScreen(
          profileState = com.example.ui.viewmodel.UiState.Success(profile),
          isUploadingMedia = false,
          isSupabaseConfigured = true,
          onUploadAvatarDp = {},
          onUpdateProfile = { _, _, _, _, _ -> },
          onSignOut = {},
          isAdminUser = true,
          configuredGithubRepo = "chndra/SwarRoom",
          appUpdateInfo = updateInfo,
          onSaveGithubRepoLink = { savedRepoLink = it },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_app_update_row").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("app_update_center_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("update_center_download_button").assertExists()
    composeTestRule.onNodeWithTag("check_github_update_button").assertExists()
    composeTestRule.onNodeWithTag("save_github_repo_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals("https://github.com/chndra/SwarRoom", savedRepoLink)
  }

  @Test
  fun `app update available dialog renders update CTA and dismisses cleanly`() {
    var dismissed = false
    val updateInfo = com.example.data.repository.AppUpdateInfo(
      hasUpdate = true,
      currentVersionName = "1.0.1",
      currentVersionCode = 1,
      latestVersionTag = "v1.0.4",
      releaseTitle = "SwarRoom APK v1.0.4",
      releaseNotes = "",
      downloadUrl = "https://github.com/chndra/SwarRoom/releases/latest",
      releasePageUrl = "https://github.com/chndra/SwarRoom/releases/latest",
      repoSlug = "chndra/SwarRoom",
    )
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.AppUpdateAvailableDialog(
          updateInfo = updateInfo,
          onDismiss = { dismissed = true },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("app_update_available_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("download_app_update_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dismiss_app_update_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(dismissed)
  }

  @Test
  fun `admin emoji upload opens link input and submit button`() {
    var submittedLabel = ""
    var submittedLink = ""
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.EmojiPanelSheet(
          emojis = com.example.data.model.SwarCatalogDefaults.builtInEmojis,
          isAdminUser = true,
          isUploading = false,
          onSelectEmoji = {},
          onAddCustomEmoji = { label, _, _, _, emojiLinkUrl, onDone ->
            submittedLabel = label
            submittedLink = emojiLinkUrl
            onDone()
          },
          onUpdateCustomEmoji = { _, _, _, _, _, _, _, onDone -> onDone() },
          onDeleteCustomEmoji = { _, onDone -> onDone() },
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_add_emoji_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("emoji_link_input").assertExists()
    composeTestRule.onNodeWithTag("submit_emoji_button").assertExists()
    composeTestRule.onNodeWithTag("emoji_link_input")
      .performScrollTo()
      .performTextInput("https://example.com/party.gif")
    composeTestRule.onNodeWithTag("emoji_name_input")
      .performScrollTo()
      .performTextInput("पार्टी इमोजी")
    composeTestRule.onNodeWithTag("submit_emoji_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals("https://example.com/party.gif", submittedLink)
    assertEquals("पार्टी इमोजी", submittedLabel)
  }

  @Test
  fun `gift panel renders square DP card with coins overlay and no VIDEO corner badge`() {
    val room = com.example.data.model.VoiceRoom(
      id = "room_gift_grid",
      ownerId = "admin_1",
      ownerName = "Chandra",
      title = "Gift Grid Room",
      roomCode = "445566",
    )
    val videoGift = com.example.data.model.GiftCatalogItem(
      id = "g_royal_car",
      name = "स्पेशल गिफ्ट",
      category = com.example.data.model.SwarCatalogDefaults.giftCategories.first(),
      emoji = "🚗",
      thumbnailUrl = "https://example.com/car_dp.png",
      mediaUrl = "https://example.com/car_video.mp4",
      mediaType = "video",
      coinCost = 999L,
    )
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.GiftPanelSheet(
          room = room,
          seats = emptyList(),
          gifts = listOf(videoGift),
          userCoins = 5000L,
          isAdminUser = true,
          isUploading = false,
          onClaimBonusCoins = {},
          onSendGift = { _, _, _, _ -> },
          onAddCustomGift = { _, _, _, _, _, _, _, onDone -> onDone() },
          onUpdateCustomGift = { _, _, _, _, _, _, _, _, _, _, onDone -> onDone() },
          onDeleteCustomGift = { _, onDone -> onDone() },
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("gift_card_g_royal_car").assertIsDisplayed()
    composeTestRule.onNodeWithTag("gift_dp_square_box_g_royal_car", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("gift_dp_coins_g_royal_car", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("999", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("स्पेशल गिफ्ट", useUnmergedTree = true).assertDoesNotExist()
    composeTestRule.onNodeWithText("VIDEO", useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun `fullscreen gift overlay loads video silently in background without loading spinner text`() {
    val videoEvent = com.example.data.model.GiftEvent(
      id = "evt_bg_preload",
      roomId = "room_1",
      roomTitle = "Room 1",
      senderUid = "u1",
      senderName = "Chandra",
      recipientUid = "u2",
      recipientName = "Guest",
      giftId = "g1",
      giftName = "रॉयल जेट",
      giftEmoji = "✈️",
      giftMediaUrl = "https://example.com/jet.mp4",
      giftMediaType = "video",
      coinCost = 500L,
    )
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.FullScreenGiftOverlay(
          event = videoEvent,
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("वीडियो लोड हो रहा है...").assertDoesNotExist()
  }

  @Test
  fun `network geo fallback parser extracts city location and rejects error responses`() {
    val parsedOk = AuditDateLocationHelper.parseIpGeoJson(
      """{"success":true,"latitude":26.8467,"longitude":80.9462,"city":"Lucknow","region":"Uttar Pradesh","country":"India"}"""
    )
    assertNotNull(parsedOk)
    assertTrue(parsedOk!!.isValidCaptured)
    assertEquals("Lucknow, Uttar Pradesh, India", parsedOk.label)
    assertEquals(26.8467, parsedOk.lat, 0.0001)
    assertEquals(80.9462, parsedOk.lng, 0.0001)

    val parsedFailed = AuditDateLocationHelper.parseIpGeoJson("""{"success":false,"message":"reserved range"}""")
    assertNull(parsedFailed)
  }

  @Test
  fun `first time onboarding blocks submission when location is not captured`() {
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.FirstTimeOnboardingScreen(
          userId = "u_noloc_1",
          userEmail = "noloc@gmail.com",
          displayName = "NoLoc User",
          avatarUrl = "",
          isSubmitting = false,
          onSubmitOnboarding = { _, _, _ -> },
          onSignOut = {},
          initialDobIso = "1996-01-01",
          autoCaptureLocation = false,
          initialLocationResult = com.example.data.repository.CapturedLocationResult(
            label = "GPS / नेटवर्क लोकेशन प्राप्त नहीं हो सकी",
            lat = 0.0,
            lng = 0.0,
            status = "unavailable",
          ),
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_accept_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_submit_button").performScrollTo().assertIsNotEnabled()
  }

  @Test
  fun `first time onboarding gates submission on captured location age input and accept button`() {
    var submittedDob = ""
    var submittedTerms = false
    var submittedLocationStatus = ""
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.FirstTimeOnboardingScreen(
          userId = "u_onboard_1",
          userEmail = "singer1@gmail.com",
          displayName = "रवि शर्मा",
          avatarUrl = "",
          isSubmitting = false,
          onSubmitOnboarding = { dobIso, locResult, terms ->
            submittedDob = dobIso
            submittedLocationStatus = locResult.status
            submittedTerms = terms
          },
          onSignOut = {},
          initialDobIso = "",
          autoCaptureLocation = false,
          initialLocationResult = com.example.data.repository.CapturedLocationResult(
            label = "लखनऊ, उत्तर प्रदेश, भारत",
            lat = 26.8467,
            lng = 80.9462,
            status = "captured",
          ),
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_accept_button").performScrollTo().assertIsNotEnabled()
    composeTestRule.onNodeWithTag("onboarding_submit_button").performScrollTo().assertIsNotEnabled()
    // Under-18 age must keep Accept button and submit button locked
    composeTestRule.onNodeWithTag("onboarding_age_input").performScrollTo().performTextInput("16")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_accept_button").performScrollTo().assertIsNotEnabled()
    composeTestRule.onNodeWithTag("onboarding_submit_button").performScrollTo().assertIsNotEnabled()
    // Entering 18+ age unlocks Accept button
    composeTestRule.onNodeWithTag("onboarding_age_input").performScrollTo().performTextReplacement("25")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_dob_value", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    // Still disabled until Accept is clicked
    composeTestRule.onNodeWithTag("onboarding_submit_button").performScrollTo().assertIsNotEnabled()
    composeTestRule.onNodeWithTag("onboarding_accept_button").performScrollTo().assertIsEnabled().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_submit_button").performScrollTo().assertIsEnabled().performClick()
    composeTestRule.waitForIdle()
    assertEquals(AuditDateLocationHelper.dobIsoFromAgeYears(25), submittedDob)
    assertEquals("captured", submittedLocationStatus)
    assertTrue(submittedTerms)
  }

  @Test
  fun `admin user audit dialog searches by ID filters by date displays chats DPs and toggles block`() {
    val todayKey = AuditDateLocationHelper.dateKeyFromMillis()
    val numA = SwarUserIdentity.numericIdForUid("u_alpha")
    val numB = SwarUserIdentity.numericIdForUid("u_beta")
    val profiles = listOf(
      UserAuditProfile(
        id = "u_alpha",
        userId = "u_alpha",
        numericId = numA,
        userEmail = "alpha@gmail.com",
        displayName = "अमित सिंह",
        dobIso = "2001-08-15",
        ageYears = 25L,
        locationLabel = "जयपुर, राजस्थान",
        locationStatus = "captured",
        termsAccepted = true,
        onboardingCompleted = true,
        isBlockedByAdmin = false,
      ),
      UserAuditProfile(
        id = "u_beta",
        userId = "u_beta",
        numericId = numB,
        userEmail = "beta@gmail.com",
        displayName = "प्रिया वर्मा",
        dobIso = "2002-01-20",
        ageYears = 24L,
        locationLabel = "दिल्ली",
        locationStatus = "captured",
        termsAccepted = true,
        onboardingCompleted = true,
        isBlockedByAdmin = false,
      ),
    )
    val logs = listOf(
      UserActivityLog(
        id = "log_visit_1",
        userId = "u_alpha",
        numericId = numA,
        userEmail = "alpha@gmail.com",
        userName = "अमित सिंह",
        activityType = "room_visit",
        roomId = "room_101",
        roomTitle = "भजन संध्या",
        roomCode = "112233",
        dateKey = todayKey,
        enteredAtMs = 1_760_000_000_000L,
        exitedAtMs = 1_760_000_600_000L,
        durationSeconds = 600L,
      ),
      UserActivityLog(
        id = "log_chat_1",
        userId = "u_alpha",
        numericId = numA,
        userEmail = "alpha@gmail.com",
        userName = "अमित सिंह",
        activityType = "room_chat",
        roomId = "room_101",
        roomTitle = "भजन संध्या",
        textContent = "नमस्ते सभी को!",
        dateKey = todayKey,
        enteredAtMs = 1_760_000_100_000L,
      ),
      UserActivityLog(
        id = "log_dm_dp_1",
        userId = "u_alpha",
        numericId = numA,
        userEmail = "alpha@gmail.com",
        userName = "अमित सिंह",
        activityType = "inbox_media",
        conversationId = "u_alpha_u_beta",
        targetUid = "u_beta",
        targetNumericId = numB,
        targetName = "प्रिया वर्मा",
        textContent = "📷 फोटो / DP भेजा",
        mediaUrl = "https://example.com/sent_dp.jpg",
        dateKey = todayKey,
        enteredAtMs = 1_760_000_200_000L,
      ),
    )

    var blockedTargetUid = ""
    var blockedState = false
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.AdminUserAuditDialog(
          auditProfiles = profiles,
          activityLogs = logs,
          isPurgingExpired = false,
          onPurgeExpiredLogs = {},
          onToggleUserBlock = { uid, _, blocked ->
            blockedTargetUid = uid
            blockedState = blocked
          },
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_user_audit_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("audit_filter_today").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("audit_search_input").performTextInput(numA)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("audit_user_card_$numA").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithText("भजन संध्या", substring = true).onFirst().assertIsDisplayed()
    composeTestRule.onNodeWithTag("audit_tab_room_chats").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("नमस्ते सभी को!").assertIsDisplayed()
    composeTestRule.onNodeWithTag("audit_tab_inbox").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("audit_inbox_dp_log_dm_dp_1").assertExists()
    composeTestRule.onNodeWithTag("audit_toggle_block_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals("u_alpha", blockedTargetUid)
    assertTrue(blockedState)
  }

  @Test
  fun `profile screen shows admin audit setting only to admin and opens audit dialog`() {
    val profile = com.example.data.model.UserProfile(
      id = "admin_1",
      userId = "admin_1",
      displayName = "Chandra",
      coins = 5_000L,
    )
    var purged = false
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.screens.ProfileScreen(
          profileState = com.example.ui.viewmodel.UiState.Success(profile),
          isUploadingMedia = false,
          isSupabaseConfigured = true,
          onUploadAvatarDp = {},
          onUpdateProfile = { _, _, _, _, _ -> },
          onSignOut = {},
          isAdminUser = true,
          onPurgeExpiredAuditLogs = { purged = true },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_admin_audit_row").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(purged)
    composeTestRule.onNodeWithTag("admin_user_audit_dialog").assertIsDisplayed()
  }

  @Test
  fun `room settings studio sheet exposes identity, theme, manager, kick log, music, pin and seat count controls`() {
    val targetNumericId = SwarUserIdentity.numericIdForUid("u_candidate")
    val room = com.example.data.model.VoiceRoom(
      id = "room_studio",
      ownerId = "host_1",
      ownerName = "होस्ट",
      title = "संगीत महफ़िल",
      roomCode = "554433",
      seatCount = 12L,
    )
    val kickLogs = listOf(
      com.example.data.model.RoomKickLog(
        id = "kick_1",
        roomId = "room_studio",
        roomOwnerId = "host_1",
        actorUid = "mgr_1",
        actorNumericId = SwarUserIdentity.numericIdForUid("mgr_1"),
        actorName = "राहुल मैनेजर",
        actorRole = "manager",
        targetUid = "u_bad",
        targetNumericId = SwarUserIdentity.numericIdForUid("u_bad"),
        targetName = "रोहित",
        reason = "नियम उल्लंघन",
        kickedAtMs = 1_760_000_500_000L,
      ),
    )
    var updatedTitle = ""
    var updatedThemeLink = ""
    var addedManagerId = ""
    var pinResult: Pair<Boolean, String>? = null
    var resizedSeats = -1
    var toggledMusic = false

    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.RoomSettingsSheet(
          room = room,
          isHost = true,
          isUploadingMedia = false,
          isMonitorLoopbackOn = false,
          isAgoraConnected = true,
          agoraStatusText = "कनेक्टेड",
          userProfiles = emptyMap(),
          livePresenceMembers = emptyList(),
          seats = emptyList(),
          kickLogs = kickLogs,
          roomMusicState = com.example.audio.RoomMusicState(
            trackTitle = "दिल की धड़कन",
            isPlaying = true,
            isPrepared = true,
            volumePercent = 70,
            usingRoomBroadcast = true,
          ),
          onToggleMonitorLoopback = {},
          onUpdateIdentity = { title, _, _, _ -> updatedTitle = title },
          onUpdateTheme = { _, _, link -> updatedThemeLink = link },
          onAddManagerById = { addedManagerId = it },
          onRemoveManager = {},
          onUpdateRoomPinLock = { enabled, pin -> pinResult = enabled to pin },
          onResizeRoomSeats = { resizedSeats = it },
          onPickMusicUri = {},
          onToggleMusicPlayPause = { toggledMusic = true },
          onStopMusic = {},
          onMusicVolumeChange = {},
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("room_settings_dp_picker").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("room_settings_name_input").performScrollTo().performTextReplacement("नया स्टेज")
    composeTestRule.onNodeWithTag("room_settings_save_identity_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals("नया स्टेज", updatedTitle)

    composeTestRule.onNodeWithTag("room_settings_theme_link_input").performScrollTo()
      .performTextInput("https://cdn.example.com/night_theme.gif")
    composeTestRule.onNodeWithTag("room_settings_apply_theme_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals("https://cdn.example.com/night_theme.gif", updatedThemeLink)

    composeTestRule.onNodeWithTag("room_settings_manager_id_input").performScrollTo()
      .performTextInput(targetNumericId)
    composeTestRule.onNodeWithTag("room_settings_manager_accept_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(targetNumericId, addedManagerId)

    composeTestRule.onNodeWithTag("room_settings_kick_log_kick_1").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("room_settings_pick_music_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("room_settings_music_play_pause").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(toggledMusic)

    composeTestRule.onNodeWithTag("room_settings_pin_input").performScrollTo().performTextInput("4321")
    composeTestRule.onNodeWithTag("room_settings_pin_toggle_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(true to "4321", pinResult)

    composeTestRule.onNodeWithTag("room_settings_seat_count_input").performScrollTo().performTextReplacement("8")
    composeTestRule.onNodeWithTag("room_settings_seat_count_apply_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(8, resizedSeats)
  }

  @Test
  fun `lobby displays room lock badge and triggers locked room callback`() {
    val lockedRoom = com.example.data.model.VoiceRoom(
      id = "room_pin_1",
      ownerId = "host_other",
      ownerName = "होस्ट",
      title = "प्राइवेट कमरा",
      roomCode = "111222",
      isPinLocked = true,
      roomPin = "2580",
      seatCount = 10L,
    )
    var clickedRoomId = ""
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.screens.LobbyScreen(
          roomsState = com.example.ui.viewmodel.UiState.Success(listOf(lockedRoom)),
          currentUserId = "guest_user",
          onRoomClick = { room -> clickedRoomId = room.id },
          onCreateRoomClick = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("room_lock_badge_room_pin_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("room_card_room_pin_1").performClick()
    composeTestRule.waitForIdle()
    assertEquals("room_pin_1", clickedRoomId)
  }

  @Test
  fun `room pin entry dialog rejects wrong pin and admits matching pin`() {
    var admittedPin = ""
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.RoomPinEntryDialog(
          roomTitle = "प्राइवेट कमरा",
          expectedPin = "2580",
          onSuccessPin = { admittedPin = it },
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("room_pin_entry_input").performTextInput("1111")
    composeTestRule.onNodeWithTag("room_pin_submit_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("room_pin_error_text").assertIsDisplayed()
    assertEquals("", admittedPin)

    composeTestRule.onNodeWithTag("room_pin_entry_input").performTextReplacement("2580")
    composeTestRule.onNodeWithTag("room_pin_submit_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals("2580", admittedPin)
  }

  @Test
  fun `voice room renders custom theme, dynamic seats, and allows manager kick on non-owner only`() {
    val room = com.example.data.model.VoiceRoom(
      id = "room_mgr",
      ownerId = "host_owner",
      ownerName = "मालिक",
      title = "मैनेजर स्टेज",
      roomCode = "909090",
      backgroundTheme = "custom",
      themeBackgroundUrl = "https://cdn.example.com/custom_stage.jpg",
      managerUids = listOf("mgr_user"),
      isPinLocked = true,
      roomPin = "4455",
      seatCount = 6L,
    )
    val seats = (0 until 6).map { idx ->
      when (idx) {
        0 -> com.example.data.model.RoomSeat(
          id = "seat_0",
          roomId = room.id,
          roomOwnerId = room.ownerId,
          seatIndex = 0L,
          occupantUid = "host_owner",
          occupantName = "मालिक",
        )
        2 -> com.example.data.model.RoomSeat(
          id = "seat_2",
          roomId = room.id,
          roomOwnerId = room.ownerId,
          seatIndex = 2L,
          occupantUid = "member_u",
          occupantName = "सदस्य वन",
        )
        else -> com.example.data.model.RoomSeat(
          id = "seat_$idx",
          roomId = room.id,
          roomOwnerId = room.ownerId,
          seatIndex = idx.toLong(),
        )
      }
    }
    var kickedUid = ""
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.screens.VoiceRoomScreen(
          room = room,
          seats = seats,
          messages = emptyList(),
          currentUserId = "mgr_user",
          isAdminUser = false,
          isUploadingMedia = false,
          userCoins = 2000L,
          emojiCatalog = emptyList(),
          giftCatalog = emptyList(),
          latestMarqueeGift = null,
          activeFullScreenGift = null,
          isMicMuted = true,
          isSpeakerOn = true,
          isMonitorLoopbackOn = false,
          micAudioLevel = 0f,
          roomMusicState = com.example.audio.RoomMusicState(isPrepared = true, trackTitle = "साउंडट्रैक"),
          onLeaveRoom = {},
          onSeatTapped = { _, _, _ -> },
          onLeaveSeat = {},
          onToggleSeatLock = {},
          onToggleMic = { _, _ -> },
          onAudioPermissionGranted = {},
          onToggleSpeaker = {},
          onToggleMonitorLoopback = {},
          onSendMessage = { _, _ -> },
          onSendEmoji = {},
          onAddCustomEmoji = { _, _, _, _, _, _ -> },
          onUpdateCustomEmoji = { _, _, _, _, _, _, _, _ -> },
          onDeleteCustomEmoji = { _, _ -> },
          onSendGift = { _, _, _, _ -> },
          onAddCustomGift = { _, _, _, _, _, _, _, _ -> },
          onUpdateCustomGift = { _, _, _, _, _, _, _, _, _, _, _ -> },
          onDeleteCustomGift = { _, _ -> },
          onDismissFullScreenGift = {},
          onClaimBonusCoins = {},
          onUpdateRoomSettings = { _, _, _ -> },
          onStartDirectChat = { _, _, _ -> },
          onShowToast = {},
          onKickRoomMember = { uid, _, _ -> kickedUid = uid },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("room_custom_theme_background").assertExists()
    composeTestRule.onNodeWithTag("room_top_lock_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("room_top_music_badge").assertIsDisplayed()
    composeTestRule.onNodeWithText("बात करना").assertDoesNotExist()

    // Tap Host seat as Manager -> cannot kick or mute Room Owner
    composeTestRule.onNodeWithTag("host_seat_badge_0").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("other_profile_kick_button").assertDoesNotExist()
    composeTestRule.onNodeWithTag("other_profile_mute_button").assertDoesNotExist()
    // Clicking block (with no-op handler) cleanly dismisses the ModalBottomSheet
    composeTestRule.onNodeWithTag("other_profile_block_button").performClick()
    composeTestRule.waitForIdle()

    // Tap regular member seat as Manager -> Kick, Mute, Lock visible and working
    composeTestRule.onAllNodesWithText("सदस्य वन").onFirst().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("other_profile_mute_button").assertExists()
    composeTestRule.onNodeWithTag("other_profile_lock_seat").assertExists()
    composeTestRule.onNodeWithTag("other_profile_kick_button").assertExists().performClick()
    composeTestRule.waitForIdle()
    assertEquals("member_u", kickedUid)
  }

  @Test
  fun `profile CP row generates ID and submits 10-digit partner request`() {
    val myUid = "u_cp_sender"
    val partnerNumericId = SwarUserIdentity.numericIdForUid("u_cp_receiver")
    val profile = com.example.data.model.UserProfile(
      id = myUid,
      userId = myUid,
      displayName = "अजय कुमार",
      coins = 3_000L,
    )
    var generatedSynced = false
    var submittedTargetId = ""
    var submittedBondTitle = ""

    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.screens.ProfileScreen(
          profileState = com.example.ui.viewmodel.UiState.Success(profile),
          isUploadingMedia = false,
          isSupabaseConfigured = true,
          onUploadAvatarDp = {},
          onUpdateProfile = { _, _, _, _, _ -> },
          onSignOut = {},
          onGenerateSyncOwnCpId = { generatedSynced = true },
          onSendCpRequestById = { id, bond ->
            submittedTargetId = id
            submittedBondTitle = bond
          },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_cp_row").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cp_partner_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cp_own_numeric_id").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cp_generate_sync_id_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(generatedSynced)

    composeTestRule.onNodeWithTag("cp_partner_id_input").performScrollTo().performTextInput(partnerNumericId)
    composeTestRule.onNodeWithTag("cp_submit_request_button").performScrollTo().assertIsEnabled().performClick()
    composeTestRule.waitForIdle()
    assertEquals(partnerNumericId, submittedTargetId)
    assertTrue(submittedBondTitle.isNotBlank())
  }

  @Test
  fun `incoming CP notification popup lets recipient click agree to join IDs`() {
    val request = com.example.data.model.CpRelationship(
      id = SwarUserIdentity.cpPairId("u_req", "u_rec"),
      memberUids = listOf("u_rec", "u_req"),
      memberNames = mapOf("u_req" to "सोनिया", "u_rec" to "विकास"),
      memberNumericIds = mapOf(
        "u_req" to SwarUserIdentity.numericIdForUid("u_req"),
        "u_rec" to SwarUserIdentity.numericIdForUid("u_rec"),
      ),
      requesterUid = "u_req",
      recipientUid = "u_rec",
      bondTitle = "बेस्ट फ्रेंड",
      status = "pending",
    )
    var agreed = false
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.IncomingCpNotificationDialog(
          request = request,
          isSubmitting = false,
          onAgree = { agreed = true },
          onDecline = {},
          onLater = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("incoming_cp_notification_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cp_popup_agree_button").assertIsEnabled().performClick()
    composeTestRule.waitForIdle()
    assertTrue(agreed)
  }

  @Test
  fun `voice room shows adjacent CP seat bridge animation and tapping either partner shows both joined IDs`() {
    val room = com.example.data.model.VoiceRoom(
      id = "room_cp_stage",
      ownerId = "host_1",
      ownerName = "होस्ट",
      title = "दोस्ती मंच",
      roomCode = "121212",
      seatCount = 10L,
    )
    val guestAId = SwarUserIdentity.numericIdForUid("guest_a")
    val guestBId = SwarUserIdentity.numericIdForUid("guest_b")
    val cpGuestPair = com.example.data.model.CpRelationship(
      id = SwarUserIdentity.cpPairId("guest_a", "guest_b"),
      memberUids = listOf("guest_a", "guest_b"),
      memberNames = mapOf("guest_a" to "अमित दोस्त", "guest_b" to "प्रिया दोस्त"),
      memberNumericIds = mapOf("guest_a" to guestAId, "guest_b" to guestBId),
      requesterUid = "guest_a",
      recipientUid = "guest_b",
      bondTitle = "बेस्ट फ्रेंड",
      status = "accepted",
      cpXp = 650L,
      totalGiftsCount = 12L,
      acceptedAtMs = System.currentTimeMillis() - 40L * 86_400_000L,
    )
    val seats = (0 until 10).map { idx ->
      when (idx) {
        0 -> com.example.data.model.RoomSeat(
          id = "seat_0",
          roomId = room.id,
          roomOwnerId = room.ownerId,
          seatIndex = 0L,
          occupantUid = "host_1",
          occupantName = "होस्ट",
        )
        2 -> com.example.data.model.RoomSeat(
          id = "seat_2",
          roomId = room.id,
          roomOwnerId = room.ownerId,
          seatIndex = 2L,
          occupantUid = "guest_a",
          occupantName = "अमित दोस्त",
        )
        3 -> com.example.data.model.RoomSeat(
          id = "seat_3",
          roomId = room.id,
          roomOwnerId = room.ownerId,
          seatIndex = 3L,
          occupantUid = "guest_b",
          occupantName = "प्रिया दोस्त",
        )
        else -> com.example.data.model.RoomSeat(
          id = "seat_$idx",
          roomId = room.id,
          roomOwnerId = room.ownerId,
          seatIndex = idx.toLong(),
        )
      }
    }

    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.screens.VoiceRoomScreen(
          room = room,
          seats = seats,
          messages = emptyList(),
          currentUserId = "viewer_u",
          isAdminUser = false,
          isUploadingMedia = false,
          userCoins = 2000L,
          emojiCatalog = emptyList(),
          giftCatalog = emptyList(),
          latestMarqueeGift = null,
          activeFullScreenGift = null,
          isMicMuted = true,
          isSpeakerOn = true,
          isMonitorLoopbackOn = false,
          micAudioLevel = 0f,
          acceptedCpLinks = listOf(cpGuestPair),
          onLeaveRoom = {},
          onSeatTapped = { _, _, _ -> },
          onLeaveSeat = {},
          onToggleSeatLock = {},
          onToggleMic = { _, _ -> },
          onAudioPermissionGranted = {},
          onToggleSpeaker = {},
          onToggleMonitorLoopback = {},
          onSendMessage = { _, _ -> },
          onSendEmoji = {},
          onAddCustomEmoji = { _, _, _, _, _, _ -> },
          onUpdateCustomEmoji = { _, _, _, _, _, _, _, _ -> },
          onDeleteCustomEmoji = { _, _ -> },
          onSendGift = { _, _, _, _ -> },
          onAddCustomGift = { _, _, _, _, _, _, _, _ -> },
          onUpdateCustomGift = { _, _, _, _, _, _, _, _, _, _, _ -> },
          onDeleteCustomGift = { _, _ -> },
          onDismissFullScreenGift = {},
          onClaimBonusCoins = {},
          onUpdateRoomSettings = { _, _, _ -> },
          onStartDirectChat = { _, _, _ -> },
          onShowToast = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cp_adjacent_bridge_2_3").assertIsDisplayed()

    // Tap either paired partner -> bottom sheet reveals BOTH joined IDs & names together
    composeTestRule.onAllNodesWithText("प्रिया दोस्त").onFirst().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("sheet_joint_cp_card").performScrollTo().assertIsDisplayed()
    val inJointCard = hasAnyAncestor(hasTestTag("sheet_joint_cp_card"))
    composeTestRule.onNode(hasText("अमित दोस्त") and inJointCard, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onNode(hasText("प्रिया दोस्त") and inJointCard, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onNode(hasText("ID: $guestAId") and inJointCard, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onNode(hasText("ID: $guestBId") and inJointCard, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `lucky gift category blocks sending for non-CP users`() {
    val room = com.example.data.model.VoiceRoom(
      id = "room_lucky_gate",
      ownerId = "host_1",
      ownerName = "होस्ट",
      title = "लकी टेस्ट रूम",
      roomCode = "334455",
    )
    val luckyGift = com.example.data.model.GiftCatalogItem(
      id = "g_cp_lucky_locked",
      name = "दिल बंधन",
      category = com.example.data.model.SwarCatalogDefaults.LUCKY_CP_CATEGORY,
      emoji = "💞",
      coinCost = 199L,
    )
    var sentGiftCalled = false
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.GiftPanelSheet(
          room = room,
          seats = emptyList(),
          gifts = listOf(luckyGift),
          userCoins = 2000L,
          isAdminUser = false,
          isUploading = false,
          currentUserId = "u_single",
          acceptedCpLinks = emptyList(),
          onClaimBonusCoins = {},
          onSendGift = { _, _, _, _ -> sentGiftCalled = true },
          onAddCustomGift = { _, _, _, _, _, _, _, onDone -> onDone() },
          onUpdateCustomGift = { _, _, _, _, _, _, _, _, _, _, onDone -> onDone() },
          onDeleteCustomGift = { _, onDone -> onDone() },
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("gift_category_tab_3").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cp_lucky_category_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("gift_cp_badge_g_cp_lucky_locked", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("send_gift_button").assertIsNotEnabled()
    assertTrue(!sentGiftCalled)
  }

  @Test
  fun `lucky gift category unlocks and auto-selects linked CP partner to send and grow CP level`() {
    val room = com.example.data.model.VoiceRoom(
      id = "room_lucky_cp",
      ownerId = "host_other",
      ownerName = "अन्य होस्ट",
      title = "CP गिफ्ट स्टेज",
      roomCode = "667788",
    )
    val partnerPair = com.example.data.model.CpRelationship(
      id = SwarUserIdentity.cpPairId("u_cp_me", "u_cp_buddy"),
      memberUids = listOf("u_cp_buddy", "u_cp_me"),
      memberNames = mapOf("u_cp_me" to "रोहन", "u_cp_buddy" to "प्रिया"),
      memberNumericIds = mapOf(
        "u_cp_me" to SwarUserIdentity.numericIdForUid("u_cp_me"),
        "u_cp_buddy" to SwarUserIdentity.numericIdForUid("u_cp_buddy"),
      ),
      requesterUid = "u_cp_me",
      recipientUid = "u_cp_buddy",
      status = "accepted",
      acceptedAtMs = 1_700_000_000_000L,
    )
    val luckyGift = com.example.data.model.GiftCatalogItem(
      id = "g_cp_lucky_1",
      name = "रॉयल कपल रिंग",
      category = com.example.data.model.SwarCatalogDefaults.LUCKY_CP_CATEGORY,
      emoji = "💍",
      coinCost = 299L,
    )
    var sentGiftId = ""
    var sentRecipientUid = ""
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.GiftPanelSheet(
          room = room,
          seats = listOf(
            com.example.data.model.RoomSeat(
              id = "seat_2",
              roomId = room.id,
              roomOwnerId = room.ownerId,
              seatIndex = 2L,
              occupantUid = "u_cp_buddy",
              occupantName = "प्रिया",
            )
          ),
          gifts = listOf(luckyGift),
          userCoins = 2000L,
          isAdminUser = false,
          isUploading = false,
          currentUserId = "u_cp_me",
          acceptedCpLinks = listOf(partnerPair),
          onClaimBonusCoins = {},
          onSendGift = { gift, uid, _, _ ->
            sentGiftId = gift.id
            sentRecipientUid = uid
          },
          onAddCustomGift = { _, _, _, _, _, _, _, onDone -> onDone() },
          onUpdateCustomGift = { _, _, _, _, _, _, _, _, _, _, onDone -> onDone() },
          onDeleteCustomGift = { _, onDone -> onDone() },
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("gift_category_tab_3").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cp_lucky_category_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("send_gift_button").assertIsEnabled().performClick()
    composeTestRule.waitForIdle()
    assertEquals("g_cp_lucky_1", sentGiftId)
    assertEquals("u_cp_buddy", sentRecipientUid)
  }

  @Test
  fun `memory card music button auto scans tracks opens library filters and plays selected song`() {
    assertEquals(
      Manifest.permission.READ_MEDIA_AUDIO,
      com.example.audio.DeviceMusicScanner.requiredAudioReadPermission(),
    )
    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app).grantPermissions(com.example.audio.DeviceMusicScanner.requiredAudioReadPermission())

    val tracks = listOf(
      com.example.audio.DeviceMusicTrack(
        id = "t1",
        uri = Uri.parse("content://media/external/audio/media/101"),
        title = "दिलबर धुने",
        artist = "अरिजीत",
        folderName = "Download",
        durationMs = 205_000L,
        isFromSdCard = false,
      ),
      com.example.audio.DeviceMusicTrack(
        id = "t2",
        uri = Uri.parse("content://media/sdcard/audio/media/202"),
        title = "SD गाना मिक्स",
        artist = "डीजे राजा",
        folderName = "SD Card/Music",
        durationMs = 185_000L,
        isFromSdCard = true,
      ),
    )
    val room = com.example.data.model.VoiceRoom(
      id = "room_music_auto",
      ownerId = "host_1",
      ownerName = "होस्ट",
      title = "म्यूजिक रूम",
      roomCode = "778899",
    )
    var scanTriggered = false
    var nextTriggered = false
    var prevTriggered = false
    var selectedTrackId = ""

    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.RoomSettingsSheet(
          room = room,
          isHost = true,
          isUploadingMedia = false,
          isMonitorLoopbackOn = false,
          isAgoraConnected = true,
          agoraStatusText = "कनेक्टेड",
          userProfiles = emptyMap(),
          livePresenceMembers = emptyList(),
          seats = emptyList(),
          kickLogs = emptyList(),
          roomMusicState = com.example.audio.RoomMusicState(
            activeTrackUri = tracks.first().uri.toString(),
            trackTitle = tracks.first().title,
            isPlaying = true,
            isPrepared = true,
          ),
          scannedMusicTracks = tracks,
          isScanningMusic = false,
          onToggleMonitorLoopback = {},
          onUpdateIdentity = { _, _, _, _ -> },
          onUpdateTheme = { _, _, _ -> },
          onAddManagerById = {},
          onRemoveManager = {},
          onUpdateRoomPinLock = { _, _ -> },
          onResizeRoomSeats = {},
          onPickMusicUri = {},
          onToggleMusicPlayPause = {},
          onStopMusic = {},
          onMusicVolumeChange = {},
          onScanMemoryCardMusic = { scanTriggered = true },
          onPlayScannedMusicTrack = { track -> selectedTrackId = track.id },
          onPlayNextMusic = { nextTriggered = true },
          onPlayPreviousMusic = { prevTriggered = true },
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("room_settings_music_prev").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("room_settings_music_next").performScrollTo().performClick()
    assertTrue(prevTriggered)
    assertTrue(nextTriggered)

    composeTestRule.onNodeWithTag("room_settings_pick_music_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(scanTriggered)
    composeTestRule.onNodeWithTag("memory_card_music_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("music_library_list").assertIsDisplayed()
    composeTestRule.onNodeWithTag("music_track_row_t1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("music_track_row_t2").assertIsDisplayed()

    composeTestRule.onNodeWithTag("music_library_search_input").performTextInput("SD गाना")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("music_track_row_t1").assertDoesNotExist()
    composeTestRule.onNodeWithTag("music_track_row_t2").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assertEquals("t2", selectedTrackId)
    composeTestRule.onNodeWithTag("memory_card_music_dialog").assertDoesNotExist()
  }

  @Test
  fun `memory card music dialog handles duplicate track ids and missing permission without crashing`() {
    val duplicateTracks = listOf(
      com.example.audio.DeviceMusicTrack(
        id = "ms_external_primary_42",
        uri = Uri.parse("content://media/external_primary/audio/media/42"),
        title = "सफर गीत",
        artist = "गायक एक",
        folderName = "Music",
        durationMs = 180_000L,
      ),
      com.example.audio.DeviceMusicTrack(
        id = "ms_external_primary_42",
        uri = Uri.parse("content://media/external/audio/media/42"),
        title = "सफर गीत",
        artist = "गायक एक",
        folderName = "Music",
        durationMs = 180_000L,
      ),
    )
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.RoomSettingsSheet(
          room = com.example.data.model.VoiceRoom(
            id = "room_dup_music",
            ownerId = "host_1",
            ownerName = "होस्ट",
            title = "सेफ म्यूजिक",
            roomCode = "101010",
          ),
          isHost = true,
          isUploadingMedia = false,
          isMonitorLoopbackOn = false,
          isAgoraConnected = false,
          agoraStatusText = "",
          userProfiles = emptyMap(),
          livePresenceMembers = emptyList(),
          seats = emptyList(),
          kickLogs = emptyList(),
          roomMusicState = com.example.audio.RoomMusicState(),
          scannedMusicTracks = duplicateTracks,
          isScanningMusic = false,
          onToggleMonitorLoopback = {},
          onUpdateIdentity = { _, _, _, _ -> },
          onUpdateTheme = { _, _, _ -> },
          onAddManagerById = {},
          onRemoveManager = {},
          onUpdateRoomPinLock = { _, _ -> },
          onResizeRoomSeats = {},
          onPickMusicUri = {},
          onToggleMusicPlayPause = {},
          onStopMusic = {},
          onMusicVolumeChange = {},
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("room_settings_pick_music_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("memory_card_music_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("music_grant_permission_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("music_library_list").assertIsDisplayed()
  }

  @Test
  fun `admin user audit dialog allows entering 10 digit ID and adding custom coins`() {
    val targetNumericId = SwarUserIdentity.numericIdForUid("u_coin_target")
    var lookedUpId = ""
    var grantedId = ""
    var grantedAmount = ""

    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.AdminUserAuditDialog(
          auditProfiles = emptyList(),
          activityLogs = emptyList(),
          isPurgingExpired = false,
          onPurgeExpiredLogs = {},
          onToggleUserBlock = { _, _, _ -> },
          onDismiss = {},
          lastCoinGrantSummary = com.example.data.model.AdminCoinGrantSummary(
            targetUid = "u_coin_target",
            numericId = targetNumericId,
            displayName = "अमित सिंह",
            previousCoins = 1_500L,
            addedCoins = 250_000L,
            newBalance = 251_500L,
          ),
          onLookupCoinTargetById = { id -> lookedUpId = id },
          onGrantCoinsById = { id, amt ->
            grantedId = id
            grantedAmount = amt
          },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_coin_topup_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("audit_coin_status_banner").assertIsDisplayed()

    composeTestRule.onNodeWithTag("audit_coin_id_input").performTextReplacement(targetNumericId)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("audit_coin_lookup_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(targetNumericId, lookedUpId)

    composeTestRule.onNodeWithTag("audit_coin_amount_input").performTextReplacement("250000")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("audit_coin_grant_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(targetNumericId, grantedId)
    assertEquals("250000", grantedAmount)
  }

  @Test
  fun `admin user detail view expands coin grant card and grants coins to selected user`() {
    val numA = SwarUserIdentity.numericIdForUid("u_alpha")
    val profiles = listOf(
      com.example.data.model.UserAuditProfile(
        id = "u_alpha",
        userId = "u_alpha",
        numericId = numA,
        userEmail = "alpha@gmail.com",
        displayName = "अमित सिंह",
      ),
    )
    var detailLookedUpId = ""
    var detailGrantedId = ""
    var detailGrantedAmt = ""

    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.AdminUserAuditDialog(
          auditProfiles = profiles,
          activityLogs = emptyList(),
          isPurgingExpired = false,
          onPurgeExpiredLogs = {},
          onToggleUserBlock = { _, _, _ -> },
          onDismiss = {},
          lastCoinGrantSummary = com.example.data.model.AdminCoinGrantSummary(
            targetUid = "u_alpha",
            numericId = numA,
            displayName = "अमित सिंह",
            previousCoins = 2_000L,
            addedCoins = 0L,
            newBalance = 2_000L,
          ),
          onLookupCoinTargetById = { detailLookedUpId = it },
          onGrantCoinsById = { id, amt ->
            detailGrantedId = id
            detailGrantedAmt = amt
          },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("audit_search_input").performTextInput(numA)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("audit_user_card_$numA").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("audit_detail_coin_toggle_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(numA, detailLookedUpId)

    composeTestRule.onNodeWithTag("audit_detail_coin_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("audit_detail_coin_status_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("audit_detail_coin_amount_input").performTextReplacement("50000")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("audit_detail_coin_grant_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(numA, detailGrantedId)
    assertEquals("50000", detailGrantedAmt)
  }

  @Test
  fun `level showcase roadmap tab renders progressive milestones current LV26 status and LV85 halfway marker`() {
    val coins = 247_545L
    val levelInfo = SwarUserIdentity.levelForCoins(coins)
    assertEquals(26, levelInfo.level)

    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.LevelCardsShowcaseDialog(
          levelInfo = levelInfo,
          coins = coins,
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("level_cards_showcase_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("level_roadmap_tab").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("level_roadmap_list").assertIsDisplayed()
    composeTestRule.onNodeWithTag("level_roadmap_list")
      .performScrollToNode(hasTestTag("roadmap_current_status_banner"))
    composeTestRule.onNodeWithTag("roadmap_current_status_banner").assertIsDisplayed()
    composeTestRule.onNodeWithText("★ आपकी वर्तमान स्थिति: LV.26 (कुल HP: 2,47,545 | आधा: 1,23,772.5)")
      .assertIsDisplayed()
    composeTestRule.onAllNodesWithText("अगले LV.27 के लिए", substring = true).onFirst().assertIsDisplayed()
    composeTestRule.onNodeWithTag("level_roadmap_list")
      .performScrollToNode(hasTestTag("roadmap_halfway_banner"))
    composeTestRule.onNodeWithTag("roadmap_halfway_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("level_roadmap_list")
      .performScrollToNode(hasTestTag("roadmap_milestone_120"))
    composeTestRule.onNodeWithText("कुल HP: 47,78,163").assertIsDisplayed()
  }

  @Test
  fun `voice room rocket widget displays top 3 DPs below rocket and opens 5 tier details dialog`() {
    val room = com.example.data.model.VoiceRoom(
      id = "room_rkt_ui",
      ownerId = "host_1",
      ownerName = "होस्ट",
      title = "रॉकेट लॉन्च रूम",
      roomCode = "555666",
      seatCount = 8L,
    )
    val rocket = com.example.data.model.RoomRocketState(
      id = room.id,
      roomId = room.id,
      roomOwnerId = room.ownerId,
      rocketLevel = 3,
      currentCoins = 2_862_820L,
      targetCoins = 5_725_640L,
      contributors = mapOf(
        "u1" to 1_200_000L,
        "u2" to 900_000L,
        "u3" to 600_000L,
        "u4" to 162_820L,
      ),
      contributorNames = mapOf(
        "u1" to "राजा #1",
        "u2" to "प्रिया #2",
        "u3" to "अमित #3",
        "u4" to "विकास #4",
      ),
    )

    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.screens.VoiceRoomScreen(
          room = room,
          seats = emptyList(),
          messages = emptyList(),
          currentUserId = "u1",
          isAdminUser = false,
          isUploadingMedia = false,
          userCoins = 10_000L,
          emojiCatalog = emptyList(),
          giftCatalog = emptyList(),
          latestMarqueeGift = null,
          activeFullScreenGift = null,
          isMicMuted = true,
          isSpeakerOn = true,
          isMonitorLoopbackOn = false,
          micAudioLevel = 0f,
          roomRocketState = rocket,
          onLeaveRoom = {},
          onSeatTapped = { _, _, _ -> },
          onLeaveSeat = {},
          onToggleSeatLock = {},
          onToggleMic = { _, _ -> },
          onAudioPermissionGranted = {},
          onToggleSpeaker = {},
          onToggleMonitorLoopback = {},
          onSendMessage = { _, _ -> },
          onSendEmoji = {},
          onAddCustomEmoji = { _, _, _, _, _, _ -> },
          onUpdateCustomEmoji = { _, _, _, _, _, _, _, _ -> },
          onDeleteCustomEmoji = { _, _ -> },
          onSendGift = { _, _, _, _ -> },
          onAddCustomGift = { _, _, _, _, _, _, _, _ -> },
          onUpdateCustomGift = { _, _, _, _, _, _, _, _, _, _, _ -> },
          onDeleteCustomGift = { _, _ -> },
          onDismissFullScreenGift = {},
          onClaimBonusCoins = {},
          onUpdateRoomSettings = { _, _, _ -> },
          onStartDirectChat = { _, _, _ -> },
          onShowToast = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("room_rocket_widget").assertIsDisplayed()
    val rocketBounds = composeTestRule.onNodeWithTag("room_rocket_widget").fetchSemanticsNode().boundsInRoot
    val giftBounds = composeTestRule.onNodeWithTag("open_gift_panel_button").fetchSemanticsNode().boundsInRoot
    assertTrue("Rocket should sit above Gift Box", rocketBounds.bottom <= giftBounds.top)
    assertTrue("Rocket should sit above Gift Box at right edge", rocketBounds.right >= giftBounds.left)
    composeTestRule.onNodeWithTag("room_rocket_level_text", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("room_rocket_top3_dps", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("rocket_top3_dp_slot_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("rocket_top3_dp_slot_2", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("rocket_top3_dp_slot_3", useUnmergedTree = true).assertIsDisplayed()

    composeTestRule.onNodeWithTag("room_rocket_widget").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("rocket_details_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("टॉप 3 से बाहर").assertExists()
    composeTestRule.onNodeWithTag("rocket_tier_row_1").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("rocket_tier_row_5").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `full screen rocket launch overlay rides rank 1 DP on rocket blasts and displays top 3 payouts`() {
    val launchEvent = com.example.data.model.RocketLaunchEvent(
      id = "rkt_anim_1",
      roomId = "room_anim",
      roomTitle = "सेलिब्रेशन स्टेज",
      launcherUid = "u1",
      rocketLevel = 2,
      targetCoins = 1_900_040L,
      totalRewardCoins = 52_500L,
      totalRewardHp = 6_000L,
      winnerUids = listOf("u1", "u2", "u3"),
      winnerNames = mapOf("u1" to "सुपरस्टार राज", "u2" to "प्रिया", "u3" to "रोहन"),
      winnerContributions = mapOf("u1" to 900_000L, "u2" to 600_000L, "u3" to 400_040L),
      winnerRewardCoins = mapOf("u1" to 17_500L, "u2" to 17_500L, "u3" to 17_500L),
      winnerRewardHp = mapOf("u1" to 2_000L, "u2" to 2_000L, "u3" to 2_000L),
    )
    var dismissed = false
    composeTestRule.mainClock.autoAdvance = false
    try {
      composeTestRule.setContent {
        SwarRoomTheme {
          com.example.ui.components.FullScreenRocketLaunchOverlay(
            event = launchEvent,
            onDismiss = { dismissed = true },
          )
        }
      }
      // Mid-ascent: #1 contributor's DP rides the rocket upward
      composeTestRule.mainClock.advanceTimeBy(800L)
      composeTestRule.onNodeWithTag("full_screen_rocket_launch_overlay").assertIsDisplayed()
      composeTestRule.onNodeWithTag("rocket_launch_rank1_dp").assertIsDisplayed()

      // Post-blast celebration: Top-3 winners & payout card appears
      composeTestRule.mainClock.advanceTimeBy(4_500L)
      composeTestRule.onNodeWithTag("rocket_launch_winners_card").assertIsDisplayed()
      composeTestRule.onNodeWithText("सुपरस्टार राज").assertIsDisplayed()
      composeTestRule.onAllNodesWithText("+17,500 🪙", substring = true).onFirst().assertExists()
      composeTestRule.onAllNodesWithText("+2,000 HP", substring = true).onFirst().assertExists()

      composeTestRule.onNodeWithTag("dismiss_rocket_launch_button").performClick()
      assertTrue(dismissed)
    } finally {
      composeTestRule.mainClock.autoAdvance = true
    }
  }

  @Test
  fun `voice room shows mic badge on seat DP only when muted and hides it when unmuted`() {
    val room = com.example.data.model.VoiceRoom(
      id = "room_mic_badge",
      ownerId = "host_1",
      ownerName = "होस्ट",
      title = "ऑडियो टेस्ट रूम",
      roomCode = "445566",
      seatCount = 6L,
    )
    val seats = listOf(
      com.example.data.model.RoomSeat(
        id = "seat_0",
        roomId = "room_mic_badge",
        seatIndex = 0L,
        occupantUid = "host_1",
        occupantName = "म्यूट होस्ट",
        isMuted = true,
      ),
      com.example.data.model.RoomSeat(
        id = "seat_1",
        roomId = "room_mic_badge",
        seatIndex = 1L,
        occupantUid = "cohost_2",
        occupantName = "अनम्यूट कोहोस्ट",
        isMuted = false,
      ),
      com.example.data.model.RoomSeat(
        id = "seat_2",
        roomId = "room_mic_badge",
        seatIndex = 2L,
        occupantUid = "guest_3",
        occupantName = "म्यूट गेस्ट",
        isMuted = true,
      ),
      com.example.data.model.RoomSeat(
        id = "seat_3",
        roomId = "room_mic_badge",
        seatIndex = 3L,
        occupantUid = "guest_4",
        occupantName = "अनम्यूट गेस्ट",
        isMuted = false,
      ),
    )

    var speakerState = true
    composeTestRule.setContent {
      val isSpeakerOn = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
      SwarRoomTheme {
        com.example.ui.screens.VoiceRoomScreen(
          room = room,
          seats = seats,
          messages = emptyList(),
          currentUserId = "viewer_1",
          isAdminUser = false,
          isUploadingMedia = false,
          userCoins = 1000L,
          emojiCatalog = emptyList(),
          giftCatalog = emptyList(),
          latestMarqueeGift = null,
          activeFullScreenGift = null,
          isMicMuted = true,
          isSpeakerOn = isSpeakerOn.value,
          isMonitorLoopbackOn = false,
          micAudioLevel = 0f,
          onLeaveRoom = {},
          onSeatTapped = { _, _, _ -> },
          onLeaveSeat = {},
          onToggleSeatLock = {},
          onToggleMic = { _, _ -> },
          onAudioPermissionGranted = {},
          onToggleSpeaker = {
            isSpeakerOn.value = !isSpeakerOn.value
            speakerState = isSpeakerOn.value
          },
          onToggleMonitorLoopback = {},
          onSendMessage = { _, _ -> },
          onSendEmoji = {},
          onAddCustomEmoji = { _, _, _, _, _, _ -> },
          onUpdateCustomEmoji = { _, _, _, _, _, _, _, _ -> },
          onDeleteCustomEmoji = { _, _ -> },
          onSendGift = { _, _, _, _ -> },
          onAddCustomGift = { _, _, _, _, _, _, _, _ -> },
          onUpdateCustomGift = { _, _, _, _, _, _, _, _, _, _, _ -> },
          onDeleteCustomGift = { _, _ -> },
          onDismissFullScreenGift = {},
          onClaimBonusCoins = {},
          onUpdateRoomSettings = { _, _, _ -> },
          onStartDirectChat = { _, _, _ -> },
          onShowToast = {},
        )
      }
    }
    composeTestRule.waitForIdle()

    // Muted host & guest DPs show the muted badge; unmuted host & guest DPs hide it completely
    composeTestRule.onNodeWithTag("host_seat_muted_badge_0", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("host_seat_muted_badge_1", useUnmergedTree = true).assertDoesNotExist()
    composeTestRule.onNodeWithTag("guest_seat_muted_badge_2", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("guest_seat_muted_badge_3", useUnmergedTree = true).assertDoesNotExist()

    composeTestRule.onNodeWithTag("room_speaker_toggle").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(false, speakerState)
    composeTestRule.onNodeWithTag("room_speaker_toggle").performClick()
    composeTestRule.waitForIdle()
    assertEquals(true, speakerState)
  }

  @Test
  fun `emoji panel locks love category for single user and blocks sending`() {
    var sentEmoji: com.example.data.model.EmojiCatalogItem? = null
    var blockedReason: String? = null
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.EmojiPanelSheet(
          emojis = com.example.data.model.SwarCatalogDefaults.builtInEmojis,
          isAdminUser = false,
          isUploading = false,
          onSelectEmoji = { sentEmoji = it },
          onAddCustomEmoji = { _, _, _, _, _, _ -> },
          onUpdateCustomEmoji = { _, _, _, _, _, _, _, _ -> },
          onDeleteCustomEmoji = { _, _ -> },
          onDismiss = {},
          currentUserId = "single_user_1",
          acceptedCpLinks = emptyList(),
          onBlockedCpEmoji = { blockedReason = it },
        )
      }
    }
    composeTestRule.waitForIdle()
    // Category index 2 is "प्यार"
    composeTestRule.onNodeWithTag("emoji_category_tab_2").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cp_love_emoji_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("emoji_locked_badge_def_e9", useUnmergedTree = true).assertIsDisplayed()

    composeTestRule.onNodeWithTag("emoji_item_def_e9").performClick()
    composeTestRule.waitForIdle()
    assertNull(sentEmoji)
    assertNotNull(blockedReason)
    assertTrue(blockedReason!!.contains("CP"))
  }

  @Test
  fun `emoji panel unlocks love category for joined CP partners and sends cleanly`() {
    val joinedCp = com.example.data.model.CpRelationship(
      id = SwarUserIdentity.cpPairId("u_one", "u_two"),
      memberUids = listOf("u_one", "u_two"),
      memberNames = mapOf("u_one" to "कबीर", "u_two" to "मीरा"),
      memberNumericIds = mapOf(
        "u_one" to SwarUserIdentity.numericIdForUid("u_one"),
        "u_two" to SwarUserIdentity.numericIdForUid("u_two"),
      ),
      requesterUid = "u_one",
      recipientUid = "u_two",
      status = "accepted",
      cpXp = 400L,
      acceptedAtMs = 1_700_000_000_000L,
    )
    var sentEmoji: com.example.data.model.EmojiCatalogItem? = null
    var blockedReason: String? = null
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.EmojiPanelSheet(
          emojis = com.example.data.model.SwarCatalogDefaults.builtInEmojis,
          isAdminUser = false,
          isUploading = false,
          onSelectEmoji = { sentEmoji = it },
          onAddCustomEmoji = { _, _, _, _, _, _ -> },
          onUpdateCustomEmoji = { _, _, _, _, _, _, _, _ -> },
          onDeleteCustomEmoji = { _, _ -> },
          onDismiss = {},
          currentUserId = "u_one",
          acceptedCpLinks = listOf(joinedCp),
          onBlockedCpEmoji = { blockedReason = it },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("emoji_category_tab_2").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("cp_love_emoji_banner").assertIsDisplayed()
    composeTestRule.onAllNodesWithText("मीरा", substring = true).onFirst().assertIsDisplayed()
    composeTestRule.onNodeWithTag("emoji_locked_badge_def_e9", useUnmergedTree = true).assertDoesNotExist()

    composeTestRule.onNodeWithTag("emoji_item_def_e9").performClick()
    composeTestRule.waitForIdle()
    assertNull(blockedReason)
    assertEquals("def_e9", sentEmoji?.id)
  }

  @Test
  fun `profile level stays at LV1 when wallet coins are recharged high and advances strictly with earned rocket HP`() {
    val profileState = androidx.compose.runtime.mutableStateOf<com.example.ui.viewmodel.UiState<com.example.data.model.UserProfile?>>(
      com.example.ui.viewmodel.UiState.Success(
        com.example.data.model.UserProfile(
          id = "u_lvl_check",
          userId = "u_lvl_check",
          displayName = "राहुल",
          coins = 50_000_000L,
          hp = 0L,
        )
      )
    )
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.screens.ProfileScreen(
          profileState = profileState.value,
          isUploadingMedia = false,
          isSupabaseConfigured = true,
          onUploadAvatarDp = {},
          onUpdateProfile = { _, _, _, _, _ -> },
          onSignOut = {},
          onClaimBonusCoins = {},
          giftCatalog = emptyList(),
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithText("LV.1 • नया स्वर", substring = true).onFirst().assertIsDisplayed()
    composeTestRule.onAllNodesWithText("वर्तमान HP: 0 HP", substring = true).onFirst().assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithText("LV.120", substring = true).fetchSemanticsNodes().size)

    // Earn Rocket HP -> Level converts strictly from HP
    profileState.value = com.example.ui.viewmodel.UiState.Success(
      com.example.data.model.UserProfile(
        id = "u_lvl_check",
        userId = "u_lvl_check",
        displayName = "राहुल",
        coins = 50_000_000L,
        hp = 31_149L,
      )
    )
    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithText("LV.10", substring = true).onFirst().assertIsDisplayed()
    composeTestRule.onAllNodesWithText("31,149 HP", substring = true).onFirst().assertIsDisplayed()
  }

  @Test
  fun `local media cache manager persists entry mute and server saver preferences`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.repository.LocalMediaCacheManager.setEntryVideoMuted(context, false)
    com.example.data.repository.LocalMediaCacheManager.setServerSaverEnabled(context, true)
    assertEquals(false, com.example.data.repository.LocalMediaCacheManager.isEntryVideoMuted(context))
    assertEquals(true, com.example.data.repository.LocalMediaCacheManager.isServerSaverEnabled(context))
    assertEquals(context.filesDir, com.example.data.repository.LocalMediaCacheManager.preferredMediaBaseDir(context))

    com.example.data.repository.LocalMediaCacheManager.setEntryVideoMuted(context, true)
    com.example.data.repository.LocalMediaCacheManager.setServerSaverEnabled(context, false)
    assertEquals(true, com.example.data.repository.LocalMediaCacheManager.isEntryVideoMuted(context))
    assertEquals(false, com.example.data.repository.LocalMediaCacheManager.isServerSaverEnabled(context))
    assertEquals(context.cacheDir, com.example.data.repository.LocalMediaCacheManager.preferredMediaBaseDir(context))

    // Restore clean defaults
    com.example.data.repository.LocalMediaCacheManager.setEntryVideoMuted(context, false)
    com.example.data.repository.LocalMediaCacheManager.setServerSaverEnabled(context, true)
  }

  @Test
  fun `level showcase style tab equips unlocked tier clears to plain and toggles entry mute`() {
    val hp120 = SwarUserIdentity.requiredHpForLevel(120)
    val configs = mapOf(
      10 to com.example.data.model.LevelTierConfig(
        badgeLevel = 10,
        avatarFrameUrl = "https://cdn.example.com/f10.png",
        typingBannerUrl = "https://cdn.example.com/t10.png",
        entryVideoUrl = "https://cdn.example.com/e10.mp4",
      ),
      120 to com.example.data.model.LevelTierConfig(
        badgeLevel = 120,
        avatarFrameUrl = "https://cdn.example.com/f120.png",
        typingBannerUrl = "https://cdn.example.com/t120.png",
        entryVideoUrl = "https://cdn.example.com/e120.mp4",
      ),
    )
    var equippedFrame = -1L
    var equippedTyping = -1L
    var equippedEntry = -1L
    var mutedState = false

    composeTestRule.setContent {
      val frameState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(-1L) }
      val typingState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(-1L) }
      val entryState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(-1L) }
      val muteState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

      SwarRoomTheme {
        com.example.ui.components.LevelCardsShowcaseDialog(
          levelInfo = SwarUserIdentity.levelForCoins(hp120),
          coins = 10_000L,
          onDismiss = {},
          levelConfigs = configs,
          claimedTiers = emptySet(),
          isAdminUser = false,
          isUploadingMedia = false,
          sampleAvatarUrl = "",
          sampleDisplayName = "सम्राट",
          onSaveTierConfig = { _, _, _, _, _, _, _, _, _, _, onDone -> onDone() },
          onClaimTierReward = {},
          initialTab = 2,
          equippedFrameLevel = frameState.value,
          equippedTypingLevel = typingState.value,
          equippedEntryVideoLevel = entryState.value,
          onEquipCosmetic = { f, t, e ->
            f?.let {
              frameState.value = it
              equippedFrame = it
            }
            t?.let {
              typingState.value = it
              equippedTyping = it
            }
            e?.let {
              entryState.value = it
              equippedEntry = it
            }
          },
          isEntryVideoMuted = muteState.value,
          onToggleEntryVideoMuted = {
            muteState.value = !muteState.value
            mutedState = muteState.value
          },
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("style_customizer_panel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("style_entry_mute_switch").performClick()
    composeTestRule.waitForIdle()
    assertEquals(true, mutedState)

    composeTestRule.onNodeWithTag("style_frame_option_10").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(10L, equippedFrame)

    composeTestRule.onNodeWithTag("style_typing_option_10").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(10L, equippedTyping)

    composeTestRule.onNodeWithTag("style_entry_option_10").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(10L, equippedEntry)

    composeTestRule.onNodeWithTag("style_clear_all_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(0L, equippedFrame)
    assertEquals(0L, equippedTyping)
    assertEquals(0L, equippedEntry)
  }

  @Test
  fun `profile local storage card toggles server saver entry mute and clears local cache`() {
    var serverSaver = true
    var entryMuted = false
    var cacheCleared = false
    val profileState = com.example.ui.viewmodel.UiState.Success(
      com.example.data.model.UserProfile(
        id = "u_storage",
        userId = "u_storage",
        displayName = "काव्या",
        coins = 5_000L,
        hp = 40_000L,
      )
    )

    composeTestRule.setContent {
      val saverState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
      val muteState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
      val statsState = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
          com.example.data.repository.LocalCacheStats(
            mediaBytes = 4_194_304L,
            imageCacheBytes = 1_048_576L,
            httpCacheBytes = 0L,
            cachedMediaFiles = 6,
          )
        )
      }
      SwarRoomTheme {
        com.example.ui.screens.ProfileScreen(
          profileState = profileState,
          isUploadingMedia = false,
          isSupabaseConfigured = true,
          onUploadAvatarDp = {},
          onUpdateProfile = { _, _, _, _, _ -> },
          onSignOut = {},
          isEntryVideoMuted = muteState.value,
          onToggleEntryVideoMuted = {
            muteState.value = !muteState.value
            entryMuted = muteState.value
          },
          isServerSaverEnabled = saverState.value,
          onToggleServerSaverEnabled = { enabled ->
            saverState.value = enabled
            serverSaver = enabled
          },
          localCacheStats = statsState.value,
          onClearLocalCache = {
            cacheCleared = true
            statsState.value = com.example.data.repository.LocalCacheStats()
          },
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("profile_cosmetic_customizer_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_local_storage_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("local_cache_size_text").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("profile_entry_mute_toggle").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(true, entryMuted)

    composeTestRule.onNodeWithTag("server_saver_toggle").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(false, serverSaver)

    composeTestRule.onNodeWithTag("clear_local_cache_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(true, cacheCleared)
  }

  @Test
  fun `admin audit dialog sets user level up and down freely by 10 digit id`() {
    var updatedId = ""
    var updatedLevel = 0
    val targetNumericId = "4829103756"

    composeTestRule.setContent {
      val summaryState = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<com.example.data.model.AdminLevelUpdateSummary?>(null)
      }
      SwarRoomTheme {
        com.example.ui.components.AdminUserAuditDialog(
          auditProfiles = emptyList(),
          activityLogs = emptyList(),
          isPurgingExpired = false,
          onPurgeExpiredLogs = {},
          onToggleUserBlock = { _, _, _ -> },
          onDismiss = {},
          isUpdatingLevel = false,
          lastLevelUpdateSummary = summaryState.value,
          onSetLevelById = { numId, targetLvl ->
            updatedId = numId
            updatedLevel = targetLvl
            summaryState.value = com.example.data.model.AdminLevelUpdateSummary(
              targetUid = "uid_target",
              numericId = numId,
              displayName = "रोहन",
              previousLevel = 80,
              newLevel = targetLvl,
              newHp = SwarUserIdentity.requiredHpForLevel(targetLvl),
            )
          },
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("audit_coin_id_input").performScrollTo().performTextReplacement(targetNumericId)
    composeTestRule.onNodeWithTag("audit_level_input").performScrollTo().performTextReplacement("120")
    composeTestRule.onNodeWithTag("audit_level_set_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(targetNumericId, updatedId)
    assertEquals(120, updatedLevel)
    composeTestRule.onNodeWithTag("audit_level_status_banner").assertIsDisplayed()

    // Now reduce by 5 (120 -> 115) and apply
    composeTestRule.onNodeWithTag("audit_level_dec_5").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("audit_level_set_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(115, updatedLevel)
  }

  @Test
  fun `voice room renders enlarged emoji overlay over seated user without breaking seat layout`() {
    val room = com.example.data.model.VoiceRoom(
      id = "room_emoji_size",
      ownerId = "host_1",
      ownerName = "होस्ट",
      title = "इमोजी टेस्ट रूम",
      roomCode = "778899",
      seatCount = 6L,
    )
    val seats = listOf(
      com.example.data.model.RoomSeat(
        id = "seat_0",
        roomId = "room_emoji_size",
        seatIndex = 0L,
        occupantUid = "host_1",
        occupantName = "होस्ट",
        isMuted = false,
      ),
    )
    val emojiMsg = com.example.data.model.RoomMessage(
      id = "msg_emoji_big",
      roomId = "room_emoji_size",
      senderUid = "host_1",
      senderName = "होस्ट",
      text = "🥳 पार्टी",
      messageType = "emoji",
      createdAt = null,
    )

    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.screens.VoiceRoomScreen(
          room = room,
          seats = seats,
          messages = listOf(emojiMsg),
          currentUserId = "viewer_1",
          isAdminUser = false,
          isUploadingMedia = false,
          userCoins = 1000L,
          emojiCatalog = com.example.data.model.SwarCatalogDefaults.builtInEmojis,
          giftCatalog = emptyList(),
          latestMarqueeGift = null,
          activeFullScreenGift = null,
          isMicMuted = true,
          isSpeakerOn = true,
          isMonitorLoopbackOn = false,
          micAudioLevel = 0f,
          onLeaveRoom = {},
          onSeatTapped = { _, _, _ -> },
          onLeaveSeat = {},
          onToggleSeatLock = {},
          onToggleMic = { _, _ -> },
          onAudioPermissionGranted = {},
          onToggleSpeaker = {},
          onToggleMonitorLoopback = {},
          onSendMessage = { _, _ -> },
          onSendEmoji = {},
          onAddCustomEmoji = { _, _, _, _, _, _ -> },
          onUpdateCustomEmoji = { _, _, _, _, _, _, _, _ -> },
          onDeleteCustomEmoji = { _, _ -> },
          onSendGift = { _, _, _, _ -> },
          onAddCustomGift = { _, _, _, _, _, _, _, _ -> },
          onUpdateCustomGift = { _, _, _, _, _, _, _, _, _, _, _ -> },
          onDeleteCustomGift = { _, _ -> },
          onDismissFullScreenGift = {},
          onClaimBonusCoins = {},
          onUpdateRoomSettings = { _, _, _ -> },
          onStartDirectChat = { _, _, _ -> },
          onShowToast = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("seat_emoji_overlay", useUnmergedTree = true).assertIsDisplayed()
  }
}

