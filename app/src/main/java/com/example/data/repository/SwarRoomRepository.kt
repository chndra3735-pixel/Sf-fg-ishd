package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Conversation
import com.example.data.model.CpRelationship
import com.example.data.model.DirectMessage
import com.example.data.model.EmojiCatalogItem
import com.example.data.model.GiftCatalogItem
import com.example.data.model.GiftEvent
import com.example.data.model.LevelRewardClaim
import com.example.data.model.LevelTierConfig
import com.example.data.model.MomentPost
import com.example.data.model.NumericIdLookup
import com.example.data.model.RoomKickLog
import com.example.data.model.RoomMessage
import com.example.data.model.RoomPresenceMember
import com.example.data.model.RoomSeat
import com.example.data.model.SwarCatalogDefaults
import com.example.data.model.SwarUserIdentity
import com.example.data.model.UserActivityLog
import com.example.data.model.UserAuditProfile
import com.example.data.model.UserProfile
import com.example.data.model.VoiceRoom
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class RoomGiftEventsSnapshot(
  val events: List<GiftEvent>,
  val isFromCache: Boolean,
)

class SwarRoomRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  // --- User Profile ---

  fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
    val docRef = db.collection("users").document(uid)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error -> handleFlowError(error, OperationType.GET, docRef.path, uid); throw error }
    )
  }

  suspend fun ensureUserProfile(
    displayName: String,
    avatarUrl: String,
  ): Result<UserProfile> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val existing = docRef.get().await()
    if (existing.exists()) {
      val profile = existing.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
      if (profile != null) return@runCatching profile
    }
    val safeName = displayName.trim().take(40).ifBlank { "Swar User" }
    val safeHandle = safeName.lowercase().replace(Regex("[^a-z0-9_]"), "_").take(20).ifBlank { "user_${uid.take(6)}" }
    val payload = mapOf(
      "userId" to uid,
      "displayName" to safeName,
      "handle" to safeHandle,
      "avatarUrl" to avatarUrl.take(600),
      "bio" to "SwarRoom पर लाइव बातचीत और संगीत!",
      "gender" to "male",
      "clubTag" to "SWAR",
      "coins" to 1500L,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      docRef.get().await().toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
        ?: UserProfile(id = uid, userId = uid, displayName = safeName, handle = safeHandle, avatarUrl = avatarUrl)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateUserProfile(
    displayName: String,
    handle: String,
    bio: String,
    gender: String,
    clubTag: String,
    avatarUrl: String? = null,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val updates = mutableMapOf<String, Any>(
      "displayName" to displayName.trim().take(40).ifBlank { "Swar User" },
      "handle" to handle.trim().take(30).ifBlank { "swar_${uid.take(6)}" },
      "bio" to bio.trim().take(160),
      "gender" to if (gender in listOf("male", "female", "other")) gender else "other",
      "clubTag" to clubTag.trim().take(16).ifBlank { "SWAR" },
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (avatarUrl != null) {
      updates["avatarUrl"] = avatarUrl.take(600)
    }
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun getUserProfile(uid: String): UserProfile? {
    val docRef = db.collection("users").document(uid)
    return try {
      docRef.get().await().toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  // --- Voice Rooms ---

  fun observePublicRooms(): Flow<List<VoiceRoom>> = flow {
    val uid = requireUserId()
    val query = db.collection("rooms").whereEqualTo("isPublic", true)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(VoiceRoom::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "rooms", uid); throw error }
    )
  }

  fun observeRoom(roomId: String): Flow<VoiceRoom?> = flow {
    val uid = requireUserId()
    val docRef = db.collection("rooms").document(roomId)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(VoiceRoom::class.java, ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error -> handleFlowError(error, OperationType.GET, docRef.path, uid); throw error }
    )
  }

  suspend fun getRoom(roomId: String): VoiceRoom? {
    requireUserId()
    val docRef = db.collection("rooms").document(roomId)
    return try {
      docRef.get().await().toObject(VoiceRoom::class.java, ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  suspend fun createRoom(
    title: String,
    category: String,
    backgroundTheme: String,
    clubTag: String,
    welcomeNotice: String,
    coverUrl: String,
    seatCount: Int,
    hostProfile: UserProfile,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val roomRef = db.collection("rooms").document(roomId)
    val validatedSeats = seatCount.coerceIn(1, 22)
    val roomCode = (100000..999999).random().toString()
    val safeTheme = if (backgroundTheme in listOf("stadium", "aurora", "neon", "sunset", "custom")) backgroundTheme else "stadium"

    val batch = db.batch()
    val roomPayload = mapOf(
      "ownerId" to uid,
      "ownerName" to hostProfile.displayName.take(40).ifBlank { "Host" },
      "ownerAvatarUrl" to hostProfile.avatarUrl.take(600),
      "title" to title.trim().take(60).ifBlank { "लाइव वॉइस रूम" },
      "roomCode" to roomCode,
      "category" to category.trim().take(24).ifBlank { "लोकप्रिय" },
      "coverUrl" to coverUrl.take(600),
      "backgroundTheme" to safeTheme,
      "themeBackgroundUrl" to "",
      "clubTag" to clubTag.trim().take(16).ifBlank { hostProfile.clubTag.ifBlank { "SWAR" } },
      "welcomeNotice" to welcomeNotice.trim().take(200).ifBlank {
        "SwarRoom में आपका स्वागत है। कृपया एक दूसरे का सम्मान करें और सभ्य तरीके से चैट करें।"
      },
      "seatCount" to validatedSeats.toLong(),
      "managerUids" to emptyList<String>(),
      "isPinLocked" to false,
      "roomPin" to "",
      "listenerCount" to 1L,
      "isLive" to true,
      "isPublic" to true,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    batch.set(roomRef, roomPayload)

    for (index in 0 until validatedSeats) {
      val seatRef = roomRef.collection("seats").document("seat_$index")
      val isHostSeat = index == 0
      val seatPayload = mapOf(
        "roomId" to roomId,
        "roomOwnerId" to uid,
        "isRoomPublic" to true,
        "seatIndex" to index.toLong(),
        "occupantUid" to if (isHostSeat) uid else "",
        "occupantName" to if (isHostSeat) hostProfile.displayName.take(40) else "",
        "occupantAvatarUrl" to if (isHostSeat) hostProfile.avatarUrl.take(600) else "",
        "isLocked" to false,
        "isMuted" to false,
        "isSpeaking" to false,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      batch.set(seatRef, seatPayload)
    }

    try {
      batch.commit().await()
      sendRoomSystemMessage(
        roomId = roomId,
        roomOwnerId = uid,
        isRoomPublic = true,
        senderName = hostProfile.displayName.ifBlank { "Host" },
        senderAvatarUrl = hostProfile.avatarUrl,
        text = "${hostProfile.displayName} का कमरे में स्वागत है",
        messageType = "join",
        badgeTag = clubTag.ifBlank { "HOST" },
      )
      roomId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, roomRef.path)
      throw e
    }
  }

  suspend fun ensureMinimumSeats(room: VoiceRoom, targetSeatCount: Int = 22): Result<Unit> = runCatching {
    val uid = requireUserId()
    if (room.ownerId != uid) return@runCatching
    val seatsCollection = db.collection("rooms").document(room.id).collection("seats")
    val existing = seatsCollection.whereEqualTo("isRoomPublic", room.isPublic).get().await()
    val existingIndices = existing.documents
      .mapNotNull { it.getLong("seatIndex")?.toInt() }
      .toSet()
    val missingIndices = (0 until targetSeatCount).filter { it !in existingIndices }
    if (missingIndices.isEmpty()) return@runCatching
    val batch = db.batch()
    missingIndices.forEach { index ->
      val seatRef = seatsCollection.document("seat_$index")
      val seatPayload = mapOf(
        "roomId" to room.id,
        "roomOwnerId" to room.ownerId,
        "isRoomPublic" to room.isPublic,
        "seatIndex" to index.toLong(),
        "occupantUid" to "",
        "occupantName" to "",
        "occupantAvatarUrl" to "",
        "isLocked" to false,
        "isMuted" to false,
        "isSpeaking" to false,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      batch.set(seatRef, seatPayload)
    }
    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "rooms/${room.id}/seats")
      throw e
    }
  }

  suspend fun updateRoomSettings(
    roomId: String,
    title: String,
    backgroundTheme: String,
    welcomeNotice: String,
    coverUrl: String? = null,
    themeBackgroundUrl: String? = null,
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("rooms").document(roomId)
    val safeTheme = if (backgroundTheme in listOf("stadium", "aurora", "neon", "sunset", "custom")) {
      backgroundTheme
    } else {
      "stadium"
    }
    val updates = mutableMapOf<String, Any>(
      "title" to title.trim().take(60).ifBlank { "लाइव वॉइस रूम" },
      "backgroundTheme" to safeTheme,
      "welcomeNotice" to welcomeNotice.trim().take(200),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (coverUrl != null) {
      updates["coverUrl"] = coverUrl.trim().take(600)
    }
    if (themeBackgroundUrl != null) {
      updates["themeBackgroundUrl"] = themeBackgroundUrl.trim().take(800)
    }
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun updateRoomIdentity(
    roomId: String,
    title: String,
    welcomeNotice: String,
    coverUrl: String? = null,
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("rooms").document(roomId)
    val updates = mutableMapOf<String, Any>(
      "title" to title.trim().take(60).ifBlank { "लाइव वॉइस रूम" },
      "welcomeNotice" to welcomeNotice.trim().take(200),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (coverUrl != null) {
      updates["coverUrl"] = coverUrl.trim().take(600)
    }
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun updateRoomTheme(
    roomId: String,
    backgroundTheme: String,
    themeBackgroundUrl: String,
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("rooms").document(roomId)
    val safeTheme = if (backgroundTheme in listOf("stadium", "aurora", "neon", "sunset", "custom")) {
      backgroundTheme
    } else {
      "stadium"
    }
    val updates = mapOf(
      "backgroundTheme" to safeTheme,
      "themeBackgroundUrl" to themeBackgroundUrl.trim().take(800),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun updateRoomManagers(
    roomId: String,
    managerUids: List<String>,
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("rooms").document(roomId)
    val sanitized = managerUids
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
      .take(5)
    try {
      docRef.update(
        mapOf(
          "managerUids" to sanitized,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun updateRoomPinLock(
    roomId: String,
    isPinLocked: Boolean,
    roomPin: String,
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("rooms").document(roomId)
    val cleanPin = roomPin.filter { it.isDigit() }.take(8)
    require(!isPinLocked || cleanPin.length in 4..8) {
      "रूम लॉक करने के लिए 4 से 8 अंकों का पिन कोड डालें।"
    }
    try {
      docRef.update(
        mapOf(
          "isPinLocked" to isPinLocked,
          "roomPin" to if (isPinLocked) cleanPin else "",
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun resizeRoomSeats(
    room: VoiceRoom,
    newSeatCount: Int,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(room.ownerId == uid) { "केवल रूम होस्ट ही सीटों की संख्या बदल सकते हैं।" }
    val safeCount = newSeatCount.coerceIn(1, 22)
    val roomRef = db.collection("rooms").document(room.id)
    val seatsCollection = roomRef.collection("seats")
    val existing = seatsCollection.whereEqualTo("isRoomPublic", room.isPublic).get().await()
    val existingIndices = existing.documents
      .mapNotNull { it.getLong("seatIndex")?.toInt() }
      .toSet()

    val batch = db.batch()
    batch.update(
      roomRef,
      mapOf(
        "seatCount" to safeCount.toLong(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )

    for (index in 0 until safeCount) {
      if (index !in existingIndices) {
        val seatRef = seatsCollection.document("seat_$index")
        val seatPayload = mapOf(
          "roomId" to room.id,
          "roomOwnerId" to room.ownerId,
          "isRoomPublic" to room.isPublic,
          "seatIndex" to index.toLong(),
          "occupantUid" to "",
          "occupantName" to "",
          "occupantAvatarUrl" to "",
          "isLocked" to false,
          "isMuted" to false,
          "isSpeaking" to false,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        batch.set(seatRef, seatPayload)
      }
    }

    existing.documents.forEach { doc ->
      val idx = doc.getLong("seatIndex")?.toInt() ?: 0
      if (idx >= safeCount) {
        batch.delete(doc.reference)
      }
    }

    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
      throw e
    }
  }

  suspend fun upsertNumericIdLookup(
    displayName: String,
    avatarUrl: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val numericId = SwarUserIdentity.numericIdForUid(uid)
    val docRef = db.collection("numeric_id_lookup").document(numericId)
    val payload = mapOf(
      "userId" to uid,
      "numericId" to numericId,
      "displayName" to displayName.trim().take(40).ifBlank { "Swar User" },
      "avatarUrl" to avatarUrl.trim().take(600),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun findUserByNumericId(numericId: String): NumericIdLookup? {
    requireUserId()
    val cleanId = numericId.filter { it.isDigit() }
    if (cleanId.length != 10) return null
    val docRef = db.collection("numeric_id_lookup").document(cleanId)
    return try {
      val snap = docRef.get().await()
      if (snap.exists()) snap.toObject(NumericIdLookup::class.java, ServerTimestampBehavior.ESTIMATE)
      else null
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  fun observeRoomKickLogs(roomId: String): Flow<List<RoomKickLog>> = flow {
    val uid = requireUserId()
    val query = db.collection("rooms").document(roomId).collection("kick_logs")
      .whereEqualTo("isRoomPublic", true)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(RoomKickLog::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.kickedAtMs }
            .take(80)
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "rooms/$roomId/kick_logs", uid); throw error }
    )
  }

  suspend fun createRoomKickLog(
    room: VoiceRoom,
    actor: UserProfile,
    actorRole: String,
    targetUid: String,
    targetName: String,
    targetAvatarUrl: String,
    reason: String = "रूम से बाहर किया गया",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val safeRole = if (actorRole == "manager") "manager" else "owner"
    val logId = "kick_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("rooms").document(room.id).collection("kick_logs").document(logId)
    val payload = mapOf(
      "roomId" to room.id,
      "roomOwnerId" to room.ownerId,
      "isRoomPublic" to room.isPublic,
      "actorUid" to uid,
      "actorNumericId" to SwarUserIdentity.numericIdForUid(uid).take(20),
      "actorName" to actor.displayName.trim().take(40).ifBlank { "Admin" },
      "actorAvatarUrl" to actor.avatarUrl.trim().take(600),
      "actorRole" to safeRole,
      "targetUid" to targetUid.trim().take(128),
      "targetNumericId" to SwarUserIdentity.numericIdForUid(targetUid).take(20),
      "targetName" to targetName.trim().take(40).ifBlank { "Member" },
      "targetAvatarUrl" to targetAvatarUrl.trim().take(600),
      "reason" to reason.trim().take(200),
      "kickedAtMs" to System.currentTimeMillis(),
      "createdAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun adjustListenerCount(roomId: String, delta: Long): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("rooms").document(roomId)
    try {
      db.runTransaction { tx ->
        val snap = tx.get(docRef)
        if (!snap.exists()) return@runTransaction
        val current = snap.getLong("listenerCount") ?: 1L
        val next = (current + delta).coerceIn(0L, 9999L)
        tx.update(docRef, mapOf("listenerCount" to next, "updatedAt" to FieldValue.serverTimestamp()))
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun syncRoomListenerCount(roomId: String, exactCount: Long): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("rooms").document(roomId)
    val clamped = exactCount.coerceIn(0L, 9999L)
    try {
      docRef.update(
        mapOf(
          "listenerCount" to clamped,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteRoom(roomId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("rooms").document(roomId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- Room Seats ---

  fun observeRoomSeats(roomId: String): Flow<List<RoomSeat>> = flow {
    val uid = requireUserId()
    val query = db.collection("rooms").document(roomId).collection("seats")
      .whereEqualTo("isRoomPublic", true)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(RoomSeat::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.seatIndex }
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "rooms/$roomId/seats", uid); throw error }
    )
  }

  suspend fun claimSeat(roomId: String, seatIndex: Int, profile: UserProfile): Result<Unit> = runCatching {
    val uid = requireUserId()
    val seatRef = db.collection("rooms").document(roomId).collection("seats").document("seat_$seatIndex")
    try {
      seatRef.update(
        mapOf(
          "occupantUid" to uid,
          "occupantName" to profile.displayName.take(40).ifBlank { "Speaker" },
          "occupantAvatarUrl" to profile.avatarUrl.take(600),
          "isMuted" to false,
          "isSpeaking" to false,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, seatRef.path)
      throw e
    }
  }

  suspend fun moveToSeat(
    roomId: String,
    vacateSeatIndices: List<Int>,
    targetSeatIndex: Int,
    profile: UserProfile,
    preserveMuted: Boolean = false,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val seatsCollection = db.collection("rooms").document(roomId).collection("seats")
    val batch = db.batch()
    val emptyUpdates = mapOf(
      "occupantUid" to "",
      "occupantName" to "",
      "occupantAvatarUrl" to "",
      "isMuted" to false,
      "isSpeaking" to false,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    vacateSeatIndices
      .distinct()
      .filter { it != targetSeatIndex }
      .forEach { oldIdx ->
        batch.update(seatsCollection.document("seat_$oldIdx"), emptyUpdates)
      }
    val targetUpdates = mutableMapOf<String, Any>(
      "occupantUid" to uid,
      "occupantName" to profile.displayName.take(40).ifBlank { "Speaker" },
      "occupantAvatarUrl" to profile.avatarUrl.take(600),
      "isSpeaking" to false,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (!preserveMuted) {
      targetUpdates["isMuted"] = false
    }
    batch.update(seatsCollection.document("seat_$targetSeatIndex"), targetUpdates)
    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "rooms/$roomId/seats/seat_$targetSeatIndex")
      throw e
    }
  }

  suspend fun leaveSeat(roomId: String, seatIndex: Int): Result<Unit> = runCatching {
    requireUserId()
    val seatRef = db.collection("rooms").document(roomId).collection("seats").document("seat_$seatIndex")
    try {
      seatRef.update(
        mapOf(
          "occupantUid" to "",
          "occupantName" to "",
          "occupantAvatarUrl" to "",
          "isMuted" to false,
          "isSpeaking" to false,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, seatRef.path)
      throw e
    }
  }

  suspend fun updateSeatAudioState(
    roomId: String,
    seatIndex: Int,
    isMuted: Boolean,
    isSpeaking: Boolean,
  ): Result<Unit> = runCatching {
    requireUserId()
    val seatRef = db.collection("rooms").document(roomId).collection("seats").document("seat_$seatIndex")
    try {
      seatRef.update(
        mapOf(
          "isMuted" to isMuted,
          "isSpeaking" to isSpeaking,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, seatRef.path)
      throw e
    }
  }

  suspend fun toggleSeatLock(roomId: String, seatIndex: Int, lock: Boolean): Result<Unit> = runCatching {
    requireUserId()
    val seatRef = db.collection("rooms").document(roomId).collection("seats").document("seat_$seatIndex")
    try {
      val updates = mutableMapOf<String, Any>(
        "isLocked" to lock,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (lock) {
        updates["occupantUid"] = ""
        updates["occupantName"] = ""
        updates["occupantAvatarUrl"] = ""
        updates["isSpeaking"] = false
      }
      seatRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, seatRef.path)
      throw e
    }
  }

  suspend fun vacateUserSeatsInRoom(
    roomId: String,
    seatIndexHint: Int? = null,
    targetUid: String? = null,
  ): Result<Int> = runCatching {
    val uid = targetUid?.takeIf { it.isNotBlank() } ?: requireUserId()
    val seatsCollection = db.collection("rooms").document(roomId).collection("seats")
    val emptyUpdates = mapOf(
      "occupantUid" to "",
      "occupantName" to "",
      "occupantAvatarUrl" to "",
      "isMuted" to false,
      "isSpeaking" to false,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    var vacatedCount = 0
    try {
      val matching = seatsCollection
        .whereEqualTo("isRoomPublic", true)
        .whereEqualTo("occupantUid", uid)
        .get()
        .await()
      if (!matching.isEmpty) {
        val batch = db.batch()
        matching.documents.forEach { doc ->
          batch.update(doc.reference, emptyUpdates)
          vacatedCount++
        }
        batch.commit().await()
      } else if (seatIndexHint != null) {
        val seatRef = seatsCollection.document("seat_$seatIndexHint")
        val snap = seatRef.get().await()
        if (snap.exists() && snap.getString("occupantUid") == uid) {
          seatRef.update(emptyUpdates).await()
          vacatedCount++
        }
      }
      vacatedCount
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "rooms/$roomId/seats")
      throw e
    }
  }

  suspend fun updateUserSeatProfile(
    roomId: String,
    displayName: String,
    avatarUrl: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val safeName = displayName.trim().take(40).ifBlank { "Swar User" }
    val safeAvatar = avatarUrl.take(600)
    val roomRef = db.collection("rooms").document(roomId)
    try {
      val roomSnap = roomRef.get().await()
      if (roomSnap.exists() && roomSnap.getString("ownerId") == uid) {
        roomRef.update(
          mapOf(
            "ownerName" to safeName,
            "ownerAvatarUrl" to safeAvatar,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      val matchingSeats = roomRef.collection("seats")
        .whereEqualTo("isRoomPublic", true)
        .whereEqualTo("occupantUid", uid)
        .get()
        .await()
      if (!matchingSeats.isEmpty) {
        val batch = db.batch()
        matchingSeats.documents.forEach { doc ->
          batch.update(
            doc.reference,
            mapOf(
              "occupantName" to safeName,
              "occupantAvatarUrl" to safeAvatar,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        batch.commit().await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, roomRef.path)
      throw e
    }
  }

  // --- Real-Time Room Text Messages ---

  fun observeRoomMessages(roomId: String): Flow<List<RoomMessage>> = flow {
    val uid = requireUserId()
    val query = db.collection("rooms").document(roomId).collection("messages")
      .whereEqualTo("isRoomPublic", true)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(RoomMessage::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "rooms/$roomId/messages", uid); throw error }
    )
  }

  suspend fun sendRoomMessage(
    room: VoiceRoom,
    sender: UserProfile,
    text: String,
    messageType: String = "text",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val cleanText = text.trim().take(500)
    require(cleanText.isNotEmpty()) { "Message cannot be empty" }
    val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val msgRef = db.collection("rooms").document(room.id).collection("messages").document(msgId)
    val payload = mapOf(
      "roomId" to room.id,
      "roomOwnerId" to room.ownerId,
      "isRoomPublic" to room.isPublic,
      "senderUid" to uid,
      "senderName" to sender.displayName.take(40).ifBlank { "User" },
      "senderAvatarUrl" to sender.avatarUrl.take(600),
      "text" to cleanText,
      "messageType" to if (messageType in listOf("text", "join", "gift", "emoji")) messageType else "text",
      "badgeTag" to sender.clubTag.take(24),
      "createdAt" to FieldValue.serverTimestamp(),
    )
    try {
      msgRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path)
      throw e
    }
  }

  suspend fun sendRoomSystemMessage(
    roomId: String,
    roomOwnerId: String,
    isRoomPublic: Boolean,
    senderName: String,
    senderAvatarUrl: String,
    text: String,
    messageType: String,
    badgeTag: String,
  ) {
    val uid = requireUserId()
    val msgId = "sys_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val msgRef = db.collection("rooms").document(roomId).collection("messages").document(msgId)
    val payload = mapOf(
      "roomId" to roomId,
      "roomOwnerId" to roomOwnerId,
      "isRoomPublic" to isRoomPublic,
      "senderUid" to uid,
      "senderName" to senderName.take(40).ifBlank { "User" },
      "senderAvatarUrl" to senderAvatarUrl.take(600),
      "text" to text.trim().take(500).ifBlank { "Joined room" },
      "messageType" to if (messageType in listOf("text", "join", "gift", "emoji")) messageType else "join",
      "badgeTag" to badgeTag.take(24),
      "createdAt" to FieldValue.serverTimestamp(),
    )
    try {
      msgRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path)
    }
  }

  suspend fun deleteUserJoinMessagesInRoom(
    roomId: String,
    targetUid: String? = null,
  ): Result<Int> = runCatching {
    val uid = targetUid?.takeIf { it.isNotBlank() } ?: requireUserId()
    val msgCollection = db.collection("rooms").document(roomId).collection("messages")
    val matching = msgCollection
      .whereEqualTo("isRoomPublic", true)
      .whereEqualTo("senderUid", uid)
      .get()
      .await()
    val joinDocs = matching.documents.filter { it.getString("messageType") == "join" }
    if (joinDocs.isEmpty()) return@runCatching 0
    val batch = db.batch()
    joinDocs.forEach { doc -> batch.delete(doc.reference) }
    batch.commit().await()
    joinDocs.size
  }

  // --- Live Room Presence & Seat Heartbeats ---

  fun observeRoomPresence(roomId: String): Flow<List<RoomPresenceMember>> = flow {
    val uid = requireUserId()
    val query = db.collection("rooms").document(roomId).collection("presence")
      .whereEqualTo("isRoomPublic", true)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(RoomPresenceMember::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.joinedAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "rooms/$roomId/presence", uid); throw error }
    )
  }

  suspend fun upsertRoomPresence(
    room: VoiceRoom,
    profile: UserProfile,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val presenceRef = db.collection("rooms").document(room.id).collection("presence").document(uid)
    val safeName = profile.displayName.trim().take(40).ifBlank { "Swar User" }
    val safeAvatar = profile.avatarUrl.take(600)
    val safeClub = profile.clubTag.take(24)
    try {
      val existing = presenceRef.get().await()
      if (existing.exists()) {
        presenceRef.update(
          mapOf(
            "displayName" to safeName,
            "avatarUrl" to safeAvatar,
            "clubTag" to safeClub,
            "lastSeenAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        presenceRef.set(
          mapOf(
            "userId" to uid,
            "roomId" to room.id,
            "roomOwnerId" to room.ownerId,
            "isRoomPublic" to room.isPublic,
            "displayName" to safeName,
            "avatarUrl" to safeAvatar,
            "clubTag" to safeClub,
            "joinedAt" to FieldValue.serverTimestamp(),
            "lastSeenAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, presenceRef.path)
      throw e
    }
  }

  suspend fun touchRoomPresenceHeartbeat(
    roomId: String,
    profile: UserProfile,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val presenceRef = db.collection("rooms").document(roomId).collection("presence").document(uid)
    try {
      presenceRef.update(
        mapOf(
          "displayName" to profile.displayName.trim().take(40).ifBlank { "Swar User" },
          "avatarUrl" to profile.avatarUrl.take(600),
          "clubTag" to profile.clubTag.take(24),
          "lastSeenAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val room = getRoom(roomId)
      if (room != null) {
        upsertRoomPresence(room, profile).getOrThrow()
      } else {
        throw e
      }
    }
  }

  suspend fun touchSeatHeartbeat(roomId: String, seatIndex: Int): Result<Unit> = runCatching {
    requireUserId()
    val seatRef = db.collection("rooms").document(roomId).collection("seats").document("seat_$seatIndex")
    seatRef.update(mapOf("updatedAt" to FieldValue.serverTimestamp())).await()
  }

  suspend fun removeRoomPresence(
    roomId: String,
    targetUid: String? = null,
  ): Result<Unit> = runCatching {
    val uid = targetUid?.takeIf { it.isNotBlank() } ?: requireUserId()
    val presenceRef = db.collection("rooms").document(roomId).collection("presence").document(uid)
    try {
      presenceRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, presenceRef.path)
      throw e
    }
  }

  // --- 1-on-1 Direct Messaging ---

  fun observeConversations(): Flow<List<Conversation>> = flow {
    val uid = requireUserId()
    val query = db.collection("conversations").whereArrayContains("participantUids", uid)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(Conversation::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "conversations", uid); throw error }
    )
  }

  fun observeDirectMessages(conversationId: String): Flow<List<DirectMessage>> = flow {
    val uid = requireUserId()
    val query = db.collection("conversations").document(conversationId)
      .collection("messages")
      .whereArrayContains("participantUids", uid)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(DirectMessage::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleFlowError(error, OperationType.LIST, "conversations/$conversationId/messages", uid)
          throw error
        }
    )
  }

  suspend fun startOrGetConversation(
    currentUser: UserProfile,
    otherUid: String,
    otherName: String,
    otherAvatarUrl: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val sortedUids = listOf(uid, otherUid).sorted()
    val convoId = "${sortedUids[0]}_${sortedUids[1]}"
    val docRef = db.collection("conversations").document(convoId)
    val existing = docRef.get().await()
    if (!existing.exists()) {
      val payload = mapOf(
        "participantUids" to sortedUids,
        "participantNames" to mapOf(
          uid to currentUser.displayName.take(40).ifBlank { "Me" },
          otherUid to otherName.take(40).ifBlank { "Friend" },
        ),
        "participantAvatars" to mapOf(
          uid to currentUser.avatarUrl.take(600),
          otherUid to otherAvatarUrl.take(600),
        ),
        "lastMessage" to "नई बातचीत शुरू हुई",
        "lastSenderUid" to uid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      try {
        docRef.set(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }
    convoId
  }

  suspend fun sendDirectMessage(
    conversationId: String,
    participantUids: List<String>,
    senderName: String,
    text: String,
    mediaUrl: String = "",
    mediaType: String = "text",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val cleanMediaUrl = mediaUrl.trim().take(800)
    val safeMediaType = if (mediaType == "image" && cleanMediaUrl.isNotBlank()) "image" else "text"
    val cleanText = text.trim().take(1000).ifBlank {
      if (safeMediaType == "image") "📷 फोटो / DP भेजा" else ""
    }
    require(cleanText.isNotEmpty()) { "Message cannot be empty" }
    val msgId = "dm_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val convoRef = db.collection("conversations").document(conversationId)
    val msgRef = convoRef.collection("messages").document(msgId)

    val batch = db.batch()
    batch.set(
      msgRef,
      mapOf(
        "conversationId" to conversationId,
        "participantUids" to participantUids,
        "senderUid" to uid,
        "senderName" to senderName.take(40).ifBlank { "User" },
        "text" to cleanText,
        "mediaUrl" to cleanMediaUrl,
        "mediaType" to safeMediaType,
        "createdAt" to FieldValue.serverTimestamp(),
      ),
    )
    batch.update(
      convoRef,
      mapOf(
        "lastMessage" to cleanText.take(500),
        "lastSenderUid" to uid,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, msgRef.path)
      throw e
    }
  }

  // --- Community Video & Image Moments (Media stored in Supabase Storage) ---

  fun observeMoments(): Flow<List<MomentPost>> = flow {
    val uid = requireUserId()
    val query = db.collection("moments").whereEqualTo("isPublic", true)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(MomentPost::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "moments", uid); throw error }
    )
  }

  suspend fun createMomentPost(
    author: UserProfile,
    caption: String,
    mediaUrl: String,
    mediaType: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val momentId = "mom_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("moments").document(momentId)
    val payload = mapOf(
      "authorUid" to uid,
      "authorName" to author.displayName.take(40).ifBlank { "Creator" },
      "authorAvatarUrl" to author.avatarUrl.take(600),
      "caption" to caption.trim().take(300),
      "mediaUrl" to mediaUrl.take(800),
      "mediaType" to if (mediaType == "video") "video" else "image",
      "isPublic" to true,
      "likesCount" to 0L,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      momentId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun likeMoment(momentId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("moments").document(momentId)
    try {
      docRef.update(
        mapOf(
          "likesCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteMoment(momentId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("moments").document(momentId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- Admin-Managed Emoji & Gift Catalogs + Global Gift Events ---

  fun observeEmojiCatalog(): Flow<List<EmojiCatalogItem>> = flow {
    val uid = requireUserId()
    val query = db.collection("emoji_catalog").whereEqualTo("isActive", true)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(EmojiCatalogItem::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "emoji_catalog", uid); throw error }
    )
  }

  suspend fun addEmojiToCatalog(
    label: String,
    category: String,
    symbol: String,
    imageUrl: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val cleanLabel = label.trim().take(30).ifBlank { "कस्टम इमोजी" }
    val cleanSymbol = symbol.trim().take(16)
    val cleanImage = imageUrl.trim().take(800)
    require(cleanSymbol.isNotEmpty() || cleanImage.isNotEmpty()) {
      "इमोजी चिह्न या इमेज में से कम से कम एक आवश्यक है।"
    }
    val emojiId = "emo_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("emoji_catalog").document(emojiId)
    val payload = mapOf(
      "createdByUid" to uid,
      "label" to cleanLabel,
      "category" to category.trim().take(24).ifBlank { "ट्रेंडिंग" },
      "symbol" to cleanSymbol,
      "imageUrl" to cleanImage,
      "isActive" to true,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      emojiId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateEmojiInCatalog(
    emojiId: String,
    label: String,
    category: String,
    symbol: String,
    imageUrl: String,
  ): Result<Unit> = runCatching {
    requireUserId()
    val cleanLabel = label.trim().take(30).ifBlank { "कस्टम इमोजी" }
    val cleanSymbol = symbol.trim().take(16)
    val cleanImage = imageUrl.trim().take(800)
    require(cleanSymbol.isNotEmpty() || cleanImage.isNotEmpty()) {
      "इमोजी चिह्न या इमेज में से कम से कम एक आवश्यक है।"
    }
    val docRef = db.collection("emoji_catalog").document(emojiId)
    val updates = mapOf(
      "label" to cleanLabel,
      "category" to category.trim().take(24).ifBlank { "ट्रेंडिंग" },
      "symbol" to cleanSymbol,
      "imageUrl" to cleanImage,
      "isActive" to true,
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteEmojiFromCatalog(emojiId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("emoji_catalog").document(emojiId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  fun observeGiftCatalog(): Flow<List<GiftCatalogItem>> = flow {
    val uid = requireUserId()
    val query = db.collection("gift_catalog").whereEqualTo("isActive", true)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(GiftCatalogItem::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "gift_catalog", uid); throw error }
    )
  }

  suspend fun addGiftToCatalog(
    name: String,
    category: String,
    emoji: String,
    thumbnailUrl: String,
    mediaUrl: String,
    mediaType: String,
    coinCost: Long,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val giftId = "gift_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("gift_catalog").document(giftId)
    val safeMediaType = if (mediaType in listOf("emoji", "image", "video")) mediaType else "emoji"
    val safeCost = coinCost.coerceIn(1L, 999999L)
    val payload = mapOf(
      "createdByUid" to uid,
      "name" to name.trim().take(40).ifBlank { "स्पेशल गिफ्ट" },
      "category" to category.trim().take(24).ifBlank { "हॉट" },
      "emoji" to emoji.trim().take(16).ifBlank { "🎁" },
      "thumbnailUrl" to thumbnailUrl.trim().take(800),
      "mediaUrl" to mediaUrl.trim().take(800),
      "mediaType" to safeMediaType,
      "coinCost" to safeCost,
      "isActive" to true,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      giftId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateGiftInCatalog(
    giftId: String,
    name: String,
    category: String,
    emoji: String,
    thumbnailUrl: String,
    mediaUrl: String,
    mediaType: String,
    coinCost: Long,
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("gift_catalog").document(giftId)
    val safeMediaType = if (mediaType in listOf("emoji", "image", "video")) mediaType else "emoji"
    val safeCost = coinCost.coerceIn(1L, 999999L)
    val updates = mapOf(
      "name" to name.trim().take(40).ifBlank { "स्पेशल गिफ्ट" },
      "category" to category.trim().take(24).ifBlank { "हॉट" },
      "emoji" to emoji.trim().take(16).ifBlank { "🎁" },
      "thumbnailUrl" to thumbnailUrl.trim().take(800),
      "mediaUrl" to mediaUrl.trim().take(800),
      "mediaType" to safeMediaType,
      "coinCost" to safeCost,
      "isActive" to true,
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteGiftFromCatalog(
    giftId: String,
    mediaUrls: List<String> = emptyList(),
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("gift_catalog").document(giftId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }

    // Real-time delete all public gift_events referencing this deleted giftId or mediaUrl
    val urlSet = mediaUrls.map { it.trim() }.filter { it.isNotBlank() }.toSet()
    runCatching {
      val snapshot = db.collection("gift_events")
        .whereEqualTo("isPublic", true)
        .get()
        .await()
      val matchingRefs = snapshot.documents.filter { doc ->
        val evtGiftId = doc.getString("giftId").orEmpty()
        val evtMediaUrl = doc.getString("giftMediaUrl").orEmpty().trim()
        evtGiftId == giftId || (evtMediaUrl.isNotBlank() && evtMediaUrl in urlSet)
      }.map { it.reference }
      matchingRefs.chunked(400).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { batch.delete(it) }
        batch.commit().await()
      }
    }
  }

  fun observeGlobalGiftEvents(): Flow<List<GiftEvent>> = flow {
    val uid = requireUserId()
    val query = db.collection("gift_events").whereEqualTo("isPublic", true)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          val fallbackNow = Timestamp.now()
          snapshot.toObjects(GiftEvent::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: fallbackNow }
            .take(80)
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "gift_events", uid); throw error }
    )
  }

  fun observeRoomGiftEvents(roomId: String): Flow<RoomGiftEventsSnapshot> = flow {
    val uid = requireUserId()
    val query = db.collection("gift_events")
      .whereEqualTo("roomId", roomId)
      .whereEqualTo("isPublic", true)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          val fallbackNow = Timestamp.now()
          val items = snapshot.toObjects(GiftEvent::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: fallbackNow }
            .take(100)
          RoomGiftEventsSnapshot(
            events = items,
            isFromCache = snapshot.metadata.isFromCache,
          )
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "gift_events", uid); throw error }
    )
  }

  suspend fun sendGiftTransaction(
    room: VoiceRoom,
    sender: UserProfile,
    recipientUid: String,
    recipientName: String,
    gift: GiftCatalogItem,
    quantity: Int,
  ): Result<GiftEvent> = runCatching {
    val uid = requireUserId()
    val safeQty = quantity.coerceIn(1, 99)
    val totalCost = (gift.coinCost * safeQty).coerceIn(0L, 999999L)
    val userRef = db.collection("users").document(uid)
    val eventId = "evt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val eventRef = db.collection("gift_events").document(eventId)
    val msgId = "gmsg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val msgRef = db.collection("rooms").document(room.id).collection("messages").document(msgId)

    val safeGiftName = gift.name.take(40).ifBlank { "गिफ्ट" }
    val safeGiftEmoji = gift.emoji.take(16).ifBlank { "🎁" }
    val safeRecipientName = recipientName.trim().take(40).ifBlank { "सभी सदस्य" }
    val safeSenderName = sender.displayName.trim().take(40).ifBlank { "User" }
    val safeMediaUrl = gift.mediaUrl.ifBlank { gift.thumbnailUrl }.take(800)
    val safeMediaType = when {
      gift.mediaType == "video" && safeMediaUrl.isNotBlank() -> "video"
      gift.mediaType == "image" && safeMediaUrl.isNotBlank() -> "image"
      else -> "emoji"
    }

    val eventPayload = mapOf(
      "roomId" to room.id,
      "roomTitle" to room.title.take(60).ifBlank { "लाइव रूम" },
      "senderUid" to uid,
      "senderName" to safeSenderName,
      "senderAvatarUrl" to sender.avatarUrl.take(600),
      "recipientUid" to recipientUid.take(128),
      "recipientName" to safeRecipientName,
      "giftId" to gift.id.take(128).ifBlank { "builtin_gift" },
      "giftName" to safeGiftName,
      "giftEmoji" to safeGiftEmoji,
      "giftMediaUrl" to safeMediaUrl,
      "giftMediaType" to safeMediaType,
      "coinCost" to totalCost,
      "quantity" to safeQty.toLong(),
      "isPublic" to true,
      "createdAt" to FieldValue.serverTimestamp(),
    )

    val qtySuffix = if (safeQty > 1) " x$safeQty" else ""
    val msgPayload = mapOf(
      "roomId" to room.id,
      "roomOwnerId" to room.ownerId,
      "isRoomPublic" to room.isPublic,
      "senderUid" to uid,
      "senderName" to safeSenderName,
      "senderAvatarUrl" to sender.avatarUrl.take(600),
      "text" to "$safeRecipientName को $safeGiftEmoji $safeGiftName$qtySuffix भेजा!",
      "messageType" to "gift",
      "badgeTag" to "🎁 GIFT",
      "createdAt" to FieldValue.serverTimestamp(),
    )

    val cpPairRef = if (recipientUid.isNotBlank() && recipientUid != uid) {
      db.collection("cp_links").document(SwarUserIdentity.cpPairId(uid, recipientUid))
    } else {
      null
    }
    val isLuckyCpGift = SwarCatalogDefaults.isCpLuckyGiftCategory(gift.category)
    val xpGain = totalCost.coerceAtLeast(10L)

    try {
      db.runTransaction { tx ->
        val userSnap = tx.get(userRef)
        val cpSnap = cpPairRef?.let { tx.get(it) }
        val hasAcceptedCpPair = cpPairRef != null &&
          cpSnap != null &&
          cpSnap.exists() &&
          cpSnap.getString("status") == "accepted"

        if (isLuckyCpGift && !hasAcceptedCpPair) {
          throw IllegalStateException(
            "यह लकी गिफ्ट केवल जुड़ी हुई CP जोड़ी एक-दूसरे को भेज सकती है! पहले प्रोफ़ाइल में CP जोड़ें और अपने CP पार्टनर को चुनें।"
          )
        }

        val currentCoins = userSnap.getLong("coins") ?: 0L
        if (currentCoins < totalCost) {
          throw IllegalStateException("कॉइंस कम हैं! इस गिफ्ट के लिए $totalCost कॉइंस चाहिए।")
        }
        tx.update(
          userRef,
          mapOf(
            "coins" to (currentCoins - totalCost),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(eventRef, eventPayload)
        tx.set(msgRef, msgPayload)
        if (isLuckyCpGift && hasAcceptedCpPair && cpPairRef != null && cpSnap != null) {
          val prevXp = cpSnap.getLong("cpXp") ?: 0L
          val prevGifts = cpSnap.getLong("totalGiftsCount") ?: 0L
          tx.update(
            cpPairRef,
            mapOf(
              "cpXp" to (prevXp + xpGain).coerceAtMost(999_999_999L),
              "totalGiftsCount" to (prevGifts + safeQty.toLong()).coerceAtMost(99_999_999L),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }.await()
      GiftEvent(
        id = eventId,
        roomId = room.id,
        roomTitle = room.title.take(60),
        senderUid = uid,
        senderName = safeSenderName,
        senderAvatarUrl = sender.avatarUrl,
        recipientUid = recipientUid,
        recipientName = safeRecipientName,
        giftId = gift.id.ifBlank { "builtin_gift" },
        giftName = safeGiftName,
        giftEmoji = safeGiftEmoji,
        giftMediaUrl = safeMediaUrl,
        giftMediaType = safeMediaType,
        coinCost = totalCost,
        quantity = safeQty.toLong(),
        isPublic = true,
        createdAt = Timestamp.now(),
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: IllegalStateException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, eventRef.path)
      throw e
    }
  }

  suspend fun claimBonusCoins(amount: Long = 500L): Result<Long> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val safeBonus = amount.coerceIn(1L, 50000L)
    try {
      db.runTransaction { tx ->
        val snap = tx.get(docRef)
        val current = snap.getLong("coins") ?: 0L
        val next = (current + safeBonus).coerceAtMost(999999999L)
        tx.update(
          docRef,
          mapOf(
            "coins" to next,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        next
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- Admin-Managed Per-Level Configurations, Coin Rewards & Room Entry Video ---

  fun observeLevelConfigs(): Flow<Map<Int, LevelTierConfig>> = flow {
    val uid = requireUserId()
    val colRef = db.collection("level_configs")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(LevelTierConfig::class.java, ServerTimestampBehavior.ESTIMATE)
            .associateBy { it.badgeLevel.toInt() }
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "level_configs", uid); throw error }
    )
  }

  suspend fun upsertLevelConfig(
    badgeLevel: Int,
    entryVideoUrl: String,
    avatarFrameUrl: String,
    rewardCoins: Long,
    profileBadgeUrl: String,
    typingBannerUrl: String = "",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(badgeLevel in VALID_TIER_LEVELS) { "अमान्य स्तर चयनित है।" }
    val docRef = db.collection("level_configs").document(badgeLevel.toString())
    val payload = mapOf(
      "badgeLevel" to badgeLevel.toLong(),
      "entryVideoUrl" to entryVideoUrl.trim().take(800),
      "avatarFrameUrl" to avatarFrameUrl.trim().take(800),
      "rewardCoins" to rewardCoins.coerceIn(0L, 999999L),
      "profileBadgeUrl" to profileBadgeUrl.trim().take(800),
      "typingBannerUrl" to typingBannerUrl.trim().take(800),
      "updatedByUid" to uid,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  fun observeClaimedLevelTiers(): Flow<Set<Int>> = flow {
    val uid = requireUserId()
    val query = db.collection("level_claims").whereEqualTo("userId", uid)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(LevelRewardClaim::class.java, ServerTimestampBehavior.ESTIMATE)
            .map { it.badgeLevel.toInt() }
            .toSet()
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "level_claims", uid); throw error }
    )
  }

  suspend fun claimLevelRewardCoins(
    badgeLevel: Int,
    rewardCoins: Long,
  ): Result<Long> = runCatching {
    val uid = requireUserId()
    require(badgeLevel in VALID_TIER_LEVELS) { "अमान्य स्तर चयनित है।" }
    val safeReward = rewardCoins.coerceIn(0L, 999999L)
    if (safeReward <= 0L) return@runCatching 0L
    val claimId = "${uid}_$badgeLevel"
    val claimRef = db.collection("level_claims").document(claimId)
    val userRef = db.collection("users").document(uid)
    try {
      db.runTransaction { tx ->
        val claimSnap = tx.get(claimRef)
        if (claimSnap.exists()) {
          return@runTransaction 0L
        }
        val userSnap = tx.get(userRef)
        val currentCoins = userSnap.getLong("coins") ?: 0L
        val nextCoins = (currentCoins + safeReward).coerceAtMost(999999999L)
        tx.update(
          userRef,
          mapOf(
            "coins" to nextCoins,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          claimRef,
          mapOf(
            "userId" to uid,
            "badgeLevel" to badgeLevel.toLong(),
            "coinsGranted" to safeReward,
            "claimedAt" to FieldValue.serverTimestamp(),
          ),
        )
        safeReward
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, claimRef.path)
      throw e
    }
  }

  suspend fun broadcastRoomEntryVideo(
    room: VoiceRoom,
    sender: UserProfile,
    badgeLevel: Int,
    entryVideoUrl: String,
  ): Result<GiftEvent> = runCatching {
    val uid = requireUserId()
    val cleanUrl = entryVideoUrl.trim().take(800)
    require(cleanUrl.isNotBlank()) { "एंट्री वीडियो लिंक खाली है।" }
    val eventId = "ent_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val eventRef = db.collection("gift_events").document(eventId)
    val safeName = sender.displayName.trim().take(40).ifBlank { "Member" }
    val giftId = "entry_lv_$badgeLevel"
    val giftName = "LV.$badgeLevel रॉयल एंट्री"
    val payload = mapOf(
      "roomId" to room.id,
      "roomTitle" to room.title.take(60).ifBlank { "लाइव रूम" },
      "senderUid" to uid,
      "senderName" to safeName,
      "senderAvatarUrl" to sender.avatarUrl.take(600),
      "recipientUid" to uid.take(128),
      "recipientName" to safeName,
      "giftId" to giftId,
      "giftName" to giftName,
      "giftEmoji" to "👑",
      "giftMediaUrl" to cleanUrl,
      "giftMediaType" to "video",
      "coinCost" to 0L,
      "quantity" to 1L,
      "isPublic" to true,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    try {
      eventRef.set(payload).await()
      GiftEvent(
        id = eventId,
        roomId = room.id,
        roomTitle = room.title.take(60),
        senderUid = uid,
        senderName = safeName,
        senderAvatarUrl = sender.avatarUrl,
        recipientUid = uid,
        recipientName = safeName,
        giftId = giftId,
        giftName = giftName,
        giftEmoji = "👑",
        giftMediaUrl = cleanUrl,
        giftMediaType = "video",
        coinCost = 0L,
        quantity = 1L,
        isPublic = true,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, eventRef.path)
      throw e
    }
  }

  // --- Global GitHub Release App Update Configuration ---

  fun observeAppUpdateConfig(): Flow<com.example.data.model.AppUpdateConfig?> = flow {
    val uid = requireUserId()
    val docRef = db.collection("app_config").document("update")
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(com.example.data.model.AppUpdateConfig::class.java, ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error -> handleFlowError(error, OperationType.GET, docRef.path, uid); throw error }
    )
  }

  suspend fun saveAppUpdateConfig(githubRepoUrl: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("app_config").document("update")
    val payload = mapOf(
      "githubRepoUrl" to githubRepoUrl.trim().take(400),
      "updatedByUid" to uid,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  // --- First-Time Onboarding, Audit Profiles & 7-Month Activity Logs ---

  fun observeOwnAuditProfile(uid: String): Flow<UserAuditProfile?> = flow {
    val docRef = db.collection("user_audit_profiles").document(uid)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(UserAuditProfile::class.java, ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error -> handleFlowError(error, OperationType.GET, docRef.path, uid); throw error }
    )
  }

  suspend fun saveOnboardingAuditProfile(
    userEmail: String,
    displayName: String,
    avatarUrl: String,
    dobIso: String,
    locationLabel: String,
    locationLat: Double,
    locationLng: Double,
    locationStatus: String,
    termsAccepted: Boolean,
    onboardingCompleted: Boolean,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("user_audit_profiles").document(uid)
    val safeName = displayName.trim().take(40).ifBlank { "Swar User" }
    val numericId = SwarUserIdentity.numericIdForUid(uid).take(20)
    val safeEmail = userEmail.trim().take(120)
    val safeAvatar = avatarUrl.trim().take(600)
    val cleanDob = dobIso.trim().take(20)
    val computedAge = AuditDateLocationHelper.calculateAgeYears(cleanDob).toLong().coerceIn(0L, 130L)
    val safeLocationStatus = if (locationStatus in listOf("pending", "captured", "denied", "unavailable")) {
      locationStatus
    } else {
      "unavailable"
    }
    val safeLat = locationLat.coerceIn(-90.0, 90.0)
    val safeLng = locationLng.coerceIn(-180.0, 180.0)
    val safeLabel = locationLabel.trim().take(240)

    try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(
          mapOf(
            "numericId" to numericId,
            "userEmail" to safeEmail,
            "displayName" to safeName,
            "avatarUrl" to safeAvatar,
            "dobIso" to cleanDob,
            "ageYears" to computedAge,
            "locationLabel" to safeLabel,
            "locationLat" to safeLat,
            "locationLng" to safeLng,
            "locationStatus" to safeLocationStatus,
            "termsAccepted" to termsAccepted,
            "onboardingCompleted" to onboardingCompleted,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      } else {
        docRef.set(
          mapOf(
            "userId" to uid,
            "numericId" to numericId,
            "userEmail" to safeEmail,
            "displayName" to safeName,
            "avatarUrl" to safeAvatar,
            "dobIso" to cleanDob,
            "ageYears" to computedAge,
            "locationLabel" to safeLabel,
            "locationLat" to safeLat,
            "locationLng" to safeLng,
            "locationStatus" to safeLocationStatus,
            "termsAccepted" to termsAccepted,
            "onboardingCompleted" to onboardingCompleted,
            "isBlockedByAdmin" to false,
            "firstLoginAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun syncAuditProfileIdentity(
    userEmail: String,
    displayName: String,
    avatarUrl: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("user_audit_profiles").document(uid)
    val snap = docRef.get().await()
    if (!snap.exists()) return@runCatching
    docRef.update(
      mapOf(
        "numericId" to SwarUserIdentity.numericIdForUid(uid).take(20),
        "userEmail" to userEmail.trim().take(120),
        "displayName" to displayName.trim().take(40).ifBlank { "Swar User" },
        "avatarUrl" to avatarUrl.trim().take(600),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    ).await()
  }

  suspend fun setUserBlockedByAdmin(
    targetUid: String,
    blocked: Boolean,
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("user_audit_profiles").document(targetUid)
    try {
      docRef.update(
        mapOf(
          "isBlockedByAdmin" to blocked,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun createActivityLog(
    userEmail: String,
    userName: String,
    userAvatarUrl: String,
    activityType: String,
    roomId: String = "",
    roomTitle: String = "",
    roomCode: String = "",
    conversationId: String = "",
    targetUid: String = "",
    targetNumericId: String = "",
    targetName: String = "",
    textContent: String = "",
    mediaUrl: String = "",
    enteredAtMs: Long = System.currentTimeMillis(),
    exitedAtMs: Long = 0L,
    durationSeconds: Long = 0L,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val safeType = if (activityType in listOf("room_visit", "room_chat", "inbox_chat", "inbox_media")) {
      activityType
    } else {
      "room_visit"
    }
    val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val docRef = db.collection("user_activity_logs").document(logId)
    val nowMs = System.currentTimeMillis()
    val payload = mapOf(
      "userId" to uid,
      "numericId" to SwarUserIdentity.numericIdForUid(uid).take(20),
      "userEmail" to userEmail.trim().take(120),
      "userName" to userName.trim().take(40).ifBlank { "Swar User" },
      "userAvatarUrl" to userAvatarUrl.trim().take(600),
      "activityType" to safeType,
      "roomId" to roomId.take(128),
      "roomTitle" to roomTitle.trim().take(60),
      "roomCode" to roomCode.trim().take(16),
      "conversationId" to conversationId.take(260),
      "targetUid" to targetUid.take(128),
      "targetNumericId" to targetNumericId.take(20),
      "targetName" to targetName.trim().take(40),
      "textContent" to textContent.trim().take(1000),
      "mediaUrl" to mediaUrl.trim().take(800),
      "dateKey" to AuditDateLocationHelper.dateKeyFromMillis(enteredAtMs.takeIf { it > 0L } ?: nowMs),
      "enteredAtMs" to enteredAtMs.coerceAtLeast(0L),
      "exitedAtMs" to exitedAtMs.coerceAtLeast(0L),
      "durationSeconds" to durationSeconds.coerceIn(0L, 8640000L),
      "createdAt" to FieldValue.serverTimestamp(),
      "expiresAt" to AuditDateLocationHelper.sevenMonthsExpiryTimestamp(nowMs),
    )
    try {
      docRef.set(payload).await()
      logId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateRoomVisitDuration(
    logId: String,
    exitedAtMs: Long,
    durationSeconds: Long,
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("user_activity_logs").document(logId)
    try {
      docRef.update(
        mapOf(
          "exitedAtMs" to exitedAtMs.coerceAtLeast(0L),
          "durationSeconds" to durationSeconds.coerceIn(0L, 8640000L),
        ),
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  fun observeAllAuditProfilesAsAdmin(): Flow<List<UserAuditProfile>> = flow {
    val uid = requireUserId()
    val colRef = db.collection("user_audit_profiles")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(UserAuditProfile::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt ?: it.firstLoginAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "user_audit_profiles", uid); throw error }
    )
  }

  fun observeAllActivityLogsAsAdmin(): Flow<List<UserActivityLog>> = flow {
    val uid = requireUserId()
    val colRef = db.collection("user_activity_logs")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          val nowMs = System.currentTimeMillis()
          snapshot.toObjects(UserActivityLog::class.java, ServerTimestampBehavior.ESTIMATE)
            .filter { AuditDateLocationHelper.isWithinSevenMonths(it.expiresAt, it.createdAt, nowMs) }
            .sortedByDescending {
              maxOf(it.enteredAtMs, it.createdAt?.toDate()?.time ?: 0L)
            }
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "user_activity_logs", uid); throw error }
    )
  }

  suspend fun purgeExpiredActivityLogs(): Result<Int> = runCatching {
    requireUserId()
    val now = Timestamp.now()
    val expiredSnapshot = db.collection("user_activity_logs")
      .whereLessThanOrEqualTo("expiresAt", now)
      .get()
      .await()
    if (expiredSnapshot.isEmpty) return@runCatching 0
    val refs = expiredSnapshot.documents.map { it.reference }
    refs.chunked(400).forEach { chunk ->
      val batch = db.batch()
      chunk.forEach { batch.delete(it) }
      batch.commit().await()
    }
    refs.size
  }

  // --- CP / Best-Friend Partner Linking & Progression ---

  fun observeMyCpLinks(): Flow<List<CpRelationship>> = flow {
    val uid = requireUserId()
    val query = db.collection("cp_links").whereArrayContains("memberUids", uid)
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(CpRelationship::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "cp_links", uid); throw error }
    )
  }

  fun observeAcceptedCpLinks(): Flow<List<CpRelationship>> = flow {
    val uid = requireUserId()
    val query = db.collection("cp_links").whereEqualTo("status", "accepted")
    emitAll(
      query.snapshots()
        .map { snapshot ->
          snapshot.toObjects(CpRelationship::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.cpXp }
        }
        .catch { error -> handleFlowError(error, OperationType.LIST, "cp_links", uid); throw error }
    )
  }

  suspend fun sendCpRequest(
    senderProfile: UserProfile,
    recipientNumericId: String,
    bondTitle: String = "बेस्ट फ्रेंड",
  ): Result<CpRelationship> = runCatching {
    val uid = requireUserId()
    val cleanId = recipientNumericId.filter { it.isDigit() }
    require(cleanId.length == 10) { "कृपया सही 10-अंकों का ID नंबर दर्ज करें।" }
    val ownNumericId = SwarUserIdentity.numericIdForUid(uid)
    require(cleanId != ownNumericId) { "आप अपनी खुद की ID पर CP अनुरोध नहीं भेज सकते।" }

    val target = findUserByNumericId(cleanId)
      ?: throw IllegalStateException("यह ID नंबर ($cleanId) अभी रजिस्टर नहीं मिला। सामने वाले से ऐप खोलकर अपनी ID सिंक करने को कहें।")
    val targetUid = target.userId.trim()
    require(targetUid.isNotBlank() && targetUid != uid) { "अमान्य यूज़र ID मिली।" }

    val acceptedSnap = db.collection("cp_links")
      .whereEqualTo("status", "accepted")
      .get()
      .await()
    val alreadyPaired = acceptedSnap.documents.any { doc ->
      val members = (doc.get("memberUids") as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
      uid in members || targetUid in members
    }
    require(!alreadyPaired) { "आप या सामने वाला यूज़र पहले से किसी CP पार्टनर से जुड़े हुए हैं।" }

    val pairId = SwarUserIdentity.cpPairId(uid, targetUid)
    val docRef = db.collection("cp_links").document(pairId)
    val existing = docRef.get().await()
    if (existing.exists()) {
      val status = existing.getString("status").orEmpty()
      if (status == "accepted") {
        throw IllegalStateException("आप दोनों पहले से CP पार्टनर के रूप में जुड़े हैं!")
      }
      if (status == "pending") {
        val reqUid = existing.getString("requesterUid").orEmpty()
        if (reqUid == targetUid) {
          throw IllegalStateException("${target.displayName.ifBlank { "इस यूज़र" }} ने आपको पहले ही CP अनुरोध भेजा है — कृपया स्वीकार करें!")
        } else {
          throw IllegalStateException("CP अनुरोध पहले से भेजा जा चुका है। सामने वाले की स्वीकृति की प्रतीक्षा करें।")
        }
      }
    }

    val sortedUids = listOf(uid, targetUid).sorted()
    val senderName = senderProfile.displayName.trim().take(40).ifBlank { "Swar User" }
    val targetName = target.displayName.trim().take(40).ifBlank { "Swar Partner" }
    val cleanTitle = bondTitle.trim().take(40).ifBlank { "बेस्ट फ्रेंड" }

    val memberNumericIds = mapOf(
      sortedUids[0] to if (sortedUids[0] == uid) ownNumericId else cleanId,
      sortedUids[1] to if (sortedUids[1] == uid) ownNumericId else cleanId,
    )
    val memberNames = mapOf(
      sortedUids[0] to if (sortedUids[0] == uid) senderName else targetName,
      sortedUids[1] to if (sortedUids[1] == uid) senderName else targetName,
    )
    val memberAvatars = mapOf(
      sortedUids[0] to if (sortedUids[0] == uid) senderProfile.avatarUrl.take(600) else target.avatarUrl.take(600),
      sortedUids[1] to if (sortedUids[1] == uid) senderProfile.avatarUrl.take(600) else target.avatarUrl.take(600),
    )

    val payload = mapOf(
      "memberUids" to sortedUids,
      "memberNumericIds" to memberNumericIds,
      "memberNames" to memberNames,
      "memberAvatars" to memberAvatars,
      "requesterUid" to uid,
      "recipientUid" to targetUid,
      "status" to "pending",
      "cpXp" to 0L,
      "totalGiftsCount" to 0L,
      "bondTitle" to cleanTitle,
      "acceptedAtMs" to 0L,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    try {
      docRef.set(payload).await()
      CpRelationship(
        id = pairId,
        memberUids = sortedUids,
        memberNumericIds = memberNumericIds,
        memberNames = memberNames,
        memberAvatars = memberAvatars,
        requesterUid = uid,
        recipientUid = targetUid,
        status = "pending",
        cpXp = 0L,
        totalGiftsCount = 0L,
        bondTitle = cleanTitle,
        acceptedAtMs = 0L,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun acceptCpRequest(
    pairId: String,
    accepterProfile: UserProfile,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("cp_links").document(pairId)
    val snap = docRef.get().await()
    require(snap.exists()) { "यह CP अनुरोध अब उपलब्ध नहीं है।" }
    val rel = snap.toObject(CpRelationship::class.java, ServerTimestampBehavior.ESTIMATE)
      ?: throw IllegalStateException("अनुरोध डेटा पढ़ा नहीं जा सका।")
    require(rel.status == "pending") { "यह अनुरोध पहले ही प्रोसेस हो चुका है।" }
    require(rel.recipientUid == uid) { "केवल प्राप्तकर्ता ही इस अनुरोध को स्वीकार कर सकता है।" }

    val otherUid = rel.partnerUidFor(uid)
    val acceptedSnap = db.collection("cp_links")
      .whereEqualTo("status", "accepted")
      .get()
      .await()
    val conflict = acceptedSnap.documents.any { doc ->
      doc.id != pairId && (doc.get("memberUids") as? List<*>)?.mapNotNull { it?.toString() }.orEmpty().let { members ->
        uid in members || otherUid in members
      }
    }
    require(!conflict) { "आप या पार्टनर पहले से किसी अन्य CP जोड़ी में जुड़े हुए हैं।" }

    val updatedNames = rel.memberNames.toMutableMap().apply {
      put(uid, accepterProfile.displayName.trim().take(40).ifBlank { this[uid].orEmpty().ifBlank { "Partner" } })
    }
    val updatedAvatars = rel.memberAvatars.toMutableMap().apply {
      put(uid, accepterProfile.avatarUrl.take(600))
    }

    try {
      docRef.update(
        mapOf(
          "status" to "accepted",
          "acceptedAtMs" to System.currentTimeMillis().coerceAtLeast(1L),
          "memberNames" to updatedNames,
          "memberAvatars" to updatedAvatars,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun declineOrBreakCp(pairId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("cp_links").document(pairId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  private companion object {
    val VALID_TIER_LEVELS = setOf(1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120)
  }

  private fun handleFlowError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    expectedUid: String,
  ) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
  }
}
