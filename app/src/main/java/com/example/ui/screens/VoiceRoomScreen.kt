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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.data.model.EmojiCatalogItem
import com.example.data.model.GiftCatalogItem
import com.example.data.model.GiftEvent
import com.example.data.model.RoomMessage
import com.example.data.model.RoomSeat
import com.example.data.model.SwarUserIdentity
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
import com.example.ui.components.CrossRoomGiftMarqueeStrip
import com.example.ui.components.EmojiPanelSheet
import com.example.ui.components.FullScreenGiftOverlay
import com.example.ui.components.GiftPanelSheet
import com.example.ui.components.UserAvatar
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
  onAddCustomEmoji: (String, String, String, Uri?, () -> Unit) -> Unit,
  onUpdateCustomEmoji: (EmojiCatalogItem, String, String, String, Uri?, Boolean, () -> Unit) -> Unit,
  onDeleteCustomEmoji: (EmojiCatalogItem, () -> Unit) -> Unit,
  onSendGift: (GiftCatalogItem, String, String, Int) -> Unit,
  onAddCustomGift: (String, String, String, Long, Uri?, Uri?, () -> Unit) -> Unit,
  onUpdateCustomGift: (GiftCatalogItem, String, String, String, Long, Uri?, Uri?, Boolean, Boolean, () -> Unit) -> Unit,
  onDeleteCustomGift: (GiftCatalogItem, () -> Unit) -> Unit,
  onDismissFullScreenGift: () -> Unit,
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
  var selectedSeatForSheet by remember { mutableStateOf<RoomSeat?>(null) }
  var selectedProfileTarget by remember { mutableStateOf<RoomProfileTarget?>(null) }

  val visibleMessages = remember(messages, blockedUserIds) {
    if (blockedUserIds.isEmpty()) messages
    else messages.filter { it.senderUid !in blockedUserIds }
  }

  val isHost = room.ownerId == currentUserId
  val topSeats = remember(seats, room) {
    listOf(0L, 1L).map { idx ->
      seats.firstOrNull { it.seatIndex == idx } ?: RoomSeat(
        id = "seat_$idx",
        roomId = room.id,
        roomOwnerId = room.ownerId,
        isRoomPublic = room.isPublic,
        seatIndex = idx,
      )
    }
  }
  val guestSeats = remember(seats) { seats.filter { it.seatIndex >= 2L } }
  val seatedMembers = remember(seats) { seats.filter { it.occupantUid.isNotBlank() } }
  val seatedUids = remember(seatedMembers) { seatedMembers.map { it.occupantUid }.toSet() }
  val totalSeatCount = if (seats.isNotEmpty()) seats.size else room.seatCount.toInt()
  val isCurrentUserSeated = currentUserId in seatedUids
  val hostView = LocalView.current
  DisposableEffect(hostView, isCurrentUserSeated) {
    hostView.keepScreenOn = isCurrentUserSeated
    onDispose {
      hostView.keepScreenOn = false
    }
  }
  val totalInRoom = maxOf(
    room.listenerCount,
    seatedMembers.size.toLong() + if (isCurrentUserSeated) 0L else 1L,
  )
  val audienceCount = (totalInRoom - seatedMembers.size).coerceAtLeast(0L)
  val recentListeners = remember(visibleMessages, seatedUids, currentUserId, audienceCount) {
    val maxExtra = (audienceCount - if (isCurrentUserSeated) 0L else 1L).coerceAtLeast(0L).toInt()
    if (maxExtra <= 0) {
      emptyList()
    } else {
      visibleMessages
        .asReversed()
        .filter { it.senderUid.isNotBlank() && it.senderUid !in seatedUids && it.senderUid != currentUserId }
        .distinctBy { it.senderUid }
        .take(maxExtra)
    }
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
        userCoins = userCoins,
        userProfiles = userProfiles,
        recentListeners = recentListeners,
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
    // Full-bleed atmospheric background matching Screenshot 1
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
    if (room.coverUrl.isNotBlank()) {
      AsyncImage(
        model = room.coverUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alpha = 0.55f,
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

        Surface(
          onClick = { showComposer = true },
          color = SwarCyan,
          shape = RoundedCornerShape(16.dp),
        ) {
          Text(
            text = stringResource(id = R.string.talk_pill),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF061B29),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }

        IconButton(onClick = { showSettingsDialog = true }) {
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
        CrossRoomGiftMarqueeStrip(event = latestMarqueeGift)
      }

      // Top 2 Host / Co-Host Seats Centered Side-by-Side
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 6.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        topSeats.forEach { topSeat ->
          HostSeatBadge(
            seat = topSeat,
            emptyLabel = if (topSeat.seatIndex == 0L) "होस्ट सीट 1" else "होस्ट सीट 2",
            localLevel = if (topSeat.occupantUid == currentUserId) micAudioLevel else 0f,
            activeEmoji = topSeat.occupantUid.takeIf { it.isNotBlank() }?.let { activeSeatReactions[it] },
            onClick = { openSeatOrProfileSheet(topSeat) },
          )
        }
      }

      // Multi-Seat Voice Grid (5 columns matching Screenshot 1)
      LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(guestSeats, key = { it.id.ifBlank { "seat_${it.seatIndex}" } }) { seat ->
          GuestSeatTile(
            seat = seat,
            localLevel = if (seat.occupantUid == currentUserId) micAudioLevel else 0f,
            activeEmoji = seat.occupantUid.takeIf { it.isNotBlank() }?.let { activeSeatReactions[it] },
            onClick = { openSeatOrProfileSheet(seat) },
          )
        }
      }

      // Real-time Room Chat Stream (Screenshot 1 lower overlay)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(172.dp)
          .padding(horizontal = 12.dp),
      ) {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
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
            RoomMessageBubble(
              message = msg,
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

        // Speaker Toggle
        RoomRoundActionButton(
          icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
          description = "स्पीकर टॉगल",
          tint = if (isSpeakerOn) SwarCyan else Color.White,
          containerColor = Color(0x66000000),
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

    // Full-Screen Gift Playback Overlay (video gifts play full-screen; emoji/image gifts animate full-screen)
    if (activeFullScreenGift != null) {
      FullScreenGiftOverlay(
        event = activeFullScreenGift,
        onDismiss = onDismissFullScreenGift,
      )
    }
  }
  } // End ModalNavigationDrawer

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
    val levelInfo = remember(isSelf, userCoins, remoteProfile?.coins) {
      if (isSelf) SwarUserIdentity.levelForCoins(userCoins)
      else SwarUserIdentity.levelForProfile(remoteProfile)
    }
    val isBlocked = profileTarget.uid in blockedUserIds

    ModalBottomSheet(
      onDismissRequest = { selectedProfileTarget = null },
      containerColor = Color(0xFF170E2E),
      contentColor = Color.White,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // 1. Top Circular DP
        UserAvatar(
          url = resolvedAvatar,
          name = resolvedName,
          size = 82.dp,
          borderColor = if (isSelf) SwarGold else SwarCyan,
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

        // 4. Below ID: Level Badge & XP Bar
        Surface(
          color = levelInfo.badgeColor.copy(alpha = 0.16f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, levelInfo.badgeColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.WorkspacePremium,
                  contentDescription = null,
                  tint = levelInfo.badgeColor,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = "Lv.${levelInfo.level} • ${levelInfo.title}",
                  style = MaterialTheme.typography.labelLarge,
                  color = levelInfo.badgeColor,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.testTag("sheet_user_level_badge"),
                )
              }
              Text(
                text = if (liveSeat != null) {
                  when (liveSeat.seatIndex) {
                    0L -> "होस्ट सीट 1"
                    1L -> "होस्ट सीट 2"
                    else -> "सीट #${liveSeat.seatIndex - 1L}"
                  }
                } else {
                  "लिसनर"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD0C9EC),
              )
            }
            LinearProgressIndicator(
              progress = { levelInfo.progress },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = levelInfo.badgeColor,
              trackColor = Color(0x33FFFFFF),
            )
          }
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

            if (isHost) {
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
          // Other User Bottom Controls: Kick Out + Block + Mute (+ Lock if host seat & DM)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (isHost && liveSeat != null) {
              Button(
                onClick = {
                  onKickSeatOccupant(liveSeat)
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

            if (isHost && liveSeat != null) {
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

          if (isHost && liveSeat != null) {
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
            if (sheetSeat.isLocked && isHost) {
              onToggleSeatLock(sheetSeat)
            }
            onSeatTapped(sheetSeat.copy(isLocked = false), hasAudioPermission.value) {
              audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            selectedSeatForSheet = null
          },
          enabled = !sheetSeat.isLocked || isHost,
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

        if (isHost) {
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

  // Room Settings & Audio Monitor Dialog
  if (showSettingsDialog) {
    RoomSettingsDialog(
      room = room,
      isHost = isHost,
      isMonitorLoopbackOn = isMonitorLoopbackOn,
      isAgoraConnected = isAgoraConnected,
      agoraStatusText = agoraStatusText,
      onToggleMonitorLoopback = onToggleMonitorLoopback,
      onSave = { newTitle, newTheme, newNotice ->
        onUpdateRoomSettings(newTitle, newTheme, newNotice)
        showSettingsDialog = false
      },
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
      onClaimBonusCoins = onClaimBonusCoins,
      onSendGift = onSendGift,
      onAddCustomGift = onAddCustomGift,
      onUpdateCustomGift = onUpdateCustomGift,
      onDeleteCustomGift = onDeleteCustomGift,
      onDismiss = { showGiftPanel = false },
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
  recentListeners: List<RoomMessage>,
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
        recentListeners.forEach { participant ->
          val listenerLevel = SwarUserIdentity.levelForProfile(userProfiles[participant.senderUid])
          DrawerListenerRow(
            name = participant.senderName.ifBlank { "श्रोता" },
            avatarUrl = participant.senderAvatarUrl,
            subtitle = "लिसनर • Lv.${listenerLevel.level}",
            onClick = {
              onListenerMemberClick(
                participant.senderUid,
                participant.senderName,
                participant.senderAvatarUrl,
              )
            },
          )
        }
        val namedCount = (if (selfListenerShown) 1 else 0) + recentListeners.size
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
  activeEmoji: ActiveSeatEmoji? = null,
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
              .size(72.dp)
              .scale(pulse)
              .clip(CircleShape)
              .border(2.5.dp, SwarCyan, CircleShape)
          )
        }
        UserAvatar(
          url = seat?.occupantAvatarUrl.orEmpty(),
          name = seat?.occupantName.orEmpty(),
          size = 62.dp,
          borderColor = if (speaking) SwarMint else SwarGold,
          borderWidth = 2.dp,
        )
        if (activeEmoji != null) {
          SeatEmojiReactionOverlay(reaction = activeEmoji, dpSize = 62.dp)
        }
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .size(22.dp)
            .clip(CircleShape)
            .background(if (seat?.isMuted == true) SwarCoral else SwarCyan),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = if (seat?.isMuted == true) Icons.Filled.MicOff else Icons.Filled.Mic,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(13.dp),
          )
        }
      } else {
        Box(
          modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(Color(0x44FFFFFF))
            .border(1.5.dp, SwarGold.copy(alpha = 0.7f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.Chair,
            contentDescription = if (isLocked) "लॉक होस्ट सीट" else "खाली होस्ट सीट",
            tint = Color(0xEEFFFFFF),
            modifier = Modifier.size(26.dp),
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = when {
        isOccupied -> seat?.occupantName.orEmpty()
        isLocked -> "लॉक सीट"
        else -> emptyLabel
      },
      style = MaterialTheme.typography.labelMedium,
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
  activeEmoji: ActiveSeatEmoji? = null,
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
        UserAvatar(
          url = seat.occupantAvatarUrl,
          name = seat.occupantName,
          size = 48.dp,
          borderColor = if (isSpeaking) SwarCyan else Color(0x88FFFFFF),
          borderWidth = if (isSpeaking) 2.5.dp else 1.dp,
        )
        if (activeEmoji != null) {
          SeatEmojiReactionOverlay(reaction = activeEmoji, dpSize = 48.dp)
        }
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .size(18.dp)
            .clip(CircleShape)
            .background(if (seat.isMuted) SwarCoral else SwarMint),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = if (seat.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(11.dp),
          )
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
  onClick: (() -> Unit)? = null,
) {
  Surface(
    color = Color(0x66000000),
    shape = RoundedCornerShape(14.dp),
    modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (message.badgeTag.isNotBlank()) {
        Surface(
          color = when (message.messageType) {
            "gift" -> SwarCoral
            "join" -> SwarViolet
            else -> SwarCyan.copy(alpha = 0.25f)
          },
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = message.badgeTag,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
      }

      Text(
        text = buildAnnotatedString {
          withStyle(SpanStyle(color = SwarCyan, fontWeight = FontWeight.Bold)) {
            append("${message.senderName}: ")
          }
          withStyle(SpanStyle(color = Color.White)) {
            append(message.text)
          }
        },
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
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

@Composable
private fun RoomSettingsDialog(
  room: VoiceRoom,
  isHost: Boolean,
  isMonitorLoopbackOn: Boolean,
  isAgoraConnected: Boolean,
  agoraStatusText: String,
  onToggleMonitorLoopback: () -> Unit,
  onSave: (String, String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var title by remember { mutableStateOf(room.title) }
  var theme by remember { mutableStateOf(room.backgroundTheme) }
  var notice by remember { mutableStateOf(room.welcomeNotice) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("रूम और ऑडियो सेटिंग्स") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (agoraStatusText.isNotBlank()) {
          Surface(
            color = if (isAgoraConnected) SwarMint.copy(alpha = 0.14f) else SwarCyan.copy(alpha = 0.12f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(
                1.dp,
                if (isAgoraConnected) SwarMint.copy(alpha = 0.5f) else SwarCyan.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp),
              ),
          ) {
            Text(
              text = "Agora वॉइस इंजन: $agoraStatusText",
              style = MaterialTheme.typography.bodySmall,
              color = if (isAgoraConnected) SwarMint else SwarCyan,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("माइक मॉनिटर टेस्ट (इको)", style = MaterialTheme.typography.titleSmall)
            Text(
              "बोलते समय अपनी आवाज़ हेडफ़ोन/स्पीकर में सुनें",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(checked = isMonitorLoopbackOn, onCheckedChange = { onToggleMonitorLoopback() })
        }

        if (isHost) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(60) },
            label = { Text("रूम का नाम") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = notice,
            onValueChange = { notice = it.take(200) },
            label = { Text("स्वागत संदेश") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    },
    confirmButton = {
      if (isHost) {
        Button(onClick = { onSave(title, theme, notice) }) {
          Text("सहेजें")
        }
      } else {
        TextButton(onClick = onDismiss) { Text("ठीक है") }
      }
    },
    dismissButton = {
      if (isHost) {
        TextButton(onClick = onDismiss) { Text("रद्द करें") }
      }
    },
  )
}
