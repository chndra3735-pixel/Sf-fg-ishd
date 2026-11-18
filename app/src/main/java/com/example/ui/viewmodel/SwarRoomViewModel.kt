package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SeatCleanupService
import com.example.SeatPresenceTracker
import com.example.audio.VoiceAudioEngine
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.EmojiCatalogItem
import com.example.data.model.GiftCatalogItem
import com.example.data.model.GiftEvent
import com.example.data.model.LevelTierConfig
import com.example.data.model.MomentPost
import com.example.data.model.RoomMessage
import com.example.data.model.RoomPresenceMember
import com.example.data.model.RoomSeat
import com.example.data.model.SwarCatalogDefaults
import com.example.data.model.SwarUserIdentity
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
import com.example.data.repository.MediaCompressor
import com.example.data.repository.SupabaseNetworkClient
import com.example.data.repository.SupabaseStorageRepository
import com.example.data.repository.SwarRoomRepository
import com.example.data.repository.VideoLinkResolution
import com.example.data.repository.VideoLinkResolver
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class SwarRoomViewModel(
  private val repository: SwarRoomRepository,
  private val supabaseStorage: SupabaseStorageRepository,
  private val audioEngine: VoiceAudioEngine,
  val currentUserId: String,
  private val initialDisplayName: String,
  private val initialAvatarUrl: String,
  val currentUserEmail: String = "",
  val isEmailVerified: Boolean = false,
  private val appContext: Context? = null,
) : ViewModel() {

  val isAdminUser: Boolean =
    currentUserEmail.equals(ADMIN_EMAIL, ignoreCase = true) && isEmailVerified

  private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val events: SharedFlow<String> = _events.asSharedFlow()

  private val _isUploadingMedia = MutableStateFlow(false)
  val isUploadingMedia: StateFlow<Boolean> = _isUploadingMedia.asStateFlow()

  private val _giftUploadError = MutableStateFlow<String?>(null)
  val giftUploadError: StateFlow<String?> = _giftUploadError.asStateFlow()

  fun clearGiftUploadError() {
    _giftUploadError.value = null
  }

  val isSupabaseConfigured: Boolean
    get() = supabaseStorage.isConfigured()

  val userProfile: StateFlow<UiState<UserProfile?>> =
    repository.observeUserProfile(currentUserId)
      .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
      .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "प्रोफ़ाइल लोड नहीं हो सकी"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val rooms: StateFlow<UiState<List<VoiceRoom>>> =
    repository.observePublicRooms()
      .map<List<VoiceRoom>, UiState<List<VoiceRoom>>> { UiState.Success(it) }
      .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "वॉइस रूम लोड नहीं हो सके"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  val moments: StateFlow<UiState<List<MomentPost>>> =
    repository.observeMoments()
      .map<List<MomentPost>, UiState<List<MomentPost>>> { UiState.Success(it) }
      .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "मोमेंट्स लोड नहीं हो सके"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  private val blockPrefs by lazy {
    appContext?.applicationContext?.getSharedPreferences("swarroom_blocks_$currentUserId", Context.MODE_PRIVATE)
  }

  private val _blockedUserIds = MutableStateFlow(
    appContext?.applicationContext
      ?.getSharedPreferences("swarroom_blocks_$currentUserId", Context.MODE_PRIVATE)
      ?.getStringSet(KEY_BLOCKED_UIDS, emptySet())
      ?.toSet()
      ?: emptySet()
  )
  val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

  private val _memberProfiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
  val memberProfiles: StateFlow<Map<String, UserProfile>> = _memberProfiles.asStateFlow()

  val conversations: StateFlow<UiState<List<Conversation>>> =
    combine(
      repository.observeConversations(),
      _blockedUserIds,
    ) { list, blocked ->
      list.filter { convo ->
        val otherUids = convo.participantUids.filter { it != currentUserId }
        otherUids.none { it in blocked }
      }
    }
      .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
      .catch { error -> emit(UiState.Error(unwrapFirestoreMessage(error, "संदेश लोड नहीं हो सके"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

  private val catalogHiddenPrefs by lazy {
    appContext?.applicationContext?.getSharedPreferences("swarroom_catalog_hidden_$currentUserId", Context.MODE_PRIVATE)
  }

  private val _hiddenEmojiIds = MutableStateFlow(
    appContext?.applicationContext
      ?.getSharedPreferences("swarroom_catalog_hidden_$currentUserId", Context.MODE_PRIVATE)
      ?.getStringSet(KEY_HIDDEN_EMOJIS, emptySet())
      ?.toSet()
      ?: emptySet()
  )

  private val _hiddenGiftIds = MutableStateFlow(
    appContext?.applicationContext
      ?.getSharedPreferences("swarroom_catalog_hidden_$currentUserId", Context.MODE_PRIVATE)
      ?.getStringSet(KEY_HIDDEN_GIFTS, emptySet())
      ?.toSet()
      ?: emptySet()
  )

  // --- Emoji & Gift Catalogs (built-in 4 categories + admin-uploaded Firestore items) ---
  val emojiCatalog: StateFlow<List<EmojiCatalogItem>> =
    combine(
      repository.observeEmojiCatalog().catch { emit(emptyList()) },
      _hiddenEmojiIds,
    ) { remote, hiddenIds ->
      (remote.reversed() + SwarCatalogDefaults.builtInEmojis).filterNot { it.id in hiddenIds }
    }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SwarCatalogDefaults.builtInEmojis)

  val giftCatalog: StateFlow<List<GiftCatalogItem>> =
    combine(
      repository.observeGiftCatalog().catch { emit(emptyList()) },
      _hiddenGiftIds,
    ) { remote, hiddenIds ->
      (remote.reversed() + SwarCatalogDefaults.builtInGifts).filterNot { it.id in hiddenIds }
    }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SwarCatalogDefaults.builtInGifts)

  // --- Per-Level Rewards (Entry Video, Animated Frame, Coins Bonus, Profile Badge) ---
  val levelConfigs: StateFlow<Map<Int, LevelTierConfig>> =
    repository.observeLevelConfigs()
      .catch { emit(emptyMap()) }
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

  private val _claimedLevelTiers = MutableStateFlow<Set<Int>>(emptySet())
  val claimedLevelTiers: StateFlow<Set<Int>> = _claimedLevelTiers.asStateFlow()
  private var hasLoadedLevelClaims = false
  private val claimingTiersInFlight = mutableSetOf<Int>()
  private var lastRemoteGiftSnapshot: Map<String, GiftCatalogItem> = emptyMap()

  // --- Global Gift Marquee & Full-Screen Gift Playback ---
  private val _latestMarqueeGift = MutableStateFlow<GiftEvent?>(null)
  val latestMarqueeGift: StateFlow<GiftEvent?> = _latestMarqueeGift.asStateFlow()

  private val _activeFullScreenGift = MutableStateFlow<GiftEvent?>(null)
  val activeFullScreenGift: StateFlow<GiftEvent?> = _activeFullScreenGift.asStateFlow()

  private val seenGiftEventIds = mutableSetOf<String>()
  private val pendingGiftQueue = ArrayDeque<GiftEvent>()
  private var pendingMarqueeGift: GiftEvent? = null
  private var initialGiftSnapshotHandled = false

  // --- Active Voice Room ---
  private val _activeRoomId = MutableStateFlow<String?>(null)
  val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

  private val _roomJoinedAtMs = MutableStateFlow(0L)
  private val _presenceClockMs = MutableStateFlow(System.currentTimeMillis())

  @Volatile
  private var serverClockOffsetMs: Long = 0L

  @Volatile
  private var sawValidatedInternet: Boolean = false

  private var registeredConnectivityManager: ConnectivityManager? = null
  private var registeredNetworkCallback: ConnectivityManager.NetworkCallback? = null

  private val _isRoomMinimized = MutableStateFlow(false)
  val isRoomMinimized: StateFlow<Boolean> = _isRoomMinimized.asStateFlow()

  val activeRoom: StateFlow<VoiceRoom?> =
    _activeRoomId.flatMapLatest { roomId ->
      if (roomId == null) flowOf(null)
      else repository.observeRoom(roomId).catch { emit(null) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

  val activeRoomSeats: StateFlow<List<RoomSeat>> =
    _activeRoomId.flatMapLatest { roomId ->
      if (roomId == null) flowOf(emptyList())
      else repository.observeRoomSeats(roomId).catch { emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  private val rawRoomPresence: StateFlow<List<RoomPresenceMember>> =
    _activeRoomId.flatMapLatest { roomId ->
      if (roomId == null) flowOf(emptyList())
      else repository.observeRoomPresence(roomId).catch { emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val activeRoomPresence: StateFlow<List<RoomPresenceMember>> =
    combine(
      rawRoomPresence,
      _presenceClockMs,
      _blockedUserIds,
      _activeRoomId,
    ) { members, clockNow, blocked, activeId ->
      if (activeId == null) {
        emptyList()
      } else {
        val ownDoc = members.firstOrNull { it.userId == currentUserId }
        ownDoc?.lastSeenAt?.toDate()?.time?.let { ownServerMs ->
          serverClockOffsetMs = ownServerMs - System.currentTimeMillis()
        }
        val estimatedServerNow = clockNow + serverClockOffsetMs
        members.filter { member ->
          member.userId.isNotBlank() &&
            member.userId !in blocked &&
            (member.userId == currentUserId ||
              isTimestampFresh(member.lastSeenAt?.toDate()?.time, estimatedServerNow, PRESENCE_STALE_MS))
        }
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val activeRoomMessages: StateFlow<List<RoomMessage>> =
    combine(
      _activeRoomId.flatMapLatest { roomId ->
        if (roomId == null) flowOf(emptyList())
        else repository.observeRoomMessages(roomId).catch { emit(emptyList()) }
      },
      _blockedUserIds,
      activeRoomPresence,
      activeRoomSeats,
      _roomJoinedAtMs,
    ) { msgs, blocked, livePresence, seats, joinedAtMs ->
      val liveUids = buildSet {
        if (_activeRoomId.value != null) add(currentUserId)
        livePresence.forEach { if (it.userId.isNotBlank()) add(it.userId) }
        seats.forEach { if (it.occupantUid.isNotBlank()) add(it.occupantUid) }
      }
      filterLiveRoomMessages(
        messages = msgs,
        blockedUids = blocked,
        liveMemberUids = liveUids,
        roomJoinedAtMs = joinedAtMs,
      )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  // --- Active Direct Conversation ---
  private val _activeConversationId = MutableStateFlow<String?>(null)
  val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

  val activeDirectMessages: StateFlow<List<DirectMessage>> =
    _activeConversationId.flatMapLatest { convoId ->
      if (convoId == null) flowOf(emptyList())
      else repository.observeDirectMessages(convoId).catch { emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  // --- Live Voice Mic & Agora Audio State ---
  val isAgoraConfigured: Boolean
    get() = audioEngine.isAgoraConfigured
  val isAgoraConnected: StateFlow<Boolean> = audioEngine.isAgoraConnected
  val agoraStatusText: StateFlow<String> = audioEngine.agoraStatusText
  val micAudioLevel: StateFlow<Float> = audioEngine.audioLevel
  val isLocalSpeaking: StateFlow<Boolean> = audioEngine.isSpeaking
  val isMicCapturing: StateFlow<Boolean> = audioEngine.isCapturing

  private val _isMicMuted = MutableStateFlow(true)
  val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

  private val _isSpeakerOn = MutableStateFlow(true)
  val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

  private val _isMonitorLoopbackOn = MutableStateFlow(false)
  val isMonitorLoopbackOn: StateFlow<Boolean> = _isMonitorLoopbackOn.asStateFlow()

  init {
    SeatPresenceTracker.registerAudioDisconnect { audioEngine.leaveRoomVoice() }
    appContext?.let { ctx ->
      registerNetworkMonitor(ctx)
      viewModelScope.launch(Dispatchers.IO) {
        SeatPresenceTracker.flushPendingCleanup(ctx, currentUserId, _activeRoomId.value)
      }
    }

    // Live presence + seat heartbeat every 7s; auto-exit if offline; host prunes stale occupants
    viewModelScope.launch {
      _activeRoomId.collectLatest { roomId ->
        if (roomId == null) return@collectLatest
        while (isActive && _activeRoomId.value == roomId) {
          delay(HEARTBEAT_INTERVAL_MS)
          if (_activeRoomId.value != roomId) break
          _presenceClockMs.value = System.currentTimeMillis()
          if (!isNetworkConnected()) {
            leaveRoomOnNetworkLoss()
            break
          }
          val profile = currentProfileOrDefault()
          runCatching { repository.touchRoomPresenceHeartbeat(roomId, profile) }
          val seats = activeRoomSeats.value
          val mySeat = seats.firstOrNull { it.occupantUid == currentUserId }
          if (mySeat != null) {
            runCatching { repository.touchSeatHeartbeat(roomId, mySeat.seatIndex.toInt()) }
          }
          val room = activeRoom.value
          if (room != null && room.ownerId == currentUserId) {
            pruneStaleOccupantsAsHost(room, seats)
          }
        }
      }
    }

    // Prefetch profiles of off-seat live room members too
    viewModelScope.launch {
      activeRoomPresence.collect { members ->
        members.asSequence()
          .map { it.userId }
          .filter { it.isNotBlank() && it !in _memberProfiles.value }
          .distinct()
          .forEach { uid -> loadUserProfile(uid) }
      }
    }

    viewModelScope.launch {
      repository.ensureUserProfile(initialDisplayName, initialAvatarUrl)
    }

    // Observe claimed level rewards & auto-award newly unlocked level coins once per tier
    viewModelScope.launch {
      repository.observeClaimedLevelTiers()
        .catch { /* logged by repository */ }
        .collect { claimed ->
          hasLoadedLevelClaims = true
          _claimedLevelTiers.value = claimed
        }
    }

    viewModelScope.launch {
      combine(userProfile, levelConfigs, _claimedLevelTiers) { pState, configs, claimed ->
        Triple((pState as? UiState.Success)?.data, configs, claimed)
      }.collect { (profile, configs, claimed) ->
        if (!hasLoadedLevelClaims || profile == null || configs.isEmpty()) return@collect
        val userLevel = SwarUserIdentity.levelForProfile(profile).level
        val unlockedTiers = SwarUserIdentity.unlockedTiersForLevel(userLevel)
        for (tier in unlockedTiers) {
          val cfg = configs[tier.badgeLevel] ?: continue
          if (cfg.rewardCoins > 0L && tier.badgeLevel !in claimed && claimingTiersInFlight.add(tier.badgeLevel)) {
            launch {
              try {
                repository.claimLevelRewardCoins(tier.badgeLevel, cfg.rewardCoins)
                  .onSuccess { granted ->
                    if (granted > 0L) {
                      _claimedLevelTiers.update { it + tier.badgeLevel }
                      _events.tryEmit("🎉 LV.${tier.badgeLevel} स्तर अनलॉक बोनस: +$granted कॉइंस मिले!")
                    }
                  }
              } finally {
                claimingTiersInFlight.remove(tier.badgeLevel)
              }
            }
          }
        }
      }
    }

    // Watch remote gift catalog deletions so deleted gift videos/links disappear globally in real time
    viewModelScope.launch {
      repository.observeGiftCatalog()
        .catch { /* logged by repository */ }
        .collect { remoteList ->
          val currentMap = remoteList.associateBy { it.id }
          val removedGifts = lastRemoteGiftSnapshot.values.filter { it.id !in currentMap }
          removedGifts.forEach { deleted ->
            purgeDeletedGiftEverywhere(
              giftId = deleted.id,
              mediaUrls = listOf(deleted.mediaUrl, deleted.thumbnailUrl),
            )
          }
          lastRemoteGiftSnapshot = currentMap
        }
    }

    // Observe cross-room gift broadcasts for the marquee strip & full-screen room playback
    viewModelScope.launch {
      repository.observeGlobalGiftEvents()
        .catch { /* logged by repository */ }
        .collect { events ->
          val currentEventIds = events.asSequence().map { it.id }.toSet()
          // If the currently displayed gift event was deleted from Firestore, dismiss immediately
          val activeFull = _activeFullScreenGift.value
          if (activeFull != null && activeFull.id.isNotBlank() &&
            !activeFull.giftId.startsWith("entry_lv_") &&
            activeFull.id !in currentEventIds
          ) {
            dismissFullScreenGift()
          }
          val activeMarquee = _latestMarqueeGift.value
          if (activeMarquee != null && activeMarquee.id.isNotBlank() && activeMarquee.id !in currentEventIds) {
            _latestMarqueeGift.value = null
          }

          if (!initialGiftSnapshotHandled) {
            events.forEach { seenGiftEventIds.add(it.id) }
            initialGiftSnapshotHandled = true
          } else {
            val hiddenIds = _hiddenGiftIds.value
            val freshEvents = events.filter {
              it.id.isNotBlank() && it.giftId !in hiddenIds && seenGiftEventIds.add(it.id)
            }
            val activeId = _activeRoomId.value
            val joinedAt = _roomJoinedAtMs.value
            val newRoomEvents = if (activeId != null) {
              freshEvents
                .filter { evt ->
                  evt.roomId == activeId &&
                    (evt.createdAt == null || joinedAt == 0L || evt.createdAt.toDate().time >= joinedAt - 5_000L)
                }
                .reversed()
            } else {
              emptyList()
            }
            newRoomEvents.forEach { enqueueFullScreenGift(it) }
            // Do not show cross-room gift marquee for room-entry videos
            freshEvents.firstOrNull { !it.giftId.startsWith("entry_lv_") }
              ?.let { triggerOrQueueMarquee(it) }
          }
        }
    }

    // Sync real-time microphone speaking activity onto user's claimed Firestore seat
    viewModelScope.launch {
      audioEngine.isSpeaking
        .collectLatest { speaking ->
          val roomId = _activeRoomId.value ?: return@collectLatest
          val mySeat = activeRoomSeats.value.firstOrNull { it.occupantUid == currentUserId }
            ?: return@collectLatest
          if (!_isMicMuted.value && mySeat.isSpeaking != speaking) {
            repository.updateSeatAudioState(
              roomId = roomId,
              seatIndex = mySeat.seatIndex.toInt(),
              isMuted = false,
              isSpeaking = speaking,
            )
          }
        }
    }

    // Keep own profile synced in memberProfiles cache and prefetch profiles of seated room members
    viewModelScope.launch {
      userProfile.collect { state ->
        val mine = (state as? UiState.Success)?.data ?: return@collect
        _memberProfiles.update { it + (currentUserId to mine) }
      }
    }
    viewModelScope.launch {
      activeRoomSeats.collect { seats ->
        seats.asSequence()
          .map { it.occupantUid }
          .filter { it.isNotBlank() && it !in _memberProfiles.value }
          .distinct()
          .forEach { uid -> loadUserProfile(uid) }

        val activeId = _activeRoomId.value
        if (activeId != null) {
          val mySeat = seats.firstOrNull { it.occupantUid == currentUserId }
          SeatPresenceTracker.recordSeatState(
            context = appContext,
            userId = currentUserId,
            roomId = activeId,
            seatIndex = mySeat?.seatIndex?.toInt(),
          )
          if (mySeat == null) {
            if (!_isMicMuted.value) _isMicMuted.value = true
            audioEngine.updateSeatPublishState(isSeated = false, isMuted = true)
          } else {
            if (mySeat.isMuted && !_isMicMuted.value) {
              _isMicMuted.value = true
            }
            audioEngine.updateSeatPublishState(isSeated = true, isMuted = _isMicMuted.value)
          }
        }
      }
    }
  }

  private fun enqueueFullScreenGift(event: GiftEvent) {
    val activeId = _activeRoomId.value ?: return
    if (event.roomId != activeId) return
    if (_activeFullScreenGift.value == null) {
      _activeFullScreenGift.value = event
    } else {
      pendingGiftQueue.addLast(event)
    }
  }

  private fun triggerOrQueueMarquee(event: GiftEvent) {
    if (event.giftId.startsWith("entry_lv_")) return
    if (_activeFullScreenGift.value == null) {
      _latestMarqueeGift.value = event
    } else {
      pendingMarqueeGift = event
    }
  }

  private fun purgeDeletedGiftEverywhere(
    giftId: String,
    mediaUrls: List<String>,
  ) {
    val cleanUrls = mediaUrls.map { it.trim() }.filter { it.isNotBlank() }.toSet()
    cleanUrls.forEach { url ->
      SupabaseNetworkClient.evictCachedMedia(appContext?.cacheDir, url)
    }
    fun matchesDeleted(evt: GiftEvent): Boolean {
      if (giftId.isNotBlank() && evt.giftId == giftId) return true
      val evtUrl = evt.giftMediaUrl.trim()
      return evtUrl.isNotBlank() && evtUrl in cleanUrls
    }
    if (pendingGiftQueue.isNotEmpty()) {
      val remaining = pendingGiftQueue.filterNot(::matchesDeleted)
      pendingGiftQueue.clear()
      remaining.forEach { pendingGiftQueue.addLast(it) }
    }
    pendingMarqueeGift?.let { queuedMarquee ->
      if (matchesDeleted(queuedMarquee)) {
        pendingMarqueeGift = null
      }
    }
    _latestMarqueeGift.value?.let { currentMarquee ->
      if (matchesDeleted(currentMarquee)) {
        _latestMarqueeGift.value = null
      }
    }
    _activeFullScreenGift.value?.let { currentActive ->
      if (matchesDeleted(currentActive)) {
        dismissFullScreenGift()
      }
    }
  }

  fun clearMarqueeGift(eventId: String) {
    if (_latestMarqueeGift.value?.id == eventId) {
      _latestMarqueeGift.value = null
    }
  }

  fun dismissFullScreenGift() {
    val activeId = _activeRoomId.value
    var next: GiftEvent? = null
    while (pendingGiftQueue.isNotEmpty()) {
      val candidate = pendingGiftQueue.removeFirst()
      if (activeId != null && candidate.roomId == activeId) {
        next = candidate
        break
      }
    }
    _activeFullScreenGift.value = next
    if (next == null) {
      pendingMarqueeGift?.let { queued ->
        _latestMarqueeGift.value = queued
        pendingMarqueeGift = null
      }
    }
  }

  private fun currentProfileOrDefault(): UserProfile {
    val loaded = (userProfile.value as? UiState.Success)?.data
    return loaded ?: UserProfile(
      id = currentUserId,
      userId = currentUserId,
      displayName = initialDisplayName.ifBlank { "Swar User" },
      handle = "swar_${currentUserId.take(6)}",
      avatarUrl = initialAvatarUrl,
      clubTag = "SWAR",
    )
  }

  // --- Room Entry / Exit & Creation ---

  fun createRoom(
    title: String,
    category: String,
    backgroundTheme: String,
    clubTag: String,
    welcomeNotice: String,
    seatCount: Int,
    coverImageUri: Uri?,
    onRoomCreated: (String) -> Unit,
  ) {
    viewModelScope.launch {
      val existingRoom = (rooms.value as? UiState.Success)?.data?.firstOrNull { it.ownerId == currentUserId }
      if (existingRoom != null) {
        enterRoom(existingRoom.id, sendJoinMessage = false, incrementListenerCount = true)
        onRoomCreated(existingRoom.id)
        return@launch
      }

      var coverUrl = ""
      if (coverImageUri != null) {
        _isUploadingMedia.value = true
        val upload = supabaseStorage.uploadMediaFromUri(
          uri = coverImageUri,
          folder = "covers",
          uid = currentUserId,
          isVideo = false,
        )
        _isUploadingMedia.value = false
        upload.onSuccess { coverUrl = it }
          .onFailure { err ->
            _events.tryEmit(err.message ?: "कवर अपलोड विफल रहा, डिफ़ॉल्ट थीम का उपयोग किया जा रहा है।")
          }
      }

      repository.createRoom(
        title = title,
        category = category,
        backgroundTheme = backgroundTheme,
        clubTag = clubTag,
        welcomeNotice = welcomeNotice,
        coverUrl = coverUrl,
        seatCount = seatCount,
        hostProfile = currentProfileOrDefault(),
      ).onSuccess { roomId ->
        enterRoom(roomId, sendJoinMessage = false, incrementListenerCount = false)
        onRoomCreated(roomId)
      }.onFailure { err ->
        _events.tryEmit(err.message ?: "रूम बनाने में त्रुटि हुई")
      }
    }
  }

  fun minimizeActiveRoom() {
    if (_activeRoomId.value != null) {
      _isRoomMinimized.value = true
    }
  }

  fun expandActiveRoom() {
    if (_activeRoomId.value != null) {
      _isRoomMinimized.value = false
    }
  }

  fun enterRoom(
    roomId: String,
    sendJoinMessage: Boolean = true,
    incrementListenerCount: Boolean = true,
  ) {
    if (!isNetworkConnected()) {
      _events.tryEmit("इंटरनेट कनेक्शन उपलब्ध नहीं है — कृपया नेटवर्क चालू करें।")
      return
    }
    val prevRoomId = _activeRoomId.value
    if (prevRoomId == roomId) {
      _isRoomMinimized.value = false
      return
    }
    // Always stop/clear any active or queued full-screen gift from the previous room
    _activeFullScreenGift.value = null
    pendingGiftQueue.clear()
    _isRoomMinimized.value = false
    val prevSeatHint = if (prevRoomId != null) {
      activeRoomSeats.value.firstOrNull { it.occupantUid == currentUserId }?.seatIndex?.toInt()
    } else {
      null
    }
    if (prevRoomId != null) {
      stopMicEngine()
      audioEngine.leaveRoomVoice()
    }
    val nowMs = System.currentTimeMillis()
    _roomJoinedAtMs.value = nowMs
    _presenceClockMs.value = nowMs
    _activeRoomId.value = roomId
    SeatPresenceTracker.recordEnteredRoom(
      context = appContext,
      userId = currentUserId,
      roomId = roomId,
      listenerCounted = incrementListenerCount,
    )
    appContext?.let { ctx ->
      runCatching { ctx.startService(Intent(ctx, SeatCleanupService::class.java)) }
    }
    audioEngine.setSpeakerphone(_isSpeakerOn.value)
    audioEngine.joinRoomVoice(
      roomId = roomId,
      isBroadcaster = false,
      isMuted = true,
    )
    viewModelScope.launch {
      if (prevRoomId != null) {
        vacateRoomPresence(prevRoomId, prevSeatHint)
      }
      if (incrementListenerCount) {
        repository.adjustListenerCount(roomId, 1L)
      }
      val room = (rooms.value as? UiState.Success)?.data?.firstOrNull { it.id == roomId }
        ?: activeRoom.value?.takeIf { it.id == roomId }
        ?: repository.getRoom(roomId)
      val profile = currentProfileOrDefault()
      if (room != null) {
        runCatching { repository.upsertRoomPresence(room, profile) }
      }
      if (room != null && room.ownerId == currentUserId) {
        runCatching { repository.ensureMinimumSeats(room, 22) }
      }
      // Clean any leftover old join entry message from earlier visits before posting fresh entry
      runCatching { repository.deleteUserJoinMessagesInRoom(roomId, currentUserId) }
      if (sendJoinMessage && room != null && _activeRoomId.value == roomId) {
        repository.sendRoomSystemMessage(
          roomId = roomId,
          roomOwnerId = room.ownerId,
          isRoomPublic = room.isPublic,
          senderName = profile.displayName,
          senderAvatarUrl = profile.avatarUrl,
          text = "${profile.displayName} का कमरे में स्वागत है",
          messageType = "join",
          badgeTag = profile.clubTag,
        )
      }
      // Play user's unlocked royal entry video inside THIS room only
      if (room != null && _activeRoomId.value == roomId) {
        val userLevel = SwarUserIdentity.levelForProfile(profile).level
        val configs = levelConfigs.value
        val entryMatch = SwarUserIdentity.highestUnlockedEntryVideo(userLevel, configs)
        if (entryMatch != null) {
          val (tier, entryUrl) = entryMatch
          repository.broadcastRoomEntryVideo(
            room = room,
            sender = profile,
            badgeLevel = tier.badgeLevel,
            entryVideoUrl = entryUrl,
          ).onSuccess { entryEvent ->
            if (_activeRoomId.value == room.id && seenGiftEventIds.add(entryEvent.id)) {
              enqueueFullScreenGift(entryEvent)
            }
          }
        }
      }
    }
  }

  fun leaveActiveRoom(onComplete: (() -> Unit)? = null) {
    val roomId = _activeRoomId.value
    // Immediately cancel any full-screen gift playing for this room
    _activeFullScreenGift.value = null
    pendingGiftQueue.clear()
    _roomJoinedAtMs.value = 0L
    if (roomId == null) {
      _isRoomMinimized.value = false
      SeatPresenceTracker.clearSession(appContext)
      onComplete?.invoke()
      return
    }
    val seatHint = activeRoomSeats.value.firstOrNull { it.occupantUid == currentUserId }?.seatIndex?.toInt()
    SeatPresenceTracker.savePendingCleanup(
      context = appContext,
      presence = SeatPresenceTracker.ActivePresence(
        userId = currentUserId,
        roomId = roomId,
        seatIndex = seatHint,
        listenerCounted = true,
      ),
    )
    appContext?.let { ctx ->
      runCatching { ctx.stopService(Intent(ctx, SeatCleanupService::class.java)) }
    }
    stopMicEngine()
    audioEngine.leaveRoomVoice()
    _isRoomMinimized.value = false
    _activeRoomId.value = null
    viewModelScope.launch {
      try {
        if (vacateRoomPresence(roomId, seatHint)) {
          SeatPresenceTracker.clearPendingCleanup(appContext)
        }
      } finally {
        onComplete?.invoke()
      }
    }
  }

  fun onNetworkConnectivityChanged(isConnected: Boolean) {
    if (!isConnected) {
      if (_activeRoomId.value != null) {
        leaveRoomOnNetworkLoss()
      }
    } else {
      appContext?.let { ctx ->
        viewModelScope.launch(Dispatchers.IO) {
          SeatPresenceTracker.flushPendingCleanup(ctx, currentUserId, _activeRoomId.value)
        }
      }
      val activeId = _activeRoomId.value
      if (activeId != null) {
        viewModelScope.launch {
          runCatching { repository.touchRoomPresenceHeartbeat(activeId, currentProfileOrDefault()) }
        }
      }
    }
  }

  private fun leaveRoomOnNetworkLoss() {
    val roomId = _activeRoomId.value ?: return
    val seatHint = activeRoomSeats.value.firstOrNull { it.occupantUid == currentUserId }?.seatIndex?.toInt()
    SeatPresenceTracker.savePendingCleanup(
      context = appContext,
      presence = SeatPresenceTracker.ActivePresence(
        userId = currentUserId,
        roomId = roomId,
        seatIndex = seatHint,
        listenerCounted = true,
      ),
    )
    appContext?.let { ctx ->
      runCatching { ctx.stopService(Intent(ctx, SeatCleanupService::class.java)) }
    }
    stopMicEngine()
    audioEngine.leaveRoomVoice()
    _activeFullScreenGift.value = null
    pendingGiftQueue.clear()
    _roomJoinedAtMs.value = 0L
    _isRoomMinimized.value = false
    _activeRoomId.value = null
    _events.tryEmit("नेटवर्क कनेक्शन कट गया — आप रूम से बाहर आ गए हैं।")
    viewModelScope.launch(Dispatchers.IO) {
      if (vacateRoomPresence(roomId, seatHint)) {
        SeatPresenceTracker.clearPendingCleanup(appContext)
      }
    }
  }

  private suspend fun vacateRoomPresence(roomId: String, seatHint: Int?): Boolean {
    var ok = true
    runCatching { repository.vacateUserSeatsInRoom(roomId, seatHint).getOrThrow() }
      .onFailure { err ->
        ok = false
        Log.w(TAG, "Vacate seats error in $roomId", err)
      }
    runCatching { repository.removeRoomPresence(roomId, currentUserId).getOrThrow() }
      .onFailure { err ->
        ok = false
        Log.w(TAG, "Remove presence error in $roomId", err)
      }
    runCatching { repository.deleteUserJoinMessagesInRoom(roomId, currentUserId) }
    runCatching { repository.adjustListenerCount(roomId, -1L).getOrThrow() }
      .onFailure { err ->
        ok = false
        Log.w(TAG, "Decrement listenerCount error in $roomId", err)
      }
    return ok
  }

  private suspend fun pruneStaleOccupantsAsHost(room: VoiceRoom, seats: List<RoomSeat>) {
    val estimatedServerNow = System.currentTimeMillis() + serverClockOffsetMs
    val allPresence = rawRoomPresence.value
    val stalePresence = allPresence.filter { member ->
      member.userId.isNotBlank() &&
        member.userId != currentUserId &&
        !isTimestampFresh(member.lastSeenAt?.toDate()?.time, estimatedServerNow, PRESENCE_STALE_MS)
    }
    stalePresence.forEach { stale ->
      runCatching { repository.removeRoomPresence(room.id, stale.userId) }
      runCatching { repository.deleteUserJoinMessagesInRoom(room.id, stale.userId) }
    }
    val freshPresenceUids = allPresence
      .filter { member ->
        member.userId.isNotBlank() &&
          (member.userId == currentUserId ||
            isTimestampFresh(member.lastSeenAt?.toDate()?.time, estimatedServerNow, PRESENCE_STALE_MS))
      }
      .map { it.userId }
      .toSet()

    val staleSeats = seats.filter { seat ->
      seat.occupantUid.isNotBlank() &&
        seat.occupantUid != currentUserId &&
        seat.occupantUid !in freshPresenceUids &&
        !isTimestampFresh(seat.updatedAt?.toDate()?.time, estimatedServerNow, SEAT_STALE_MS)
    }
    staleSeats.forEach { seat ->
      runCatching { repository.leaveSeat(room.id, seat.seatIndex.toInt()) }
      runCatching { repository.deleteUserJoinMessagesInRoom(room.id, seat.occupantUid) }
    }
    val liveUids = buildSet {
      add(currentUserId)
      addAll(freshPresenceUids)
      seats.filter { it.occupantUid.isNotBlank() && it !in staleSeats }.forEach { add(it.occupantUid) }
    }
    val desiredCount = liveUids.size.toLong().coerceAtLeast(1L)
    if (desiredCount != room.listenerCount) {
      runCatching { repository.syncRoomListenerCount(room.id, desiredCount) }
    }
  }

  private fun isNetworkConnected(): Boolean {
    val cm = registeredConnectivityManager
      ?: (appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
      ?: return true
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
    if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
      sawValidatedInternet = true
    } else if (sawValidatedInternet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return false
    }
    return true
  }

  private fun registerNetworkMonitor(ctx: Context) {
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
    registeredConnectivityManager = cm
    runCatching {
      val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
      if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) {
        sawValidatedInternet = true
      }
    }
    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        viewModelScope.launch { onNetworkConnectivityChanged(true) }
      }

      override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val hasValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        if (hasValidated) {
          sawValidatedInternet = true
        }
        val effectiveOnline = hasInternet && (!sawValidatedInternet || hasValidated)
        if (!effectiveOnline && !isNetworkConnected()) {
          viewModelScope.launch { onNetworkConnectivityChanged(false) }
        } else if (effectiveOnline) {
          viewModelScope.launch { onNetworkConnectivityChanged(true) }
        }
      }

      override fun onLost(network: Network) {
        viewModelScope.launch {
          // Slight grace check for immediate interface handoff (e.g. Wi-Fi -> 4G), disconnect immediately if offline
          delay(250L)
          if (!isNetworkConnected()) {
            onNetworkConnectivityChanged(false)
          }
        }
      }

      override fun onUnavailable() {
        viewModelScope.launch { onNetworkConnectivityChanged(false) }
      }
    }
    registeredNetworkCallback = callback
    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()
    runCatching { cm.registerNetworkCallback(request, callback) }
  }

  // --- Seat & Live Mic Controls ---

  fun onSeatTapped(seat: RoomSeat, hasAudioPermission: Boolean, onNeedAudioPermission: () -> Unit) {
    val room = activeRoom.value ?: return
    val isHost = room.ownerId == currentUserId
    viewModelScope.launch {
      when {
        seat.occupantUid == currentUserId -> {
          toggleMic(hasAudioPermission, onNeedAudioPermission)
        }
        seat.occupantUid.isEmpty() && !seat.isLocked -> {
          if (!hasAudioPermission) {
            onNeedAudioPermission()
          }
          val prevSeatIndices = activeRoomSeats.value
            .filter { it.occupantUid == currentUserId && it.seatIndex != seat.seatIndex }
            .map { it.seatIndex.toInt() }
          repository.moveToSeat(
            roomId = room.id,
            vacateSeatIndices = prevSeatIndices,
            targetSeatIndex = seat.seatIndex.toInt(),
            profile = currentProfileOrDefault(),
            preserveMuted = seat.isMuted,
          ).onSuccess {
            if (!seat.isMuted && hasAudioPermission) {
              _isMicMuted.value = false
              audioEngine.startCapture()
            } else if (seat.isMuted) {
              stopMicEngine()
            }
          }.onFailure { _events.tryEmit("सीट लेने में त्रुटि: ${it.message}") }
        }
        seat.isLocked && isHost -> {
          repository.toggleSeatLock(room.id, seat.seatIndex.toInt(), lock = false)
        }
        seat.isLocked -> {
          _events.tryEmit("यह सीट होस्ट द्वारा लॉक की गई है।")
        }
        isHost && seat.occupantUid.isNotEmpty() -> {
          repository.leaveSeat(room.id, seat.seatIndex.toInt())
          _events.tryEmit("${seat.occupantName} को सीट से हटाया गया।")
        }
      }
    }
  }

  fun leaveMySeat() {
    val roomId = _activeRoomId.value ?: return
    val seatHint = activeRoomSeats.value.firstOrNull { it.occupantUid == currentUserId }?.seatIndex?.toInt()
    stopMicEngine()
    viewModelScope.launch {
      repository.vacateUserSeatsInRoom(roomId, seatHint)
    }
  }

  fun toggleSeatLock(seat: RoomSeat) {
    val roomId = _activeRoomId.value ?: return
    viewModelScope.launch {
      repository.toggleSeatLock(roomId, seat.seatIndex.toInt(), !seat.isLocked)
    }
  }

  fun toggleSeatMute(seat: RoomSeat, hasAudioPermission: Boolean, onNeedAudioPermission: () -> Unit) {
    val room = activeRoom.value ?: return
    if (seat.occupantUid == currentUserId) {
      toggleMic(hasAudioPermission, onNeedAudioPermission)
      return
    }
    if (room.ownerId != currentUserId) return
    val nextMuted = !seat.isMuted
    viewModelScope.launch {
      repository.updateSeatAudioState(
        roomId = room.id,
        seatIndex = seat.seatIndex.toInt(),
        isMuted = nextMuted,
        isSpeaking = false,
      )
    }
  }

  fun kickSeatOccupant(seat: RoomSeat) {
    val room = activeRoom.value ?: return
    if (room.ownerId != currentUserId || seat.occupantUid.isBlank() || seat.occupantUid == currentUserId) {
      return
    }
    viewModelScope.launch {
      repository.leaveSeat(room.id, seat.seatIndex.toInt())
        .onSuccess {
          _events.tryEmit("${seat.occupantName.ifBlank { "सदस्य" }} को सीट से किक आउट किया गया।")
        }
        .onFailure { err ->
          _events.tryEmit("किक आउट करने में त्रुटि: ${err.message}")
        }
    }
  }

  fun sitOnFirstAvailableSeat(hasAudioPermission: Boolean, onNeedAudioPermission: () -> Unit) {
    val firstOpenSeat = activeRoomSeats.value.firstOrNull { it.occupantUid.isBlank() && !it.isLocked }
    if (firstOpenSeat == null) {
      _events.tryEmit("अभी कोई खाली सीट उपलब्ध नहीं है।")
      return
    }
    onSeatTapped(firstOpenSeat, hasAudioPermission, onNeedAudioPermission)
  }

  fun loadUserProfile(uid: String) {
    if (uid.isBlank()) return
    viewModelScope.launch {
      val fetched = repository.getUserProfile(uid) ?: return@launch
      _memberProfiles.update { it + (uid to fetched) }
    }
  }

  fun blockUser(uid: String, displayName: String) {
    if (uid.isBlank() || uid == currentUserId) return
    val updated = _blockedUserIds.value + uid
    _blockedUserIds.value = updated
    blockPrefs?.edit()?.putStringSet(KEY_BLOCKED_UIDS, updated)?.apply()
    val room = activeRoom.value
    if (room != null && room.ownerId == currentUserId) {
      val seat = activeRoomSeats.value.firstOrNull { it.occupantUid == uid }
      if (seat != null) {
        viewModelScope.launch {
          runCatching { repository.leaveSeat(room.id, seat.seatIndex.toInt()) }
        }
      }
    }
    val safeName = displayName.ifBlank { "यूज़र" }
    _events.tryEmit("$safeName को ब्लॉक किया गया।")
  }

  fun unblockUser(uid: String, displayName: String) {
    if (uid.isBlank()) return
    val updated = _blockedUserIds.value - uid
    _blockedUserIds.value = updated
    blockPrefs?.edit()?.putStringSet(KEY_BLOCKED_UIDS, updated)?.apply()
    val safeName = displayName.ifBlank { "यूज़र" }
    _events.tryEmit("$safeName को अनब्लॉक किया गया।")
  }

  fun toggleMic(hasAudioPermission: Boolean, onNeedAudioPermission: () -> Unit) {
    val room = activeRoom.value ?: return
    val mySeat = activeRoomSeats.value.firstOrNull { it.occupantUid == currentUserId }
    if (mySeat == null) {
      _events.tryEmit("बोलने के लिए पहले किसी खाली सीट पर बैठें।")
      return
    }
    if (!hasAudioPermission) {
      onNeedAudioPermission()
      return
    }
    val nextMuted = !_isMicMuted.value
    _isMicMuted.value = nextMuted
    audioEngine.updateSeatPublishState(isSeated = true, isMuted = nextMuted)
    if (!nextMuted && !audioEngine.isAgoraConfigured) {
      _events.tryEmit("लाइव मल्टी-यूज़र आवाज़ के लिए AI Studio Secrets में AGORA_APP_ID जोड़ें।")
    }
    viewModelScope.launch {
      repository.updateSeatAudioState(
        roomId = room.id,
        seatIndex = mySeat.seatIndex.toInt(),
        isMuted = nextMuted,
        isSpeaking = false,
      )
    }
  }

  fun onAudioPermissionGranted() {
    val room = activeRoom.value ?: return
    val mySeat = activeRoomSeats.value.firstOrNull { it.occupantUid == currentUserId } ?: return
    _isMicMuted.value = false
    audioEngine.updateSeatPublishState(isSeated = true, isMuted = false)
    viewModelScope.launch {
      repository.updateSeatAudioState(room.id, mySeat.seatIndex.toInt(), isMuted = false, isSpeaking = false)
    }
  }

  fun toggleSpeaker() {
    val next = !_isSpeakerOn.value
    _isSpeakerOn.value = next
    audioEngine.setSpeakerphone(next)
  }

  fun toggleMonitorLoopback() {
    val next = !_isMonitorLoopbackOn.value
    _isMonitorLoopbackOn.value = next
    audioEngine.monitorLoopbackEnabled = next
  }

  private fun stopMicEngine() {
    _isMicMuted.value = true
    audioEngine.updateSeatPublishState(isSeated = false, isMuted = true)
  }

  // --- Room Real-Time Text Chat & Gifts ---

  fun sendRoomText(text: String, messageType: String = "text") {
    val room = activeRoom.value ?: return
    if (text.isBlank()) return
    viewModelScope.launch {
      repository.sendRoomMessage(
        room = room,
        sender = currentProfileOrDefault(),
        text = text,
        messageType = messageType,
      ).onFailure {
        _events.tryEmit("संदेश भेजा नहीं जा सका")
      }
    }
  }

  fun updateRoomSettings(title: String, backgroundTheme: String, welcomeNotice: String) {
    val room = activeRoom.value ?: return
    viewModelScope.launch {
      repository.updateRoomSettings(room.id, title, backgroundTheme, welcomeNotice)
        .onSuccess { _events.tryEmit("रूम सेटिंग्स अपडेट हो गईं") }
        .onFailure { _events.tryEmit("सेटिंग्स अपडेट विफल") }
    }
  }

  // --- 1-on-1 Direct Messaging ---

  fun openConversation(conversationId: String) {
    _activeConversationId.value = conversationId
  }

  fun closeConversation() {
    _activeConversationId.value = null
  }

  fun startDirectChatWith(
    otherUid: String,
    otherName: String,
    otherAvatarUrl: String,
    onReady: (String) -> Unit,
  ) {
    if (otherUid.isBlank() || otherUid == currentUserId) return
    viewModelScope.launch {
      repository.startOrGetConversation(
        currentUser = currentProfileOrDefault(),
        otherUid = otherUid,
        otherName = otherName,
        otherAvatarUrl = otherAvatarUrl,
      ).onSuccess { convoId ->
        _activeConversationId.value = convoId
        onReady(convoId)
      }.onFailure {
        _events.tryEmit("चैट शुरू नहीं हो सकी")
      }
    }
  }

  fun sendDirectMessage(conversation: Conversation, text: String) {
    if (text.isBlank()) return
    viewModelScope.launch {
      repository.sendDirectMessage(
        conversationId = conversation.id,
        participantUids = conversation.participantUids,
        senderName = currentProfileOrDefault().displayName,
        text = text,
      ).onFailure {
        _events.tryEmit("संदेश भेजा नहीं जा सका")
      }
    }
  }

  // --- Supabase Storage Media: DP & Moments (Video / Photo) ---

  fun uploadAndSaveAvatarDp(uri: Uri) {
    viewModelScope.launch {
      val previousAvatarUrl = currentProfileOrDefault().avatarUrl
      _isUploadingMedia.value = true
      val result = supabaseStorage.uploadMediaFromUri(
        uri = uri,
        folder = "dp",
        uid = currentUserId,
        isVideo = false,
      )
      _isUploadingMedia.value = false
      result.onSuccess { publicUrl ->
        val profile = currentProfileOrDefault()
        repository.updateUserProfile(
          displayName = profile.displayName,
          handle = profile.handle,
          bio = profile.bio,
          gender = profile.gender,
          clubTag = profile.clubTag,
          avatarUrl = publicUrl,
        ).onSuccess {
          val activeId = _activeRoomId.value
          if (activeId != null) {
            repository.updateUserSeatProfile(activeId, profile.displayName, publicUrl)
          }
          if (previousAvatarUrl.isNotBlank() && previousAvatarUrl != publicUrl) {
            supabaseStorage.deleteMediaByPublicUrl(previousAvatarUrl)
              .onSuccess { deleted ->
                if (deleted) {
                  _events.tryEmit("DP अपडेट हो गई और पुरानी DP Supabase से डिलीट हो गई!")
                } else {
                  _events.tryEmit("DP Supabase Storage पर सफलतापूर्वक अपडेट हो गई!")
                }
              }
              .onFailure { err ->
                Log.w(TAG, "Old DP delete failed", err)
                _events.tryEmit("नई DP सेव हुई, पर पुरानी DP हटाने में त्रुटि: ${err.message}")
              }
          } else {
            _events.tryEmit("DP Supabase Storage पर सफलतापूर्वक अपडेट हो गई!")
          }
        }.onFailure {
          _events.tryEmit("प्रोफ़ाइल URL सहेजने में त्रुटि हुई।")
        }
      }.onFailure { err ->
        _events.tryEmit(err.message ?: "DP अपलोड विफल")
      }
    }
  }

  fun uploadMomentPost(uri: Uri, isVideo: Boolean, caption: String) {
    viewModelScope.launch {
      _isUploadingMedia.value = true
      if (isVideo) {
        _events.tryEmit("वीडियो ऑटोमैटिक कम MB में कम्प्रेश व अपलोड हो रहा है...")
      }
      val result = supabaseStorage.uploadMediaFromUri(
        uri = uri,
        folder = "moments",
        uid = currentUserId,
        isVideo = isVideo,
      )
      _isUploadingMedia.value = false
      result.onSuccess { mediaUrl ->
        repository.createMomentPost(
          author = currentProfileOrDefault(),
          caption = caption.ifBlank { if (isVideo) "नया वीडियो मोमेंट" else "नई तस्वीर" },
          mediaUrl = mediaUrl,
          mediaType = if (isVideo) "video" else "image",
        ).onSuccess {
          _events.tryEmit("मोमेंट Supabase Storage पर अपलोड हो गया!")
        }.onFailure {
          _events.tryEmit("मोमेंट पोस्ट सहेजने में त्रुटि")
        }
      }.onFailure { err ->
        _events.tryEmit(err.message ?: "मीडिया अपलोड विफल")
      }
    }
  }

  fun likeMoment(momentId: String) {
    viewModelScope.launch {
      repository.likeMoment(momentId)
    }
  }

  fun deleteMoment(momentId: String) {
    val targetMoment = (moments.value as? UiState.Success)?.data?.firstOrNull { it.id == momentId }
    viewModelScope.launch {
      repository.deleteMoment(momentId)
        .onSuccess {
          val mediaUrl = targetMoment?.mediaUrl.orEmpty()
          if (mediaUrl.isNotBlank()) {
            supabaseStorage.deleteMediaByPublicUrl(mediaUrl)
              .onFailure { err -> Log.w(TAG, "Moment Supabase cleanup failed", err) }
          }
          _events.tryEmit("मोमेंट डिलीट हो गया।")
        }
        .onFailure { err ->
          _events.tryEmit(err.message ?: "मोमेंट डिलीट नहीं हो सका")
        }
    }
  }

  fun updateProfileDetails(
    displayName: String,
    handle: String,
    bio: String,
    gender: String,
    clubTag: String,
  ) {
    viewModelScope.launch {
      repository.updateUserProfile(displayName, handle, bio, gender, clubTag)
        .onSuccess {
          val activeId = _activeRoomId.value
          if (activeId != null) {
            repository.updateUserSeatProfile(activeId, displayName, currentProfileOrDefault().avatarUrl)
          }
          _events.tryEmit("प्रोफ़ाइल सहेजी गई")
        }
        .onFailure { _events.tryEmit("प्रोफ़ाइल अपडेट विफल") }
    }
  }

  // --- Admin-Only Emoji & Gift Catalog Additions + Gift Sending + Coins ---

  fun addCustomEmoji(
    label: String,
    category: String,
    symbol: String,
    imageUri: Uri?,
    onSuccess: () -> Unit,
  ) {
    if (!isAdminUser) {
      _events.tryEmit("केवल एडमिन Gmail (chndra3735@gmail.com) से ही नया इमोजी जोड़ा जा सकता है।")
      return
    }
    viewModelScope.launch {
      val context = appContext
      if (imageUri != null && context != null && runCatching { MediaCompressor.isZipArchive(context, imageUri) }.getOrDefault(false)) {
        _isUploadingMedia.value = true
        try {
          val entries = MediaCompressor.extractZipEntries(context, imageUri, maxCount = 20)
          if (entries.isEmpty()) {
            _events.tryEmit("इस ZIP फ़ाइल में कोई मान्य GIF या इमेज (.gif, .png, .jpg, .webp) नहीं मिली।")
            return@launch
          }
          var addedCount = 0
          var lastError: String? = null
          try {
            entries.forEachIndexed { index, entry ->
              val upload = supabaseStorage.uploadMediaFromUri(
                uri = entry.tempUri,
                folder = "emojis",
                uid = currentUserId,
                isVideo = false,
              )
              upload.onSuccess { url ->
                val resolvedLabel = if (entries.size == 1 && label.isNotBlank()) {
                  label
                } else {
                  entry.label.ifBlank { "${label.ifBlank { "स्टिकर" }} ${index + 1}" }
                }
                repository.addEmojiToCatalog(
                  label = resolvedLabel,
                  category = category,
                  symbol = symbol,
                  imageUrl = url,
                ).onSuccess {
                  addedCount++
                }.onFailure { err ->
                  lastError = err.message
                }
              }.onFailure { err ->
                lastError = err.message
              }
            }
          } finally {
            entries.forEach { runCatching { it.tempFile.delete() } }
          }
          if (addedCount > 0) {
            _events.tryEmit(
              if (addedCount == 1) "नया इमोजी सभी यूज़र्स के लिए जोड़ दिया गया! 🎉"
              else "$addedCount इमोजी ZIP से सफलतापूर्वक जोड़ दिए गए! 🎉"
            )
            onSuccess()
          } else {
            _events.tryEmit(lastError ?: "ZIP इमोजी अपलोड विफल")
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          _events.tryEmit(e.message ?: "ZIP फ़ाइल प्रोसेस करने में त्रुटि")
        } finally {
          _isUploadingMedia.value = false
        }
        return@launch
      }

      var uploadedImageUrl = ""
      if (imageUri != null) {
        _isUploadingMedia.value = true
        val upload = supabaseStorage.uploadMediaFromUri(
          uri = imageUri,
          folder = "emojis",
          uid = currentUserId,
          isVideo = false,
        )
        _isUploadingMedia.value = false
        upload.onSuccess { uploadedImageUrl = it }
          .onFailure { err ->
            _events.tryEmit(err.message ?: "इमोजी इमेज अपलोड विफल")
            return@launch
          }
      }
      repository.addEmojiToCatalog(
        label = label,
        category = category,
        symbol = symbol,
        imageUrl = uploadedImageUrl,
      ).onSuccess {
        _events.tryEmit("नया इमोजी सभी यूज़र्स के लिए जोड़ दिया गया! 🎉")
        onSuccess()
      }.onFailure { err ->
        _events.tryEmit(err.message ?: "इमोजी जोड़ने में त्रुटि")
      }
    }
  }

  fun addCustomGift(
    name: String,
    category: String,
    emoji: String,
    coinCost: Long,
    thumbnailUri: Uri?,
    dpLinkUrl: String = "",
    videoLinkUrl: String = "",
    onSuccess: () -> Unit,
  ) {
    if (!isAdminUser) {
      val msg = "केवल एडमिन Gmail (chndra3735@gmail.com) से ही नया गिफ्ट जोड़ा जा सकता है।"
      _giftUploadError.value = msg
      _events.tryEmit(msg)
      return
    }
    _giftUploadError.value = null
    viewModelScope.launch {
      var thumbUrl = ""
      var videoUrl = ""

      if (thumbnailUri != null) {
        _isUploadingMedia.value = true
        val upload = supabaseStorage.uploadMediaFromUri(
          uri = thumbnailUri,
          folder = "gifts",
          uid = currentUserId,
          isVideo = false,
        )
        _isUploadingMedia.value = false
        upload.onSuccess { thumbUrl = it }
          .onFailure { err ->
            val msg = err.message ?: "गिफ्ट फोटो / थंबनेल अपलोड विफल"
            _giftUploadError.value = msg
            _events.tryEmit(msg)
            return@launch
          }
      } else if (dpLinkUrl.trim().isNotBlank()) {
        val cleanDpLink = com.example.data.repository.VideoLinkResolver.extractFirstHttpUrl(dpLinkUrl)
        _isUploadingMedia.value = true
        val resolvedDp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
          com.example.data.repository.VideoLinkResolver.resolveImageUrl(cleanDpLink)
        }
        _isUploadingMedia.value = false
        resolvedDp.onSuccess { resolvedUrl ->
          thumbUrl = resolvedUrl.takeIf { it.length <= 800 }.orEmpty()
          if (thumbUrl.isBlank()) {
            val msg = "DP फोटो लिंक बहुत लंबा है (800 अक्षरों से कम होना चाहिए) — कृपया गैलरी से DP चुनें।"
            _giftUploadError.value = msg
            _events.tryEmit(msg)
            return@launch
          }
        }.onFailure { err ->
          val msg = err.message ?: "गिफ्ट DP लिंक पढ़ा नहीं जा सका।"
          _giftUploadError.value = msg
          _events.tryEmit(msg)
          return@launch
        }
      }

      val cleanVideoLink = com.example.data.repository.VideoLinkResolver.extractFirstHttpUrl(videoLinkUrl)
      if (cleanVideoLink.isNotBlank()) {
        if (!cleanVideoLink.startsWith("http://", ignoreCase = true) &&
          !cleanVideoLink.startsWith("https://", ignoreCase = true)
        ) {
          val msg = "कृपया https:// या http:// से शुरू होने वाला मान्य वीडियो लिंक दर्ज करें।"
          _giftUploadError.value = msg
          _events.tryEmit(msg)
          return@launch
        }
        val canonicalInitial =
          com.example.data.repository.VideoLinkResolver.instagramCanonicalReelUrl(cleanVideoLink)
            ?: cleanVideoLink
        videoUrl = if (canonicalInitial.length <= 780) canonicalInitial else cleanVideoLink.take(780)

        if (com.example.data.repository.VideoLinkResolver.isSocialOrPageVideoUrl(cleanVideoLink)) {
          _isUploadingMedia.value = true
          val resolved = kotlinx.coroutines.withTimeoutOrNull(8_000L) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
              com.example.data.repository.VideoLinkResolver.resolvePlayableVideo(cleanVideoLink)
            }
          }
          _isUploadingMedia.value = false
          when (resolved) {
            is com.example.data.repository.VideoLinkResolution.Success -> {
              if (resolved.canonicalPageUrl.isNotBlank() && resolved.canonicalPageUrl.length <= 780) {
                videoUrl = resolved.canonicalPageUrl
              }
              if (thumbUrl.isBlank() && resolved.thumbnailUrl.isNotBlank() && resolved.thumbnailUrl.length <= 800) {
                thumbUrl = resolved.thumbnailUrl
              }
            }
            is com.example.data.repository.VideoLinkResolution.Failure -> {
              if (resolved.canonicalPageUrl.isNotBlank() && resolved.canonicalPageUrl.length <= 780) {
                videoUrl = resolved.canonicalPageUrl
              }
              if (thumbUrl.isBlank() && resolved.fallbackImageUrl.isNotBlank() && resolved.fallbackImageUrl.length <= 800) {
                thumbUrl = resolved.fallbackImageUrl
              }
            }
            null -> Unit
          }
        }
      }

      val mediaType = when {
        videoUrl.isNotBlank() -> "video"
        thumbUrl.isNotBlank() -> "image"
        else -> "emoji"
      }
      val mediaUrl = videoUrl.ifBlank { thumbUrl }

      repository.addGiftToCatalog(
        name = name,
        category = category,
        emoji = emoji,
        thumbnailUrl = thumbUrl,
        mediaUrl = mediaUrl,
        mediaType = mediaType,
        coinCost = coinCost,
      ).onSuccess {
        _giftUploadError.value = null
        _events.tryEmit("नया गिफ्ट और DP सभी यूज़र्स के लिए लाइव हो गए! 🎁")
        onSuccess()
      }.onFailure { err ->
        val msg = err.message ?: "गिफ्ट जोड़ने में त्रुटि"
        _giftUploadError.value = msg
        _events.tryEmit(msg)
      }
    }
  }

  fun updateCustomEmoji(
    existing: EmojiCatalogItem,
    label: String,
    category: String,
    symbol: String,
    newImageUri: Uri?,
    removeExistingImage: Boolean,
    onSuccess: () -> Unit,
  ) {
    if (!isAdminUser) {
      _events.tryEmit("केवल एडमिन Gmail (chndra3735@gmail.com) से ही इमोजी अपडेट किया जा सकता है।")
      return
    }
    if (existing.id.isBlank() || existing.id.startsWith("def_")) {
      _events.tryEmit("डिफ़ॉल्ट इमोजी बदला नहीं जा सकता।")
      return
    }
    viewModelScope.launch {
      val oldImageUrl = existing.imageUrl
      var finalImageUrl = if (removeExistingImage) "" else oldImageUrl
      if (newImageUri != null) {
        _isUploadingMedia.value = true
        val upload = supabaseStorage.uploadMediaFromUri(
          uri = newImageUri,
          folder = "emojis",
          uid = currentUserId,
          isVideo = false,
        )
        _isUploadingMedia.value = false
        upload.onSuccess { finalImageUrl = it }
          .onFailure { err ->
            _events.tryEmit(err.message ?: "नई इमोजी इमेज अपलोड विफल")
            return@launch
          }
      }
      repository.updateEmojiInCatalog(
        emojiId = existing.id,
        label = label,
        category = category,
        symbol = symbol,
        imageUrl = finalImageUrl,
      ).onSuccess {
        if (oldImageUrl.isNotBlank() && oldImageUrl != finalImageUrl) {
          supabaseStorage.deleteMediaByPublicUrl(oldImageUrl)
            .onFailure { err -> Log.w(TAG, "Old emoji image delete warning", err) }
        }
        _events.tryEmit("इमोजी अपडेट हो गया! ✨")
        onSuccess()
      }.onFailure { err ->
        _events.tryEmit(err.message ?: "इमोजी अपडेट करने में त्रुटि")
      }
    }
  }

  fun deleteCustomEmoji(
    item: EmojiCatalogItem,
    onSuccess: () -> Unit,
  ) {
    if (!isAdminUser) {
      _events.tryEmit("केवल एडमिन ही इमोजी डिलीट कर सकते हैं।")
      return
    }
    if (item.id.isBlank()) {
      _events.tryEmit("अमान्य इमोजी चयनित है।")
      return
    }
    if (item.id.startsWith("def_")) {
      _hiddenEmojiIds.update { prev ->
        val updated = prev + item.id
        catalogHiddenPrefs?.edit()?.putStringSet(KEY_HIDDEN_EMOJIS, updated)?.apply()
        updated
      }
      _events.tryEmit("इमोजी '${item.label}' पैनल से हटा दिया गया!")
      onSuccess()
      return
    }
    viewModelScope.launch {
      _isUploadingMedia.value = true
      repository.deleteEmojiFromCatalog(item.id)
        .onSuccess {
          _hiddenEmojiIds.update { it + item.id }
          _isUploadingMedia.value = false
          _events.tryEmit("इमोजी पैनल से पूरी तरह डिलीट हो गया!")
          onSuccess()
          val oldImageUrl = item.imageUrl
          if (oldImageUrl.isNotBlank()) {
            viewModelScope.launch {
              supabaseStorage.deleteMediaByPublicUrl(oldImageUrl)
                .onFailure { err -> Log.w(TAG, "Emoji storage delete warning", err) }
            }
          }
        }
        .onFailure { err ->
          _isUploadingMedia.value = false
          _events.tryEmit(err.message ?: "इमोजी डिलीट करने में त्रुटि")
        }
    }
  }

  fun updateCustomGift(
    existing: GiftCatalogItem,
    name: String,
    category: String,
    emoji: String,
    coinCost: Long,
    newThumbnailUri: Uri?,
    newDpLinkUrl: String = "",
    newVideoLinkUrl: String = "",
    removeExistingThumbnail: Boolean,
    removeExistingVideo: Boolean,
    onSuccess: () -> Unit,
  ) {
    if (!isAdminUser) {
      val msg = "केवल एडमिन Gmail (chndra3735@gmail.com) से ही गिफ्ट अपडेट किया जा सकता है।"
      _giftUploadError.value = msg
      _events.tryEmit(msg)
      return
    }
    if (existing.id.isBlank() || existing.id.startsWith("def_")) {
      val msg = "डिफ़ॉल्ट गिफ्ट बदला नहीं जा सकता।"
      _giftUploadError.value = msg
      _events.tryEmit(msg)
      return
    }
    _giftUploadError.value = null
    viewModelScope.launch {
      val oldThumbUrl = existing.thumbnailUrl
      val oldVideoUrl = if (existing.mediaType == "video") existing.mediaUrl else ""

      var finalThumbUrl = if (removeExistingThumbnail) "" else oldThumbUrl
      var finalVideoUrl = if (removeExistingVideo) "" else oldVideoUrl

      if (newThumbnailUri != null) {
        _isUploadingMedia.value = true
        val upload = supabaseStorage.uploadMediaFromUri(
          uri = newThumbnailUri,
          folder = "gifts",
          uid = currentUserId,
          isVideo = false,
        )
        _isUploadingMedia.value = false
        upload.onSuccess { finalThumbUrl = it }
          .onFailure { err ->
            val msg = err.message ?: "नया थंबनेल अपलोड विफल"
            _giftUploadError.value = msg
            _events.tryEmit(msg)
            return@launch
          }
      } else if (newDpLinkUrl.trim().isNotBlank() && newDpLinkUrl.trim() != oldThumbUrl) {
        val cleanDpLink = com.example.data.repository.VideoLinkResolver.extractFirstHttpUrl(newDpLinkUrl)
        _isUploadingMedia.value = true
        val resolvedDp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
          com.example.data.repository.VideoLinkResolver.resolveImageUrl(cleanDpLink)
        }
        _isUploadingMedia.value = false
        resolvedDp.onSuccess { resolvedUrl ->
          finalThumbUrl = resolvedUrl.takeIf { it.length <= 800 }.orEmpty()
          if (finalThumbUrl.isBlank()) {
            val msg = "DP फोटो लिंक बहुत लंबा है (800 अक्षरों से कम होना चाहिए) — कृपया गैलरी से DP चुनें।"
            _giftUploadError.value = msg
            _events.tryEmit(msg)
            return@launch
          }
        }.onFailure { err ->
          val msg = err.message ?: "नया DP लिंक पढ़ा नहीं जा सका।"
          _giftUploadError.value = msg
          _events.tryEmit(msg)
          return@launch
        }
      }

      val cleanVideoLink = com.example.data.repository.VideoLinkResolver.extractFirstHttpUrl(newVideoLinkUrl)
      if (cleanVideoLink.isNotBlank() && cleanVideoLink != oldVideoUrl) {
        if (!cleanVideoLink.startsWith("http://", ignoreCase = true) &&
          !cleanVideoLink.startsWith("https://", ignoreCase = true)
        ) {
          val msg = "कृपया https:// या http:// से शुरू होने वाला मान्य वीडियो लिंक दर्ज करें।"
          _giftUploadError.value = msg
          _events.tryEmit(msg)
          return@launch
        }
        val canonicalInitial =
          com.example.data.repository.VideoLinkResolver.instagramCanonicalReelUrl(cleanVideoLink)
            ?: cleanVideoLink
        finalVideoUrl = if (canonicalInitial.length <= 780) canonicalInitial else cleanVideoLink.take(780)

        if (com.example.data.repository.VideoLinkResolver.isSocialOrPageVideoUrl(cleanVideoLink)) {
          _isUploadingMedia.value = true
          val resolved = kotlinx.coroutines.withTimeoutOrNull(8_000L) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
              com.example.data.repository.VideoLinkResolver.resolvePlayableVideo(cleanVideoLink)
            }
          }
          _isUploadingMedia.value = false
          when (resolved) {
            is com.example.data.repository.VideoLinkResolution.Success -> {
              if (resolved.canonicalPageUrl.isNotBlank() && resolved.canonicalPageUrl.length <= 780) {
                finalVideoUrl = resolved.canonicalPageUrl
              }
              if (finalThumbUrl.isBlank() && resolved.thumbnailUrl.isNotBlank() && resolved.thumbnailUrl.length <= 800) {
                finalThumbUrl = resolved.thumbnailUrl
              }
            }
            is com.example.data.repository.VideoLinkResolution.Failure -> {
              if (resolved.canonicalPageUrl.isNotBlank() && resolved.canonicalPageUrl.length <= 780) {
                finalVideoUrl = resolved.canonicalPageUrl
              }
              if (finalThumbUrl.isBlank() && resolved.fallbackImageUrl.isNotBlank() && resolved.fallbackImageUrl.length <= 800) {
                finalThumbUrl = resolved.fallbackImageUrl
              }
            }
            null -> Unit
          }
        }
      } else if (removeExistingVideo) {
        finalVideoUrl = ""
      }

      val finalMediaType = when {
        finalVideoUrl.isNotBlank() -> "video"
        finalThumbUrl.isNotBlank() -> "image"
        else -> "emoji"
      }
      val finalMediaUrl = finalVideoUrl.ifBlank { finalThumbUrl }

      repository.updateGiftInCatalog(
        giftId = existing.id,
        name = name,
        category = category,
        emoji = emoji,
        thumbnailUrl = finalThumbUrl,
        mediaUrl = finalMediaUrl,
        mediaType = finalMediaType,
        coinCost = coinCost,
      ).onSuccess {
        val obsoleteUrls = buildList {
          if (oldThumbUrl.isNotBlank() && oldThumbUrl != finalThumbUrl && oldThumbUrl != finalMediaUrl) {
            add(oldThumbUrl)
          }
          if (oldVideoUrl.isNotBlank() && oldVideoUrl != finalVideoUrl && oldVideoUrl != finalThumbUrl) {
            add(oldVideoUrl)
          }
        }
        if (obsoleteUrls.isNotEmpty()) {
          supabaseStorage.deleteMediaByPublicUrls(obsoleteUrls)
            .onFailure { err -> Log.w(TAG, "Replaced gift media delete warning", err) }
        }
        _giftUploadError.value = null
        _events.tryEmit("गिफ्ट सफलतापूर्वक अपडेट हो गया! 🎁")
        onSuccess()
      }.onFailure { err ->
        val msg = err.message ?: "गिफ्ट अपडेट करने में त्रुटि"
        _giftUploadError.value = msg
        _events.tryEmit(msg)
      }
    }
  }

  fun deleteCustomGift(
    gift: GiftCatalogItem,
    onSuccess: () -> Unit,
  ) {
    if (!isAdminUser) {
      _events.tryEmit("केवल एडमिन ही गिफ्ट डिलीट कर सकते हैं।")
      return
    }
    if (gift.id.isBlank()) {
      _events.tryEmit("अमान्य गिफ्ट चयनित है।")
      return
    }
    val mediaUrls = listOf(gift.thumbnailUrl, gift.mediaUrl)
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
    purgeDeletedGiftEverywhere(giftId = gift.id, mediaUrls = mediaUrls)

    if (gift.id.startsWith("def_")) {
      _hiddenGiftIds.update { prev ->
        val updated = prev + gift.id
        catalogHiddenPrefs?.edit()?.putStringSet(KEY_HIDDEN_GIFTS, updated)?.apply()
        updated
      }
      _events.tryEmit("गिफ्ट '${gift.name}' और इसका लिंक पैनल से हटा दिया गया!")
      onSuccess()
      return
    }
    viewModelScope.launch {
      _isUploadingMedia.value = true
      try {
        repository.deleteGiftFromCatalog(gift.id, mediaUrls)
          .onSuccess {
            _hiddenGiftIds.update { prev ->
              val updated = prev + gift.id
              catalogHiddenPrefs?.edit()?.putStringSet(KEY_HIDDEN_GIFTS, updated)?.apply()
              updated
            }
            var storageMessage = "गिफ्ट '${gift.name}' व इसका लिंक हर जगह से रियल-टाइम डिलीट हो गए!"
            if (mediaUrls.isNotEmpty()) {
              supabaseStorage.deleteMediaByPublicUrls(mediaUrls)
                .onSuccess { deletedCount ->
                  storageMessage = if (deletedCount > 0) {
                    "गिफ्ट '${gift.name}', DP और वीडियो लिंक स्टोरेज व हर रूम से डिलीट हो गए!"
                  } else {
                    "गिफ्ट '${gift.name}' और लिंक हर जगह से रियल-टाइम डिलीट हो गए!"
                  }
                }
                .onFailure { err ->
                  Log.w(TAG, "Gift storage delete warning", err)
                  storageMessage = "गिफ्ट पैनल से डिलीट हो गया!"
                }
            }
            _events.tryEmit(storageMessage)
            onSuccess()
          }
          .onFailure { err ->
            _events.tryEmit(err.message ?: "गिफ्ट डिलीट करने में त्रुटि")
          }
      } finally {
        _isUploadingMedia.value = false
      }
    }
  }

  fun saveLevelTierConfig(
    badgeLevel: Int,
    entryVideoLink: String,
    entryVideoUri: Uri?,
    frameLink: String,
    frameImageUri: Uri?,
    rewardCoins: Long,
    badgeLink: String,
    badgeImageUri: Uri?,
    onDone: () -> Unit = {},
  ) {
    if (!isAdminUser) {
      _events.tryEmit("केवल एडमिन Gmail (chndra3735@gmail.com) से ही स्तर कार्ड सेटिंग बदली जा सकती है।")
      return
    }
    viewModelScope.launch {
      _isUploadingMedia.value = true
      try {
        var finalEntryUrl = VideoLinkResolver.extractFirstHttpUrl(entryVideoLink)
        if (entryVideoUri != null) {
          val up = supabaseStorage.uploadMediaFromUri(
            uri = entryVideoUri,
            folder = "level_entries",
            uid = currentUserId,
            isVideo = true,
          )
          up.onSuccess { finalEntryUrl = it }
            .onFailure { err ->
              _events.tryEmit(err.message ?: "एंट्री वीडियो अपलोड विफल")
              return@launch
            }
        } else if (finalEntryUrl.isNotBlank()) {
          val canonical = VideoLinkResolver.instagramCanonicalReelUrl(finalEntryUrl) ?: finalEntryUrl
          finalEntryUrl = canonical.take(780)
        }

        var finalFrameUrl = VideoLinkResolver.extractFirstHttpUrl(frameLink)
        if (frameImageUri != null) {
          val up = supabaseStorage.uploadMediaFromUri(
            uri = frameImageUri,
            folder = "level_frames",
            uid = currentUserId,
            isVideo = false,
          )
          up.onSuccess { finalFrameUrl = it }
            .onFailure { err ->
              _events.tryEmit(err.message ?: "फ्रेम अपलोड विफल")
              return@launch
            }
        } else if (finalFrameUrl.isNotBlank() && finalFrameUrl.length <= 800 &&
          !finalFrameUrl.lowercase().let {
            it.contains(".gif") || it.contains(".png") || it.contains(".webp") || it.contains(".jpg") || it.contains(".jpeg") || it.contains("supabase.co")
          }
        ) {
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            VideoLinkResolver.resolveImageUrl(finalFrameUrl)
          }.onSuccess { resolved ->
            if (resolved.length <= 800) finalFrameUrl = resolved
          }
        }

        var finalBadgeUrl = VideoLinkResolver.extractFirstHttpUrl(badgeLink)
        if (badgeImageUri != null) {
          val up = supabaseStorage.uploadMediaFromUri(
            uri = badgeImageUri,
            folder = "level_badges",
            uid = currentUserId,
            isVideo = false,
          )
          up.onSuccess { finalBadgeUrl = it }
            .onFailure { err ->
              _events.tryEmit(err.message ?: "बैज अपलोड विफल")
              return@launch
            }
        } else if (finalBadgeUrl.isNotBlank() && finalBadgeUrl.length <= 800 &&
          !finalBadgeUrl.lowercase().let {
            it.contains(".gif") || it.contains(".png") || it.contains(".webp") || it.contains(".jpg") || it.contains(".jpeg") || it.contains("supabase.co")
          }
        ) {
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            VideoLinkResolver.resolveImageUrl(finalBadgeUrl)
          }.onSuccess { resolved ->
            if (resolved.length <= 800) finalBadgeUrl = resolved
          }
        }

        repository.upsertLevelConfig(
          badgeLevel = badgeLevel,
          entryVideoUrl = finalEntryUrl.take(800),
          avatarFrameUrl = finalFrameUrl.take(800),
          rewardCoins = rewardCoins.coerceIn(0L, 999999L),
          profileBadgeUrl = finalBadgeUrl.take(800),
        ).onSuccess {
          _events.tryEmit("LV.$badgeLevel कार्ड सेटिंग सभी यूज़र्स के लिए लाइव सेव हो गई! ✨")
          onDone()
        }.onFailure { err ->
          _events.tryEmit(err.message ?: "स्तर सेटिंग सेव करने में त्रुटि")
        }
      } finally {
        _isUploadingMedia.value = false
      }
    }
  }

  fun claimLevelTierReward(badgeLevel: Int) {
    val profile = (userProfile.value as? UiState.Success)?.data ?: return
    val userLevel = SwarUserIdentity.levelForProfile(profile).level
    val tier = SwarUserIdentity.levelTiers.firstOrNull { it.badgeLevel == badgeLevel } ?: return
    if (userLevel < tier.minLevel) {
      _events.tryEmit("यह इनाम LV.${tier.minLevel} खुलने पर अनलॉक होगा।")
      return
    }
    val reward = levelConfigs.value[badgeLevel]?.rewardCoins ?: 0L
    if (reward <= 0L) {
      _events.tryEmit("इस स्तर पर अभी कोई कॉइन बोनस सेट नहीं है।")
      return
    }
    if (badgeLevel in _claimedLevelTiers.value || !claimingTiersInFlight.add(badgeLevel)) {
      _events.tryEmit("इस स्तर का कॉइन इनाम पहले ही मिल चुका है।")
      return
    }
    viewModelScope.launch {
      try {
        repository.claimLevelRewardCoins(badgeLevel, reward)
          .onSuccess { granted ->
            if (granted > 0L) {
              _claimedLevelTiers.update { it + badgeLevel }
              _events.tryEmit("🎉 LV.$badgeLevel बोनस: +$granted कॉइंस आपके वॉलेट में जुड़ गए!")
            } else {
              _claimedLevelTiers.update { it + badgeLevel }
              _events.tryEmit("यह इनाम पहले ही प्राप्त हो चुका है।")
            }
          }
          .onFailure { err ->
            _events.tryEmit(err.message ?: "इनाम कॉइंस लेने में त्रुटि")
          }
      } finally {
        claimingTiersInFlight.remove(badgeLevel)
      }
    }
  }

  fun sendEmojiReaction(item: EmojiCatalogItem) {
    val display = buildString {
      if (item.symbol.isNotBlank()) append(item.symbol).append(" ")
      append(item.label)
    }.trim()
    sendRoomText(display, "emoji")
  }

  fun sendGift(
    gift: GiftCatalogItem,
    recipientUid: String,
    recipientName: String,
    quantity: Int,
  ) {
    val room = activeRoom.value ?: return
    viewModelScope.launch {
      repository.sendGiftTransaction(
        room = room,
        sender = currentProfileOrDefault(),
        recipientUid = recipientUid,
        recipientName = recipientName,
        gift = gift,
        quantity = quantity,
      ).onSuccess { event ->
        // Snapshot observer also receives this event; mark & enqueue strictly for the active room
        if (_activeRoomId.value == event.roomId && seenGiftEventIds.add(event.id)) {
          enqueueFullScreenGift(event)
          triggerOrQueueMarquee(event)
        }
      }.onFailure { err ->
        _events.tryEmit(err.message ?: "गिफ्ट भेजा नहीं जा सका")
      }
    }
  }

  fun claimDailyBonusCoins() {
    viewModelScope.launch {
      repository.claimBonusCoins(500L)
        .onSuccess { newBalance ->
          _events.tryEmit("🪙 +500 कॉइंस जुड़े! नया बैलेंस: $newBalance")
        }
        .onFailure {
          _events.tryEmit("कॉइंस जोड़ने में त्रुटि")
        }
    }
  }

  private fun unwrapFirestoreMessage(error: Throwable, fallback: String): String {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val rootError = firestoreError ?: error
    Log.w(TAG, "Stream error", rootError)
    return rootError.message ?: fallback
  }

  override fun onCleared() {
    super.onCleared()
    registeredNetworkCallback?.let { cb ->
      runCatching { registeredConnectivityManager?.unregisterNetworkCallback(cb) }
    }
    registeredNetworkCallback = null
    registeredConnectivityManager = null
    appContext?.let { ctx ->
      SeatPresenceTracker.cleanupSynchronously(ctx, timeoutMs = 800L)
    }
    SeatPresenceTracker.registerAudioDisconnect(null)
    audioEngine.release()
  }

  companion object {
    private const val TAG = "SwarRoomVM"
    private const val STOP_TIMEOUT_MS = 5000L
    private const val ADMIN_EMAIL = "chndra3735@gmail.com"
    private const val KEY_BLOCKED_UIDS = "blocked_uids"
    private const val KEY_HIDDEN_EMOJIS = "hidden_emojis"
    private const val KEY_HIDDEN_GIFTS = "hidden_gifts"
    const val HEARTBEAT_INTERVAL_MS = 7_000L
    const val PRESENCE_STALE_MS = 25_000L
    const val SEAT_STALE_MS = 28_000L

    fun isTimestampFresh(
      tsMillis: Long?,
      estimatedServerNowMs: Long,
      staleThresholdMs: Long = PRESENCE_STALE_MS,
    ): Boolean {
      if (tsMillis == null) return true
      return (estimatedServerNowMs - tsMillis) <= staleThresholdMs
    }

    fun filterLiveRoomMessages(
      messages: List<RoomMessage>,
      blockedUids: Set<String>,
      liveMemberUids: Set<String>,
      roomJoinedAtMs: Long,
      sessionJoinWindowMs: Long = 5_000L,
    ): List<RoomMessage> {
      return messages.filter { msg ->
        if (msg.senderUid in blockedUids) return@filter false
        if (msg.messageType == "join") {
          if (msg.senderUid !in liveMemberUids) return@filter false
          if (roomJoinedAtMs > 0L) {
            val createdMs = msg.createdAt?.toDate()?.time
            if (createdMs != null && createdMs < roomJoinedAtMs - sessionJoinWindowMs) {
              return@filter false
            }
          }
        }
        true
      }
    }
  }
}
