package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
  initialLocationResult: CapturedLocationResult? = null,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  val numericId = remember(userId) { SwarUserIdentity.numericIdForUid(userId) }

  var selectedDobIso by remember(initialDobIso) { mutableStateOf(initialDobIso) }
  val initialAge = remember(initialDobIso) {
    AuditDateLocationHelper.calculateAgeYears(initialDobIso).takeIf { it > 0 }?.toString().orEmpty()
  }
  var ageInputText by remember(initialDobIso) { mutableStateOf(initialAge) }
  var showDatePicker by remember { mutableStateOf(false) }
  var termsAccepted by remember { mutableStateOf(false) }
  var isCapturingLocation by remember { mutableStateOf(false) }
  var locationResult by remember(initialLocationResult) {
    mutableStateOf(
      initialLocationResult ?: CapturedLocationResult(
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
  val enteredAge = ageInputText.toIntOrNull() ?: 0
  val isLocationCaptured = locationResult.isValidCaptured
  val isDobAndAgeValid = selectedDobIso.isNotBlank() && calculatedAge in 13..120 && enteredAge in 13..120

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
          label = "लोकेशन अनुमति अस्वीकृत — लोकेशन के बिना प्रवेश नहीं मिलेगा",
          lat = 0.0,
          lng = 0.0,
          status = "denied",
        )
      }
    },
  )

  LaunchedEffect(autoCaptureLocation, initialLocationResult) {
    if (initialLocationResult != null) return@LaunchedEffect
    if (!autoCaptureLocation) {
      locationResult = CapturedLocationResult(
        label = "लोकेशन प्रतीक्षारत — आगे बढ़ने के लिए लोकेशन प्राप्त करें",
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

  DisposableEffect(lifecycleOwner, autoCaptureLocation) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME && autoCaptureLocation && !isLocationCaptured) {
        if (AuditDateLocationHelper.hasLocationPermission(context)) {
          captureLocationNow()
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  val missingItems = buildList {
    if (!isLocationCaptured) add("लाइव लोकेशन")
    if (!isDobAndAgeValid) add("जन्मतिथि / उम्र (13+ वर्ष)")
    if (!termsAccepted) add("Accept बटन / टिक बॉक्स")
  }
  val canSubmit = isLocationCaptured && isDobAndAgeValid && termsAccepted && !isSubmitting

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
                text = "प्रवेश सत्यापन आवश्यक",
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
        text = "अंदर प्रवेश करने से पहले लोकेशन प्राप्त करना, जन्मतिथि/उम्र भरना और नियमों को Accept करना अनिवार्य है:",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFFD9D5EE),
      )

      // 1. Live Location Capture Card (Strict Gate)
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = if (isLocationCaptured) 1.5.dp else 1.dp,
            color = if (isLocationCaptured) Color(0xFF1B873F) else Color(0xFFE53935),
            shape = RoundedCornerShape(18.dp),
          ),
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
                text = "1. लोकेशन सत्यापन (अनिवार्य)",
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
                color = when {
                  isLocationCaptured -> Color(0xFF1B873F)
                  locationResult.status == "denied" || locationResult.status == "gps_disabled" || locationResult.status == "unavailable" -> Color(0xFFC62828)
                  else -> Color(0xFF5F667A)
                },
                modifier = Modifier.testTag("onboarding_location_status"),
              )
            }
            if (isCapturingLocation) {
              CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = SwarCyan)
            } else if (isLocationCaptured) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "लोकेशन प्राप्त",
                tint = Color(0xFF1B873F),
                modifier = Modifier.size(22.dp),
              )
            }
          }

          if (!isLocationCaptured && !isCapturingLocation) {
            Text(
              text = "बिना लोकेशन प्राप्त हुए अंदर प्रवेश करने की अनुमति नहीं है।",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFC62828),
              fontWeight = FontWeight.SemiBold,
            )
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
              text = if (isLocationCaptured) "लोकेशन अपडेट करें" else "लोकेशन अनुमति दें / पुनः जांचें",
              style = MaterialTheme.typography.labelLarge,
            )
          }

          if (locationResult.status == "gps_disabled") {
            Button(
              onClick = {
                runCatching {
                  context.startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  )
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFF3E0),
                contentColor = Color(0xFFE65100),
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_open_gps_settings_button"),
            ) {
              Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("मोबाइल लोकेशन (GPS) चालू करें", fontWeight = FontWeight.Bold)
            }
          } else if (locationResult.status == "denied") {
            TextButton(
              onClick = {
                runCatching {
                  val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                  ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  context.startActivity(intent)
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_open_app_settings_button"),
            ) {
              Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("ऐप सेटिंग से लोकेशन अनुमति दें")
            }
          }
        }
      }

      // 2. Date of Birth & Age Card
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("onboarding_dob_card"),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
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
                contentDescription = "जन्मतिथि व उम्र",
                tint = Color(0xFFEF6C00),
                modifier = Modifier.size(20.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "2. जन्मतिथि और उम्र डालें",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF171A23),
                fontWeight = FontWeight.Bold,
              )
              if (selectedDobIso.isNotBlank()) {
                Text(
                  text = "जन्मतिथि: ${AuditDateLocationHelper.formatDobDisplay(selectedDobIso)} • आयु: $calculatedAge वर्ष",
                  style = MaterialTheme.typography.bodySmall,
                  color = if (calculatedAge in 13..120) Color(0xFF1B873F) else SwarCoral,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.testTag("onboarding_dob_value"),
                )
              } else {
                Text(
                  text = "अपनी उम्र लिखें या नीचे कैलेंडर से जन्मतिथि चुनें",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF5F667A),
                )
              }
            }
          }

          OutlinedTextField(
            value = ageInputText,
            onValueChange = { raw ->
              val digits = raw.filter { it.isDigit() }.take(3)
              ageInputText = digits
              val parsedAge = digits.toIntOrNull()
              if (parsedAge != null && parsedAge in 13..120) {
                if (selectedDobIso.isBlank() ||
                  AuditDateLocationHelper.calculateAgeYears(selectedDobIso) != parsedAge
                ) {
                  selectedDobIso = AuditDateLocationHelper.dobIsoFromAgeYears(parsedAge)
                }
              } else if (digits.isBlank()) {
                selectedDobIso = ""
              }
            },
            label = { Text("उम्र डालें (वर्षों में, जैसे 25)") },
            placeholder = { Text("न्यूनतम 13 वर्ष") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = ageInputText.isNotBlank() && enteredAge !in 13..120,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color(0xFF171A23),
              unfocusedTextColor = Color(0xFF171A23),
              focusedBorderColor = SwarStageIndigo,
              unfocusedBorderColor = Color(0xFFCDD3E2),
              focusedLabelColor = SwarStageIndigo,
              unfocusedLabelColor = Color(0xFF5F667A),
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("onboarding_age_input"),
          )

          if ((ageInputText.isNotBlank() && enteredAge !in 13..120) ||
            (selectedDobIso.isNotBlank() && calculatedAge !in 13..120)
          ) {
            Text(
              text = "इस ऐप का उपयोग करने के लिए उम्र 13 से 120 वर्ष के बीच होनी चाहिए।",
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
              text = if (selectedDobIso.isBlank()) "कैलेंडर से जन्मतिथि चुनें" else "कैलेंडर से जन्मतिथि बदलें",
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      // 3. Room Conduct Checkbox + Mandatory Accept Action
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
        Column(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
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
                text = "3. रूम नियम व प्रवेश अनुमति (Accept करें)",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF171A23),
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "मैं सहमत हूँ कि रूम में किसी भी प्रकार की अभद्र भाषा, गाली-गलौज या दुर्व्यवहार करने पर एडमिन द्वारा मेरा अकाउंट ब्लॉक कर दिया जाएगा और लोकेशन व चैट ऑडिट सुरक्षित रहेगा।",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF424A5E),
              )
            }
          }
          OutlinedButton(
            onClick = { termsAccepted = !termsAccepted },
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (termsAccepted) Color(0xFFE8F5E9) else Color(0xFFF5F7FB),
              contentColor = if (termsAccepted) Color(0xFF1B873F) else Color(0xFF171A23),
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("onboarding_accept_button"),
          ) {
            if (termsAccepted) {
              Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Accept किया गया ✓", fontWeight = FontWeight.Bold)
            } else {
              Text("Accept करें (स्वीकार करें)", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      if (!canSubmit && missingItems.isNotEmpty()) {
        Text(
          text = "अंदर प्रवेश के लिए अभी बाकी है: ${missingItems.joinToString(" • ")}",
          style = MaterialTheme.typography.labelMedium,
          color = SwarGold,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.testTag("onboarding_gate_status"),
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      Button(
        onClick = { onSubmitOnboarding(selectedDobIso, locationResult, termsAccepted) },
        enabled = canSubmit,
        colors = ButtonDefaults.buttonColors(
          containerColor = SwarCyan,
          contentColor = SwarDeepNight,
          disabledContainerColor = Color(0x3300E5FF),
          disabledContentColor = Color(0x88FFFFFF),
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
            text = "Accept करके ऐप में प्रवेश करें",
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
              val pickedDob = AuditDateLocationHelper.dobIsoFromMillis(millis)
              selectedDobIso = pickedDob
              val computed = AuditDateLocationHelper.calculateAgeYears(pickedDob)
              ageInputText = if (computed > 0) computed.toString() else ""
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
