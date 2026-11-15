package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.EmojiCatalogItem
import com.example.data.model.GiftCatalogItem
import com.example.data.model.GiftEvent
import com.example.data.model.RoomMessage
import com.example.data.model.RoomSeat
import com.example.data.model.VoiceRoom
import com.example.ui.components.CoinBalancePill
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
) {
  BackHandler(onBack = onLeaveRoom)

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
  var showSeatedMembersSheet by remember { mutableStateOf(false) }
  var selectedSeatForSheet by remember { mutableStateOf<RoomSeat?>(null) }

  val isHost = room.ownerId == currentUserId
  val hostSeat = seats.firstOrNull { it.seatIndex == 0L }
  val guestSeats = remember(seats) { seats.filter { it.seatIndex > 0L } }
  val seatedMembers = remember(seats) { seats.filter { it.occupantUid.isNotBlank() } }
  val seatedCount = seatedMembers.size
  val totalSeatCount = if (seats.isNotEmpty()) seats.size else room.seatCount.toInt()
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

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
        UserAvatar(
          url = room.ownerAvatarUrl,
          name = room.ownerName,
          size = 40.dp,
          borderColor = SwarGold,
          borderWidth = 1.5.dp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
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
                text = "${room.listenerCount}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD0C9EC),
                fontWeight = FontWeight.SemiBold,
              )
            }
            Text(
              text = "• सीट $seatedCount/$totalSeatCount",
              style = MaterialTheme.typography.labelSmall,
              color = SwarCyan,
              fontWeight = FontWeight.SemiBold,
            )
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
          onClick = onLeaveRoom,
          modifier = Modifier.testTag("leave_room_button"),
        ) {
          Icon(Icons.Filled.PowerSettingsNew, contentDescription = "रूम छोड़ें", tint = Color.White)
        }
      }

      // Coins, Real-Time Seated Count & Live Audio Badge Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CoinBalancePill(
          coins = userCoins,
          onAddCoinsClick = onClaimBonusCoins,
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            onClick = { showSeatedMembersSheet = true },
            color = Color(0x5512263F),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .border(1.dp, SwarCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
              .testTag("room_live_occupancy_pill"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(if (seatedCount > 0) SwarMint else Color(0xFFA098C0))
              )
              Text(
                text = "सीट पर: $seatedCount/$totalSeatCount",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }

          if (!isMicMuted) {
            Surface(
              color = SwarMint.copy(alpha = 0.22f),
              shape = RoundedCornerShape(14.dp),
            ) {
              Text(
                text = "माइक ${(micAudioLevel * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = SwarMint,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              )
            }
          }
        }
      }

      // Cross-Room Right-to-Left Gift Announcement Strip
      if (latestMarqueeGift != null) {
        CrossRoomGiftMarqueeStrip(event = latestMarqueeGift)
      }

      // Host Seat Centered (Screenshot 1)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 6.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
      ) {
        HostSeatBadge(
          seat = hostSeat,
          fallbackName = room.ownerName,
          fallbackAvatar = room.ownerAvatarUrl,
          localLevel = if (hostSeat?.occupantUid == currentUserId) micAudioLevel else 0f,
          onClick = {
            hostSeat?.let {
              if (it.occupantUid == currentUserId || it.occupantUid.isEmpty()) {
                onSeatTapped(it, hasAudioPermission.value) {
                  audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              } else {
                selectedSeatForSheet = it
              }
            }
          },
        )
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
            onClick = {
              if (seat.occupantUid.isEmpty() && !seat.isLocked) {
                onSeatTapped(seat, hasAudioPermission.value) {
                  audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              } else {
                selectedSeatForSheet = seat
              }
            },
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
          items(messages, key = { it.id.ifBlank { "${it.senderUid}_${it.text.hashCode()}" } }) { msg ->
            RoomMessageBubble(message = msg)
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

  // Seat Options Bottom Sheet
  val sheetSeat = selectedSeatForSheet
  if (sheetSeat != null) {
    ModalBottomSheet(onDismissRequest = { selectedSeatForSheet = null }) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        val label = if (sheetSeat.seatIndex == 0L) "होस्ट सीट" else "सीट #${sheetSeat.seatIndex}"
        Text(
          text = if (sheetSeat.occupantName.isNotBlank()) "$label • ${sheetSeat.occupantName}" else label,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )

        if (sheetSeat.occupantUid == currentUserId) {
          Button(
            onClick = {
              onToggleMic(hasAudioPermission.value) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
              selectedSeatForSheet = null
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(if (isMicMuted) "माइक अनम्यूट करें" else "माइक म्यूट करें")
          }
          if (sheetSeat.seatIndex != 0L) {
            OutlinedButton(
              onClick = {
                onLeaveSeat()
                selectedSeatForSheet = null
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("सीट छोड़ें")
            }
          }
        } else {
          if (sheetSeat.occupantUid.isNotBlank()) {
            Button(
              onClick = {
                onStartDirectChat(sheetSeat.occupantUid, sheetSeat.occupantName, sheetSeat.occupantAvatarUrl)
                selectedSeatForSheet = null
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("${sheetSeat.occupantName} को निजी संदेश भेजें")
            }
          }
          if (isHost && sheetSeat.seatIndex != 0L) {
            OutlinedButton(
              onClick = {
                onToggleSeatLock(sheetSeat)
                selectedSeatForSheet = null
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(
                imageVector = if (sheetSeat.isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = null,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(if (sheetSeat.isLocked) "सीट अनलॉक करें" else "सीट लॉक करें")
            }
            if (sheetSeat.occupantUid.isNotBlank()) {
              TextButton(
                onClick = {
                  onSeatTapped(sheetSeat, hasAudioPermission.value) {}
                  selectedSeatForSheet = null
                },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text("सीट से हटाएं", color = SwarCoral)
              }
            }
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
      onToggleMonitorLoopback = onToggleMonitorLoopback,
      onSave = { newTitle, newTheme, newNotice ->
        onUpdateRoomSettings(newTitle, newTheme, newNotice)
        showSettingsDialog = false
      },
      onDismiss = { showSettingsDialog = false },
    )
  }

  // Real-time Seated Members Panel Sheet
  if (showSeatedMembersSheet) {
    ModalBottomSheet(onDismissRequest = { showSeatedMembersSheet = false }) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "लाइव रूम पैनल • सीट पर बैठे: $seatedCount/$totalSeatCount",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "रूम में कुल सक्रिय सदस्य: ${room.listenerCount}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (seatedMembers.isEmpty()) {
          Text(
            text = "अभी कोई भी सदस्य सीट पर नहीं बैठा है। किसी खाली सीट पर टैप करके बैठें!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
          )
        } else {
          seatedMembers.forEach { memberSeat ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                UserAvatar(
                  url = memberSeat.occupantAvatarUrl,
                  name = memberSeat.occupantName,
                  size = 38.dp,
                  borderColor = if (memberSeat.seatIndex == 0L) SwarGold else SwarCyan,
                  borderWidth = 1.5.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = memberSeat.occupantName.ifBlank { "सदस्य" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = if (memberSeat.seatIndex == 0L) "होस्ट सीट #0" else "सीट #${memberSeat.seatIndex}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Icon(
                  imageVector = if (memberSeat.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                  contentDescription = if (memberSeat.isMuted) "म्यूट" else "माइक चालू",
                  tint = if (memberSeat.isMuted) SwarCoral else SwarMint,
                )
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }
    }
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
}

@Composable
private fun HostSeatBadge(
  seat: RoomSeat?,
  fallbackName: String,
  fallbackAvatar: String,
  localLevel: Float,
  onClick: () -> Unit,
) {
  val speaking = seat?.isSpeaking == true || localLevel > 0.08f
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
      .padding(4.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
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
        url = seat?.occupantAvatarUrl?.ifBlank { fallbackAvatar } ?: fallbackAvatar,
        name = seat?.occupantName?.ifBlank { fallbackName } ?: fallbackName,
        size = 62.dp,
        borderColor = if (speaking) SwarMint else SwarGold,
        borderWidth = 2.dp,
      )
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
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = seat?.occupantName?.ifBlank { fallbackName } ?: fallbackName,
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
      text = if (isOccupied) seat.occupantName else seat.seatIndex.toString(),
      style = MaterialTheme.typography.labelSmall,
      color = Color.White,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun RoomMessageBubble(message: RoomMessage) {
  Surface(
    color = Color(0x66000000),
    shape = RoundedCornerShape(14.dp),
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
