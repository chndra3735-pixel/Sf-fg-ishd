const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_1";
const BOB_UID = "bob_uid_2";
const ADMIN_UID = "admin_uid_chandra";
const ADMIN_EMAIL = "chndra3735@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserPayload(uid) {
  return {
    userId: uid,
    displayName: "Aarav Sharma",
    handle: "aarav_up55",
    avatarUrl: "https://example.supabase.co/storage/v1/object/public/swarroom-media/dp/avatar.jpg",
    bio: "Live voice host",
    gender: "male",
    clubTag: "UP55",
    coins: 500,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validRoomPayload(ownerId, isPublic = true) {
  return {
    ownerId,
    ownerName: "Aarav",
    ownerAvatarUrl: "",
    title: "Cricket & Chill",
    roomCode: "884512",
    category: "लोकप्रिय",
    coverUrl: "",
    backgroundTheme: "stadium",
    clubTag: "UP55",
    welcomeNotice: "SwarRoom में स्वागत है!",
    seatCount: 20,
    listenerCount: 1,
    isLive: true,
    isPublic,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

// 1. Unauthenticated bounds
test("Unauthenticated user cannot read rooms or users", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("rooms").where("isPublic", "==", true).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
});

// 2. User profile CRUD & cross-user isolation
test("User can create and update own profile, cannot modify another user profile or inject ghost field", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(alice.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  await assertFails(
    alice.collection("users").doc(ALICE_UID).set({
      ...validUserPayload(ALICE_UID),
      isAdmin: true,
    })
  );

  await assertFails(
    bob.collection("users").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: serverTimestamp() })
  );

  await assertSucceeds(alice.collection("users").where("userId", "==", ALICE_UID).get());
  await assertFails(alice.collection("users").get());
});

// 3. Rooms & Seats
test("Room owner creates room + seat in batch; Bob can query public rooms and claim unlocked seat", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  const roomRef = alice.collection("rooms").doc("room_101");
  const seatRef = roomRef.collection("seats").doc("seat_1");
  const batch = alice.batch();
  batch.set(roomRef, validRoomPayload(ALICE_UID, true));
  batch.set(seatRef, {
    roomId: "room_101",
    roomOwnerId: ALICE_UID,
    isRoomPublic: true,
    seatIndex: 1,
    occupantUid: "",
    occupantName: "",
    occupantAvatarUrl: "",
    isLocked: false,
    isMuted: false,
    isSpeaking: false,
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(batch.commit());

  await assertSucceeds(bob.collection("rooms").where("isPublic", "==", true).get());
  await alice.collection("rooms").doc("room_private").set(validRoomPayload(ALICE_UID, false));
  await assertFails(bob.collection("rooms").get());
  await assertFails(bob.collection("rooms").doc("room_private").get());

  await assertSucceeds(
    bob.collection("rooms").doc("room_101").collection("seats").doc("seat_1").update({
      occupantUid: BOB_UID,
      occupantName: "Bob",
      occupantAvatarUrl: "",
      isMuted: false,
      isSpeaking: false,
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    bob.collection("rooms").doc("room_101").collection("seats").doc("seat_1").update({
      isLocked: true,
      updatedAt: serverTimestamp(),
    })
  );
});

// 4. Admin-only Emoji & Gift Catalog + Email Spoof Protection + Global Gift Events
test("Only verified admin Gmail can add emojis & gifts; all signed-in users can read & broadcast gift_events", async () => {
  const verifiedAdmin = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  const spoofedAdmin = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  const bob = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@gmail.com",
    email_verified: true,
  }).firestore();

  const emojiPayload = {
    createdByUid: ADMIN_UID,
    label: "रॉयल फायर",
    category: "ट्रेंडिंग",
    symbol: "🔥",
    imageUrl: "",
    isActive: true,
    createdAt: serverTimestamp(),
  };

  // Spoofed unverified admin & normal user must be denied
  await assertFails(spoofedAdmin.collection("emoji_catalog").doc("emo_1").set({ ...emojiPayload, createdByUid: "spoof_uid" }));
  await assertFails(bob.collection("emoji_catalog").doc("emo_2").set({ ...emojiPayload, createdByUid: BOB_UID }));

  // Verified admin succeeds
  await assertSucceeds(verifiedAdmin.collection("emoji_catalog").doc("emo_1").set(emojiPayload));

  // Admin creates full-screen video gift
  const giftPayload = {
    createdByUid: ADMIN_UID,
    name: "शाही ड्रैगन वीडियो",
    category: "वीडियो VIP",
    emoji: "🐉",
    thumbnailUrl: "",
    mediaUrl: "https://proj.supabase.co/storage/v1/object/public/swarroom-media/gifts/dragon.mp4",
    mediaType: "video",
    coinCost: 500,
    isActive: true,
    createdAt: serverTimestamp(),
  };
  await assertFails(bob.collection("gift_catalog").doc("gift_1").set({ ...giftPayload, createdByUid: BOB_UID }));
  await assertSucceeds(verifiedAdmin.collection("gift_catalog").doc("gift_1").set(giftPayload));

  // All signed-in users can read active catalog items
  await assertSucceeds(bob.collection("emoji_catalog").where("isActive", "==", true).get());
  await assertSucceeds(bob.collection("gift_catalog").where("isActive", "==", true).get());

  // Bob creates a room & broadcasts a gift_event
  await bob.collection("rooms").doc("room_202").set(validRoomPayload(BOB_UID, true));
  await assertSucceeds(
    bob.collection("gift_events").doc("evt_1").set({
      roomId: "room_202",
      roomTitle: "Cricket & Chill",
      senderUid: BOB_UID,
      senderName: "Bob",
      senderAvatarUrl: "",
      recipientUid: ADMIN_UID,
      recipientName: "Chandra",
      giftId: "gift_1",
      giftName: "शाही ड्रैगन वीडियो",
      giftEmoji: "🐉",
      giftMediaUrl: "https://proj.supabase.co/storage/v1/object/public/swarroom-media/gifts/dragon.mp4",
      giftMediaType: "video",
      coinCost: 500,
      quantity: 1,
      isPublic: true,
      createdAt: serverTimestamp(),
    })
  );
  await assertSucceeds(verifiedAdmin.collection("gift_events").where("isPublic", "==", true).get());
  await assertFails(bob.collection("gift_events").doc("evt_1").delete());
  await assertSucceeds(verifiedAdmin.collection("gift_events").doc("evt_1").delete());
});

// 5. Level Configs & Level Reward Claims
test("Only verified admin can write level_configs; all signed-in users can read level_configs and claim own level rewards once", async () => {
  const verifiedAdmin = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@gmail.com",
    email_verified: true,
  }).firestore();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();

  const levelPayload = {
    badgeLevel: 10,
    entryVideoUrl: "https://www.instagram.com/reel/Dfqs1NiPAe3/",
    avatarFrameUrl: "https://example.supabase.co/storage/v1/object/public/swarroom-media/frames/silver.gif",
    rewardCoins: 2500,
    profileBadgeUrl: "https://example.supabase.co/storage/v1/object/public/swarroom-media/badges/lv10.gif",
    updatedByUid: ADMIN_UID,
    updatedAt: serverTimestamp(),
  };

  await assertFails(bob.collection("level_configs").doc("10").set({ ...levelPayload, updatedByUid: BOB_UID }));
  await assertFails(verifiedAdmin.collection("level_configs").doc("99").set({ ...levelPayload, badgeLevel: 99 }));
  await assertSucceeds(verifiedAdmin.collection("level_configs").doc("10").set(levelPayload));
  await assertSucceeds(bob.collection("level_configs").get());

  // Level reward claim isolated per user & immutable
  const bobClaimId = `${BOB_UID}_10`;
  const aliceClaimId = `${ALICE_UID}_10`;
  await assertSucceeds(
    bob.collection("level_claims").doc(bobClaimId).set({
      userId: BOB_UID,
      badgeLevel: 10,
      coinsGranted: 2500,
      claimedAt: serverTimestamp(),
    })
  );
  await assertFails(
    bob.collection("level_claims").doc(aliceClaimId).set({
      userId: ALICE_UID,
      badgeLevel: 10,
      coinsGranted: 2500,
      claimedAt: serverTimestamp(),
    })
  );
  await assertFails(bob.collection("level_claims").doc(bobClaimId).update({ coinsGranted: 9999 }));
  await assertSucceeds(bob.collection("level_claims").where("userId", "==", BOB_UID).get());
  await assertFails(alice.collection("level_claims").get());
});

// 6. Live Room Presence, Heartbeats & Entry Message Cleanup
test("Users manage own room presence heartbeat, host can prune stale presence, and users delete own join messages", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  const roomRef = alice.collection("rooms").doc("room_live_1");
  await assertSucceeds(roomRef.set(validRoomPayload(ALICE_UID, true)));
  await assertSucceeds(
    roomRef.collection("seats").doc("seat_1").set({
      roomId: "room_live_1",
      roomOwnerId: ALICE_UID,
      isRoomPublic: true,
      seatIndex: 1,
      occupantUid: BOB_UID,
      occupantName: "Bob",
      occupantAvatarUrl: "",
      isLocked: false,
      isMuted: false,
      isSpeaking: false,
      updatedAt: serverTimestamp(),
    })
  );

  const bobRoomRef = bob.collection("rooms").doc("room_live_1");

  // Seated user can refresh updatedAt heartbeat
  await assertSucceeds(
    bobRoomRef.collection("seats").doc("seat_1").update({
      updatedAt: serverTimestamp(),
    })
  );

  // Bob creates and updates own presence heartbeat
  const bobPresenceRef = bobRoomRef.collection("presence").doc(BOB_UID);
  await assertSucceeds(
    bobPresenceRef.set({
      userId: BOB_UID,
      roomId: "room_live_1",
      roomOwnerId: ALICE_UID,
      isRoomPublic: true,
      displayName: "Bob",
      avatarUrl: "",
      clubTag: "UP55",
      joinedAt: serverTimestamp(),
      lastSeenAt: serverTimestamp(),
    })
  );

  await assertFails(
    bobRoomRef.collection("presence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      roomId: "room_live_1",
      roomOwnerId: ALICE_UID,
      isRoomPublic: true,
      displayName: "Spoof",
      avatarUrl: "",
      clubTag: "UP55",
      joinedAt: serverTimestamp(),
      lastSeenAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    bobPresenceRef.update({
      lastSeenAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    alice.collection("rooms").doc("room_live_1").collection("presence").where("isRoomPublic", "==", true).get()
  );

  // Room join message can be deleted by sender when leaving
  const msgRef = bobRoomRef.collection("messages").doc("sys_bob_join");
  await assertSucceeds(
    msgRef.set({
      roomId: "room_live_1",
      roomOwnerId: ALICE_UID,
      isRoomPublic: true,
      senderUid: BOB_UID,
      senderName: "Bob",
      senderAvatarUrl: "",
      text: "Bob का कमरे में स्वागत है",
      messageType: "join",
      badgeTag: "UP55",
      createdAt: serverTimestamp(),
    })
  );
  await assertSucceeds(msgRef.delete());

  // Room owner (Alice) can prune Bob's stale presence doc
  await assertSucceeds(roomRef.collection("presence").doc(BOB_UID).delete());
});

// 7. Global GitHub App Update Config
test("Only verified admin can configure app_config/update; signed-in users can read it", async () => {
  const verifiedAdmin = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@gmail.com",
    email_verified: true,
  }).firestore();
  const unauth = testEnv.unauthenticatedContext().firestore();

  const updatePayload = {
    githubRepoUrl: "https://github.com/chndra3735/SwarRoom",
    updatedByUid: ADMIN_UID,
    updatedAt: serverTimestamp(),
  };

  await assertFails(bob.collection("app_config").doc("update").set({ ...updatePayload, updatedByUid: BOB_UID }));
  await assertSucceeds(verifiedAdmin.collection("app_config").doc("update").set(updatePayload));
  await assertSucceeds(bob.collection("app_config").doc("update").get());
  await assertFails(bob.collection("app_config").get());
  await assertFails(unauth.collection("app_config").doc("update").get());
});

// 8. Private User Audit Profiles (Onboarding, Location, DOB, Conduct Checkbox, Admin Block) & 7-Month Activity Logs
test("Users save own onboarding audit profile & 7-month logs; ONLY verified admin can list/read all users and audit logs", async () => {
  const verifiedAdmin = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const alice = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@gmail.com",
    email_verified: true,
  }).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@gmail.com",
    email_verified: true,
  }).firestore();

  const auditProfilePayload = {
    userId: ALICE_UID,
    numericId: "1234567890",
    userEmail: "alice@gmail.com",
    displayName: "Alice",
    avatarUrl: "https://lh3.googleusercontent.com/alice.jpg",
    dobIso: "2001-08-15",
    ageYears: 25,
    locationLabel: "Lucknow, Uttar Pradesh, India",
    locationLat: 26.85,
    locationLng: 80.95,
    locationStatus: "captured",
    termsAccepted: true,
    onboardingCompleted: true,
    isBlockedByAdmin: false,
    firstLoginAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };

  // Alice creates her own audit profile; cannot self-block or spoof Bob
  await assertSucceeds(alice.collection("user_audit_profiles").doc(ALICE_UID).set(auditProfilePayload));
  await assertFails(
    alice.collection("user_audit_profiles").doc(ALICE_UID).update({
      isBlockedByAdmin: true,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(bob.collection("user_audit_profiles").doc(ALICE_UID).get());
  await assertFails(bob.collection("user_audit_profiles").get());

  // Verified admin can list, inspect, and moderate block state
  await assertSucceeds(verifiedAdmin.collection("user_audit_profiles").get());
  await assertSucceeds(
    verifiedAdmin.collection("user_audit_profiles").doc(ALICE_UID).update({
      isBlockedByAdmin: true,
      updatedAt: serverTimestamp(),
    })
  );

  // 7-month retained activity log creation & duration update
  const sevenMonthsOut = new Date(Date.now() + 210 * 24 * 60 * 60 * 1000);
  const tooLongExpiry = new Date(Date.now() + 260 * 24 * 60 * 60 * 1000);
  const visitLogPayload = {
    userId: ALICE_UID,
    numericId: "1234567890",
    userEmail: "alice@gmail.com",
    userName: "Alice",
    userAvatarUrl: "https://lh3.googleusercontent.com/alice.jpg",
    activityType: "room_visit",
    roomId: "room_1",
    roomTitle: "Music Room",
    roomCode: "112233",
    conversationId: "",
    targetUid: "",
    targetNumericId: "",
    targetName: "",
    textContent: "",
    mediaUrl: "",
    dateKey: "2026-11-21",
    enteredAtMs: Date.now() - 60000,
    exitedAtMs: 0,
    durationSeconds: 0,
    createdAt: serverTimestamp(),
    expiresAt: sevenMonthsOut,
  };

  await assertSucceeds(alice.collection("user_activity_logs").doc("visit_1").set(visitLogPayload));
  await assertSucceeds(
    alice.collection("user_activity_logs").doc("visit_1").update({
      exitedAtMs: Date.now(),
      durationSeconds: 120,
    })
  );
  // Expiry > 225 days rejected
  await assertFails(
    alice.collection("user_activity_logs").doc("visit_bad_exp").set({
      ...visitLogPayload,
      expiresAt: tooLongExpiry,
    })
  );
  // Non-admin cannot read/list/delete activity logs; only admin can
  await assertFails(alice.collection("user_activity_logs").doc("visit_1").get());
  await assertFails(bob.collection("user_activity_logs").get());
  await assertFails(alice.collection("user_activity_logs").doc("visit_1").delete());
  await assertSucceeds(verifiedAdmin.collection("user_activity_logs").get());
  await assertSucceeds(verifiedAdmin.collection("user_activity_logs").doc("visit_1").delete());

  // Inbox DM image media write
  const convoId = `${ALICE_UID}_${BOB_UID}`;
  await assertSucceeds(
    alice.collection("conversations").doc(convoId).set({
      participantUids: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantAvatars: { [ALICE_UID]: "", [BOB_UID]: "" },
      lastMessage: "DP भेजा",
      lastSenderUid: ALICE_UID,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    alice.collection("conversations").doc(convoId).collection("messages").doc("dm_img_1").set({
      conversationId: convoId,
      participantUids: [ALICE_UID, BOB_UID],
      senderUid: ALICE_UID,
      senderName: "Alice",
      text: "📷 फोटो / DP भेजा",
      mediaUrl: "https://proj.supabase.co/storage/v1/object/public/swarroom-media/dm/pic.jpg",
      mediaType: "image",
      createdAt: serverTimestamp(),
    })
  );
});

// 9. Room Settings Studio: Custom Theme, PIN Lock, Dynamic Seat Count, Managers (max 5), Kick Audit Logs & Numeric ID Lookup
test("Host configures theme/PIN/seats/managers; manager can lock/mute/kick non-owner and write kick_logs; numeric lookup forbids list", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieUid = "charlie_uid_3";
  const charlie = testEnv.authenticatedContext(charlieUid).firestore();

  // Numeric ID lookup allows get-by-ID for signed-in users, denies list and cross-user write
  await assertSucceeds(
    bob.collection("numeric_id_lookup").doc("2222222222").set({
      userId: BOB_UID,
      numericId: "2222222222",
      displayName: "Bob",
      avatarUrl: "",
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(alice.collection("numeric_id_lookup").doc("2222222222").get());
  await assertFails(alice.collection("numeric_id_lookup").get());
  await assertFails(
    alice.collection("numeric_id_lookup").doc("3333333333").set({
      userId: BOB_UID,
      numericId: "3333333333",
      displayName: "Spoof",
      avatarUrl: "",
      updatedAt: serverTimestamp(),
    })
  );

  // Alice creates a room and seats
  const roomRef = alice.collection("rooms").doc("room_ctrl_1");
  await assertSucceeds(roomRef.set(validRoomPayload(ALICE_UID, true)));
  await assertSucceeds(
    roomRef.collection("seats").doc("seat_0").set({
      roomId: "room_ctrl_1",
      roomOwnerId: ALICE_UID,
      isRoomPublic: true,
      seatIndex: 0,
      occupantUid: ALICE_UID,
      occupantName: "Alice",
      occupantAvatarUrl: "",
      isLocked: false,
      isMuted: false,
      isSpeaking: false,
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    roomRef.collection("seats").doc("seat_1").set({
      roomId: "room_ctrl_1",
      roomOwnerId: ALICE_UID,
      isRoomPublic: true,
      seatIndex: 1,
      occupantUid: charlieUid,
      occupantName: "Charlie",
      occupantAvatarUrl: "",
      isLocked: false,
      isMuted: false,
      isSpeaking: false,
      updatedAt: serverTimestamp(),
    })
  );

  // Host updates custom background theme, PIN lock, seatCount, and assigns Bob as manager
  await assertSucceeds(
    roomRef.update({
      themeBackgroundUrl: "https://proj.supabase.co/storage/v1/object/public/swarroom-media/room_themes/royal.jpg",
      isPinLocked: true,
      roomPin: "4590",
      seatCount: 8,
      managerUids: [BOB_UID],
      updatedAt: serverTimestamp(),
    })
  );

  // Exceeding 5 managers fails
  await assertFails(
    roomRef.update({
      managerUids: ["m1", "m2", "m3", "m4", "m5", "m6"],
      updatedAt: serverTimestamp(),
    })
  );

  // Non-owner Bob cannot resize seatCount
  await assertFails(
    bob.collection("rooms").doc("room_ctrl_1").update({
      seatCount: 12,
      updatedAt: serverTimestamp(),
    })
  );

  // Manager Bob can mute & kick Charlie (non-owner) and record a kick_log
  const bobRoom = bob.collection("rooms").doc("room_ctrl_1");
  await assertSucceeds(
    bobRoom.collection("seats").doc("seat_1").update({
      isMuted: true,
      isSpeaking: false,
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    bobRoom.collection("seats").doc("seat_1").update({
      occupantUid: "",
      occupantName: "",
      occupantAvatarUrl: "",
      isMuted: false,
      isSpeaking: false,
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    bobRoom.collection("kick_logs").doc("kick_1").set({
      roomId: "room_ctrl_1",
      roomOwnerId: ALICE_UID,
      isRoomPublic: true,
      actorUid: BOB_UID,
      actorNumericId: "2222222222",
      actorName: "Bob",
      actorAvatarUrl: "",
      actorRole: "manager",
      targetUid: charlieUid,
      targetNumericId: "3333333333",
      targetName: "Charlie",
      targetAvatarUrl: "",
      reason: "रूम नियम उल्लंघन",
      kickedAtMs: Date.now(),
      createdAt: serverTimestamp(),
    })
  );

  // Manager Bob CANNOT mute or kick Host Alice, and Charlie cannot forge a kick_log
  await assertFails(
    bobRoom.collection("seats").doc("seat_0").update({
      isMuted: true,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    charlie.collection("rooms").doc("room_ctrl_1").collection("kick_logs").doc("kick_fake").set({
      roomId: "room_ctrl_1",
      roomOwnerId: ALICE_UID,
      isRoomPublic: true,
      actorUid: charlieUid,
      actorNumericId: "3333333333",
      actorName: "Charlie",
      actorAvatarUrl: "",
      actorRole: "manager",
      targetUid: "other_uid",
      targetNumericId: "4444444444",
      targetName: "Other",
      targetAvatarUrl: "",
      reason: "",
      kickedAtMs: Date.now(),
      createdAt: serverTimestamp(),
    })
  );

  // Room owner & members can list kick_logs; deleting kick_logs is forbidden
  await assertSucceeds(roomRef.collection("kick_logs").where("isRoomPublic", "==", true).get());
  await assertFails(roomRef.collection("kick_logs").doc("kick_1").delete());
});


