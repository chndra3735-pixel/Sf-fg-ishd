package com.example

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tracks the active voice-room session AND any uncleaned exit due to sudden
 * network cut or task swipe, so seats, live presence docs, and entry messages
 * are cleaned up immediately or as soon as internet reconnects.
 */
object SeatPresenceTracker {
  private const val TAG = "SeatPresenceTracker"
  private const val PREFS_NAME = "swarroom_active_presence"
  private const val KEY_USER_ID = "active_user_id"
  private const val KEY_ROOM_ID = "active_room_id"
  private const val KEY_SEAT_INDEX = "active_seat_index"
  private const val KEY_LISTENER_COUNTED = "active_listener_counted"

  private const val KEY_PENDING_USER_ID = "pending_user_id"
  private const val KEY_PENDING_ROOM_ID = "pending_room_id"
  private const val KEY_PENDING_SEAT_INDEX = "pending_seat_index"
  private const val KEY_PENDING_LISTENER_COUNTED = "pending_listener_counted"

  data class ActivePresence(
    val userId: String,
    val roomId: String,
    val seatIndex: Int?,
    val listenerCounted: Boolean,
  )

  private val lock = Any()

  @Volatile
  private var inMemoryPresence: ActivePresence? = null

  @Volatile
  private var isFlushingPending = false

  @Volatile
  private var audioDisconnectCallback: (() -> Unit)? = null

  fun registerAudioDisconnect(callback: (() -> Unit)?) {
    audioDisconnectCallback = callback
  }

  fun recordEnteredRoom(
    context: Context?,
    userId: String,
    roomId: String,
    listenerCounted: Boolean,
  ) {
    if (userId.isBlank() || roomId.isBlank()) return
    val next = ActivePresence(
      userId = userId,
      roomId = roomId,
      seatIndex = null,
      listenerCounted = listenerCounted,
    )
    synchronized(lock) {
      inMemoryPresence = next
      persistActive(context, next)
    }
  }

  fun recordSeatState(
    context: Context?,
    userId: String,
    roomId: String,
    seatIndex: Int?,
  ) {
    if (userId.isBlank() || roomId.isBlank()) return
    synchronized(lock) {
      val current = inMemoryPresence ?: readPersistedActive(context)
      val next = if (current != null && current.userId == userId && current.roomId == roomId) {
        current.copy(seatIndex = seatIndex)
      } else {
        ActivePresence(
          userId = userId,
          roomId = roomId,
          seatIndex = seatIndex,
          listenerCounted = false,
        )
      }
      inMemoryPresence = next
      persistActive(context, next)
    }
  }

  fun clearSession(context: Context?) {
    synchronized(lock) {
      inMemoryPresence = null
      persistActive(context, null)
    }
  }

  fun savePendingCleanup(
    context: Context?,
    presence: ActivePresence,
  ) {
    if (presence.userId.isBlank() || presence.roomId.isBlank()) return
    synchronized(lock) {
      inMemoryPresence = null
      persistActive(context, null)
      persistPending(context, presence)
    }
  }

  fun clearPendingCleanup(context: Context?) {
    synchronized(lock) {
      persistPending(context, null)
    }
  }

  fun peekPendingCleanup(context: Context?): ActivePresence? {
    synchronized(lock) {
      return readPersistedPending(context)
    }
  }

  fun consumeActivePresence(context: Context?): ActivePresence? {
    synchronized(lock) {
      val snapshot = inMemoryPresence ?: readPersistedActive(context) ?: readPersistedPending(context)
      inMemoryPresence = null
      persistActive(context, null)
      return snapshot
    }
  }

  /**
   * Moves active presence into pending cleanup, attempts synchronous cleanup, and
   * only deletes pending cleanup from disk when Firestore actually confirms cleanup.
   */
  fun cleanupSynchronously(context: Context, timeoutMs: Long = 1500L) {
    runCatching { audioDisconnectCallback?.invoke() }
    val target = synchronized(lock) {
      val snapshot = inMemoryPresence ?: readPersistedActive(context) ?: readPersistedPending(context)
      inMemoryPresence = null
      persistActive(context, null)
      if (snapshot != null) {
        persistPending(context, snapshot)
      }
      snapshot
    } ?: return

    val authUid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
    if (authUid.isNullOrBlank() || authUid != target.userId) return

    val db = resolveDb(context) ?: return
    val latch = CountDownLatch(1)

    Thread {
      try {
        if (performFullCleanupBlocking(db, target)) {
          clearPendingCleanup(context)
        }
      } catch (t: Throwable) {
        Log.w(TAG, "Presence cleanup deferred until reconnect for room=${target.roomId}", t)
      } finally {
        latch.countDown()
      }
    }.start()

    if (timeoutMs > 0L) {
      runCatching { latch.await(timeoutMs, TimeUnit.MILLISECONDS) }
    }
  }

  /**
   * Flushes any uncleaned active/pending room presence when network returns or app restarts.
   */
  fun flushPendingCleanup(
    context: Context?,
    expectedUid: String,
    activeRoomIdNow: String? = null,
  ): ActivePresence? {
    if (context == null || expectedUid.isBlank()) return null
    val candidate = synchronized(lock) {
      if (isFlushingPending) return@synchronized null
      val pending = readPersistedPending(context)
        ?: inMemoryPresence.takeIf { it?.roomId != activeRoomIdNow }
        ?: readPersistedActive(context).takeIf { it?.roomId != activeRoomIdNow }
      if (pending != null && pending.userId == expectedUid && pending.roomId != activeRoomIdNow) {
        isFlushingPending = true
        persistPending(context, pending)
        persistActive(context, null)
        if (inMemoryPresence?.roomId == pending.roomId) {
          inMemoryPresence = null
        }
        pending
      } else {
        null
      }
    } ?: return null

    val db = resolveDb(context)
    if (db == null) {
      isFlushingPending = false
      return null
    }
    var cleaned: ActivePresence? = null
    try {
      if (performFullCleanupBlocking(db, candidate)) {
        clearPendingCleanup(context)
        cleaned = candidate
      }
    } catch (t: Throwable) {
      Log.w(TAG, "Pending cleanup retry still waiting for network (${candidate.roomId})", t)
    } finally {
      isFlushingPending = false
    }
    return cleaned
  }

  private fun resolveDb(context: Context): FirebaseFirestore? {
    val appCtx = context.applicationContext
    val databaseId = runCatching { appCtx.getString(R.string.firestore_database_id) }.getOrNull().orEmpty()
    if (databaseId.isBlank()) return null
    return runCatching { FirebaseFirestore.getInstance(databaseId) }.getOrNull()
  }

  private fun performFullCleanupBlocking(
    db: FirebaseFirestore,
    target: ActivePresence,
  ): Boolean {
    var allSucceeded = true
    runCatching {
      vacateSeatsBlocking(db, target.roomId, target.userId, target.seatIndex)
    }.onFailure { allSucceeded = false }

    runCatching {
      deleteRoomPresenceBlocking(db, target.roomId, target.userId)
    }.onFailure { allSucceeded = false }

    runCatching {
      deleteUserJoinMessagesBlocking(db, target.roomId, target.userId)
    }

    if (target.listenerCounted) {
      runCatching {
        decrementListenerCountBlocking(db, target.roomId)
      }.onFailure { allSucceeded = false }
    }
    return allSucceeded
  }

  private fun vacateSeatsBlocking(
    db: FirebaseFirestore,
    roomId: String,
    uid: String,
    seatHint: Int?,
  ) {
    val seatsCollection = db.collection("rooms").document(roomId).collection("seats")
    val emptyUpdates = mapOf(
      "occupantUid" to "",
      "occupantName" to "",
      "occupantAvatarUrl" to "",
      "isMuted" to false,
      "isSpeaking" to false,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    val matching = Tasks.await(
      seatsCollection
        .whereEqualTo("isRoomPublic", true)
        .whereEqualTo("occupantUid", uid)
        .get(),
      1500,
      TimeUnit.MILLISECONDS,
    )
    if (!matching.isEmpty) {
      val batch = db.batch()
      matching.documents.forEach { doc ->
        batch.update(doc.reference, emptyUpdates)
      }
      Tasks.await(batch.commit(), 1500, TimeUnit.MILLISECONDS)
    } else if (seatHint != null) {
      val seatRef = seatsCollection.document("seat_$seatHint")
      val snap = Tasks.await(seatRef.get(), 1200, TimeUnit.MILLISECONDS)
      if (snap.exists() && snap.getString("occupantUid") == uid) {
        Tasks.await(seatRef.update(emptyUpdates), 1200, TimeUnit.MILLISECONDS)
      }
    }
  }

  private fun deleteRoomPresenceBlocking(
    db: FirebaseFirestore,
    roomId: String,
    uid: String,
  ) {
    val ref = db.collection("rooms").document(roomId).collection("presence").document(uid)
    Tasks.await(ref.delete(), 1200, TimeUnit.MILLISECONDS)
  }

  private fun deleteUserJoinMessagesBlocking(
    db: FirebaseFirestore,
    roomId: String,
    uid: String,
  ) {
    val msgColl = db.collection("rooms").document(roomId).collection("messages")
    val matching = Tasks.await(
      msgColl
        .whereEqualTo("isRoomPublic", true)
        .whereEqualTo("senderUid", uid)
        .get(),
      1500,
      TimeUnit.MILLISECONDS,
    )
    val joinDocs = matching.documents.filter { it.getString("messageType") == "join" }
    if (joinDocs.isNotEmpty()) {
      val batch = db.batch()
      joinDocs.forEach { doc -> batch.delete(doc.reference) }
      Tasks.await(batch.commit(), 1500, TimeUnit.MILLISECONDS)
    }
  }

  private fun decrementListenerCountBlocking(
    db: FirebaseFirestore,
    roomId: String,
  ) {
    val docRef = db.collection("rooms").document(roomId)
    val txTask = db.runTransaction { tx ->
      val snap = tx.get(docRef)
      if (!snap.exists()) return@runTransaction null
      val current = snap.getLong("listenerCount") ?: 1L
      val next = (current - 1L).coerceIn(0L, 9999L)
      tx.update(docRef, mapOf("listenerCount" to next, "updatedAt" to FieldValue.serverTimestamp()))
      null
    }
    Tasks.await(txTask, 1200, TimeUnit.MILLISECONDS)
  }

  private fun persistActive(context: Context?, presence: ActivePresence?) {
    val prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
    prefs.edit().apply {
      if (presence == null) {
        remove(KEY_USER_ID)
        remove(KEY_ROOM_ID)
        remove(KEY_SEAT_INDEX)
        remove(KEY_LISTENER_COUNTED)
      } else {
        putString(KEY_USER_ID, presence.userId)
        putString(KEY_ROOM_ID, presence.roomId)
        putInt(KEY_SEAT_INDEX, presence.seatIndex ?: -1)
        putBoolean(KEY_LISTENER_COUNTED, presence.listenerCounted)
      }
    }.apply()
  }

  private fun persistPending(context: Context?, presence: ActivePresence?) {
    val prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
    prefs.edit().apply {
      if (presence == null) {
        remove(KEY_PENDING_USER_ID)
        remove(KEY_PENDING_ROOM_ID)
        remove(KEY_PENDING_SEAT_INDEX)
        remove(KEY_PENDING_LISTENER_COUNTED)
      } else {
        putString(KEY_PENDING_USER_ID, presence.userId)
        putString(KEY_PENDING_ROOM_ID, presence.roomId)
        putInt(KEY_PENDING_SEAT_INDEX, presence.seatIndex ?: -1)
        putBoolean(KEY_PENDING_LISTENER_COUNTED, presence.listenerCounted)
      }
    }.apply()
  }

  private fun readPersistedActive(context: Context?): ActivePresence? {
    val prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return null
    val uid = prefs.getString(KEY_USER_ID, null).orEmpty()
    val roomId = prefs.getString(KEY_ROOM_ID, null).orEmpty()
    if (uid.isBlank() || roomId.isBlank()) return null
    val seatIdx = prefs.getInt(KEY_SEAT_INDEX, -1).takeIf { it >= 0 }
    val counted = prefs.getBoolean(KEY_LISTENER_COUNTED, false)
    return ActivePresence(uid, roomId, seatIdx, counted)
  }

  private fun readPersistedPending(context: Context?): ActivePresence? {
    val prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return null
    val uid = prefs.getString(KEY_PENDING_USER_ID, null).orEmpty()
    val roomId = prefs.getString(KEY_PENDING_ROOM_ID, null).orEmpty()
    if (uid.isBlank() || roomId.isBlank()) return null
    val seatIdx = prefs.getInt(KEY_PENDING_SEAT_INDEX, -1).takeIf { it >= 0 }
    val counted = prefs.getBoolean(KEY_PENDING_LISTENER_COUNTED, false)
    return ActivePresence(uid, roomId, seatIdx, counted)
  }
}
