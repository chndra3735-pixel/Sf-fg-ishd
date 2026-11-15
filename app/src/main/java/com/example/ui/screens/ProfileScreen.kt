package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.ui.components.CoinEmblem
import com.example.ui.components.SupabaseConfigNoticeCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarDeepNight
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarViolet
import com.example.ui.viewmodel.UiState

@Composable
fun ProfileScreen(
  profileState: UiState<UserProfile?>,
  isUploadingMedia: Boolean,
  isSupabaseConfigured: Boolean,
  onUploadAvatarDp: (Uri) -> Unit,
  onUpdateProfile: (displayName: String, handle: String, bio: String, gender: String, clubTag: String) -> Unit,
  onSignOut: () -> Unit,
) {
  var showEditDialog by remember { mutableStateOf(false) }

  val dpPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri -> uri?.let(onUploadAvatarDp) },
  )

  val profile = (profileState as? UiState.Success)?.data

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Hero Profile Banner
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = SwarDeepNight),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.linearGradient(listOf(SwarDeepNight, Color(0xFF28194E)))
          )
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(contentAlignment = Alignment.BottomEnd) {
          UserAvatar(
            url = profile?.avatarUrl.orEmpty(),
            name = profile?.displayName.orEmpty().ifBlank { "Swar User" },
            size = 92.dp,
            borderColor = SwarCyan,
            borderWidth = 2.5.dp,
          )
          Surface(
            onClick = {
              dpPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = CircleShape,
            color = SwarCyan,
            modifier = Modifier
              .size(34.dp)
              .testTag("change_dp_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (isUploadingMedia) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SwarDeepNight)
              } else {
                Icon(
                  imageVector = Icons.Filled.CameraAlt,
                  contentDescription = "DP बदलें",
                  tint = SwarDeepNight,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = profile?.displayName.orEmpty().ifBlank { "Swar User" },
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "@${profile?.handle.orEmpty().ifBlank { "swar_user" }} • ${profile?.clubTag.orEmpty().ifBlank { "SWAR" }}",
          style = MaterialTheme.typography.bodySmall,
          color = SwarCyan,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = profile?.bio.orEmpty().ifBlank { "SwarRoom पर लाइव बातचीत और संगीत!" },
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFCCC6E8),
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = Color(0x28FFFFFF),
            shape = RoundedCornerShape(14.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              CoinEmblem(sizeDp = 16)
              Text(
                text = "${profile?.coins ?: 1500} कॉइंस",
                style = MaterialTheme.typography.labelLarge,
                color = SwarGold,
                fontWeight = FontWeight.Bold,
              )
            }
          }
          Surface(
            onClick = { showEditDialog = true },
            color = SwarViolet,
            shape = RoundedCornerShape(14.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("प्रोफ़ाइल संपादित करें", style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
          }
        }
      }
    }

    // Supabase DP upload action button
    OutlinedButton(
      onClick = {
        dpPicker.launch(
          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
      },
      enabled = !isUploadingMedia,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().height(50.dp),
    ) {
      Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        if (isUploadingMedia) "DP Supabase पर अपलोड हो रही है..."
        else "प्रोफ़ाइल DP अपलोड / बदलें (Supabase Storage)"
      )
    }

    SupabaseConfigNoticeCard(isConfigured = isSupabaseConfigured)

    // Backend summary card
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text("हाइब्रिड क्लाउड आर्किटेक्चर", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
          "• Supabase Storage: प्रोफ़ाइल DP, रूम कवर और वीडियो मोमेंट्स",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          "• Firebase Auth & Firestore: Google साइन-इन, लाइव सीटें, रीयल-टाइम रूम चैट और निजी संदेश",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Button(
      onClick = onSignOut,
      colors = ButtonDefaults.buttonColors(containerColor = SwarCoral, contentColor = Color.White),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("sign_out_button"),
    ) {
      Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text("साइन आउट करें", fontWeight = FontWeight.Bold)
    }
  }

  if (showEditDialog && profile != null) {
    EditProfileDialog(
      profile = profile,
      onDismiss = { showEditDialog = false },
      onSave = { name, handle, bio, gender, club ->
        onUpdateProfile(name, handle, bio, gender, club)
        showEditDialog = false
      },
    )
  }
}

@Composable
private fun EditProfileDialog(
  profile: UserProfile,
  onDismiss: () -> Unit,
  onSave: (String, String, String, String, String) -> Unit,
) {
  var name by remember { mutableStateOf(profile.displayName) }
  var handle by remember { mutableStateOf(profile.handle) }
  var bio by remember { mutableStateOf(profile.bio) }
  var gender by remember { mutableStateOf(profile.gender) }
  var clubTag by remember { mutableStateOf(profile.clubTag) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("प्रोफ़ाइल संपादित करें") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it.take(40) },
          label = { Text("नाम") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = handle,
          onValueChange = { handle = it.take(30) },
          label = { Text("हैंडल") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = clubTag,
          onValueChange = { clubTag = it.take(16) },
          label = { Text("क्लब बैज") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = bio,
          onValueChange = { bio = it.take(160) },
          label = { Text("बायो") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("male" to "पुरुष", "female" to "महिला", "other" to "अन्य").forEach { (key, label) ->
            FilterChip(
              selected = gender == key,
              onClick = { gender = key },
              label = { Text(label) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = { onSave(name, handle, bio, gender, clubTag) }) {
        Text("सहेजें")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("रद्द करें") }
    },
  )
}
