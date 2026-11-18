package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.AppUpdateInfo
import com.example.data.repository.GitHubUpdateChecker
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarDeepNight
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarViolet

@Composable
fun AppUpdateAvailableDialog(
  updateInfo: AppUpdateInfo,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(SwarCyan, SwarViolet))),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.SystemUpdate,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
          )
        }
        Column {
          Text(
            text = "नया अपडेट उपलब्ध है!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
          )
          Text(
            text = updateInfo.repoSlug,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          color = Color(0xFFE9F7FF),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text("मौजूदा वर्ज़न", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D6A85))
              Text(
                text = "v${updateInfo.currentVersionName}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F273D),
              )
            }
            Text("→", style = MaterialTheme.typography.titleMedium, color = SwarViolet, fontWeight = FontWeight.Bold)
            Column(horizontalAlignment = Alignment.End) {
              Text("नया GitHub वर्ज़न", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0A66C2))
              Text(
                text = updateInfo.latestVersionTag,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0A66C2),
              )
            }
          }
        }

        Text(
          text = updateInfo.releaseTitle,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
        )

        if (updateInfo.releaseNotes.isNotBlank()) {
          Text(
            text = updateInfo.releaseNotes.take(260),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          Text(
            text = "GitHub पर नया बदलाव पुश होने के बाद नवीनतम SwarRoom.apk तैयार है। नीचे दबाकर ब्राउज़र से डाउनलोड व अपडेट करें।",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val targetUrl = updateInfo.downloadUrl.ifBlank { updateInfo.releasePageUrl }
          val opened = runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
          }.isSuccess
          if (!opened) {
            Toast.makeText(context, "अपडेट लिंक खोला नहीं जा सका", Toast.LENGTH_SHORT).show()
          }
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008CE6), contentColor = Color.White),
        modifier = Modifier.testTag("download_app_update_button"),
      ) {
        Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("अभी अपडेट करें", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("dismiss_app_update_button"),
      ) {
        Text("बाद में")
      }
    },
    modifier = Modifier.testTag("app_update_available_dialog"),
  )
}

@Composable
fun AppUpdateCenterDialog(
  currentVersionName: String,
  currentVersionCode: Int,
  configuredGithubRepo: String,
  appUpdateInfo: AppUpdateInfo?,
  isCheckingUpdate: Boolean,
  updateCheckError: String?,
  isAdminUser: Boolean,
  onCheckForUpdate: () -> Unit,
  onSaveGithubRepoLink: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var repoInput by remember(configuredGithubRepo) {
    mutableStateOf(
      if (configuredGithubRepo.isNotBlank()) "https://github.com/$configuredGithubRepo" else ""
    )
  }
  val extractedPreview = remember(repoInput) {
    GitHubUpdateChecker.extractRepoSlug(repoInput)
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color.White,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .widthIn(max = 460.dp)
        .heightIn(max = 640.dp)
        .testTag("app_update_center_dialog"),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = Color(0xFF008CE6))
          Text(
            text = "ऐप अपडेट (GitHub)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A2033),
          )
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            color = Color(0xFFF3F6FB),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("इंस्टॉल किया गया वर्ज़न", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7387))
                Text(
                  text = "v$currentVersionName (बिल्ड $currentVersionCode)",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color(0xFF1A2033),
                )
              }
              if (configuredGithubRepo.isNotBlank()) {
                Surface(
                  color = Color(0xFFE1F3FF),
                  shape = RoundedCornerShape(8.dp),
                ) {
                  Text(
                    text = configuredGithubRepo,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF006CB5),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
            }
          }

          if (appUpdateInfo != null) {
            val hasUpdate = appUpdateInfo.hasUpdate
            Surface(
              color = if (hasUpdate) Color(0xFFFFF4E0) else Color(0xFFEAFBF0),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = if (hasUpdate) Icons.Filled.CloudDownload else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (hasUpdate) Color(0xFFE67E00) else Color(0xFF1B8E3E),
                  )
                  Text(
                    text = if (hasUpdate) {
                      "नया अपडेट ${appUpdateInfo.latestVersionTag} तैयार है!"
                    } else {
                      "आपका ऐप नवीनतम वर्ज़न (${appUpdateInfo.latestVersionTag}) पर है।"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C2333),
                  )
                }

                if (hasUpdate) {
                  Button(
                    onClick = {
                      val targetUrl = appUpdateInfo.downloadUrl.ifBlank { appUpdateInfo.releasePageUrl }
                      runCatching {
                        context.startActivity(
                          Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                          }
                        )
                      }.onFailure {
                        Toast.makeText(context, "डाउनलोड लिंक खोला नहीं जा सका", Toast.LENGTH_SHORT).show()
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008CE6), contentColor = Color.White),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(48.dp)
                      .testTag("update_center_download_button"),
                  ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("नया APK डाउनलोड / अपडेट करें", fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }

          if (!updateCheckError.isNullOrBlank()) {
            Surface(
              color = SwarCoral.copy(alpha = 0.12f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = updateCheckError,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB71C1C),
                modifier = Modifier.padding(10.dp),
              )
            }
          }

          OutlinedButton(
            onClick = onCheckForUpdate,
            enabled = !isCheckingUpdate,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("check_github_update_button"),
          ) {
            if (isCheckingUpdate) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("GitHub रिलीज़ जांची जा रही है...")
            } else {
              Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("अभी अपडेट जांचें", fontWeight = FontWeight.SemiBold)
            }
          }

          if (isAdminUser) {
            Surface(
              color = SwarDeepNight.copy(alpha = 0.05f),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(Icons.Filled.Link, contentDescription = null, tint = SwarViolet, modifier = Modifier.size(18.dp))
                  Text(
                    text = "एडमिन GitHub रिपॉजिटरी लिंक",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = SwarViolet,
                  )
                }
                Text(
                  text = "GitHub रिपो लिंक पेस्ट करें। जब भी आप GitHub पर नया कोड पुश करेंगे, सभी यूज़र्स के ऐप में अपडेट बटन अपने आप दिखेगा।",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                  value = repoInput,
                  onValueChange = { repoInput = it.take(300) },
                  label = { Text("GitHub रिपो लिंक (URL या owner/repo)") },
                  placeholder = { Text("https://github.com/username/SwarRoom") },
                  singleLine = true,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_github_repo_input"),
                )
                if (extractedPreview != null) {
                  Text(
                    text = "✓ निकाली गई रिपॉजिटरी: $extractedPreview",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF1B8E3E),
                    fontWeight = FontWeight.Bold,
                  )
                }
                Button(
                  onClick = { onSaveGithubRepoLink(repoInput) },
                  enabled = !isCheckingUpdate && extractedPreview != null,
                  colors = ButtonDefaults.buttonColors(containerColor = SwarGold, contentColor = SwarDeepNight),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_github_repo_button"),
                ) {
                  Text("GitHub लिंक सेव करें व जांचें", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) {
            Text("बंद करें")
          }
        }
      }
    }
  }
}
