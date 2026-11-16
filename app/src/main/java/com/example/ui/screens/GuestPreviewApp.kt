package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.GiftEvent
import com.example.data.model.RoomMessage
import com.example.data.model.RoomSeat
import com.example.data.model.SwarCatalogDefaults
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.CreateMomentDialog
import com.example.ui.components.CreateRoomDialog
import com.example.ui.components.MinimizedRoomFloatingBar
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarDeepNight
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

private enum class BrowseTab {
  ROOMS,
  MOMENTS,
  MESSAGES,
  PROFILE,
}

private const val PREVIEW_GUEST_UID = "preview_guest"

@Composable
fun GuestPreviewSwarRoomApp(
  isSupabaseConfigured: Boolean,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) {
    runCatching { CredentialManager.create(context) }.getOrNull()
  }
  val snackbarHostState = remember { SnackbarHostState() }

  var isSigningIn by remember { mutableStateOf(false) }
  var currentTab by remember { mutableStateOf(BrowseTab.ROOMS) }
  var showCreateRoomDialog by remember { mutableStateOf(false) }
  var showCreateMomentDialog by remember { mutableStateOf(false) }

  // Local UI-inspection state (zero Firestore queries while unauthenticated)
  var createdPreviewRoom by remember { mutableStateOf<VoiceRoom?>(null) }
  var savedPreviewSeats by remember { mutableStateOf<List<RoomSeat>>(emptyList()) }
  var savedPreviewMessages by remember { mutableStateOf<List<RoomMessage>>(emptyList()) }
  var previewRoom by remember { mutableStateOf<VoiceRoom?>(null) }
  var previewSeats by remember { mutableStateOf<List<RoomSeat>>(emptyList()) }
  var previewMessages by remember { mutableStateOf<List<RoomMessage>>(emptyList()) }
  var previewCoins by remember { mutableLongStateOf(1500L) }
  var latestMarqueeGift by remember { mutableStateOf<GiftEvent?>(null) }
  var activeFullScreenGift by remember { mutableStateOf<GiftEvent?>(null) }
  var isMicMuted by remember { mutableStateOf(true) }
  var isSpeakerOn by remember { mutableStateOf(true) }
  var isRoomMinimized by remember { mutableStateOf(false) }
  var msgSeq by remember { mutableIntStateOf(0) }
  var blockedPreviewUids by remember { mutableStateOf(emptySet<String>()) }

  val guestProfile = remember(previewCoins) {
    UserProfile(
      id = PREVIEW_GUEST_UID,
      userId = PREVIEW_GUEST_UID,
      displayName = "अतिथि (प्रीव्यू)",
      handle = "swar_preview",
      bio = "बिना लॉगिन ऐप लेआउट और पैनल प्रीव्यू मोड।",
      clubTag = "SWAR",
      coins = previewCoins,
    )
  }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {},
      onUnauthenticated = {},
      scope = scope,
    )
  }

  fun triggerGoogleSignIn() {
    if (isSigningIn) return
    isSigningIn = true
    onGoogleSignInClicked(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = { isSigningIn = false },
      onAuthError = { msg ->
        isSigningIn = false
        scope.launch { snackbarHostState.showSnackbar(msg) }
      },
      scope = scope,
      onAuthCancelled = { isSigningIn = false },
    )
  }

  fun openStagePreview(
    title: String = "वॉइस रूम प्रीव्यू स्टेज",
    category: String = "लोकप्रिय",
    theme: String = "stadium",
    clubTag: String = "SWAR",
    welcomeNotice: String = "SwarRoom प्रीव्यू स्टेज में स्वागत है! सीट, इमोजी और गिफ्ट पैनल यहां से देखें।",
    seatCount: Int = 22,
    coverUrl: String = "",
    persistAsCreatedRoom: Boolean = false,
  ) {
    val roomId = if (persistAsCreatedRoom) "preview_my_room" else "preview_room"
    val room = VoiceRoom(
      id = roomId,
      ownerId = PREVIEW_GUEST_UID,
      ownerName = guestProfile.displayName,
      title = title,
      roomCode = "100100",
      category = category,
      coverUrl = coverUrl,
      backgroundTheme = theme,
      clubTag = clubTag.ifBlank { "SWAR" },
      welcomeNotice = welcomeNotice,
      seatCount = seatCount.toLong(),
      listenerCount = 1L,
      isLive = true,
      isPublic = true,
    )
    val initialSeats = List(seatCount) { index ->
      RoomSeat(
        id = "seat_$index",
        roomId = roomId,
        roomOwnerId = PREVIEW_GUEST_UID,
        seatIndex = index.toLong(),
        occupantUid = if (index == 0) PREVIEW_GUEST_UID else "",
        occupantName = if (index == 0) guestProfile.displayName else "",
        isMuted = true,
      )
    }
    if (persistAsCreatedRoom) {
      createdPreviewRoom = room
      savedPreviewSeats = initialSeats
      savedPreviewMessages = emptyList()
    }
    previewRoom = room
    previewSeats = initialSeats
    previewMessages = emptyList()
    isMicMuted = true
    isRoomMinimized = false
  }

  fun enterExistingCreatedPreviewRoom() {
    val existing = createdPreviewRoom ?: return
    previewRoom = existing
    previewSeats = savedPreviewSeats
    previewMessages = savedPreviewMessages
    isMicMuted = true
    isRoomMinimized = false
  }

  fun exitPreviewRoomCompletely(active: VoiceRoom) {
    if (active.id == createdPreviewRoom?.id) {
      createdPreviewRoom = active
      savedPreviewSeats = previewSeats
      savedPreviewMessages = previewMessages
    }
    isRoomMinimized = false
    previewRoom = null
    previewSeats = emptyList()
    previewMessages = emptyList()
    activeFullScreenGift = null
  }

  val activePreviewRoom = previewRoom
  if (activePreviewRoom != null && !isRoomMinimized) {
    Box(modifier = Modifier.fillMaxSize()) {
      VoiceRoomScreen(
        room = activePreviewRoom,
        seats = previewSeats,
        messages = previewMessages,
        currentUserId = PREVIEW_GUEST_UID,
        isAdminUser = false,
        isUploadingMedia = false,
        userCoins = previewCoins,
        emojiCatalog = SwarCatalogDefaults.builtInEmojis,
        giftCatalog = SwarCatalogDefaults.builtInGifts,
        latestMarqueeGift = latestMarqueeGift,
        activeFullScreenGift = activeFullScreenGift,
        isMicMuted = isMicMuted,
        isSpeakerOn = isSpeakerOn,
        isMonitorLoopbackOn = false,
        micAudioLevel = 0f,
        onLeaveRoom = { exitPreviewRoomCompletely(activePreviewRoom) },
        onSeatTapped = { seat, _, _ ->
          if (seat.isLocked) {
            scope.launch { snackbarHostState.showSnackbar("यह सीट लॉक है।") }
          } else if (seat.occupantUid == PREVIEW_GUEST_UID) {
            isMicMuted = !isMicMuted
            previewSeats = previewSeats.map {
              if (it.seatIndex == seat.seatIndex) it.copy(isMuted = isMicMuted) else it
            }
          } else if (seat.occupantUid.isBlank()) {
            previewSeats = previewSeats.map {
              when {
                it.occupantUid == PREVIEW_GUEST_UID -> it.copy(occupantUid = "", occupantName = "", isMuted = false)
                it.seatIndex == seat.seatIndex -> it.copy(
                  occupantUid = PREVIEW_GUEST_UID,
                  occupantName = guestProfile.displayName,
                  isMuted = isMicMuted,
                )
                else -> it
              }
            }
          }
        },
        onLeaveSeat = {
          isMicMuted = true
          previewSeats = previewSeats.map {
            if (it.occupantUid == PREVIEW_GUEST_UID) it.copy(occupantUid = "", occupantName = "", isMuted = false)
            else it
          }
        },
        onToggleSeatLock = { seat ->
          val nextLock = !seat.isLocked
          previewSeats = previewSeats.map {
            if (it.seatIndex == seat.seatIndex) {
              if (nextLock) it.copy(isLocked = true, occupantUid = "", occupantName = "")
              else it.copy(isLocked = false)
            } else it
          }
        },
        onToggleMic = { _, _ ->
          val onSeat = previewSeats.any { it.occupantUid == PREVIEW_GUEST_UID }
          if (!onSeat) {
            scope.launch { snackbarHostState.showSnackbar("बोलने के लिए पहले किसी खाली सीट पर बैठें।") }
          } else {
            isMicMuted = !isMicMuted
            previewSeats = previewSeats.map {
              if (it.occupantUid == PREVIEW_GUEST_UID) it.copy(isMuted = isMicMuted) else it
            }
          }
        },
        onAudioPermissionGranted = { isMicMuted = false },
        onToggleSpeaker = { isSpeakerOn = !isSpeakerOn },
        onToggleMonitorLoopback = {
          scope.launch { snackbarHostState.showSnackbar("माइक मॉनिटर क्लाउड/लाइव रूम में उपलब्ध है।") }
        },
        onSendMessage = { text, type ->
          msgSeq++
          previewMessages = previewMessages + RoomMessage(
            id = "pmsg_$msgSeq",
            roomId = activePreviewRoom.id,
            senderUid = PREVIEW_GUEST_UID,
            senderName = guestProfile.displayName,
            text = text,
            messageType = type,
            badgeTag = guestProfile.clubTag,
          )
        },
        onSendEmoji = { item ->
          val display = "${item.symbol} ${item.label}".trim()
          msgSeq++
          previewMessages = previewMessages + RoomMessage(
            id = "pmsg_$msgSeq",
            roomId = activePreviewRoom.id,
            senderUid = PREVIEW_GUEST_UID,
            senderName = guestProfile.displayName,
            text = display,
            messageType = "emoji",
            badgeTag = guestProfile.clubTag,
          )
        },
        onAddCustomEmoji = { _, _, _, _, onDone ->
          onDone()
          scope.launch { snackbarHostState.showSnackbar("क्लाउड पर अपलोड करने के लिए Google साइन इन करें।") }
        },
        onUpdateCustomEmoji = { _, _, _, _, _, _, onDone -> onDone() },
        onDeleteCustomEmoji = { _, onDone -> onDone() },
        onSendGift = { gift, recipientUid, recipientName, quantity ->
          val totalCost = gift.coinCost * quantity
          if (previewCoins < totalCost) {
            scope.launch { snackbarHostState.showSnackbar("कॉइंस कम हैं! '+' पर टैप कर बोनस कॉइंस लें।") }
          } else {
            previewCoins -= totalCost
            val evt = GiftEvent(
              id = "pevt_${System.currentTimeMillis()}",
              roomId = activePreviewRoom.id,
              roomTitle = activePreviewRoom.title,
              senderUid = PREVIEW_GUEST_UID,
              senderName = guestProfile.displayName,
              recipientUid = recipientUid,
              recipientName = recipientName,
              giftId = gift.id,
              giftName = gift.name,
              giftEmoji = gift.emoji,
              giftMediaUrl = gift.mediaUrl,
              giftMediaType = gift.mediaType,
              coinCost = totalCost,
              quantity = quantity.toLong(),
            )
            activeFullScreenGift = evt
            msgSeq++
            val qtySuffix = if (quantity > 1) " x$quantity" else ""
            previewMessages = previewMessages + RoomMessage(
              id = "pmsg_$msgSeq",
              roomId = activePreviewRoom.id,
              senderUid = PREVIEW_GUEST_UID,
              senderName = guestProfile.displayName,
              text = "$recipientName को ${gift.emoji} ${gift.name}$qtySuffix भेजा!",
              messageType = "gift",
              badgeTag = "🎁 GIFT",
            )
          }
        },
        onAddCustomGift = { _, _, _, _, _, _, onDone ->
          onDone()
          scope.launch { snackbarHostState.showSnackbar("क्लाउड पर अपलोड करने के लिए Google साइन इन करें।") }
        },
        onUpdateCustomGift = { _, _, _, _, _, _, _, _, _, onDone -> onDone() },
        onDeleteCustomGift = { _, onDone -> onDone() },
        onDismissFullScreenGift = {
          val finished = activeFullScreenGift
          activeFullScreenGift = null
          if (finished != null) {
            latestMarqueeGift = finished
          }
        },
        onClearMarqueeGift = { eventId ->
          if (latestMarqueeGift?.id == eventId) {
            latestMarqueeGift = null
          }
        },
        onClaimBonusCoins = { previewCoins += 500L },
        onUpdateRoomSettings = { newTitle, newTheme, newNotice ->
          previewRoom = activePreviewRoom.copy(
            title = newTitle.ifBlank { activePreviewRoom.title },
            backgroundTheme = newTheme,
            welcomeNotice = newNotice,
          )
        },
        onStartDirectChat = { _, _, _ ->
          scope.launch { snackbarHostState.showSnackbar("निजी संदेश भेजने के लिए Google साइन इन करें।") }
        },
        onShowToast = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
        currentUserName = guestProfile.displayName,
        currentUserAvatar = guestProfile.avatarUrl,
        onToggleSeatMute = { seat, _, _ ->
          if (seat.occupantUid == PREVIEW_GUEST_UID) {
            isMicMuted = !isMicMuted
            previewSeats = previewSeats.map {
              if (it.seatIndex == seat.seatIndex) it.copy(isMuted = isMicMuted) else it
            }
          } else {
            previewSeats = previewSeats.map {
              if (it.seatIndex == seat.seatIndex) it.copy(isMuted = !it.isMuted) else it
            }
          }
        },
        onMinimizeRoom = { isRoomMinimized = true },
        blockedUserIds = blockedPreviewUids,
        onBlockUser = { uid, name ->
          blockedPreviewUids = blockedPreviewUids + uid
          previewSeats = previewSeats.map {
            if (it.occupantUid == uid) it.copy(occupantUid = "", occupantName = "", isMuted = false)
            else it
          }
          scope.launch { snackbarHostState.showSnackbar("$name को ब्लॉक किया गया।") }
        },
        onUnblockUser = { uid, name ->
          blockedPreviewUids = blockedPreviewUids - uid
          scope.launch { snackbarHostState.showSnackbar("$name को अनब्लॉक किया गया।") }
        },
        onKickSeatOccupant = { seat ->
          previewSeats = previewSeats.map {
            if (it.seatIndex == seat.seatIndex) it.copy(occupantUid = "", occupantName = "", isMuted = false)
            else it
          }
          scope.launch { snackbarHostState.showSnackbar("${seat.occupantName.ifBlank { "सदस्य" }} को सीट से किक आउट किया गया।") }
        },
        onSitOnAvailableSeat = { _, _ ->
          val openSeat = previewSeats.firstOrNull { it.occupantUid.isBlank() && !it.isLocked }
          if (openSeat == null) {
            scope.launch { snackbarHostState.showSnackbar("अभी कोई खाली सीट उपलब्ध नहीं है।") }
          } else {
            previewSeats = previewSeats.map {
              when {
                it.occupantUid == PREVIEW_GUEST_UID -> it.copy(occupantUid = "", occupantName = "", isMuted = false)
                it.seatIndex == openSeat.seatIndex -> it.copy(
                  occupantUid = PREVIEW_GUEST_UID,
                  occupantName = guestProfile.displayName,
                  isMuted = isMicMuted,
                )
                else -> it
              }
            }
          }
        },
      )
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 64.dp),
      )
    }
    return
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      Surface(
        color = SwarDeepNight,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "प्रीव्यू मोड — सभी स्क्रीन देखें",
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "क्लाउड रूम, चैट व अपलोड के लिए Google साइन इन करें",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFC2BBE0),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          TextButton(
            onClick = ::triggerGoogleSignIn,
            enabled = !isSigningIn,
            colors = ButtonDefaults.textButtonColors(
              containerColor = SwarCyan,
              contentColor = SwarDeepNight,
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.testTag("browse_mode_sign_in_button"),
          ) {
            if (isSigningIn) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SwarDeepNight)
            } else {
              Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("साइन इन", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
          }
        }
      }
    },
    bottomBar = {
      NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
          selected = currentTab == BrowseTab.ROOMS,
          onClick = { currentTab = BrowseTab.ROOMS },
          icon = {
            Icon(
              imageVector = if (currentTab == BrowseTab.ROOMS) Icons.Filled.Home else Icons.Outlined.Home,
              contentDescription = null,
            )
          },
          label = { Text(stringResource(id = R.string.nav_rooms)) },
          modifier = Modifier.testTag("nav_tab_rooms"),
        )
        NavigationBarItem(
          selected = currentTab == BrowseTab.MOMENTS,
          onClick = { currentTab = BrowseTab.MOMENTS },
          icon = {
            Icon(
              imageVector = if (currentTab == BrowseTab.MOMENTS) Icons.Filled.Explore else Icons.Outlined.Explore,
              contentDescription = null,
            )
          },
          label = { Text(stringResource(id = R.string.nav_moments)) },
          modifier = Modifier.testTag("nav_tab_moments"),
        )
        NavigationBarItem(
          selected = currentTab == BrowseTab.MESSAGES,
          onClick = { currentTab = BrowseTab.MESSAGES },
          icon = {
            Icon(
              imageVector = if (currentTab == BrowseTab.MESSAGES) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
              contentDescription = null,
            )
          },
          label = { Text(stringResource(id = R.string.nav_messages)) },
          modifier = Modifier.testTag("nav_tab_messages"),
        )
        NavigationBarItem(
          selected = currentTab == BrowseTab.PROFILE,
          onClick = { currentTab = BrowseTab.PROFILE },
          icon = {
            Icon(
              imageVector = if (currentTab == BrowseTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
              contentDescription = null,
            )
          },
          label = { Text(stringResource(id = R.string.nav_profile)) },
          modifier = Modifier.testTag("nav_tab_profile"),
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      when (currentTab) {
        BrowseTab.ROOMS -> LobbyScreen(
          roomsState = UiState.Success(listOfNotNull(createdPreviewRoom)),
          currentUserId = PREVIEW_GUEST_UID,
          onRoomClick = { clicked ->
            if (clicked.id == createdPreviewRoom?.id) {
              enterExistingCreatedPreviewRoom()
            } else {
              openStagePreview(title = clicked.title, coverUrl = clicked.coverUrl)
            }
          },
          onCreateRoomClick = {
            if (createdPreviewRoom != null) {
              enterExistingCreatedPreviewRoom()
            } else {
              showCreateRoomDialog = true
            }
          },
          onOpenStagePreview = { openStagePreview() },
        )
        BrowseTab.MOMENTS -> MomentsScreen(
          momentsState = UiState.Success(emptyList()),
          currentUserId = PREVIEW_GUEST_UID,
          isSupabaseConfigured = isSupabaseConfigured,
          onAddMomentClick = { showCreateMomentDialog = true },
          onLikeMoment = {},
          onDeleteMoment = {},
        )
        BrowseTab.MESSAGES -> MessagesScreen(
          conversationsState = UiState.Success(emptyList()),
          activeConversationId = null,
          directMessages = emptyList(),
          currentUserId = PREVIEW_GUEST_UID,
          onOpenConversation = {},
          onCloseConversation = {},
          onSendDirectMessage = { _, _ -> },
        )
        BrowseTab.PROFILE -> ProfileScreen(
          profileState = UiState.Success(guestProfile),
          isUploadingMedia = false,
          isSupabaseConfigured = isSupabaseConfigured,
          onUploadAvatarDp = {
            scope.launch {
              snackbarHostState.showSnackbar("DP क्लाउड पर सेव करने के लिए Google से साइन इन करें।")
            }
          },
          onUpdateProfile = { _, _, _, _, _ ->
            scope.launch {
              snackbarHostState.showSnackbar("प्रोफ़ाइल सेव करने के लिए Google से साइन इन करें।")
            }
          },
          onSignOut = {},
          isSignedIn = false,
          isSigningIn = isSigningIn,
          onSignInClick = ::triggerGoogleSignIn,
        )
      }

      if (activePreviewRoom != null && isRoomMinimized) {
        MinimizedRoomFloatingBar(
          room = activePreviewRoom,
          isMicMuted = isMicMuted,
          onExpand = { isRoomMinimized = false },
          onToggleMic = {
            val onSeat = previewSeats.any { it.occupantUid == PREVIEW_GUEST_UID }
            if (!onSeat) {
              scope.launch { snackbarHostState.showSnackbar("बोलने के लिए पहले किसी खाली सीट पर बैठें।") }
            } else {
              isMicMuted = !isMicMuted
              previewSeats = previewSeats.map {
                if (it.occupantUid == PREVIEW_GUEST_UID) it.copy(isMuted = isMicMuted) else it
              }
            }
          },
          onExitRoom = { exitPreviewRoomCompletely(activePreviewRoom) },
          modifier = Modifier.align(Alignment.BottomCenter),
        )
      }
    }
  }

  if (showCreateRoomDialog && createdPreviewRoom == null) {
    CreateRoomDialog(
      isUploading = false,
      onDismiss = { showCreateRoomDialog = false },
      onCreate = { title, category, theme, clubTag, welcome, seatsCount, coverUri ->
        showCreateRoomDialog = false
        openStagePreview(
          title = title,
          category = category,
          theme = theme,
          clubTag = clubTag,
          welcomeNotice = welcome,
          seatCount = seatsCount,
          coverUrl = coverUri?.toString().orEmpty(),
          persistAsCreatedRoom = true,
        )
        scope.launch {
          snackbarHostState.showSnackbar("प्रीव्यू रूम खुला — ऑनलाइन पब्लिश करने के लिए साइन इन करें।")
        }
      },
    )
  }

  if (showCreateMomentDialog) {
    CreateMomentDialog(
      isUploading = false,
      onDismiss = { showCreateMomentDialog = false },
      onSubmit = { _, _, _ ->
        showCreateMomentDialog = false
        scope.launch {
          snackbarHostState.showSnackbar("मोमेंट अपलोड करने के लिए ऊपर 'साइन इन' बटन से Google साइन इन करें।")
        }
      },
    )
  }
}
