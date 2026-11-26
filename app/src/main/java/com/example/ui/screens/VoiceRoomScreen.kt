package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.audio.DeviceMusicTrack
import com.example.audio.RoomMusicState
import com.example.data.model.CpRelationship
import com.example.data.model.EmojiCatalogItem
import com.example.data.model.GiftCatalogItem
import com.example.data.model.GiftEvent
import com.example.data.model.LevelTierConfig
import com.example.data.model.RocketLaunchEvent
import com.example.data.model.RoomKickLog
import com.example.data.model.RoomMessage
import com.example.data.model.RoomPresenceMember
import com.example.data.model.RoomRocketState
import com.example.data.model.RoomSeat
import com.example.data.model.SwarUserIdentity
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
import com.example.ui.components.AdjacentCpFloatingCrest
import com.example.ui.components.AdjacentCpSeatBridgeCanvas
import com.example.ui.components.CrossRoomGiftMarqueeStrip
import com.example.ui.components.EmojiPanelSheet
import com.example.ui.components.FramedUserAvatar
import com.example.ui.components.FullScreenGiftOverlay
import com.example.ui.components.FullScreenRocketLaunchOverlay
import com.example.ui.components.GiftPanelSheet
import com.example.ui.components.JointCpProfileCard
import com.example.ui.components.LevelCardsShowcaseDialog
import com.example.ui.components.OrnateWingedLevelCrest
import com.example.ui.components.ProfileSideBadgesShowcaseCard
import com.example.ui.components.RocketDetailsDialog
import com.example.ui.components.RoomRocketCompactWidget
import com.example.ui.components.RoomSettingsSheet
import com.example.ui.components.TypingBannerChatBubble
import com.example.ui.components.UserAvatar
import androidx.compose.material.icons.filled.Favorite
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarMint
import com.example.ui.theme.SwarViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ActiveSeatEmoji(
  val messageId: String,
  val symbol: String,
  val imageUrl: String,
)

private data class RoomProfileTarget(
  val uid: String,
  val displayName: String,
  val avatarUrl: String,
  val seat: RoomSeat? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRoomScreen(
  room: VoiceRoom,
  seats: List<RoomSeat>,
  messages: List<RoomMessage>,
  currentUserId: String,
  isAdminUser: Boolean,
  isUploadingMedia: Boolean,
  userCoins: Long,
  emojiCatalog: List<EmojiCatalogItem>,
  giftCatalog: List<GiftCatalogItem>,
  latestMarqueeGift: GiftEvent?,
  activeFullScreenGift: GiftEvent?,
  isMicMuted: Boolean,
  isSpeakerOn: Boolean,
  isMonitorLoopbackOn: Boolean,
  micAudioLevel: Float,
  onLeaveRoom: () -> Unit,
  onSeatTapped: (RoomSeat, Boolean, () -> Unit) -> Unit,
  onLeaveSeat: () -> Unit,
  onToggleSeatLock: (RoomSeat) -> Unit,
  onToggleMic: (Boolean, () -> Unit) -> Unit,
  onAudioPermissionGranted: () -> Unit,
  onToggleSpeaker: () -> Unit,
  onToggleMonitorLoopback: () -> Unit,
  onSendMessage: (String, String) -> Unit,
  onSendEmoji: (EmojiCatalogItem) -> Unit,
  onAddCustomEmoji: (String, String, String, Uri?, String, () -> Unit) -> Unit,
  onUpdateCustomEmoji: (EmojiCatalogItem, String, String, String, Uri?, String, Boolean, () -> Unit) -> Unit,
  onDeleteCustomEmoji: (EmojiCatalogItem, () -> Unit) -> Unit,
  onSendGift: (GiftCatalogItem, String, String, Int) -> Unit,
  onAddCustomGift: (String, String, String, Long, Uri?, String, String, () -> Unit) -> Unit,
  onUpdateCustomGift: (GiftCatalogItem, String, String, String, Long, Uri?, String, String, Boolean, Boolean, () -> Unit) -> Unit,
  onDeleteCustomGift: (GiftCatalogItem, () -> Unit) -> Unit,
  onDismissFullScreenGift: () -> Unit,
  onClearMarqueeGift: (String) -> Unit = {},
  onClaimBonusCoins: () -> Unit,
  onUpdateRoomSettings: (String, String, String) -> Unit,
  onStartDirectChat: (uid: String, name: String, avatar: String) -> Unit,
  onShowToast: (String) -> Unit,
  currentUserName: String = "",
  currentUserAvatar: String = "",
  onToggleSeatMute: (RoomSeat, Boolean, () -> Unit) -> Unit = { _, _, _ -> },
  onMinimizeRoom: () -> Unit = {},
  userProfiles: Map<String, UserProfile> = emptyMap(),
  blockedUserIds: Set<String> = emptySet(),
  onRequestUserProfile: (String) -> Unit = {},
  onBlockUser: (uid: String, name: String) -> Unit = { _, _ -> },
  onUnblockUser: (uid: String, name: String) -> Unit = { _, _ -> },
  onKickSeatOccupant: (RoomSeat) -> Unit = { seat -> onSeatTapped(seat, true) {} },
  onSitOnAvailableSeat: (Boolean, () -> Unit) -> Unit = { hasPerm, requestPerm ->
    val open = seats.firstOrNull { it.occupantUid.isBlank() && !it.isLocked }
    if (open != null) {
      onSeatTapped(open, hasPerm, requestPerm)
    } else {
      onShowToast("अभी कोई खाली सीट उपलब्ध नहीं है।")
    }
  },
  isAgoraConnected: Boolean = false,
  agoraStatusText: String = "",
  giftUploadError: String? = null,
  onClearGiftUploadError: () -> Unit = {},
  emojiUploadError: String? = null,
  onClearEmojiUploadError: () -> Unit = {},
  levelConfigs: Map<Int, LevelTierConfig> = emptyMap(),
  claimedLevelTiers: Set<Int> = emptySet(),
  onSaveLevelTierConfig: (
    badgeLevel: Int,
    entryVideoLink: String,
    entryVideoUri: Uri?,
    frameLink: String,
    frameUri: Uri?,
    rewardCoins: Long,
    badgeLink: String,
    badgeUri: Uri?,
    typingBannerLink: String,
    typingBannerUri: Uri?,
    onDone: () -> Unit,
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _, onDone -> onDone() },
  onClaimLevelTierReward: (Int) -> Unit = {},
  livePresenceMembers: List<RoomPresenceMember> = emptyList(),
  kickLogs: List<RoomKickLog> = emptyList(),
  roomMusicState: RoomMusicState = RoomMusicState(),
  scannedMusicTracks: List<DeviceMusicTrack> = emptyList(),
  isScanningMusic: Boolean = false,
  onUpdateRoomIdentity: (title: String, welcomeNotice: String, newCoverUri: Uri?, newCoverLink: String) -> Unit = { title, notice, _, _ ->
    onUpdateRoomSettings(title, room.backgroundTheme, notice)
  },
  onUpdateRoomTheme: (backgroundTheme: String, customThemeUri: Uri?, customThemeLink: String) -> Unit = { theme, _, _ ->
    onUpdateRoomSettings(room.title, theme, room.welcomeNotice)
  },
  onAddRoomManagerById: (String) -> Unit = {},
  onRemoveRoomManager: (String) -> Unit = {},
  onUpdateRoomPinLock: (Boolean, String) -> Unit = { _, _ -> },
  onResizeRoomSeats: (Int) -> Unit = {},
  onPickRoomMusic: (Uri) -> Unit = {},
  onToggleRoomMusicPlayPause: () -> Unit = {},
  onStopRoomMusic: () -> Unit = {},
  onRoomMusicVolumeChange: (Int) -> Unit = {},
  onScanMemoryCardMusic: () -> Unit = {},
  onSelectMusicFolderTree: (Uri) -> Unit = {},
  onPlayScannedMusicTrack: (DeviceMusicTrack) -> Unit = { track -> onPickRoomMusic(track.uri) },
  onPlayNextMusic: () -> Unit = {},
  onPlayPreviousMusic: () -> Unit = {},
  onKickRoomMember: (targetUid: String, targetName: String, targetAvatarUrl: String) -> Unit = { uid, _, _ ->
    seats.firstOrNull { it.occupantUid == uid }?.let { onKickSeatOccupant(it) }
  },
  acceptedCpLinks: List<CpRelationship> = emptyList(),
  onSendCpRequestById: (recipientNumericId: String, bondTitle: String) -> Unit = { _, _ -> },
  userHp: Long = userCoins,
  roomRocketState: RoomRocketState? = null,
  activeRocketLaunch: RocketLaunchEvent? = null,
  onDismissRocketLaunch: () -> Unit = {},
) {
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val coroutineScope = rememberCoroutineScope()
  var showExitOptionsDialog by remember { mutableStateOf(false) }

  BackHandler(enabled = drawerState.isOpen) {
    coroutineScope.launch { drawerState.close() }
  }
  BackHandler(enabled = !drawerState.isOpen) {
    showExitOptionsDialog = true
  }

  val context = LocalContext.current
  val hasAudioPermission = remember(context) {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  val audioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { granted ->
      hasAudioPermission.value = granted
      if (granted) {
        onAudioPermissionGranted()
      } else {
        onShowToast(context.getString(R.string.mic_permission_needed))
      }
    },
  )

  var showComposer by remember { mutableStateOf(false) }
  var composerText by remember { mutableStateOf("") }
  var showEmojiPanel by remember { mutableStateOf(false) }
  var showGiftPanel by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showRocketDetailsDialog by remember { mutableStateOf(false) }
  var selectedSeatForSheet by remember { mutableStateOf<RoomSeat?>(null) }
  var selectedProfileTarget by remember { mutableStateOf<RoomProfileTarget?>(null) }
  val effectiveOwnHp = maxOf(userHp, userCoins)

  val visibleMessages = remember(messages, blockedUserIds) {
    if (blockedUserIds.isEmpty()) messages
    else messages.filter { it.senderUid !in blockedUserIds }
  }

  val isHost = room.ownerId == currentUserId
  val isManager = currentUserId in room.managerUids
  val isModerator = isHost || isManager
  val configuredSeatCount = room.seatCount.toInt().coerceIn(1, 22)
  val visibleSeats = remember(seats, configuredSeatCount) {
    seats.filter { it.seatIndex in 0L until configuredSeatCount.toLong() }.sortedBy { it.seatIndex }
  }
  val topSeats = remember(visibleSeats, configuredSeatCount, room) {
    val indices = if (configuredSeatCount >= 2) listOf(0L, 1L) else listOf(0L)
    indices.map { idx ->
      visibleSeats.firstOrNull { it.seatIndex == idx } ?: RoomSeat(
        id = "seat_$idx",
        roomId = room.id,
        roomOwnerId = room.ownerId,
        isRoomPublic = room.isPublic,
        seatIndex = idx,
      )
    }
  }
  val guestSeats = remember(visibleSeats) { visibleSeats.filter { it.seatIndex >= 2L } }
  val seatedMembers = remember(visibleSeats) { visibleSeats.filter { it.occupantUid.isNotBlank() } }
  LaunchedEffect(seatedMembers) {
    seatedMembers.forEach { seat ->
      val uid = seat.occupantUid
      if (uid.isNotBlank() && uid != currentUserId && uid !in userProfiles) {
        onRequestUserProfile(uid)
      }
    }
  }
  val seatedUids = remember(seatedMembers) { seatedMembers.map { it.occupantUid }.toSet() }
  val totalSeatCount = configuredSeatCount
  val isCurrentUserSeated = currentUserId in seatedUids
  val hostView = LocalView.current
  DisposableEffect(hostView, isCurrentUserSeated) {
    hostView.keepScreenOn = isCurrentUserSeated
    onDispose {
      hostView.keepScreenOn = false
    }
  }
  val liveAllUids = remember(seatedUids, livePresenceMembers, currentUserId, blockedUserIds) {
    buildSet {
      addAll(seatedUids.filter { it.isNotBlank() && it !in blockedUserIds })
      livePresenceMembers.forEach { member ->
        if (member.userId.isNotBlank() && member.userId !in blockedUserIds) {
          add(member.userId)
        }
      }
      if (currentUserId.isNotBlank()) add(currentUserId)
    }
  }
  val totalInRoom = liveAllUids.size.toLong().coerceAtLeast(1L)
  val audienceCount = (totalInRoom - seatedMembers.size).coerceAtLeast(0L)
  val liveListeners = remember(livePresenceMembers, seatedUids, currentUserId, blockedUserIds) {
    livePresenceMembers
      .filter {
        it.userId.isNotBlank() &&
          it.userId !in seatedUids &&
          it.userId != currentUserId &&
          it.userId !in blockedUserIds
      }
      .distinctBy { it.userId }
  }
  val listState = rememberLazyListState()
  val activeSeatReactions = remember(room.id) { mutableStateMapOf<String, ActiveSeatEmoji>() }
  val seenEmojiIds = remember(room.id) { mutableSetOf<String>() }
  var initialEmojiBatchChecked by remember(room.id) { mutableStateOf(false) }

  fun openSeatOrProfileSheet(seat: RoomSeat) {
    if (seat.occupantUid.isNotBlank()) {
      selectedProfileTarget = RoomProfileTarget(
        uid = seat.occupantUid,
        displayName = seat.occupantName,
        avatarUrl = seat.occupantAvatarUrl,
        seat = seat,
      )
    } else {
      selectedSeatForSheet = seat
    }
  }

  LaunchedEffect(visibleMessages.size) {
    if (visibleMessages.isNotEmpty()) {
      listState.animateScrollToItem(visibleMessages.lastIndex)
    }
  }

  LaunchedEffect(visibleMessages, emojiCatalog) {
    val emojiMessages = visibleMessages.filter { it.messageType == "emoji" && it.senderUid.isNotBlank() }
    val now = System.currentTimeMillis()
    if (!initialEmojiBatchChecked && visibleMessages.isNotEmpty()) {
      initialEmojiBatchChecked = true
      emojiMessages.forEach { msg ->
        val createdMs = msg.createdAt?.toDate()?.time
        val isJustSent = createdMs == null || (now - createdMs) <= 5000L
        if (!isJustSent) {
          seenEmojiIds.add(msg.id)
        }
      }
    }

    emojiMessages.forEach { msg ->
      val keyId = msg.id.ifBlank { "${msg.senderUid}_${msg.text}_${msg.createdAt?.seconds ?: 0L}" }
      if (seenEmojiIds.add(keyId)) {
        val createdMs = msg.createdAt?.toDate()?.time
        if (createdMs == null || (now - createdMs) <= 15000L) {
          val matchedItem = emojiCatalog.firstOrNull { item ->
            val expected = buildString {
              if (item.symbol.isNotBlank()) append(item.symbol).append(" ")
              append(item.label)
            }.trim()
            expected == msg.text || item.label == msg.text
          }
          val resolvedSymbol = matchedItem?.symbol?.ifBlank { "" }
            ?: msg.text.substringBefore(" ").ifBlank { "😊" }
          val resolvedImageUrl = matchedItem?.imageUrl.orEmpty()
          val reaction = ActiveSeatEmoji(
            messageId = keyId,
            symbol = resolvedSymbol,
            imageUrl = resolvedImageUrl,
          )
          activeSeatReactions[msg.senderUid] = reaction
          coroutineScope.launch {
            delay(2800L)
            if (activeSeatReactions[msg.senderUid]?.messageId == reaction.messageId) {
              activeSeatReactions.remove(msg.senderUid)
            }
          }
        }
      }
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      RoomParticipantsDrawerSheet(
        room = room,
        totalSeatCount = totalSeatCount,
        totalInRoom = totalInRoom,
        audienceCount = audienceCount,
        seatedMembers = seatedMembers,
        isCurrentUserSeated = isCurrentUserSeated,
        currentUserId = currentUserId,
        currentUserName = currentUserName,
        currentUserAvatar = currentUserAvatar,
        userCoins = effectiveOwnHp,
        userProfiles = userProfiles,
        liveListeners = liveListeners,
        onOwnProfileClick = {
          coroutineScope.launch { drawerState.close() }
          val mySeat = seatedMembers.firstOrNull { it.occupantUid == currentUserId }
          selectedProfileTarget = RoomProfileTarget(
            uid = currentUserId,
            displayName = currentUserName.ifBlank { "आप" },
            avatarUrl = currentUserAvatar,
            seat = mySeat,
          )
        },
        onSeatMemberClick = { seat ->
          coroutineScope.launch { drawerState.close() }
          openSeatOrProfileSheet(seat)
        },
        onListenerMemberClick = { uid, name, avatar ->
          coroutineScope.launch { drawerState.close() }
          selectedProfileTarget = RoomProfileTarget(
            uid = uid,
            displayName = name,
            avatarUrl = avatar,
            seat = seatedMembers.firstOrNull { it.occupantUid == uid },
          )
        },
        onOpenSettings = {
          coroutineScope.launch { drawerState.close() }
          showSettingsDialog = true
        },
        onLeaveRoom = {
          coroutineScope.launch { drawerState.close() }
          onLeaveRoom()
        },
        onClose = { coroutineScope.launch { drawerState.close() } },
      )
    },
  ) {
  Box(modifier = Modifier.fillMaxSize()) {
    // Full-bleed atmospheric background (Preset themes OR Host Custom Theme Background visible to everyone)
    when {
      room.themeBackgroundUrl.isNotBlank() -> {
        AsyncImage(
          model = room.themeBackgroundUrl,
          contentDescription = "रूम थीम बैकग्राउंड",
          modifier = Modifier
            .fillMaxSize()
            .testTag("room_custom_theme_background"),
          contentScale = ContentScale.Crop,
        )
      }
      room.backgroundTheme == "neon" -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color(0xFF0A0624), Color(0xFF1E0B4B), Color(0xFF062C42))
              )
            ),
        )
      }
      room.backgroundTheme == "sunset" -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color(0xFF3A0F2E), Color(0xFF6B1E3D), Color(0xFF1F0C29))
              )
            ),
        )
      }
      else -> {
        val bgRes = when (room.backgroundTheme) {
          "aurora" -> R.drawable.img_room_bg_aurora
          else -> R.drawable.img_room_bg_stadium
        }
        Image(
          painter = painterResource(id = bgRes),
          contentDescription = null,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
      }
    }
    if (room.themeBackgroundUrl.isBlank() && room.coverUrl.isNotBlank()) {
      AsyncImage(
        model = room.coverUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alpha = 0.45f,
      )
    }
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0x99120A26), Color(0x66160D30), Color(0xCC0D071D))
          )
        )
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding(),
    ) {
      // Top Room Bar (Screenshot 1 layout)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Top Room DP — clickable to open Real-Time Side Menu (On-Seat + Listeners)
        Box(
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable { coroutineScope.launch { drawerState.open() } }
            .testTag("room_dp_menu_button"),
          contentAlignment = Alignment.Center,
        ) {
          UserAvatar(
            url = room.coverUrl.ifBlank { room.ownerAvatarUrl },
            name = room.title.ifBlank { room.ownerName },
            size = 42.dp,
            borderColor = SwarGold,
            borderWidth = 1.5.dp,
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(
          modifier = Modifier
            .weight(1f)
            .clickable { coroutineScope.launch { drawerState.open() } },
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.WorkspacePremium,
              contentDescription = null,
              tint = SwarGold,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = room.title,
              style = MaterialTheme.typography.titleSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "ID: ${room.roomCode}",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFD0C9EC),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "रूम में कुल सदस्य",
                tint = Color(0xFFD0C9EC),
                modifier = Modifier.size(12.dp),
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                text = "$totalInRoom",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD0C9EC),
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }

        if (room.isPinLocked && room.roomPin.isNotBlank()) {
          Surface(
            color = Color(0x55FFB800),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .border(1.dp, SwarGold.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
              .testTag("room_top_lock_badge"),
          ) {
            Icon(
              imageVector = Icons.Filled.Lock,
              contentDescription = "रूम पिन लॉक सक्रिय",
              tint = SwarGold,
              modifier = Modifier
                .padding(5.dp)
                .size(14.dp),
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
        }

        if (roomMusicState.isPrepared) {
          Surface(
            onClick = { showSettingsDialog = true },
            color = Color(0x5500E5B4),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .border(1.dp, SwarMint.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
              .testTag("room_top_music_badge"),
          ) {
            Icon(
              imageVector = Icons.Filled.MusicNote,
              contentDescription = "रूम संगीत नियंत्रण",
              tint = SwarMint,
              modifier = Modifier
                .padding(5.dp)
                .size(14.dp),
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
        }

        if (room.clubTag.isNotBlank()) {
          Surface(
            color = Color(0x88173F3B),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.border(1.dp, SwarMint.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
          ) {
            Text(
              text = room.clubTag,
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
        }

        IconButton(
          onClick = { showSettingsDialog = true },
          modifier = Modifier.testTag("room_settings_button"),
        ) {
          Icon(Icons.Filled.Settings, contentDescription = "रूम सेटिंग्स", tint = Color.White)
        }
        IconButton(onClick = { onShowToast("रूम ID कॉपी हो गई: ${room.roomCode}") }) {
          Icon(Icons.Filled.Share, contentDescription = "रूम साझा करें", tint = Color.White)
        }
        IconButton(
          onClick = { showExitOptionsDialog = true },
          modifier = Modifier.testTag("leave_room_button"),
        ) {
          Icon(Icons.Filled.PowerSettingsNew, contentDescription = "रूम विकल्प", tint = Color.White)
        }
      }

      // Cross-Room Right-to-Left Gift Announcement Strip
      if (latestMarqueeGift != null) {
        CrossRoomGiftMarqueeStrip(
          event = latestMarqueeGift,
          onFinished = { onClearMarqueeGift(latestMarqueeGift.id) },
        )
      }

      // Top 2 Host / Co-Host Seats Centered Side-by-Side (with Adjacent CP Animation Bridge if paired)
      val hostPairCp = if (topSeats.size >= 2) {
        SwarUserIdentity.areAcceptedCpPartners(
          topSeats[0].occupantUid,
          topSeats[1].occupantUid,
          acceptedCpLinks,
        )
      } else null
      val hostPairInfo = remember(hostPairCp?.cpXp, hostPairCp?.acceptedAtMs) {
        hostPairCp?.let { SwarUserIdentity.cpLevelInfoFor(it.cpXp, it.acceptedAtMs) }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
      ) {
        if (hostPairInfo != null) {
          AdjacentCpSeatBridgeCanvas(
            info = hostPairInfo,
            startCenterFractionX = 0.34f,
            endCenterFractionX = 0.66f,
            modifier = Modifier.matchParentSize(),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          topSeats.forEach { topSeat ->
            val occUid = topSeat.occupantUid
            val occFrameUrl = if (occUid.isNotBlank()) {
              val occHp = if (occUid == currentUserId) {
                effectiveOwnHp
              } else {
                SwarUserIdentity.effectiveHpForProfile(userProfiles[occUid])
              }
              SwarUserIdentity.highestUnlockedFrameUrl(
                SwarUserIdentity.levelForCoins(occHp).level,
                levelConfigs,
              )
            } else ""
            val isCpPartnerSeated = SwarUserIdentity.findAcceptedCpForUser(occUid, acceptedCpLinks) != null
            HostSeatBadge(
              seat = topSeat,
              emptyLabel = if (topSeat.seatIndex == 0L) "होस्ट सीट 1" else "होस्ट सीट 2",
              localLevel = if (topSeat.occupantUid == currentUserId) micAudioLevel else 0f,
              frameUrl = occFrameUrl,
              activeEmoji = topSeat.occupantUid.takeIf { it.isNotBlank() }?.let { activeSeatReactions[it] },
              isCpLinked = isCpPartnerSeated,
              onClick = { openSeatOrProfileSheet(topSeat) },
            )
          }
        }
        if (hostPairInfo != null && topSeats.size >= 2) {
          AdjacentCpFloatingCrest(
            info = hostPairInfo,
            tag = "cp_adjacent_bridge_${topSeats[0].seatIndex}_${topSeats[1].seatIndex}",
            modifier = Modifier
              .align(Alignment.TopCenter)
              .padding(top = 2.dp),
          )
        }
      }

      // Multi-Seat Voice Grid (5 columns per row with smooth cross-seat CP Aura Animation when friends sit side-by-side)
      val seatRows = remember(guestSeats) { guestSeats.chunked(5) }
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        seatRows.forEach { rowSeats ->
          val adjacentPairs = remember(rowSeats, acceptedCpLinks) {
            buildList {
              for (col in 0 until (rowSeats.size - 1)) {
                val left = rowSeats[col]
                val right = rowSeats[col + 1]
                val rel = SwarUserIdentity.areAcceptedCpPartners(
                  left.occupantUid,
                  right.occupantUid,
                  acceptedCpLinks,
                )
                if (rel != null) {
                  add(Triple(col, rel, left.seatIndex to right.seatIndex))
                }
              }
            }
          }
          Box(modifier = Modifier.fillMaxWidth()) {
            adjacentPairs.forEach { (col, rel, _) ->
              val info = remember(rel.cpXp, rel.acceptedAtMs) {
                SwarUserIdentity.cpLevelInfoFor(rel.cpXp, rel.acceptedAtMs)
              }
              AdjacentCpSeatBridgeCanvas(
                info = info,
                startCenterFractionX = (col + 0.5f) / 5f,
                endCenterFractionX = (col + 1.5f) / 5f,
                modifier = Modifier.matchParentSize(),
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.Top,
            ) {
              for (col in 0 until 5) {
                if (col < rowSeats.size) {
                  val seat = rowSeats[col]
                  val occUid = seat.occupantUid
                  val occFrameUrl = if (occUid.isNotBlank()) {
                    val occHp = if (occUid == currentUserId) {
                      effectiveOwnHp
                    } else {
                      SwarUserIdentity.effectiveHpForProfile(userProfiles[occUid])
                    }
                    SwarUserIdentity.highestUnlockedFrameUrl(
                      SwarUserIdentity.levelForCoins(occHp).level,
                      levelConfigs,
                    )
                  } else ""
                  val isCpPartnerSeated = SwarUserIdentity.findAcceptedCpForUser(occUid, acceptedCpLinks) != null
                  Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopCenter,
                  ) {
                    GuestSeatTile(
                      seat = seat,
                      localLevel = if (seat.occupantUid == currentUserId) micAudioLevel else 0f,
                      frameUrl = occFrameUrl,
                      activeEmoji = seat.occupantUid.takeIf { it.isNotBlank() }?.let { activeSeatReactions[it] },
                      isCpLinked = isCpPartnerSeated,
                      onClick = { openSeatOrProfileSheet(seat) },
                    )
                  }
                } else {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }

            adjacentPairs.forEach { (col, rel, seatIndices) ->
              val info = remember(rel.cpXp, rel.acceptedAtMs) {
                SwarUserIdentity.cpLevelInfoFor(rel.cpXp, rel.acceptedAtMs)
              }
              val midFraction = (col + 1f) / 5f
              val horizontalBias = (midFraction * 2f - 1f).coerceIn(-0.9f, 0.9f)
              AdjacentCpFloatingCrest(
                info = info,
                tag = "cp_adjacent_bridge_${seatIndices.first}_${seatIndices.second}",
                modifier = Modifier
                  .align(androidx.compose.ui.BiasAlignment(horizontalBias, -0.95f)),
              )
            }
          }
        }
      }

      // Real-time Room Chat Stream + Rocket Widget positioned directly above Gift Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(172.dp)
          .padding(horizontal = 12.dp),
      ) {
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxSize()
            .padding(end = 114.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          item(key = "welcome_banner") {
            Surface(
              color = Color(0x66000000),
              shape = RoundedCornerShape(14.dp),
            ) {
              Text(
                text = room.welcomeNotice,
                style = MaterialTheme.typography.bodySmall,
                color = SwarCyan,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          }
          items(visibleMessages, key = { it.id.ifBlank { "${it.senderUid}_${it.text.hashCode()}" } }) { msg ->
            if (msg.senderUid.isNotBlank() && msg.senderUid != currentUserId && msg.senderUid !in userProfiles) {
              LaunchedEffect(msg.senderUid) {
                onRequestUserProfile(msg.senderUid)
              }
            }
            val senderHp = if (msg.senderUid == currentUserId) {
              effectiveOwnHp
            } else {
              SwarUserIdentity.effectiveHpForProfile(userProfiles[msg.senderUid])
            }
            val senderLevel = SwarUserIdentity.levelForCoins(senderHp).level
            val senderTypingBannerUrl = if (msg.messageType == "text") {
              SwarUserIdentity.highestUnlockedTypingBannerUrl(senderLevel, levelConfigs)
            } else {
              ""
            }
            RoomMessageBubble(
              message = msg,
              typingBannerUrl = senderTypingBannerUrl,
              onClick = if (msg.senderUid.isNotBlank()) {
                {
                  selectedProfileTarget = RoomProfileTarget(
                    uid = msg.senderUid,
                    displayName = msg.senderName,
                    avatarUrl = msg.senderAvatarUrl,
                    seat = seatedMembers.firstOrNull { it.occupantUid == msg.senderUid },
                  )
                }
              } else null,
            )
          }
        }

        RoomRocketCompactWidget(
          rocketState = roomRocketState,
          onOpenDetails = { showRocketDetailsDialog = true },
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 2.dp),
        )
      }

      // Quick reaction space handled by EmojiPanelSheet modal below

      // Live Text Message Composer (expands cleanly above bottom bar)
      AnimatedVisibility(visible = showComposer) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC120B26))
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = composerText,
            onValueChange = { composerText = it.take(500) },
            placeholder = { Text("लाइव संदेश लिखें...", color = Color(0xFFA7A0C8)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = SwarCyan,
              unfocusedBorderColor = Color(0x55FFFFFF),
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("room_chat_input"),
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = {
              if (composerText.isNotBlank()) {
                onSendMessage(composerText, "text")
                composerText = ""
                showComposer = false
              }
            },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(SwarCyan)
              .testTag("room_chat_send_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "भेजें", tint = Color(0xFF051824))
          }
          IconButton(onClick = { showComposer = false }) {
            Icon(Icons.Filled.Close, contentDescription = "बंद करें", tint = Color.White)
          }
        }
      }

      // Bottom Room Action Bar (matching Screenshot 1)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          onClick = { showComposer = true },
          color = Color(0x66000000),
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("say_hello_bar"),
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            Text(
              text = stringResource(id = R.string.say_hello_hint),
              color = Color(0xFFD5CEF0),
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }

        // DM Host shortcut
        RoomRoundActionButton(
          icon = Icons.Filled.MailOutline,
          description = "निजी संदेश",
          tint = Color.White,
          containerColor = Color(0x66000000),
          onClick = {
            if (room.ownerId != currentUserId) {
              onStartDirectChat(room.ownerId, room.ownerName, room.ownerAvatarUrl)
            } else {
              onShowToast("सीट पर किसी सदस्य पर टैप करके उन्हें निजी संदेश भेजें।")
            }
          },
        )

        // Speaker Toggle (Main Loudspeaker Mute / Unmute — never earpiece!)
        RoomRoundActionButton(
          icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
          description = if (isSpeakerOn) "रूम आवाज़ म्यूट करें" else "रूम आवाज़ चालू करें",
          tint = if (isSpeakerOn) SwarCyan else SwarCoral,
          containerColor = Color(0x66000000),
          modifier = Modifier.testTag("room_speaker_toggle"),
          onClick = onToggleSpeaker,
        )

        // Live Mic Mute / Unmute Toggle
        RoomRoundActionButton(
          icon = if (isMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
          description = if (isMicMuted) "माइक चालू करें" else "माइक बंद करें",
          tint = if (isMicMuted) Color.White else Color(0xFF041A24),
          containerColor = if (isMicMuted) Color(0x66000000) else SwarMint,
          modifier = Modifier.testTag("room_mic_toggle"),
          onClick = {
            onToggleMic(hasAudioPermission.value) {
              audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
          },
        )

        // Emoji Panel Trigger
        RoomRoundActionButton(
          icon = Icons.Filled.EmojiEmotions,
          description = "इमोजी पैनल",
          tint = SwarGold,
          containerColor = Color(0x66000000),
          modifier = Modifier.testTag("open_emoji_panel_button"),
          onClick = { showEmojiPanel = true },
        )

        // Gift Panel Trigger
        RoomRoundActionButton(
          icon = Icons.Filled.CardGiftcard,
          description = "गिफ्ट पैनल",
          tint = Color.White,
          containerColor = Brush.linearGradient(listOf(SwarCoral, SwarViolet)),
          modifier = Modifier.testTag("open_gift_panel_button"),
          onClick = { showGiftPanel = true },
        )
      }
    }

    // Full-Screen Gift Playback Overlay (strictly scoped to the active room only)
    if (activeFullScreenGift != null && activeFullScreenGift.roomId == room.id) {
      FullScreenGiftOverlay(
        event = activeFullScreenGift,
        onDismiss = onDismissFullScreenGift,
        onPlaybackMessage = onShowToast,
      )
    }

    // Full-Screen Rocket Launch & Blast Overlay (#1 Rank DP rides on rocket -> Blast -> Top-3 Rewards)
    if (activeRocketLaunch != null && activeRocketLaunch.roomId == room.id) {
      FullScreenRocketLaunchOverlay(
        event = activeRocketLaunch,
        onDismiss = onDismissRocketLaunch,
      )
    }
  }
  } // End ModalNavigationDrawer

  if (showRocketDetailsDialog) {
    RocketDetailsDialog(
      rocketState = roomRocketState,
      onDismiss = { showRocketDetailsDialog = false },
    )
  }

  // Profile Card Sheet (Own DP/Seat OR Other User's DP/Seat/Listener/Chat)
  val profileTarget = selectedProfileTarget
  if (profileTarget != null) {
    LaunchedEffect(profileTarget.uid) {
      if (profileTarget.uid.isNotBlank() && profileTarget.uid != currentUserId) {
        onRequestUserProfile(profileTarget.uid)
      }
    }
    val liveSeat = profileTarget.seat?.let { s ->
      seats.firstOrNull { it.seatIndex == s.seatIndex && it.occupantUid == profileTarget.uid }
        ?: seatedMembers.firstOrNull { it.occupantUid == profileTarget.uid }
    } ?: seatedMembers.firstOrNull { it.occupantUid == profileTarget.uid }
    val isSelf = profileTarget.uid == currentUserId
    val remoteProfile = userProfiles[profileTarget.uid]
    val resolvedName = when {
      isSelf -> currentUserName.ifBlank { profileTarget.displayName.ifBlank { "आप" } }
      else -> remoteProfile?.displayName?.ifBlank { profileTarget.displayName }
        ?: profileTarget.displayName.ifBlank { "सदस्य" }
    }
    val resolvedAvatar = when {
      isSelf -> currentUserAvatar.ifBlank { profileTarget.avatarUrl }
      else -> remoteProfile?.avatarUrl?.ifBlank { profileTarget.avatarUrl } ?: profileTarget.avatarUrl
    }
    val resolvedClub = remoteProfile?.clubTag?.ifBlank { "" }.orEmpty()
    val numericId = remember(profileTarget.uid) {
      SwarUserIdentity.numericIdForUid(profileTarget.uid)
    }
    val levelInfo = remember(isSelf, effectiveOwnHp, remoteProfile?.coins, remoteProfile?.hp) {
      if (isSelf) SwarUserIdentity.levelForCoins(effectiveOwnHp)
      else SwarUserIdentity.levelForProfile(remoteProfile)
    }
    val isBlocked = profileTarget.uid in blockedUserIds
    val memberSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
      onDismissRequest = { selectedProfileTarget = null },
      sheetState = memberSheetState,
      containerColor = Color(0xFF170E2E),
      contentColor = Color.White,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        val activeFrameUrl = remember(levelInfo.level, levelConfigs) {
          SwarUserIdentity.highestUnlockedFrameUrl(levelInfo.level, levelConfigs)
        }
        // 1. Top Circular DP with Animated Level Frame
        FramedUserAvatar(
          url = resolvedAvatar,
          name = resolvedName,
          avatarSize = 82.dp,
          frameUrl = activeFrameUrl,
          borderColor = if (isSelf || activeFrameUrl.isNotBlank()) SwarGold else SwarCyan,
          borderWidth = 2.5.dp,
        )

        // 2. Below DP: Name (+ Club Tag)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = if (isSelf) "$resolvedName (आप)" else resolvedName,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (profileTarget.uid == room.ownerId || profileTarget.uid in room.managerUids) {
            val roleLabel = if (profileTarget.uid == room.ownerId) "होस्ट" else "मैनेजर"
            Surface(
              color = if (profileTarget.uid == room.ownerId) SwarGold.copy(alpha = 0.28f) else SwarCyan.copy(alpha = 0.28f),
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = roleLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (profileTarget.uid == room.ownerId) SwarGold else SwarCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          if (resolvedClub.isNotBlank()) {
            Surface(
              color = SwarViolet.copy(alpha = 0.55f),
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = resolvedClub,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }

        // 3. Below Name: Unique 10-Digit ID + Copy Icon
        Surface(
          color = Color(0x28FFFFFF),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.border(1.dp, SwarCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
        ) {
          Row(
            modifier = Modifier.padding(start = 12.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "ID: $numericId",
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag("sheet_user_numeric_id"),
            )
            IconButton(
              onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("SwarRoom ID", numericId))
                onShowToast("ID कॉपी हो गई: $numericId")
              },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("sheet_copy_id_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "ID कॉपी करें",
                tint = SwarCyan,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }

        var showSheetLevelCards by remember(profileTarget.uid) { mutableStateOf(false) }

        // 4. Below ID: Royal Side-by-Side Level Card (Left) + Unlocked Achievement Badges / ब्याज Showcase (Right)
        val sheetTier = remember(levelInfo.level) { SwarUserIdentity.tierForLevel(levelInfo.level) }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            onClick = { showSheetLevelCards = true },
            color = levelInfo.badgeColor.copy(alpha = 0.16f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(0.95f)
              .border(1.dp, levelInfo.badgeColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
              .testTag("sheet_level_card"),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
              verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              OrnateWingedLevelCrest(
                tier = sheetTier,
                displayText = "LV.${levelInfo.level}",
                badgeWidth = 92.dp,
                badgeHeight = 32.dp,
                isCompact = true,
              )
              Text(
                text = "Lv.${levelInfo.level} • ${levelInfo.title}",
                style = MaterialTheme.typography.labelMedium,
                color = levelInfo.badgeColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("sheet_user_level_badge"),
              )
              LinearProgressIndicator(
                progress = { levelInfo.progress },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(5.dp)
                  .clip(RoundedCornerShape(4.dp)),
                color = levelInfo.badgeColor,
                trackColor = Color(0x33FFFFFF),
              )
              Text(
                text = "वर्तमान: ${SwarUserIdentity.formatIndianNumber(levelInfo.currentHp)} HP",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE6E0FF),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = if (levelInfo.level >= 120) {
                  "सर्वोच्च LV.120 पूर्ण"
                } else {
                  "अगले LV.${levelInfo.level + 1}: ${SwarUserIdentity.formatIndianNumber(levelInfo.nextLevelRemainingCoins)} HP बाकी"
                },
                style = MaterialTheme.typography.labelSmall,
                color = SwarCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = if (liveSeat != null) {
                  when (liveSeat.seatIndex) {
                    0L -> "होस्ट सीट 1"
                    1L -> "होस्ट सीट 2"
                    else -> "सीट #${liveSeat.seatIndex - 1L}"
                  }
                } else {
                  "लिसनर • 13 कार्ड्स ›"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD0C9EC),
                maxLines = 1,
              )
            }
          }

          ProfileSideBadgesShowcaseCard(
            userLevel = levelInfo.level,
            levelConfigs = levelConfigs,
            onOpenAllCards = { showSheetLevelCards = true },
            modifier = Modifier.weight(1.05f),
          )
        }

        val targetCpRel = remember(profileTarget.uid, acceptedCpLinks) {
          SwarUserIdentity.findAcceptedCpForUser(profileTarget.uid, acceptedCpLinks)
        }
        val ownCpRel = remember(currentUserId, acceptedCpLinks) {
          SwarUserIdentity.findAcceptedCpForUser(currentUserId, acceptedCpLinks)
        }
        if (targetCpRel != null) {
          JointCpProfileCard(
            cp = targetCpRel,
            focusUid = profileTarget.uid,
            darkSurface = true,
            modifier = Modifier.testTag("sheet_joint_cp_card"),
            onPartnerClick = { pUid, pName, pAvatar ->
              if (pUid.isNotBlank() && pUid != profileTarget.uid) {
                if (pUid != currentUserId) onRequestUserProfile(pUid)
                selectedProfileTarget = RoomProfileTarget(
                  uid = pUid,
                  displayName = pName,
                  avatarUrl = pAvatar,
                  seat = seatedMembers.firstOrNull { it.occupantUid == pUid },
                )
              }
            },
          )
        } else if (!isSelf && ownCpRel == null) {
          OutlinedButton(
            onClick = {
              onSendCpRequestById(numericId, "बेस्ट फ्रेंड")
              selectedProfileTarget = null
            },
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("sheet_send_cp_request_button"),
          ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFFF4081), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("CP पार्टनर जोड़ने का अनुरोध भेजें", color = Color(0xFFFFB2CE), fontWeight = FontWeight.Bold)
          }
        }

        if (showSheetLevelCards) {
          val sheetCoins = if (isSelf) effectiveOwnHp else SwarUserIdentity.effectiveHpForProfile(remoteProfile)
          LevelCardsShowcaseDialog(
            levelInfo = levelInfo,
            coins = sheetCoins,
            onDismiss = { showSheetLevelCards = false },
            levelConfigs = levelConfigs,
            claimedTiers = if (isSelf) claimedLevelTiers else emptySet(),
            isAdminUser = isAdminUser,
            isUploadingMedia = isUploadingMedia,
            sampleAvatarUrl = resolvedAvatar,
            sampleDisplayName = resolvedName,
            onSaveTierConfig = onSaveLevelTierConfig,
            onClaimTierReward = onClaimLevelTierReward,
          )
        }

        HorizontalDivider(color = Color(0x28FFFFFF))

        // 5. Bottom Controls
        if (isSelf) {
          // Own Profile Bottom Controls: Sit / Leave Seat + Lock/Unlock Seat + Mic Mute
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (liveSeat != null) {
              OutlinedButton(
                onClick = {
                  onLeaveSeat()
                  selectedProfileTarget = null
                },
                modifier = Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("own_profile_leave_seat"),
              ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = SwarCoral)
                Spacer(modifier = Modifier.width(6.dp))
                Text("सीट से उठें", color = Color.White, maxLines = 1)
              }
            } else {
              Button(
                onClick = {
                  onSitOnAvailableSeat(hasAudioPermission.value) {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                  selectedProfileTarget = null
                },
                colors = ButtonDefaults.buttonColors(containerColor = SwarCyan, contentColor = Color(0xFF041A24)),
                modifier = Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("own_profile_sit_seat"),
              ) {
                Icon(Icons.Filled.EventSeat, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("सीट पर बैठें", fontWeight = FontWeight.Bold, maxLines = 1)
              }
            }

            if (isModerator) {
              val seatToToggleLock = liveSeat ?: topSeats.firstOrNull()
              if (seatToToggleLock != null) {
                OutlinedButton(
                  onClick = {
                    onToggleSeatLock(seatToToggleLock)
                    selectedProfileTarget = null
                  },
                  modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("own_profile_lock_seat"),
                ) {
                  Icon(
                    imageVector = if (seatToToggleLock.isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = SwarGold,
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (seatToToggleLock.isLocked) "अनलॉक करें" else "सीट लॉक करें",
                    color = Color.White,
                    maxLines = 1,
                  )
                }
              }
            }
          }

          if (liveSeat != null) {
            Button(
              onClick = {
                onToggleMic(hasAudioPermission.value) {
                  audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
                selectedProfileTarget = null
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isMicMuted) SwarMint else SwarViolet,
                contentColor = if (isMicMuted) Color(0xFF041A24) else Color.White,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("own_profile_mic_toggle"),
            ) {
              Icon(
                imageVector = if (isMicMuted) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = null,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(if (isMicMuted) "माइक अनम्यूट करें" else "माइक म्यूट करें", fontWeight = FontWeight.Bold)
            }
          }
        } else {
          // Other User Bottom Controls: Host & Managers can Kick Out / Mute / Lock Seat (never Owner)
          val canModerateTarget = isModerator && profileTarget.uid != room.ownerId && profileTarget.uid != currentUserId
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (canModerateTarget) {
              Button(
                onClick = {
                  onKickRoomMember(profileTarget.uid, resolvedName, resolvedAvatar)
                  selectedProfileTarget = null
                },
                colors = ButtonDefaults.buttonColors(containerColor = SwarCoral, contentColor = Color.White),
                modifier = Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("other_profile_kick_button"),
              ) {
                Icon(Icons.Filled.PersonRemove, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("किक आउट", fontWeight = FontWeight.Bold, maxLines = 1)
              }
            }

            OutlinedButton(
              onClick = {
                if (isBlocked) {
                  onUnblockUser(profileTarget.uid, resolvedName)
                } else {
                  onBlockUser(profileTarget.uid, resolvedName)
                }
                selectedProfileTarget = null
              },
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("other_profile_block_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Block,
                contentDescription = null,
                tint = if (isBlocked) SwarMint else SwarCoral,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isBlocked) "अनब्लॉक" else "ब्लॉक करें",
                color = if (isBlocked) SwarMint else SwarCoral,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
              )
            }

            if (canModerateTarget && liveSeat != null) {
              OutlinedButton(
                onClick = {
                  onToggleSeatMute(liveSeat, hasAudioPermission.value) {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                  selectedProfileTarget = null
                },
                modifier = Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .testTag("other_profile_mute_button"),
              ) {
                Icon(
                  imageVector = if (liveSeat.isMuted) Icons.Filled.Mic else Icons.Filled.MicOff,
                  contentDescription = null,
                  tint = if (liveSeat.isMuted) SwarMint else SwarGold,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (liveSeat.isMuted) "अनम्यूट" else "म्यूट करें",
                  color = Color.White,
                  maxLines = 1,
                )
              }
            }
          }

          if (canModerateTarget && liveSeat != null) {
            OutlinedButton(
              onClick = {
                onToggleSeatLock(liveSeat)
                selectedProfileTarget = null
              },
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("other_profile_lock_seat"),
            ) {
              Icon(
                imageVector = if (liveSeat.isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = null,
                tint = SwarGold,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (liveSeat.isLocked) "सीट अनलॉक करें" else "सीट लॉक करें",
                color = Color.White,
              )
            }
          }

          if (!isBlocked) {
            TextButton(
              onClick = {
                onStartDirectChat(profileTarget.uid, resolvedName, resolvedAvatar)
                selectedProfileTarget = null
              },
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize(),
            ) {
              Icon(Icons.Filled.MailOutline, contentDescription = null, tint = SwarCyan)
              Spacer(modifier = Modifier.width(8.dp))
              Text("$resolvedName को निजी संदेश भेजें", color = SwarCyan)
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }
    }
  }

  // Empty/Locked Seat Options Bottom Sheet (shown on tapping an EMPTY or LOCKED seat: Sit, Lock, Mute)
  val sheetSeat = selectedSeatForSheet
  if (sheetSeat != null) {
    ModalBottomSheet(onDismissRequest = { selectedSeatForSheet = null }) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        val seatLabel = when (sheetSeat.seatIndex) {
          0L -> "होस्ट सीट 1"
          1L -> "होस्ट सीट 2"
          else -> "सीट #${sheetSeat.seatIndex - 1L}"
        }
        val statusText = if (sheetSeat.isLocked) "लॉक सीट" else "खाली सीट"
        Text(
          text = "$seatLabel • $statusText",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )

        Button(
          onClick = {
            if (sheetSeat.isLocked && isModerator) {
              onToggleSeatLock(sheetSeat)
            }
            onSeatTapped(sheetSeat.copy(isLocked = false), hasAudioPermission.value) {
              audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            selectedSeatForSheet = null
          },
          enabled = !sheetSeat.isLocked || isModerator,
          colors = ButtonDefaults.buttonColors(containerColor = SwarCyan, contentColor = Color(0xFF041A24)),
          modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("seat_action_sit"),
        ) {
          Icon(Icons.Filled.EventSeat, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (sheetSeat.isLocked) "लॉक है — अनलॉक करके बैठें" else "इस सीट पर बैठें",
            fontWeight = FontWeight.Bold,
          )
        }

        if (isModerator) {
          OutlinedButton(
            onClick = {
              onToggleSeatMute(sheetSeat, hasAudioPermission.value) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
              selectedSeatForSheet = null
            },
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("seat_action_mute"),
          ) {
            Icon(
              imageVector = if (sheetSeat.isMuted) Icons.Filled.Mic else Icons.Filled.MicOff,
              contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (sheetSeat.isMuted) "सीट अनम्यूट करें" else "सीट म्यूट करें")
          }

          OutlinedButton(
            onClick = {
              onToggleSeatLock(sheetSeat)
              selectedSeatForSheet = null
            },
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("seat_action_lock"),
          ) {
            Icon(
              imageVector = if (sheetSeat.isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
              contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (sheetSeat.isLocked) "सीट अनलॉक करें" else "सीट लॉक करें")
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }
    }
  }

  // Room Control Studio Sheet (DP/Name, Themes, Managers, Kick Audit, Memory Music, PIN Lock, Seat Count, Audio)
  if (showSettingsDialog) {
    RoomSettingsSheet(
      room = room,
      isHost = isHost,
      isUploadingMedia = isUploadingMedia,
      isMonitorLoopbackOn = isMonitorLoopbackOn,
      isAgoraConnected = isAgoraConnected,
      agoraStatusText = agoraStatusText,
      userProfiles = userProfiles,
      livePresenceMembers = livePresenceMembers,
      seats = visibleSeats,
      kickLogs = kickLogs,
      roomMusicState = roomMusicState,
      scannedMusicTracks = scannedMusicTracks,
      isScanningMusic = isScanningMusic,
      onToggleMonitorLoopback = onToggleMonitorLoopback,
      onUpdateIdentity = onUpdateRoomIdentity,
      onUpdateTheme = onUpdateRoomTheme,
      onAddManagerById = onAddRoomManagerById,
      onRemoveManager = onRemoveRoomManager,
      onUpdateRoomPinLock = onUpdateRoomPinLock,
      onResizeRoomSeats = onResizeRoomSeats,
      onPickMusicUri = onPickRoomMusic,
      onToggleMusicPlayPause = onToggleRoomMusicPlayPause,
      onStopMusic = onStopRoomMusic,
      onMusicVolumeChange = onRoomMusicVolumeChange,
      onScanMemoryCardMusic = onScanMemoryCardMusic,
      onSelectMusicFolderTree = onSelectMusicFolderTree,
      onPlayScannedMusicTrack = onPlayScannedMusicTrack,
      onPlayNextMusic = onPlayNextMusic,
      onPlayPreviousMusic = onPlayPreviousMusic,
      onDismiss = { showSettingsDialog = false },
    )
  }

  // 4-Category Emoji Panel Sheet (with Admin-only '+' upload & long-press Update/Delete)
  if (showEmojiPanel) {
    EmojiPanelSheet(
      emojis = emojiCatalog,
      isAdminUser = isAdminUser,
      isUploading = isUploadingMedia,
      onSelectEmoji = onSendEmoji,
      onAddCustomEmoji = onAddCustomEmoji,
      onUpdateCustomEmoji = onUpdateCustomEmoji,
      onDeleteCustomEmoji = onDeleteCustomEmoji,
      onDismiss = { showEmojiPanel = false },
      emojiUploadError = emojiUploadError,
      onClearEmojiUploadError = onClearEmojiUploadError,
    )
  }

  // 4-Category Gift Panel Sheet (with Coins balance, Admin '+' upload & long-press Update/Delete)
  if (showGiftPanel) {
    GiftPanelSheet(
      room = room,
      seats = seats,
      gifts = giftCatalog,
      userCoins = userCoins,
      isAdminUser = isAdminUser,
      isUploading = isUploadingMedia,
      currentUserId = currentUserId,
      acceptedCpLinks = acceptedCpLinks,
      presenceMembers = livePresenceMembers,
      onClaimBonusCoins = onClaimBonusCoins,
      onSendGift = onSendGift,
      onAddCustomGift = onAddCustomGift,
      onUpdateCustomGift = onUpdateCustomGift,
      onDeleteCustomGift = onDeleteCustomGift,
      onDismiss = { showGiftPanel = false },
      giftUploadError = giftUploadError,
      onClearGiftUploadError = onClearGiftUploadError,
    )
  }

  // Two-Option Switch-Off Dialog: Minimize Room OR Exit Room Completely
  if (showExitOptionsDialog) {
    AlertDialog(
      onDismissRequest = { showExitOptionsDialog = false },
      containerColor = Color(0xFF1A1033),
      titleContentColor = Color.White,
      textContentColor = Color.White,
      title = {
        Text(
          text = "रूम विकल्प",
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Surface(
            onClick = {
              showExitOptionsDialog = false
              onMinimizeRoom()
            },
            color = Color(0xFF24184A),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, SwarCyan.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
              .minimumInteractiveComponentSize()
              .testTag("minimize_room_option_button"),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.CloseFullscreen,
                contentDescription = "रूम छोटा करें",
                tint = SwarCyan,
                modifier = Modifier.size(26.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "रूम छोटा करें (Minimize)",
                  style = MaterialTheme.typography.titleSmall,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "रूम चालू रहेगा और आप होम, मोमेंट्स, संदेश व सेटिंग्स में जा सकेंगे",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFFD0C9EC),
                )
              }
            }
          }

          Surface(
            onClick = {
              showExitOptionsDialog = false
              onLeaveRoom()
            },
            color = Color(0xFF2D1530),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, SwarCoral.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
              .minimumInteractiveComponentSize()
              .testTag("confirm_exit_room_option_button"),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = "रूम से बाहर निकलें",
                tint = SwarCoral,
                modifier = Modifier.size(26.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "रूम से बाहर निकलें",
                  style = MaterialTheme.typography.titleSmall,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "सीट छोड़कर रूम से पूरी तरह बाहर निकल जाएं",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFFFFC5CF),
                )
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showExitOptionsDialog = false }) {
          Text("रद्द करें", color = SwarCyan)
        }
      },
    )
  }
}

@Composable
private fun RoomParticipantsDrawerSheet(
  room: VoiceRoom,
  totalSeatCount: Int,
  totalInRoom: Long,
  audienceCount: Long,
  seatedMembers: List<RoomSeat>,
  isCurrentUserSeated: Boolean,
  currentUserId: String,
  currentUserName: String,
  currentUserAvatar: String,
  userCoins: Long,
  userProfiles: Map<String, UserProfile>,
  liveListeners: List<RoomPresenceMember>,
  onOwnProfileClick: () -> Unit,
  onSeatMemberClick: (RoomSeat) -> Unit,
  onListenerMemberClick: (uid: String, name: String, avatar: String) -> Unit,
  onOpenSettings: () -> Unit,
  onLeaveRoom: () -> Unit,
  onClose: () -> Unit,
) {
  val ownNumericId = remember(currentUserId) { SwarUserIdentity.numericIdForUid(currentUserId) }
  val ownLevel = remember(userCoins) { SwarUserIdentity.levelForCoins(userCoins) }

  ModalDrawerSheet(
    drawerContainerColor = Color(0xFF140C29),
    drawerContentColor = Color.White,
    modifier = Modifier.width(305.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Room Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        UserAvatar(
          url = room.coverUrl.ifBlank { room.ownerAvatarUrl },
          name = room.title.ifBlank { room.ownerName },
          size = 52.dp,
          borderColor = SwarGold,
          borderWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = room.title.ifBlank { "लाइव वॉइस रूम" },
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "होस्ट: ${room.ownerName} • ID: ${room.roomCode}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFD0C9EC),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(onClick = onClose) {
          Icon(Icons.Filled.Close, contentDescription = "बंद करें", tint = Color.White)
        }
      }

      // Own Profile Quick Card (tap DP to view ID, Level & Seat controls)
      Surface(
        onClick = onOwnProfileClick,
        color = Color(0xFF231648),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, SwarCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
          .testTag("drawer_own_profile_card"),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          UserAvatar(
            url = currentUserAvatar,
            name = currentUserName.ifBlank { "आप" },
            size = 44.dp,
            borderColor = SwarGold,
            borderWidth = 2.dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${currentUserName.ifBlank { "आप" }} (मेरा प्रोफ़ाइल)",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "ID: $ownNumericId • Lv.${ownLevel.level}",
              style = MaterialTheme.typography.labelSmall,
              color = SwarCyan,
            )
          }
          Surface(
            color = ownLevel.badgeColor.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = "Lv.${ownLevel.level}",
              style = MaterialTheme.typography.labelSmall,
              color = ownLevel.badgeColor,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
      }

      // Real-time Summary Counters
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        DrawerMetricCard(
          label = "सीट पर",
          value = "${seatedMembers.size}/$totalSeatCount",
          accent = SwarCyan,
          modifier = Modifier.weight(1f),
        )
        DrawerMetricCard(
          label = "लिसनर में",
          value = "$audienceCount",
          accent = SwarMint,
          modifier = Modifier.weight(1f),
        )
        DrawerMetricCard(
          label = "कुल लाइव",
          value = "$totalInRoom",
          accent = SwarGold,
          modifier = Modifier.weight(1f),
        )
      }

      HorizontalDivider(color = Color(0x28FFFFFF))

      // Section 1: Real-time Seated Members
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.EventSeat, contentDescription = null, tint = SwarCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "सीट पर बैठे (${seatedMembers.size})",
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
      }

      if (seatedMembers.isEmpty()) {
        Text(
          text = "अभी कोई सीट पर नहीं बैठा है।",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFB5ACDA),
        )
      } else {
        seatedMembers.forEach { memberSeat ->
          val isSelf = memberSeat.occupantUid == currentUserId
          val memberLevel = if (isSelf) ownLevel else SwarUserIdentity.levelForProfile(userProfiles[memberSeat.occupantUid])
          val seatLabel = when (memberSeat.seatIndex) {
            0L -> "होस्ट सीट 1"
            1L -> "होस्ट सीट 2"
            else -> "सीट #${memberSeat.seatIndex - 1L}"
          }
          Surface(
            onClick = { onSeatMemberClick(memberSeat) },
            color = Color(0xFF211642),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              UserAvatar(
                url = memberSeat.occupantAvatarUrl,
                name = memberSeat.occupantName,
                size = 36.dp,
                borderColor = if (memberSeat.seatIndex == 0L) SwarGold else SwarCyan,
                borderWidth = 1.5.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (isSelf) "${memberSeat.occupantName} (आप)" else memberSeat.occupantName.ifBlank { "सदस्य" },
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color.White,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "$seatLabel • Lv.${memberLevel.level}",
                  style = MaterialTheme.typography.labelSmall,
                  color = SwarCyan,
                )
              }
              Icon(
                imageVector = if (memberSeat.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = if (memberSeat.isMuted) "म्यूट" else "माइक चालू",
                tint = if (memberSeat.isMuted) SwarCoral else SwarMint,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }

      HorizontalDivider(color = Color(0x28FFFFFF))

      // Section 2: Real-time Listeners (Off-seat Audience)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Headset, contentDescription = null, tint = SwarMint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "लिसनर में ($audienceCount)",
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
      }

      val selfListenerShown = !isCurrentUserSeated
      if (audienceCount == 0L && !selfListenerShown) {
        Text(
          text = "अभी नीचे कोई लिसनर नहीं है — सभी सीट पर बैठे हैं।",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFB5ACDA),
        )
      } else {
        if (selfListenerShown) {
          DrawerListenerRow(
            name = currentUserName.ifBlank { "आप" },
            avatarUrl = currentUserAvatar,
            subtitle = "लिसनर (आप) • Lv.${ownLevel.level}",
            onClick = onOwnProfileClick,
          )
        }
        liveListeners.forEach { participant ->
          val listenerLevel = SwarUserIdentity.levelForProfile(userProfiles[participant.userId])
          DrawerListenerRow(
            name = participant.displayName.ifBlank { "श्रोता" },
            avatarUrl = participant.avatarUrl,
            subtitle = "लिसनर • Lv.${listenerLevel.level}",
            onClick = {
              onListenerMemberClick(
                participant.userId,
                participant.displayName,
                participant.avatarUrl,
              )
            },
          )
        }
        val namedCount = (if (selfListenerShown) 1 else 0) + liveListeners.size
        val remainingListeners = (audienceCount - namedCount).coerceAtLeast(0L)
        if (remainingListeners > 0L) {
          Surface(
            color = Color(0x22FFFFFF),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "+$remainingListeners अन्य सदस्य लिसनर में सुन रहे हैं",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFD0C9EC),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.weight(1f))
      HorizontalDivider(color = Color(0x28FFFFFF))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onOpenSettings,
          modifier = Modifier.weight(1f),
        ) {
          Text("सेटिंग्स", color = Color.White)
        }
        Button(
          onClick = onLeaveRoom,
          colors = ButtonDefaults.buttonColors(containerColor = SwarCoral, contentColor = Color.White),
          modifier = Modifier.weight(1f),
        ) {
          Text("बाहर निकलें")
        }
      }
    }
  }
}

@Composable
private fun DrawerMetricCard(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color(0xFF211642),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier.border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        color = accent,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFFD0C9EC),
      )
    }
  }
}

@Composable
private fun DrawerListenerRow(
  name: String,
  avatarUrl: String,
  subtitle: String,
  onClick: () -> Unit = {},
) {
  Surface(
    onClick = onClick,
    color = Color(0xFF1D1338),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      UserAvatar(
        url = avatarUrl,
        name = name,
        size = 34.dp,
        borderColor = SwarMint,
        borderWidth = 1.dp,
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = name,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = SwarMint,
        )
      }
      Icon(
        imageVector = Icons.Filled.Headset,
        contentDescription = "लिसनर",
        tint = SwarMint,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

@Composable
private fun HostSeatBadge(
  seat: RoomSeat?,
  emptyLabel: String = "होस्ट सीट",
  localLevel: Float,
  frameUrl: String = "",
  activeEmoji: ActiveSeatEmoji? = null,
  isCpLinked: Boolean = false,
  onClick: () -> Unit,
) {
  val isOccupied = seat?.occupantUid?.isNotBlank() == true
  val isLocked = seat?.isLocked == true
  val speaking = isOccupied && (seat?.isSpeaking == true || localLevel > 0.08f)
  val pulseTransition = rememberInfiniteTransition(label = "hostPulse")
  val pulse by pulseTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.16f,
    animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
    label = "scale",
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clickable(onClick = onClick)
      .padding(4.dp)
      .testTag("host_seat_badge_${seat?.seatIndex ?: 0L}"),
  ) {
    Box(contentAlignment = Alignment.Center) {
      if (isOccupied) {
        if (speaking) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
              }
              .clip(CircleShape)
              .border(2.5.dp, SwarCyan, CircleShape)
          )
        }
        FramedUserAvatar(
          url = seat?.occupantAvatarUrl.orEmpty(),
          name = seat?.occupantName.orEmpty(),
          avatarSize = 48.dp,
          frameUrl = frameUrl,
          borderColor = when {
            isCpLinked -> Color(0xFFFF4081)
            speaking -> SwarMint
            else -> SwarGold
          },
          borderWidth = if (isCpLinked) 2.2.dp else 1.8.dp,
        )
        if (activeEmoji != null) {
          SeatEmojiReactionOverlay(reaction = activeEmoji, dpSize = 48.dp)
        }
        if (isCpLinked) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .size(16.dp)
              .clip(CircleShape)
              .background(Color(0xFFE91E63)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.Favorite,
              contentDescription = "CP पार्टनर",
              tint = Color.White,
              modifier = Modifier.size(10.dp),
            )
          }
        }
        if (seat?.isMuted == true) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .size(18.dp)
              .clip(CircleShape)
              .background(SwarCoral)
              .testTag("host_seat_muted_badge_${seat.seatIndex}"),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.MicOff,
              contentDescription = "माइक म्यूट है",
              tint = Color.White,
              modifier = Modifier.size(11.dp),
            )
          }
        }
      } else {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0x44FFFFFF))
            .border(1.5.dp, SwarGold.copy(alpha = 0.7f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.Chair,
            contentDescription = if (isLocked) "लॉक होस्ट सीट" else "खाली होस्ट सीट",
            tint = Color(0xEEFFFFFF),
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = when {
        isOccupied -> seat?.occupantName.orEmpty()
        isLocked -> "लॉक सीट"
        else -> emptyLabel
      },
      style = MaterialTheme.typography.labelSmall,
      color = Color.White,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun GuestSeatTile(
  seat: RoomSeat,
  localLevel: Float,
  frameUrl: String = "",
  activeEmoji: ActiveSeatEmoji? = null,
  isCpLinked: Boolean = false,
  onClick: () -> Unit,
) {
  val isOccupied = seat.occupantUid.isNotBlank()
  val isSpeaking = seat.isSpeaking || localLevel > 0.08f

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clickable(onClick = onClick)
      .padding(vertical = 2.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      if (isOccupied) {
        FramedUserAvatar(
          url = seat.occupantAvatarUrl,
          name = seat.occupantName,
          avatarSize = 46.dp,
          frameUrl = frameUrl,
          borderColor = when {
            isCpLinked -> Color(0xFFFF4081)
            isSpeaking -> SwarCyan
            else -> Color(0x88FFFFFF)
          },
          borderWidth = if (isCpLinked || isSpeaking) 2.2.dp else 1.dp,
        )
        if (activeEmoji != null) {
          SeatEmojiReactionOverlay(reaction = activeEmoji, dpSize = 46.dp)
        }
        if (isCpLinked) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .size(16.dp)
              .clip(CircleShape)
              .background(Color(0xFFE91E63)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.Favorite,
              contentDescription = "CP पार्टनर",
              tint = Color.White,
              modifier = Modifier.size(10.dp),
            )
          }
        }
        if (seat.isMuted) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .size(18.dp)
              .clip(CircleShape)
              .background(SwarCoral)
              .testTag("guest_seat_muted_badge_${seat.seatIndex}"),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.MicOff,
              contentDescription = "माइक म्यूट है",
              tint = Color.White,
              modifier = Modifier.size(11.dp),
            )
          }
        }
      } else {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0x44FFFFFF))
            .border(1.dp, Color(0x33FFFFFF), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = if (seat.isLocked) Icons.Filled.Lock else Icons.Filled.Chair,
            contentDescription = if (seat.isLocked) "लॉक सीट" else "खाली सीट",
            tint = Color(0xDDFFFFFF),
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = if (isOccupied) seat.occupantName else (seat.seatIndex - 1L).coerceAtLeast(1L).toString(),
      style = MaterialTheme.typography.labelSmall,
      color = Color.White,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun SeatEmojiReactionOverlay(
  reaction: ActiveSeatEmoji,
  dpSize: Dp,
) {
  val scaleAnim = remember(reaction.messageId) { Animatable(0.72f) }
  LaunchedEffect(reaction.messageId) {
    scaleAnim.animateTo(
      targetValue = 1.08f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    )
    scaleAnim.animateTo(
      targetValue = 1.0f,
      animationSpec = tween(180),
    )
  }
  Box(
    modifier = Modifier
      .size(dpSize)
      .scale(scaleAnim.value)
      .clip(CircleShape)
      .background(if (reaction.imageUrl.isNotBlank()) Color.Transparent else Color(0x99120824))
      .testTag("seat_emoji_overlay"),
    contentAlignment = Alignment.Center,
  ) {
    if (reaction.imageUrl.isNotBlank()) {
      AsyncImage(
        model = reaction.imageUrl,
        contentDescription = "इमोजी रिएक्शन",
        modifier = Modifier
          .fillMaxSize()
          .clip(CircleShape),
        contentScale = ContentScale.Crop,
      )
    } else {
      Text(
        text = reaction.symbol,
        fontSize = if (dpSize >= 56.dp) 46.sp else 36.sp,
      )
    }
  }
}

@Composable
private fun RoomMessageBubble(
  message: RoomMessage,
  typingBannerUrl: String = "",
  onClick: (() -> Unit)? = null,
) {
  val tag = if (typingBannerUrl.isNotBlank()) {
    "room_message_typing_banner_${message.id}"
  } else {
    "room_message_bubble_${message.id}"
  }
  TypingBannerChatBubble(
    senderName = message.senderName,
    messageText = message.text,
    badgeTag = message.badgeTag,
    bannerUrl = typingBannerUrl,
    messageType = message.messageType,
    modifier = Modifier.testTag(tag),
    onClick = onClick,
  )
}

@Composable
private fun RoomRoundActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  description: String,
  tint: Color,
  containerColor: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Box(
    modifier = modifier
      .size(46.dp)
      .clip(CircleShape)
      .background(containerColor)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = Modifier.size(21.dp))
  }
}

@Composable
private fun RoomRoundActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  description: String,
  tint: Color,
  containerColor: Brush,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Box(
    modifier = modifier
      .size(46.dp)
      .clip(CircleShape)
      .background(containerColor)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = Modifier.size(21.dp))
  }
}


