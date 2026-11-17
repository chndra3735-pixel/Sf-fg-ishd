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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GiftCatalogItem
import com.example.data.model.SwarUserIdentity
import com.example.data.model.UserLevelInfo
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarDeepNight
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarViolet

@Composable
fun FamilyCrestEmblem(
  clubTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val label = clubTag.trim().ifBlank { "FAMILY" }.take(8).uppercase()
  Box(
    modifier = modifier
      .size(width = 72.dp, height = 60.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f

      // Left golden wings
      val leftWing = Path().apply {
        moveTo(cx - 12f, cy - 8f)
        lineTo(8f, cy - 28f)
        lineTo(14f, cy - 6f)
        lineTo(4f, cy + 4f)
        lineTo(18f, cy + 12f)
        lineTo(cx - 10f, cy + 16f)
        close()
      }
      drawPath(
        path = leftWing,
        brush = Brush.linearGradient(listOf(Color(0xFFFFE57F), Color(0xFFD68910))),
      )

      // Right golden wings
      val rightWing = Path().apply {
        moveTo(cx + 12f, cy - 8f)
        lineTo(size.width - 8f, cy - 28f)
        lineTo(size.width - 14f, cy - 6f)
        lineTo(size.width - 4f, cy + 4f)
        lineTo(size.width - 18f, cy + 12f)
        lineTo(cx + 10f, cy + 16f)
        close()
      }
      drawPath(
        path = rightWing,
        brush = Brush.linearGradient(listOf(Color(0xFFFFE57F), Color(0xFFD68910))),
      )

      // Top crown
      val crown = Path().apply {
        moveTo(cx - 18f, cy - 16f)
        lineTo(cx - 22f, cy - 36f)
        lineTo(cx - 9f, cy - 25f)
        lineTo(cx, cy - 42f)
        lineTo(cx + 9f, cy - 25f)
        lineTo(cx + 22f, cy - 36f)
        lineTo(cx + 18f, cy - 16f)
        close()
      }
      drawPath(
        path = crown,
        brush = Brush.verticalGradient(listOf(Color(0xFFFFF59D), Color(0xFFE09200))),
      )

      // Central ruby-amber shield
      val shield = Path().apply {
        moveTo(cx - 22f, cy - 16f)
        lineTo(cx + 22f, cy - 16f)
        lineTo(cx + 22f, cy + 6f)
        lineTo(cx, cy + 28f)
        lineTo(cx - 22f, cy + 6f)
        close()
      }
      drawPath(
        path = shield,
        brush = Brush.verticalGradient(listOf(Color(0xFFE89611), Color(0xFF8C3B00))),
      )
      drawPath(
        path = shield,
        color = Color(0xFFFFF0A6),
        style = Stroke(width = 3f, join = StrokeJoin.Round),
      )

      // Crossed golden sword highlights inside shield
      drawLine(
        color = Color(0xFFFFE57F),
        start = Offset(cx - 12f, cy - 8f),
        end = Offset(cx + 12f, cy + 14f),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
      )
      drawLine(
        color = Color(0xFFFFE57F),
        start = Offset(cx + 12f, cy - 8f),
        end = Offset(cx - 12f, cy + 14f),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
      )
    }

    // Bottom golden ribbon banner
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = Color.Transparent,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 2.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Brush.horizontalGradient(listOf(Color(0xFFFFC107), Color(0xFFFFF1A8), Color(0xFFD68910))))
        .border(1.dp, Color(0xFF7B3E00), RoundedCornerShape(8.dp)),
    ) {
      Text(
        text = label,
        fontSize = 8.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF422000),
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
      )
    }
  }
}

@Composable
fun WalletGradientCard(
  coins: Long,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .height(86.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(
        Brush.linearGradient(
          colors = listOf(Color(0xFFFF7D54), Color(0xFFFFAA58), Color(0xFFFF6D84))
        )
      )
      .clickable(onClick = onClick)
      .testTag("profile_wallet_card"),
  ) {
    // Subtle decorative geometric stars & custom 3D wallet artwork
    Canvas(modifier = Modifier.fillMaxSize()) {
      val starPositions = listOf(
        Offset(size.width * 0.25f, size.height * 0.22f) to 14f,
        Offset(size.width * 0.48f, size.height * 0.65f) to 18f,
        Offset(size.width * 0.54f, size.height * 0.18f) to 10f,
      )
      starPositions.forEach { (center, r) ->
        drawCircle(color = Color.White.copy(alpha = 0.12f), radius = r, center = center)
      }

      // Stylized 3D pastel wallet on right side
      val wx = size.width * 0.58f
      val wy = size.height * 0.25f
      val ww = size.width * 0.34f
      val wh = size.height * 0.60f

      // Peeking crisp cream note / card
      drawRoundRect(
        color = Color(0xFFFFF7E0),
        topLeft = Offset(wx + ww * 0.12f, wy - wh * 0.18f),
        size = Size(ww * 0.68f, wh * 0.45f),
        cornerRadius = CornerRadius(10f, 10f),
      )
      // Golden coin peeking out top-left of wallet
      drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFB300))),
        radius = wh * 0.22f,
        center = Offset(wx + ww * 0.24f, wy - wh * 0.04f),
      )
      // Main wallet body
      drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFC178), Color(0xFFFF5E8A))),
        topLeft = Offset(wx, wy),
        size = Size(ww, wh),
        cornerRadius = CornerRadius(18f, 18f),
      )
      // Wallet clasp flap
      drawRoundRect(
        color = Color(0xFFFFE082),
        topLeft = Offset(wx + ww * 0.62f, wy + wh * 0.30f),
        size = Size(ww * 0.42f, wh * 0.36f),
        cornerRadius = CornerRadius(12f, 12f),
      )
      drawCircle(
        color = Color(0xFFFF6D00),
        radius = 4.5f,
        center = Offset(wx + ww * 0.76f, wy + wh * 0.48f),
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "वॉलेट",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        CoinEmblem(sizeDp = 16)
        Text(
          text = coins.toString(),
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.testTag("profile_wallet_coins"),
        )
      }
    }
  }
}

@Composable
fun StoreGradientCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .height(86.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(
        Brush.linearGradient(
          colors = listOf(Color(0xFF63B4FF), Color(0xFF4798FF), Color(0xFF72D2FF))
        )
      )
      .clickable(onClick = onClick)
      .testTag("profile_store_card"),
  ) {
    // Decorative sparkles + stylized 3D boutique storefront on right side
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(color = Color.White.copy(alpha = 0.14f), radius = 16f, center = Offset(size.width * 0.30f, size.height * 0.25f))
      drawCircle(color = Color.White.copy(alpha = 0.12f), radius = 12f, center = Offset(size.width * 0.45f, size.height * 0.70f))

      val sx = size.width * 0.60f
      val sy = size.height * 0.22f
      val sw = size.width * 0.32f
      val sh = size.height * 0.64f

      // Store building body
      drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFB3E5FC), Color(0xFF4FC3F7))),
        topLeft = Offset(sx + 4f, sy + sh * 0.22f),
        size = Size(sw - 8f, sh * 0.78f),
        cornerRadius = CornerRadius(14f, 14f),
      )
      // Door arch inside storefront
      drawRoundRect(
        color = Color(0xFFE1F5FE),
        topLeft = Offset(sx + sw * 0.28f, sy + sh * 0.52f),
        size = Size(sw * 0.44f, sh * 0.48f),
        cornerRadius = CornerRadius(10f, 10f),
      )
      // Storefront striped awning
      drawRoundRect(
        brush = Brush.horizontalGradient(listOf(Color(0xFF2979FF), Color(0xFF80D8FF))),
        topLeft = Offset(sx - 4f, sy),
        size = Size(sw + 8f, sh * 0.34f),
        cornerRadius = CornerRadius(12f, 12f),
      )
      // Awning white stripes
      for (i in 0..2) {
        val stripeX = sx + 8f + i * (sw / 3f)
        drawRoundRect(
          color = Color.White.copy(alpha = 0.75f),
          topLeft = Offset(stripeX, sy + 2f),
          size = Size(sw * 0.14f, sh * 0.28f),
          cornerRadius = CornerRadius(4f, 4f),
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      contentAlignment = Alignment.CenterStart,
    ) {
      Text(
        text = "स्टोर",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun GoldenWingedLevelBadge(
  level: Int,
  modifier: Modifier = Modifier,
) {
  val tier = SwarUserIdentity.tierForLevel(level)
  OrnateWingedLevelCrest(
    tier = tier,
    displayText = "LV.$level",
    badgeWidth = 88.dp,
    badgeHeight = 30.dp,
    isCompact = true,
    modifier = modifier.testTag("profile_level_badge"),
  )
}

@Composable
fun ProfileMenuRow(
  title: String,
  icon: ImageVector,
  iconTint: Color,
  iconBgColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  showNewBadge: Boolean = false,
  trailingText: String? = null,
  trailingContent: (@Composable () -> Unit)? = null,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(54.dp)
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(30.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(iconBgColor),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(18.dp),
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Text(
      text = title,
      style = MaterialTheme.typography.bodyLarge,
      color = Color(0xFF1E2230),
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f, fill = false),
    )

    if (showNewBadge) {
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        color = Color(0xFFE84C3D),
        shape = RoundedCornerShape(10.dp),
      ) {
        Text(
          text = "new",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    if (trailingContent != null) {
      trailingContent()
      Spacer(modifier = Modifier.width(6.dp))
    } else if (!trailingText.isNullOrBlank()) {
      Text(
        text = trailingText,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF8C93A6),
      )
      Spacer(modifier = Modifier.width(4.dp))
    }

    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = Color(0xFFB3B9C9),
      modifier = Modifier.size(20.dp),
    )
  }
}

@Composable
fun WalletOverviewDialog(
  coins: Long,
  levelInfo: UserLevelInfo,
  canClaimBonus: Boolean,
  onClaimBonusCoins: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("वॉलेट और कॉइंस") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFFFFF4E5),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            CoinEmblem(sizeDp = 32)
            Column {
              Text("उपलब्ध बैलेंस", style = MaterialTheme.typography.labelMedium, color = Color(0xFF7D5421))
              Text(
                text = "$coins कॉइंस",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF422100),
                fontWeight = FontWeight.ExtraBold,
              )
            }
          }
        }
        Text(
          text = "वर्तमान स्तर: Lv.${levelInfo.level} (${levelInfo.title})",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
        )
        LinearProgressIndicator(
          progress = { levelInfo.progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = SwarGold,
          trackColor = Color(0xFFE8ECF4),
        )
        Text(
          text = if (levelInfo.level >= 120) "आप अधिकतम स्तर पर पहुँच चुके हैं!"
          else "अगले स्तर के लिए ${levelInfo.nextLevelRemainingCoins} कॉइंस और कमाएं।",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {
      if (canClaimBonus) {
        Button(
          onClick = {
            onClaimBonusCoins()
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A59), contentColor = Color.White),
        ) {
          Text("मुफ़्त +500 कॉइंस लें")
        }
      } else {
        TextButton(onClick = onDismiss) { Text("ठीक है") }
      }
    },
    dismissButton = {
      if (canClaimBonus) {
        TextButton(onClick = onDismiss) { Text("बंद करें") }
      }
    },
  )
}

@Composable
fun SwarStoreDialog(
  coins: Long,
  gifts: List<GiftCatalogItem>,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("SwarRoom स्टोर") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          CoinEmblem(sizeDp = 18)
          Text("आपका बैलेंस: $coins कॉइंस", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Text(
          text = "रूम में गिफ्ट पैनल खोलकर इन शाही गिफ्ट्स को लाइव भेजें:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyVerticalGrid(
          columns = GridCells.Fixed(3),
          modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
          contentPadding = PaddingValues(2.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(gifts.take(12), key = { it.id.ifBlank { it.name } }) { gift ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = SwarDeepNight,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                if (gift.thumbnailUrl.isNotBlank()) {
                  AsyncImage(
                    model = gift.thumbnailUrl,
                    contentDescription = gift.name,
                    modifier = Modifier
                      .size(36.dp)
                      .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                  )
                } else {
                  Text(gift.emoji.ifBlank { "🎁" }, fontSize = 24.sp)
                }
                Text(
                  text = gift.name,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "🪙 ${gift.coinCost}",
                  fontSize = 10.sp,
                  color = SwarGold,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("ठीक है") }
    },
  )
}

@Composable
fun LevelDetailsDialog(
  levelInfo: UserLevelInfo,
  coins: Long,
  onDismiss: () -> Unit,
) {
  LevelCardsShowcaseDialog(
    levelInfo = levelInfo,
    coins = coins,
    onDismiss = onDismiss,
  )
}

@Composable
fun ProfileFeatureInfoDialog(
  title: String,
  description: String,
  highlightBadge: String,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = SwarViolet)
        Text(title)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = SwarCyan.copy(alpha = 0.14f),
        ) {
          Text(
            text = highlightBadge,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF0B4F6C),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("ठीक है") }
    },
  )
}

@Composable
fun LanguageSelectionDialog(
  currentLanguage: String,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Language, contentDescription = null, tint = SwarCoral)
        Text("भाषा चुनें")
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("हिंदी (डिफ़ॉल्ट)", "English (Hindi UI Active)").forEach { option ->
          val selected = currentLanguage == option
          Surface(
            onClick = { onSelect(option) },
            shape = RoundedCornerShape(12.dp),
            color = if (selected) Color(0xFFE7F5FF) else Color(0xFFF6F7FA),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = if (selected) "✓ $option" else option,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
              color = Color(0xFF1E2230),
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("बंद करें") }
    },
  )
}
