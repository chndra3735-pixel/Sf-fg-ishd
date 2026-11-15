package com.example.ui.screens

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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.VoiceRoom
import com.example.ui.components.LiveEqualizerBars
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarViolet
import com.example.ui.viewmodel.UiState

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
      .background(MaterialTheme.colorScheme.background),
  ) {
    // Top Lobby Header matching screenshot 2
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
              fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
              color = if (active) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (active) {
              Spacer(modifier = Modifier.height(3.dp))
              Box(
                modifier = Modifier
                  .width(24.dp)
                  .height(3.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(Brush.horizontalGradient(listOf(SwarCyan, SwarViolet)))
              )
            }
          }
        }
      }

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
        )
      }

      Surface(
        onClick = onCreateRoomClick,
        shape = RoundedCornerShape(14.dp),
        color = SwarCyan.copy(alpha = 0.16f),
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("create_room_button"),
      ) {
        Box(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.AddHome,
            contentDescription = "नया रूम बनाएं",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    if (isSearchOpen) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("रूम का नाम, ID या होस्ट खोजें...") },
        singleLine = true,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
      )
    }

    when (roomsState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = SwarCyan)
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("room_card_${room.id}"),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(0.95f)
        .clip(RoundedCornerShape(18.dp))
        .border(
          width = 1.5.dp,
          brush = Brush.linearGradient(
            listOf(SwarCyan.copy(alpha = 0.7f), SwarViolet.copy(alpha = 0.7f))
          ),
          shape = RoundedCornerShape(18.dp),
        ),
    ) {
      // Background visual: Supabase coverUrl -> host DP -> bundled room theme art
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

      // Dark bottom vignette for crisp title readability
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(Color(0x22000000), Color.Transparent, Color(0xCC0D081D))
            )
          )
      )

      // Top-left "नया" or Club badge
      Row(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (room.clubTag.isNotBlank()) {
          Surface(
            color = SwarViolet.copy(alpha = 0.85f),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = room.clubTag,
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            )
          }
        }
      }

      // Top-right live equalizer + listener count pill (like Screenshot 2)
      Surface(
        color = Color(0x88000000),
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

      // Bottom room title + badge
      Row(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 10.dp),
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
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Host footer row matching Screenshot 2
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 2.dp),
    ) {
      UserAvatar(
        url = room.ownerAvatarUrl,
        name = room.ownerName,
        size = 20.dp,
        borderColor = SwarCyan,
        borderWidth = 1.dp,
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = room.ownerName,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = "${room.seatCount} सीट",
        style = MaterialTheme.typography.labelSmall,
        color = SwarGold,
      )
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
      .padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .size(84.dp)
        .clip(CircleShape)
        .background(SwarCyan.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Filled.GraphicEq,
        contentDescription = null,
        tint = SwarCyan,
        modifier = Modifier.size(42.dp),
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "अभी कोई लाइव वॉइस रूम सक्रिय नहीं है",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = "अपना पहला लाइव वॉइस रूम शुरू करें और दोस्तों को बातचीत व टेक्स्ट चैट के लिए आमंत्रित करें।",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(18.dp))
    Button(
      onClick = onCreateRoomClick,
      colors = ButtonDefaults.buttonColors(containerColor = SwarCyan, contentColor = Color.Black),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier.testTag("empty_create_room_button"),
    ) {
      Text("नया वॉइस रूम बनाएं", fontWeight = FontWeight.Bold)
    }
    if (onOpenStagePreview != null) {
      Spacer(modifier = Modifier.height(10.dp))
      androidx.compose.material3.OutlinedButton(
        onClick = onOpenStagePreview,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.testTag("open_stage_preview_button"),
      ) {
        Text("वॉइस रूम स्टेज और पैनल देखें", fontWeight = FontWeight.SemiBold)
      }
    }
  }
}
