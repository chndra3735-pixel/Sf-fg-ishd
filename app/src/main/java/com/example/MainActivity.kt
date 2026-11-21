package com.example

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.audio.VoiceAudioEngine
import com.example.data.repository.SupabaseNetworkClient
import com.example.data.repository.SupabaseStorageRepository
import com.example.data.repository.SwarRoomRepository
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.components.AppUpdateAvailableDialog
import com.example.ui.components.CreateMomentDialog
import com.example.ui.components.CreateRoomDialog
import com.example.ui.components.MinimizedRoomFloatingBar
import com.example.ui.screens.GuestPreviewSwarRoomApp
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.MomentsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.VoiceRoomScreen
import com.example.ui.theme.SwarCoral
import com.example.ui.theme.SwarCyan
import com.example.ui.theme.SwarDeepNight
import com.example.ui.theme.SwarRoomTheme
import com.example.ui.theme.SwarStageIndigo
import com.example.ui.theme.SwarViolet
import com.example.ui.viewmodel.SwarRoomViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class MainTab(val route: String) {
  ROOMS("rooms"),
  MOMENTS("moments"),
  MESSAGES("messages"),
  PROFILE("profile"),
}

private fun ensureFirebaseAuth(context: Context): FirebaseAuth? {
  return runCatching {
    val appReady = if (FirebaseApp.getApps(context).isNotEmpty()) {
      true
    } else {
      FirebaseApp.initializeApp(context) != null
    }
    if (appReady) Firebase.auth else null
  }.getOrNull()
}

class MainActivity : ComponentActivity() {
  private var isSeatServiceBound = false
  private val seatServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
    override fun onServiceDisconnected(name: ComponentName?) {
      isSeatServiceBound = false
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val cleanupIntent = Intent(this, SeatCleanupService::class.java)
    isSeatServiceBound = runCatching {
      bindService(cleanupIntent, seatServiceConnection, Context.BIND_AUTO_CREATE)
    }.getOrDefault(false)

    Coil.setImageLoader {
      ImageLoader.Builder(applicationContext)
        .okHttpClient { SupabaseNetworkClient.sharedOkHttpClient }
        .components {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(ImageDecoderDecoder.Factory())
          } else {
            add(GifDecoder.Factory())
          }
        }
        .build()
    }
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(
        android.graphics.Color.TRANSPARENT,
        android.graphics.Color.TRANSPARENT,
      ),
      navigationBarStyle = SystemBarStyle.light(
        android.graphics.Color.TRANSPARENT,
        android.graphics.Color.TRANSPARENT,
      ),
    )
    setContent {
      SwarRoomTheme {
        SwarRoomAppRoot()
      }
    }
  }

  override fun onDestroy() {
    if (!isChangingConfigurations) {
      SeatPresenceTracker.cleanupSynchronously(applicationContext, timeoutMs = 900L)
    }
    if (isSeatServiceBound) {
      runCatching { unbindService(seatServiceConnection) }
      isSeatServiceBound = false
    }
    super.onDestroy()
  }
}

@Composable
fun SwarRoomAppRoot(
  firebaseAuthProvider: (Context) -> FirebaseAuth? = ::ensureFirebaseAuth,
) {
  val context = LocalContext.current
  var retryCount by remember { mutableIntStateOf(0) }
  val auth = remember(context, retryCount, firebaseAuthProvider) { firebaseAuthProvider(context) }

  if (auth == null) {
    FirebaseSetupRequiredScreen(onRetry = { retryCount++ })
    return
  }

  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

  val user = currentUser
  if (user == null) {
    GuestPreviewSwarRoomApp(
      isSupabaseConfigured = remember(context) {
        SupabaseStorageRepository(context.applicationContext).isConfigured()
      },
    )
  } else {
    key(user.uid) {
      AuthenticatedSwarRoomApp(user = user)
    }
  }
}

@Composable
private fun FirebaseSetupRequiredScreen(
  onRetry: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(SwarDeepNight, SwarStageIndigo, Color(0xFF0E0820))
        )
      )
      .safeDrawingPadding()
      .padding(horizontal = 24.dp, vertical = 20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 460.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(Brush.linearGradient(listOf(SwarCoral, SwarViolet))),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.CloudOff,
          contentDescription = "Firebase कॉन्फ़िगरेशन आवश्यक",
          tint = Color.White,
          modifier = Modifier.size(36.dp),
        )
      }

      Text(
        text = "Firebase सेटअप अधूरा है",
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White,
        textAlign = TextAlign.Center,
      )

      Text(
        text = "इस APK को बनाते समय google-services.json फ़ाइल शामिल नहीं हो सकी, इसलिए ऐप ने क्रैश होने से पहले सुरक्षित रूप से रोका है।",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFFC5BFE2),
        textAlign = TextAlign.Center,
      )

      Surface(
        color = Color(0x22FFFFFF),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "सही APK बनाने के 3 आसान चरण:",
            style = MaterialTheme.typography.titleSmall,
            color = SwarCyan,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "1. GitHub रिपॉजिटरी के Settings → Secrets and variables → Actions खोलें।",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
          )
          Text(
            text = "2. GOOGLE_SERVICES_JSON नाम का Secret जोड़कर अपने प्रोजेक्ट की google-services.json फ़ाइल पेस्ट करें।",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
          )
          Text(
            text = "3. GitHub Actions दोबारा चलाएं और नया SwarRoom.apk इंस्टॉल करें।",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
          )
        }
      }

      Button(
        onClick = onRetry,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("retry_firebase_init_button"),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = SwarCyan,
          contentColor = SwarDeepNight,
        ),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "पुनः जांचें",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun AuthenticatedSwarRoomApp(user: FirebaseUser) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) {
    runCatching { CredentialManager.create(context) }.getOrNull()
  }

  val viewModel: SwarRoomViewModel = viewModel(
    key = user.uid,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        SwarRoomViewModel(
          repository = SwarRoomRepository(db, Firebase.auth),
          supabaseStorage = SupabaseStorageRepository(app),
          audioEngine = VoiceAudioEngine(app),
          currentUserId = user.uid,
          initialDisplayName = user.displayName.orEmpty(),
          initialAvatarUrl = user.photoUrl?.toString().orEmpty(),
          currentUserEmail = user.email.orEmpty(),
          isEmailVerified = user.isEmailVerified,
          appContext = app,
        )
      }
    },
  )

  val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
  val roomsState by viewModel.rooms.collectAsStateWithLifecycle()
  val momentsState by viewModel.moments.collectAsStateWithLifecycle()
  val conversationsState by viewModel.conversations.collectAsStateWithLifecycle()
  val activeRoom by viewModel.activeRoom.collectAsStateWithLifecycle()
  val activeRoomSeats by viewModel.activeRoomSeats.collectAsStateWithLifecycle()
  val activeRoomMessages by viewModel.activeRoomMessages.collectAsStateWithLifecycle()
  val emojiCatalog by viewModel.emojiCatalog.collectAsStateWithLifecycle()
  val giftCatalog by viewModel.giftCatalog.collectAsStateWithLifecycle()
  val latestMarqueeGift by viewModel.latestMarqueeGift.collectAsStateWithLifecycle()
  val activeFullScreenGift by viewModel.activeFullScreenGift.collectAsStateWithLifecycle()
  val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
  val activeDirectMessages by viewModel.activeDirectMessages.collectAsStateWithLifecycle()
  val isUploadingMedia by viewModel.isUploadingMedia.collectAsStateWithLifecycle()
  val isMicMuted by viewModel.isMicMuted.collectAsStateWithLifecycle()
  val isSpeakerOn by viewModel.isSpeakerOn.collectAsStateWithLifecycle()
  val isMonitorLoopbackOn by viewModel.isMonitorLoopbackOn.collectAsStateWithLifecycle()
  val micAudioLevel by viewModel.micAudioLevel.collectAsStateWithLifecycle()
  val isRoomMinimized by viewModel.isRoomMinimized.collectAsStateWithLifecycle()
  val memberProfiles by viewModel.memberProfiles.collectAsStateWithLifecycle()
  val blockedUserIds by viewModel.blockedUserIds.collectAsStateWithLifecycle()
  val isAgoraConnected by viewModel.isAgoraConnected.collectAsStateWithLifecycle()
  val agoraStatusText by viewModel.agoraStatusText.collectAsStateWithLifecycle()
  val giftUploadError by viewModel.giftUploadError.collectAsStateWithLifecycle()
  val emojiUploadError by viewModel.emojiUploadError.collectAsStateWithLifecycle()
  val levelConfigs by viewModel.levelConfigs.collectAsStateWithLifecycle()
  val claimedLevelTiers by viewModel.claimedLevelTiers.collectAsStateWithLifecycle()
  val activeRoomPresence by viewModel.activeRoomPresence.collectAsStateWithLifecycle()
  val configuredGithubRepo by viewModel.configuredGithubRepo.collectAsStateWithLifecycle()
  val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()
  val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
  val updateCheckError by viewModel.updateCheckError.collectAsStateWithLifecycle()
  val showUpdatePrompt by viewModel.showUpdatePrompt.collectAsStateWithLifecycle()

  var currentTab by remember { mutableStateOf(MainTab.ROOMS) }
  var showCreateRoomDialog by remember { mutableStateOf(false) }
  var showCreateMomentDialog by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val miniBarAudioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { granted ->
      if (granted) viewModel.onAudioPermissionGranted()
      else scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.mic_permission_needed)) }
    },
  )

  LaunchedEffect(viewModel) {
    viewModel.events.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  val currentRoom = activeRoom
  val inVoiceRoom = currentRoom != null && !isRoomMinimized
  LaunchedEffect(inVoiceRoom, context) {
    val activity = context as? ComponentActivity ?: return@LaunchedEffect
    if (inVoiceRoom) {
      activity.enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      )
    } else {
      activity.enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.light(
          android.graphics.Color.TRANSPARENT,
          android.graphics.Color.TRANSPARENT,
        ),
        navigationBarStyle = SystemBarStyle.light(
          android.graphics.Color.TRANSPARENT,
          android.graphics.Color.TRANSPARENT,
        ),
      )
    }
  }

  if (inVoiceRoom && currentRoom != null) {
    Box(modifier = Modifier.fillMaxSize()) {
      val userProfile = (profileState as? UiState.Success)?.data
      val coins = userProfile?.coins ?: 1500L
      VoiceRoomScreen(
        room = currentRoom,
        seats = activeRoomSeats,
        messages = activeRoomMessages,
        currentUserId = user.uid,
        isAdminUser = viewModel.isAdminUser,
        isUploadingMedia = isUploadingMedia,
        userCoins = coins,
        emojiCatalog = emojiCatalog,
        giftCatalog = giftCatalog,
        latestMarqueeGift = latestMarqueeGift,
        activeFullScreenGift = activeFullScreenGift,
        isMicMuted = isMicMuted,
        isSpeakerOn = isSpeakerOn,
        isMonitorLoopbackOn = isMonitorLoopbackOn,
        micAudioLevel = micAudioLevel,
        onLeaveRoom = { viewModel.leaveActiveRoom() },
        onSeatTapped = { seat, hasPerm, requestPerm ->
          viewModel.onSeatTapped(seat, hasPerm, requestPerm)
        },
        onLeaveSeat = { viewModel.leaveMySeat() },
        onToggleSeatLock = { viewModel.toggleSeatLock(it) },
        onToggleMic = { hasPerm, requestPerm -> viewModel.toggleMic(hasPerm, requestPerm) },
        onAudioPermissionGranted = { viewModel.onAudioPermissionGranted() },
        onToggleSpeaker = { viewModel.toggleSpeaker() },
        onToggleMonitorLoopback = { viewModel.toggleMonitorLoopback() },
        onSendMessage = { text, type -> viewModel.sendRoomText(text, type) },
        onSendEmoji = { item -> viewModel.sendEmojiReaction(item) },
        onAddCustomEmoji = { label, category, symbol, imageUri, emojiLinkUrl, onDone ->
          viewModel.addCustomEmoji(label, category, symbol, imageUri, emojiLinkUrl, onDone)
        },
        onUpdateCustomEmoji = { existing, label, category, symbol, newImageUri, newEmojiLinkUrl, removeExisting, onDone ->
          viewModel.updateCustomEmoji(
            existing = existing,
            label = label,
            category = category,
            symbol = symbol,
            newImageUri = newImageUri,
            newEmojiLinkUrl = newEmojiLinkUrl,
            removeExistingImage = removeExisting,
            onSuccess = onDone,
          )
        },
        onDeleteCustomEmoji = { item, onDone ->
          viewModel.deleteCustomEmoji(item, onDone)
        },
        onSendGift = { gift, recipientUid, recipientName, quantity ->
          viewModel.sendGift(gift, recipientUid, recipientName, quantity)
        },
        onAddCustomGift = { name, category, emoji, coinCost, thumbUri, dpLinkUrl, videoLinkUrl, onDone ->
          viewModel.addCustomGift(name, category, emoji, coinCost, thumbUri, dpLinkUrl, videoLinkUrl, onDone)
        },
        onUpdateCustomGift = { existing, name, category, emoji, coinCost, newThumbUri, newDpLinkUrl, newVideoLinkUrl, removeThumb, removeVideo, onDone ->
          viewModel.updateCustomGift(
            existing = existing,
            name = name,
            category = category,
            emoji = emoji,
            coinCost = coinCost,
            newThumbnailUri = newThumbUri,
            newDpLinkUrl = newDpLinkUrl,
            newVideoLinkUrl = newVideoLinkUrl,
            removeExistingThumbnail = removeThumb,
            removeExistingVideo = removeVideo,
            onSuccess = onDone,
          )
        },
        onDeleteCustomGift = { gift, onDone ->
          viewModel.deleteCustomGift(gift, onDone)
        },
        onDismissFullScreenGift = { viewModel.dismissFullScreenGift() },
        onClearMarqueeGift = { viewModel.clearMarqueeGift(it) },
        onClaimBonusCoins = { viewModel.claimDailyBonusCoins() },
        onUpdateRoomSettings = { title, theme, notice ->
          viewModel.updateRoomSettings(title, theme, notice)
        },
        onStartDirectChat = { otherUid, otherName, otherAvatar ->
          viewModel.startDirectChatWith(otherUid, otherName, otherAvatar) {
            viewModel.leaveActiveRoom()
            currentTab = MainTab.MESSAGES
          }
        },
        onShowToast = { msg ->
          scope.launch { snackbarHostState.showSnackbar(msg) }
        },
        currentUserName = userProfile?.displayName?.ifBlank { user.displayName.orEmpty() } ?: user.displayName.orEmpty(),
        currentUserAvatar = userProfile?.avatarUrl?.ifBlank { user.photoUrl?.toString().orEmpty() } ?: user.photoUrl?.toString().orEmpty(),
        onToggleSeatMute = { seat, hasPerm, requestPerm ->
          viewModel.toggleSeatMute(seat, hasPerm, requestPerm)
        },
        onMinimizeRoom = { viewModel.minimizeActiveRoom() },
        userProfiles = memberProfiles,
        blockedUserIds = blockedUserIds,
        onRequestUserProfile = { uid -> viewModel.loadUserProfile(uid) },
        onBlockUser = { uid, name -> viewModel.blockUser(uid, name) },
        onUnblockUser = { uid, name -> viewModel.unblockUser(uid, name) },
        onKickSeatOccupant = { seat -> viewModel.kickSeatOccupant(seat) },
        onSitOnAvailableSeat = { hasPerm, requestPerm ->
          viewModel.sitOnFirstAvailableSeat(hasPerm, requestPerm)
        },
        isAgoraConnected = isAgoraConnected,
        agoraStatusText = agoraStatusText,
        giftUploadError = giftUploadError,
        onClearGiftUploadError = { viewModel.clearGiftUploadError() },
        emojiUploadError = emojiUploadError,
        onClearEmojiUploadError = { viewModel.clearEmojiUploadError() },
        levelConfigs = levelConfigs,
        claimedLevelTiers = claimedLevelTiers,
        onSaveLevelTierConfig = { lvl, vidLink, vidUri, frLink, frUri, coinsReward, bdgLink, bdgUri, onDone ->
          viewModel.saveLevelTierConfig(lvl, vidLink, vidUri, frLink, frUri, coinsReward, bdgLink, bdgUri, onDone)
        },
        onClaimLevelTierReward = { lvl -> viewModel.claimLevelTierReward(lvl) },
        livePresenceMembers = activeRoomPresence,
      )
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
          .align(androidx.compose.ui.Alignment.TopCenter)
          .padding(top = 64.dp),
      )
    }
    return
  }

  val navItemColors = NavigationBarItemDefaults.colors(
    selectedIconColor = Color(0xFF008CE6),
    selectedTextColor = Color(0xFF008CE6),
    indicatorColor = Color(0xFFE1F3FF),
    unselectedIconColor = Color(0xFF647087),
    unselectedTextColor = Color(0xFF647087),
  )

  Scaffold(
    containerColor = Color.White,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    bottomBar = {
      Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = Color(0xFFE7EFF7), thickness = 1.dp)
        NavigationBar(
          containerColor = Color.White,
          tonalElevation = 0.dp,
        ) {
          NavigationBarItem(
            selected = currentTab == MainTab.ROOMS,
            onClick = { currentTab = MainTab.ROOMS },
            colors = navItemColors,
            icon = {
              Icon(
                imageVector = if (currentTab == MainTab.ROOMS) Icons.Filled.Home else Icons.Outlined.Home,
                contentDescription = null,
              )
            },
            label = { Text(stringResource(id = R.string.nav_rooms)) },
            modifier = Modifier.testTag("nav_tab_rooms"),
          )
          NavigationBarItem(
            selected = currentTab == MainTab.MOMENTS,
            onClick = { currentTab = MainTab.MOMENTS },
            colors = navItemColors,
            icon = {
              Icon(
                imageVector = if (currentTab == MainTab.MOMENTS) Icons.Filled.Explore else Icons.Outlined.Explore,
                contentDescription = null,
              )
            },
            label = { Text(stringResource(id = R.string.nav_moments)) },
            modifier = Modifier.testTag("nav_tab_moments"),
          )
          NavigationBarItem(
            selected = currentTab == MainTab.MESSAGES,
            onClick = { currentTab = MainTab.MESSAGES },
            colors = navItemColors,
            icon = {
              Icon(
                imageVector = if (currentTab == MainTab.MESSAGES) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                contentDescription = null,
              )
            },
            label = { Text(stringResource(id = R.string.nav_messages)) },
            modifier = Modifier.testTag("nav_tab_messages"),
          )
          NavigationBarItem(
            selected = currentTab == MainTab.PROFILE,
            onClick = { currentTab = MainTab.PROFILE },
            colors = navItemColors,
            icon = {
              Icon(
                imageVector = if (currentTab == MainTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                contentDescription = null,
              )
            },
            label = { Text(stringResource(id = R.string.nav_profile)) },
            modifier = Modifier.testTag("nav_tab_profile"),
          )
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      val myOwnedRoom = (roomsState as? UiState.Success)?.data?.firstOrNull { it.ownerId == user.uid }
      when (currentTab) {
        MainTab.ROOMS -> LobbyScreen(
          roomsState = roomsState,
          currentUserId = user.uid,
          onRoomClick = { room -> viewModel.enterRoom(room.id) },
          onCreateRoomClick = {
            if (myOwnedRoom != null) {
              viewModel.enterRoom(myOwnedRoom.id)
            } else {
              showCreateRoomDialog = true
            }
          },
        )
        MainTab.MOMENTS -> MomentsScreen(
          momentsState = momentsState,
          currentUserId = user.uid,
          isSupabaseConfigured = viewModel.isSupabaseConfigured,
          onAddMomentClick = { showCreateMomentDialog = true },
          onLikeMoment = { viewModel.likeMoment(it) },
          onDeleteMoment = { viewModel.deleteMoment(it) },
        )
        MainTab.MESSAGES -> MessagesScreen(
          conversationsState = conversationsState,
          activeConversationId = activeConversationId,
          directMessages = activeDirectMessages,
          currentUserId = user.uid,
          onOpenConversation = { viewModel.openConversation(it) },
          onCloseConversation = { viewModel.closeConversation() },
          onSendDirectMessage = { convo, text -> viewModel.sendDirectMessage(convo, text) },
        )
        MainTab.PROFILE -> ProfileScreen(
          profileState = profileState,
          isUploadingMedia = isUploadingMedia,
          isSupabaseConfigured = viewModel.isSupabaseConfigured,
          onUploadAvatarDp = { uri -> viewModel.uploadAndSaveAvatarDp(uri) },
          onUpdateProfile = { name, handle, bio, gender, club ->
            viewModel.updateProfileDetails(name, handle, bio, gender, club)
          },
          onSignOut = {
            viewModel.leaveActiveRoom {
              signOut(credentialManager, scope)
            }
          },
          onClaimBonusCoins = { viewModel.claimDailyBonusCoins() },
          giftCatalog = giftCatalog,
          levelConfigs = levelConfigs,
          claimedLevelTiers = claimedLevelTiers,
          isAdminUser = viewModel.isAdminUser,
          onSaveLevelTierConfig = { lvl, vidLink, vidUri, frLink, frUri, coinsReward, bdgLink, bdgUri, onDone ->
            viewModel.saveLevelTierConfig(lvl, vidLink, vidUri, frLink, frUri, coinsReward, bdgLink, bdgUri, onDone)
          },
          onClaimLevelTierReward = { lvl -> viewModel.claimLevelTierReward(lvl) },
          configuredGithubRepo = configuredGithubRepo,
          appUpdateInfo = appUpdateInfo,
          isCheckingUpdate = isCheckingUpdate,
          updateCheckError = updateCheckError,
          onCheckForAppUpdate = { viewModel.checkForAppUpdate(manual = true) },
          onSaveGithubRepoLink = { link -> viewModel.saveGithubRepoLink(link) },
        )
      }

      if (currentRoom != null && isRoomMinimized) {
        MinimizedRoomFloatingBar(
          room = currentRoom,
          isMicMuted = isMicMuted,
          onExpand = { viewModel.expandActiveRoom() },
          onToggleMic = {
            val hasPerm = ContextCompat.checkSelfPermission(
              context,
              Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            viewModel.toggleMic(hasPerm) {
              miniBarAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
          },
          onExitRoom = { viewModel.leaveActiveRoom() },
          modifier = Modifier.align(Alignment.BottomCenter),
        )
      }
    }
  }

  val ownedRoom = (roomsState as? UiState.Success)?.data?.firstOrNull { it.ownerId == user.uid }
  if (showCreateRoomDialog && ownedRoom == null) {
    CreateRoomDialog(
      isUploading = isUploadingMedia,
      onDismiss = { showCreateRoomDialog = false },
      onCreate = { title, category, theme, clubTag, welcome, seatsCount, coverUri ->
        viewModel.createRoom(
          title = title,
          category = category,
          backgroundTheme = theme,
          clubTag = clubTag,
          welcomeNotice = welcome,
          seatCount = seatsCount,
          coverImageUri = coverUri,
          onRoomCreated = { showCreateRoomDialog = false },
        )
      },
    )
  }

  if (showCreateMomentDialog) {
    CreateMomentDialog(
      isUploading = isUploadingMedia,
      onDismiss = { showCreateMomentDialog = false },
      onSubmit = { uri, isVideo, caption ->
        viewModel.uploadMomentPost(uri, isVideo, caption)
        showCreateMomentDialog = false
      },
    )
  }

  val availableUpdate = appUpdateInfo
  if (showUpdatePrompt && availableUpdate != null && availableUpdate.hasUpdate) {
    AppUpdateAvailableDialog(
      updateInfo = availableUpdate,
      onDismiss = { viewModel.dismissUpdatePrompt() },
    )
  }
}
