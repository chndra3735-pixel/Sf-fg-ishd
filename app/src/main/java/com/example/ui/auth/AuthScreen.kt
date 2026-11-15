package com.example.ui.auth

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarDeepNight
import com.example.ui.theme.SwarStageIndigo
import com.example.ui.theme.SwarViolet

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) {
    runCatching { CredentialManager.create(context) }.getOrNull()
  }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(SwarDeepNight, SwarStageIndigo, Color(0xFF0E0820))
        )
      )
      .safeDrawingPadding()
      .padding(horizontal = 24.dp, vertical = 20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 460.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(210.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1E143B),
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          Image(
            painter = painterResource(id = R.drawable.img_auth_hero),
            contentDescription = "SwarRoom लाइव ऑडियो स्टेज",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color.Transparent, Color(0xCC140B29))
                )
              )
          )
          Row(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(SwarCyan, SwarViolet))),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "लाइव वॉइस • रीयल-टाइम चैट",
                style = MaterialTheme.typography.bodySmall,
                color = SwarCyan,
              )
            }
          }
        }
      }

      Text(
        text = "दोस्तों के साथ लाइव बातचीत शुरू करें",
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White,
        textAlign = TextAlign.Center,
      )
      Text(
        text = "मल्टी-सीट वॉइस रूम, रीयल-टाइम टेक्स्ट चैट और Supabase स्टोरेज पर वीडियो व DP साझा करें।",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFFC5BFE2),
        textAlign = TextAlign.Center,
      )

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FeatureHighlightRow(
          icon = Icons.Filled.Mic,
          tint = SwarCyan,
          title = "लाइव वॉइस सीट और इको-फ्री माइक",
          subtitle = "रूम में सीट लें, लाइव बोलें और वेव एनिमेशन देखें",
        )
        FeatureHighlightRow(
          icon = Icons.AutoMirrored.Filled.Chat,
          tint = SwarCoral,
          title = "रीयल-टाइम रूम चैट और डायरेक्ट मैसेज",
          subtitle = "Firestore के साथ तुरंत टेक्स्ट और इमोजी रिएक्शन",
        )
        FeatureHighlightRow(
          icon = Icons.Filled.CloudUpload,
          tint = Color(0xFFFFC83D),
          title = "Supabase क्लाउड स्टोरेज",
          subtitle = "प्रोफ़ाइल DP, रूम कवर और शॉर्ट वीडियो अपलोड",
        )
      }

      if (errorMessage != null) {
        Surface(
          color = Color(0x33FF4D8D),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = errorMessage.orEmpty(),
            color = Color(0xFFFFB4CC),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      Button(
        onClick = {
          errorMessage = null
          isLoading = true
          onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
              isLoading = false
              onAuthSuccess()
            },
            onAuthError = { msg ->
              isLoading = false
              errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
              isLoading = false
            },
          )
        },
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("google_sign_in_button"),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = SwarCyan,
          contentColor = SwarDeepNight,
        ),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = SwarDeepNight,
            strokeWidth = 2.5.dp,
          )
        } else {
          Text(
            text = stringResource(id = R.string.sign_in_google),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  tint: Color,
  title: String,
  subtitle: String,
) {
  Surface(
    color = Color(0x1FFFFFFF),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = Color.White)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB5AFD3))
      }
    }
  }
}
