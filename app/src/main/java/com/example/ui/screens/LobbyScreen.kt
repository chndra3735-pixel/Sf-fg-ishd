package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.VoiceRoom
import com.example.ui.components.LiveEqualizerBars
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarCyanDark
import com.example.ui.theme.SwarViolet
import com.example.ui.viewmodel.UiState

private val LobbyHeaderBrush = Brush.verticalGradient(
  colors = listOf(
    Color(0xFFD6EEFF),
    Color(0xFFEEF7FF),
    Color(0xFFFFFFFF),
  ),
)

@Composable
fun LobbyScreen(
  roomsState: UiState<List<VoiceRoom>>,
  currentUserId: String,
  onRoomClick: (VoiceRoom) -> Unit,
  onCreateRoomClick: () -> Unit,
  onOpenStagePreview: (() -> Unit)? = null,
) {
  var selectedTab by remember { mutableStateOf("लोकप्रिय") }
  var isSearchOpen by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White),
  ) {
    // Soft pastel sky-blue to crisp white top header matching Profile aesthetic
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(LobbyHeaderBrush)
        .padding(bottom = 4.dp),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(18.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          listOf(
            stringResource(id = R.string.tab_following),
            stringResource(id = R.string.tab_popular),
            stringResource(id = R.string.tab_recent),
          ).forEach { tab ->
            val active = selectedTab == tab
            Column(
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .clickable { selectedTab = tab }
                .padding(vertical = 4.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = tab,
                style = if (active) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (active) Color(0xFF13192B) else Color(0xFF63708A),
              )
              if (active) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                  modifier = Modifier
                    .width(28.dp)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(SwarCyan, SwarViolet, SwarCoral))),
                )
              }
            }
          }
        }

        Surface(
          shape = CircleShape,
          color = Color.White,
          border = BorderStroke(1.dp, Color(0xFFD4E6F7)),
          shadowElevation = 2.dp,
        ) {
          IconButton(
            onClick = {
              isSearchOpen = !isSearchOpen
              if (!isSearchOpen) searchQuery = ""
            },
            modifier = Modifier.testTag("lobby_search_button"),
          ) {
            Icon(
              imageVector = if (isSearchOpen) Icons.Filled.Close else Icons.Filled.Search,
              contentDescription = "रूम खोजें",
              tint = Color(0xFF1B2742),
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
          onClick = onCreateRoomClick,
          shape = RoundedCornerShape(16.dp),
          color = Color.Transparent,
          shadowElevation = 3.dp,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("create_room_button"),
        ) {
          Box(
            modifier = Modifier
              .background(
                Brush.linearGradient(listOf(Color(0xFF00AEFF), SwarViolet)),
                shape = RoundedCornerShape(16.dp),
              )
              .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.AddHome,
              contentDescription = "नया रूम बनाएं",
              tint = Color.White,
            )
          }
        }
      }

      if (isSearchOpen) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("रूम का नाम, ID या होस्ट खोजें...", color = Color(0xFF7B859A)) },
          singleLine = true,
          shape = RoundedCornerShape(22.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = SwarCyanDark,
            unfocusedBorderColor = Color(0xFFD2E4F6),
            focusedTextColor = Color(0xFF151926),
            unfocusedTextColor = Color(0xFF151926),
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        )
      }
    }

    when (roomsState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = SwarCyanDark)
        }
      }
      is UiState.Error -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = roomsState.message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
      is UiState.Success -> {
        val filtered = remember(roomsState.data, selectedTab, searchQuery, currentUserId) {
          roomsState.data
            .filter { room ->
              val matchesTab = when (selectedTab) {
                "फॉलोइंग" -> room.ownerId == currentUserId || room.listenerCount >= 1
                else -> true
              }
              val q = searchQuery.trim()
              val matchesQuery = q.isEmpty() ||
                room.title.contains(q, ignoreCase = true) ||
                room.ownerName.contains(q, ignoreCase = true) ||
                room.roomCode.contains(q, ignoreCase = true)
              matchesTab && matchesQuery
            }
            .let { list ->
              if (selectedTab == "लोकप्रिय") list.sortedByDescending { it.listenerCount }
              else list
            }
        }

        if (filtered.isEmpty()) {
          EmptyRoomsView(
            onCreateRoomClick = onCreateRoomClick,
            onOpenStagePreview = onOpenStagePreview,
          )
        } else {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            items(filtered, key = { it.id }) { room ->
              LobbyRoomCard(room = room, onClick = { onRoomClick(room) })
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LobbyRoomCard(
  room: VoiceRoom,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(20.dp),
    color = Color.White,
    shadowElevation = 3.dp,
    border = BorderStroke(1.dp, Color(0xFFE4EEF8)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("room_card_${room.id}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 10.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f)
          .padding(5.dp)
          .clip(RoundedCornerShape(16.dp))
          .border(
            width = 1.dp,
            brush = Brush.linearGradient(
              listOf(SwarCyan.copy(alpha = 0.55f), SwarViolet.copy(alpha = 0.45f)),
            ),
            shape = RoundedCornerShape(16.dp),
          ),
      ) {
        val fallbackDrawable = when (room.backgroundTheme) {
          "aurora" -> R.drawable.img_room_bg_aurora
          else -> R.drawable.img_room_bg_stadium
        }
        Image(
          painter = painterResource(id = fallbackDrawable),
          contentDescription = null,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
        val imageModel = room.coverUrl.ifBlank { room.ownerAvatarUrl }
        if (imageModel.isNotBlank()) {
          AsyncImage(
            model = imageModel,
            contentDescription = room.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        }

        // Bottom vignette for crisp LIVE & room title legibility
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color(0x18000000), Color.Transparent, Color(0xCC0D1120)),
              ),
            ),
        )

        // Top-left Club badge
        Row(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (room.clubTag.isNotBlank()) {
            Surface(
              color = SwarViolet.copy(alpha = 0.9f),
              shape = RoundedCornerShape(10.dp),
            ) {
              Text(
                text = room.clubTag,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
              )
            }
          }
        }

        // Top-right live equalizer + listener count pill
        Surface(
          color = Color(0x99101526),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            LiveEqualizerBars(barColor = SwarCyan)
            Text(
              text = "${room.listenerCount} लाइव",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        // Bottom room title + LIVE badge
        Row(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(horizontal = 9.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = SwarCoral,
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = "LIVE",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = room.title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Clean white footer row
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 10.dp),
      ) {
        UserAvatar(
          url = room.ownerAvatarUrl,
          name = room.ownerName,
          size = 22.dp,
          borderColor = SwarCyanDark,
          borderWidth = 1.dp,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = room.ownerName,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF1B2133),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Surface(
          color = Color(0xFFFFF5DC),
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = "${room.seatCount} सीट",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB37400),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyRoomsView(
  onCreateRoomClick: () -> Unit,
  onOpenStagePreview: (() -> Unit)? = null,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .size(88.dp)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            listOf(Color(0xFFD5F1FF), Color(0xFFECE4FF)),
          ),
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Filled.GraphicEq,
        contentDescription = null,
        tint = SwarCyanDark,
        modifier = Modifier.size(44.dp),
      )
    }
    Spacer(modifier = Modifier.height(18.dp))
    Text(
      text = "अभी कोई लाइव वॉइस रूम सक्रिय नहीं है",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = Color(0xFF151A28),
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "अपना पहला लाइव वॉइस रूम शुरू करें और दोस्तों को बातचीत व टेक्स्ट चैट के लिए आमंत्रित करें।",
      style = MaterialTheme.typography.bodyMedium,
      color = Color(0xFF5D677D),
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(22.dp))
    Button(
      onClick = onCreateRoomClick,
      colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF00A8F3),
        contentColor = Color.White,
      ),
      shape = RoundedCornerShape(24.dp),
      elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
      modifier = Modifier.testTag("empty_create_room_button"),
    ) {
      Text("नया वॉइस रूम बनाएं", fontWeight = FontWeight.Bold)
    }
    if (onOpenStagePreview != null) {
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedButton(
        onClick = onOpenStagePreview,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFCCE2F5)),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = Color(0xFFF6FAFF),
          contentColor = Color(0xFF1A4E8A),
        ),
        modifier = Modifier.testTag("open_stage_preview_button"),
      ) {
        Text("वॉइस रूम स्टेज और पैनल देखें", fontWeight = FontWeight.SemiBold)
      }
    }
  }
}
