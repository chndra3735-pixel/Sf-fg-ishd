package com.example.ui.components

import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
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
 * Royal cross-room announcement ribbon announcing live gifts:
 * "<Sender> ने <Recipient> को <Gift> भेजा"
 * Scrolls smoothly across once and disappears cleanly along with the ribbon itself.
 */
@Composable
fun CrossRoomGiftMarqueeStrip(
  event: GiftEvent,
  onFinished: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var rowWidthPx by remember(event.id) { mutableIntStateOf(0) }
  var isBannerVisible by remember(event.id) { mutableStateOf(true) }
  val scrollProgress = remember(event.id) { Animatable(0f) }

  LaunchedEffect(event.id) {
    isBannerVisible = true
    scrollProgress.snapTo(0f)
    var waitedMs = 0L
    while (rowWidthPx <= 0 && waitedMs < 600L) {
      delay(16L)
      waitedMs += 16L
    }
    val extraDurationMs = (rowWidthPx * 4L).coerceIn(0L, 3500L)
    val scrollDurationMs = (6500L + extraDurationMs).toInt()
    scrollProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = scrollDurationMs, easing = LinearEasing),
    )
    isBannerVisible = false
    delay(260L)
    onFinished()
  }

  val shimmerTransition = rememberInfiniteTransition(label = "royalRibbonShimmer")
  val shimmerFraction by shimmerTransition.animateFloat(
    initialValue = -0.35f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "shimmerProgress",
  )

  val qtyText = if (event.quantity > 1L) " x${event.quantity}" else ""

  AnimatedVisibility(
    visible = isBannerVisible,
    enter = fadeIn(tween(240)) + slideInHorizontally(tween(260)) { it / 3 } + expandVertically(tween(240)),
    exit = fadeOut(tween(220)) + shrinkVertically(tween(220)),
    modifier = modifier,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 4.dp)
        .height(44.dp)
        .clip(RoundedCornerShape(22.dp))
        .background(
          Brush.horizontalGradient(
            colors = listOf(
              Color(0xF21B0B3D),
              Color(0xF24C126E),
              Color(0xF2841658),
              Color(0xF22D0D4E),
            )
          )
        )
        .border(
          width = 1.5.dp,
          brush = Brush.horizontalGradient(
            colors = listOf(
              Color(0xFFFFF59D),
              SwarGold,
              SwarCoral,
              SwarGold,
              Color(0xFFFFF59D),
            )
          ),
          shape = RoundedCornerShape(22.dp),
        )
        .testTag("cross_room_gift_marquee"),
    ) {
      // Animated luxury light sheen sweeping across the royal ribbon
      Canvas(modifier = Modifier.fillMaxSize()) {
        val bandWidth = size.width * 0.28f
        val startX = (size.width + bandWidth) * shimmerFraction - bandWidth
        drawRect(
          brush = Brush.linearGradient(
            colors = listOf(
              Color.Transparent,
              Color(0x28FFF8DC),
              Color.Transparent,
            ),
            start = Offset(startX, 0f),
            end = Offset(startX + bandWidth, size.height),
          )
        )
        // Subtle top satin rim highlight
        drawLine(
          color = Color(0x44FFFFFF),
          start = Offset(18f, 1.5f),
          end = Offset(size.width - 18f, 1.5f),
          strokeWidth = 1.5f,
        )
      }

      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Left Royal Golden Crest Badge
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color.Transparent,
          modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
              Brush.linearGradient(
                colors = listOf(Color(0xFFFFF1A8), SwarGold, Color(0xFFE68900))
              )
            )
            .border(1.dp, Color(0xFFFFF8DC), RoundedCornerShape(16.dp)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.WorkspacePremium,
              contentDescription = null,
              tint = Color(0xFF422100),
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "GIFT",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF3A1C00),
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.6.sp,
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Scrolling Marquee Track (clipped cleanly between left crest and right sparkle)
        BoxWithConstraints(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clipToBounds(),
          contentAlignment = Alignment.CenterStart,
        ) {
          val density = LocalDensity.current
          val trackWidthPx = with(density) { maxWidth.toPx() }
          val travelDistance = trackWidthPx + rowWidthPx.toFloat().coerceAtLeast(240f)
          val offsetX = trackWidthPx - (travelDistance * scrollProgress.value)

          Row(
            modifier = Modifier
              .wrapContentWidth(unbounded = true)
              .onSizeChanged { rowWidthPx = it.width }
              .offset { IntOffset(offsetX.roundToInt(), 0) }
              .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            UserAvatar(
              url = event.senderAvatarUrl,
              name = event.senderName,
              size = 26.dp,
              borderColor = SwarGold,
              borderWidth = 1.2.dp,
            )

            Text(
              text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFFFFE57F), fontWeight = FontWeight.ExtraBold)) {
                  append(event.senderName)
                }
                withStyle(SpanStyle(color = Color.White)) {
                  append(" ने ")
                }
                withStyle(SpanStyle(color = SwarCyan, fontWeight = FontWeight.ExtraBold)) {
                  append(event.recipientName)
                }
                withStyle(SpanStyle(color = Color.White)) {
                  append(" को ")
                }
                withStyle(SpanStyle(color = Color(0xFFFF9EBB), fontWeight = FontWeight.ExtraBold)) {
                  append("${event.giftEmoji} ${event.giftName}$qtyText")
                }
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) {
                  append(" भेजा!")
                }
                if (event.roomTitle.isNotBlank()) {
                  withStyle(SpanStyle(color = Color(0xFFD8CAFF), fontWeight = FontWeight.Medium)) {
                    append("  ✦ ${event.roomTitle}")
                  }
                }
              },
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
              softWrap = false,
            )

            if (event.giftMediaType == "image" && event.giftMediaUrl.isNotBlank()) {
              AsyncImage(
                model = event.giftMediaUrl,
                contentDescription = null,
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .border(1.dp, SwarGold, CircleShape),
                contentScale = ContentScale.Crop,
              )
            }
          }
        }

        // Right Sparkle Jewel Cap
        Box(
          modifier = Modifier
            .padding(start = 4.dp, end = 4.dp)
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(0x33FFD54F)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFFFFE57F),
            modifier = Modifier.size(14.dp),
          )
        }
      }
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
  var isVideoPrepared by remember(event.id) { mutableStateOf(false) }
  var preparedDurationMs by remember(event.id) { mutableStateOf<Long?>(null) }
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

  LaunchedEffect(event.id, isVideoGift, preparedDurationMs) {
    val autoDismissMs = if (isVideoGift) {
      preparedDurationMs?.let { (it + 2_000L).coerceIn(4_000L, 90_000L) } ?: 35_000L
    } else {
      3_400L
    }
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

  val blockTouchInteractionSource = remember { MutableInteractionSource() }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(if (isVideoGift) Color.Black else Color(0xD90E0622))
      // Consume touches without closing the room video when touched anywhere on screen
      .clickable(
        interactionSource = blockTouchInteractionSource,
        indication = null,
        onClick = { if (!isVideoGift) onDismiss() },
      )
      .testTag("fullscreen_gift_overlay"),
  ) {
    if (isVideoGift) {
      var mediaPlayerRef by remember(event.id) { mutableStateOf<MediaPlayer?>(null) }
      var surfaceRef by remember(event.id) { mutableStateOf<Surface?>(null) }
      val playbackUri = resolvedVideoUri

      if (playbackUri != null) {
        key(playbackUri) {
          AndroidView(
            factory = { ctx ->
              TextureView(ctx).apply {
                isOpaque = true
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                  override fun onSurfaceTextureAvailable(
                    surfaceTexture: SurfaceTexture,
                    width: Int,
                    height: Int,
                  ) {
                    val videoSurface = Surface(surfaceTexture)
                    surfaceRef = videoSurface
                    val player = MediaPlayer()
                    mediaPlayerRef = player
                    runCatching {
                      player.setAudioAttributes(
                        AudioAttributes.Builder()
                          .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                          .setUsage(AudioAttributes.USAGE_MEDIA)
                          .build()
                      )
                      player.setSurface(videoSurface)
                      player.setDataSource(ctx, playbackUri)
                      player.isLooping = false
                      player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                      player.setOnPreparedListener { mp ->
                        isVideoPrepared = true
                        val dur = mp.duration.toLong()
                        if (dur > 0L) {
                          preparedDurationMs = dur
                        }
                        mp.start()
                      }
                      player.setOnCompletionListener {
                        onDismiss()
                      }
                      player.setOnErrorListener { _, _, _ ->
                        videoFailed = true
                        true
                      }
                      player.prepareAsync()
                    }.onFailure {
                      runCatching { player.release() }
                      runCatching { videoSurface.release() }
                      if (mediaPlayerRef === player) mediaPlayerRef = null
                      if (surfaceRef === videoSurface) surfaceRef = null
                      videoFailed = true
                    }
                  }

                  override fun onSurfaceTextureSizeChanged(
                    surfaceTexture: SurfaceTexture,
                    width: Int,
                    height: Int,
                  ) = Unit

                  override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                    runCatching { mediaPlayerRef?.stop() }
                    runCatching { mediaPlayerRef?.release() }
                    mediaPlayerRef = null
                    runCatching { surfaceRef?.release() }
                    surfaceRef = null
                    return true
                  }

                  override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
                }
              }
            },
            modifier = Modifier.fillMaxSize(),
          )
        }
      }

      if (playbackUri == null || !isVideoPrepared) {
        CircularProgressIndicator(
          color = SwarGold,
          modifier = Modifier.align(Alignment.Center),
        )
      }

      DisposableEffect(event.id, playbackUri) {
        onDispose {
          runCatching { mediaPlayerRef?.stop() }
          runCatching { mediaPlayerRef?.release() }
          mediaPlayerRef = null
          runCatching { surfaceRef?.release() }
          surfaceRef = null
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

      // Skip button shown only for non-video celebrations so video playback never closes on touch
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
    }
  }
}
