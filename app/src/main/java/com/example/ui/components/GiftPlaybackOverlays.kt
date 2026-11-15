package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.model.GiftEvent
import com.example.data.repository.SupabaseNetworkClient
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarViolet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun CoinEmblem(
  modifier: Modifier = Modifier,
  sizeDp: Int = 20,
) {
  Box(
    modifier = modifier
      .size(sizeDp.dp)
      .clip(CircleShape)
      .background(
        Brush.radialGradient(
          colors = listOf(Color(0xFFFFF3B0), SwarGold, Color(0xFFD98A00))
        )
      )
      .border(1.2.dp, Color(0xFFFFE680), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = "₹",
      color = Color(0xFF5A3300),
      fontWeight = FontWeight.ExtraBold,
      fontSize = (sizeDp * 0.56f).sp,
    )
  }
}

@Composable
fun CoinBalancePill(
  coins: Long,
  onAddCoinsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onAddCoinsClick,
    color = Color(0xCC1E1438),
    shape = RoundedCornerShape(20.dp),
    modifier = modifier
      .border(
        1.dp,
        Brush.horizontalGradient(listOf(SwarGold, Color(0xFFFF9800))),
        RoundedCornerShape(20.dp),
      )
      .testTag("coins_balance_pill"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      CoinEmblem(sizeDp = 18)
      Text(
        text = coins.toString(),
        style = MaterialTheme.typography.labelLarge,
        color = SwarGold,
        fontWeight = FontWeight.Bold,
      )
      Box(
        modifier = Modifier
          .size(18.dp)
          .clip(CircleShape)
          .background(SwarGold),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = "कॉइंस जोड़ें",
          tint = Color(0xFF3E2200),
          modifier = Modifier.size(13.dp),
        )
      }
    }
  }
}

/**
 * Right-to-left scrolling marquee banner across all rooms announcing live gifts:
 * "<Sender> ने <Recipient> को <Gift> भेजा"
 */
@Composable
fun CrossRoomGiftMarqueeStrip(
  event: GiftEvent,
  modifier: Modifier = Modifier,
) {
  var textWidthPx by remember(event.id) { mutableIntStateOf(0) }
  val density = LocalDensity.current

  BoxWithConstraints(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 4.dp)
      .height(34.dp)
      .clip(RoundedCornerShape(17.dp))
      .background(
        Brush.horizontalGradient(
          colors = listOf(
            Color(0xDD4B1878),
            Color(0xDD9A1F6A),
            Color(0xDD4B1878),
          )
        )
      )
      .border(1.dp, SwarGold.copy(alpha = 0.85f), RoundedCornerShape(17.dp))
      .clipToBounds()
      .testTag("cross_room_gift_marquee"),
    contentAlignment = Alignment.CenterStart,
  ) {
    val containerWidthPx = with(density) { maxWidth.toPx() }
    val travelDistance = containerWidthPx + textWidthPx.toFloat().coerceAtLeast(300f)

    val infiniteTransition = rememberInfiniteTransition(label = "giftMarquee")
    val progress by infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 7500, easing = LinearEasing),
        repeatMode = RepeatMode.Restart,
      ),
      label = "marqueeProgress",
    )

    // Moves smoothly from Right (+containerWidthPx) to Left (-textWidthPx)
    val offsetX = containerWidthPx - (travelDistance * progress)
    val qtyText = if (event.quantity > 1L) " x${event.quantity}" else ""

    Row(
      modifier = Modifier
        .wrapContentWidth(unbounded = true)
        .onSizeChanged { textWidthPx = it.width }
        .offset { IntOffset(offsetX.roundToInt(), 0) }
        .padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.WorkspacePremium,
        contentDescription = null,
        tint = SwarGold,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = buildAnnotatedString {
          withStyle(SpanStyle(color = SwarGold, fontWeight = FontWeight.ExtraBold)) {
            append(event.senderName)
          }
          withStyle(SpanStyle(color = Color.White)) {
            append(" ने ")
          }
          withStyle(SpanStyle(color = SwarCyan, fontWeight = FontWeight.Bold)) {
            append(event.recipientName)
          }
          withStyle(SpanStyle(color = Color.White)) {
            append(" को ")
          }
          withStyle(SpanStyle(color = Color(0xFFFFE57F), fontWeight = FontWeight.Bold)) {
            append("${event.giftEmoji} ${event.giftName}$qtyText")
          }
          withStyle(SpanStyle(color = Color.White)) {
            append(" भेजा! ")
          }
          if (event.roomTitle.isNotBlank()) {
            withStyle(SpanStyle(color = Color(0xFFD6C8FF))) {
              append("• [${event.roomTitle}]")
            }
          }
        },
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        softWrap = false,
      )
    }
  }
}

/**
 * Full-screen gift playback overlay for everyone inside the room.
 * Plays Supabase video gifts full-screen via VideoView; falls back to full-screen celebration for emoji/image gifts.
 */
@Composable
fun FullScreenGiftOverlay(
  event: GiftEvent,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var videoFailed by remember(event.id) { mutableStateOf(false) }
  val isVideoGift = event.giftMediaType == "video" && event.giftMediaUrl.isNotBlank() && !videoFailed
  var resolvedVideoUri by remember(event.id, event.giftMediaUrl) {
    mutableStateOf<Uri?>(if (event.giftMediaUrl.startsWith("http")) null else Uri.parse(event.giftMediaUrl))
  }

  LaunchedEffect(event.id, event.giftMediaUrl, isVideoGift) {
    if (isVideoGift && event.giftMediaUrl.startsWith("http")) {
      val cached = withContext(Dispatchers.IO) {
        SupabaseNetworkClient.cacheRemoteMediaFile(context.cacheDir, event.giftMediaUrl)
      }
      resolvedVideoUri = cached?.let { Uri.fromFile(it) } ?: Uri.parse(event.giftMediaUrl)
    }
  }

  LaunchedEffect(event.id, isVideoGift) {
    val autoDismissMs = if (isVideoGift) 14_000L else 3_400L
    delay(autoDismissMs)
    onDismiss()
  }

  val pulseTransition = rememberInfiniteTransition(label = "giftBurst")
  val scaleAnim by pulseTransition.animateFloat(
    initialValue = 0.88f,
    targetValue = 1.14f,
    animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
    label = "giftScale",
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(if (isVideoGift) Color.Black else Color(0xD90E0622))
      .clickable(onClick = onDismiss)
      .testTag("fullscreen_gift_overlay"),
  ) {
    if (isVideoGift) {
      var videoViewRef by remember(event.id) { mutableStateOf<VideoView?>(null) }
      val playbackUri = resolvedVideoUri

      if (playbackUri != null) {
        key(playbackUri) {
          AndroidView(
            factory = { ctx ->
              VideoView(ctx).apply {
                setVideoURI(playbackUri)
                setOnPreparedListener { mp ->
                  mp.isLooping = false
                  mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                  start()
                }
                setOnCompletionListener {
                  onDismiss()
                }
                setOnErrorListener { _, _, _ ->
                  videoFailed = true
                  true
                }
                videoViewRef = this
              }
            },
            modifier = Modifier.fillMaxSize(),
          )
        }
      }

      DisposableEffect(event.id) {
        onDispose {
          videoViewRef?.stopPlayback()
        }
      }
    } else {
      // Festive full-screen visual gift burst for image/emoji gifts
      Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(SwarGold.copy(alpha = 0.42f), SwarCoral.copy(alpha = 0.18f), Color.Transparent),
            center = center,
            radius = size.minDimension * 0.48f,
          ),
          center = center,
          radius = size.minDimension * 0.48f,
        )
        drawCircle(
          color = SwarGold.copy(alpha = 0.45f),
          radius = size.minDimension * 0.28f * scaleAnim,
          center = center,
          style = Stroke(width = 6f),
        )
      }

      Column(
        modifier = Modifier
          .align(Alignment.Center)
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (event.giftMediaUrl.isNotBlank() && event.giftMediaType == "image") {
          AsyncImage(
            model = event.giftMediaUrl,
            contentDescription = event.giftName,
            modifier = Modifier
              .size(210.dp)
              .scale(scaleAnim)
              .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop,
          )
        } else {
          Text(
            text = event.giftEmoji.ifBlank { "🎁" },
            fontSize = 112.sp,
            modifier = Modifier.scale(scaleAnim),
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        val qtySuffix = if (event.quantity > 1L) " x${event.quantity}" else ""
        Text(
          text = "${event.giftName}$qtySuffix",
          style = MaterialTheme.typography.headlineSmall,
          color = SwarGold,
          fontWeight = FontWeight.ExtraBold,
        )
      }
    }

    // Top-right Skip button
    IconButton(
      onClick = onDismiss,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .statusBarsPadding()
        .padding(16.dp)
        .size(48.dp)
        .clip(CircleShape)
        .background(Color(0x88000000)),
    ) {
      Icon(Icons.Filled.Close, contentDescription = "गिफ्ट बंद करें", tint = Color.White)
    }

    // Bottom Sender -> Recipient Celebration Card
    Surface(
      color = Color(0xBB160B32),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 28.dp)
        .border(1.5.dp, SwarGold, RoundedCornerShape(24.dp)),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        UserAvatar(
          url = event.senderAvatarUrl,
          name = event.senderName,
          size = 44.dp,
          borderColor = SwarGold,
          borderWidth = 1.5.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
          val qtySuffix = if (event.quantity > 1L) " x${event.quantity}" else ""
          Text(
            text = "${event.senderName} ने ${event.recipientName} को भेजा",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${event.giftEmoji} ${event.giftName}$qtySuffix • 🪙 ${event.coinCost}",
            style = MaterialTheme.typography.bodySmall,
            color = SwarGold,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Surface(
          color = SwarViolet,
          shape = RoundedCornerShape(14.dp),
        ) {
          Text(
            text = "LIVE",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
    }
  }
}
