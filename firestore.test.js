const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CREATOR_UID = "creator_001";
const CREATOR_EMAIL = "s21903125@gmail.com";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function creatorDb() {
  return testEnv.authenticatedContext(CREATOR_UID, {
    email: CREATOR_EMAIL,
    email_verified: true,
  }).firestore();
}

function spoofedCreatorDb() {
  return testEnv.authenticatedContext("spoof_999", {
    email: CREATOR_EMAIL,
    email_verified: false,
  }).firestore();
}

function aliceDb() {
  return testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
}

function bobDb() {
  return testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("vip_trades").get());
  await assertFails(unauth.collection("market_analyses").get());
  await assertFails(unauth.collection("community_messages").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("admin_meta").doc("config").get());
});

test("User profile isolation: create, update own prefs, block privilege escalation and cross-user PII read", async () => {
  const now = new Date(Date.now() - 2000);
  const alice = aliceDb();
  const bob = bobDb();

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Trader",
      email: "alice@example.com",
      joinedGuidelines: false,
      suspended: false,
      fcmTokens: ["token_a"],
      mutedCategories: [],
      darkMode: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // User cannot self-suspend or inject ghost fields
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      suspended: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bob.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@example.com",
      joinedGuidelines: false,
      suspended: true,
      fcmTokens: [],
      mutedCategories: [],
      darkMode: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Allowed profile update
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      joinedGuidelines: true,
      darkMode: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user PII read & list denied for normal users
  await assertFails(bob.collection("users").doc(ALICE_UID).get());
  await assertFails(bob.collection("users").get());

  // Creator/admin can list and suspend/unsuspend users
  const admin = creatorDb();
  await assertSucceeds(admin.collection("users").get());
  await assertSucceeds(
    admin.collection("users").doc(ALICE_UID).update({
      suspended: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("One-time admin setup: only verified creator can create once; spoofed email & normal users blocked", async () => {
  const now = new Date(Date.now() - 2000);
  const creator = creatorDb();
  const spoof = spoofedCreatorDb();
  const alice = aliceDb();

  // Normal user and unverified email spoof rejected
  await assertFails(
    alice.collection("admin_meta").doc("config").set({
      creatorUid: ALICE_UID,
      adminUserId: "hacker",
      passwordHash: "hash_12345678901234567890",
      salt: "salt_123456789012",
      failedAttempts: 0,
      lockedUntilEpochMs: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    spoof.collection("admin_meta").doc("config").set({
      creatorUid: "spoof_999",
      adminUserId: "spoof",
      passwordHash: "hash_12345678901234567890",
      salt: "salt_123456789012",
      failedAttempts: 0,
      lockedUntilEpochMs: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Creator creates once
  await assertSucceeds(
    creator.collection("admin_meta").doc("config").set({
      creatorUid: CREATOR_UID,
      adminUserId: "chartadmin",
      passwordHash: "pbkdf2_hash_placeholder_1234567890",
      salt: "random_salt_123456",
      failedAttempts: 0,
      lockedUntilEpochMs: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cannot delete or recreate
  await assertFails(creator.collection("admin_meta").doc("config").delete());

  // Can update password/lockout
  await assertSucceeds(
    creator.collection("admin_meta").doc("config").update({
      failedAttempts: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("VIP trades and market analyses: admin writes, normal users react +/-1 only", async () => {
  const now = new Date(Date.now() - 2000);
  const creator = creatorDb();
  const alice = aliceDb();

  await assertSucceeds(
    creator.collection("vip_trades").doc("trade_1").set({
      title: "EUR/USD Bullish Breakout",
      body: "Entry on 15M retest above 1.0840.",
      pair: "EUR/USD",
      direction: "LONG",
      entryPrice: "1.0845",
      stopLoss: "1.0815",
      takeProfit: "1.0910",
      authorId: CREATOR_UID,
      reactionFire: 0,
      reactionRocket: 0,
      reactionHundred: 0,
      reactionBull: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Normal user cannot create or edit trade text
  await assertFails(
    alice.collection("vip_trades").doc("trade_2").set({
      title: "Fake Signal",
      body: "Fake",
      pair: "BTC/USD",
      direction: "LONG",
      entryPrice: "90000",
      stopLoss: "88000",
      takeProfit: "95000",
      authorId: ALICE_UID,
      reactionFire: 0,
      reactionRocket: 0,
      reactionHundred: 0,
      reactionBull: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    alice.collection("vip_trades").doc("trade_1").update({
      title: "Hijacked",
    })
  );

  // Normal user can toggle reaction count by +1
  await assertSucceeds(
    alice.collection("vip_trades").doc("trade_1").update({
      reactionFire: 1,
    })
  );
  // Jumping reaction by +50 fails
  await assertFails(
    alice.collection("vip_trades").doc("trade_1").update({
      reactionFire: 50,
    })
  );
});

test("Community moderation: suspended user blocked from posting on backend", async () => {
  const now = new Date(Date.now() - 2000);
  const alice = aliceDb();
  const creator = creatorDb();

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      joinedGuidelines: true,
      suspended: false,
      fcmTokens: [],
      mutedCategories: [],
      darkMode: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    alice.collection("community_messages").doc("msg_1").set({
      userId: ALICE_UID,
      authorName: "Alice",
      authorInitials: "A",
      body: "Watching EUR/USD London open!",
      tickerTag: "EUR/USD",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin suspends Alice
  await assertSucceeds(
    creator.collection("users").doc(ALICE_UID).update({
      suspended: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Suspended Alice cannot post
  await assertFails(
    alice.collection("community_messages").doc("msg_2").set({
      userId: ALICE_UID,
      authorName: "Alice",
      authorInitials: "A",
      body: "Blocked message",
      tickerTag: "",
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Admin can delete msg_1
  await assertSucceeds(creator.collection("community_messages").doc("msg_1").delete());
});

test("Issue reports: user creates own open report & filtered list; admin resolves", async () => {
  const now = new Date(Date.now() - 2000);
  const alice = aliceDb();
  const bob = bobDb();
  const creator = creatorDb();

  await assertSucceeds(
    alice.collection("issue_reports").doc("rep_1").set({
      userId: ALICE_UID,
      userEmail: "alice@example.com",
      userName: "Alice",
      subject: "Chart zoom",
      description: "Pinch zoom on chart detail",
      status: "open",
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    alice.collection("issue_reports").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(alice.collection("issue_reports").get());
  await assertFails(bob.collection("issue_reports").doc("rep_1").get());

  await assertSucceeds(
    creator.collection("issue_reports").doc("rep_1").update({
      status: "resolved",
      adminNote: "Fixed in next build",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
