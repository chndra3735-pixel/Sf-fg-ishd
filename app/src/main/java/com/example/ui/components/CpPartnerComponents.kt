package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CpLevelInfo
import com.example.data.model.CpRelationship
import com.example.data.model.SwarUserIdentity
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarMint
import com.example.ui.theme.SwarViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val BOND_TITLE_OPTIONS = listOf(
  "बेस्ट फ्रेंड",
  "कपल पार्टनर",
  "सोलमेट",
  "सीपी जोड़ी",
)

/**
 * Displays BOTH joined partners together (DP + Name + 10-digit ID + CP Gift Level + Days together).
 * Used whenever anyone touches either partner's ID/seat/profile or opens the CP Hub.
 */
@Composable
fun JointCpProfileCard(
  cp: CpRelationship,
  focusUid: String,
  modifier: Modifier = Modifier,
  darkSurface: Boolean = true,
  onPartnerClick: ((uid: String, name: String, avatarUrl: String) -> Unit)? = null,
) {
  val leftUid = focusUid.takeIf { it.isNotBlank() && it in cp.memberUids }
    ?: cp.memberUids.firstOrNull().orEmpty()
  val rightUid = cp.partnerUidFor(leftUid).ifBlank {
    cp.memberUids.lastOrNull().orEmpty()
  }
  val leftName = cp.memberNames[leftUid].orEmpty().ifBlank { "पार्टनर 1" }
  val rightName = cp.memberNames[rightUid].orEmpty().ifBlank { "पार्टनर 2" }
  val leftAvatar = cp.memberAvatars[leftUid].orEmpty()
  val rightAvatar = cp.memberAvatars[rightUid].orEmpty()
  val leftNumericId = cp.numericIdForMember(leftUid)
  val rightNumericId = cp.numericIdForMember(rightUid)
  val info = remember(cp.cpXp, cp.acceptedAtMs) {
    SwarUserIdentity.cpLevelInfoFor(cp.cpXp, cp.acceptedAtMs)
  }

  val infinite = rememberInfiniteTransition(label = "jointCpAura")
  val phase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
    label = "phase",
  )

  val cardBg = if (darkSurface) Color(0xFF22133F) else Color(0xFFFFF4F9)
  val titleColor = if (darkSurface) Color.White else Color(0xFF1F1630)
  val subColor = if (darkSurface) Color(0xFFD5CCE8) else Color(0xFF5D4C74)

  Surface(
    color = cardBg,
    shape = RoundedCornerShape(18.dp),
    shadowElevation = if (darkSurface) 0.dp else 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(
        width = 1.5.dp,
        brush = Brush.linearGradient(listOf(info.primaryColor, info.secondaryColor)),
        shape = RoundedCornerShape(18.dp),
      ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Top CP Header: Bond Title + Level + Age
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = info.primaryColor,
            modifier = Modifier
              .size(16.dp)
              .graphicsLayer {
                val s = 1f + 0.14f * sin(phase * 2f * PI.toFloat())
                scaleX = s
                scaleY = s
              },
          )
          Text(
            text = "CP जोड़ी • ${cp.bondTitle}",
            style = MaterialTheme.typography.labelLarge,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Surface(
          color = info.primaryColor.copy(alpha = if (darkSurface) 0.25f else 0.18f),
          shape = RoundedCornerShape(10.dp),
        ) {
          Text(
            text = "CP Lv.${info.level}",
            style = MaterialTheme.typography.labelMedium,
            color = if (darkSurface) info.primaryColor else Color(0xFFAD1457),
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }

      // Twin Avatars with Animated Energy Link & Both 10-Digit IDs Side-by-Side
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        ) {
          val yMid = size.height * 0.42f
          val xStart = size.width * 0.26f
          val xEnd = size.width * 0.74f
          val waveSpan = (xEnd - xStart).coerceAtLeast(10f)
          val path = Path()
          val steps = 28
          for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = xStart + waveSpan * t
            val amp = (6f + info.auraTier * 2f) * sin(t * PI.toFloat())
            val y = yMid + sin((t * 3f - phase * 2f) * PI.toFloat()) * amp
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
          }
          drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(info.primaryColor, info.secondaryColor, info.primaryColor)),
            style = Stroke(width = 4f, cap = StrokeCap.Round),
          )
          val sparkleX = xStart + waveSpan * phase
          drawCircle(
            color = Color.White,
            radius = 5f,
            center = Offset(sparkleX, yMid),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          // Partner 1 (Tapped User)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable(enabled = onPartnerClick != null) {
                onPartnerClick?.invoke(leftUid, leftName, leftAvatar)
              }
              .padding(4.dp),
          ) {
            UserAvatar(
              url = leftAvatar,
              name = leftName,
              size = 50.dp,
              borderColor = info.primaryColor,
              borderWidth = 2.dp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = leftName,
              style = MaterialTheme.typography.labelMedium,
              color = titleColor,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "ID: $leftNumericId",
              style = MaterialTheme.typography.labelSmall,
              color = subColor,
              fontSize = 10.sp,
            )
          }

          // Center Bond Crest
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = info.primaryColor.copy(alpha = 0.22f),
              modifier = Modifier
                .size(34.dp)
                .border(1.dp, info.secondaryColor.copy(alpha = 0.7f), CircleShape),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.Link,
                  contentDescription = "जुड़ी ID",
                  tint = if (darkSurface) Color.White else Color(0xFFC2185B),
                  modifier = Modifier.size(18.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = info.title,
              style = MaterialTheme.typography.labelSmall,
              color = if (darkSurface) info.secondaryColor else Color(0xFF7B1FA2),
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
              textAlign = TextAlign.Center,
              maxLines = 1,
            )
          }

          // Partner 2 (Linked CP Partner)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable(enabled = onPartnerClick != null) {
                onPartnerClick?.invoke(rightUid, rightName, rightAvatar)
              }
              .padding(4.dp)
              .testTag("joint_cp_partner_chip"),
          ) {
            UserAvatar(
              url = rightAvatar,
              name = rightName,
              size = 50.dp,
              borderColor = info.secondaryColor,
              borderWidth = 2.dp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = rightName,
              style = MaterialTheme.typography.labelMedium,
              color = titleColor,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "ID: $rightNumericId",
              style = MaterialTheme.typography.labelSmall,
              color = subColor,
              fontSize = 10.sp,
            )
          }
        }
      }

      // CP Level Progress + Gift Count + Bond Longevity
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
          progress = { info.progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = info.primaryColor,
          trackColor = if (darkSurface) Color(0x33FFFFFF) else Color(0xFFEBD9E8),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "🎁 ${cp.totalGiftsCount} गिफ्ट • XP ${cp.cpXp}",
            style = MaterialTheme.typography.labelSmall,
            color = subColor,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "⏳ ${info.ageLabel} • ${info.auraLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = if (darkSurface) info.secondaryColor else Color(0xFF6A1B9A),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

/**
 * Draw-phase smooth animated bridge connecting two adjacent seats ("बाजू बाजू में") when paired CP friends
 * sit next to each other. Visual richness scales with BOTH CP Gift Level and Bond Age (Aura Tier 1..5).
 */
@Composable
fun AdjacentCpSeatBridgeCanvas(
  info: CpLevelInfo,
  startCenterFractionX: Float,
  endCenterFractionX: Float,
  modifier: Modifier = Modifier,
) {
  val infinite = rememberInfiniteTransition(label = "adjacentSeatAura")
  val phase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
    label = "wavePhase",
  )

  Canvas(modifier = modifier) {
    val startX = size.width * startCenterFractionX
    val endX = size.width * endCenterFractionX
    val span = (endX - startX).coerceAtLeast(1f)
    val yCenter = size.height * 0.44f

    // Seat halos around left & right occupied seats (brighter & multi-ring as bond gets older/higher)
    val haloAlpha = 0.20f + 0.08f * sin(phase * 2f * PI.toFloat())
    val baseRadius = (size.height * 0.36f).coerceAtLeast(18f)
    listOf(startX, endX).forEachIndexed { idx, cx ->
      val haloColor = if (idx == 0) info.primaryColor else info.secondaryColor
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(haloColor.copy(alpha = haloAlpha + info.auraTier * 0.04f), Color.Transparent),
          center = Offset(cx, yCenter),
          radius = baseRadius * (1.25f + info.auraTier * 0.08f),
        ),
        radius = baseRadius * (1.25f + info.auraTier * 0.08f),
        center = Offset(cx, yCenter),
      )
      if (info.auraTier >= 3) {
        drawCircle(
          color = haloColor.copy(alpha = 0.45f),
          radius = baseRadius * (0.95f + 0.07f * sin((phase + idx * 0.5f) * 2f * PI.toFloat())),
          center = Offset(cx, yCenter),
          style = Stroke(width = 2.2f),
        )
      }
    }

    // Primary smooth connecting silk ribbon wave
    val waveCount = when {
      info.auraTier >= 5 -> 3
      info.auraTier >= 3 -> 2
      else -> 1
    }
    for (w in 0 until waveCount) {
      val path = Path()
      val steps = 32
      val phaseOffset = w * 0.33f
      val ampScale = 7f + info.auraTier * 2.5f
      for (i in 0..steps) {
        val t = i.toFloat() / steps
        val x = startX + span * t
        val env = sin(t * PI.toFloat())
        val angle = (t * (2.5f + w * 0.5f) - (phase + phaseOffset) * 2f) * PI.toFloat()
        val y = yCenter + sin(angle) * ampScale * env
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
      }
      val strokeColor = if (w % 2 == 0) {
        Brush.horizontalGradient(listOf(info.primaryColor, info.secondaryColor, info.primaryColor))
      } else {
        Brush.horizontalGradient(listOf(info.secondaryColor, Color.White, info.primaryColor))
      }
      drawPath(
        path = path,
        brush = strokeColor,
        style = Stroke(width = if (w == 0) 4.2f else 2.5f, cap = StrokeCap.Round),
      )
    }

    // Orbiting particles & comets scaling with CP Level + Longevity
    val particleCount = (info.auraTier * 2).coerceIn(2, 10)
    for (p in 0 until particleCount) {
      val t = ((phase + p.toFloat() / particleCount) % 1f)
      val x = startX + span * t
      val env = sin(t * PI.toFloat())
      val y = yCenter + cos((t * 3f - phase * 2f) * PI.toFloat()) * (8f + info.auraTier * 2f) * env
      val particleRadius = if (info.auraTier >= 4 && p % 2 == 0) 4.5f else 3f
      drawCircle(
        color = if (p % 2 == 0) Color.White else info.secondaryColor,
        radius = particleRadius * (0.65f + 0.5f * env),
        center = Offset(x, y),
      )
    }

    // Tier 5 Legendary Constellation Sparkles
    if (info.auraTier >= 5) {
      val midX = (startX + endX) * 0.5f
      val starR = 8f + 3f * sin(phase * 2f * PI.toFloat())
      drawLine(
        color = SwarGold.copy(alpha = 0.85f),
        start = Offset(midX - starR, yCenter),
        end = Offset(midX + starR, yCenter),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round,
      )
      drawLine(
        color = SwarCyan.copy(alpha = 0.85f),
        start = Offset(midX, yCenter - starR),
        end = Offset(midX, yCenter + starR),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round,
      )
    }
  }
}

@Composable
fun AdjacentCpFloatingCrest(
  info: CpLevelInfo,
  tag: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color(0xDD1A0D33),
    shape = RoundedCornerShape(10.dp),
    modifier = modifier
      .border(1.dp, Brush.horizontalGradient(listOf(info.primaryColor, info.secondaryColor)), RoundedCornerShape(10.dp))
      .testTag(tag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.Favorite,
        contentDescription = "CP साथी",
        tint = info.primaryColor,
        modifier = Modifier.size(10.dp),
      )
      Text(
        text = if (info.ageDays > 0L) "Lv.${info.level}•${info.ageDays}d" else "CP Lv.${info.level}",
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White,
        maxLines = 1,
      )
    }
  }
}

/**
 * Full interactive CP Partner Management Hub Dialog opened from ProfileScreen ("CP स्तर") or VoiceRoom.
 */
@Composable
fun CpPartnerDialog(
  currentUserId: String,
  currentDisplayName: String,
  currentAvatarUrl: String,
  activeCpLink: CpRelationship?,
  incomingRequests: List<CpRelationship>,
  outgoingRequests: List<CpRelationship>,
  isSubmitting: Boolean,
  onGenerateSyncOwnId: () -> Unit,
  onSendRequestById: (recipientNumericId: String, bondTitle: String) -> Unit,
  onAcceptRequest: (CpRelationship) -> Unit,
  onDeclineOrBreak: (CpRelationship) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val ownNumericId = remember(currentUserId) { SwarUserIdentity.numericIdForUid(currentUserId) }
  var partnerIdInput by remember { mutableStateOf("") }
  var selectedBondTitle by remember { mutableStateOf(BOND_TITLE_OPTIONS.first()) }
  var confirmBreakPair by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color.White,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 20.dp)
        .widthIn(max = 460.dp)
        .testTag("cp_partner_dialog"),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
      ) {
        // Gradient Header Banner
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFF8E24AA), Color(0xFF4A148C)))
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = CircleShape,
              color = Color(0x33FFFFFF),
              modifier = Modifier.size(44.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.Favorite,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(24.dp),
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "सीपी (CP) • कपल व दोस्त जोड़ें",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text = "ID नंबर से जुड़ें • गिफ्ट व समय के साथ लेवल बढ़ाएं",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFE1F0),
              )
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("close_cp_dialog_button"),
            ) {
              Icon(Icons.Filled.Close, contentDescription = "बंद करें", tint = Color.White)
            }
          }
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // 1. Own Numeric ID Card with Generate/Sync & Copy buttons
          Surface(
            color = Color(0xFFF6F4FF),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, Color(0xFFDAD1FA), RoundedCornerShape(16.dp)),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              UserAvatar(
                url = currentAvatarUrl,
                name = currentDisplayName,
                size = 44.dp,
                borderColor = SwarViolet,
                borderWidth = 1.5.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = currentDisplayName.ifBlank { "आप" },
                  style = MaterialTheme.typography.labelLarge,
                  color = Color(0xFF1F1630),
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "आपका ID: $ownNumericId",
                  style = MaterialTheme.typography.bodySmall,
                  color = SwarViolet,
                  fontWeight = FontWeight.ExtraBold,
                  modifier = Modifier.testTag("cp_own_numeric_id"),
                )
              }
              IconButton(
                onClick = {
                  val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                  cm?.setPrimaryClip(ClipData.newPlainText("SwarRoom CP ID", ownNumericId))
                  Toast.makeText(context, "ID कॉपी हो गई: $ownNumericId", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "ID कॉपी करें", tint = SwarViolet, modifier = Modifier.size(18.dp))
              }
              IconButton(
                onClick = onGenerateSyncOwnId,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("cp_generate_sync_id_button"),
              ) {
                Icon(Icons.Filled.Refresh, contentDescription = "ID जनरेट और सिंक करें", tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
              }
            }
          }

          // 2. Incoming CP Request Notifications (Agree / Decline)
          if (incomingRequests.isNotEmpty()) {
            incomingRequests.forEach { req ->
              val senderName = req.memberNames[req.requesterUid].orEmpty().ifBlank { "दोस्त" }
              val senderAvatar = req.memberAvatars[req.requesterUid].orEmpty()
              val senderNumId = req.numericIdForMember(req.requesterUid)
              Surface(
                color = Color(0xFFFFF5F8),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.5.dp, Color(0xFFFF4081), RoundedCornerShape(16.dp))
                  .testTag("cp_incoming_request_card_${req.id}"),
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "नया CP पार्टनर अनुरोध मिला!",
                      style = MaterialTheme.typography.labelLarge,
                      color = Color(0xFFC2185B),
                      fontWeight = FontWeight.ExtraBold,
                    )
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                      url = senderAvatar,
                      name = senderName,
                      size = 44.dp,
                      borderColor = Color(0xFFE91E63),
                      borderWidth = 1.5.dp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = senderName,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF1F1630),
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text = "ID: $senderNumId • ${req.bondTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B5A7E),
                      )
                    }
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    OutlinedButton(
                      onClick = { onDeclineOrBreak(req) },
                      enabled = !isSubmitting,
                      modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("cp_decline_request_button"),
                    ) {
                      Text("अस्वीकार", color = SwarCoral, fontWeight = FontWeight.Bold)
                    }
                    Button(
                      onClick = { onAcceptRequest(req) },
                      enabled = !isSubmitting,
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63), contentColor = Color.White),
                      modifier = Modifier
                        .weight(1.3f)
                        .minimumInteractiveComponentSize()
                        .testTag("cp_accept_request_button"),
                    ) {
                      Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("हाँ, एग्री है (जोड़ें)", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                  }
                }
              }
            }
          }

          // 3. Joined Active CP Pair View OR Submit Partner ID Form
          if (activeCpLink != null) {
            JointCpProfileCard(
              cp = activeCpLink,
              focusUid = currentUserId,
              darkSurface = false,
              modifier = Modifier.testTag("cp_dialog_active_pair_card"),
            )

            Surface(
              color = Color(0xFFF3FAFF),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFCCE7FF), RoundedCornerShape(14.dp)),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(20.dp))
                Text(
                  text = "वॉइस रूम में आप दोनों जब बाजू-बाजू सीट पर बैठेंगे, तब खूबसूरत एनीमेशन दिखेगा। एक-दूसरे को 'लकी 💞 CP' श्रेणी के गिफ्ट भेजने और साथ जुड़ने के दिनों के साथ आपका CP लेवल व एनीमेशन और शानदार होता जाएगा!",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF28415A),
                )
              }
            }

            if (confirmBreakPair) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = { confirmBreakPair = false },
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                ) {
                  Text("रहने दें")
                }
                Button(
                  onClick = {
                    confirmBreakPair = false
                    onDeclineOrBreak(activeCpLink)
                  },
                  enabled = !isSubmitting,
                  colors = ButtonDefaults.buttonColors(containerColor = SwarCoral, contentColor = Color.White),
                  modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("cp_confirm_break_button"),
                ) {
                  Text("हाँ, अलग करें", fontWeight = FontWeight.Bold)
                }
              }
            } else {
              TextButton(
                onClick = { confirmBreakPair = true },
                modifier = Modifier
                  .align(Alignment.CenterHorizontally)
                  .testTag("cp_break_link_button"),
              ) {
                Text("CP जोड़ी अलग करें", color = SwarCoral, fontWeight = FontWeight.SemiBold)
              }
            }
          } else {
            // Outgoing pending requests if any
            if (outgoingRequests.isNotEmpty()) {
              outgoingRequests.forEach { outReq ->
                val targetName = outReq.partnerNameFor(currentUserId)
                val targetNumId = outReq.partnerNumericIdFor(currentUserId)
                Surface(
                  color = Color(0xFFFFFBF0),
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SwarGold, RoundedCornerShape(14.dp)),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    UserAvatar(
                      url = outReq.partnerAvatarFor(currentUserId),
                      name = targetName,
                      size = 40.dp,
                      borderColor = SwarGold,
                      borderWidth = 1.5.dp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "$targetName (प्रतीक्षारत)",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF422C00),
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text = "ID: $targetNumId • ${outReq.bondTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7A5B14),
                      )
                    }
                    TextButton(
                      onClick = { onDeclineOrBreak(outReq) },
                      enabled = !isSubmitting,
                    ) {
                      Text("रद्द करें", color = SwarCoral, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
              HorizontalDivider(color = Color(0xFFECEAF4))
            }

            // Submit Partner 10-Digit ID Form
            Text(
              text = "दूसरे का 10-अंकीय ID नंबर डालें और सबमिट करें:",
              style = MaterialTheme.typography.labelLarge,
              color = Color(0xFF281D40),
              fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
              value = partnerIdInput,
              onValueChange = { raw ->
                partnerIdInput = raw.filter { it.isDigit() }.take(10)
              },
              label = { Text("पार्टनर / दोस्त का 10-अंकीय ID नंबर") },
              placeholder = { Text("जैसे: 7116195725") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF171A23),
                unfocusedTextColor = Color(0xFF171A23),
                focusedBorderColor = Color(0xFFE91E63),
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("cp_partner_id_input"),
            )

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              BOND_TITLE_OPTIONS.forEach { option ->
                val selected = selectedBondTitle == option
                FilterChip(
                  selected = selected,
                  onClick = { selectedBondTitle = option },
                  label = { Text(option, fontSize = 12.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFE4EE),
                    selectedLabelColor = Color(0xFFC2185B),
                  ),
                )
              }
            }

            Button(
              onClick = {
                onSendRequestById(partnerIdInput, selectedBondTitle)
                partnerIdInput = ""
              },
              enabled = partnerIdInput.length == 10 && !isSubmitting,
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE91E63),
                contentColor = Color.White,
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("cp_submit_request_button"),
            ) {
              if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
              } else {
                Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("सबमिट करें (CP अनुरोध भेजें)", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Real-Time Incoming CP Notification Dialog shown anywhere in the app when another user submits your ID.
 */
@Composable
fun IncomingCpNotificationDialog(
  request: CpRelationship,
  isSubmitting: Boolean,
  onAgree: () -> Unit,
  onDecline: () -> Unit,
  onLater: () -> Unit,
) {
  val senderName = request.memberNames[request.requesterUid].orEmpty().ifBlank { "Swar दोस्त" }
  val senderAvatar = request.memberAvatars[request.requesterUid].orEmpty()
  val senderId = request.numericIdForMember(request.requesterUid)

  Dialog(
    onDismissRequest = onLater,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color(0xFF1D1036),
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 22.dp)
        .widthIn(max = 400.dp)
        .border(
          width = 2.dp,
          brush = Brush.linearGradient(listOf(Color(0xFFFF4081), SwarGold, SwarCyan)),
          shape = RoundedCornerShape(24.dp),
        )
        .testTag("incoming_cp_notification_dialog"),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          color = Color(0x33FF4081),
          shape = RoundedCornerShape(14.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = SwarGold, modifier = Modifier.size(16.dp))
            Text(
              text = "CP जोड़ने का निमंत्रण!",
              style = MaterialTheme.typography.labelLarge,
              color = SwarGold,
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }

        FramedUserAvatar(
          url = senderAvatar,
          name = senderName,
          avatarSize = 74.dp,
          borderColor = Color(0xFFFF4081),
          borderWidth = 2.5.dp,
        )

        Text(
          text = senderName,
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
          fontWeight = FontWeight.ExtraBold,
        )
        Text(
          text = "ID: $senderId • रिश्ता: ${request.bondTitle}",
          style = MaterialTheme.typography.labelMedium,
          color = SwarCyan,
          fontWeight = FontWeight.SemiBold,
        )

        Text(
          text = "$senderName ने आपके साथ CP जोड़ी बनाने का अनुरोध भेजा है। यदि आप 'हाँ, एग्री है' करेंगे तो दोनों ID एक साथ जुड़ जाएंगी!",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFE0D9F4),
          textAlign = TextAlign.Center,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onDecline,
            enabled = !isSubmitting,
            modifier = Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("cp_popup_decline_button"),
          ) {
            Text("अस्वीकार", color = SwarCoral, fontWeight = FontWeight.Bold)
          }
          Button(
            onClick = onAgree,
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFE91E63),
              contentColor = Color.White,
            ),
            modifier = Modifier
              .weight(1.4f)
              .minimumInteractiveComponentSize()
              .testTag("cp_popup_agree_button"),
          ) {
            if (isSubmitting) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
              Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("हाँ, एग्री है", fontWeight = FontWeight.Bold)
            }
          }
        }

        TextButton(
          onClick = onLater,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text("बाद में देखें", color = Color(0xFFBDB3DC))
        }
      }
    }
  }
}
