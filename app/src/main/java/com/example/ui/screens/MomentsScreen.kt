package com.example.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.model.MomentPost
import com.example.ui.components.SupabaseConfigNoticeCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarDeepNight
import com.example.ui.viewmodel.UiState

@Composable
fun MomentsScreen(
  momentsState: UiState<List<MomentPost>>,
  currentUserId: String,
  isSupabaseConfigured: Boolean,
  onAddMomentClick: () -> Unit,
  onLikeMoment: (String) -> Unit,
  onDeleteMoment: (String) -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "मोमेंट्स और वीडियो",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Supabase क्लाउड स्टोरेज मीडिया फ़ीड",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = onAddMomentClick,
          colors = ButtonDefaults.buttonColors(containerColor = SwarCyan, contentColor = SwarDeepNight),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.testTag("add_moment_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("अपलोड", fontWeight = FontWeight.Bold)
        }
      }

      SupabaseConfigNoticeCard(
        isConfigured = isSupabaseConfigured,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
      )

      when (momentsState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SwarCyan)
          }
        }
        is UiState.Error -> {
          Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(momentsState.message, color = MaterialTheme.colorScheme.error)
          }
        }
        is UiState.Success -> {
          val posts = momentsState.data
          if (posts.isEmpty()) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Filled.VideoLibrary,
                contentDescription = null,
                tint = SwarCyan,
                modifier = Modifier.size(56.dp),
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "अभी कोई वीडियो या फ़ोटो मोमेंट नहीं है",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "अपना पहला वीडियो या फ़ोटो Supabase Storage पर अपलोड करें।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
              modifier = Modifier.fillMaxSize(),
            ) {
              items(posts, key = { it.id }) { post ->
                MomentPostCard(
                  post = post,
                  isOwner = post.authorUid == currentUserId,
                  onLike = { onLikeMoment(post.id) },
                  onDelete = { onDeleteMoment(post.id) },
                )
              }
            }
          }
        }
      }
    }

    FloatingActionButton(
      onClick = onAddMomentClick,
      containerColor = SwarCyan,
      contentColor = SwarDeepNight,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(18.dp),
    ) {
      Icon(Icons.Filled.Add, contentDescription = "मोमेंट जोड़ें")
    }
  }
}

@Composable
private fun MomentPostCard(
  post: MomentPost,
  isOwner: Boolean,
  onLike: () -> Unit,
  onDelete: () -> Unit,
) {
  var isPlayingVideo by remember(post.id) { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        UserAvatar(
          url = post.authorAvatarUrl,
          name = post.authorName,
          size = 40.dp,
          borderColor = SwarCyan,
          borderWidth = 1.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = post.authorName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = if (post.mediaType == "video") "Supabase वीडियो क्लिप" else "Supabase फ़ोटो",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (isOwner) {
          IconButton(onClick = onDelete) {
            Icon(
              imageVector = Icons.Filled.DeleteOutline,
              contentDescription = "पोस्ट हटाएं",
              tint = SwarCoral,
            )
          }
        }
      }

      if (post.caption.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = post.caption, style = MaterialTheme.typography.bodyMedium)
      }

      Spacer(modifier = Modifier.height(10.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 10f)
          .clip(RoundedCornerShape(16.dp))
          .background(SwarDeepNight),
        contentAlignment = Alignment.Center,
      ) {
        if (post.mediaType == "video") {
          if (isPlayingVideo) {
            AndroidView(
              factory = { ctx ->
                VideoView(ctx).apply {
                  setVideoURI(Uri.parse(post.mediaUrl))
                  val controller = MediaController(ctx)
                  controller.setAnchorView(this)
                  setMediaController(controller)
                  setOnPreparedListener { mp ->
                    mp.isLooping = true
                    start()
                  }
                }
              },
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                onClick = { isPlayingVideo = true },
                shape = CircleShape,
                color = SwarCyan,
                modifier = Modifier.size(56.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "वीडियो चलाएं",
                    tint = SwarDeepNight,
                    modifier = Modifier.size(32.dp),
                  )
                }
              }
              Text(
                text = "वीडियो चलाने के लिए टैप करें",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
              )
            }
          }
        } else {
          AsyncImage(
            model = post.mediaUrl,
            contentDescription = post.caption.ifBlank { "मोमेंट फ़ोटो" },
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        IconButton(onClick = onLike) {
          Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "पसंद करें",
            tint = SwarCoral,
          )
        }
        Text(
          text = "${post.likesCount} पसंद",
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}
