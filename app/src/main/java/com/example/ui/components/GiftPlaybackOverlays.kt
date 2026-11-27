package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.draw.alpha
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
import com.example.data.repository.LocalMediaCacheManager
import com.example.data.repository.SupabaseNetworkClient
import com.example.data.repository.VideoLinkResolution
import com.example.data.repository.VideoLinkResolver
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarGold
import com.example.ui.theme.SwarViolet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
  var ribbonWidthPx by remember(event.id) { mutableIntStateOf(0) }
  var isBannerVisible by remember(event.id) { mutableStateOf(true) }
  val slideProgress = remember(event.id) { Animatable(0f) }

  LaunchedEffect(event.id) {
    isBannerVisible = true
    slideProgress.snapTo(0f)
    var waitedMs = 0L
    while (ribbonWidthPx <= 0 && waitedMs < 600L) {
      delay(16L)
      waitedMs += 16L
    }
    val extraDurationMs = (ribbonWidthPx * 4L).coerceIn(0L, 3500L)
    val slideDurationMs = (6500L + extraDurationMs).toInt()
    slideProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = slideDurationMs, easing = LinearEasing),
    )
    isBannerVisible = false
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

  if (isBannerVisible) {
    BoxWithConstraints(
      modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .height(46.dp)
        .clipToBounds()
        .testTag("cross_room_gift_marquee"),
      contentAlignment = Alignment.CenterStart,
    ) {
      val density = LocalDensity.current
      val containerWidthPx = with(density) { maxWidth.toPx() }
      val effectiveRibbonWidth = ribbonWidthPx.toFloat().coerceAtLeast(320f)
      val travelDistance = containerWidthPx + effectiveRibbonWidth
      val offsetX = containerWidthPx - (travelDistance * slideProgress.value)

      // Entire royal ribbon (gradient pill + crest + stationary name text + gift DP + sparkle)
      // enters from right edge and exits completely off the left edge together.
      Box(
        modifier = Modifier
          .wrapContentWidth(unbounded = true)
          .onSizeChanged { ribbonWidthPx = it.width }
          .offset { IntOffset(offsetX.roundToInt(), 0) }
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
          ),
      ) {
        // Animated luxury light sheen across the sliding ribbon
        Canvas(modifier = Modifier.matchParentSize()) {
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
          drawLine(
            color = Color(0x44FFFFFF),
            start = Offset(18f, 1.5f),
            end = Offset(size.width - 18f, 1.5f),
            strokeWidth = 1.5f,
          )
        }

        Row(
          modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Left Royal Golden Crest Badge (fixed inside ribbon)
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

          // Right Sparkle Jewel Cap (fixed inside ribbon)
          Box(
            modifier = Modifier
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
}

/**
 * Full-screen gift playback overlay for everyone inside the room.
 * Plays Supabase video gifts full-screen via VideoView; falls back to full-screen celebration for emoji/image gifts.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FullScreenGiftOverlay(
  event: GiftEvent,
  onDismiss: () -> Unit,
  onPlaybackMessage: (String) -> Unit = {},
  isAudioMuted: Boolean = false,
  onToggleAudioMute: (() -> Unit)? = null,
  senderFrameUrl: String = "",
  senderExactLevel: Int? = null,
) {
  val context = LocalContext.current
  var videoFailed by remember(event.id) { mutableStateOf(false) }
  var isVideoPrepared by remember(event.id) { mutableStateOf(false) }
  var isEmbedLoaded by remember(event.id) { mutableStateOf(false) }
  var preparedDurationMs by remember(event.id) { mutableStateOf<Long?>(null) }
  var fallbackThumbUrl by remember(event.id) { mutableStateOf("") }
  var embedFallbackUrl by remember(event.id) { mutableStateOf<String?>(null) }
  val cleanGiftUrl = remember(event.giftMediaUrl) {
    VideoLinkResolver.extractFirstHttpUrl(event.giftMediaUrl)
  }
  val directInstagramEmbed = remember(cleanGiftUrl) {
    VideoLinkResolver.instagramEmbedUrl(cleanGiftUrl)
  }
  var resolvedEmbedUrl by remember(event.id, cleanGiftUrl) {
    mutableStateOf(directInstagramEmbed)
  }
  val isEmbedMode = embedFallbackUrl != null
  val isVideoGift = event.giftMediaType == "video" && cleanGiftUrl.isNotBlank() && !videoFailed && !isEmbedMode
  var resolvedVideoUri by remember(event.id, cleanGiftUrl) {
    mutableStateOf<Uri?>(if (cleanGiftUrl.startsWith("http", ignoreCase = true)) null else Uri.parse(cleanGiftUrl))
  }

  var resolvedPlayableHttpUrl by remember(event.id, cleanGiftUrl) { mutableStateOf("") }
  var triedRemoteStreamFallback by remember(event.id, cleanGiftUrl) { mutableStateOf(false) }

  LaunchedEffect(event.id, cleanGiftUrl) {
    if (event.giftMediaType == "video" && cleanGiftUrl.startsWith("http", ignoreCase = true)) {
      var resolution = withTimeoutOrNull(20_000L) {
        withContext(Dispatchers.IO) { VideoLinkResolver.resolvePlayableVideo(cleanGiftUrl) }
      }
      if (resolution == null || resolution is VideoLinkResolution.Failure) {
        delay(600L)
        val retryRes = withTimeoutOrNull(20_000L) {
          withContext(Dispatchers.IO) { VideoLinkResolver.resolvePlayableVideo(cleanGiftUrl) }
        }
        if (retryRes != null && (resolution == null || retryRes is VideoLinkResolution.Success)) {
          resolution = retryRes
        }
      }
      when (resolution) {
        is VideoLinkResolution.Success -> {
          resolvedPlayableHttpUrl = resolution.playableUrl
          if (resolution.thumbnailUrl.isNotBlank()) {
            fallbackThumbUrl = resolution.thumbnailUrl
          }
          VideoLinkResolver.instagramEmbedUrl(resolution.canonicalPageUrl)?.let {
            resolvedEmbedUrl = it
          }
          if (resolution.isHlsStream) {
            resolvedVideoUri = Uri.parse(resolution.playableUrl)
          } else {
            val cached = withTimeoutOrNull(25_000L) {
              withContext(Dispatchers.IO) {
                val baseDir = LocalMediaCacheManager.preferredMediaBaseDir(context)
                SupabaseNetworkClient.cacheRemoteMediaFile(baseDir, resolution.playableUrl)
              }
            }
            resolvedVideoUri = cached?.let { Uri.fromFile(it) } ?: Uri.parse(resolution.playableUrl)
          }
        }
        is VideoLinkResolution.Failure -> {
          if (resolution.fallbackImageUrl.isNotBlank()) {
            fallbackThumbUrl = resolution.fallbackImageUrl
          }
          val embedUrl = resolution.embedFallbackUrl.ifBlank {
            resolvedEmbedUrl ?: VideoLinkResolver.instagramEmbedUrl(resolution.canonicalPageUrl).orEmpty()
          }
          if (embedUrl.isNotBlank()) {
            resolvedEmbedUrl = embedUrl
            embedFallbackUrl = embedUrl
          } else {
            videoFailed = true
            onPlaybackMessage(resolution.reasonHindi)
          }
        }
        null -> {
          val embedUrl = resolvedEmbedUrl.orEmpty()
          if (embedUrl.isNotBlank()) {
            embedFallbackUrl = embedUrl
          } else {
            videoFailed = true
            onPlaybackMessage("वीडियो लिंक लोड होने में अधिक समय लग रहा है — कृपया पुनः प्रयास करें।")
          }
        }
      }
    }
  }

  val isOverlayVisible = when {
    isEmbedMode -> isEmbedLoaded
    isVideoGift -> isVideoPrepared
    else -> true
  }

  LaunchedEffect(event.id, isVideoGift, isEmbedMode, isOverlayVisible, preparedDurationMs) {
    if (!isOverlayVisible) {
      // Generous underground preload cap for slow connections before automatic dismissal
      delay(65_000L)
      onDismiss()
      return@LaunchedEffect
    }
    val autoDismissMs = when {
      isVideoGift -> preparedDurationMs?.let { (it + 2_000L).coerceIn(4_000L, 90_000L) } ?: 35_000L
      isEmbedMode -> 25_000L
      else -> 3_400L
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

  val overlayBg = when {
    !isOverlayVisible -> Color.Transparent
    isVideoGift || isEmbedMode -> Color.Black
    else -> Color(0xD90E0622)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(overlayBg)
      .then(
        if (isOverlayVisible) {
          Modifier.clickable(
            interactionSource = blockTouchInteractionSource,
            indication = null,
            onClick = { if (!isVideoGift && !isEmbedMode) onDismiss() },
          )
        } else {
          Modifier
        }
      )
      .testTag("fullscreen_gift_overlay"),
  ) {
    if (isEmbedMode && embedFallbackUrl != null) {
      var webViewRef by remember(event.id) { mutableStateOf<WebView?>(null) }

      AndroidView(
        factory = { ctx ->
          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
            var embedTerminated = false
            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (!embedTerminated && newProgress >= 95) {
                  isEmbedLoaded = true
                }
              }
            }
            webViewClient = object : WebViewClient() {
              override fun onPageFinished(view: WebView?, url: String?) {
                if (embedTerminated) return
                isEmbedLoaded = true
                val mutedJs = if (isAudioMuted) "true" else "false"
                view?.evaluateJavascript(
                  "try{var v=document.querySelector('video');if(v){v.muted=$mutedJs;v.play();}}catch(e){}",
                  null,
                )
              }

              override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
              ) {
                if (!embedTerminated && request?.isForMainFrame == true) {
                  embedTerminated = true
                  runCatching {
                    view?.stopLoading()
                    view?.webChromeClient = null
                    (view?.parent as? ViewGroup)?.removeView(view)
                    view?.removeAllViews()
                    view?.destroy()
                  }
                  webViewRef = null
                  embedFallbackUrl = null
                  videoFailed = true
                  onPlaybackMessage("एम्बेड वीडियो लोड नहीं हो सका — कृपया सीधा .mp4 वीडियो लिंक उपयोग करें।")
                }
              }

              @SuppressLint("NewApi")
              override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail?,
              ): Boolean {
                if (!embedTerminated) {
                  embedTerminated = true
                  runCatching {
                    view?.stopLoading()
                    view?.webChromeClient = null
                    (view?.parent as? ViewGroup)?.removeView(view)
                    view?.removeAllViews()
                    view?.destroy()
                  }
                  webViewRef = null
                  embedFallbackUrl = null
                  videoFailed = true
                  onPlaybackMessage("इस डिवाइस पर वेब एम्बेड प्लेयर क्रैश हुआ — सीधे प्ले के लिए .mp4 वीडियो लिंक जोड़ें।")
                }
                return true
              }
            }
            loadUrl(embedFallbackUrl!!)
            webViewRef = this
          }
        },
        modifier = Modifier
          .fillMaxSize()
          .alpha(if (isEmbedLoaded) 1f else 0f),
      )

      LaunchedEffect(isAudioMuted, isEmbedLoaded, webViewRef) {
        if (isEmbedLoaded) {
          val mutedJs = if (isAudioMuted) "true" else "false"
          webViewRef?.evaluateJavascript(
            "try{var v=document.querySelector('video');if(v){v.muted=$mutedJs;}}catch(e){}",
            null,
          )
        }
      }

      if (isEmbedLoaded) {
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(16.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0x99000000)),
        ) {
          Icon(Icons.Filled.Close, contentDescription = "गिफ्ट बंद करें", tint = Color.White)
        }
      }

      DisposableEffect(event.id, embedFallbackUrl) {
        onDispose {
          val wv = webViewRef
          webViewRef = null
          if (wv != null) {
            runCatching {
              wv.stopLoading()
              wv.webChromeClient = null
              wv.onPause()
              (wv.parent as? ViewGroup)?.removeView(wv)
              wv.removeAllViews()
              wv.destroy()
            }
          }
        }
      }
    } else if (isVideoGift) {
      var mediaPlayerRef by remember(event.id) { mutableStateOf<MediaPlayer?>(null) }
      var surfaceRef by remember(event.id) { mutableStateOf<Surface?>(null) }
      val playbackUri = resolvedVideoUri

      fun applyFitTransform(textureView: TextureView, videoW: Int, videoH: Int) {
        val viewW = textureView.width.toFloat()
        val viewH = textureView.height.toFloat()
        if (viewW <= 0f || viewH <= 0f || videoW <= 0 || videoH <= 0) return
        val videoAspect = videoW.toFloat() / videoH.toFloat()
        val viewAspect = viewW / viewH
        val scaleX: Float
        val scaleY: Float
        if (videoAspect > viewAspect) {
          scaleX = 1f
          scaleY = viewAspect / videoAspect
        } else {
          scaleX = videoAspect / viewAspect
          scaleY = 1f
        }
        val matrix = android.graphics.Matrix().apply {
          setScale(scaleX, scaleY, viewW / 2f, viewH / 2f)
        }
        textureView.setTransform(matrix)
      }

      if (playbackUri != null) {
        key(playbackUri) {
          AndroidView(
            factory = { ctx ->
              TextureView(ctx).apply {
                isOpaque = true
                var currentVideoWidth = 0
                var currentVideoHeight = 0
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
                      val scheme = playbackUri.scheme.orEmpty().lowercase()
                      if (scheme == "http" || scheme == "https") {
                        val uriText = playbackUri.toString()
                        val headers = buildMap {
                          put(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                          )
                          if (uriText.contains("cdninstagram.com", ignoreCase = true) ||
                            uriText.contains("fbcdn.net", ignoreCase = true)
                          ) {
                            put("Referer", "https://www.instagram.com/")
                          } else if (uriText.contains("pinimg.com", ignoreCase = true)) {
                            put("Referer", "https://www.pinterest.com/")
                          }
                        }
                        player.setDataSource(ctx, playbackUri, headers)
                      } else {
                        player.setDataSource(ctx, playbackUri)
                      }
                      player.isLooping = false
                      player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                      player.setOnVideoSizeChangedListener { _, vw, vh ->
                        currentVideoWidth = vw
                        currentVideoHeight = vh
                        post { applyFitTransform(this@apply, vw, vh) }
                      }
                      player.setOnPreparedListener { mp ->
                        currentVideoWidth = mp.videoWidth
                        currentVideoHeight = mp.videoHeight
                        val dur = mp.duration.toLong()
                        if (dur > 0L) {
                          preparedDurationMs = dur
                        }
                        val vol = if (isAudioMuted) 0f else 1f
                        mp.setVolume(vol, vol)
                        mp.start()
                        post {
                          applyFitTransform(this@apply, mp.videoWidth, mp.videoHeight)
                          isVideoPrepared = true
                        }
                      }
                      player.setOnCompletionListener {
                        onDismiss()
                      }
                      player.setOnErrorListener { _, what, extra ->
                        if (playbackUri.scheme == "file" &&
                          !triedRemoteStreamFallback &&
                          resolvedPlayableHttpUrl.isNotBlank()
                        ) {
                          triedRemoteStreamFallback = true
                          val baseDir = LocalMediaCacheManager.preferredMediaBaseDir(context)
                          SupabaseNetworkClient.evictCachedMedia(baseDir, resolvedPlayableHttpUrl)
                          SupabaseNetworkClient.evictCachedMedia(context.cacheDir, resolvedPlayableHttpUrl)
                          resolvedVideoUri = Uri.parse(resolvedPlayableHttpUrl)
                        } else {
                          val fallbackEmbed = resolvedEmbedUrl
                          if (!fallbackEmbed.isNullOrBlank()) {
                            embedFallbackUrl = fallbackEmbed
                          } else {
                            videoFailed = true
                            onPlaybackMessage("यह वीडियो लिंक प्ले नहीं हो सका (कोड: $what/$extra)।")
                          }
                        }
                        true
                      }
                      player.prepareAsync()
                    }.onFailure { err ->
                      runCatching { player.release() }
                      runCatching { videoSurface.release() }
                      if (mediaPlayerRef === player) mediaPlayerRef = null
                      if (surfaceRef === videoSurface) surfaceRef = null
                      val fallbackEmbed = resolvedEmbedUrl
                      if (!fallbackEmbed.isNullOrBlank()) {
                        embedFallbackUrl = fallbackEmbed
                      } else {
                        videoFailed = true
                        onPlaybackMessage("वीडियो चलाने में त्रुटि: ${err.message ?: "असमर्थित लिंक"}")
                      }
                    }
                  }

                  override fun onSurfaceTextureSizeChanged(
                    surfaceTexture: SurfaceTexture,
                    width: Int,
                    height: Int,
                  ) {
                    applyFitTransform(this@apply, currentVideoWidth, currentVideoHeight)
                  }

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
            modifier = Modifier
              .fillMaxSize()
              .alpha(if (isVideoPrepared) 1f else 0f),
          )
        }
      }

      // Watchdog: if MediaPlayer stalls preparing for >25s, fall back to Instagram embed or error cleanly
      LaunchedEffect(event.id, playbackUri, isVideoPrepared) {
        if (playbackUri != null && !isVideoPrepared) {
          delay(25_000L)
          if (!isVideoPrepared) {
            runCatching { mediaPlayerRef?.stop() }
            runCatching { mediaPlayerRef?.release() }
            mediaPlayerRef = null
            val fallbackEmbed = resolvedEmbedUrl
            if (!fallbackEmbed.isNullOrBlank()) {
              embedFallbackUrl = fallbackEmbed
            } else {
              videoFailed = true
              onPlaybackMessage("वीडियो लोड होने में अधिक समय लगा — कृपया दूसरा लिंक उपयोग करें।")
            }
          }
        }
      }

      LaunchedEffect(isAudioMuted, mediaPlayerRef) {
        val vol = if (isAudioMuted) 0f else 1f
        runCatching { mediaPlayerRef?.setVolume(vol, vol) }
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
        val fallbackImageModel = when {
          event.giftMediaUrl.isNotBlank() && event.giftMediaType == "image" -> event.giftMediaUrl
          fallbackThumbUrl.isNotBlank() -> fallbackThumbUrl
          else -> ""
        }
        if (fallbackImageModel.isNotBlank()) {
          AsyncImage(
            model = fallbackImageModel,
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

    if (isOverlayVisible && (isVideoGift || isEmbedMode) && onToggleAudioMute != null) {
      IconButton(
        onClick = onToggleAudioMute,
        modifier = Modifier
          .align(Alignment.TopStart)
          .statusBarsPadding()
          .padding(16.dp)
          .minimumInteractiveComponentSize()
          .size(48.dp)
          .clip(CircleShape)
          .background(Color(0xA6000000))
          .border(1.dp, if (isAudioMuted) SwarCoral else SwarCyan, CircleShape)
          .testTag("entry_video_mute_toggle"),
      ) {
        Icon(
          imageVector = if (isAudioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
          contentDescription = if (isAudioMuted) "एंट्री वीडियो आवाज़ चालू करें" else "एंट्री वीडियो आवाज़ म्यूट करें",
          tint = if (isAudioMuted) SwarCoral else Color.White,
        )
      }
    }

    if (isOverlayVisible && event.giftId.startsWith("entry_lv_")) {
      val parsedBadgeLevel = event.giftId.removePrefix("entry_lv_").toIntOrNull() ?: 1
      val displayEntryLevel = (senderExactLevel ?: parsedBadgeLevel).coerceIn(1, 120)
      val bannerScale = remember(event.id) { Animatable(0.84f) }
      LaunchedEffect(event.id, isOverlayVisible) {
        bannerScale.animateTo(
          targetValue = 1f,
          animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.62f,
            stiffness = 260f,
          ),
        )
      }
      RoyalGoldenEntryBanner(
        senderName = event.senderName,
        senderAvatarUrl = event.senderAvatarUrl,
        entryLevel = displayEntryLevel,
        frameUrl = senderFrameUrl,
        compact = false,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .statusBarsPadding()
          .padding(top = 66.dp, start = 10.dp, end = 10.dp)
          .fillMaxWidth()
          .widthIn(max = 500.dp)
          .scale(bannerScale.value)
          .testTag("entry_video_royal_banner"),
      )
    }
  }
}
