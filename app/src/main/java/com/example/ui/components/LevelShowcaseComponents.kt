package com.example.ui.components

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.data.model.LevelTierSpec
import com.example.data.model.SwarUserIdentity
import com.example.data.model.UserLevelInfo
import com.example.ui.theme.SwarGold

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
  isCurrentTier: Boolean,
  isUnlocked: Boolean,
  isSelected: Boolean,
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
      .height(166.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF262047),
            Color(0xFF1A1534),
            Color(0xFF141029),
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
        .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
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
          .padding(top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        OrnateWingedLevelCrest(
          tier = tier,
          displayText = tier.badgeLabel,
          badgeWidth = 148.dp,
          badgeHeight = 56.dp,
          isCompact = false,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = tier.rangeLabel,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFE5E2F5),
          maxLines = 1,
        )

        Spacer(modifier = Modifier.height(3.dp))

        val requiredCoins = (tier.minLevel - 1) * 1000L
        Text(
          text = if (requiredCoins == 0L) tier.titleHindi else "${tier.titleHindi} • ${requiredCoins / 1000}K+ कॉइन",
          fontSize = 11.sp,
          color = if (isCurrentTier) SwarGold else Color(0xFF9C96BE),
          fontWeight = if (isCurrentTier) FontWeight.SemiBold else FontWeight.Normal,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun LevelCardsShowcaseDialog(
  levelInfo: UserLevelInfo,
  coins: Long,
  onDismiss: () -> Unit,
) {
  val tiers = SwarUserIdentity.levelTiers
  val currentTier = remember(levelInfo.level) { SwarUserIdentity.tierForLevel(levelInfo.level) }
  var selectedTier by remember(levelInfo.level) { mutableStateOf(currentTier) }

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
                text = "सभी 13 लेवल रैंक और कॉइन लक्ष्य",
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
                  text = "कुल कॉइंस: $coins",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color(0xFFD5D0EE),
                )
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
                "बधाई हो! आप सर्वोच्च LV.120 स्तर पर हैं।"
              } else {
                "अगले स्तर (LV.${levelInfo.level + 1}) के लिए ${levelInfo.nextLevelRemainingCoins} कॉइंस और चाहिए।"
              },
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFBDB6E2),
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large 2-Column (Adaptive) Cards Grid for all 13 Level Tiers
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 148.dp),
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
            LevelTierLargeCard(
              tier = tier,
              isCurrentTier = isCurrent,
              isUnlocked = isUnlocked,
              isSelected = selectedTier.badgeLevel == tier.badgeLevel,
              onClick = { selectedTier = tier },
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Tier Milestone Info Pill
        val minRequiredCoins = (selectedTier.minLevel - 1) * 1000L
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF211B40),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, selectedTier.glowColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
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
                  "अनलॉक लक्ष्य: $minRequiredCoins कॉइंस"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFACA5D6),
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (levelInfo.level >= selectedTier.minLevel) "अनलॉक ✓" else "लॉक्ड",
              style = MaterialTheme.typography.labelMedium,
              color = if (levelInfo.level >= selectedTier.minLevel) Color(0xFF4CD964) else SwarGold,
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }
      }
    }
  }
}
