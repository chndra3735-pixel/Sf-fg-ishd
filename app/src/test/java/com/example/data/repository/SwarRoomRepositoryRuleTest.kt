package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GiftCatalogItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class SwarRoomRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createRoomAndSendMessages_authenticatedUsers_succeedsAndSyncs() = runBlocking {
    val aliceEmail = "alice_${UUID.randomUUID().toString().take(8)}@test.com"
    val bobEmail = "bob_${UUID.randomUUID().toString().take(8)}@test.com"

    val aliceUid = signInTestUser(aliceEmail)
    val repoAlice = SwarRoomRepository(firestore, auth)
    val aliceProfile = withTimeout(TIMEOUT_MS) {
      repoAlice.ensureUserProfile("Alice Host", "").getOrThrow()
    }
    assertEquals(aliceUid, aliceProfile.userId)

    val roomId = withTimeout(TIMEOUT_MS) {
      repoAlice.createRoom(
        title = "क्रिकेट और मस्ती",
        category = "लोकप्रिय",
        backgroundTheme = "stadium",
        clubTag = "UP55",
        welcomeNotice = "SwarRoom में स्वागत है!",
        coverUrl = "",
        seatCount = 8,
        hostProfile = aliceProfile,
      ).getOrThrow()
    }
    assertTrue(roomId.isNotEmpty())

    val rooms = withTimeout(TIMEOUT_MS) {
      repoAlice.observePublicRooms().first { list -> list.any { it.id == roomId } }
    }
    val createdRoom = rooms.first { it.id == roomId }
    assertEquals("क्रिकेट और मस्ती", createdRoom.title)

    val seats = withTimeout(TIMEOUT_MS) {
      repoAlice.observeRoomSeats(roomId).first { it.size == 8 }
    }
    assertEquals(aliceUid, seats.first { it.seatIndex == 0L }.occupantUid)

    // Bob signs in, claims seat 1, and sends a real-time room text message
    val bobUid = signInTestUser(bobEmail)
    val repoBob = SwarRoomRepository(firestore, auth)
    val bobProfile = withTimeout(TIMEOUT_MS) {
      repoBob.ensureUserProfile("Bob Guest", "").getOrThrow()
    }

    withTimeout(TIMEOUT_MS) {
      repoBob.claimSeat(roomId, 1, bobProfile).getOrThrow()
    }
    val updatedSeats = withTimeout(TIMEOUT_MS) {
      repoBob.observeRoomSeats(roomId).first { list -> list.any { it.seatIndex == 1L && it.occupantUid == bobUid } }
    }
    assertEquals("Bob Guest", updatedSeats.first { it.seatIndex == 1L }.occupantName)

    withTimeout(TIMEOUT_MS) {
      repoBob.sendRoomMessage(createdRoom, bobProfile, "नमस्ते दोस्तों!", "text").getOrThrow()
    }
    val roomMessages = withTimeout(TIMEOUT_MS) {
      repoBob.observeRoomMessages(roomId).first { list -> list.any { it.text == "नमस्ते दोस्तों!" } }
    }
    assertTrue(roomMessages.isNotEmpty())

    // Bob cannot lock seat (owner only)
    val lockResult = withTimeout(TIMEOUT_MS) { repoBob.toggleSeatLock(roomId, 2, true) }
    assertTrue(lockResult.isFailure)
    val lockError = lockResult.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(lockError)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, lockError?.code)
  }

  @Test
  fun directMessaging_andMoments_enforcesParticipantAndOwnerSecurity() = runBlocking {
    val aliceEmail = "dm_alice_${UUID.randomUUID().toString().take(8)}@test.com"
    val bobEmail = "dm_bob_${UUID.randomUUID().toString().take(8)}@test.com"
    val charlieEmail = "dm_charlie_${UUID.randomUUID().toString().take(8)}@test.com"

    val aliceUid = signInTestUser(aliceEmail)
    val repo = SwarRoomRepository(firestore, auth)
    val aliceProfile = withTimeout(TIMEOUT_MS) { repo.ensureUserProfile("Alice", "").getOrThrow() }

    val bobUid = signInTestUser(bobEmail)
    val bobProfile = withTimeout(TIMEOUT_MS) { repo.ensureUserProfile("Bob", "").getOrThrow() }

    val convoId = withTimeout(TIMEOUT_MS) {
      repo.startOrGetConversation(bobProfile, aliceUid, "Alice", "").getOrThrow()
    }
    withTimeout(TIMEOUT_MS) {
      repo.sendDirectMessage(convoId, listOf(aliceUid, bobUid).sorted(), "Bob", "कैसे हो?").getOrThrow()
    }
    val dms = withTimeout(TIMEOUT_MS) {
      repo.observeDirectMessages(convoId).first { it.any { msg -> msg.text == "कैसे हो?" } }
    }
    assertEquals(1, dms.size)

    // Moments stored with Supabase URL
    val momentId = withTimeout(TIMEOUT_MS) {
      repo.createMomentPost(
        author = bobProfile,
        caption = "My live singing clip",
        mediaUrl = "https://proj.supabase.co/storage/v1/object/public/swarroom-media/moments/clip.mp4",
        mediaType = "video",
      ).getOrThrow()
    }
    assertTrue(momentId.isNotEmpty())

    // Charlie cannot read Alice & Bob's conversation or delete Bob's moment
    signInTestUser(charlieEmail)
    try {
      withTimeout(TIMEOUT_MS) {
        firestore.collection("conversations").document(convoId).get().await()
      }
      fail("Expected PERMISSION_DENIED for third party reading conversation")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertNotNull(fsEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    val deleteResult = withTimeout(TIMEOUT_MS) { repo.deleteMoment(momentId) }
    assertTrue(deleteResult.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      deleteResult.exceptionOrNull()?.unwrapFirestoreException()?.code,
    )
  }

  @Test
  fun observePublicRooms_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = SwarRoomRepository(firestore, auth)

    try {
      withTimeout(TIMEOUT_MS) { repo.observePublicRooms().first() }
      fail("Expected unauthenticated observePublicRooms to fail")
    } catch (e: Exception) {
      // Unauthenticated call throws IllegalStateException from requireUserId() or PERMISSION_DENIED
      assertTrue(
        e is IllegalStateException ||
          e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED,
      )
    }
  }

  @Test
  fun giftAndEmojiRules_andGiftTransaction_workAsExpected() = runBlocking {
    val senderEmail = "sender_${UUID.randomUUID().toString().take(8)}@test.com"
    val senderUid = signInTestUser(senderEmail)
    val repo = SwarRoomRepository(firestore, auth)
    val profile = withTimeout(TIMEOUT_MS) { repo.ensureUserProfile("Riya", "").getOrThrow() }
    assertEquals(1500L, profile.coins)

    // Regular user cannot write directly to emoji_catalog or gift_catalog
    val emojiAddResult = withTimeout(TIMEOUT_MS) {
      repo.addEmojiToCatalog("नया", "ट्रेंडिंग", "🔥", "")
    }
    assertTrue(emojiAddResult.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      emojiAddResult.exceptionOrNull()?.unwrapFirestoreException()?.code,
    )

    val giftAddResult = withTimeout(TIMEOUT_MS) {
      repo.addGiftToCatalog("फेक गिफ्ट", "हॉट", "🎁", "", "", "emoji", 100L)
    }
    assertTrue(giftAddResult.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      giftAddResult.exceptionOrNull()?.unwrapFirestoreException()?.code,
    )

    // Regular user can observe active catalogs cleanly
    val emojis = withTimeout(TIMEOUT_MS) { repo.observeEmojiCatalog().first() }
    val gifts = withTimeout(TIMEOUT_MS) { repo.observeGiftCatalog().first() }
    assertNotNull(emojis)
    assertNotNull(gifts)

    // Create room and send a video gift transaction
    val roomId = withTimeout(TIMEOUT_MS) {
      repo.createRoom(
        title = "डांस फ्लोर",
        category = "लोकप्रिय",
        backgroundTheme = "stadium",
        clubTag = "VIP",
        welcomeNotice = "स्वागत है",
        coverUrl = "",
        seatCount = 8,
        hostProfile = profile,
      ).getOrThrow()
    }
    val room = withTimeout(TIMEOUT_MS) {
      repo.observePublicRooms().first { list -> list.any { it.id == roomId } }.first { it.id == roomId }
    }

    val videoGift = GiftCatalogItem(
      id = "vid_gift_1",
      name = "शाही ड्रैगन वीडियो",
      category = "वीडियो VIP",
      emoji = "🐉",
      mediaUrl = "https://proj.supabase.co/storage/v1/object/public/swarroom-media/gifts/dragon.mp4",
      mediaType = "video",
      coinCost = 300L,
    )

    val sentEvent = withTimeout(TIMEOUT_MS) {
      repo.sendGiftTransaction(
        room = room,
        sender = profile,
        recipientUid = senderUid,
        recipientName = "Riya Host",
        gift = videoGift,
        quantity = 2,
      ).getOrThrow()
    }
    assertEquals(600L, sentEvent.coinCost)
    assertEquals("video", sentEvent.giftMediaType)

    // Verify coins deducted (1500 - 600 = 900)
    val updatedProfile = repo.getUserProfile(senderUid)
    assertEquals(900L, updatedProfile?.coins)

    // Verify bonus coins claim (900 + 500 = 1400)
    val afterBonus = withTimeout(TIMEOUT_MS) { repo.claimBonusCoins(500L).getOrThrow() }
    assertEquals(1400L, afterBonus)

    // Verify global gift_events stream receives the event
    val globalEvents = withTimeout(TIMEOUT_MS) {
      repo.observeGlobalGiftEvents().first { list -> list.any { it.id == sentEvent.id } }
    }
    assertTrue(globalEvents.any { it.giftName == "शाही ड्रैगन वीडियो" })
  }

  private companion object {
    const val TIMEOUT_MS = 5000L
  }
}
