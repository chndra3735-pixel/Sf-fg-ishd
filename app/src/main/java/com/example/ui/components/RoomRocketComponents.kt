package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.RocketLaunchEvent
import com.example.data.model.RoomRocketState
import com.example.data.model.SwarUserIdentity
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarMint
import com.example.ui.theme.SwarViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RoomRocketCompactWidget(
  rocketState: RoomRocketState?,
  onOpenDetails: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val rocketLevel = (rocketState?.rocketLevel ?: 1).coerceIn(1, 5)
  val spec = remember(rocketLevel) { SwarUserIdentity.rocketTierSpec(rocketLevel) }
  val targetCoins = (rocketState?.targetCoins ?: spec.targetCoins).coerceAtLeast(1L)
  val currentCoins = (rocketState?.currentCoins ?: 0L).coerceIn(0L, targetCoins)
  val progress = (currentCoins.toFloat() / targetCoins.toFloat()).coerceIn(0f, 1f)
  val percentInt = (progress * 100f).roundToInt().coerceIn(0, 100)
  val top3 = remember(rocketState) {
    SwarUserIdentity.rankedRocketContributors(rocketState).take(3)
  }

  val pulseTransition = rememberInfiniteTransition(label = "rocket_widget_pulse")
  val rocketFloat by pulseTransition.animateFloat(
    initialValue = -2f,
    targetValue = 2.5f,
    animationSpec = infiniteRepeatable(
      animation = tween(1100, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "rocket_float",
  )
  val glowAlpha by pulseTransition.animateFloat(
    initialValue = 0.45f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(
      animation = tween(950, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "rocket_glow",
  )

  Surface(
    onClick = onOpenDetails,
    color = Color(0xAA13092C),
    shape = RoundedCornerShape(16.dp),
    shadowElevation = 6.dp,
    modifier = modifier
      .width(108.dp)
      .border(
        width = 1.2.dp,
        brush = Brush.verticalGradient(
          listOf(spec.primaryColor.copy(alpha = glowAlpha), SwarGold.copy(alpha = 0.7f))
        ),
        shape = RoundedCornerShape(16.dp),
      )
      .testTag("room_rocket_widget"),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      // Top Rocket Icon + LV Badge
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .offset(y = rocketFloat.dp)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                listOf(spec.flameColor.copy(alpha = 0.35f), spec.primaryColor.copy(alpha = 0.16f))
              )
            )
            .border(1.dp, spec.primaryColor.copy(alpha = 0.8f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.RocketLaunch,
            contentDescription = "रॉकेट स्तर $rocketLevel",
            tint = spec.primaryColor,
            modifier = Modifier.size(16.dp),
          )
        }
        Column {
          Text(
            text = "LV.$rocketLevel",
            style = MaterialTheme.typography.labelSmall,
            color = SwarGold,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            modifier = Modifier.testTag("room_rocket_level_text"),
          )
          Text(
            text = "$percentInt%",
            style = MaterialTheme.typography.labelSmall,
            color = spec.primaryColor,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
          )
        }
      }

      // Charge Progress Bar
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = spec.primaryColor,
        trackColor = Color(0x38FFFFFF),
      )

      Text(
        text = "${SwarUserIdentity.formatIndianNumber(currentCoins)}/${formatCompactCoins(targetCoins)}",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFFE6E0FF),
        fontSize = 8.5.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      // Top 3 Contributors' Circular DPs Displayed Directly Underneath the Rocket
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 1.dp)
          .testTag("room_rocket_top3_dps"),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        for (rankIdx in 0 until 3) {
          val contributor = top3.getOrNull(rankIdx)
          val rankColor = when (rankIdx) {
            0 -> SwarGold
            1 -> Color(0xFFE0E6F8)
            else -> Color(0xFFFFA06B)
          }
          Box(
            modifier = Modifier
              .size(26.dp)
              .testTag("rocket_top3_dp_slot_${rankIdx + 1}"),
            contentAlignment = Alignment.Center,
          ) {
            if (contributor != null) {
              UserAvatar(
                url = contributor.avatarUrl,
                name = contributor.displayName,
                size = 24.dp,
                borderColor = rankColor,
                borderWidth = 1.4.dp,
              )
            } else {
              Box(
                modifier = Modifier
                  .size(23.dp)
                  .clip(CircleShape)
                  .background(Color(0x28FFFFFF))
                  .border(1.dp, rankColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "${rankIdx + 1}",
                  color = rankColor.copy(alpha = 0.85f),
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
            // Mini Rank Badge Pill
            Box(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 3.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(rankColor)
                .padding(horizontal = 3.dp, vertical = 0.5.dp),
            ) {
              Text(
                text = "#${rankIdx + 1}",
                color = Color(0xFF190B2E),
                fontSize = 6.5.sp,
                fontWeight = FontWeight.ExtraBold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun RocketDetailsDialog(
  rocketState: RoomRocketState?,
  onDismiss: () -> Unit,
) {
  val rocketLevel = (rocketState?.rocketLevel ?: 1).coerceIn(1, 5)
  val activeSpec = remember(rocketLevel) { SwarUserIdentity.rocketTierSpec(rocketLevel) }
  val targetCoins = (rocketState?.targetCoins ?: activeSpec.targetCoins).coerceAtLeast(1L)
  val currentCoins = (rocketState?.currentCoins ?: 0L).coerceIn(0L, targetCoins)
  val remainingCoins = (targetCoins - currentCoins).coerceAtLeast(0L)
  val progress = (currentCoins.toFloat() / targetCoins.toFloat()).coerceIn(0f, 1f)
  val contributors = remember(rocketState) { SwarUserIdentity.rankedRocketContributors(rocketState) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      color = Color(0xFF140B29),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 24.dp)
        .heightIn(max = 650.dp)
        .border(1.5.dp, activeSpec.primaryColor.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
        .testTag("rocket_details_dialog"),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(activeSpec.primaryColor.copy(alpha = 0.18f))
              .border(1.5.dp, activeSpec.primaryColor, CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.RocketLaunch,
              contentDescription = null,
              tint = activeSpec.primaryColor,
              modifier = Modifier.size(24.dp),
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = activeSpec.titleHindi,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text = "लॉन्च इनाम: ${SwarUserIdentity.formatIndianNumber(activeSpec.rewardCoins)} कॉइंस + ${SwarUserIdentity.formatIndianNumber(activeSpec.rewardHp)} HP",
              style = MaterialTheme.typography.labelSmall,
              color = SwarGold,
              fontWeight = FontWeight.SemiBold,
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("close_rocket_details_button"),
          ) {
            Icon(Icons.Filled.Close, contentDescription = "बंद करें", tint = Color.White)
          }
        }

        // Current Charge Box
        Surface(
          color = Color(0xFF1F1240),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, activeSpec.primaryColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "रॉकेट चार्ज प्रोग्रेस",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${(progress * 100f).roundToInt()}%",
                style = MaterialTheme.typography.titleSmall,
                color = activeSpec.primaryColor,
                fontWeight = FontWeight.ExtraBold,
              )
            }
            LinearProgressIndicator(
              progress = { progress },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(6.dp)),
              color = activeSpec.primaryColor,
              trackColor = Color(0x30FFFFFF),
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "जमा: ${SwarUserIdentity.formatIndianNumber(currentCoins)} 🪙",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD6CEFA),
              )
              Text(
                text = "बाकी: ${SwarUserIdentity.formatIndianNumber(remainingCoins)} 🪙",
                style = MaterialTheme.typography.labelSmall,
                color = SwarGold,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        // Top-3 Partners Rule Notice
        Surface(
          color = Color(0x28FFB800),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SwarGold.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.EmojiEvents,
              contentDescription = null,
              tint = SwarGold,
              modifier = Modifier.size(22.dp),
            )
            Text(
              text = "टॉप 3 भागीदार नियम: केवल सबसे ज़्यादा गिफ्ट भेजने वाले #1, #2 और #3 पार्टनर को ही बराबर कॉइंस + HP इनाम मिलेगा (#1 का DP रॉकेट पर उड़कर ब्लास्ट होगा)। #4 से आगे कोई इनाम नहीं।",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFFFF2C2),
            )
          }
        }

        // Live Contributors Leaderboard
        Text(
          text = "इस राउंड के भागीदार (${contributors.size})",
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )

        if (contributors.isEmpty()) {
          Surface(
            color = Color(0x18FFFFFF),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "अभी किसी ने गिफ्ट नहीं भेजा है। गिफ्ट भेजकर नंबर #1 बनें और रॉकेट पर अपनी DP उड़ाएं!",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFC5BCE8),
              modifier = Modifier.padding(12.dp),
            )
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            contributors.take(10).forEach { member ->
              val rankBg = when (member.rank) {
                1 -> SwarGold.copy(alpha = 0.16f)
                2 -> Color(0x28E0E6F8)
                3 -> Color(0x28FF8A65)
                else -> Color(0x14FFFFFF)
              }
              val rankAccent = when (member.rank) {
                1 -> SwarGold
                2 -> Color(0xFFE0E6F8)
                3 -> Color(0xFFFFAB91)
                else -> Color(0xFF9C94BC)
              }
              Surface(
                color = rankBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, rankAccent.copy(alpha = if (member.isTop3Partner) 0.55f else 0.18f), RoundedCornerShape(12.dp)),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = "#${member.rank}",
                    color = rankAccent,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.width(26.dp),
                  )
                  UserAvatar(
                    url = member.avatarUrl,
                    name = member.displayName,
                    size = 34.dp,
                    borderColor = rankAccent,
                    borderWidth = 1.5.dp,
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = member.displayName,
                      color = Color.White,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "खर्च: ${SwarUserIdentity.formatIndianNumber(member.contributedCoins)} कॉइंस",
                      color = Color(0xFFD0C9EC),
                      style = MaterialTheme.typography.labelSmall,
                    )
                  }
                  if (member.isTop3Partner) {
                    Column(horizontalAlignment = Alignment.End) {
                      Text(
                        text = "+${SwarUserIdentity.formatIndianNumber(member.projectedRewardCoins)} 🪙",
                        color = SwarGold,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                      )
                      Text(
                        text = "+${SwarUserIdentity.formatIndianNumber(member.projectedRewardHp)} HP",
                        color = SwarCyan,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  } else {
                    Text(
                      text = "टॉप 3 से बाहर",
                      color = Color(0xFF9E96B8),
                      style = MaterialTheme.typography.labelSmall,
                    )
                  }
                }
              }
            }
          }
        }

        HorizontalDivider(color = Color(0x28FFFFFF))

        // 5-Tier Rocket Economy Chart (LV.1 to LV.5)
        Text(
          text = "पाँचों रॉकेट चार्ट (LV.1 से LV.5)",
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SwarUserIdentity.rocketTiers.forEach { tier ->
            val isCurrent = tier.level == rocketLevel
            Surface(
              color = if (isCurrent) tier.primaryColor.copy(alpha = 0.16f) else Color(0xFF1C1036),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .border(
                  width = if (isCurrent) 1.5.dp else 1.dp,
                  color = if (isCurrent) tier.primaryColor else Color(0x28FFFFFF),
                  shape = RoundedCornerShape(14.dp),
                )
                .testTag("rocket_tier_row_${tier.level}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  color = tier.primaryColor.copy(alpha = 0.22f),
                  shape = RoundedCornerShape(10.dp),
                ) {
                  Text(
                    text = "LV.${tier.level}",
                    color = tier.primaryColor,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = "लक्ष्य: ${SwarUserIdentity.formatIndianNumber(tier.targetCoins)} कॉइंस",
                      color = Color.White,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                    )
                    if (isCurrent) {
                      Surface(
                        color = SwarGold,
                        shape = RoundedCornerShape(6.dp),
                      ) {
                        Text(
                          text = "सक्रिय",
                          color = Color(0xFF1B0F00),
                          fontSize = 9.sp,
                          fontWeight = FontWeight.ExtraBold,
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                      }
                    }
                  }
                  Text(
                    text = "इनाम: 🪙 ${SwarUserIdentity.formatIndianNumber(tier.rewardCoins)} कॉइंस  •  ⚡ ${SwarUserIdentity.formatIndianNumber(tier.rewardHp)} HP",
                    color = SwarGold,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun FullScreenRocketLaunchOverlay(
  event: RocketLaunchEvent,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val spec = remember(event.rocketLevel) { SwarUserIdentity.rocketTierSpec(event.rocketLevel) }
  val timeline = remember(event.id) { Animatable(0f) }

  LaunchedEffect(event.id) {
    timeline.snapTo(0f)
    timeline.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 8200, easing = LinearEasing),
    )
    onDismiss()
  }

  val infinite = rememberInfiniteTransition(label = "launch_fx")
  val flamePulse by infinite.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(140, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "flame_pulse",
  )

  val t = timeline.value
  val rank1Uid = event.winnerUids.firstOrNull().orEmpty()
  val rank1Name = event.winnerNames[rank1Uid].orEmpty().ifBlank { "विजेता #1" }
  val rank1Avatar = event.winnerAvatars[rank1Uid].orEmpty()

  // Ascent phase: 0.0 -> 0.44; Blast phase: 0.40 -> 0.64; Winners card: 0.50 -> 1.0
  val ascentProgress = (t / 0.44f).coerceIn(0f, 1f)
  val easedAscent = FastOutSlowInEasing.transform(ascentProgress)
  val rocketVisible = t < 0.47f
  val blastProgress = ((t - 0.40f) / 0.26f).coerceIn(0f, 1f)
  val cardAlpha = ((t - 0.48f) / 0.14f).coerceIn(0f, 1f)

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xF5060214), Color(0xF5140836), Color(0xFA04010C))
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("full_screen_rocket_launch_overlay"),
  ) {
    val boxW = constraints.maxWidth.toFloat()
    val boxH = constraints.maxHeight.toFloat()

    // Full-screen cosmic background stars, rocket flames, shockwave rings & blast sparks
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Twinkling background star streaks
      for (i in 0 until 28) {
        val sx = ((i * 73 + 19) % 100) / 100f * w
        val baseY = ((i * 41 + 7) % 100) / 100f * h
        val sy = (baseY + t * h * 0.65f) % h
        val r = if (i % 3 == 0) 3.2f else 1.8f
        drawCircle(
          color = Color.White.copy(alpha = 0.25f + (i % 4) * 0.15f),
          radius = r,
          center = Offset(sx, sy),
        )
      }

      if (rocketVisible) {
        val shakeX = if (t < 0.10f) sin(t * 160f) * 9f else 0f
        val rx = w * 0.5f + shakeX
        val startY = h * 0.80f
        val endY = h * 0.22f
        val ry = startY + (endY - startY) * easedAscent
        val rocketScale = 1f + 0.12f * sin(easedAscent * PI.toFloat())
        val bodyW = 92f * rocketScale
        val bodyH = 166f * rocketScale

        // Outer exhaust plume glow
        val exhaustLen = (130f + 110f * easedAscent) * flamePulse
        drawOval(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color.White,
              Color(0xFFFFEA00),
              spec.flameColor,
              spec.flameColor.copy(alpha = 0f),
            ),
            startY = ry + bodyH * 0.42f,
            endY = ry + bodyH * 0.42f + exhaustLen,
          ),
          topLeft = Offset(rx - bodyW * 0.38f, ry + bodyH * 0.38f),
          size = Size(bodyW * 0.76f, exhaustLen),
        )

        // Left & right booster flames
        drawOval(
          color = spec.flameColor.copy(alpha = 0.75f),
          topLeft = Offset(rx - bodyW * 0.62f, ry + bodyH * 0.40f),
          size = Size(bodyW * 0.26f, exhaustLen * 0.55f),
        )
        drawOval(
          color = spec.flameColor.copy(alpha = 0.75f),
          topLeft = Offset(rx + bodyW * 0.36f, ry + bodyH * 0.40f),
          size = Size(bodyW * 0.26f, exhaustLen * 0.55f),
        )

        // Rocket left & right aerodynamic fins
        val leftFin = Path().apply {
          moveTo(rx - bodyW * 0.48f, ry + bodyH * 0.08f)
          lineTo(rx - bodyW * 0.98f, ry + bodyH * 0.46f)
          lineTo(rx - bodyW * 0.44f, ry + bodyH * 0.38f)
          close()
        }
        val rightFin = Path().apply {
          moveTo(rx + bodyW * 0.48f, ry + bodyH * 0.08f)
          lineTo(rx + bodyW * 0.98f, ry + bodyH * 0.46f)
          lineTo(rx + bodyW * 0.44f, ry + bodyH * 0.38f)
          close()
        }
        drawPath(leftFin, color = SwarCoral)
        drawPath(rightFin, color = SwarCoral)

        // Nose Cone
        val nose = Path().apply {
          moveTo(rx, ry - bodyH * 0.72f)
          lineTo(rx - bodyW * 0.48f, ry - bodyH * 0.28f)
          lineTo(rx + bodyW * 0.48f, ry - bodyH * 0.28f)
          close()
        }
        drawPath(
          nose,
          brush = Brush.verticalGradient(listOf(SwarGold, spec.primaryColor)),
        )

        // Main Fuselage Body
        drawRoundRect(
          brush = Brush.horizontalGradient(
            listOf(spec.primaryColor, Color(0xFFF6F8FF), spec.primaryColor)
          ),
          topLeft = Offset(rx - bodyW * 0.48f, ry - bodyH * 0.30f),
          size = Size(bodyW * 0.96f, bodyH * 0.72f),
          cornerRadius = CornerRadius(24f, 24f),
        )
      }

      // Dramatic Peak-Altitude Blast Shockwaves & Radial Gold/HP Spark Explosion
      if (blastProgress > 0f && blastProgress < 1f) {
        val bx = w * 0.5f
        val by = h * 0.24f
        val fade = (1f - blastProgress).coerceIn(0f, 1f)
        val maxRadius = maxOf(w, h) * 0.62f

        // Flash core
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              Color.White.copy(alpha = fade * 0.92f),
              SwarGold.copy(alpha = fade * 0.65f),
              spec.flameColor.copy(alpha = fade * 0.3f),
              Color.Transparent,
            ),
            center = Offset(bx, by),
            radius = (80f + maxRadius * blastProgress * 0.65f).coerceAtLeast(10f),
          ),
          radius = (80f + maxRadius * blastProgress * 0.65f).coerceAtLeast(10f),
          center = Offset(bx, by),
        )

        // Triple expanding shockwave rings
        drawCircle(
          color = SwarGold.copy(alpha = fade * 0.9f),
          radius = maxRadius * blastProgress,
          center = Offset(bx, by),
          style = Stroke(width = 10f * fade + 2f),
        )
        drawCircle(
          color = spec.primaryColor.copy(alpha = fade * 0.85f),
          radius = maxRadius * (blastProgress * 0.72f),
          center = Offset(bx, by),
          style = Stroke(width = 7f * fade + 1.5f),
        )

        // 28 exploding reward streaks
        val sparkCount = 28
        for (i in 0 until sparkCount) {
          val angle = (2.0 * PI * i / sparkCount).toFloat()
          val dist = maxRadius * (0.25f + 0.85f * blastProgress) * (0.7f + (i % 3) * 0.18f)
          val px = bx + cos(angle) * dist
          val py = by + sin(angle) * dist
          val tailX = bx + cos(angle) * (dist * 0.72f)
          val tailY = by + sin(angle) * (dist * 0.72f)
          val color = when (i % 3) {
            0 -> SwarGold
            1 -> SwarCyan
            else -> spec.flameColor
          }
          drawLine(
            color = color.copy(alpha = fade),
            start = Offset(tailX, tailY),
            end = Offset(px, py),
            strokeWidth = 6f * fade + 1.5f,
            cap = StrokeCap.Round,
          )
          drawCircle(
            color = Color.White.copy(alpha = fade),
            radius = 6f * fade + 2f,
            center = Offset(px, py),
          )
        }
      }
    }

    // #1 Rank Contributor's DP Riding Directly on the Rocket Cockpit as It Flies!
    if (rocketVisible) {
      val shakeX = if (t < 0.10f) sin(t * 160f) * 9f else 0f
      val startY = boxH * 0.80f
      val endY = boxH * 0.22f
      val currentY = startY + (endY - startY) * easedAscent
      val dpBoxHalfPx = with(androidx.compose.ui.platform.LocalDensity.current) { 38.dp.toPx() }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .offset {
            IntOffset(
              x = ((boxW * 0.5f + shakeX) - dpBoxHalfPx).roundToInt(),
              y = (currentY - dpBoxHalfPx * 1.15f).roundToInt(),
            )
          }
          .width(76.dp)
          .testTag("rocket_launch_rank1_dp"),
      ) {
        Surface(
          color = SwarGold,
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = "👑 #1 सवार",
            color = Color(0xFF1A0E00),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        UserAvatar(
          url = rank1Avatar,
          name = rank1Name,
          size = 56.dp,
          borderColor = SwarGold,
          borderWidth = 3.dp,
        )
      }

      // Top Ascent Banner
      Surface(
        color = Color(0xAA170C35),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 26.dp, start = 20.dp, end = 64.dp)
          .border(1.2.dp, spec.primaryColor.copy(alpha = 0.8f), RoundedCornerShape(18.dp)),
      ) {
        Text(
          text = "🚀 रॉकेट LV.${event.rocketLevel} लॉन्च! $rank1Name रॉकेट पर सवार...",
          color = Color.White,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
      }
    }

    // Blast Title Flash at Peak
    if (blastProgress > 0.05f && blastProgress < 0.95f) {
      val blastScale = 0.8f + 0.5f * sin(blastProgress * PI.toFloat())
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 32.dp)
          .scale(blastScale),
      ) {
        Text(
          text = "💥 रॉकेट LV.${event.rocketLevel} ब्लास्ट! 💥",
          style = MaterialTheme.typography.headlineSmall,
          color = SwarGold,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center,
        )
      }
    }

    // Close / Skip Button (always accessible top-right)
    IconButton(
      onClick = onDismiss,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(12.dp)
        .minimumInteractiveComponentSize()
        .clip(CircleShape)
        .background(Color(0x66000000))
        .testTag("rocket_launch_skip_button"),
    ) {
      Icon(Icons.Filled.Close, contentDescription = "एनीमेशन बंद करें", tint = Color.White)
    }

    // Phase 3: Top-3 Winners Reward Payout Card After Blast
    if (cardAlpha > 0.02f) {
      Surface(
        color = Color(0xEE180D34),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 12.dp,
        modifier = Modifier
          .align(Alignment.Center)
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .alpha(cardAlpha)
          .border(
            width = 1.6.dp,
            brush = Brush.linearGradient(listOf(SwarGold, spec.primaryColor, SwarCoral)),
            shape = RoundedCornerShape(24.dp),
          )
          .testTag("rocket_launch_winners_card"),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = SwarGold, modifier = Modifier.size(26.dp))
            Text(
              text = "रॉकेट LV.${event.rocketLevel} ब्लास्ट इनाम!",
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
              fontWeight = FontWeight.ExtraBold,
            )
          }

          Surface(
            color = Color(0x28FFB800),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.border(1.dp, SwarGold.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = SwarGold, modifier = Modifier.size(16.dp))
                Text(
                  text = "${SwarUserIdentity.formatIndianNumber(event.totalRewardCoins)} कॉइंस",
                  color = SwarGold,
                  fontWeight = FontWeight.ExtraBold,
                  style = MaterialTheme.typography.labelLarge,
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = SwarCyan, modifier = Modifier.size(16.dp))
                Text(
                  text = "${SwarUserIdentity.formatIndianNumber(event.totalRewardHp)} HP",
                  color = SwarCyan,
                  fontWeight = FontWeight.ExtraBold,
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }
          }

          Text(
            text = "टॉप ${event.winnerUids.size.coerceAtMost(3)} भागीदार विजेता (बाकी भागीदारों को इनाम नहीं)",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFD6CEFA),
            textAlign = TextAlign.Center,
          )

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            event.winnerUids.take(3).forEachIndexed { idx, uid ->
              val name = event.winnerNames[uid].orEmpty().ifBlank { "विजेता #${idx + 1}" }
              val avatar = event.winnerAvatars[uid].orEmpty()
              val rewardCoins = event.winnerRewardCoins[uid] ?: 0L
              val rewardHp = event.winnerRewardHp[uid] ?: 0L
              val contrib = event.winnerContributions[uid] ?: 0L
              val medalColor = when (idx) {
                0 -> SwarGold
                1 -> Color(0xFFE0E6F8)
                else -> Color(0xFFFFAB91)
              }
              Surface(
                color = medalColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, medalColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Text(
                    text = "#${idx + 1}",
                    color = medalColor,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                  )
                  UserAvatar(
                    url = avatar,
                    name = name,
                    size = 42.dp,
                    borderColor = medalColor,
                    borderWidth = 2.dp,
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = name,
                      color = Color.White,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "योगदान: ${SwarUserIdentity.formatIndianNumber(contrib)}",
                      color = Color(0xFFD0C9EC),
                      style = MaterialTheme.typography.labelSmall,
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = "+${SwarUserIdentity.formatIndianNumber(rewardCoins)} 🪙",
                      color = SwarGold,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                      text = "+${SwarUserIdentity.formatIndianNumber(rewardHp)} HP (लेवल+)",
                      color = SwarMint,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }

          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(
              containerColor = SwarGold,
              contentColor = Color(0xFF1A0E00),
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("dismiss_rocket_launch_button"),
          ) {
            Text("शानदार! ठीक है", fontWeight = FontWeight.ExtraBold)
          }
        }
      }
    }
  }
}

private fun formatCompactCoins(value: Long): String {
  return when {
    value >= 10_000_000L -> "${"%.1f".format(value / 10_000_000.0)}Cr"
    value >= 100_000L -> "${"%.1f".format(value / 100_000.0)}L"
    value >= 1_000L -> "${value / 1_000L}K"
    else -> value.toString()
  }
}
