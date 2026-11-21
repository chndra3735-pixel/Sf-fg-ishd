package com.example.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.SwarUserIdentity
import com.example.data.repository.AuditDateLocationHelper
import com.example.data.repository.CapturedLocationResult
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarDeepNight
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarStageIndigo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstTimeOnboardingScreen(
  userId: String,
  userEmail: String,
  displayName: String,
  avatarUrl: String,
  isSubmitting: Boolean,
  onSubmitOnboarding: (dobIso: String, locationResult: CapturedLocationResult, termsAccepted: Boolean) -> Unit,
  onSignOut: () -> Unit,
  initialDobIso: String = "",
  autoCaptureLocation: Boolean = true,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val numericId = remember(userId) { SwarUserIdentity.numericIdForUid(userId) }

  var selectedDobIso by remember { mutableStateOf(initialDobIso) }
  var showDatePicker by remember { mutableStateOf(false) }
  var termsAccepted by remember { mutableStateOf(false) }
  var isCapturingLocation by remember { mutableStateOf(false) }
  var locationResult by remember {
    mutableStateOf(
      CapturedLocationResult(
        label = "लोकेशन प्राप्त की जा रही है...",
        lat = 0.0,
        lng = 0.0,
        status = "pending",
      )
    )
  }

  val calculatedAge = remember(selectedDobIso) {
    AuditDateLocationHelper.calculateAgeYears(selectedDobIso)
  }

  val captureLocationNow: () -> Unit = {
    if (!isCapturingLocation) {
      isCapturingLocation = true
      scope.launch {
        locationResult = AuditDateLocationHelper.captureBestCoarseLocation(context)
        isCapturingLocation = false
      }
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { granted ->
      if (granted) {
        captureLocationNow()
      } else {
        locationResult = CapturedLocationResult(
          label = "लोकेशन अनुमति अस्वीकृत — आप पुनः प्रयास या आगे बढ़ सकते हैं",
          lat = 0.0,
          lng = 0.0,
          status = "denied",
        )
      }
    },
  )

  LaunchedEffect(autoCaptureLocation) {
    if (!autoCaptureLocation) {
      locationResult = CapturedLocationResult(
        label = "लोकेशन प्रतीक्षारत",
        lat = 0.0,
        lng = 0.0,
        status = "pending",
      )
      return@LaunchedEffect
    }
    if (AuditDateLocationHelper.hasLocationPermission(context)) {
      captureLocationNow()
    } else {
      locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
  }

  val canSubmit = selectedDobIso.isNotBlank() && calculatedAge >= 13 && termsAccepted && !isSubmitting

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(SwarDeepNight, SwarStageIndigo, Color(0xFF140B29))
        )
      )
      .safeDrawingPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 560.dp)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Welcome Badge & Profile Header
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0x22FFFFFF),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          UserAvatar(
            url = avatarUrl,
            name = displayName.ifBlank { "Swar User" },
            size = 60.dp,
            borderColor = SwarCyan,
            borderWidth = 2.dp,
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = SwarCyan,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "प्रथम लॉगिन सत्यापन",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
              )
            }
            Text(
              text = displayName.ifBlank { "Swar User" },
              style = MaterialTheme.typography.bodyMedium,
              color = SwarGold,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "ID:$numericId" + if (userEmail.isNotBlank()) " • $userEmail" else "",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFD6D1EC),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Text(
        text = "रूम और चैट सुरक्षित रखने के लिए कृपया पहली बार यह 3 विवरण पूरे करें:",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFFD9D5EE),
      )

      // 1. Live Location Capture Card
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE3F5FF)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "लोकेशन",
                tint = Color(0xFF0288D1),
                modifier = Modifier.size(20.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "1. लोकेशन सत्यापन",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF171A23),
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = if (isCapturingLocation) {
                  "लोकेशन प्राप्त की जा रही है..."
                } else {
                  locationResult.label.ifBlank { "लोकेशन प्रतीक्षारत" }
                },
                style = MaterialTheme.typography.bodySmall,
                color = when (locationResult.status) {
                  "captured" -> Color(0xFF1B873F)
                  "denied" -> Color(0xFFD84315)
                  else -> Color(0xFF5F667A)
                },
                modifier = Modifier.testTag("onboarding_location_status"),
              )
            }
            if (isCapturingLocation) {
              CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = SwarCyan)
            } else if (locationResult.status == "captured") {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "लोकेशन प्राप्त",
                tint = Color(0xFF1B873F),
                modifier = Modifier.size(22.dp),
              )
            }
          }

          OutlinedButton(
            onClick = {
              if (AuditDateLocationHelper.hasLocationPermission(context)) {
                captureLocationNow()
              } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("onboarding_retry_location_button"),
          ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (locationResult.status == "captured") "लोकेशन अपडेट करें" else "लोकेशन अनुमति दें / पुनः जांचें",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }

      // 2. Date of Birth Picker Card
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .clickable { showDatePicker = true }
          .testTag("onboarding_dob_card"),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF2E0)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.Cake,
                contentDescription = "जन्मतिथि",
                tint = Color(0xFFEF6C00),
                modifier = Modifier.size(20.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "2. जन्मतिथि (Date of Birth) चुनें",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF171A23),
                fontWeight = FontWeight.Bold,
              )
              if (selectedDobIso.isNotBlank()) {
                Text(
                  text = "जन्मतिथि: ${AuditDateLocationHelper.formatDobDisplay(selectedDobIso)} • आयु: $calculatedAge वर्ष",
                  style = MaterialTheme.typography.bodySmall,
                  color = if (calculatedAge >= 13) Color(0xFF1B873F) else SwarCoral,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.testTag("onboarding_dob_value"),
                )
              } else {
                Text(
                  text = "तारीख चुनने के लिए नीचे बटन दबाएं",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF5F667A),
                )
              }
            }
          }
          if (selectedDobIso.isNotBlank() && calculatedAge < 13) {
            Text(
              text = "इस ऐप का उपयोग करने के लिए न्यूनतम आयु 13 वर्ष होनी चाहिए।",
              style = MaterialTheme.typography.labelSmall,
              color = SwarCoral,
            )
          }
          Button(
            onClick = { showDatePicker = true },
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFE7F4FF),
              contentColor = Color(0xFF006CB5),
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("onboarding_pick_dob_button"),
          ) {
            Text(
              text = if (selectedDobIso.isBlank()) "कैलेंडर से जन्मतिथि चुनें" else "जन्मतिथि बदलें",
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      // 3. Room Conduct Checkbox ("टिक बॉक्स")
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = if (termsAccepted) 1.5.dp else 1.dp,
            color = if (termsAccepted) Color(0xFF1B873F) else Color(0xFFE0E5F0),
            shape = RoundedCornerShape(18.dp),
          )
          .clip(RoundedCornerShape(18.dp))
          .clickable { termsAccepted = !termsAccepted }
          .testTag("onboarding_conduct_row"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Checkbox(
            checked = termsAccepted,
            onCheckedChange = { termsAccepted = it },
            colors = CheckboxDefaults.colors(
              checkedColor = Color(0xFF1B873F),
              checkmarkColor = Color.White,
            ),
            modifier = Modifier.testTag("onboarding_conduct_checkbox"),
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "3. रूम आचार संहिता (टिक बॉक्स)",
              style = MaterialTheme.typography.titleSmall,
              color = Color(0xFF171A23),
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "मैं सहमत हूँ कि रूम में किसी भी प्रकार की अभद्र भाषा, गाली-गलौज या दुर्व्यवहार करने पर एडमिन द्वारा मेरा अकाउंट ब्लॉक कर दिया जाएगा।",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF424A5E),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = { onSubmitOnboarding(selectedDobIso, locationResult, termsAccepted) },
        enabled = canSubmit,
        colors = ButtonDefaults.buttonColors(
          containerColor = SwarCyan,
          contentColor = SwarDeepNight,
          disabledContainerColor = Color(0x4400E5FF),
          disabledContentColor = Color(0x99FFFFFF),
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("onboarding_submit_button"),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = SwarDeepNight)
        } else {
          Text(
            text = "सबमिट करें और आगे बढ़ें",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }

      TextButton(
        onClick = onSignOut,
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .testTag("onboarding_sign_out_button"),
      ) {
        Icon(
          Icons.AutoMirrored.Filled.Logout,
          contentDescription = null,
          tint = Color(0xFFD6D1EC),
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("दूसरे Google खाते से साइन इन करें", color = Color(0xFFD6D1EC))
      }
    }
  }

  if (showDatePicker) {
    val todayMs = remember { System.currentTimeMillis() }
    val datePickerState = rememberDatePickerState(
      selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
          return utcTimeMillis <= todayMs
        }
      }
    )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              selectedDobIso = AuditDateLocationHelper.dobIsoFromMillis(millis)
            }
            showDatePicker = false
          },
        ) {
          Text("चुनें", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text("रद्द करें")
        }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }
}

@Composable
fun AccountBlockedScreen(
  userId: String,
  userEmail: String,
  onSignOut: () -> Unit,
) {
  val numericId = remember(userId) { SwarUserIdentity.numericIdForUid(userId) }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(SwarDeepNight, Color(0xFF280B1A), Color(0xFF140715))
        )
      )
      .safeDrawingPadding()
      .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color.White,
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 460.dp)
        .testTag("account_blocked_screen"),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Box(
          modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(Color(0xFFFFEBEE)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Block,
            contentDescription = "अकाउंट ब्लॉक",
            tint = SwarCoral,
            modifier = Modifier.size(36.dp),
          )
        }

        Text(
          text = "आपका अकाउंट ब्लॉक किया गया है",
          style = MaterialTheme.typography.titleLarge,
          color = Color(0xFF171A23),
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "रूम नियमों के उल्लंघन या अभद्र व्यवहार के कारण एडमिन ने इस खाते की पहुँच अस्थायी/स्थायी रूप से रोक दी है।",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFF4F566B),
          textAlign = TextAlign.Center,
        )
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFFF3F5FA),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = "ID: $numericId",
              style = MaterialTheme.typography.labelLarge,
              color = Color(0xFF171A23),
              fontWeight = FontWeight.Bold,
            )
            if (userEmail.isNotBlank()) {
              Text(
                text = userEmail,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B7388),
              )
            }
          }
        }
        Button(
          onClick = onSignOut,
          colors = ButtonDefaults.buttonColors(containerColor = SwarCoral, contentColor = Color.White),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("blocked_sign_out_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("साइन आउट करें", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
