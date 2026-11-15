package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.VoiceAudioEngine
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.EmojiCatalogItem
import com.example.data.model.GiftCatalogItem
import com.example.data.model.GiftEvent
import com.example.data.model.MomentPost
import com.example.data.model.RoomMessage
import com.example.data.model.RoomSeat
import com.example.data.model.SwarCatalogDefaults
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
import com.example.data.repository.SupabaseStorageRepository
import com.example.data.repository.SwarRoomRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

  // --- Emoji & Gift Catalogs (built-in 4 categories + admin-uploaded Firestore items) ---
  val emojiCatalog: StateFlow<List<EmojiCatalogItem>> =
    repository.observeEmojiCatalog()
      .map { remote -> remote.reversed() + SwarCatalogDefaults.builtInEmojis }
      .catch { emit(SwarCatalogDefaults.builtInEmojis) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SwarCatalogDefaults.builtInEmojis)

  val giftCatalog: StateFlow<List<GiftCatalogItem>> =
    repository.observeGiftCatalog()
      .map { remote -> remote.reversed() + SwarCatalogDefaults.builtInGifts }
      .catch { emit(SwarCatalogDefaults.builtInGifts) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SwarCatalogDefaults.builtInGifts)

  // --- Global Gift Marquee & Full-Screen Gift Playback ---
  private val _latestMarqueeGift = MutableStateFlow<GiftEvent?>(null)
  val latestMarqueeGift: StateFlow<GiftEvent?> = _latestMarqueeGift.asStateFlow()

  private val _activeFullScreenGift = MutableStateFlow<GiftEvent?>(null)
  val activeFullScreenGift: StateFlow<GiftEvent?> = _activeFullScreenGift.asStateFlow()

  private val seenGiftEventIds = mutableSetOf<String>()
  private val pendingGiftQueue = ArrayDeque<GiftEvent>()
  private var initialGiftSnapshotHandled = false

  // --- Active Voice Room ---
  private val _activeRoomId = MutableStateFlow<String?>(null)
  val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

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

  val activeRoomMessages: StateFlow<List<RoomMessage>> =
    combine(
      _activeRoomId.flatMapLatest { roomId ->
        if (roomId == null) flowOf(emptyList())
        else repository.observeRoomMessages(roomId).catch { emit(emptyList()) }
      },
      _blockedUserIds,
    ) { msgs, blocked ->
      if (blocked.isEmpty()) msgs else msgs.filter { it.senderUid !in blocked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  // --- Active Direct Conversation ---
  private val _activeConversationId = MutableStateFlow<String?>(null)
  val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

  val activeDirectMessages: StateFlow<List<DirectMessage>> =
    _activeConversationId.flatMapLatest { convoId ->
      if (convoId == null) flowOf(emptyList())
      else repository.observeDirectMessages(convoId).catch { emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  // --- Live Voice Mic State ---
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
    viewModelScope.launch {
      repository.ensureUserProfile(initialDisplayName, initialAvatarUrl)
    }

    // Observe cross-room gift broadcasts for the right-to-left marquee & full-screen room playback
    viewModelScope.launch {
      repository.observeGlobalGiftEvents()
        .catch { /* logged by repository */ }
        .collect { events ->
          if (events.isNotEmpty()) {
            _latestMarqueeGift.value = events.first()
          }
          if (!initialGiftSnapshotHandled) {
            events.forEach { seenGiftEventIds.add(it.id) }
            initialGiftSnapshotHandled = true
          } else {
            val newRoomEvents = events
              .filter { it.id.isNotBlank() && seenGiftEventIds.add(it.id) }
              .filter { it.roomId == _activeRoomId.value }
              .reversed()
            newRoomEvents.forEach { enqueueFullScreenGift(it) }
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
      }
    }
  }

  private fun enqueueFullScreenGift(event: GiftEvent) {
    if (_activeFullScreenGift.value == null) {
      _activeFullScreenGift.value = event
    } else {
      pendingGiftQueue.addLast(event)
    }
  }

  fun dismissFullScreenGift() {
    val next = if (pendingGiftQueue.isNotEmpty()) pendingGiftQueue.removeFirst() else null
    _activeFullScreenGift.value = next
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
    val prevRoomId = _activeRoomId.value
    if (prevRoomId == roomId) {
      _isRoomMinimized.value = false
      return
    }
    _isRoomMinimized.value = false
    val prevSeatHint = if (prevRoomId != null) {
      activeRoomSeats.value.firstOrNull { it.occupantUid == currentUserId }?.seatIndex?.toInt()
    } else {
      null
    }
    if (prevRoomId != null) {
      stopMicEngine()
    }
    _activeRoomId.value = roomId
    audioEngine.setSpeakerphone(_isSpeakerOn.value)
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
      if (room != null && room.ownerId == currentUserId) {
        runCatching { repository.ensureMinimumSeats(room, 22) }
      }
      if (sendJoinMessage) {
        val profile = currentProfileOrDefault()
        if (room != null) {
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
      }
    }
  }

  fun leaveActiveRoom(onComplete: (() -> Unit)? = null) {
    val roomId = _activeRoomId.value
    if (roomId == null) {
      _isRoomMinimized.value = false
      onComplete?.invoke()
      return
    }
    val seatHint = activeRoomSeats.value.firstOrNull { it.occupantUid == currentUserId }?.seatIndex?.toInt()
    stopMicEngine()
    _isRoomMinimized.value = false
    _activeRoomId.value = null
    viewModelScope.launch {
      try {
        vacateRoomPresence(roomId, seatHint)
      } finally {
        onComplete?.invoke()
      }
    }
  }

  private suspend fun vacateRoomPresence(roomId: String, seatHint: Int?) {
    runCatching { repository.vacateUserSeatsInRoom(roomId, seatHint) }
      .onFailure { err -> Log.w(TAG, "Vacate seats error in $roomId", err) }
    runCatching { repository.adjustListenerCount(roomId, -1L) }
      .onFailure { err -> Log.w(TAG, "Decrement listenerCount error in $roomId", err) }
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
    if (nextMuted) {
      audioEngine.stopCapture()
    } else {
      audioEngine.startCapture()
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
    audioEngine.startCapture()
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
    audioEngine.stopCapture()
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
    videoUri: Uri?,
    onSuccess: () -> Unit,
  ) {
    if (!isAdminUser) {
      _events.tryEmit("केवल एडमिन Gmail (chndra3735@gmail.com) से ही नया गिफ्ट जोड़ा जा सकता है।")
      return
    }
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
            _events.tryEmit(err.message ?: "गिफ्ट थंबनेल अपलोड विफल")
            return@launch
          }
      }

      if (videoUri != null) {
        _isUploadingMedia.value = true
        _events.tryEmit("गिफ्ट वीडियो कम MB में कम्प्रेश व अपलोड हो रहा है...")
        val upload = supabaseStorage.uploadMediaFromUri(
          uri = videoUri,
          folder = "gifts",
          uid = currentUserId,
          isVideo = true,
        )
        _isUploadingMedia.value = false
        upload.onSuccess { videoUrl = it }
          .onFailure { err ->
            _events.tryEmit(err.message ?: "गिफ्ट वीडियो अपलोड विफल")
            return@launch
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
        _events.tryEmit("नया गिफ्ट सभी यूज़र्स के लिए लाइव हो गया! 🎁")
        onSuccess()
      }.onFailure { err ->
        _events.tryEmit(err.message ?: "गिफ्ट जोड़ने में त्रुटि")
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
    if (item.id.isBlank() || item.id.startsWith("def_")) {
      _events.tryEmit("डिफ़ॉल्ट इमोजी डिलीट नहीं किया जा सकता।")
      return
    }
    viewModelScope.launch {
      _isUploadingMedia.value = true
      repository.deleteEmojiFromCatalog(item.id)
        .onSuccess {
          val oldImageUrl = item.imageUrl
          if (oldImageUrl.isNotBlank()) {
            supabaseStorage.deleteMediaByPublicUrl(oldImageUrl)
              .onSuccess { deleted ->
                _events.tryEmit(
                  if (deleted) "इमोजी ऐप और Supabase Storage दोनों से डिलीट हो गया!"
                  else "इमोजी ऐप से डिलीट हो गया!"
                )
              }
              .onFailure { err ->
                _events.tryEmit("ऐप से इमोजी हटाया गया, पर Supabase फ़ाइल डिलीट नहीं हुई: ${err.message}")
              }
          } else {
            _events.tryEmit("इमोजी पूरी तरह डिलीट हो गया!")
          }
          onSuccess()
        }
        .onFailure { err ->
          _events.tryEmit(err.message ?: "इमोजी डिलीट करने में त्रुटि")
        }
      _isUploadingMedia.value = false
    }
  }

  fun updateCustomGift(
    existing: GiftCatalogItem,
    name: String,
    category: String,
    emoji: String,
    coinCost: Long,
    newThumbnailUri: Uri?,
    newVideoUri: Uri?,
    removeExistingThumbnail: Boolean,
    removeExistingVideo: Boolean,
    onSuccess: () -> Unit,
  ) {
    if (!isAdminUser) {
      _events.tryEmit("केवल एडमिन Gmail (chndra3735@gmail.com) से ही गिफ्ट अपडेट किया जा सकता है।")
      return
    }
    if (existing.id.isBlank() || existing.id.startsWith("def_")) {
      _events.tryEmit("डिफ़ॉल्ट गिफ्ट बदला नहीं जा सकता।")
      return
    }
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
            _events.tryEmit(err.message ?: "नया थंबनेल अपलोड विफल")
            return@launch
          }
      }

      if (newVideoUri != null) {
        _isUploadingMedia.value = true
        _events.tryEmit("नया गिफ्ट वीडियो कम MB में कम्प्रेश व अपलोड हो रहा है...")
        val upload = supabaseStorage.uploadMediaFromUri(
          uri = newVideoUri,
          folder = "gifts",
          uid = currentUserId,
          isVideo = true,
        )
        _isUploadingMedia.value = false
        upload.onSuccess { finalVideoUrl = it }
          .onFailure { err ->
            _events.tryEmit(err.message ?: "नया गिफ्ट वीडियो अपलोड विफल")
            return@launch
          }
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
        _events.tryEmit("गिफ्ट सफलतापूर्वक अपडेट हो गया! 🎁")
        onSuccess()
      }.onFailure { err ->
        _events.tryEmit(err.message ?: "गिफ्ट अपडेट करने में त्रुटि")
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
    if (gift.id.isBlank() || gift.id.startsWith("def_")) {
      _events.tryEmit("डिफ़ॉल्ट गिफ्ट डिलीट नहीं किया जा सकता।")
      return
    }
    viewModelScope.launch {
      _isUploadingMedia.value = true
      repository.deleteGiftFromCatalog(gift.id)
        .onSuccess {
          val mediaUrls = listOf(gift.thumbnailUrl, gift.mediaUrl).filter { it.isNotBlank() }.distinct()
          if (mediaUrls.isNotEmpty()) {
            supabaseStorage.deleteMediaByPublicUrls(mediaUrls)
              .onSuccess { deletedCount ->
                _events.tryEmit(
                  if (deletedCount > 0) "गिफ्ट ऐप और Supabase Storage दोनों से पूरी तरह डिलीट हो गया!"
                  else "गिफ्ट ऐप से पूरी तरह डिलीट हो गया!"
                )
              }
              .onFailure { err ->
                _events.tryEmit("ऐप से गिफ्ट हटाया गया, पर Supabase फ़ाइल डिलीट नहीं हुई: ${err.message}")
              }
          } else {
            _events.tryEmit("गिफ्ट पूरी तरह डिलीट हो गया!")
          }
          onSuccess()
        }
        .onFailure { err ->
          _events.tryEmit(err.message ?: "गिफ्ट डिलीट करने में त्रुटि")
        }
      _isUploadingMedia.value = false
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
        // Snapshot observer also receives this event; mark & enqueue immediately if not yet seen for zero latency
        if (seenGiftEventIds.add(event.id)) {
          _latestMarqueeGift.value = event
          enqueueFullScreenGift(event)
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
    audioEngine.release()
  }

  private companion object {
    const val TAG = "SwarRoomVM"
    const val STOP_TIMEOUT_MS = 5000L
    const val ADMIN_EMAIL = "chndra3735@gmail.com"
    const val KEY_BLOCKED_UIDS = "blocked_uids"
  }
}
