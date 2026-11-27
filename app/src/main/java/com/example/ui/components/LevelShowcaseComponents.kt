package com.example.ui.components

import android.net.Uri
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.LevelRoadmapMilestone
import com.example.data.model.LevelTierConfig
import com.example.data.model.LevelTierSpec
import com.example.data.model.SwarUserIdentity
import com.example.data.model.UserLevelInfo
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarMint
import com.example.ui.theme.SwarViolet

@Composable
fun OrnateWingedLevelCrest(
  tier: LevelTierSpec,
  displayText: String = tier.badgeLabel,
  modifier: Modifier = Modifier,
  badgeWidth: Dp = 152.dp,
  badgeHeight: Dp = 56.dp,
  isCompact: Boolean = false,
) {
  Box(
    modifier = modifier.size(width = badgeWidth, height = badgeHeight),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val cy = h / 2f

      // Subtle glowing aura behind high-tier crests
      drawOval(
        brush = Brush.radialGradient(
          colors = listOf(tier.glowColor.copy(alpha = 0.36f), Color.Transparent),
          center = Offset(w / 2f, cy),
          radius = w * 0.45f,
        ),
        topLeft = Offset(w * 0.08f, h * 0.06f),
        size = Size(w * 0.84f, h * 0.88f),
      )

      val wingBrush = Brush.linearGradient(
        colors = listOf(tier.wingHighlightColor, tier.rimColor, tier.wingShadowColor),
        start = Offset(0f, 0f),
        end = Offset(w, h),
      )

      // Left multi-feather ornate wing
      val leftWingOuter = Path().apply {
        moveTo(w * 0.24f, cy - h * 0.12f)
        lineTo(w * 0.02f, h * 0.08f)
        lineTo(w * 0.11f, h * 0.34f)
        lineTo(w * 0.04f, h * 0.44f)
        lineTo(w * 0.13f, h * 0.58f)
        lineTo(w * 0.07f, h * 0.72f)
        lineTo(w * 0.25f, cy + h * 0.26f)
        close()
      }
      drawPath(leftWingOuter, wingBrush)
      drawPath(
        path = leftWingOuter,
        color = tier.rimColor.copy(alpha = 0.9f),
        style = Stroke(width = if (isCompact) 1.2f else 1.8f, join = StrokeJoin.Round),
      )

      // Right multi-feather ornate wing
      val rightWingOuter = Path().apply {
        moveTo(w * 0.76f, cy - h * 0.12f)
        lineTo(w * 0.98f, h * 0.08f)
        lineTo(w * 0.89f, h * 0.34f)
        lineTo(w * 0.96f, h * 0.44f)
        lineTo(w * 0.87f, h * 0.58f)
        lineTo(w * 0.93f, h * 0.72f)
        lineTo(w * 0.75f, cy + h * 0.26f)
        close()
      }
      drawPath(rightWingOuter, wingBrush)
      drawPath(
        path = rightWingOuter,
        color = tier.rimColor.copy(alpha = 0.9f),
        style = Stroke(width = if (isCompact) 1.2f else 1.8f, join = StrokeJoin.Round),
      )

      // Central metallic capsule frame
      val capLeft = w * 0.16f
      val capRight = w * 0.84f
      val capTop = h * 0.20f
      val capBottom = h * 0.80f
      val capW = capRight - capLeft
      val capH = capBottom - capTop
      val corner = CornerRadius(capH / 2f, capH / 2f)

      // Outer dark rim shadow
      drawRoundRect(
        color = Color(0x88000000),
        topLeft = Offset(capLeft - 2f, capTop + 2f),
        size = Size(capW + 4f, capH + 2f),
        cornerRadius = corner,
      )

      // Main capsule gradient body
      drawRoundRect(
        brush = Brush.verticalGradient(
          colors = listOf(tier.capsuleTopColor, tier.capsuleBottomColor),
          startY = capTop,
          endY = capBottom,
        ),
        topLeft = Offset(capLeft, capTop),
        size = Size(capW, capH),
        cornerRadius = corner,
      )

      // Top satin gloss highlight inside capsule
      drawRoundRect(
        brush = Brush.verticalGradient(
          colors = listOf(Color.White.copy(alpha = 0.42f), Color.Transparent),
          startY = capTop + 2f,
          endY = cy,
        ),
        topLeft = Offset(capLeft + 4f, capTop + 2f),
        size = Size(capW - 8f, capH * 0.44f),
        cornerRadius = CornerRadius(capH * 0.4f, capH * 0.4f),
      )

      // Metallic rim border around capsule
      drawRoundRect(
        brush = Brush.horizontalGradient(
          colors = listOf(tier.rimColor, Color.White.copy(alpha = 0.92f), tier.rimColor),
        ),
        topLeft = Offset(capLeft, capTop),
        size = Size(capW, capH),
        cornerRadius = corner,
        style = Stroke(width = if (isCompact) 2.2f else 3.4f),
      )

      // Left & right 4-pointed diamond gems
      val gemR = capH * 0.22f
      val leftGemX = capLeft + capH * 0.36f
      val rightGemX = capRight - capH * 0.36f
      listOf(leftGemX, rightGemX).forEach { gx ->
        val gemPath = Path().apply {
          moveTo(gx, cy - gemR)
          lineTo(gx + gemR * 0.78f, cy)
          lineTo(gx, cy + gemR)
          lineTo(gx - gemR * 0.78f, cy)
          close()
        }
        drawPath(gemPath, color = tier.gemColor)
        drawCircle(
          color = Color.White.copy(alpha = 0.85f),
          radius = gemR * 0.24f,
          center = Offset(gx - gemR * 0.14f, cy - gemR * 0.16f),
        )
      }
    }

    Text(
      text = displayText,
      style = TextStyle(
        color = tier.textColor,
        fontSize = when {
          isCompact -> 10.sp
          displayText.length >= 6 -> 16.sp
          else -> 18.sp
        },
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Serif,
        letterSpacing = 0.4.sp,
        shadow = Shadow(
          color = Color(0xCC000000),
          offset = Offset(0f, 2f),
          blurRadius = 4f,
        ),
      ),
      maxLines = 1,
    )
  }
}

@Composable
fun LevelTierLargeCard(
  tier: LevelTierSpec,
  config: LevelTierConfig = LevelTierConfig(badgeLevel = tier.badgeLevel.toLong()),
  isCurrentTier: Boolean,
  isUnlocked: Boolean,
  isSelected: Boolean,
  isAdminUser: Boolean = false,
  sampleAvatarUrl: String = "",
  sampleDisplayName: String = "Swar",
  shimmerFraction: Float = 0f,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor = when {
    isCurrentTier -> SwarGold
    isSelected -> tier.glowColor
    isUnlocked -> tier.rimColor.copy(alpha = 0.45f)
    else -> Color(0xFF332D58)
  }
  val borderWidth = if (isCurrentTier || isSelected) 2.dp else 1.dp

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = Color.Transparent,
    modifier = modifier
      .fillMaxWidth()
      .height(224.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF28214D),
            Color(0xFF1A1534),
            Color(0xFF120E26),
          )
        )
      )
      .border(borderWidth, borderColor, RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .testTag("level_card_${tier.badgeLevel}"),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
      // Live continuous light sheen across every level card
      Canvas(modifier = Modifier.matchParentSize()) {
        val bandW = size.width * 0.35f
        val startX = (size.width + bandW) * shimmerFraction - bandW
        drawRect(
          brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, tier.glowColor.copy(alpha = 0.14f), Color.Transparent),
            start = Offset(startX, 0f),
            end = Offset(startX + bandW, size.height),
          )
        )
      }

      // Top Left: Quick indicator if Entry Video / Frame / Badge live
      Row(
        modifier = Modifier.align(Alignment.TopStart),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (config.entryVideoUrl.isNotBlank()) {
          Surface(
            color = SwarCyan.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.border(0.8.dp, SwarCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Icon(Icons.Filled.Movie, contentDescription = null, tint = SwarCyan, modifier = Modifier.size(10.dp))
              Text("एंट्री", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
          }
        }
        if (SwarUserIdentity.isTypingBoxTier(tier.badgeLevel)) {
          Surface(
            color = SwarGold.copy(alpha = 0.22f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.border(0.8.dp, SwarGold.copy(alpha = 0.72f), RoundedCornerShape(8.dp)),
          ) {
            Text(
              text = "गोल्डन पट्टी",
              fontSize = 8.sp,
              color = SwarGold,
              fontWeight = FontWeight.ExtraBold,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            )
          }
        }
        if (config.rewardCoins > 0L) {
          Surface(
            color = SwarGold.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.border(0.8.dp, SwarGold.copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
          ) {
            Text(
              text = "+${config.rewardCoins}",
              fontSize = 8.sp,
              color = SwarGold,
              fontWeight = FontWeight.ExtraBold,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            )
          }
        }
      }

      // Top status chip: Current / Unlocked / Locked
      Row(
        modifier = Modifier.align(Alignment.TopEnd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (isCurrentTier) {
          Surface(
            color = SwarGold,
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = "आपका स्तर",
              fontSize = 9.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color(0xFF3D2200),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        } else if (isUnlocked) {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "अनलॉक",
            tint = Color(0xFF4CD964),
            modifier = Modifier.size(16.dp),
          )
        } else {
          Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "लॉक्ड",
            tint = Color(0xFF6B658E),
            modifier = Modifier.size(14.dp),
          )
        }
      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        OrnateWingedLevelCrest(
          tier = tier,
          displayText = tier.badgeLabel,
          badgeWidth = 144.dp,
          badgeHeight = 52.dp,
          isCompact = false,
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = tier.rangeLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE5E2F5),
            maxLines = 1,
          )
          val requiredCoins = SwarUserIdentity.requiredHpForLevel(tier.minLevel)
          Text(
            text = if (requiredCoins == 0L) tier.titleHindi else "${tier.titleHindi} • ${SwarUserIdentity.formatIndianNumber(requiredCoins)} HP",
            fontSize = 10.sp,
            color = if (isCurrentTier) SwarGold else Color(0xFF9C96BE),
            fontWeight = if (isCurrentTier) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        // Live Real-Time 4-Reward Strip inside every card (Animated Frame + Badge + Video + Coins)
        Surface(
          color = Color(0x66110C24),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(0.8.dp, tier.rimColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            // 1. Live Animated DP Frame preview
            FramedUserAvatar(
              url = sampleAvatarUrl,
              name = sampleDisplayName,
              avatarSize = 24.dp,
              frameUrl = config.avatarFrameUrl,
              borderColor = tier.rimColor,
              borderWidth = 1.dp,
            )

            // 2. Live Animated Profile Badge ("ब्याज") preview
            if (config.profileBadgeUrl.isNotBlank()) {
              AsyncImage(
                model = config.profileBadgeUrl,
                contentDescription = "बैज",
                modifier = Modifier
                  .size(26.dp)
                  .clip(CircleShape)
                  .border(1.dp, SwarGold, CircleShape),
                contentScale = ContentScale.Crop,
              )
            } else {
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF28204B)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.WorkspacePremium,
                  contentDescription = null,
                  tint = tier.rimColor.copy(alpha = 0.75f),
                  modifier = Modifier.size(14.dp),
                )
              }
            }

            // 3. Video + Coins status or Admin setup cue
            Surface(
              color = if (isAdminUser) Color(0xFF31255C) else tier.glowColor.copy(alpha = 0.22f),
              shape = RoundedCornerShape(8.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Icon(
                  imageVector = if (isAdminUser) Icons.Filled.Settings else Icons.Filled.AutoAwesome,
                  contentDescription = null,
                  tint = if (isAdminUser) SwarCyan else SwarGold,
                  modifier = Modifier.size(12.dp),
                )
                Text(
                  text = if (isAdminUser) "सेटअप" else "लाइव",
                  fontSize = 10.sp,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun LevelCardsShowcaseDialog(
  levelInfo: UserLevelInfo,
  coins: Long,
  onDismiss: () -> Unit,
  levelConfigs: Map<Int, LevelTierConfig> = emptyMap(),
  claimedTiers: Set<Int> = emptySet(),
  isAdminUser: Boolean = false,
  isUploadingMedia: Boolean = false,
  sampleAvatarUrl: String = "",
  sampleDisplayName: String = "Swar",
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
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _, onDone -> onDone() },
  onClaimTierReward: (Int) -> Unit = {},
  initialTab: Int = 0,
  equippedFrameLevel: Long = -1L,
  equippedTypingLevel: Long = -1L,
  equippedEntryVideoLevel: Long = -1L,
  onEquipCosmetic: (frameLevel: Long?, typingLevel: Long?, entryVideoLevel: Long?) -> Unit = { _, _, _ -> },
  isEntryVideoMuted: Boolean = false,
  onToggleEntryVideoMuted: () -> Unit = {},
) {
  val tiers = SwarUserIdentity.levelTiers
  val currentTier = remember(levelInfo.level) { SwarUserIdentity.tierForLevel(levelInfo.level) }
  var selectedTier by remember(levelInfo.level) { mutableStateOf(currentTier) }
  var detailTier by remember { mutableStateOf<LevelTierSpec?>(null) }
  var selectedShowcaseTab by remember(initialTab) { mutableStateOf(initialTab.coerceIn(0, 2)) }

  val cardShimmer = rememberInfiniteTransition(label = "levelGridShimmer")
  val shimmerFraction by cardShimmer.animateFloat(
    initialValue = -0.35f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
    label = "gridSheen",
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color(0xFF130F26),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .widthIn(max = 760.dp)
        .fillMaxHeight(0.88f)
        .border(1.dp, Color(0xFF342C61), RoundedCornerShape(24.dp))
        .testTag("level_cards_showcase_dialog"),
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SwarGold.copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = SwarGold,
                modifier = Modifier.size(22.dp),
              )
            }
            Column {
              Text(
                text = "शाही स्तर कार्ड्स (LV.1 ~ LV.120)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text = if (isAdminUser) "किसी भी कार्ड पर टैप करके एंट्री वीडियो, फ्रेम, कॉइन व बैज जोड़ें" else "कार्ड पर टैप करें और लाइव एनीमेशन व इनाम देखें",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA7A0CE),
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(CircleShape)
              .background(Color(0xFF231D42))
              .testTag("close_level_cards_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "बंद करें",
              tint = Color.White,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active Level Summary Hero Card
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = Color(0xFF211B40),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, currentTier.rimColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              OrnateWingedLevelCrest(
                tier = currentTier,
                displayText = "LV.${levelInfo.level}",
                badgeWidth = 116.dp,
                badgeHeight = 42.dp,
                isCompact = false,
              )
              Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f),
              ) {
                Text(
                  text = levelInfo.title,
                  style = MaterialTheme.typography.titleSmall,
                  color = SwarGold,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "वर्तमान HP: ${SwarUserIdentity.formatIndianNumber(coins)} • आधा: ${SwarUserIdentity.formatHalfHp(coins)}",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color(0xFFD5D0EE),
                )
                if (levelInfo.level < 120) {
                  Text(
                    text = "इस स्तर में: ${SwarUserIdentity.formatIndianNumber(levelInfo.intoLevelHp)} / ${SwarUserIdentity.formatIndianNumber(levelInfo.levelSpanHp)} HP",
                    style = MaterialTheme.typography.labelSmall,
                    color = SwarCyan,
                  )
                }
              }
            }

            LinearProgressIndicator(
              progress = { levelInfo.progress },
              modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = currentTier.glowColor,
              trackColor = Color(0xFF342C5E),
            )

            Text(
              text = if (levelInfo.level >= 120) {
                "बधाई हो! आप सर्वोच्च LV.120 (${SwarUserIdentity.formatIndianNumber(SwarUserIdentity.MAX_LEVEL_120_HP)} HP) स्तर पर हैं।"
              } else {
                "अगले स्तर (LV.${levelInfo.level + 1}) के लिए ${SwarUserIdentity.formatIndianNumber(levelInfo.nextLevelRemainingCoins)} HP और चाहिए (लक्ष्य: ${SwarUserIdentity.formatIndianNumber(levelInfo.nextLevelTargetHp)} HP)"
              },
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFBDB6E2),
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Compact 3-Tab Pill Switcher: Level Cards (13), HP Roadmap (LV.1-120), My Style Customizer
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            onClick = { selectedShowcaseTab = 0 },
            shape = RoundedCornerShape(12.dp),
            color = if (selectedShowcaseTab == 0) SwarGold else Color(0xFF211B40),
            modifier = Modifier
              .weight(1f)
              .border(
                1.dp,
                if (selectedShowcaseTab == 0) SwarGold else Color(0xFF3A3068),
                RoundedCornerShape(12.dp),
              )
              .testTag("level_cards_tab"),
          ) {
            Box(
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "🏅 कार्ड्स (${tiers.size})",
                style = MaterialTheme.typography.labelMedium,
                color = if (selectedShowcaseTab == 0) Color(0xFF2B1800) else Color(0xFFD5D0EE),
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          Surface(
            onClick = { selectedShowcaseTab = 1 },
            shape = RoundedCornerShape(12.dp),
            color = if (selectedShowcaseTab == 1) SwarCyan else Color(0xFF211B40),
            modifier = Modifier
              .weight(1f)
              .border(
                1.dp,
                if (selectedShowcaseTab == 1) SwarCyan else Color(0xFF3A3068),
                RoundedCornerShape(12.dp),
              )
              .testTag("level_roadmap_tab"),
          ) {
            Box(
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "📊 रोडमैप",
                style = MaterialTheme.typography.labelMedium,
                color = if (selectedShowcaseTab == 1) Color(0xFF002030) else Color(0xFFD5D0EE),
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          Surface(
            onClick = { selectedShowcaseTab = 2 },
            shape = RoundedCornerShape(12.dp),
            color = if (selectedShowcaseTab == 2) SwarMint else Color(0xFF211B40),
            modifier = Modifier
              .weight(1f)
              .border(
                1.dp,
                if (selectedShowcaseTab == 2) SwarMint else Color(0xFF3A3068),
                RoundedCornerShape(12.dp),
              )
              .testTag("level_style_customizer_tab"),
          ) {
            Box(
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "✨ मेरा स्टाइल",
                style = MaterialTheme.typography.labelMedium,
                color = if (selectedShowcaseTab == 2) Color(0xFF052414) else Color(0xFFD5D0EE),
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedShowcaseTab) {
          0 -> {
            // Large Adaptive Cards Grid for all 13 Level Tiers with live rewards preview
            LazyVerticalGrid(
              columns = GridCells.Adaptive(minSize = 152.dp),
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("level_cards_grid"),
              contentPadding = PaddingValues(bottom = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(tiers, key = { it.badgeLevel }) { tier ->
                val isCurrent = levelInfo.level in tier.minLevel..tier.maxLevel
                val isUnlocked = levelInfo.level >= tier.minLevel
                val tierConfig = levelConfigs[tier.badgeLevel] ?: LevelTierConfig(badgeLevel = tier.badgeLevel.toLong())
                LevelTierLargeCard(
                  tier = tier,
                  config = tierConfig,
                  isCurrentTier = isCurrent,
                  isUnlocked = isUnlocked,
                  isSelected = selectedTier.badgeLevel == tier.badgeLevel,
                  isAdminUser = isAdminUser,
                  sampleAvatarUrl = sampleAvatarUrl,
                  sampleDisplayName = sampleDisplayName,
                  shimmerFraction = shimmerFraction,
                  onClick = {
                    selectedTier = tier
                    detailTier = tier
                  },
                )
              }
            }
          }
          1 -> {
            LevelHpRoadmapList(
              coins = coins,
              currentLevel = levelInfo.level,
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            )
          }
          else -> {
            MyStyleCosmeticsTab(
              userLevel = levelInfo.level,
              levelConfigs = levelConfigs,
              sampleAvatarUrl = sampleAvatarUrl,
              sampleDisplayName = sampleDisplayName,
              equippedFrameLevel = equippedFrameLevel,
              equippedTypingLevel = equippedTypingLevel,
              equippedEntryVideoLevel = equippedEntryVideoLevel,
              onEquipCosmetic = onEquipCosmetic,
              isEntryVideoMuted = isEntryVideoMuted,
              onToggleEntryVideoMuted = onToggleEntryVideoMuted,
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Tier Quick Open Bar
        val minRequiredCoins = SwarUserIdentity.requiredHpForLevel(selectedTier.minLevel)
        Surface(
          onClick = { detailTier = selectedTier },
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF211B40),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, selectedTier.glowColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .testTag("open_selected_tier_studio_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${selectedTier.rangeLabel} • ${selectedTier.titleHindi}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = if (minRequiredCoins == 0L) {
                  "शुरुआती स्तर — सभी नए सदस्यों के लिए खुला"
                } else {
                  "अनलॉक लक्ष्य: ${SwarUserIdentity.formatIndianNumber(minRequiredCoins)} HP"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFACA5D6),
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              val selReward = levelConfigs[selectedTier.badgeLevel]?.rewardCoins ?: 0L
              if (selReward > 0L) {
                Text(
                  text = "+$selReward 🪙",
                  style = MaterialTheme.typography.labelSmall,
                  color = SwarGold,
                  fontWeight = FontWeight.Bold,
                )
              }
              Text(
                text = if (isAdminUser) "सेटअप ⚙" else if (levelInfo.level >= selectedTier.minLevel) "खोलें ✨" else "देखें",
                style = MaterialTheme.typography.labelMedium,
                color = if (levelInfo.level >= selectedTier.minLevel) SwarMint else SwarGold,
                fontWeight = FontWeight.ExtraBold,
              )
            }
          }
        }
      }
    }
  }

  val activeDetailTier = detailTier
  if (activeDetailTier != null) {
    LevelTierDetailConfiguratorDialog(
      tier = activeDetailTier,
      config = levelConfigs[activeDetailTier.badgeLevel] ?: LevelTierConfig(badgeLevel = activeDetailTier.badgeLevel.toLong()),
      userLevel = levelInfo.level,
      isClaimed = activeDetailTier.badgeLevel in claimedTiers,
      isAdminUser = isAdminUser,
      isUploadingMedia = isUploadingMedia,
      sampleAvatarUrl = sampleAvatarUrl,
      sampleDisplayName = sampleDisplayName,
      onDismiss = { detailTier = null },
      onClaimRewardCoins = onClaimTierReward,
      onSaveTierConfig = onSaveTierConfig,
      equippedFrameLevel = equippedFrameLevel,
      equippedTypingLevel = equippedTypingLevel,
      equippedEntryVideoLevel = equippedEntryVideoLevel,
      onEquipCosmetic = onEquipCosmetic,
      isEntryVideoMuted = isEntryVideoMuted,
      onToggleEntryVideoMuted = onToggleEntryVideoMuted,
    )
  }
}

@Composable
private fun MyStyleCosmeticsTab(
  userLevel: Int,
  levelConfigs: Map<Int, LevelTierConfig>,
  sampleAvatarUrl: String,
  sampleDisplayName: String,
  equippedFrameLevel: Long,
  equippedTypingLevel: Long,
  equippedEntryVideoLevel: Long,
  onEquipCosmetic: (frameLevel: Long?, typingLevel: Long?, entryVideoLevel: Long?) -> Unit,
  isEntryVideoMuted: Boolean,
  onToggleEntryVideoMuted: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val unlockedTiers = remember(userLevel) { SwarUserIdentity.unlockedTiersForLevel(userLevel) }
  val activeFrameUrl = remember(userLevel, levelConfigs, equippedFrameLevel) {
    SwarUserIdentity.resolvedFrameUrl(userLevel, levelConfigs, equippedFrameLevel)
  }
  val activeTypingUrl = remember(userLevel, levelConfigs, equippedTypingLevel) {
    SwarUserIdentity.resolvedTypingBannerUrl(userLevel, levelConfigs, equippedTypingLevel)
  }
  val activeEntryVideo = remember(userLevel, levelConfigs, equippedEntryVideoLevel) {
    SwarUserIdentity.resolvedEntryVideo(userLevel, levelConfigs, equippedEntryVideoLevel)
  }

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(bottom = 10.dp)
      .testTag("style_customizer_panel"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // 1. Live Style Preview Card
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF1F183D),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, SwarGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "सक्रिय स्टाइल प्रीव्यू (LV.$userLevel)",
            style = MaterialTheme.typography.labelLarge,
            color = SwarGold,
            fontWeight = FontWeight.ExtraBold,
          )
          OutlinedButton(
            onClick = { onEquipCosmetic(0L, 0L, 0L) },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier
              .height(36.dp)
              .testTag("style_clear_all_button"),
          ) {
            Text(
              text = "सब सादा करें",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          FramedUserAvatar(
            url = sampleAvatarUrl,
            name = sampleDisplayName,
            avatarSize = 60.dp,
            frameUrl = activeFrameUrl,
            borderColor = if (activeFrameUrl.isNotBlank()) SwarGold else SwarCyan,
            borderWidth = 2.dp,
          )
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = when {
                equippedFrameLevel == 0L -> "DP फ्रेम: सादा (उतारा हुआ)"
                equippedFrameLevel > 0L -> "DP फ्रेम: LV.$equippedFrameLevel सक्रिय"
                activeFrameUrl.isNotBlank() -> "DP फ्रेम: ऑटो (उच्चतम सक्रिय)"
                else -> "DP फ्रेम: कोई नहीं"
              },
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFE3DDF8),
            )
            Text(
              text = when {
                equippedTypingLevel == 0L -> "टाइपिंग पट्टी: सादा बबल"
                equippedTypingLevel > 0L -> "टाइपिंग पट्टी: LV.$equippedTypingLevel सक्रिय"
                activeTypingUrl.isNotBlank() -> "टाइपिंग पट्टी: ऑटो (उच्चतम सक्रिय)"
                else -> "टाइपिंग पट्टी: कोई नहीं"
              },
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFE3DDF8),
            )
            Text(
              text = when {
                equippedEntryVideoLevel == 0L -> "एंट्री वीडियो: बंद (सादा प्रवेश)"
                equippedEntryVideoLevel > 0L -> "एंट्री वीडियो: LV.$equippedEntryVideoLevel सक्रिय"
                activeEntryVideo != null -> "एंट्री वीडियो: ऑटो (${activeEntryVideo.first.badgeLabel})"
                else -> "एंट्री वीडियो: उपलब्ध नहीं"
              },
              style = MaterialTheme.typography.labelSmall,
              color = SwarCyan,
            )
          }
        }

        TypingBannerChatBubble(
          senderName = sampleDisplayName.ifBlank { "Swar" },
          messageText = "मेरा चुना हुआ स्टाइल कैसा लग रहा है? ✨",
          badgeTag = "LV.$userLevel",
          bannerUrl = activeTypingUrl,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    // 2. Entry Video Mute / Unmute Toggle Card
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = Color(0xFF1D1739),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, SwarCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = if (isEntryVideoMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
          contentDescription = null,
          tint = if (isEntryVideoMuted) SwarCoral else SwarCyan,
          modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "एंट्री वीडियो आवाज़ म्यूट रखें",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = if (isEntryVideoMuted) "म्यूट है — एंट्री वीडियो बिना आवाज़ चलेगा" else "आवाज़ चालू — रूम प्रवेश पर वीडियो आवाज़ सुनाई देगी",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFB5ACDA),
          )
        }
        Switch(
          checked = isEntryVideoMuted,
          onCheckedChange = { onToggleEntryVideoMuted() },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = SwarCoral,
            uncheckedThumbColor = SwarCyan,
            uncheckedTrackColor = Color(0xFF2D2456),
          ),
          modifier = Modifier.testTag("style_entry_mute_switch"),
        )
      }
    }

    // 3. DP Frame Picker
    CosmeticTierPickerSection(
      title = "👑 DP फ्रेम (अनलॉक स्तरों में से चुनें या उतारें)",
      unlockedTiers = unlockedTiers,
      equippedLevel = equippedFrameLevel,
      hasAsset = { badge -> levelConfigs[badge]?.avatarFrameUrl?.isNotBlank() == true },
      tagPrefix = "style_frame_option",
      plainLabel = "सादा (बिना फ्रेम)",
      autoLabel = "ऑटो (उच्चतम)",
      onSelectLevel = { choice -> onEquipCosmetic(choice, null, null) },
    )

    // 4. Typing Banner / Box Picker — Exclusively LV.30 / 60 / 90 / 120
    CosmeticTierPickerSection(
      title = "💬 गोल्डन टाइपिंग बॉक्स (केवल LV.30, 60, 90, 120)",
      unlockedTiers = unlockedTiers,
      equippedLevel = equippedTypingLevel,
      hasAsset = { badge -> SwarUserIdentity.isTypingBoxTier(badge) },
      tagPrefix = "style_typing_option",
      plainLabel = "सादा चैट बबल",
      autoLabel = "ऑटो (उच्चतम)",
      emptyHint = "पहला गोल्डन टाइपिंग बॉक्स LV.30 पर खुलेगा (फिर LV.60, LV.90 और LV.120 पर)।",
      onSelectLevel = { choice -> onEquipCosmetic(null, choice, null) },
    )

    // 5. Entry Video Picker
    CosmeticTierPickerSection(
      title = "🎬 एंट्री वीडियो (पसंदीदा चुनें या बंद करें)",
      unlockedTiers = unlockedTiers,
      equippedLevel = equippedEntryVideoLevel,
      hasAsset = { badge -> levelConfigs[badge]?.entryVideoUrl?.isNotBlank() == true },
      tagPrefix = "style_entry_option",
      plainLabel = "सादा प्रवेश (वीडियो बंद)",
      autoLabel = "ऑटो (उच्चतम)",
      onSelectLevel = { choice -> onEquipCosmetic(null, null, choice) },
    )
  }
}

@Composable
private fun CosmeticTierPickerSection(
  title: String,
  unlockedTiers: List<LevelTierSpec>,
  equippedLevel: Long,
  hasAsset: (Int) -> Boolean,
  tagPrefix: String,
  plainLabel: String,
  autoLabel: String,
  emptyHint: String = "अनलॉक स्तरों में अभी कस्टम मीडिया नहीं जुड़ा है — फिर भी आप 'सादा' या 'ऑटो' चुन सकते हैं।",
  onSelectLevel: (Long) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF1B1536),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, Color(0xFF332B5C), RoundedCornerShape(14.dp)),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(Icons.Filled.Style, contentDescription = null, tint = SwarGold, modifier = Modifier.size(16.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CosmeticChoiceChip(
          label = plainLabel,
          selected = equippedLevel == 0L,
          accentColor = SwarCoral,
          onClick = { onSelectLevel(0L) },
          modifier = Modifier
            .weight(1f)
            .testTag("${tagPrefix}_0"),
        )
        CosmeticChoiceChip(
          label = autoLabel,
          selected = equippedLevel < 0L,
          accentColor = SwarCyan,
          onClick = { onSelectLevel(-1L) },
          modifier = Modifier
            .weight(1f)
            .testTag("${tagPrefix}_auto"),
        )
      }

      val availableTiers = unlockedTiers.filter { hasAsset(it.badgeLevel) }
      if (availableTiers.isEmpty()) {
        Text(
          text = emptyHint,
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFF9F98C4),
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          availableTiers.chunked(3).forEach { rowTiers ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              rowTiers.forEach { t ->
                val isSel = equippedLevel == t.badgeLevel.toLong()
                CosmeticChoiceChip(
                  label = "${t.badgeLabel} ✓",
                  selected = isSel,
                  accentColor = SwarMint,
                  onClick = { onSelectLevel(t.badgeLevel.toLong()) },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("${tagPrefix}_${t.badgeLevel}"),
                )
              }
              repeat(3 - rowTiers.size) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CosmeticChoiceChip(
  label: String,
  selected: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(10.dp),
    color = if (selected) accentColor.copy(alpha = 0.22f) else Color(0xFF251E47),
    modifier = modifier
      .height(42.dp)
      .border(
        width = if (selected) 1.5.dp else 1.dp,
        color = if (selected) accentColor else Color(0xFF3E356B),
        shape = RoundedCornerShape(10.dp),
      ),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) accentColor else Color(0xFFE1DCF8),
        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun LevelHpRoadmapList(
  coins: Long,
  currentLevel: Int,
  modifier: Modifier = Modifier,
) {
  val milestones = SwarUserIdentity.levelRoadmapMilestones
  val maxHp = SwarUserIdentity.MAX_LEVEL_120_HP
  val overallProgress = (coins.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)

  LazyColumn(
    modifier = modifier.testTag("level_roadmap_list"),
    contentPadding = PaddingValues(bottom = 12.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    item(key = "roadmap_summary_progress") {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E173B),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp)
          .border(1.dp, SwarCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "कुल सफ़र (LV.1 → LV.120)",
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text = "${(overallProgress * 100f).toInt()}% • लक्ष्य ${SwarUserIdentity.formatIndianNumber(maxHp)} HP",
              style = MaterialTheme.typography.labelSmall,
              color = SwarGold,
              fontWeight = FontWeight.Bold,
            )
          }
          LinearProgressIndicator(
            progress = { overallProgress },
            modifier = Modifier
              .fillMaxWidth()
              .height(7.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = SwarGold,
            trackColor = Color(0xFF332A5B),
          )
        }
      }
    }

    milestones.forEachIndexed { idx, m ->
      val nextMilestoneLevel = milestones.getOrNull(idx + 1)?.level
      val showCurrentPositionAfterThis = if (nextMilestoneLevel != null) {
        currentLevel >= m.level && currentLevel < nextMilestoneLevel
      } else {
        currentLevel >= m.level
      }

      if (m.deltaFromPrevious > 0L) {
        item(key = "roadmap_delta_${m.level}") {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 24.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .width(2.5.dp)
                .height(22.dp)
                .background(
                  if (coins >= m.totalHp) SwarMint else Color(0xFF473C78),
                  RoundedCornerShape(2.dp),
                ),
            )
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF241D48),
            ) {
              Text(
                text = "▼ +${SwarUserIdentity.formatIndianNumber(m.deltaFromPrevious)} HP",
                style = MaterialTheme.typography.labelSmall,
                color = if (coins >= m.totalHp) SwarMint else Color(0xFFB5ACDA),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      item(key = "roadmap_node_${m.level}") {
        val tier = SwarUserIdentity.tierForLevel(m.level)
        val reached = coins >= m.totalHp
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (reached) Color(0xFF231D46) else Color(0xFF191433),
          modifier = Modifier
            .fillMaxWidth()
            .border(
              1.dp,
              if (reached) tier.rimColor.copy(alpha = 0.65f) else Color(0xFF312A56),
              RoundedCornerShape(14.dp),
            )
            .testTag("roadmap_milestone_${m.level}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = tier.capsuleBottomColor.copy(alpha = 0.85f),
              modifier = Modifier.border(1.dp, tier.rimColor, RoundedCornerShape(10.dp)),
            ) {
              Text(
                text = "LV.${m.level}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = if (m.level == 1) "शुरुआत: 0 HP" else "कुल HP: ${SwarUserIdentity.formatIndianNumber(m.totalHp)}",
                  style = MaterialTheme.typography.labelLarge,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
                if (m.level == 120) {
                  Text(
                    text = "(अंतिम लक्ष्य)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SwarGold,
                    fontWeight = FontWeight.ExtraBold,
                  )
                }
              }
              Text(
                text = if (m.level == 1) {
                  "${tier.titleHindi} • प्रारंभिक पड़ाव"
                } else {
                  "आधा (50%): ${SwarUserIdentity.formatHalfHp(m.totalHp)} HP • ${tier.titleHindi}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (reached) SwarCyan else Color(0xFFA7A0CE),
              )
            }

            Icon(
              imageVector = if (reached) Icons.Filled.CheckCircle else Icons.Filled.Lock,
              contentDescription = if (reached) "पूर्ण" else "लॉक्ड",
              tint = if (reached) SwarMint else Color(0xFF6B658E),
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      if (showCurrentPositionAfterThis) {
        item(key = "roadmap_current_status") {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF33250B),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 3.dp)
              .border(1.2.dp, SwarGold, RoundedCornerShape(12.dp))
              .testTag("roadmap_current_status_banner"),
          ) {
            val curInfo = SwarUserIdentity.levelForCoins(coins)
            val neededLabel = if (curInfo.level >= 120) {
              "सर्वोच्च LV.120 पूर्ण"
            } else {
              "अगले LV.${curInfo.level + 1} के लिए ${SwarUserIdentity.formatIndianNumber(curInfo.nextLevelRemainingCoins)} HP और चाहिए"
            }
            Column(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = "★ आपकी वर्तमान स्थिति: LV.$currentLevel (कुल HP: ${SwarUserIdentity.formatIndianNumber(coins)} | आधा: ${SwarUserIdentity.formatHalfHp(coins)})",
                style = MaterialTheme.typography.labelMedium,
                color = SwarGold,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text = neededLabel,
                style = MaterialTheme.typography.labelSmall,
                color = SwarCyan,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      if (m.isHalfwayZoneAfter) {
        item(key = "roadmap_halfway_lv85") {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF122C3D),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 3.dp)
              .border(1.dp, SwarCyan, RoundedCornerShape(12.dp))
              .testTag("roadmap_halfway_banner"),
          ) {
            Text(
              text = "◆ पूरे 120 लेवल का ठीक आधा (50%) पड़ाव ≈ LV.85 (${SwarUserIdentity.formatHalfHp(maxHp)} HP)",
              style = MaterialTheme.typography.labelSmall,
              color = SwarCyan,
              fontWeight = FontWeight.ExtraBold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }
        }
      }
    }

    item(key = "roadmap_key_points") {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1D163A),
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp)
          .border(1.dp, Color(0xFF3B3068), RoundedCornerShape(14.dp)),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Text(
            text = "डायग्राम के मुख्य बिंदु (10-10 लेवल के अंतर से)",
            style = MaterialTheme.typography.labelLarge,
            color = SwarGold,
            fontWeight = FontWeight.ExtraBold,
          )
          Text(
            text = "• शुरुआती रफ़्तार (LV.1 से LV.30): पहले 10 लेवल में सिर्फ 31,149 HP, और LV.20 से LV.30 के बीच 1,89,714 HP।",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFD5D0EE),
          )
          Text(
            text = "• मध्यम पड़ाव (LV.30 से LV.80): हर 10 लेवल के ब्लॉक में लगभग 65,800 HP अतिरिक्त जुड़ते जाते हैं।",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFD5D0EE),
          )
          Text(
            text = "• आधा सफ़र (50% बंटवारा): LV.120 (47,78,163 HP) का ठीक आधा (23,89,081.5 HP) LV.85 पर पूरा होता है।",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFD5D0EE),
          )
          Text(
            text = "• अंतिम छलाँग (LV.110 से LV.120): आखिरी 10 लेवल में 7,58,410 HP की आवश्यकता पड़ती है।",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFD5D0EE),
          )
        }
      }
    }
  }
}
