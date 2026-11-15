package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.VoiceRoom
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarDeepNight
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarMint
import com.example.ui.theme.SwarViolet

@Composable
fun UserAvatar(
  url: String,
  name: String,
  size: Dp,
  modifier: Modifier = Modifier,
  borderColor: Color = Color.Transparent,
  borderWidth: Dp = 0.dp,
) {
  val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
  val gradientColors = remember(name) {
    when ((name.hashCode() and 0x7FFFFFFF) % 4) {
      0 -> listOf(SwarCyan, SwarViolet)
      1 -> listOf(SwarCoral, SwarGold)
      2 -> listOf(SwarViolet, SwarCoral)
      else -> listOf(SwarMint, SwarCyan)
    }
  }

  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .then(
        if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, CircleShape)
        else Modifier
      )
      .background(Brush.linearGradient(gradientColors)),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = initial,
      color = Color.White,
      fontWeight = FontWeight.Bold,
      style = if (size >= 48.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
    )
    if (url.isNotBlank()) {
      AsyncImage(
        model = url,
        contentDescription = "$name प्रोफ़ाइल फ़ोटो",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
    }
  }
}

@Composable
fun LiveEqualizerBars(
  modifier: Modifier = Modifier,
  barColor: Color = Color.White,
  isAnimating: Boolean = true,
) {
  val transition = rememberInfiniteTransition(label = "eq")
  val h1 by transition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
    label = "b1",
  )
  val h2 by transition.animateFloat(
    initialValue = 0.9f,
    targetValue = 0.3f,
    animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse),
    label = "b2",
  )
  val h3 by transition.animateFloat(
    initialValue = 0.45f,
    targetValue = 0.95f,
    animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
    label = "b3",
  )

  Row(
    modifier = modifier.height(12.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.Bottom,
  ) {
    listOf(
      if (isAnimating) h1 else 0.5f,
      if (isAnimating) h2 else 0.9f,
      if (isAnimating) h3 else 0.6f,
    ).forEach { fraction ->
      Box(
        modifier = Modifier
          .width(2.5.dp)
          .height((12f * fraction).dp)
          .clip(RoundedCornerShape(1.5.dp))
          .background(barColor)
      )
    }
  }
}

@Composable
fun SupabaseConfigNoticeCard(
  isConfigured: Boolean,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = if (isConfigured) SwarMint.copy(alpha = 0.14f) else SwarGold.copy(alpha = 0.15f),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = if (isConfigured) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
        contentDescription = null,
        tint = if (isConfigured) SwarMint else SwarGold,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = if (isConfigured) {
          "Supabase Storage कनेक्टेड है — DP, कवर और वीडियो सीधे क्लाउड बकेट में सेव होंगे।"
        } else {
          stringResource(id = R.string.supabase_not_configured)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
fun CreateRoomDialog(
  isUploading: Boolean,
  onDismiss: () -> Unit,
  onCreate: (
    title: String,
    category: String,
    theme: String,
    clubTag: String,
    welcomeNotice: String,
    seatCount: Int,
    coverUri: Uri?,
  ) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var pickedCoverUri by remember { mutableStateOf<Uri?>(null) }

  val coverPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri -> pickedCoverUri = uri },
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(id = R.string.create_room_title)) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Top: Profile / Room Photo Upload
        Box(
          modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .border(2.dp, SwarCyan, CircleShape)
            .background(SwarCyan.copy(alpha = 0.12f))
            .clickable {
              coverPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            }
            .testTag("create_room_photo_picker"),
          contentAlignment = Alignment.Center,
        ) {
          if (pickedCoverUri != null) {
            AsyncImage(
              model = pickedCoverUri,
              contentDescription = "चुनी गई रूम प्रोफ़ाइल फ़ोटो",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
            )
          } else {
            Icon(
              imageVector = Icons.Filled.AddPhotoAlternate,
              contentDescription = "प्रोफ़ाइल फ़ोटो अपलोड करें",
              tint = SwarCyan,
              modifier = Modifier.size(34.dp),
            )
          }
        }

        OutlinedButton(
          onClick = {
            coverPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (pickedCoverUri == null) "प्रोफ़ाइल फ़ोटो अपलोड करें" else "फ़ोटो बदलें ✓"
          )
        }

        // Bottom: Room Name Input
        OutlinedTextField(
          value = title,
          onValueChange = { title = it.take(60) },
          label = { Text("रूम का नाम") },
          placeholder = { Text("अपना रूम नाम लिखें...") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("room_title_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onCreate(
            title.trim().ifBlank { "लाइव वॉइस रूम" },
            "लोकप्रिय",
            "stadium",
            "SWAR",
            "SwarRoom में आपका स्वागत है। कृपया एक दूसरे का सम्मान करें और सभ्य तरीके से चैट करें।",
            22,
            pickedCoverUri,
          )
        },
        enabled = !isUploading,
        colors = ButtonDefaults.buttonColors(containerColor = SwarCyan, contentColor = Color.Black),
        modifier = Modifier.testTag("confirm_create_room_button"),
      ) {
        Text(if (isUploading) "अपलोड हो रहा है..." else "सबमिट करें", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("रद्द करें") }
    },
  )
}

@Composable
fun CreateMomentDialog(
  isUploading: Boolean,
  onDismiss: () -> Unit,
  onSubmit: (uri: Uri, isVideo: Boolean, caption: String) -> Unit,
) {
  var caption by remember { mutableStateOf("") }
  var pickedUri by remember { mutableStateOf<Uri?>(null) }
  var isVideo by remember { mutableStateOf(true) }

  val imagePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        pickedUri = uri
        isVideo = false
      }
    },
  )
  val videoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        pickedUri = uri
        isVideo = true
      }
    },
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("नया वीडियो / फ़ोटो मोमेंट") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          "आपका वीडियो या फ़ोटो Supabase Storage में अपलोड होगा और मोमेंट्स फ़ीड में दिखेगा।",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = {
              videoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
              )
            },
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("वीडियो")
          }
          OutlinedButton(
            onClick = {
              imagePicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("फ़ोटो")
          }
        }
        if (pickedUri != null) {
          Surface(
            color = SwarCyan.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = if (isVideo) "वीडियो चयनित ✓" else "फ़ोटो चयनित ✓",
              style = MaterialTheme.typography.labelLarge,
              color = SwarCyan,
              modifier = Modifier.padding(10.dp),
            )
          }
        }
        OutlinedTextField(
          value = caption,
          onValueChange = { caption = it.take(300) },
          label = { Text("कैप्शन लिखें...") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { pickedUri?.let { onSubmit(it, isVideo, caption) } },
        enabled = pickedUri != null && !isUploading,
        colors = ButtonDefaults.buttonColors(containerColor = SwarCyan, contentColor = Color.Black),
      ) {
        Text(if (isUploading) "अपलोड हो रहा है..." else "पोस्ट करें")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("रद्द करें") }
    },
  )
}

@Composable
fun MinimizedRoomFloatingBar(
  room: VoiceRoom,
  isMicMuted: Boolean,
  onExpand: () -> Unit,
  onToggleMic: () -> Unit,
  onExitRoom: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onExpand,
    shape = RoundedCornerShape(26.dp),
    color = SwarDeepNight.copy(alpha = 0.96f),
    shadowElevation = 10.dp,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 10.dp)
      .border(1.5.dp, Brush.horizontalGradient(listOf(SwarCyan, SwarViolet)), RoundedCornerShape(26.dp))
      .testTag("minimized_room_bar"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      UserAvatar(
        url = room.coverUrl.ifBlank { room.ownerAvatarUrl },
        name = room.title.ifBlank { room.ownerName },
        size = 42.dp,
        borderColor = SwarGold,
        borderWidth = 1.5.dp,
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          LiveEqualizerBars(barColor = SwarCyan)
          Text(
            text = room.title.ifBlank { "लाइव वॉइस रूम" },
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = "रूम चल रहा है • वापस खोलने के लिए टैप करें",
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFD0C9EC),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      IconButton(
        onClick = onToggleMic,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .clip(CircleShape)
          .background(if (isMicMuted) Color(0x33FFFFFF) else SwarMint.copy(alpha = 0.25f))
          .testTag("minimized_room_mic_button"),
      ) {
        Icon(
          imageVector = if (isMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
          contentDescription = if (isMicMuted) "माइक चालू करें" else "माइक बंद करें",
          tint = if (isMicMuted) Color.White else SwarMint,
        )
      }

      IconButton(
        onClick = onExpand,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("minimized_room_expand_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.OpenInFull,
          contentDescription = "रूम बड़ा करें",
          tint = SwarCyan,
        )
      }

      IconButton(
        onClick = onExitRoom,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("minimized_room_exit_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Close,
          contentDescription = "रूम से बाहर निकलें",
          tint = SwarCoral,
        )
      }
    }
  }
}
