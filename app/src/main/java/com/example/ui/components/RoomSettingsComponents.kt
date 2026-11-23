package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.audio.RoomMusicState
import com.example.data.model.RoomKickLog
import com.example.data.model.RoomPresenceMember
import com.example.data.model.RoomSeat
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarMint
import com.example.ui.theme.SwarViolet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoomSettingsSheet(
  room: VoiceRoom,
  isHost: Boolean,
  isUploadingMedia: Boolean,
  isMonitorLoopbackOn: Boolean,
  isAgoraConnected: Boolean,
  agoraStatusText: String,
  userProfiles: Map<String, UserProfile>,
  livePresenceMembers: List<RoomPresenceMember>,
  seats: List<RoomSeat>,
  kickLogs: List<RoomKickLog>,
  roomMusicState: RoomMusicState,
  onToggleMonitorLoopback: () -> Unit,
  onUpdateIdentity: (title: String, welcomeNotice: String, newCoverUri: Uri?, newCoverLink: String) -> Unit,
  onUpdateTheme: (backgroundTheme: String, customThemeUri: Uri?, customThemeLink: String) -> Unit,
  onAddManagerById: (String) -> Unit,
  onRemoveManager: (String) -> Unit,
  onUpdateRoomPinLock: (Boolean, String) -> Unit,
  onResizeRoomSeats: (Int) -> Unit,
  onPickMusicUri: (Uri) -> Unit,
  onToggleMusicPlayPause: () -> Unit,
  onStopMusic: () -> Unit,
  onMusicVolumeChange: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var title by remember(room.id, room.title) { mutableStateOf(room.title) }
  var welcomeNotice by remember(room.id, room.welcomeNotice) { mutableStateOf(room.welcomeNotice) }
  var pendingCoverUri by remember(room.id) { mutableStateOf<Uri?>(null) }
  var coverLinkInput by remember(room.id) { mutableStateOf("") }

  var selectedTheme by remember(room.id, room.backgroundTheme) { mutableStateOf(room.backgroundTheme) }
  var pendingThemeUri by remember(room.id) { mutableStateOf<Uri?>(null) }
  var themeLinkInput by remember(room.id, room.themeBackgroundUrl) { mutableStateOf(room.themeBackgroundUrl) }

  val coverPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      pendingCoverUri = uri
      coverLinkInput = ""
    }
  }
  val themePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      pendingThemeUri = uri
      selectedTheme = "custom"
      themeLinkInput = ""
    }
  }
  val musicPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      onPickMusicUri(uri)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF150E2A),
    contentColor = Color.White,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 660.dp)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .testTag("room_settings_sheet"),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.Tune, contentDescription = null, tint = SwarCyan)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "रूम कंट्रोल स्टूडियो",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("room_settings_close_button"),
        ) {
          Icon(Icons.Filled.Close, contentDescription = "बंद करें", tint = Color.White)
        }
      }

      if (isUploadingMedia) {
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth(),
          color = SwarCyan,
          trackColor = Color(0x33FFFFFF),
        )
      }

      // 1. Top Section — Room DP & Room Name
      RoomStudioCard(
        title = "रूम DP और नाम",
        icon = Icons.Filled.CameraAlt,
        accent = SwarCyan,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .border(2.dp, SwarGold, CircleShape)
              .clickable(enabled = isHost) {
                coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              }
              .testTag("room_settings_dp_picker"),
            contentAlignment = Alignment.Center,
          ) {
            val previewModel = pendingCoverUri ?: coverLinkInput.ifBlank { room.coverUrl.ifBlank { room.ownerAvatarUrl } }
            if (previewModel.toString().isNotBlank()) {
              AsyncImage(
                model = previewModel,
                contentDescription = "रूम DP",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
              )
            } else {
              UserAvatar(url = "", name = room.title, size = 72.dp)
            }
            if (isHost) {
              Box(
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .fillMaxWidth()
                  .background(Color(0xAA000000))
                  .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text("बदलें", style = MaterialTheme.typography.labelSmall, color = SwarGold)
              }
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = room.title,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = if (isHost) "रूम DP बदलने के लिए फोटो पर टैप करें या लिंक डालें" else "रूम ID: ${room.roomCode}",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFD0C9EC),
            )
          }
        }

        if (isHost) {
          OutlinedTextField(
            value = coverLinkInput,
            onValueChange = {
              coverLinkInput = it
              if (it.isNotBlank()) pendingCoverUri = null
            },
            label = { Text("रूम DP फोटो लिंक (वैकल्पिक)") },
            singleLine = true,
            colors = roomStudioTextFieldColors(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("room_settings_dp_link"),
          )
          OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(60) },
            label = { Text("रूम का नाम") },
            singleLine = true,
            colors = roomStudioTextFieldColors(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("room_settings_name_input"),
          )
          OutlinedTextField(
            value = welcomeNotice,
            onValueChange = { welcomeNotice = it.take(200) },
            label = { Text("स्वागत संदेश") },
            maxLines = 2,
            colors = roomStudioTextFieldColors(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("room_settings_notice_input"),
          )
          Button(
            onClick = { onUpdateIdentity(title, welcomeNotice, pendingCoverUri, coverLinkInput) },
            enabled = !isUploadingMedia && title.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = SwarCyan, contentColor = Color(0xFF041A24)),
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("room_settings_save_identity_button"),
          ) {
            Text("DP व नाम सेव करें", fontWeight = FontWeight.Bold)
          }
        }
      }

      // 2. Room Themes (Custom Background Themes for Everyone in Room)
      if (isHost) {
        RoomStudioCard(
          title = "रूम बैकग्राउंड थीम्स",
          icon = Icons.Filled.Palette,
          accent = SwarViolet,
        ) {
          val presets = listOf(
            "stadium" to "स्टेडियम",
            "aurora" to "ऑरोरा",
            "neon" to "नियॉन",
            "sunset" to "सनसेट",
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            presets.forEach { (key, label) ->
              FilterChip(
                selected = selectedTheme == key && pendingThemeUri == null && themeLinkInput.isBlank(),
                onClick = {
                  selectedTheme = key
                  pendingThemeUri = null
                  themeLinkInput = ""
                },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SwarViolet,
                  selectedLabelColor = Color.White,
                  containerColor = Color(0x22FFFFFF),
                  labelColor = Color(0xFFD0C9EC),
                ),
                modifier = Modifier.testTag("room_settings_theme_chip_$key"),
              )
            }
          }

          val themePreview: Any? = pendingThemeUri ?: themeLinkInput.takeIf { it.isNotBlank() }
          if (themePreview != null) {
            AsyncImage(
              model = themePreview,
              contentDescription = "कस्टम रूम थीम प्रीव्यू",
              modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, SwarCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
              contentScale = ContentScale.Crop,
            )
          }

          OutlinedButton(
            onClick = {
              themePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            border = BorderStroke(1.dp, SwarViolet.copy(alpha = 0.7f)),
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("room_settings_theme_photo_picker"),
          ) {
            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("गैलरी से कस्टम थीम फोटो / GIF अपलोड करें", color = Color.White)
          }

          OutlinedTextField(
            value = themeLinkInput,
            onValueChange = {
              themeLinkInput = it
              if (it.isNotBlank()) {
                pendingThemeUri = null
                selectedTheme = "custom"
              }
            },
            label = { Text("या कस्टम थीम फोटो/GIF लिंक डालें") },
            singleLine = true,
            colors = roomStudioTextFieldColors(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("room_settings_theme_link_input"),
          )

          Button(
            onClick = { onUpdateTheme(selectedTheme, pendingThemeUri, themeLinkInput) },
            enabled = !isUploadingMedia,
            colors = ButtonDefaults.buttonColors(containerColor = SwarViolet, contentColor = Color.White),
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("room_settings_apply_theme_button"),
          ) {
            Text("रूम थीम सभी को दिखाएं (Apply)", fontWeight = FontWeight.Bold)
          }
        }
      }

      // 3. Room Managers (Max 5 per room)
      RoomManagersSection(
        room = room,
        isHost = isHost,
        userProfiles = userProfiles,
        livePresenceMembers = livePresenceMembers,
        seats = seats,
        onAddManagerById = onAddManagerById,
        onRemoveManager = onRemoveManager,
      )

      // 4. Admin / Kick Audit Panel
      RoomKickAuditSection(kickLogs = kickLogs)

      // 5. Memory Card Music Player (Zero-Permission OpenDocument)
      RoomMusicSection(
        roomMusicState = roomMusicState,
        onPickMusicClick = { musicPicker.launch(arrayOf("audio/*")) },
        onTogglePlayPause = onToggleMusicPlayPause,
        onStopMusic = onStopMusic,
        onVolumeChange = onMusicVolumeChange,
      )

      // 6 & 7. Room PIN Lock & Dynamic Seat Count
      RoomPinAndSeatCountSections(
        room = room,
        isHost = isHost,
        onUpdateRoomPinLock = onUpdateRoomPinLock,
        onResizeRoomSeats = onResizeRoomSeats,
      )

      // 8. Agora Voice & Mic Echo Loopback
      RoomStudioCard(
        title = "ऑडियो और माइक मॉनिटर",
        icon = Icons.Filled.GraphicEq,
        accent = SwarCyan,
      ) {
        if (agoraStatusText.isNotBlank()) {
          Surface(
            color = if (isAgoraConnected) SwarMint.copy(alpha = 0.16f) else SwarCyan.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "Agora वॉइस इंजन: $agoraStatusText",
              style = MaterialTheme.typography.bodySmall,
              color = if (isAgoraConnected) SwarMint else SwarCyan,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(10.dp),
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("माइक मॉनिटर टेस्ट (इको)", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(
              "बोलते समय अपनी आवाज़ हेडफ़ोन/स्पीकर में सुनें",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFD0C9EC),
            )
          }
          Switch(
            checked = isMonitorLoopbackOn,
            onCheckedChange = { onToggleMonitorLoopback() },
            modifier = Modifier.testTag("room_settings_echo_switch"),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}

@Composable
fun RoomPinEntryDialog(
  roomTitle: String,
  expectedPin: String,
  onSuccessPin: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var pinInput by remember { mutableStateOf("") }
  var wrongPinError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF1A1132),
    titleContentColor = Color.White,
    textContentColor = Color.White,
    icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = SwarGold) },
    title = { Text("पिन कोड दर्ज करें", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "“$roomTitle” रूम लॉक है। प्रवेश करने के लिए होस्ट द्वारा सेट किया गया पिन कोड डालें।",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFD0C9EC),
        )
        OutlinedTextField(
          value = pinInput,
          onValueChange = { raw ->
            pinInput = raw.filter { it.isDigit() }.take(8)
            wrongPinError = false
          },
          label = { Text("रूम पिन कोड") },
          singleLine = true,
          isError = wrongPinError,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          colors = roomStudioTextFieldColors(),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("room_pin_entry_input"),
        )
        if (wrongPinError) {
          Text(
            text = "गलत पिन कोड! कृपया सही पिन डालें।",
            style = MaterialTheme.typography.labelSmall,
            color = SwarCoral,
            modifier = Modifier.testTag("room_pin_error_text"),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (pinInput.isNotBlank() && pinInput == expectedPin) {
            onSuccessPin(pinInput)
          } else {
            wrongPinError = true
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = SwarGold, contentColor = Color(0xFF1B1201)),
        modifier = Modifier.testTag("room_pin_submit_button"),
      ) {
        Text("प्रवेश करें", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("room_pin_cancel_button"),
      ) {
        Text("रद्द करें", color = Color(0xFFD0C9EC))
      }
    },
  )
}
