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
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validOrderPayload(userId, overrides = {}) {
  return {
    userId,
    shopId: "s1",
    shopName: "النور للذهب",
    items: [
      {
        productId: "p1",
        shopId: "s1",
        name: "جنيه ذهب",
        category: "جنيهات وسبائك",
        karat: 21,
        weightGrams: 8.0,
        stamped: true,
        quantity: 1,
      },
    ],
    goldValue: 43400.0,
    makingFee: 720.0,
    serviceFee: 441.2,
    pickupWindow: "اليوم · 4:00–8:00 م",
    status: "READY_FOR_PICKUP",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

function validPrefsPayload(userId, overrides = {}) {
  return {
    userId,
    kycFrontDone: true,
    kycBackDone: false,
    kycSelfieDone: false,
    kycStatus: "NOT_STARTED",
    selectedPaymentMethodId: "card",
    priceAlertEnabled: true,
    priceAlertAbove: true,
    priceAlertTarget: "5500",
    favoriteShopIds: ["s1"],
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot list or create orders or preferences", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/orders`).get());
  await assertFails(
    unauthDb
      .collection(`users/${ALICE_UID}/orders`)
      .doc("KG-10001")
      .set(validOrderPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/preferences/settings`).get()
  );
});

test("Authenticated owner can create and list their own orders", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/orders`)
      .doc("KG-20481")
      .set(validOrderPayload(ALICE_UID))
  );
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/orders`).get());
});

test("Cross-user access to orders and preferences is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/orders`)
      .doc("KG-20481")
      .set(validOrderPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/preferences/settings`)
      .set(validPrefsPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/orders`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/orders/KG-20481`).get()
  );
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/preferences/settings`).get()
  );
  await assertFails(
    bobDb
      .doc(`users/${ALICE_UID}/orders/KG-99999`)
      .set(validOrderPayload(BOB_UID))
  );
});

test("Shadow fields, spoofed userId, invalid orderId, and negative amounts are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/orders/KG-20482`)
      .set(validOrderPayload(ALICE_UID, { isAdmin: true }))
  );
  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/orders/KG-20483`)
      .set(validOrderPayload(BOB_UID))
  );
  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/orders/bad$id`)
      .set(validOrderPayload(ALICE_UID))
  );
  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/orders/KG-20484`)
      .set(validOrderPayload(ALICE_UID, { goldValue: -10 }))
  );
});

test("Order lifecycle transitions and terminal state locking are enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb.doc(`users/${ALICE_UID}/orders/KG-20485`);
  await assertSucceeds(orderRef.set(validOrderPayload(ALICE_UID)));

  // Attempting to tamper with goldValue during completion fails
  await assertFails(
    orderRef.update({
      status: "COMPLETED",
      goldValue: 1,
      updatedAt: serverTimestamp(),
    })
  );

  // Valid completion succeeds
  await assertSucceeds(
    orderRef.update({
      status: "COMPLETED",
      updatedAt: serverTimestamp(),
    })
  );

  // Valid first rating after completion succeeds
  await assertSucceeds(
    orderRef.update({
      ratingGiven: 5,
      updatedAt: serverTimestamp(),
    })
  );

  // Re-rating or reverting completed status fails
  await assertFails(
    orderRef.update({
      ratingGiven: 1,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    orderRef.update({
      status: "READY_FOR_PICKUP",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(orderRef.delete());
});

test("Order dispute transitions and locks further updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb.doc(`users/${ALICE_UID}/orders/KG-20486`);
  await assertSucceeds(orderRef.set(validOrderPayload(ALICE_UID)));

  await assertSucceeds(
    orderRef.update({
      status: "DISPUTED",
      disputeReason: "القطعة غير مطابقة للوزن",
      updatedAt: serverTimestamp(),
    })
  );

  // Terminal state lock after DISPUTED
  await assertFails(
    orderRef.update({
      status: "COMPLETED",
      updatedAt: serverTimestamp(),
    })
  );
});

test("User preferences CRUD enforces 'settings' doc ID and immutable createdAt", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/preferences/other`)
      .set(validPrefsPayload(ALICE_UID))
  );

  const prefsRef = aliceDb.doc(`users/${ALICE_UID}/preferences/settings`);
  await assertSucceeds(prefsRef.set(validPrefsPayload(ALICE_UID)));
  await assertSucceeds(
    prefsRef.update({
      kycBackDone: true,
      kycSelfieDone: true,
      kycStatus: "VERIFIED",
      priceAlertTarget: "5600",
      favoriteShopIds: ["s1", "s2"],
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    prefsRef.update({
      favoriteShopIds: [],
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    prefsRef.update({
      favoriteShopIds: [123],
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    prefsRef.update({
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(prefsRef.delete());
});
