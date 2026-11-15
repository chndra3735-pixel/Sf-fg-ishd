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
});
