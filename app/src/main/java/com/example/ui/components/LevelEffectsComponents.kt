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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
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
  val prestige = ((tier.badgeLevel - 1).coerceIn(0, 119) / 119f)
  val isSovereign = tier.badgeLevel >= 100
  val bgBrush = if (isDarkSurface) {
    Brush.verticalGradient(
      listOf(
        tier.capsuleTopColor.copy(alpha = 0.36f + 0.22f * prestige),
        Color(0xFF140D2C),
      )
    )
  } else {
    Brush.verticalGradient(
      listOf(
        tier.capsuleTopColor.copy(alpha = 0.16f + 0.14f * prestige),
        Color(0xFFF6F8FC),
      )
    )
  }
  val chipBorderBrush = if (isSovereign) {
    Brush.horizontalGradient(listOf(tier.rimColor, Color(0xFFFFF8D6), SwarGold, tier.rimColor))
  } else {
    Brush.horizontalGradient(listOf(tier.rimColor.copy(alpha = 0.65f + 0.25f * prestige), tier.glowColor.copy(alpha = 0.65f)))
  }
  val labelBrush = Brush.linearGradient(
    colors = if (isDarkSurface) {
      listOf(
        tier.textColor,
        if (isSovereign) Color(0xFFFFF8D6) else Color.White,
        if (isSovereign) SwarGold else tier.rimColor,
        tier.textColor,
      )
    } else {
      listOf(
        Color(0xFF1D2336),
        if (isSovereign) Color(0xFF8A5400) else Color(0xFF475580),
        Color(0xFF1D2336),
      )
    },
    start = Offset((sheenFraction - 0.28f) * 140f, 0f),
    end = Offset((sheenFraction + 0.28f) * 140f, 48f),
  )

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(bgBrush)
      .border(if (isSovereign) 1.3.dp else 1.dp, chipBorderBrush, RoundedCornerShape(12.dp))
      .padding(horizontal = 8.dp, vertical = 5.dp),
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val bandW = size.width * (0.35f + 0.10f * prestige)
      val sx = (size.width + bandW) * sheenFraction - bandW
      drawRect(
        brush = Brush.linearGradient(
          colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.20f + 0.28f * prestige),
            if (isSovereign) Color(0xFFFFE57F).copy(alpha = 0.24f) else Color.Transparent,
            Color.Transparent,
          ),
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
        style = MaterialTheme.typography.labelSmall.copy(
          brush = labelBrush,
          fontWeight = FontWeight.ExtraBold,
        ),
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
  val trimmedBanner = bannerUrl.trim()
  val hasBanner = trimmedBanner.isNotBlank()
  val builtinTier = remember(trimmedBanner) { SwarUserIdentity.builtinTypingTierFromUrl(trimmedBanner) }
  val shape = RoundedCornerShape(if (hasBanner) 16.dp else 14.dp)

  if (!hasBanner) {
    Box(
      modifier = modifier
        .clip(shape)
        .background(Color(0x66000000))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
      TypingBannerMessageRow(
        senderName = senderName,
        messageText = messageText,
        badgeTag = badgeTag,
        messageType = messageType,
        hasBanner = false,
        titleColor = SwarCyan,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      )
    }
    return
  }

  val infiniteTransition = rememberInfiniteTransition(label = "goldenTypingAnim")
  val sheenX by infiniteTransition.animateFloat(
    initialValue = -0.35f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
    label = "sheen",
  )
  val glowPulse by infiniteTransition.animateFloat(
    initialValue = 0.55f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(1150, easing = LinearEasing), RepeatMode.Reverse),
    label = "glow",
  )
  val sparklePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
    label = "sparkle",
  )

  val palette = remember(builtinTier) {
    when (builtinTier) {
      30 -> GoldenTypingPalette(
        topBg = Color(0xFF211428),
        bottomBg = Color(0xFF33200B),
        rimPrimary = Color(0xFFFFD875),
        rimSecondary = Color(0xFFFFA96B),
        underGlowCore = Color(0xFFFFE588),
        underGlowEdge = Color(0xFFFF9A3C),
        badgeBg = Color(0xDD2C1930),
        titleColor = Color(0xFFFFDF7C),
      )
      60 -> GoldenTypingPalette(
        topBg = Color(0xFF0E1A3B),
        bottomBg = Color(0xFF2C2109),
        rimPrimary = Color(0xFFFFD54F),
        rimSecondary = Color(0xFF56E4FF),
        underGlowCore = Color(0xFFFFEA8A),
        underGlowEdge = Color(0xFF38D4FF),
        badgeBg = Color(0xDD132248),
        titleColor = Color(0xFFFFE066),
      )
      90 -> GoldenTypingPalette(
        topBg = Color(0xFF2E0B1A),
        bottomBg = Color(0xFF3B2106),
        rimPrimary = Color(0xFFFFD740),
        rimSecondary = Color(0xFFFF5277),
        underGlowCore = Color(0xFFFFEE99),
        underGlowEdge = Color(0xFFFF4D6D),
        badgeBg = Color(0xDD380F21),
        titleColor = Color(0xFFFFE47A),
      )
      else -> GoldenTypingPalette(
        topBg = Color(0xFF140F07),
        bottomBg = Color(0xFF3F2B04),
        rimPrimary = Color(0xFFFFF3B0),
        rimSecondary = Color(0xFFFFAE00),
        underGlowCore = Color(0xFFFFF8D6),
        underGlowEdge = Color(0xFFFFB703),
        badgeBg = Color(0xDD261904),
        titleColor = Color(0xFFFFE885),
      )
    }
  }

  val animatedRimBrush = Brush.horizontalGradient(
    colors = listOf(
      palette.rimSecondary,
      palette.rimPrimary,
      Color.White.copy(alpha = 0.85f * glowPulse),
      palette.rimPrimary,
      palette.rimSecondary,
    )
  )

  Box(
    modifier = modifier
      .widthIn(min = 188.dp)
      .clip(shape)
      .border(width = 1.5.dp, brush = animatedRimBrush, shape = shape)
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
  ) {
    if (builtinTier == null) {
      AsyncImage(
        model = trimmedBanner,
        contentDescription = "गोल्डन टाइपिंग पट्टी बैकग्राउंड",
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
      )
      Box(
        modifier = Modifier
          .matchParentSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color(0xA610091F),
                Color(0x7A140D22),
                Color(0xBF281B05),
              )
            )
          ),
      )
    } else {
      Box(
        modifier = Modifier
          .matchParentSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(palette.topBg, palette.topBg.copy(alpha = 0.94f), palette.bottomBg)
            )
          ),
      )
    }

    // Specular golden light sweep + sparkling luxury diamond glints across background
    Canvas(modifier = Modifier.matchParentSize()) {
      val bandW = size.width * 0.34f
      val startX = (size.width + bandW) * sheenX - bandW
      drawRect(
        brush = Brush.linearGradient(
          colors = listOf(
            Color.Transparent,
            palette.rimPrimary.copy(alpha = 0.16f * glowPulse),
            Color.White.copy(alpha = 0.12f),
            Color.Transparent,
          ),
          start = Offset(startX, 0f),
          end = Offset(startX + bandW, size.height),
        )
      )

      // Delicate inner corner golden filigree brackets
      val bracket = 9.dp.toPx()
      val inset = 4.dp.toPx()
      val accentStroke = 1.4.dp.toPx()
      val cornerColor = palette.rimPrimary.copy(alpha = 0.72f * glowPulse)
      drawLine(cornerColor, Offset(inset, inset + bracket), Offset(inset, inset), strokeWidth = accentStroke, cap = StrokeCap.Round)
      drawLine(cornerColor, Offset(inset, inset), Offset(inset + bracket, inset), strokeWidth = accentStroke, cap = StrokeCap.Round)
      drawLine(cornerColor, Offset(size.width - inset - bracket, inset), Offset(size.width - inset, inset), strokeWidth = accentStroke, cap = StrokeCap.Round)
      drawLine(cornerColor, Offset(size.width - inset, inset), Offset(size.width - inset, inset + bracket), strokeWidth = accentStroke, cap = StrokeCap.Round)

      // Twinkling golden stardust above & along the glowing floor
      val starPositions = listOf(0.14f, 0.36f, 0.58f, 0.79f, 0.91f)
      starPositions.forEachIndexed { idx, xFrac ->
        val angle = (sparklePhase * 2f * PI + idx * 1.3).toFloat()
        val alpha = ((sin(angle) * 0.5f) + 0.5f).coerceIn(0.15f, 0.95f)
        val sx = size.width * xFrac
        val sy = size.height * (if (idx % 2 == 0) 0.24f else 0.72f)
        val r = (2.2f + (idx % 3) * 1.1f).dp.toPx()
        val starPath = Path().apply {
          moveTo(sx, sy - r)
          lineTo(sx + r * 0.32f, sy - r * 0.32f)
          lineTo(sx + r, sy)
          lineTo(sx + r * 0.32f, sy + r * 0.32f)
          lineTo(sx, sy + r)
          lineTo(sx - r * 0.32f, sy + r * 0.32f)
          lineTo(sx - r, sy)
          lineTo(sx - r * 0.32f, sy - r * 0.32f)
          close()
        }
        drawPath(starPath, color = Color(0xFFFFF4C2).copy(alpha = alpha))
      }
    }

    // Typed text sits cleanly on TOP; radiant golden animated floor shines UNDERNEATH
    Column(modifier = Modifier.fillMaxWidth()) {
      TypingBannerMessageRow(
        senderName = senderName,
        messageText = messageText,
        badgeTag = badgeTag,
        messageType = messageType,
        hasBanner = true,
        titleColor = palette.titleColor,
        badgeBgColor = palette.badgeBg,
        modifier = Modifier.padding(start = 13.dp, end = 13.dp, top = 8.dp, bottom = 5.dp),
      )

      // Luminous shimmering golden under-glow beam & rising light wave underneath the text
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp),
      ) {
        // Soft golden upward aura bloom underneath the text line
        drawRect(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color.Transparent,
              palette.underGlowEdge.copy(alpha = 0.28f * glowPulse),
              palette.rimPrimary.copy(alpha = 0.55f * glowPulse),
            )
          )
        )

        // Flowing dual golden wave ribbons
        val wavePath = Path()
        val baseY = size.height * 0.62f
        val amp = size.height * 0.24f
        wavePath.moveTo(0f, baseY)
        val steps = 24
        for (i in 0..steps) {
          val t = i.toFloat() / steps
          val x = size.width * t
          val y = baseY + sin((t * 4f + sparklePhase * 2f) * PI).toFloat() * amp
          wavePath.lineTo(x, y)
        }
        drawPath(
          path = wavePath,
          brush = Brush.horizontalGradient(
            colors = listOf(
              palette.underGlowEdge.copy(alpha = 0.4f),
              palette.underGlowCore.copy(alpha = 0.95f * glowPulse),
              Color.White.copy(alpha = 0.9f),
              palette.underGlowCore.copy(alpha = 0.95f * glowPulse),
              palette.underGlowEdge.copy(alpha = 0.4f),
            )
          ),
          style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        // Bright gliding golden energy comet along the bottom shimmer bar
        val cometCenter = (size.width * sparklePhase).coerceIn(0f, size.width)
        val cometRadius = size.width * 0.22f
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              Color.White.copy(alpha = 0.95f),
              palette.underGlowCore.copy(alpha = 0.72f),
              Color.Transparent,
            ),
            center = Offset(cometCenter, size.height * 0.75f),
            radius = cometRadius.coerceAtLeast(18f),
          ),
          center = Offset(cometCenter, size.height * 0.75f),
          radius = cometRadius.coerceAtLeast(18f),
        )

        // Bottom specular golden floor strip
        drawLine(
          brush = Brush.horizontalGradient(
            colors = listOf(
              Color.Transparent,
              palette.rimPrimary,
              Color.White,
              palette.rimPrimary,
              Color.Transparent,
            )
          ),
          start = Offset(0f, size.height - 1.dp.toPx()),
          end = Offset(size.width, size.height - 1.dp.toPx()),
          strokeWidth = 2.dp.toPx(),
        )
      }
    }
  }
}

private data class GoldenTypingPalette(
  val topBg: Color,
  val bottomBg: Color,
  val rimPrimary: Color,
  val rimSecondary: Color,
  val underGlowCore: Color,
  val underGlowEdge: Color,
  val badgeBg: Color,
  val titleColor: Color,
)

@Composable
private fun TypingBannerMessageRow(
  senderName: String,
  messageText: String,
  badgeTag: String,
  messageType: String,
  hasBanner: Boolean,
  titleColor: Color,
  badgeBgColor: Color = Color(0xCC241547),
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (badgeTag.isNotBlank()) {
      val parsedLevel = remember(badgeTag) {
        if (badgeTag.startsWith("LV.", ignoreCase = true)) {
          badgeTag.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 120)
        } else {
          null
        }
      }
      if (parsedLevel != null) {
        val levelTier = remember(parsedLevel) { SwarUserIdentity.tierForLevel(parsedLevel) }
        val prestige = ((parsedLevel - 1).coerceIn(0, 119) / 119f)
        val isSovereign = parsedLevel >= 101
        val lvAnim = rememberInfiniteTransition(label = "chatLvShimmer")
        val sweep by lvAnim.animateFloat(
          initialValue = -0.35f,
          targetValue = 1.35f,
          animationSpec = infiniteRepeatable(
            tween(durationMillis = if (isSovereign) 1500 else 2200, easing = LinearEasing),
            RepeatMode.Restart,
          ),
          label = "chatLvSweep",
        )
        val lvTextBrush = Brush.linearGradient(
          colors = listOf(
            levelTier.textColor,
            if (isSovereign) Color(0xFFFFF8D6) else Color.White,
            if (isSovereign) Color(0xFFFFE57F) else levelTier.rimColor,
            levelTier.textColor,
          ),
          start = Offset((sweep - 0.25f) * 160f, 0f),
          end = Offset((sweep + 0.25f) * 160f, 52f),
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
              Brush.horizontalGradient(
                listOf(levelTier.capsuleTopColor, levelTier.capsuleBottomColor)
              )
            )
            .border(
              width = if (isSovereign) 1.2.dp else 0.9.dp,
              brush = Brush.horizontalGradient(
                listOf(levelTier.rimColor, Color.White.copy(alpha = 0.78f + 0.22f * prestige), levelTier.rimColor)
              ),
              shape = RoundedCornerShape(8.dp),
            ),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.matchParentSize()) {
            val bw = size.width * 0.42f
            val sx = (size.width + bw) * sweep - bw
            drawRect(
              brush = Brush.linearGradient(
                colors = listOf(
                  Color.Transparent,
                  Color.White.copy(alpha = 0.22f + 0.30f * prestige),
                  Color.Transparent,
                ),
                start = Offset(sx, 0f),
                end = Offset(sx + bw, size.height),
              )
            )
          }
          Text(
            text = badgeTag,
            style = MaterialTheme.typography.labelSmall.copy(
              brush = lvTextBrush,
              fontWeight = FontWeight.ExtraBold,
              shadow = Shadow(color = Color(0xCC000000), offset = Offset(0f, 1f), blurRadius = 3f),
            ),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      } else {
        Surface(
          color = when (messageType) {
            "gift" -> SwarCoral
            "join" -> SwarViolet
            else -> if (hasBanner) badgeBgColor else SwarCyan.copy(alpha = 0.25f)
          },
          shape = RoundedCornerShape(8.dp),
          modifier = if (hasBanner) {
            Modifier.border(0.8.dp, SwarGold.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
          } else Modifier,
        ) {
          Text(
            text = badgeTag,
            style = MaterialTheme.typography.labelSmall,
            color = if (hasBanner) SwarGold else Color.White,
            fontWeight = if (hasBanner) FontWeight.ExtraBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }
      Spacer(modifier = Modifier.width(6.dp))
    }

    val textShadow = if (hasBanner) {
      Shadow(color = Color(0xE6000000), offset = Offset(1f, 1.5f), blurRadius = 5f)
    } else {
      null
    }
    Text(
      text = buildAnnotatedString {
        withStyle(
          SpanStyle(
            color = titleColor,
            fontWeight = FontWeight.ExtraBold,
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

/**
 * Sovereign 24K Golden Royal Entry Banner ("शाही एंट्री पट्टी") rendered over entry videos and previews.
 * Features sculpted golden wings ("पंख") and crown ("ताज") scaled by level tier, DP with sunburst halo,
 * ornate LV crest badge, member name, and a continuous shimmering golden light-wave floor underneath.
 */
@Composable
fun RoyalGoldenEntryBanner(
  senderName: String,
  senderAvatarUrl: String,
  entryLevel: Int,
  frameUrl: String = "",
  modifier: Modifier = Modifier,
  compact: Boolean = false,
) {
  val safeLevel = entryLevel.coerceIn(1, 120)
  val tier = remember(safeLevel) { SwarUserIdentity.tierForLevel(safeLevel) }
  val prestigeProgress = ((safeLevel - 1).coerceIn(0, 119) / 119f)
  val rankTier = remember(safeLevel) {
    when {
      safeLevel >= 101 -> 5
      safeLevel >= 90 -> 4
      safeLevel >= 60 -> 3
      safeLevel >= 30 -> 2
      else -> 1
    }
  }

  val infinite = rememberInfiniteTransition(label = "royalEntryBannerAnim")
  val sheenX by infinite.animateFloat(
    initialValue = -0.35f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
    label = "entrySheen",
  )
  val glowPulse by infinite.animateFloat(
    initialValue = 0.62f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(980, easing = LinearEasing), RepeatMode.Reverse),
    label = "entryGlow",
  )
  val sparklePhase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
    label = "entrySparkle",
  )
  val haloAngleDeg by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(6500, easing = LinearEasing), RepeatMode.Restart),
    label = "entryHalo",
  )

  val wingExtend = if (compact) 14.dp else 22.dp
  val crownHeadroom = if (compact) 12.dp else 18.dp
  val avatarSize = if (compact) 30.dp else 46.dp
  val shape = RoundedCornerShape(if (compact) 14.dp else 20.dp)

  val cardBgGradient = Brush.horizontalGradient(
    colors = listOf(
      Color(0xEE1C1003),
      Color(0xF23A2404),
      tier.capsuleBottomColor.copy(alpha = 0.88f),
      Color(0xF23A2404),
      Color(0xEE180D02),
    )
  )
  val goldRimBrush = Brush.horizontalGradient(
    colors = listOf(
      Color(0xFFB8730A),
      Color(0xFFFFD86B),
      Color(0xFFFFF8D6).copy(alpha = glowPulse),
      tier.rimColor,
      Color(0xFFFFD86B),
      Color(0xFFB8730A),
    )
  )

  Box(modifier = modifier.semantics(mergeDescendants = true) {}) {
    // Back Layer: Sculpted Twin Royal Golden Wings flaring outward on Left & Right
    Canvas(modifier = Modifier.matchParentSize()) {
      val w = size.width
      val h = size.height
      val bodyTop = crownHeadroom.toPx()
      val cy = bodyTop + (h - bodyTop) * 0.48f
      val wingReach = wingExtend.toPx() * 1.15f
      val wingH = (h - bodyTop) * 0.78f

      val wingBrush = Brush.linearGradient(
        colors = listOf(
          Color(0xFFFFF4B8),
          SwarGold,
          tier.rimColor,
          Color(0xFFAA6800),
        ),
        start = Offset(0f, cy - wingH * 0.5f),
        end = Offset(w, cy + wingH * 0.5f),
      )

      // Outer golden halo behind the whole banner
      drawRoundRect(
        brush = Brush.radialGradient(
          colors = listOf(
            SwarGold.copy(alpha = 0.34f * glowPulse),
            tier.glowColor.copy(alpha = 0.16f * glowPulse),
            Color.Transparent,
          ),
          center = Offset(w / 2f, cy),
          radius = w * 0.55f,
        ),
        topLeft = Offset(0f, bodyTop * 0.5f),
        size = Size(w, h - bodyTop * 0.5f),
        cornerRadius = CornerRadius(30f, 30f),
      )

      val featherCount = (rankTier + 1).coerceIn(2, 5)
      for (layer in 0 until featherCount) {
        val scale = 1f - layer * 0.16f
        val dy = (layer - featherCount / 2f) * (wingH * 0.16f)
        val tipLeftX = (wingReach * (1f - scale)).coerceAtLeast(1.dp.toPx())
        val tipRightX = w - tipLeftX
        val anchorLeftX = wingReach * 0.92f
        val anchorRightX = w - anchorLeftX

        val leftFeather = Path().apply {
          moveTo(anchorLeftX, cy + dy - wingH * 0.11f * scale)
          lineTo(tipLeftX, cy + dy - wingH * 0.32f * scale)
          lineTo(tipLeftX + wingReach * 0.28f, cy + dy)
          lineTo(anchorLeftX, cy + dy + wingH * 0.13f * scale)
          close()
        }
        drawPath(leftFeather, wingBrush)
        drawPath(
          leftFeather,
          color = Color(0xFFFFF5CC).copy(alpha = 0.85f * glowPulse),
          style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round),
        )

        val rightFeather = Path().apply {
          moveTo(anchorRightX, cy + dy - wingH * 0.11f * scale)
          lineTo(tipRightX, cy + dy - wingH * 0.32f * scale)
          lineTo(tipRightX - wingReach * 0.28f, cy + dy)
          lineTo(anchorRightX, cy + dy + wingH * 0.13f * scale)
          close()
        }
        drawPath(rightFeather, wingBrush)
        drawPath(
          rightFeather,
          color = Color(0xFFFFF5CC).copy(alpha = 0.85f * glowPulse),
          style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round),
        )
      }
    }

    // Center Royal 24K Gold Banner Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          start = wingExtend * 0.68f,
          end = wingExtend * 0.68f,
          top = crownHeadroom,
        )
        .clip(shape)
        .background(cardBgGradient)
        .border(width = if (compact) 1.5.dp else 2.dp, brush = goldRimBrush, shape = shape),
    ) {
      // Specular Gold Sweep + Corner Filigree + Stardust Gems
      Canvas(modifier = Modifier.matchParentSize()) {
        val bandW = size.width * 0.32f
        val sx = (size.width + bandW) * sheenX - bandW
        drawRect(
          brush = Brush.linearGradient(
            colors = listOf(
              Color.Transparent,
              Color(0xFFFFE57F).copy(alpha = 0.24f * glowPulse),
              Color.White.copy(alpha = 0.16f),
              Color.Transparent,
            ),
            start = Offset(sx, 0f),
            end = Offset(sx + bandW, size.height),
          )
        )

        // Engraved inner gold double hairline top accent
        drawLine(
          brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color(0xFFFFF1A8).copy(alpha = 0.7f), Color.Transparent)
          ),
          start = Offset(12.dp.toPx(), 3.dp.toPx()),
          end = Offset(size.width - 12.dp.toPx(), 3.dp.toPx()),
          strokeWidth = 1.dp.toPx(),
        )

        // Floating 4-point sparkles (richer density from LV.1 up to LV.101-120)
        val sparkleX = if (rankTier >= 5) {
          listOf(0.14f, 0.25f, 0.39f, 0.52f, 0.66f, 0.79f, 0.90f, 0.96f)
        } else {
          listOf(0.22f, 0.43f, 0.64f, 0.82f, 0.93f)
        }
        sparkleX.forEachIndexed { idx, frac ->
          val wave = sin((sparklePhase * 2f * PI + idx * 1.15).toDouble()).toFloat()
          val a = (wave * 0.45f + 0.55f).coerceIn(0.2f, 1f) * (0.68f + 0.32f * prestigeProgress)
          val px = size.width * frac
          val py = size.height * (if (idx % 2 == 0) 0.22f else 0.72f)
          val r = ((if (compact) 2f else 3f) + (idx % 2) * 1.1f + prestigeProgress * 0.7f).dp.toPx()
          val star = Path().apply {
            moveTo(px, py - r)
            lineTo(px + r * 0.3f, py - r * 0.3f)
            lineTo(px + r, py)
            lineTo(px + r * 0.3f, py + r * 0.3f)
            lineTo(px, py + r)
            lineTo(px - r * 0.3f, py + r * 0.3f)
            lineTo(px - r, py)
            lineTo(px - r * 0.3f, py - r * 0.3f)
            close()
          }
          drawPath(star, Color(0xFFFFF8D6).copy(alpha = a))
        }
      }

      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(
              horizontal = if (compact) 8.dp else 14.dp,
              vertical = if (compact) 6.dp else 10.dp,
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        ) {
          // User DP inside rotating Golden Sunburst Halo + Frame
          Box(
            modifier = Modifier.size(avatarSize + (if (compact) 10.dp else 14.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val c = Offset(size.width / 2f, size.height / 2f)
              val outerR = size.minDimension * 0.5f
              val innerR = size.minDimension * 0.36f
              drawCircle(
                brush = Brush.radialGradient(
                  colors = listOf(SwarGold.copy(alpha = 0.52f * glowPulse), Color.Transparent),
                  center = c,
                  radius = outerR,
                ),
                center = c,
                radius = outerR,
              )
              val rayCount = 8 + rankTier * 2
              for (rIdx in 0 until rayCount) {
                val theta = Math.toRadians((haloAngleDeg + rIdx * (360f / rayCount)).toDouble())
                val p1 = Offset(
                  c.x + (cos(theta) * innerR).toFloat(),
                  c.y + (sin(theta) * innerR).toFloat(),
                )
                val p2 = Offset(
                  c.x + (cos(theta) * outerR).toFloat(),
                  c.y + (sin(theta) * outerR).toFloat(),
                )
                drawLine(
                  color = Color(0xFFFFE57F).copy(alpha = 0.7f * glowPulse),
                  start = p1,
                  end = p2,
                  strokeWidth = 1.4.dp.toPx(),
                  cap = StrokeCap.Round,
                )
              }
            }
            FramedUserAvatar(
              url = senderAvatarUrl,
              name = senderName,
              avatarSize = avatarSize,
              frameUrl = frameUrl,
              borderColor = Color(0xFFFFE57F),
              borderWidth = if (compact) 1.5.dp else 2.2.dp,
            )
          }

          // Center: Level Crest + Royal Title + Member Name
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              val chipTextBrush = Brush.linearGradient(
                colors = listOf(
                  Color(0xFFFFE57F),
                  Color(0xFFFFFBE6),
                  if (rankTier >= 5) Color(0xFFFFD54F) else Color.White,
                  Color(0xFFFFE57F),
                ),
                start = Offset((sheenX - 0.25f) * 180f, 0f),
                end = Offset((sheenX + 0.25f) * 180f, 60f),
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(
                    Brush.horizontalGradient(
                      listOf(Color(0xEE2B1600), tier.capsuleBottomColor.copy(alpha = 0.92f), Color(0xEE2B1600))
                    )
                  )
                  .border(
                    width = if (rankTier >= 5) 1.3.dp else 1.dp,
                    brush = goldRimBrush,
                    shape = RoundedCornerShape(10.dp),
                  )
                  .testTag("entry_banner_level_chip"),
              ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                  val bandW = size.width * 0.45f
                  val sx = (size.width + bandW) * sheenX - bandW
                  drawRect(
                    brush = Brush.linearGradient(
                      colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = (0.24f + 0.34f * prestigeProgress) * glowPulse),
                        Color.Transparent,
                      ),
                      start = Offset(sx, 0f),
                      end = Offset(sx + bandW, size.height),
                    )
                  )
                }
                Row(
                  modifier = Modifier.padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFFFDF6B),
                    modifier = Modifier.size(if (compact) 11.dp else 14.dp),
                  )
                  Text(
                    text = "LV.$safeLevel",
                    style = TextStyle(
                      brush = chipTextBrush,
                      fontSize = if (compact) 10.sp else 12.sp,
                      fontWeight = FontWeight.ExtraBold,
                      shadow = Shadow(color = Color(0xD9000000), offset = Offset(0f, 1.5f), blurRadius = 4f),
                    ),
                    maxLines = 1,
                  )
                }
              }
              Text(
                text = tier.titleHindi,
                color = Color(0xFFFFDF8E),
                fontSize = if (compact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Text(
              text = senderName.ifBlank { "शाही सदस्य" },
              color = Color.White,
              fontSize = if (compact) 13.sp else 17.sp,
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = MaterialTheme.typography.titleMedium.copy(
                shadow = Shadow(color = Color(0xF0000000), offset = Offset(1f, 2f), blurRadius = 6f)
              ),
              modifier = Modifier.testTag("entry_banner_sender_name"),
            )

            if (!compact) {
              Text(
                text = "✨ रूम में शानदार प्रवेश पधारे ✨",
                color = Color(0xFFFFE699),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
              )
            }
          }

          // Right: Royal Tier Medallion Crest with inner sheen & LV shimmer text
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(
                Brush.verticalGradient(
                  listOf(tier.capsuleTopColor.copy(alpha = 0.92f), tier.capsuleBottomColor.copy(alpha = 0.92f))
                )
              )
              .border(1.3.dp, goldRimBrush, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Canvas(modifier = Modifier.matchParentSize()) {
              val bandW = size.width * 0.45f
              val sx = (size.width + bandW) * sheenX - bandW
              drawRect(
                brush = Brush.linearGradient(
                  colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = (0.20f + 0.30f * prestigeProgress) * glowPulse),
                    Color.Transparent,
                  ),
                  start = Offset(sx, 0f),
                  end = Offset(sx + bandW, size.height),
                )
              )
            }
            Column(
              modifier = Modifier.padding(horizontal = if (compact) 7.dp else 10.dp, vertical = if (compact) 3.dp else 6.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFFF6C7),
                modifier = Modifier.size(if (compact) 13.dp else 18.dp),
              )
              Text(
                text = tier.badgeLabel,
                style = TextStyle(
                  brush = Brush.linearGradient(
                    colors = listOf(tier.textColor, Color(0xFFFFFBE6), SwarGold, tier.textColor),
                    start = Offset((sheenX - 0.25f) * 140f, 0f),
                    end = Offset((sheenX + 0.25f) * 140f, 50f),
                  ),
                  fontSize = if (compact) 9.sp else 11.sp,
                  fontWeight = FontWeight.ExtraBold,
                  shadow = Shadow(color = Color(0xCC000000), offset = Offset(0f, 1.5f), blurRadius = 3f),
                ),
                maxLines = 1,
              )
            }
          }
        }

        // Bottom Floor: Radiant 24K Gold Animated Under-Glow & Flowing Energy Wave
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 7.dp else 11.dp),
        ) {
          drawRect(
            brush = Brush.verticalGradient(
              colors = listOf(
                Color.Transparent,
                SwarGold.copy(alpha = 0.38f * glowPulse),
                Color(0xFFFFF4BD).copy(alpha = 0.72f * glowPulse),
              )
            )
          )

          val wavePath = Path()
          val baseY = size.height * 0.58f
          val amp = size.height * 0.28f
          wavePath.moveTo(0f, baseY)
          val steps = 28
          for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = size.width * t
            val y = baseY + sin((t * 5f + sparklePhase * 2f) * PI).toFloat() * amp
            wavePath.lineTo(x, y)
          }
          drawPath(
            path = wavePath,
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color(0xFFFF9F1C).copy(alpha = 0.45f),
                Color(0xFFFFE57F),
                Color.White,
                Color(0xFFFFE57F),
                Color(0xFFFF9F1C).copy(alpha = 0.45f),
              )
            ),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
          )

          val cometX = size.width * sparklePhase
          val cometR = (size.width * 0.24f).coerceAtLeast(22f)
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                Color.White,
                Color(0xFFFFE885).copy(alpha = 0.78f),
                Color.Transparent,
              ),
              center = Offset(cometX, size.height * 0.75f),
              radius = cometR,
            ),
            center = Offset(cometX, size.height * 0.75f),
            radius = cometR,
          )

          drawLine(
            brush = Brush.horizontalGradient(
              colors = listOf(Color.Transparent, Color(0xFFFFE066), Color.White, Color(0xFFFFE066), Color.Transparent)
            ),
            start = Offset(0f, size.height - 1.dp.toPx()),
            end = Offset(size.width, size.height - 1.dp.toPx()),
            strokeWidth = 2.2.dp.toPx(),
          )
        }
      }
    }

    // Floating 24K Golden Royal Crown ("ताज") perched right over the User DP
    val crownW = if (compact) 30.dp else 44.dp
    val crownH = if (compact) 16.dp else 24.dp
    val crownStart = wingExtend * 0.68f + (if (compact) 12.dp else 21.dp)
    Canvas(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(start = crownStart, top = 1.dp)
        .size(width = crownW, height = crownH),
    ) {
      val cw = size.width
      val ch = size.height
      val crownPath = Path().apply {
        moveTo(cw * 0.08f, ch * 0.88f)
        lineTo(cw * 0.04f, ch * 0.28f)
        lineTo(cw * 0.28f, ch * 0.56f)
        if (rankTier >= 3) {
          lineTo(cw * 0.39f, ch * 0.16f)
          lineTo(cw * 0.45f, ch * 0.44f)
        }
        lineTo(cw * 0.50f, ch * 0.04f)
        if (rankTier >= 3) {
          lineTo(cw * 0.55f, ch * 0.44f)
          lineTo(cw * 0.61f, ch * 0.16f)
        }
        lineTo(cw * 0.72f, ch * 0.56f)
        lineTo(cw * 0.96f, ch * 0.28f)
        lineTo(cw * 0.92f, ch * 0.88f)
        close()
      }
      drawPath(
        path = crownPath,
        brush = Brush.verticalGradient(
          colors = listOf(Color(0xFFFFF8CC), Color(0xFFFFD54F), Color(0xFFC67B00))
        ),
      )
      drawPath(
        path = crownPath,
        color = Color(0xFFFFF8E1),
        style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round),
      )
      // Base crown band
      drawRoundRect(
        color = Color(0xFFD98700),
        topLeft = Offset(cw * 0.08f, ch * 0.78f),
        size = Size(cw * 0.84f, ch * 0.18f),
        cornerRadius = CornerRadius(4f, 4f),
      )
      // Center jewel gem on the crown
      drawCircle(
        color = tier.gemColor.copy(alpha = glowPulse),
        radius = ch * 0.14f,
        center = Offset(cw * 0.50f, ch * 0.64f),
      )
      drawCircle(
        color = Color.White,
        radius = ch * 0.06f,
        center = Offset(cw * 0.48f, ch * 0.61f),
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

  val supportsTypingBox = remember(tier.badgeLevel) { SwarUserIdentity.isTypingBoxTier(tier.badgeLevel) }
  var typingBannerLink by remember(tier.badgeLevel, config.typingBannerUrl) {
    mutableStateOf(if (supportsTypingBox) config.typingBannerUrl else "")
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
  val previewTypingBannerModel = if (supportsTypingBox) {
    typingBannerUri?.toString()
      ?: typingBannerLink.trim().ifBlank { SwarUserIdentity.defaultTypingBannerForTier(tier.badgeLevel) }
  } else {
    ""
  }
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

            // Live Entry Video Auto-Playing Preview Box with Royal Golden Entry Banner over it
            if (previewVideoSource.isNotBlank()) {
              LevelEntryVideoPreviewBox(
                videoSource = previewVideoSource,
                entryLevel = tier.badgeLevel,
                senderName = sampleDisplayName,
                senderAvatarUrl = sampleAvatarUrl,
                frameUrl = previewFrameModel,
                isMuted = isEntryVideoMuted,
                onToggleMute = onToggleEntryVideoMuted,
              )
            }

            // Live Typing Patti ("टाइपिंग पट्टी") Chat Bubble Preview — exclusively for LV.30 / 60 / 90 / 120
            if (supportsTypingBox && previewTypingBannerModel.isNotBlank()) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "✨ ${SwarUserIdentity.typingBoxThemeTitle(previewTypingBannerModel)} — लाइव एनीमेशन प्रीव्यू:",
                  style = MaterialTheme.typography.labelSmall,
                  color = SwarGold,
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

        // Admin Configurator: Entry Video + Avatar Frame + Coins + Profile Badge (+ Typing Patti ONLY on 30/60/90/120)
        if (isAdminUser) {
          Text(
            text = if (supportsTypingBox) {
              "लेवल कार्ड सेटअप (सभी 5 विकल्प — गोल्डन टाइपिंग पट्टी सहित)"
            } else {
              "लेवल कार्ड सेटअप (टाइपिंग पट्टी केवल LV.30, 60, 90, 120 पर उपलब्ध)"
            },
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

          // Row 3: Typing Patti ("टाइपिंग पट्टी") Link + Upload — ONLY visible on LV.30, 60, 90, 120
          if (supportsTypingBox) {
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
                  label = { Text("टाइपिंग पट्टी (LV.${tier.badgeLevel} गोल्डन या कस्टम लिंक)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                  placeholder = { Text("खाली छोड़ें = बिल्ट-इन 24K गोल्डन पट्टी") },
                  leadingIcon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = SwarGold) },
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
                  Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = SwarGold, modifier = Modifier.size(16.dp))
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
                text = "बिल्ट-इन 24K गोल्डन एनीमेशन पहले से सक्रिय है — ऊपर टाइपिंग दिखेगी और नीचे चमकता एनीमेशन।",
                style = MaterialTheme.typography.labelSmall,
                color = SwarGold.copy(alpha = 0.85f),
              )
            }
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
                if (supportsTypingBox) typingBannerLink else "",
                if (supportsTypingBox) typingBannerUri else null,
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
  entryLevel: Int = 1,
  senderName: String = "",
  senderAvatarUrl: String = "",
  frameUrl: String = "",
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
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 14.dp),
      ) {
        Icon(Icons.Filled.PlayCircleFilled, contentDescription = null, tint = SwarGold, modifier = Modifier.size(22.dp))
        Text("एंट्री वीडियो लिंक सेट ✓ (रूम में प्रवेश पर प्ले होगा)", color = Color.White, style = MaterialTheme.typography.labelSmall)
      }
    }

    // Floating Royal Golden Entry Patti Preview right on top of the Entry Video
    RoyalGoldenEntryBanner(
      senderName = senderName.ifBlank { "Swar स्टार" },
      senderAvatarUrl = senderAvatarUrl,
      entryLevel = entryLevel,
      frameUrl = frameUrl,
      compact = true,
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .padding(top = 6.dp, start = 8.dp, end = 8.dp)
        .testTag("entry_video_preview_royal_banner"),
    )
  }
}
