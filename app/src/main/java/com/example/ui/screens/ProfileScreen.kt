package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.CpRelationship
import com.example.data.model.GiftCatalogItem
import com.example.data.model.LevelTierConfig
import com.example.data.model.SwarCatalogDefaults
import com.example.data.model.SwarUserIdentity
import com.example.data.model.UserActivityLog
import com.example.data.model.UserAuditProfile
import com.example.data.model.UserProfile
import com.example.data.repository.AppUpdateInfo
import com.example.ui.components.AdminUserAuditDialog
import com.example.ui.components.AppUpdateCenterDialog
import com.example.ui.components.CpPartnerDialog
import com.example.ui.components.FamilyCrestEmblem
import com.example.ui.components.FramedUserAvatar
import com.example.ui.components.GoldenWingedLevelBadge
import com.example.ui.components.JointCpProfileCard
import com.example.ui.components.LanguageSelectionDialog
import com.example.ui.components.LevelDetailsDialog
import com.example.ui.components.ProfileFeatureInfoDialog
import com.example.ui.components.ProfileMenuRow
import com.example.ui.components.StoreGradientCard
import com.example.ui.components.SupabaseConfigNoticeCard
import com.example.ui.components.SwarStoreDialog
import com.example.ui.components.UnlockedProfileBadgesLine
import com.example.ui.components.WalletGradientCard
import com.example.ui.components.WalletOverviewDialog
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarDeepNight
import com.example.ui.theme.SwarGold
import com.example.ui.viewmodel.UiState

private data class ProfileInfoPayload(
  val title: String,
  val highlight: String,
  val description: String,
)

@Composable
fun ProfileScreen(
  profileState: UiState<UserProfile?>,
  isUploadingMedia: Boolean,
  isSupabaseConfigured: Boolean,
  onUploadAvatarDp: (Uri) -> Unit,
  onUpdateProfile: (displayName: String, handle: String, bio: String, gender: String, clubTag: String) -> Unit,
  onSignOut: () -> Unit,
  isSignedIn: Boolean = true,
  isSigningIn: Boolean = false,
  onSignInClick: (() -> Unit)? = null,
  onClaimBonusCoins: (() -> Unit)? = null,
  giftCatalog: List<GiftCatalogItem> = SwarCatalogDefaults.builtInGifts,
  levelConfigs: Map<Int, LevelTierConfig> = emptyMap(),
  claimedLevelTiers: Set<Int> = emptySet(),
  isAdminUser: Boolean = false,
  onSaveLevelTierConfig: (
    badgeLevel: Int,
    entryVideoLink: String,
    entryVideoUri: Uri?,
    frameLink: String,
    frameUri: Uri?,
    rewardCoins: Long,
    badgeLink: String,
    badgeUri: Uri?,
    typingBannerLink: String,
    typingBannerUri: Uri?,
    onDone: () -> Unit,
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _, onDone -> onDone() },
  onClaimLevelTierReward: (Int) -> Unit = {},
  configuredGithubRepo: String = "",
  appUpdateInfo: AppUpdateInfo? = null,
  isCheckingUpdate: Boolean = false,
  updateCheckError: String? = null,
  onCheckForAppUpdate: () -> Unit = {},
  onSaveGithubRepoLink: (String) -> Unit = {},
  adminAuditProfiles: List<UserAuditProfile> = emptyList(),
  adminActivityLogs: List<UserActivityLog> = emptyList(),
  isPurgingExpiredLogs: Boolean = false,
  onPurgeExpiredAuditLogs: () -> Unit = {},
  onToggleUserBlockByAdmin: (targetUid: String, targetName: String, blocked: Boolean) -> Unit = { _, _, _ -> },
  activeCpLink: CpRelationship? = null,
  incomingCpRequests: List<CpRelationship> = emptyList(),
  outgoingCpRequests: List<CpRelationship> = emptyList(),
  isSubmittingCpAction: Boolean = false,
  onGenerateSyncOwnCpId: () -> Unit = {},
  onSendCpRequestById: (recipientNumericId: String, bondTitle: String) -> Unit = { _, _ -> },
  onAcceptCpRequest: (CpRelationship) -> Unit = {},
  onDeclineOrBreakCp: (CpRelationship) -> Unit = {},
) {
  var showEditDialog by remember { mutableStateOf(false) }
  var showWalletDialog by remember { mutableStateOf(false) }
  var showStoreDialog by remember { mutableStateOf(false) }
  var showLevelDialog by remember { mutableStateOf(false) }
  var showCpPartnerDialog by remember { mutableStateOf(false) }
  var showLanguageDialog by remember { mutableStateOf(false) }
  var showUpdateCenterDialog by remember { mutableStateOf(false) }
  var showAdminAuditDialog by remember { mutableStateOf(false) }
  var selectedLanguage by remember { mutableStateOf("हिंदी (डिफ़ॉल्ट)") }
  var activeFeatureInfo by remember { mutableStateOf<ProfileInfoPayload?>(null) }

  val context = LocalContext.current

  val dpPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri -> uri?.let(onUploadAvatarDp) },
  )

  val profile = (profileState as? UiState.Success)?.data
  val displayName = profile?.displayName.orEmpty().ifBlank { "Swar User" }
  val numericId = remember(profile?.userId) {
    SwarUserIdentity.numericIdForUid(profile?.userId.orEmpty())
  }
  val levelInfo = remember(profile?.coins) {
    SwarUserIdentity.levelForProfile(profile)
  }
  val activeFrameUrl = remember(levelInfo.level, levelConfigs) {
    SwarUserIdentity.highestUnlockedFrameUrl(levelInfo.level, levelConfigs)
  }
  val coins = profile?.coins ?: 1500L
  val clubTag = profile?.clubTag.orEmpty().ifBlank { "SWAR" }

  val copyIdAction = {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("SwarRoom ID", numericId))
    Toast.makeText(context, "ID कॉपी हो गई: $numericId", Toast.LENGTH_SHORT).show()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFFD6F0FC),
            Color(0xFFECF8FD),
            Color(0xFFF6F7FB),
            Color(0xFFF6F7FB),
          )
        )
      )
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Top Header: Large Circular Avatar (with animated Level Frame) + Flag-Accented Name + ID Pill
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(contentAlignment = Alignment.BottomEnd) {
        FramedUserAvatar(
          url = profile?.avatarUrl.orEmpty(),
          name = displayName,
          avatarSize = 76.dp,
          frameUrl = activeFrameUrl,
          borderColor = if (activeFrameUrl.isNotBlank()) SwarGold else Color.White,
          borderWidth = 2.dp,
          modifier = Modifier.clickable {
            dpPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
        )
        Surface(
          onClick = {
            dpPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          shape = CircleShape,
          color = Color.White,
          shadowElevation = 2.dp,
          modifier = Modifier
            .size(26.dp)
            .border(1.dp, Color(0xFFDDE3EE), CircleShape)
            .testTag("change_dp_button"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            if (isUploadingMedia) {
              CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = SwarCyan)
            } else {
              Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "DP बदलें",
                tint = Color(0xFF283044),
                modifier = Modifier.size(14.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Flag,
            contentDescription = null,
            tint = Color(0xFFE53935),
            modifier = Modifier.size(17.dp),
          )
          Text(
            text = displayName,
            style = MaterialTheme.typography.titleLarge,
            fontSize = 20.sp,
            color = Color(0xFF171A23),
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          Icon(
            imageVector = Icons.Filled.Flag,
            contentDescription = null,
            tint = Color(0xFFE53935),
            modifier = Modifier.size(17.dp),
          )
        }

        Surface(
          color = Color(0xFFE9EDF5),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = copyIdAction),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "ID:$numericId",
              style = MaterialTheme.typography.labelMedium,
              color = Color(0xFF70788B),
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.testTag("profile_numeric_id"),
            )
            Icon(
              imageVector = Icons.Filled.ContentCopy,
              contentDescription = "ID कॉपी करें",
              tint = Color(0xFF70788B),
              modifier = Modifier
                .size(13.dp)
                .testTag("copy_own_id_button"),
            )
          }
        }
      }

      IconButton(
        onClick = { showEditDialog = true },
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("edit_profile_switch_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.SwapHoriz,
          contentDescription = "प्रोफ़ाइल संपादित करें",
          tint = Color(0xFF788092),
          modifier = Modifier.size(22.dp),
        )
      }
    }

    // Stats Row (अनुसरण करना | प्रशंसक) + Golden Winged Family/Club Crest on Right
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              activeFeatureInfo = ProfileInfoPayload(
                title = "अनुसरण (Following)",
                highlight = "क्लब बैज: $clubTag",
                description = "रूम में किसी भी सदस्य की प्रोफ़ाइल खोलकर आप उनसे जुड़ सकते हैं और निजी संदेश भेज सकते हैं।",
              )
            }
            .padding(vertical = 6.dp),
        ) {
          Text(
            text = "0",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF171A23),
            fontWeight = FontWeight.ExtraBold,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "अनुसरण करना",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8B92A5),
          )
        }

        Box(
          modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color(0xFFDDE2EC))
        )

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              activeFeatureInfo = ProfileInfoPayload(
                title = "प्रशंसक (Fans)",
                highlight = "स्तर: Lv.${levelInfo.level} • ${levelInfo.title}",
                description = "रूम में होस्टिंग, बातचीत और गिफ्टिंग से आपके प्रशंसक और लोकप्रियता तेजी से बढ़ती है।",
              )
            }
            .padding(vertical = 6.dp),
        ) {
          Text(
            text = "0",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF171A23),
            fontWeight = FontWeight.ExtraBold,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "प्रशंसक",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8B92A5),
          )
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      FamilyCrestEmblem(
        clubTag = clubTag,
        onClick = { showEditDialog = true },
      )
    }

    // Unlocked Level Achievement Badges Line near top of Profile ("जितना लेवल खुलता जाएगा वह प्रोफाइल के ऊपर की लाइन पर दिखे")
    UnlockedProfileBadgesLine(
      userLevel = levelInfo.level,
      levelConfigs = levelConfigs,
      onOpenLevelCards = { showLevelDialog = true },
    )

    if (activeCpLink != null) {
      JointCpProfileCard(
        cp = activeCpLink,
        focusUid = profile?.userId.orEmpty(),
        darkSurface = false,
        modifier = Modifier
          .clickable { showCpPartnerDialog = true }
          .testTag("profile_joint_cp_banner"),
      )
    }

    // Two Vibrant Side-by-Side Feature Cards: वॉलेट (Wallet) & स्टोर (Store)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      WalletGradientCard(
        coins = coins,
        onClick = { showWalletDialog = true },
        modifier = Modifier.weight(1f),
      )
      StoreGradientCard(
        onClick = { showStoreDialog = true },
        modifier = Modifier.weight(1f),
      )
    }

    // Main Pure-White Rounded Menu List Card (Matching Reference Screenshot Rows)
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color.White,
      shadowElevation = 1.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        ProfileMenuRow(
          title = "सिक्के प्राप्त करने के लिए आमंत्रित करें",
          icon = Icons.Filled.MonetizationOn,
          iconTint = Color.White,
          iconBgColor = Color(0xFFFFA726),
          onClick = {
            val inviteText = "SwarRoom पर मेरे लाइव वॉइस रूम से जुड़ें! मेरी ID: $numericId"
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("SwarRoom Invite", inviteText))
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, inviteText)
            }
            runCatching {
              context.startActivity(Intent.createChooser(shareIntent, "दोस्तों को आमंत्रित करें"))
            }.onFailure {
              Toast.makeText(context, "आमंत्रण कॉपी हो गया!", Toast.LENGTH_SHORT).show()
            }
          },
        )
        MenuHairlineDivider()

        ProfileMenuRow(
          title = "लव हाउस",
          icon = Icons.Filled.Home,
          iconTint = Color.White,
          iconBgColor = Color(0xFF7B6BFF),
          onClick = {
            activeFeatureInfo = ProfileInfoPayload(
              title = "लव हाउस (क्लब परिवार)",
              highlight = "आपका क्लब: $clubTag",
              description = "अपने दोस्तों और रूम मेंबर्स के साथ एक खास क्लब टैग साझा करें और लाइव रूम में एक साथ चमकें।",
            )
          },
        )
        MenuHairlineDivider()

        val cpBadgeText = when {
          incomingCpRequests.isNotEmpty() -> "🔔 ${incomingCpRequests.size} नया"
          activeCpLink != null -> {
            val cpInfo = SwarUserIdentity.cpLevelInfoFor(activeCpLink.cpXp, activeCpLink.acceptedAtMs)
            "CP Lv.${cpInfo.level}"
          }
          outgoingCpRequests.isNotEmpty() -> "प्रतीक्षारत"
          else -> "CP जोड़ें"
        }
        ProfileMenuRow(
          title = "CP स्तर • कपल / दोस्त जोड़ें",
          icon = Icons.Filled.Favorite,
          iconTint = Color.White,
          iconBgColor = Color(0xFFFF4081),
          showNewBadge = incomingCpRequests.isNotEmpty(),
          trailingText = cpBadgeText,
          onClick = { showCpPartnerDialog = true },
          modifier = Modifier.testTag("profile_cp_row"),
        )
        MenuHairlineDivider()

        ProfileMenuRow(
          title = "बैगपैक",
          icon = Icons.Filled.Backpack,
          iconTint = Color.White,
          iconBgColor = Color(0xFF29B6F6),
          onClick = { showStoreDialog = true },
        )
        MenuHairlineDivider()

        ProfileMenuRow(
          title = "पदक",
          icon = Icons.Filled.MilitaryTech,
          iconTint = Color.White,
          iconBgColor = Color(0xFFFF9800),
          onClick = {
            activeFeatureInfo = ProfileInfoPayload(
              title = "उपलब्धि पदक",
              highlight = "वर्तमान पदक स्तर: Lv.${levelInfo.level}",
              description = "उपाधि '${levelInfo.title}' सक्रिय है! अधिक कॉइंस और लाइव रूम गतिविधि से नए गोल्डन पदक अनलॉक होते हैं।",
            )
          },
        )
        MenuHairlineDivider()

        ProfileMenuRow(
          title = "स्तर",
          icon = Icons.Filled.Shield,
          iconTint = Color.White,
          iconBgColor = Color(0xFF4CD964),
          trailingContent = {
            GoldenWingedLevelBadge(level = levelInfo.level)
          },
          onClick = { showLevelDialog = true },
          modifier = Modifier.testTag("profile_level_row"),
        )
        MenuHairlineDivider()

        ProfileMenuRow(
          title = "SVIP",
          icon = Icons.Filled.Diamond,
          iconTint = Color.White,
          iconBgColor = Color(0xFF8E54E9),
          onClick = {
            activeFeatureInfo = ProfileInfoPayload(
              title = "SVIP विशेषाधिकार",
              highlight = "अकाउंट आईडी: $numericId",
              description = "रूम में रॉयल एंट्री, खास क्लब बैज ($clubTag) और प्रीमियम गिफ्ट अनुभव के साथ अलग पहचान पाएं।",
            )
          },
        )
        MenuHairlineDivider()

        ProfileMenuRow(
          title = "शिष्टजन",
          icon = Icons.Filled.WorkspacePremium,
          iconTint = Color.White,
          iconBgColor = Color(0xFFFFB300),
          showNewBadge = true,
          onClick = {
            activeFeatureInfo = ProfileInfoPayload(
              title = "शिष्टजन (रॉयल मेंबरशिप)",
              highlight = "उपाधि: ${levelInfo.title}",
              description = "रॉयल वॉइस स्टेज पर विशेष सम्मान, चमकदार गिफ्ट अनाउंसमेंट रिबन और प्राथमिकता सीट पहचान।",
            )
          },
        )
      }
    }

    // Second Pure-White Card: भाषा, प्रोफ़ाइल संपादन, और खाता क्रिया
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color.White,
      shadowElevation = 1.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        ProfileMenuRow(
          title = "भाषा",
          icon = Icons.Filled.Language,
          iconTint = Color(0xFF1E2230),
          iconBgColor = Color(0xFFEDF1F7),
          trailingText = selectedLanguage,
          onClick = { showLanguageDialog = true },
        )
        MenuHairlineDivider()
        ProfileMenuRow(
          title = "प्रोफ़ाइल संपादित करें",
          icon = Icons.Filled.Edit,
          iconTint = Color(0xFF4C3585),
          iconBgColor = Color(0xFFEEE9FF),
          onClick = { showEditDialog = true },
        )
        MenuHairlineDivider()
        ProfileMenuRow(
          title = "ऐप अपडेट (GitHub)",
          icon = Icons.Filled.SystemUpdate,
          iconTint = Color(0xFF006CB5),
          iconBgColor = Color(0xFFE1F3FF),
          showNewBadge = appUpdateInfo?.hasUpdate == true,
          trailingText = if (appUpdateInfo?.hasUpdate == true) {
            appUpdateInfo.latestVersionTag
          } else {
            "v${BuildConfig.VERSION_NAME}"
          },
          onClick = { showUpdateCenterDialog = true },
          modifier = Modifier.testTag("profile_app_update_row"),
        )
        if (isAdminUser) {
          MenuHairlineDivider()
          ProfileMenuRow(
            title = "यूज़र एक्टिविटी और ऑडिट (7 माह)",
            icon = Icons.Filled.AdminPanelSettings,
            iconTint = Color.White,
            iconBgColor = Color(0xFF4C3585),
            showNewBadge = true,
            trailingText = "${adminAuditProfiles.size} यूज़र्स",
            onClick = {
              onPurgeExpiredAuditLogs()
              showAdminAuditDialog = true
            },
            modifier = Modifier.testTag("profile_admin_audit_row"),
          )
        }
      }
    }

    if (!isSupabaseConfigured) {
      SupabaseConfigNoticeCard(isConfigured = false)
    }

    if (isSignedIn) {
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
    } else if (onSignInClick != null) {
      Button(
        onClick = onSignInClick,
        enabled = !isSigningIn,
        colors = ButtonDefaults.buttonColors(containerColor = SwarCyan, contentColor = SwarDeepNight),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("profile_google_sign_in_button"),
      ) {
        if (isSigningIn) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = SwarDeepNight)
        } else {
          Text("Google से साइन इन करें", fontWeight = FontWeight.Bold)
        }
      }
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

  if (showWalletDialog) {
    WalletOverviewDialog(
      coins = coins,
      levelInfo = levelInfo,
      canClaimBonus = onClaimBonusCoins != null,
      onClaimBonusCoins = { onClaimBonusCoins?.invoke() },
      onDismiss = { showWalletDialog = false },
    )
  }

  if (showStoreDialog) {
    SwarStoreDialog(
      coins = coins,
      gifts = giftCatalog,
      onDismiss = { showStoreDialog = false },
    )
  }

  if (showLevelDialog) {
    LevelDetailsDialog(
      levelInfo = levelInfo,
      coins = coins,
      onDismiss = { showLevelDialog = false },
      levelConfigs = levelConfigs,
      claimedTiers = claimedLevelTiers,
      isAdminUser = isAdminUser,
      isUploadingMedia = isUploadingMedia,
      sampleAvatarUrl = profile?.avatarUrl.orEmpty(),
      sampleDisplayName = displayName,
      onSaveTierConfig = onSaveLevelTierConfig,
      onClaimTierReward = onClaimLevelTierReward,
    )
  }

  if (showLanguageDialog) {
    LanguageSelectionDialog(
      currentLanguage = selectedLanguage,
      onSelect = {
        selectedLanguage = it
        showLanguageDialog = false
      },
      onDismiss = { showLanguageDialog = false },
    )
  }

  val featureInfo = activeFeatureInfo
  if (featureInfo != null) {
    ProfileFeatureInfoDialog(
      title = featureInfo.title,
      highlightBadge = featureInfo.highlight,
      description = featureInfo.description,
      onDismiss = { activeFeatureInfo = null },
    )
  }

  if (showUpdateCenterDialog) {
    AppUpdateCenterDialog(
      currentVersionName = BuildConfig.VERSION_NAME,
      currentVersionCode = BuildConfig.VERSION_CODE,
      configuredGithubRepo = configuredGithubRepo,
      appUpdateInfo = appUpdateInfo,
      isCheckingUpdate = isCheckingUpdate,
      updateCheckError = updateCheckError,
      isAdminUser = isAdminUser,
      onCheckForUpdate = onCheckForAppUpdate,
      onSaveGithubRepoLink = onSaveGithubRepoLink,
      onDismiss = { showUpdateCenterDialog = false },
    )
  }

  if (showAdminAuditDialog && isAdminUser) {
    AdminUserAuditDialog(
      auditProfiles = adminAuditProfiles,
      activityLogs = adminActivityLogs,
      isPurgingExpired = isPurgingExpiredLogs,
      onPurgeExpiredLogs = onPurgeExpiredAuditLogs,
      onToggleUserBlock = onToggleUserBlockByAdmin,
      onDismiss = { showAdminAuditDialog = false },
    )
  }

  if (showCpPartnerDialog) {
    CpPartnerDialog(
      currentUserId = profile?.userId.orEmpty(),
      currentDisplayName = displayName,
      currentAvatarUrl = profile?.avatarUrl.orEmpty(),
      activeCpLink = activeCpLink,
      incomingRequests = incomingCpRequests,
      outgoingRequests = outgoingCpRequests,
      isSubmitting = isSubmittingCpAction,
      onGenerateSyncOwnId = onGenerateSyncOwnCpId,
      onSendRequestById = onSendCpRequestById,
      onAcceptRequest = onAcceptCpRequest,
      onDeclineOrBreak = onDeclineOrBreakCp,
      onDismiss = { showCpPartnerDialog = false },
    )
  }
}

@Composable
private fun MenuHairlineDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(start = 60.dp, end = 16.dp),
    thickness = 0.6.dp,
    color = Color(0xFFF0F2F7),
  )
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
          label = { Text("क्लब / परिवार बैज") },
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
