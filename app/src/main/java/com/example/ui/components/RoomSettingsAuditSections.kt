package com.example.ui.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.DeviceMusicScanner
import com.example.audio.DeviceMusicTrack
import com.example.audio.RoomMusicState
import com.example.data.model.RoomKickLog
import com.example.data.model.RoomPresenceMember
import com.example.data.model.RoomSeat
import com.example.data.model.SwarUserIdentity
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarMint
import com.example.ui.theme.SwarViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RoomManagersSection(
  room: VoiceRoom,
  isHost: Boolean,
  userProfiles: Map<String, UserProfile>,
  livePresenceMembers: List<RoomPresenceMember>,
  seats: List<RoomSeat>,
  onAddManagerById: (String) -> Unit,
  onRemoveManager: (String) -> Unit,
) {
  var managerIdInput by remember { mutableStateOf("") }
  val candidateMembers = remember(livePresenceMembers, seats, room.ownerId, room.managerUids, userProfiles) {
    val map = linkedMapOf<String, Pair<String, String>>()
    livePresenceMembers.forEach { member ->
      val uid = member.userId
      if (uid.isNotBlank() && uid != room.ownerId && uid !in room.managerUids) {
        map[uid] = member.displayName to member.avatarUrl
      }
    }
    seats.forEach { seat ->
      val uid = seat.occupantUid
      if (uid.isNotBlank() && uid != room.ownerId && uid !in room.managerUids && uid !in map) {
        map[uid] = seat.occupantName to seat.occupantAvatarUrl
      }
    }
    map.entries.map { (uid, info) ->
      val profName = userProfiles[uid]?.displayName?.ifBlank { info.first } ?: info.first
      Triple(uid, profName.ifBlank { "सदस्य" }, SwarUserIdentity.numericIdForUid(uid))
    }.take(6)
  }

  RoomStudioCard(
    title = "रूम मैनेजर (${room.managerUids.size}/5)",
    icon = Icons.Filled.AdminPanelSettings,
    accent = SwarViolet,
  ) {
    Text(
      text = "अधिकतम 5 मैनेजर बन सकते हैं • अधिकार: सीट लॉक/अनलॉक, म्यूट/अनम्यूट और किक आउट",
      style = MaterialTheme.typography.bodySmall,
      color = Color(0xFFD0C9EC),
    )

    if (isHost) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = managerIdInput,
          onValueChange = { raw -> managerIdInput = raw.filter { it.isDigit() }.take(10) },
          label = { Text("10-अंकीय यूज़र ID नंबर") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = roomStudioTextFieldColors(),
          modifier = Modifier
            .weight(1f)
            .testTag("room_settings_manager_id_input"),
        )
        Button(
          onClick = {
            if (managerIdInput.length == 10) {
              onAddManagerById(managerIdInput)
              managerIdInput = ""
            }
          },
          enabled = managerIdInput.length == 10 && room.managerUids.size < 5,
          colors = ButtonDefaults.buttonColors(containerColor = SwarCyan, contentColor = Color(0xFF041A24)),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("room_settings_manager_accept_button"),
        ) {
          Text("एक्सेप्ट", fontWeight = FontWeight.Bold)
        }
      }

      if (candidateMembers.isNotEmpty() && room.managerUids.size < 5) {
        Text(
          text = "रूम के सक्रिय सदस्यों से चुनें:",
          style = MaterialTheme.typography.labelSmall,
          color = SwarCyan,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          candidateMembers.forEach { (uid, name, numericId) ->
            FilterChip(
              selected = false,
              onClick = { managerIdInput = numericId },
              label = { Text("$name ($numericId)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
              colors = FilterChipDefaults.filterChipColors(
                containerColor = Color(0x28FFFFFF),
                labelColor = Color.White,
              ),
              modifier = Modifier.testTag("room_settings_manager_candidate_$uid"),
            )
          }
        }
      }
    }

    if (room.managerUids.isEmpty()) {
      Text(
        text = "अभी कोई रूम मैनेजर नियुक्त नहीं है।",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFAFA7CF),
      )
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        room.managerUids.forEach { mgrUid ->
          val profile = userProfiles[mgrUid]
          val seatMatch = seats.firstOrNull { it.occupantUid == mgrUid }
          val presMatch = livePresenceMembers.firstOrNull { it.userId == mgrUid }
          val name = profile?.displayName?.ifBlank { "" }
            ?: seatMatch?.occupantName?.ifBlank { "" }
            ?: presMatch?.displayName?.ifBlank { "" }
            ?: "मैनेजर"
          val avatar = profile?.avatarUrl?.ifBlank { "" }
            ?: seatMatch?.occupantAvatarUrl?.ifBlank { "" }
            ?: presMatch?.avatarUrl.orEmpty()
          val numericId = SwarUserIdentity.numericIdForUid(mgrUid)
          Surface(
            color = Color(0x22FFFFFF),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("room_settings_manager_row_$mgrUid"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              UserAvatar(url = avatar, name = name, size = 36.dp, borderColor = SwarViolet)
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("ID: $numericId", style = MaterialTheme.typography.labelSmall, color = SwarCyan)
              }
              if (isHost) {
                IconButton(
                  onClick = { onRemoveManager(mgrUid) },
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("room_settings_remove_manager_$mgrUid"),
                ) {
                  Icon(Icons.Filled.Close, contentDescription = "मैनेजर हटाएं", tint = SwarCoral)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun RoomKickAuditSection(kickLogs: List<RoomKickLog>) {
  RoomStudioCard(
    title = "एडमिन व किक पैनल (${kickLogs.size})",
    icon = Icons.Filled.Gavel,
    accent = SwarCoral,
  ) {
    if (kickLogs.isEmpty()) {
      Text(
        text = "अभी तक इस रूम में कोई किक आउट रिकॉर्ड नहीं है।",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFAFA7CF),
        modifier = Modifier.testTag("room_settings_kick_logs_empty"),
      )
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        kickLogs.take(25).forEach { log ->
          val roleLabel = if (log.actorRole == "owner") "होस्ट" else "मैनेजर"
          val formattedTime = remember(log.kickedAtMs) { formatKickTimestamp(log.kickedAtMs) }
          Surface(
            color = Color(0x22FFFFFF),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SwarCoral.copy(alpha = 0.35f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("room_settings_kick_log_${log.id}"),
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                  url = log.actorAvatarUrl,
                  name = log.actorName.ifBlank { roleLabel },
                  size = 32.dp,
                  borderColor = SwarGold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = log.actorName.ifBlank { roleLabel },
                      style = MaterialTheme.typography.labelLarge,
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      color = if (log.actorRole == "owner") SwarGold.copy(alpha = 0.25f) else SwarViolet.copy(alpha = 0.45f),
                      shape = RoundedCornerShape(6.dp),
                    ) {
                      Text(
                        text = roleLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (log.actorRole == "owner") SwarGold else Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                  }
                  Text(
                    text = "एडमिन ID: ${log.actorNumericId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD0C9EC),
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "ने किक किया →",
                  style = MaterialTheme.typography.labelSmall,
                  color = SwarCoral,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                UserAvatar(
                  url = log.targetAvatarUrl,
                  name = log.targetName.ifBlank { "सदस्य" },
                  size = 28.dp,
                  borderColor = SwarCoral,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = log.targetName.ifBlank { "सदस्य" },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                  )
                  Text(
                    text = "यूज़र ID: ${log.targetNumericId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SwarCyan,
                  )
                }
              }

              Text(
                text = "समय: $formattedTime • ${log.reason.ifBlank { "रूम से किक आउट" }}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFC8C1E8),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun RoomMusicSection(
  roomMusicState: RoomMusicState,
  scannedTracksCount: Int = 0,
  isScanningMusic: Boolean = false,
  onPickMusicClick: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onStopMusic: () -> Unit,
  onVolumeChange: (Int) -> Unit,
  onNextTrack: () -> Unit = {},
  onPreviousTrack: () -> Unit = {},
) {
  RoomStudioCard(
    title = "मेमोरी कार्ड रूम संगीत",
    icon = Icons.Filled.MusicNote,
    accent = SwarMint,
  ) {
    Button(
      onClick = onPickMusicClick,
      colors = ButtonDefaults.buttonColors(containerColor = SwarMint, contentColor = Color(0xFF042218)),
      modifier = Modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .testTag("room_settings_pick_music_button"),
    ) {
      if (isScanningMusic) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          strokeWidth = 2.dp,
          color = Color(0xFF042218),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("मेमोरी कार्ड से म्यूजिक खींचा जा रहा है...", fontWeight = FontWeight.Bold)
      } else {
        Icon(Icons.Filled.SdStorage, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (scannedTracksCount > 0) {
            "मेमोरी कार्ड म्यूजिक ($scannedTracksCount गाने)"
          } else {
            "मेमोरी कार्ड से सभी म्यूजिक खींचें"
          },
          fontWeight = FontWeight.Bold,
        )
      }
    }

    if (roomMusicState.isPrepared) {
      Surface(
        color = Color(0x24FFFFFF),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "🎵 ${roomMusicState.trackTitle.ifBlank { "चयनित ऑडियो" }}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = if (roomMusicState.usingRoomBroadcast) {
              "Agora रूम ब्रॉडकास्ट सक्रिय (सभी श्रोताओं को सुनाई दे रहा है)"
            } else {
              "लोकल प्लेयर सक्रिय (माइक ऑन होने पर रूम में ब्रॉडकास्ट)"
            },
            style = MaterialTheme.typography.labelSmall,
            color = SwarCyan,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (scannedTracksCount > 1) {
              OutlinedButton(
                onClick = onPreviousTrack,
                border = BorderStroke(1.dp, SwarCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("room_settings_music_prev"),
              ) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "पिछला गाना", tint = Color.White)
              }
            }
            OutlinedButton(
              onClick = onTogglePlayPause,
              border = BorderStroke(1.dp, SwarCyan.copy(alpha = 0.6f)),
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("room_settings_music_play_pause"),
            ) {
              Icon(
                imageVector = if (roomMusicState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (roomMusicState.isPlaying) "रोकें" else "चलाएं", color = Color.White)
            }
            if (scannedTracksCount > 1) {
              OutlinedButton(
                onClick = onNextTrack,
                border = BorderStroke(1.dp, SwarCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("room_settings_music_next"),
              ) {
                Icon(Icons.Filled.SkipNext, contentDescription = "अगला गाना", tint = Color.White)
              }
            }
            OutlinedButton(
              onClick = onStopMusic,
              border = BorderStroke(1.dp, SwarCoral.copy(alpha = 0.6f)),
              modifier = Modifier
                .weight(0.9f)
                .minimumInteractiveComponentSize()
                .testTag("room_settings_music_stop"),
            ) {
              Icon(Icons.Filled.Stop, contentDescription = null, tint = SwarCoral)
              Spacer(modifier = Modifier.width(4.dp))
              Text("बंद", color = SwarCoral)
            }
          }
          Text(
            text = "आवाज़ (Volume): ${roomMusicState.volumePercent}%",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFD0C9EC),
          )
          Slider(
            value = roomMusicState.volumePercent.toFloat(),
            onValueChange = { onVolumeChange(it.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
              thumbColor = SwarMint,
              activeTrackColor = SwarMint,
              inactiveTrackColor = Color(0x44FFFFFF),
            ),
            modifier = Modifier.testTag("room_settings_music_volume_slider"),
          )
        }
      }
    }
  }
}

@Composable
internal fun MemoryCardMusicLibraryDialog(
  tracks: List<DeviceMusicTrack>,
  isScanning: Boolean,
  hasAudioPermission: Boolean,
  activeTrackUri: String,
  isPlaying: Boolean,
  onRescan: () -> Unit,
  onRequestAudioPermission: () -> Unit,
  onPickFolderTreeClick: () -> Unit,
  onPickSingleFileClick: () -> Unit,
  onSelectTrack: (DeviceMusicTrack) -> Unit,
  onDismiss: () -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  var sdCardOnly by remember { mutableStateOf(false) }

  val filteredTracks = remember(tracks, searchQuery, sdCardOnly) {
    val q = searchQuery.trim()
    tracks.filter { track ->
      val matchesSource = !sdCardOnly || track.isFromSdCard
      val matchesQuery = q.isBlank() ||
        track.title.contains(q, ignoreCase = true) ||
        track.artist.contains(q, ignoreCase = true) ||
        track.folderName.contains(q, ignoreCase = true)
      matchesSource && matchesQuery
    }
  }
  val sdCount = remember(tracks) { tracks.count { it.isFromSdCard } }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = Color(0xFF150E2A),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 20.dp)
        .widthIn(max = 460.dp)
        .border(1.dp, SwarMint.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
        .testTag("memory_card_music_dialog"),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 14.dp),
      ) {
        // Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(listOf(Color(0xFF0A4032), Color(0xFF1A2256)))
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Filled.SdStorage, contentDescription = null, tint = SwarMint, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "मेमोरी कार्ड म्यूजिक लाइब्रेरी",
              style = MaterialTheme.typography.titleSmall,
              color = Color.White,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text = "${tracks.size} गाने स्वचालित मिले",
              style = MaterialTheme.typography.labelSmall,
              color = SwarMint,
            )
          }
          IconButton(
            onClick = onRescan,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("music_rescan_button"),
          ) {
            Icon(Icons.Filled.Refresh, contentDescription = "दोबारा स्कैन करें", tint = SwarCyan)
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("close_music_library_button"),
          ) {
            Icon(Icons.Filled.Close, contentDescription = "बंद करें", tint = Color.White)
          }
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (!hasAudioPermission) {
            Surface(
              color = Color(0x33FFB300),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SwarGold.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = "सभी गाने अपने-आप खींचने के लिए ऑडियो अनुमति दें या नीचे SD कार्ड फ़ोल्डर चुनें।",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
                  modifier = Modifier.weight(1f),
                )
                Button(
                  onClick = onRequestAudioPermission,
                  colors = ButtonDefaults.buttonColors(containerColor = SwarGold, contentColor = Color(0xFF2B1C00)),
                  modifier = Modifier.testTag("music_grant_permission_button"),
                ) {
                  Text("अनुमति दें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("गाना, गायक या फ़ोल्डर खोजें...", color = Color(0xFF9F96BF)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = SwarCyan) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = SwarMint,
              unfocusedBorderColor = Color(0x44FFFFFF),
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("music_library_search_input"),
          )

          if (sdCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = !sdCardOnly,
                onClick = { sdCardOnly = false },
                label = { Text("सभी (${tracks.size})") },
              )
              FilterChip(
                selected = sdCardOnly,
                onClick = { sdCardOnly = true },
                label = { Text("SD कार्ड ($sdCount)") },
              )
            }
          }

          if (isScanning) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              CircularProgressIndicator(color = SwarMint)
              Text(
                text = "मेमोरी कार्ड व स्टोरेज से सभी म्यूजिक स्कैन हो रहे हैं...",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD0C9EC),
                textAlign = TextAlign.Center,
              )
            }
          } else if (filteredTracks.isEmpty()) {
            Surface(
              color = Color(0x1EFFFFFF),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("music_library_empty_state"),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = SwarMint, modifier = Modifier.size(32.dp))
                Text(
                  text = if (tracks.isEmpty()) "कोई म्यूजिक ट्रैक नहीं मिला" else "खोज के अनुसार कोई गाना नहीं मिला",
                  style = MaterialTheme.typography.titleSmall,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                )
                Text(
                  text = "आप 'SD कार्ड फ़ोल्डर' चुनकर किसी भी मेमोरी कार्ड फ़ोल्डर के सभी गाने एक साथ खींच सकते हैं।",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFFC8C1E8),
                  textAlign = TextAlign.Center,
                )
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 310.dp)
                .testTag("music_library_list"),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              items(filteredTracks, key = { it.id }) { track ->
                val isCurrent = activeTrackUri == track.uri.toString()
                Surface(
                  color = if (isCurrent) Color(0x3300E5B4) else Color(0x1BFFFFFF),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .border(
                      width = if (isCurrent) 1.3.dp else 0.8.dp,
                      color = if (isCurrent) SwarMint else Color(0x28FFFFFF),
                      shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelectTrack(track) }
                    .testTag("music_track_row_${track.id}"),
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Box(
                      modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent) SwarMint else Color(0xFF2D2159)),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = if (isCurrent && isPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = if (isCurrent) Color(0xFF042218) else SwarCyan,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      val subParts = listOfNotNull(
                        track.artist.takeIf { it.isNotBlank() },
                        track.folderName.takeIf { it.isNotBlank() },
                      ).joinToString(" • ")
                      Text(
                        text = subParts.ifBlank { "मेमोरी कार्ड ऑडियो" },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (track.isFromSdCard) SwarMint else Color(0xFFBDB4DF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    Text(
                      text = DeviceMusicScanner.formatDurationClock(track.durationMs),
                      style = MaterialTheme.typography.labelSmall,
                      color = SwarCyan,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = onPickFolderTreeClick,
              border = BorderStroke(1.dp, SwarCyan.copy(alpha = 0.65f)),
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("music_pick_folder_button"),
            ) {
              Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = SwarCyan, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("SD फ़ोल्डर", color = Color.White, fontSize = 12.sp, maxLines = 1)
            }
            TextButton(
              onClick = onPickSingleFileClick,
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("music_pick_file_button"),
            ) {
              Icon(Icons.Filled.AudioFile, contentDescription = null, tint = SwarMint, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("फ़ाइल चुनें", color = SwarMint, fontSize = 12.sp, maxLines = 1)
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun RoomPinAndSeatCountSections(
  room: VoiceRoom,
  isHost: Boolean,
  onUpdateRoomPinLock: (Boolean, String) -> Unit,
  onResizeRoomSeats: (Int) -> Unit,
) {
  var pinInput by remember(room.id, room.roomPin) { mutableStateOf(room.roomPin) }
  var seatCountInput by remember(room.id, room.seatCount) {
    mutableStateOf(room.seatCount.coerceIn(1L, 22L).toString())
  }

  RoomStudioCard(
    title = "रूम पिन लॉक",
    icon = if (room.isPinLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
    accent = SwarGold,
  ) {
    val statusText = if (room.isPinLocked && room.roomPin.isNotBlank()) {
      "🔒 लॉक सक्रिय (रूम में प्रवेश के लिए पिन ज़रूरी)"
    } else {
      "🔓 अनलॉक (सभी के लिए खुला)"
    }
    Text(text = statusText, style = MaterialTheme.typography.bodySmall, color = SwarGold)

    if (isHost) {
      OutlinedTextField(
        value = pinInput,
        onValueChange = { raw -> pinInput = raw.filter { it.isDigit() }.take(8) },
        label = { Text("रूम पिन कोड (4-8 अंक)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        colors = roomStudioTextFieldColors(),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("room_settings_pin_input"),
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = { onUpdateRoomPinLock(true, pinInput) },
          enabled = pinInput.length in 4..8,
          colors = ButtonDefaults.buttonColors(containerColor = SwarGold, contentColor = Color(0xFF1D1402)),
          modifier = Modifier
            .weight(1f)
            .minimumInteractiveComponentSize()
            .testTag("room_settings_pin_toggle_button"),
        ) {
          Text(if (room.isPinLocked) "पिन अपडेट / लॉक रखें" else "पिन लॉक लगाएं", fontWeight = FontWeight.Bold)
        }
        if (room.isPinLocked) {
          OutlinedButton(
            onClick = { onUpdateRoomPinLock(false, "") },
            border = BorderStroke(1.dp, SwarCoral.copy(alpha = 0.7f)),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("room_settings_pin_unlock_button"),
          ) {
            Text("अनलॉक", color = SwarCoral, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }

  if (isHost) {
    RoomStudioCard(
      title = "सीट संख्या कम / ज़्यादा (अभी: ${room.seatCount.coerceIn(1L, 22L)})",
      icon = Icons.Filled.EventSeat,
      accent = SwarCyan,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        listOf(4, 8, 12, 16, 20, 22).forEach { count ->
          val selected = seatCountInput == count.toString()
          FilterChip(
            selected = selected,
            onClick = { seatCountInput = count.toString() },
            label = { Text("$count") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SwarCyan,
              selectedLabelColor = Color(0xFF041A24),
              containerColor = Color(0x22FFFFFF),
              labelColor = Color.White,
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("room_settings_seat_preset_$count"),
          )
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = seatCountInput,
          onValueChange = { raw -> seatCountInput = raw.filter { it.isDigit() }.take(2) },
          label = { Text("सीट संख्या लिखें (1–22)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = roomStudioTextFieldColors(),
          modifier = Modifier
            .weight(1f)
            .testTag("room_settings_seat_count_input"),
        )
        val parsedCount = seatCountInput.toIntOrNull()
        Button(
          onClick = { if (parsedCount != null) onResizeRoomSeats(parsedCount.coerceIn(1, 22)) },
          enabled = parsedCount != null && parsedCount in 1..22,
          colors = ButtonDefaults.buttonColors(containerColor = SwarCyan, contentColor = Color(0xFF041A24)),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("room_settings_seat_count_apply_button"),
        ) {
          Text("लागू करें", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
internal fun RoomStudioCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accent: Color,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0x1CFFFFFF), RoundedCornerShape(16.dp))
      .border(1.dp, accent.copy(alpha = 0.38f), RoundedCornerShape(16.dp))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = Color.White,
        fontWeight = FontWeight.Bold,
      )
    }
    content()
  }
}

@Composable
internal fun roomStudioTextFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedTextColor = Color.White,
  unfocusedTextColor = Color.White,
  focusedBorderColor = SwarCyan,
  unfocusedBorderColor = Color(0x55FFFFFF),
  focusedLabelColor = SwarCyan,
  unfocusedLabelColor = Color(0xFFC8C1E8),
  cursorColor = SwarCyan,
)

private fun formatKickTimestamp(ms: Long): String {
  if (ms <= 0L) return "अभी"
  return runCatching {
    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(ms))
  }.getOrDefault("अभी")
}
