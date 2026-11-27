package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.WorkspacePremium
import com.example.data.repository.LocalMediaCacheManager
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.LevelTierConfig
import com.example.data.model.LevelTierSpec
import com.example.data.model.SwarUserIdentity
import com.example.data.repository.SupabaseNetworkClient
import com.example.data.repository.VideoLinkResolution
import com.example.data.repository.VideoLinkResolver
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarMint
import com.example.ui.theme.SwarViolet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Circular UserAvatar overlaid cleanly with an animated level avatar frame (GIF/WebP/PNG).
 */
@Composable
fun FramedUserAvatar(
  url: String,
  name: String,
  avatarSize: Dp,
  frameUrl: String = "",
  borderColor: Color = SwarCyan,
  borderWidth: Dp = 2.dp,
  modifier: Modifier = Modifier,
) {
  val hasFrame = frameUrl.isNotBlank()
  val outerSize = if (hasFrame) avatarSize * 1.24f else avatarSize

  Box(
    modifier = modifier.size(outerSize),
    contentAlignment = Alignment.Center,
  ) {
    if (hasFrame) {
      val pulse = rememberInfiniteTransition(label = "frameGlow")
      val glowAlpha by pulse.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.56f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha",
      )
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(SwarGold.copy(alpha = glowAlpha), Color.Transparent),
          ),
        )
      }
    }

    UserAvatar(
      url = url,
      name = name,
      size = avatarSize,
      borderColor = borderColor,
      borderWidth = borderWidth,
    )

    if (hasFrame) {
      AsyncImage(
        model = frameUrl,
        contentDescription = "लेवल फ्रेम",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
      )
    }
  }
}

/**
 * Horizontal showcase strip of all unlocked level achievement badges ("ब्याज") near the top of ProfileScreen.
 */
@Composable
fun UnlockedProfileBadgesLine(
  userLevel: Int,
  levelConfigs: Map<Int, LevelTierConfig>,
  onOpenLevelCards: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val unlockedTiers = remember(userLevel) { SwarUserIdentity.unlockedTiersForLevel(userLevel) }
  val totalTiers = SwarUserIdentity.levelTiers.size
  val shimmer = rememberInfiniteTransition(label = "profileBadgesLine")
  val sheenX by shimmer.animateFloat(
    initialValue = -0.3f,
    targetValue = 1.3f,
    animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
    label = "sheen",
  )

  Surface(
    onClick = onOpenLevelCards,
    shape = RoundedCornerShape(16.dp),
    color = Color.White,
    shadowElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, Color(0xFFE0E8F5), RoundedCornerShape(16.dp))
      .testTag("profile_unlocked_badges_line"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = Color(0xFFE69500),
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "अर्जित प्रोफ़ाइल बैज (ब्याज)",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF1A2033),
            fontWeight = FontWeight.ExtraBold,
          )
        }
        Surface(
          color = Color(0xFFEEF5FF),
          shape = RoundedCornerShape(10.dp),
        ) {
          Text(
            text = "${unlockedTiers.size}/$totalTiers अनलॉक • देखें ›",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF0077CC),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        unlockedTiers.forEach { tier ->
          val badgeUrl = levelConfigs[tier.badgeLevel]?.profileBadgeUrl.orEmpty()
          UnlockedTierBadgeChip(
            tier = tier,
            badgeUrl = badgeUrl,
            sheenFraction = sheenX,
            isDarkSurface = false,
          )
        }
      }
    }
  }
}

/**
 * Stylish dark-luxury side showcase panel inside VoiceRoom Profile Card Sheet displaying all
 * unlocked achievement badges ("ब्याज") with live animated artwork.
 */
@Composable
fun ProfileSideBadgesShowcaseCard(
  userLevel: Int,
  levelConfigs: Map<Int, LevelTierConfig>,
  onOpenAllCards: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val unlockedTiers = remember(userLevel) { SwarUserIdentity.unlockedTiersForLevel(userLevel) }
  val shimmer = rememberInfiniteTransition(label = "sideBadgeShowcase")
  val sheenX by shimmer.animateFloat(
    initialValue = -0.3f,
    targetValue = 1.3f,
    animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
    label = "sheen",
  )

  Surface(
    onClick = onOpenAllCards,
    color = Color(0xFF211644),
    shape = RoundedCornerShape(14.dp),
    modifier = modifier
      .border(
        width = 1.dp,
        brush = Brush.horizontalGradient(listOf(SwarGold.copy(alpha = 0.65f), SwarCyan.copy(alpha = 0.55f))),
        shape = RoundedCornerShape(14.dp),
      )
      .testTag("sheet_unlocked_badges_panel"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = SwarGold,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = "बैज / ब्याज",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
          )
        }
        Text(
          text = "${unlockedTiers.size}/13",
          style = MaterialTheme.typography.labelSmall,
          color = SwarCyan,
          fontWeight = FontWeight.Bold,
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        unlockedTiers.asReversed().forEach { tier ->
          UnlockedTierBadgeChip(
            tier = tier,
            badgeUrl = levelConfigs[tier.badgeLevel]?.profileBadgeUrl.orEmpty(),
            sheenFraction = sheenX,
            isDarkSurface = true,
          )
        }
      }
    }
  }
}

@Composable
private fun UnlockedTierBadgeChip(
  tier: LevelTierSpec,
  badgeUrl: String,
  sheenFraction: Float,
  isDarkSurface: Boolean,
) {
  val bgBrush = if (isDarkSurface) {
    Brush.verticalGradient(listOf(tier.capsuleTopColor.copy(alpha = 0.38f), Color(0xFF140D2C)))
  } else {
    Brush.verticalGradient(listOf(tier.capsuleTopColor.copy(alpha = 0.16f), Color(0xFFF6F8FC)))
  }
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(bgBrush)
      .border(1.dp, tier.rimColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
      .padding(horizontal = 8.dp, vertical = 5.dp),
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val bandW = size.width * 0.35f
      val sx = (size.width + bandW) * sheenFraction - bandW
      drawRect(
        brush = Brush.linearGradient(
          colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
          start = Offset(sx, 0f),
          end = Offset(sx + bandW, size.height),
        )
      )
    }
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      if (badgeUrl.isNotBlank()) {
        AsyncImage(
          model = badgeUrl,
          contentDescription = "LV.${tier.badgeLevel} बैज",
          modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(1.dp, tier.rimColor, CircleShape),
          contentScale = ContentScale.Crop,
        )
      } else {
        Box(
          modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(listOf(tier.capsuleTopColor, tier.capsuleBottomColor))
            )
            .border(1.dp, tier.rimColor, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = tier.textColor,
            modifier = Modifier.size(12.dp),
          )
        }
      }
      Text(
        text = tier.badgeLabel,
        style = MaterialTheme.typography.labelSmall,
        color = if (isDarkSurface) Color.White else Color(0xFF1D2336),
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun TypingBannerChatBubble(
  senderName: String,
  messageText: String,
  badgeTag: String = "",
  bannerUrl: String = "",
  messageType: String = "text",
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  val hasBanner = bannerUrl.isNotBlank()
  val shape = RoundedCornerShape(if (hasBanner) 16.dp else 14.dp)
  val baseModifier = modifier
    .widthIn(min = if (hasBanner) 170.dp else 0.dp)
    .height(IntrinsicSize.Min)
    .clip(shape)
    .then(
      if (hasBanner) {
        Modifier.border(
          width = 1.2.dp,
          brush = Brush.horizontalGradient(listOf(SwarGold, SwarCyan)),
          shape = shape,
        )
      } else {
        Modifier.background(Color(0x66000000))
      }
    )
    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

  Box(modifier = baseModifier) {
    if (hasBanner) {
      AsyncImage(
        model = bannerUrl,
        contentDescription = "टाइपिंग पट्टी बैकग्राउंड",
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
      )
      Box(
        modifier = Modifier
          .matchParentSize()
          .background(
            Brush.horizontalGradient(
              colors = listOf(
                Color(0x9E0E0820),
                Color(0x72120B26),
                Color(0x880E0820),
              )
            )
          ),
      )
    }

    Row(
      modifier = Modifier.padding(
        horizontal = if (hasBanner) 13.dp else 10.dp,
        vertical = if (hasBanner) 8.dp else 6.dp,
      ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (badgeTag.isNotBlank()) {
        Surface(
          color = when (messageType) {
            "gift" -> SwarCoral
            "join" -> SwarViolet
            else -> if (hasBanner) Color(0xCC241547) else SwarCyan.copy(alpha = 0.25f)
          },
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = badgeTag,
            style = MaterialTheme.typography.labelSmall,
            color = if (hasBanner) SwarGold else Color.White,
            fontWeight = if (hasBanner) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
      }

      val textShadow = if (hasBanner) {
        Shadow(color = Color(0xCC000000), offset = Offset(1f, 1.5f), blurRadius = 4f)
      } else {
        null
      }
      Text(
        text = buildAnnotatedString {
          withStyle(
            SpanStyle(
              color = if (hasBanner) SwarGold else SwarCyan,
              fontWeight = FontWeight.Bold,
              shadow = textShadow,
            )
          ) {
            append("$senderName: ")
          }
          withStyle(
            SpanStyle(
              color = Color.White,
              fontWeight = if (hasBanner) FontWeight.SemiBold else FontWeight.Normal,
              fontSize = if (messageType == "emoji") 15.sp else MaterialTheme.typography.bodySmall.fontSize,
              shadow = textShadow,
            )
          ) {
            append(messageText)
          }
        },
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

/**
 * Interactive Live Preview & Admin 5-Reward Configurator Dialog for any selected Level Card.
 */
@Composable
fun LevelTierDetailConfiguratorDialog(
  tier: LevelTierSpec,
  config: LevelTierConfig,
  userLevel: Int,
  isClaimed: Boolean,
  isAdminUser: Boolean,
  isUploadingMedia: Boolean,
  sampleAvatarUrl: String,
  sampleDisplayName: String,
  onDismiss: () -> Unit,
  onClaimRewardCoins: (Int) -> Unit,
  onSaveTierConfig: (
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
  ) -> Unit,
  equippedFrameLevel: Long = -1L,
  equippedTypingLevel: Long = -1L,
  equippedEntryVideoLevel: Long = -1L,
  onEquipCosmetic: (frameLevel: Long?, typingLevel: Long?, entryVideoLevel: Long?) -> Unit = { _, _, _ -> },
  isEntryVideoMuted: Boolean = false,
  onToggleEntryVideoMuted: () -> Unit = {},
) {
  val isUnlocked = userLevel >= tier.minLevel
  var entryVideoLink by remember(tier.badgeLevel, config.entryVideoUrl) { mutableStateOf(config.entryVideoUrl) }
  var entryVideoUri by remember(tier.badgeLevel) { mutableStateOf<Uri?>(null) }

  var frameLink by remember(tier.badgeLevel, config.avatarFrameUrl) { mutableStateOf(config.avatarFrameUrl) }
  var frameUri by remember(tier.badgeLevel) { mutableStateOf<Uri?>(null) }

  var coinsInput by remember(tier.badgeLevel, config.rewardCoins) {
    mutableStateOf(if (config.rewardCoins > 0L) config.rewardCoins.toString() else "")
  }

  var badgeLink by remember(tier.badgeLevel, config.profileBadgeUrl) { mutableStateOf(config.profileBadgeUrl) }
  var badgeUri by remember(tier.badgeLevel) { mutableStateOf<Uri?>(null) }

  var typingBannerLink by remember(tier.badgeLevel, config.typingBannerUrl) {
    mutableStateOf(config.typingBannerUrl)
  }
  var typingBannerUri by remember(tier.badgeLevel) { mutableStateOf<Uri?>(null) }

  val entryVideoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        entryVideoUri = uri
      }
    },
  )
  val framePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        frameUri = uri
      }
    },
  )
  val badgePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        badgeUri = uri
      }
    },
  )
  val typingBannerPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        typingBannerUri = uri
      }
    },
  )

  val previewFrameModel = frameUri?.toString() ?: frameLink.trim()
  val previewBadgeModel = badgeUri?.toString() ?: badgeLink.trim()
  val previewTypingBannerModel = typingBannerUri?.toString() ?: typingBannerLink.trim()
  val previewVideoSource = entryVideoUri?.toString() ?: entryVideoLink.trim()
  val parsedCoins = coinsInput.trim().toLongOrNull()?.coerceIn(0L, 999999L) ?: 0L

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color(0xFF14102A),
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .widthIn(max = 680.dp)
        .border(1.5.dp, tier.rimColor.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
        .testTag("level_tier_detail_sheet"),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
          ) {
            OrnateWingedLevelCrest(
              tier = tier,
              displayText = tier.badgeLabel,
              badgeWidth = 104.dp,
              badgeHeight = 38.dp,
              isCompact = true,
            )
            Column {
              Text(
                text = "${tier.rangeLabel} • ${tier.titleHindi}",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
              )
              val targetHp = SwarUserIdentity.requiredHpForLevel(tier.minLevel)
              Text(
                text = if (isUnlocked) "अनलॉक ✓ (लाइव एनीमेशन सक्रिय)"
                else "लॉक्ड • LV.${tier.minLevel} पर खुलेगा (${SwarUserIdentity.formatIndianNumber(targetHp)} HP)",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUnlocked) SwarMint else SwarGold,
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(CircleShape)
              .background(Color(0xFF261F4B))
              .testTag("close_tier_detail_button"),
          ) {
            Icon(Icons.Filled.Close, contentDescription = "बंद करें", tint = Color.White)
          }
        }

        // Live Real-Time Animated Studio Preview
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = Color(0xFF1D173D),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, tier.glowColor.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "रियल-टाइम लाइव प्रीव्यू",
              style = MaterialTheme.typography.labelMedium,
              color = SwarCyan,
              fontWeight = FontWeight.Bold,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              // Live Frame Preview on User DP
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
              ) {
                FramedUserAvatar(
                  url = sampleAvatarUrl,
                  name = sampleDisplayName,
                  avatarSize = 64.dp,
                  frameUrl = previewFrameModel,
                  borderColor = tier.rimColor,
                  borderWidth = 2.dp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = if (previewFrameModel.isNotBlank()) "DP फ्रेम लाइव ✓" else "फ्रेम खाली",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFD5D0EE),
                )
              }

              // Live Badge Preview
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
              ) {
                if (previewBadgeModel.isNotBlank()) {
                  AsyncImage(
                    model = previewBadgeModel,
                    contentDescription = "प्रोफ़ाइल बैज",
                    modifier = Modifier
                      .size(54.dp)
                      .clip(RoundedCornerShape(14.dp))
                      .border(1.5.dp, SwarGold, RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Fit,
                  )
                } else {
                  Box(
                    modifier = Modifier
                      .size(54.dp)
                      .clip(RoundedCornerShape(14.dp))
                      .background(Color(0xFF2B2255))
                      .border(1.dp, Color(0xFF493C80), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Filled.WorkspacePremium,
                      contentDescription = null,
                      tint = tier.rimColor,
                      modifier = Modifier.size(28.dp),
                    )
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = if (previewBadgeModel.isNotBlank()) "बैज (ब्याज) लाइव ✓" else "डिफ़ॉल्ट बैज",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFD5D0EE),
                )
              }

              // Reward Coins Status & Claim Action
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
              ) {
                Box(
                  modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(SwarGold.copy(alpha = 0.16f))
                    .border(1.5.dp, SwarGold, CircleShape),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Filled.MonetizationOn,
                    contentDescription = null,
                    tint = SwarGold,
                    modifier = Modifier.size(28.dp),
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = if (parsedCoins > 0L) "+$parsedCoins कॉइन" else "कॉइन 0",
                  style = MaterialTheme.typography.labelSmall,
                  color = SwarGold,
                  fontWeight = FontWeight.ExtraBold,
                )
              }
            }

            if (isUnlocked && parsedCoins > 0L) {
              Button(
                onClick = { onClaimRewardCoins(tier.badgeLevel) },
                enabled = !isClaimed,
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isClaimed) Color(0xFF284736) else SwarGold,
                  contentColor = if (isClaimed) Color(0xFFB5E8C8) else Color(0xFF3A1E00),
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("claim_tier_coins_button"),
              ) {
                Text(
                  text = if (isClaimed) "🎉 +$parsedCoins कॉइंस प्राप्त हो चुके हैं ✓" else "🪙 +$parsedCoins लेवल कॉइंस क्लेम करें",
                  fontWeight = FontWeight.ExtraBold,
                )
              }
            }

            // Live Entry Video Auto-Playing Preview Box
            if (previewVideoSource.isNotBlank()) {
              LevelEntryVideoPreviewBox(
                videoSource = previewVideoSource,
                isMuted = isEntryVideoMuted,
                onToggleMute = onToggleEntryVideoMuted,
              )
            }

            // Live Typing Patti ("टाइपिंग पट्टी") Chat Bubble Preview
            if (previewTypingBannerModel.isNotBlank()) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "टाइपिंग पट्टी लाइव प्रीव्यू (चैट करते समय ऐसा दिखेगा):",
                  style = MaterialTheme.typography.labelSmall,
                  color = SwarMint,
                  fontWeight = FontWeight.Bold,
                )
                TypingBannerChatBubble(
                  senderName = sampleDisplayName.ifBlank { "Swar" },
                  messageText = "नमस्ते दोस्तों! गाना शुरू करें ✨🎤",
                  badgeTag = tier.badgeLabel,
                  bannerUrl = previewTypingBannerModel,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("typing_banner_live_preview"),
                )
              }
            }

            // Equip / Unequip / Plain Controls for unlocked tiers
            if (isUnlocked && (previewFrameModel.isNotBlank() || previewTypingBannerModel.isNotBlank() || previewVideoSource.isNotBlank())) {
              val tierBadgeLong = tier.badgeLevel.toLong()
              val isThisFrameEquipped = equippedFrameLevel == tierBadgeLong
              val isThisTypingEquipped = equippedTypingLevel == tierBadgeLong
              val isThisEntryEquipped = equippedEntryVideoLevel == tierBadgeLong

              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color(0xFF140F2D))
                  .border(1.dp, SwarCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                  .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "✨ इस टियर का स्टाइल पहनें / उतारें",
                    style = MaterialTheme.typography.labelMedium,
                    color = SwarGold,
                    fontWeight = FontWeight.ExtraBold,
                  )
                  Surface(
                    onClick = { onEquipCosmetic(0L, 0L, 0L) },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2B2150),
                    modifier = Modifier.testTag("clear_all_cosmetics_from_detail"),
                  ) {
                    Text(
                      text = "सब सादा करें",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color(0xFFDAD4F6),
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }

                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  if (previewFrameModel.isNotBlank()) {
                    OutlinedButton(
                      onClick = {
                        onEquipCosmetic(if (isThisFrameEquipped) 0L else tierBadgeLong, null, null)
                      },
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isThisFrameEquipped) SwarMint.copy(alpha = 0.18f) else Color.Transparent,
                      ),
                      modifier = Modifier
                        .height(42.dp)
                        .testTag("equip_frame_tier_${tier.badgeLevel}"),
                    ) {
                      if (isThisFrameEquipped) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SwarMint, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                      }
                      Text(
                        text = if (isThisFrameEquipped) "फ्रेम पहना ✓ (उतारें)" else "फ्रेम पहनें",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isThisFrameEquipped) SwarMint else Color.White,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }

                  if (previewTypingBannerModel.isNotBlank()) {
                    OutlinedButton(
                      onClick = {
                        onEquipCosmetic(null, if (isThisTypingEquipped) 0L else tierBadgeLong, null)
                      },
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isThisTypingEquipped) SwarCyan.copy(alpha = 0.18f) else Color.Transparent,
                      ),
                      modifier = Modifier
                        .height(42.dp)
                        .testTag("equip_typing_tier_${tier.badgeLevel}"),
                    ) {
                      if (isThisTypingEquipped) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SwarCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                      }
                      Text(
                        text = if (isThisTypingEquipped) "पट्टी पहनी ✓ (उतारें)" else "टाइपिंग पट्टी पहनें",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isThisTypingEquipped) SwarCyan else Color.White,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }

                  if (previewVideoSource.isNotBlank()) {
                    OutlinedButton(
                      onClick = {
                        onEquipCosmetic(null, null, if (isThisEntryEquipped) 0L else tierBadgeLong)
                      },
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isThisEntryEquipped) SwarGold.copy(alpha = 0.18f) else Color.Transparent,
                      ),
                      modifier = Modifier
                        .height(42.dp)
                        .testTag("equip_entry_tier_${tier.badgeLevel}"),
                    ) {
                      if (isThisEntryEquipped) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SwarGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                      }
                      Text(
                        text = if (isThisEntryEquipped) "एंट्री सक्रिय ✓ (बंद करें)" else "एंट्री वीडियो चुनें",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isThisEntryEquipped) SwarGold else Color.White,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Admin Configurator: Entry Video + Avatar Frame + Coins + Profile Badge + Typing Patti
        if (isAdminUser) {
          Text(
            text = "लेवल कार्ड सेटअप (सभी 5 विकल्प)",
            style = MaterialTheme.typography.labelLarge,
            color = SwarGold,
            fontWeight = FontWeight.ExtraBold,
          )

          // Row 1: Entry Video (Left) & Avatar Frame (Right — side by side!)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              OutlinedTextField(
                value = entryVideoLink,
                onValueChange = { entryVideoLink = it },
                label = { Text("एंट्री वीडियो लिंक", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("MP4 / Instagram / Pin") },
                leadingIcon = { Icon(Icons.Filled.Movie, contentDescription = null, tint = SwarCyan) },
                singleLine = true,
                colors = adminFieldColors(),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("tier_entry_video_input"),
              )
              OutlinedButton(
                onClick = {
                  entryVideoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                  )
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("pick_tier_entry_video_button"),
              ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = SwarCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (entryVideoUri != null) "वीडियो चुना ✓" else "वीडियो अपलोड",
                  color = Color.White,
                  fontSize = 12.sp,
                  maxLines = 1,
                )
              }
            }

            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              OutlinedTextField(
                value = frameLink,
                onValueChange = { frameLink = it },
                label = { Text("DP फ्रेम लिंक", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("GIF / PNG / WebP") },
                leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = SwarGold) },
                singleLine = true,
                colors = adminFieldColors(),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("tier_frame_input"),
              )
              OutlinedButton(
                onClick = {
                  framePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("pick_tier_frame_button"),
              ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = SwarGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (frameUri != null) "फ्रेम चुना ✓" else "फ्रेम अपलोड",
                  color = Color.White,
                  fontSize = 12.sp,
                  maxLines = 1,
                )
              }
            }
          }

          // Row 2: Reward Coins + Profile Badge ("ब्याज") URL & Upload
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              OutlinedTextField(
                value = coinsInput,
                onValueChange = { raw -> coinsInput = raw.filter { it.isDigit() }.take(6) },
                label = { Text("कॉइंस इनाम", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("जैसे 5000") },
                leadingIcon = { Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = SwarGold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = adminFieldColors(),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("tier_coins_input"),
              )
              Text(
                text = "लेवल खुलने पर स्वतः जुड़ेगा",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB3ACDC),
              )
            }

            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              OutlinedTextField(
                value = badgeLink,
                onValueChange = { badgeLink = it },
                label = { Text("बैज (ब्याज) लिंक", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("GIF / PNG लिंक") },
                leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = SwarMint) },
                singleLine = true,
                colors = adminFieldColors(),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("tier_badge_input"),
              )
              OutlinedButton(
                onClick = {
                  badgePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("pick_tier_badge_button"),
              ) {
                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = SwarMint, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (badgeUri != null) "बैज चुना ✓" else "बैज अपलोड",
                  color = Color.White,
                  fontSize = 12.sp,
                  maxLines = 1,
                )
              }
            }
          }

          // Row 3: Typing Patti ("टाइपिंग पट्टी") Link + Upload
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = typingBannerLink,
                onValueChange = { typingBannerLink = it },
                label = { Text("टाइपिंग पट्टी लिंक (Chat Bubble)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("GIF / PNG / WebP / Pin लिंक पेस्ट करें") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = SwarCyan) },
                singleLine = true,
                colors = adminFieldColors(),
                modifier = Modifier
                  .weight(1f)
                  .testTag("tier_typing_banner_input"),
              )
              OutlinedButton(
                onClick = {
                  typingBannerPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                  .height(52.dp)
                  .testTag("pick_tier_typing_banner_button"),
              ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = SwarCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (typingBannerUri != null) "चुनी ✓" else "अपलोड",
                  color = Color.White,
                  fontSize = 12.sp,
                  maxLines = 1,
                )
              }
            }
            Text(
              text = "रूम में टाइप करने पर आपका टेक्स्ट इसी सुंदर पट्टी के ऊपर लिखकर आएगा",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFB3ACDC),
            )
          }

          Button(
            onClick = {
              onSaveTierConfig(
                tier.badgeLevel,
                entryVideoLink,
                entryVideoUri,
                frameLink,
                frameUri,
                parsedCoins,
                badgeLink,
                badgeUri,
                typingBannerLink,
                typingBannerUri,
                onDismiss,
              )
            },
            enabled = !isUploadingMedia,
            colors = ButtonDefaults.buttonColors(containerColor = SwarCoral, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("save_level_config_button"),
          ) {
            if (isUploadingMedia) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
              Spacer(modifier = Modifier.width(8.dp))
              Text("सेव हो रहा है...", fontWeight = FontWeight.Bold)
            } else {
              Text("सेव व लाइव करें ✓", fontWeight = FontWeight.ExtraBold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun adminFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedTextColor = Color.White,
  unfocusedTextColor = Color.White,
  focusedBorderColor = SwarCyan,
  unfocusedBorderColor = Color(0xFF473E78),
  focusedLabelColor = SwarCyan,
  unfocusedLabelColor = Color(0xFFB5ACDE),
)

@SuppressLint("UseKtx")
@Composable
private fun LevelEntryVideoPreviewBox(
  videoSource: String,
  isMuted: Boolean = true,
  onToggleMute: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  var playbackUri by remember(videoSource) {
    mutableStateOf<Uri?>(if (videoSource.startsWith("http", ignoreCase = true)) null else Uri.parse(videoSource))
  }
  var fallbackThumb by remember(videoSource) { mutableStateOf("") }
  var localMuted by remember(videoSource, isMuted) { mutableStateOf(isMuted) }
  val effectiveMuted = if (onToggleMute != null) isMuted else localMuted
  var mediaPlayerRef by remember(videoSource) { mutableStateOf<MediaPlayer?>(null) }
  var surfaceRef by remember(videoSource) { mutableStateOf<Surface?>(null) }
  var isPrepared by remember(videoSource) { mutableStateOf(false) }

  LaunchedEffect(videoSource) {
    isPrepared = false
    if (videoSource.startsWith("http", ignoreCase = true)) {
      val res = withTimeoutOrNull(10_000L) {
        withContext(Dispatchers.IO) { VideoLinkResolver.resolvePlayableVideo(videoSource) }
      }
      when (res) {
        is VideoLinkResolution.Success -> {
          fallbackThumb = res.thumbnailUrl
          val cached = withTimeoutOrNull(6_000L) {
            withContext(Dispatchers.IO) {
              val mediaDir = LocalMediaCacheManager.preferredMediaBaseDir(context)
              SupabaseNetworkClient.cacheRemoteMediaFile(mediaDir, res.playableUrl)
            }
          }
          playbackUri = cached?.let { Uri.fromFile(it) } ?: Uri.parse(res.playableUrl)
        }
        is VideoLinkResolution.Failure -> {
          fallbackThumb = res.fallbackImageUrl
        }
        null -> Unit
      }
    }
  }

  LaunchedEffect(effectiveMuted, mediaPlayerRef) {
    val vol = if (effectiveMuted) 0f else 1f
    runCatching { mediaPlayerRef?.setVolume(vol, vol) }
  }

  DisposableEffect(videoSource) {
    onDispose {
      runCatching { mediaPlayerRef?.stop() }
      runCatching { mediaPlayerRef?.release() }
      mediaPlayerRef = null
      runCatching { surfaceRef?.release() }
      surfaceRef = null
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(152.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0xFF0C091A))
      .border(1.dp, SwarViolet.copy(alpha = 0.65f), RoundedCornerShape(14.dp)),
    contentAlignment = Alignment.Center,
  ) {
    val activeUri = playbackUri
    if (activeUri != null) {
      AndroidView(
        factory = { ctx ->
          TextureView(ctx).apply {
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
              override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                val surf = Surface(st)
                surfaceRef = surf
                val player = MediaPlayer().apply {
                  setSurface(surf)
                  setAudioAttributes(
                    AudioAttributes.Builder()
                      .setUsage(AudioAttributes.USAGE_MEDIA)
                      .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                      .build()
                  )
                  isLooping = true
                  setOnPreparedListener { mp ->
                    isPrepared = true
                    val v = if (effectiveMuted) 0f else 1f
                    mp.setVolume(v, v)
                    mp.start()
                  }
                  setOnErrorListener { _, _, _ -> true }
                }
                mediaPlayerRef = player
                runCatching {
                  player.setDataSource(ctx, activeUri)
                  player.prepareAsync()
                }
              }

              override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = Unit
              override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                runCatching { mediaPlayerRef?.release() }
                mediaPlayerRef = null
                runCatching { surfaceRef?.release() }
                surfaceRef = null
                return true
              }

              override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
            }
          }
        },
        modifier = Modifier.fillMaxSize(),
      )
      if (!isPrepared) {
        CircularProgressIndicator(color = SwarCyan, modifier = Modifier.size(26.dp))
      }
      Surface(
        onClick = {
          if (onToggleMute != null) {
            onToggleMute()
          } else {
            localMuted = !localMuted
          }
        },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xAA000000),
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(8.dp)
          .testTag("tier_preview_entry_mute_toggle"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = if (effectiveMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (effectiveMuted) "आवाज़ म्यूट है, चालू करें" else "आवाज़ चालू है, म्यूट करें",
            tint = if (effectiveMuted) SwarCoral else SwarCyan,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = if (effectiveMuted) "म्यूट" else "आवाज़ चालू",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    } else if (fallbackThumb.isNotBlank()) {
      AsyncImage(
        model = fallbackThumb,
        contentDescription = "एंट्री वीडियो प्रीव्यू",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
      Surface(
        color = Color(0xAA0D0920),
        shape = RoundedCornerShape(10.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(Icons.Filled.PlayCircleFilled, contentDescription = null, tint = SwarCyan, modifier = Modifier.size(18.dp))
          Text("एंट्री वीडियो रूम प्रवेश पर ऑटो-प्ले होगा", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
      }
    } else {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.Filled.PlayCircleFilled, contentDescription = null, tint = SwarGold, modifier = Modifier.size(22.dp))
        Text("एंट्री वीडियो लिंक सेट ✓ (रूम में प्रवेश पर प्ले होगा)", color = Color.White, style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}
