package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.SwarUserIdentity
import com.example.data.model.UserActivityLog
import com.example.data.model.UserAuditProfile
import com.example.data.repository.AuditDateLocationHelper
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarDeepNight
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarStageIndigo

private data class AuditUserSummary(
  val userId: String,
  val numericId: String,
  val displayName: String,
  val userEmail: String,
  val avatarUrl: String,
  val dobIso: String,
  val ageYears: Long,
  val locationLabel: String,
  val locationLat: Double,
  val locationLng: Double,
  val locationStatus: String,
  val isBlockedByAdmin: Boolean,
  val firstLoginMs: Long,
  val totalRoomSeconds: Long,
  val roomVisits: List<UserActivityLog>,
  val roomChats: List<UserActivityLog>,
  val inboxLogs: List<UserActivityLog>,
  val inboxMediaCount: Int,
)

private data class InboxFriendSummary(
  val targetUid: String,
  val targetNumericId: String,
  val targetName: String,
  val textCount: Int,
  val mediaCount: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserAuditDialog(
  auditProfiles: List<UserAuditProfile>,
  activityLogs: List<UserActivityLog>,
  isPurgingExpired: Boolean,
  onPurgeExpiredLogs: () -> Unit,
  onToggleUserBlock: (targetUid: String, targetName: String, blocked: Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedDateKey by remember { mutableStateOf<String?>(null) }
  var showCalendarPicker by remember { mutableStateOf(false) }
  var selectedUserId by remember { mutableStateOf<String?>(null) }
  var selectedDetailTab by remember { mutableIntStateOf(0) }
  var previewImageUrl by remember { mutableStateOf<String?>(null) }

  val todayKey = remember { AuditDateLocationHelper.dateKeyFromMillis() }

  val filteredLogs = remember(activityLogs, selectedDateKey) {
    if (selectedDateKey == null) {
      activityLogs
    } else {
      activityLogs.filter { it.dateKey == selectedDateKey }
    }
  }

  val userSummaries = remember(auditProfiles, filteredLogs, activityLogs) {
    val profileMap = auditProfiles.associateBy { it.userId }
    val allLogsByUser = activityLogs.groupBy { it.userId }
    val dateLogsByUser = filteredLogs.groupBy { it.userId }
    val allUids = (profileMap.keys + allLogsByUser.keys).filter { it.isNotBlank() }.distinct()

    allUids.map { uid ->
      val p = profileMap[uid]
      val fallbackLog = allLogsByUser[uid].orEmpty().firstOrNull()
      val logs = dateLogsByUser[uid].orEmpty()
      val visits = logs.filter { it.activityType == "room_visit" }
      val roomChats = logs.filter { it.activityType == "room_chat" }
      val inbox = logs.filter { it.activityType == "inbox_chat" || it.activityType == "inbox_media" }
      val mediaCount = inbox.count { it.activityType == "inbox_media" || it.mediaUrl.isNotBlank() }

      AuditUserSummary(
        userId = uid,
        numericId = p?.numericId?.ifBlank { SwarUserIdentity.numericIdForUid(uid) }
          ?: fallbackLog?.numericId?.ifBlank { SwarUserIdentity.numericIdForUid(uid) }
          ?: SwarUserIdentity.numericIdForUid(uid),
        displayName = p?.displayName?.ifBlank { fallbackLog?.userName ?: "Swar User" }
          ?: fallbackLog?.userName?.ifBlank { "Swar User" }
          ?: "Swar User",
        userEmail = p?.userEmail?.ifBlank { fallbackLog?.userEmail.orEmpty() }
          ?: fallbackLog?.userEmail.orEmpty(),
        avatarUrl = p?.avatarUrl?.ifBlank { fallbackLog?.userAvatarUrl.orEmpty() }
          ?: fallbackLog?.userAvatarUrl.orEmpty(),
        dobIso = p?.dobIso.orEmpty(),
        ageYears = p?.ageYears ?: 0L,
        locationLabel = p?.locationLabel.orEmpty(),
        locationLat = p?.locationLat ?: 0.0,
        locationLng = p?.locationLng ?: 0.0,
        locationStatus = p?.locationStatus ?: "pending",
        isBlockedByAdmin = p?.isBlockedByAdmin == true,
        firstLoginMs = p?.firstLoginAt?.toDate()?.time ?: 0L,
        totalRoomSeconds = visits.sumOf { it.durationSeconds.coerceAtLeast(0L) },
        roomVisits = visits,
        roomChats = roomChats,
        inboxLogs = inbox,
        inboxMediaCount = mediaCount,
      )
    }.sortedWith(
      compareByDescending<AuditUserSummary> {
        it.totalRoomSeconds + (it.roomChats.size * 10L) + (it.inboxLogs.size * 10L)
      }.thenByDescending { it.firstLoginMs }
    )
  }

  val displayedUsers = remember(userSummaries, searchQuery, selectedDateKey) {
    val q = searchQuery.trim().lowercase()
    userSummaries.filter { u ->
      val dateOk = if (selectedDateKey == null) {
        true
      } else {
        u.roomVisits.isNotEmpty() || u.roomChats.isNotEmpty() || u.inboxLogs.isNotEmpty()
      }
      val searchOk = if (q.isBlank()) {
        true
      } else {
        u.numericId.lowercase().contains(q) ||
          u.displayName.lowercase().contains(q) ||
          u.userEmail.lowercase().contains(q) ||
          u.userId.lowercase().contains(q) ||
          u.locationLabel.lowercase().contains(q)
      }
      dateOk && searchOk
    }
  }

  val selectedUser = remember(userSummaries, selectedUserId) {
    userSummaries.firstOrNull { it.userId == selectedUserId }
  }

  Dialog(
    onDismissRequest = {
      if (selectedUser != null) {
        selectedUserId = null
      } else {
        onDismiss()
      }
    },
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    if (selectedUser != null) {
      BackHandler { selectedUserId = null }
    }

    Surface(
      modifier = Modifier
        .fillMaxSize()
        .safeDrawingPadding()
        .testTag("admin_user_audit_dialog"),
      color = Color(0xFFF4F6FB),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Gradient Header
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(SwarDeepNight, SwarStageIndigo)))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (selectedUser != null) {
              IconButton(
                onClick = { selectedUserId = null },
                modifier = Modifier.testTag("audit_back_to_list_button"),
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "सूची पर लौटें", tint = Color.White)
              }
            }
            Column(modifier = Modifier.weight(1f).padding(start = if (selectedUser == null) 6.dp else 0.dp)) {
              Text(
                text = if (selectedUser != null) "${selectedUser.displayName} • रिपोर्ट" else "यूज़र एक्टिविटी और ऑडिट रिपोर्ट",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = if (selectedDateKey == null) {
                  "7 माह स्टोरेज • ऑटो-डिलीट सक्रिय (${displayedUsers.size} यूज़र्स)"
                } else {
                  "चयनित दिनांक: ${AuditDateLocationHelper.formatDateDisplayFromKey(selectedDateKey!!)}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = SwarCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            IconButton(
              onClick = onPurgeExpiredLogs,
              enabled = !isPurgingExpired,
              modifier = Modifier.testTag("audit_purge_expired_button"),
            ) {
              if (isPurgingExpired) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SwarCyan)
              } else {
                Icon(
                  imageVector = Icons.Filled.DeleteSweep,
                  contentDescription = "7 महीने पुराने लॉग हटाएं",
                  tint = SwarGold,
                )
              }
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.testTag("audit_close_button"),
            ) {
              Icon(Icons.Filled.Close, contentDescription = "बंद करें", tint = Color.White)
            }
          }
        }

        // Date Filter Bar (Always visible so Admin can switch date in List or User Detail!)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FilterChip(
            selected = selectedDateKey == null,
            onClick = { selectedDateKey = null },
            label = { Text("सभी (7 माह)") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFFE1F3FF),
              selectedLabelColor = Color(0xFF006CB5),
            ),
            modifier = Modifier.testTag("audit_filter_all"),
          )
          FilterChip(
            selected = selectedDateKey == todayKey,
            onClick = { selectedDateKey = todayKey },
            label = { Text("आज") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFFE1F3FF),
              selectedLabelColor = Color(0xFF006CB5),
            ),
            modifier = Modifier.testTag("audit_filter_today"),
          )
          FilterChip(
            selected = selectedDateKey != null && selectedDateKey != todayKey,
            onClick = { showCalendarPicker = true },
            leadingIcon = {
              Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = {
              Text(
                if (selectedDateKey != null && selectedDateKey != todayKey) {
                  AuditDateLocationHelper.formatDateDisplayFromKey(selectedDateKey!!)
                } else {
                  "तारीख चुनें"
                }
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFFE1F3FF),
              selectedLabelColor = Color(0xFF006CB5),
            ),
            modifier = Modifier.testTag("audit_filter_calendar"),
          )
        }
        HorizontalDivider(color = Color(0xFFE4E9F2))

        if (selectedUser == null) {
          // Search Bar
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ID नंबर, नाम या Gmail से खोजें...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Filled.Close, contentDescription = "साफ़ करें")
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp)
              .testTag("audit_search_input"),
          )

          if (displayedUsers.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (selectedDateKey != null) {
                  "इस तारीख (${AuditDateLocationHelper.formatDateDisplayFromKey(selectedDateKey!!)}) पर कोई गतिविधि रिकॉर्ड नहीं मिला।"
                } else {
                  "अभी कोई यूज़र ऑडिट डेटा उपलब्ध नहीं है।"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5D657A),
              )
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxSize(),
            ) {
              items(displayedUsers, key = { it.userId }) { user ->
                AuditUserSummaryCard(
                  user = user,
                  onClick = {
                    selectedDetailTab = 0
                    selectedUserId = user.userId
                  },
                )
              }
            }
          }
        } else {
          AdminAuditUserDetailView(
            user = selectedUser,
            selectedTab = selectedDetailTab,
            onTabSelected = { selectedDetailTab = it },
            onToggleBlock = {
              onToggleUserBlock(
                selectedUser.userId,
                selectedUser.displayName,
                !selectedUser.isBlockedByAdmin,
              )
            },
            onPreviewImage = { previewImageUrl = it },
          )
        }
      }
    }
  }

  if (showCalendarPicker) {
    val todayMs = remember { System.currentTimeMillis() }
    val datePickerState = rememberDatePickerState(
      selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
          return utcTimeMillis <= todayMs
        }
      }
    )
    DatePickerDialog(
      onDismissRequest = { showCalendarPicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              selectedDateKey = AuditDateLocationHelper.dobIsoFromMillis(millis)
            }
            showCalendarPicker = false
          },
        ) {
          Text("लागू करें", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCalendarPicker = false }) {
          Text("रद्द करें")
        }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  val imgUrl = previewImageUrl
  if (!imgUrl.isNullOrBlank()) {
    Dialog(onDismissRequest = { previewImageUrl = null }) {
      Surface(shape = RoundedCornerShape(18.dp), color = Color.Black) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          AsyncImage(
            model = imgUrl,
            contentDescription = "भेजी गई DP / फ़ोटो",
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxWidth()
              .height(340.dp)
              .clip(RoundedCornerShape(12.dp)),
          )
          Spacer(modifier = Modifier.height(8.dp))
          TextButton(onClick = { previewImageUrl = null }) {
            Text("बंद करें", color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun AuditUserSummaryCard(
  user: AuditUserSummary,
  onClick: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("audit_user_card_${user.numericId}"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        UserAvatar(
          url = user.avatarUrl,
          name = user.displayName,
          size = 50.dp,
          borderColor = if (user.isBlockedByAdmin) SwarCoral else SwarCyan,
          borderWidth = 1.5.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = user.displayName,
              style = MaterialTheme.typography.titleSmall,
              color = Color(0xFF171A23),
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE7F4FF)) {
              Text(
                text = "ID:${user.numericId}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF006CB5),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
            if (user.isBlockedByAdmin) {
              Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFEBEE)) {
                Text(
                  text = "ब्लॉक",
                  style = MaterialTheme.typography.labelSmall,
                  color = SwarCoral,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }
          if (user.userEmail.isNotBlank()) {
            Text(
              text = user.userEmail,
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF5D657A),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          if (user.dobIso.isNotBlank()) {
            Text(
              text = "जन्मतिथि: ${AuditDateLocationHelper.formatDobDisplay(user.dobIso)} (${user.ageYears} वर्ष)",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF424A5E),
            )
          }
          if (user.locationLabel.isNotBlank()) {
            Text(
              text = "📍 ${user.locationLabel}",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF1B873F),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        AuditMiniStatPill(
          label = "समय: ${AuditDateLocationHelper.formatDurationHindi(user.totalRoomSeconds)}",
          bgColor = Color(0xFFEDF7FF),
          textColor = Color(0xFF006CB5),
          modifier = Modifier.weight(1f),
        )
        AuditMiniStatPill(
          label = "रूम चैट: ${user.roomChats.size}",
          bgColor = Color(0xFFF3EEFF),
          textColor = Color(0xFF5E35B1),
        )
        AuditMiniStatPill(
          label = "इनबॉक्स: ${user.inboxLogs.size} (DP: ${user.inboxMediaCount})",
          bgColor = Color(0xFFEAF9EF),
          textColor = Color(0xFF1B873F),
        )
      }
    }
  }
}

@Composable
private fun AuditMiniStatPill(
  label: String,
  bgColor: Color,
  textColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = bgColor,
    modifier = modifier,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = textColor,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
    )
  }
}

@Composable
private fun AdminAuditUserDetailView(
  user: AuditUserSummary,
  selectedTab: Int,
  onTabSelected: (Int) -> Unit,
  onToggleBlock: () -> Unit,
  onPreviewImage: (String) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    // Identity & Block Control Card
    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          UserAvatar(
            url = user.avatarUrl,
            name = user.displayName,
            size = 58.dp,
            borderColor = if (user.isBlockedByAdmin) SwarCoral else SwarCyan,
            borderWidth = 2.dp,
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = user.displayName,
              style = MaterialTheme.typography.titleMedium,
              color = Color(0xFF171A23),
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text = "ID: ${user.numericId}" + if (user.userEmail.isNotBlank()) " • ${user.userEmail}" else "",
              style = MaterialTheme.typography.labelMedium,
              color = Color(0xFF5D657A),
            )
            if (user.dobIso.isNotBlank()) {
              Text(
                text = "जन्मतिथि: ${AuditDateLocationHelper.formatDobDisplay(user.dobIso)} • उम्र: ${user.ageYears} वर्ष",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF333B4E),
              )
            }
          }
          Button(
            onClick = onToggleBlock,
            colors = ButtonDefaults.buttonColors(
              containerColor = if (user.isBlockedByAdmin) Color(0xFF1B873F) else SwarCoral,
              contentColor = Color.White,
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("audit_toggle_block_button"),
          ) {
            Icon(
              imageVector = if (user.isBlockedByAdmin) Icons.Filled.CheckCircle else Icons.Filled.Block,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (user.isBlockedByAdmin) "अनब्लॉक" else "ब्लॉक करें",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        if (user.locationLabel.isNotBlank() || user.locationStatus == "captured") {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(16.dp))
            val coordSuffix = if (user.locationLat != 0.0 || user.locationLng != 0.0) {
              " (${String.format("%.3f, %.3f", user.locationLat, user.locationLng)})"
            } else {
              ""
            }
            Text(
              text = "प्रथम लोकेशन: ${user.locationLabel}$coordSuffix",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF333B4E),
            )
          }
        }
      }
    }

    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = Color.White,
      contentColor = Color(0xFF006CB5),
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { onTabSelected(0) },
        text = { Text("रूम समय (${user.roomVisits.size})") },
        modifier = Modifier.testTag("audit_tab_rooms"),
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { onTabSelected(1) },
        text = { Text("रूम चैट (${user.roomChats.size})") },
        modifier = Modifier.testTag("audit_tab_room_chats"),
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { onTabSelected(2) },
        text = { Text("इनबॉक्स/DP (${user.inboxLogs.size})") },
        modifier = Modifier.testTag("audit_tab_inbox"),
      )
    }

    when (selectedTab) {
      0 -> RoomVisitsTabContent(user.roomVisits, user.totalRoomSeconds)
      1 -> RoomChatsTabContent(user.roomChats)
      else -> InboxLogsTabContent(user.inboxLogs, onPreviewImage)
    }
  }
}

@Composable
private fun RoomVisitsTabContent(
  visits: List<UserActivityLog>,
  totalSeconds: Long,
) {
  if (visits.isEmpty()) {
    EmptyAuditTabState("इस अवधि में कोई रूम विज़िट रिकॉर्ड नहीं मिला।")
    return
  }
  val groupedByRoom = remember(visits) {
    visits.groupBy { it.roomId.ifBlank { it.roomTitle } }
      .entries
      .map { (_, list) ->
        Triple(
          list.first().roomTitle.ifBlank { "वॉइस रूम" },
          list.size,
          list.sumOf { it.durationSeconds.coerceAtLeast(0L) },
        )
      }
      .sortedByDescending { it.third }
  }

  LazyColumn(
    contentPadding = PaddingValues(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F4FF)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.AccessTime, contentDescription = null, tint = Color(0xFF006CB5), modifier = Modifier.size(18.dp))
            Text(
              text = "कुल रूम समय: ${AuditDateLocationHelper.formatDurationHindi(totalSeconds)}",
              style = MaterialTheme.typography.titleSmall,
              color = Color(0xFF004F87),
              fontWeight = FontWeight.ExtraBold,
            )
          }
          groupedByRoom.forEach { (title, count, secs) ->
            Text(
              text = "• $title: $count बार (${AuditDateLocationHelper.formatDurationHindi(secs)})",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF1F2D45),
            )
          }
        }
      }
    }
    items(visits, key = { it.id.ifBlank { "${it.roomId}_${it.enteredAtMs}" } }) { log ->
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.MeetingRoom, contentDescription = null, tint = Color(0xFF008CE6), modifier = Modifier.size(18.dp))
            Text(
              text = log.roomTitle.ifBlank { "वॉइस रूम" },
              style = MaterialTheme.typography.titleSmall,
              color = Color(0xFF171A23),
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
            )
            if (log.roomCode.isNotBlank()) {
              Text(
                text = "#${log.roomCode}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B7388),
              )
            }
          }
          Text(
            text = "दिनांक: ${AuditDateLocationHelper.formatDateDisplayFromKey(log.dateKey)} • अवधि: ${AuditDateLocationHelper.formatDurationHindi(log.durationSeconds)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF1B873F),
            fontWeight = FontWeight.SemiBold,
          )
          val enterStr = AuditDateLocationHelper.formatTimeDisplay(log.enteredAtMs)
          val exitStr = if (log.exitedAtMs > log.enteredAtMs) {
            AuditDateLocationHelper.formatTimeDisplay(log.exitedAtMs)
          } else {
            "सक्रिय / अपडेटेड"
          }
          Text(
            text = "प्रवेश: $enterStr → बाहर: $exitStr",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF5D657A),
          )
        }
      }
    }
  }
}

@Composable
private fun RoomChatsTabContent(chats: List<UserActivityLog>) {
  if (chats.isEmpty()) {
    EmptyAuditTabState("इस अवधि में रूम में कोई टाइप किया हुआ चैट रिकॉर्ड नहीं मिला।")
    return
  }
  LazyColumn(
    contentPadding = PaddingValues(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    items(chats, key = { it.id.ifBlank { "${it.roomId}_${it.enteredAtMs}_${it.textContent.hashCode()}" } }) { log ->
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color(0xFF5E35B1), modifier = Modifier.size(16.dp))
            Text(
              text = log.roomTitle.ifBlank { "वॉइस रूम" },
              style = MaterialTheme.typography.labelMedium,
              color = Color(0xFF5E35B1),
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = AuditDateLocationHelper.formatTimeDisplay(log.enteredAtMs),
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF6B7388),
            )
          }
          Text(
            text = log.textContent,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF171A23),
          )
        }
      }
    }
  }
}

@Composable
private fun InboxLogsTabContent(
  inboxLogs: List<UserActivityLog>,
  onPreviewImage: (String) -> Unit,
) {
  if (inboxLogs.isEmpty()) {
    EmptyAuditTabState("इस अवधि में कोई इनबॉक्स चैट या भेजी गई DP रिकॉर्ड नहीं मिली।")
    return
  }
  val friendSummaries = remember(inboxLogs) {
    inboxLogs.groupBy { it.targetUid.ifBlank { it.targetName } }
      .entries
      .map { (_, list) ->
        val first = list.first()
        InboxFriendSummary(
          targetUid = first.targetUid,
          targetNumericId = first.targetNumericId,
          targetName = first.targetName.ifBlank { "दोस्त" },
          textCount = list.count { it.activityType == "inbox_chat" },
          mediaCount = list.count { it.activityType == "inbox_media" || it.mediaUrl.isNotBlank() },
        )
      }
      .sortedByDescending { it.textCount + it.mediaCount }
  }

  LazyColumn(
    contentPadding = PaddingValues(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxSize(),
  ) {
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF9EF)),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.People, contentDescription = null, tint = Color(0xFF1B873F), modifier = Modifier.size(18.dp))
            Text(
              text = "दोस्त व इनबॉक्स सारांश (${friendSummaries.size} संपर्क)",
              style = MaterialTheme.typography.titleSmall,
              color = Color(0xFF145E2C),
              fontWeight = FontWeight.ExtraBold,
            )
          }
          friendSummaries.forEach { friend ->
            val idTag = if (friend.targetNumericId.isNotBlank()) " (ID:${friend.targetNumericId})" else ""
            Text(
              text = "• ${friend.targetName}$idTag — चैट: ${friend.textCount}, भेजी गई DP: ${friend.mediaCount}",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF1E3A2A),
            )
          }
        }
      }
    }

    items(inboxLogs, key = { it.id.ifBlank { "${it.conversationId}_${it.enteredAtMs}_${it.textContent.hashCode()}" } }) { log ->
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
              imageVector = if (log.mediaUrl.isNotBlank()) Icons.Filled.Photo else Icons.AutoMirrored.Filled.Chat,
              contentDescription = null,
              tint = Color(0xFF1B873F),
              modifier = Modifier.size(16.dp),
            )
            val idSuffix = if (log.targetNumericId.isNotBlank()) " (ID:${log.targetNumericId})" else ""
            Text(
              text = "दोस्त: ${log.targetName.ifBlank { "Swar Friend" }}$idSuffix",
              style = MaterialTheme.typography.labelMedium,
              color = Color(0xFF1B873F),
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = AuditDateLocationHelper.formatTimeDisplay(log.enteredAtMs),
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF6B7388),
            )
          }
          if (log.textContent.isNotBlank()) {
            Text(
              text = log.textContent,
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFF171A23),
            )
          }
          if (log.mediaUrl.isNotBlank()) {
            AsyncImage(
              model = log.mediaUrl,
              contentDescription = "इनबॉक्स में भेजी गई DP / फ़ोटो",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFDDE3EE), RoundedCornerShape(12.dp))
                .clickable { onPreviewImage(log.mediaUrl) }
                .testTag("audit_inbox_dp_${log.id}"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyAuditTabState(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = Color(0xFF5D657A),
    )
  }
}
