package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SwarUserIdentity
import com.example.data.model.UserActivityLog
import com.example.data.model.UserAuditProfile
import com.example.data.repository.AuditDateLocationHelper
import com.example.data.repository.MediaCompressor
import com.example.ui.theme.SwarRoomTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
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
          onSaveLevelTierConfig = { lvl, _, _, _, _, coinsReward, _, _, onDone ->
            savedBadgeLevel = lvl
            savedCoins = coinsReward
            onDone()
          },
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_unlocked_badges_line").assertIsDisplayed()
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
    composeTestRule.onNodeWithTag("save_level_config_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, savedBadgeLevel)
    assertEquals(0L, savedCoins)
    composeTestRule.onNodeWithTag("close_level_cards_button").performClick()
    composeTestRule.waitForIdle()
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
  fun `first time onboarding gates submission on DOB age and conduct checkbox`() {
    var submittedDob = ""
    var submittedTerms = false
    composeTestRule.setContent {
      SwarRoomTheme {
        com.example.ui.components.FirstTimeOnboardingScreen(
          userId = "u_onboard_1",
          userEmail = "singer1@gmail.com",
          displayName = "रवि शर्मा",
          avatarUrl = "",
          isSubmitting = false,
          onSubmitOnboarding = { dobIso, _, terms ->
            submittedDob = dobIso
            submittedTerms = terms
          },
          onSignOut = {},
          initialDobIso = "2000-05-10",
          autoCaptureLocation = false,
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_dob_value", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_submit_button").performScrollTo().assertIsNotEnabled()
    composeTestRule.onNodeWithTag("onboarding_conduct_checkbox").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_submit_button").performScrollTo().assertIsEnabled().performClick()
    composeTestRule.waitForIdle()
    assertEquals("2000-05-10", submittedDob)
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
}
