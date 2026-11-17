const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const MERCHANT_UID = "merchant_789";
const ADMIN_UID = "admin_root";
const ADMIN_EMAIL = "esmailtarek228@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext(verified = true) {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: verified,
  });
}

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
    makingFee: 0.0,
    serviceFee: 0.0,
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
    priceAlertAbove: false,
    priceAlertKarat: 24,
    priceAlertTarget: "6000",
    favoriteShopIds: ["s1"],
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

function emptyWalletPayload(userId, overrides = {}) {
  return {
    userId,
    cashAvailablePiasters: 0,
    cashPendingPiasters: 0,
    cashTotalCreditedPiasters: 0,
    cashTotalDebitedPiasters: 0,
    goldConfirmedMg: 0,
    goldReservedMg: 0,
    goldPendingMg: 0,
    gold24kMg: 0,
    gold21kMg: 0,
    gold18kMg: 0,
    totalInvestedPiasters: 0,
    lastSettlementRef: "init",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

function validMerchantPayload(ownerUid, overrides = {}) {
  return {
    ownerUid,
    shopName: "مجوهرات الشريف",
    area: "مدينة نصر",
    address: "15 شارع عباس العقاد، مدينة نصر",
    contactPhone: "+201001122334",
    verifiedEmail: "merchant@example.com",
    description: "محل معتمد لبيع السبائك والمشغولات",
    rating: 5.0,
    reviewsCount: 0,
    status: "PENDING",
    prepaidMakingApproved: false,
    prepaidMakingBudgetPiasters: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

function validMerchantKycPayload(ownerUid, overrides = {}) {
  return {
    ownerUid,
    legalFullName: "أحمد محمد الشريف",
    nationalIdNumber: "29001010101234",
    commercialRegNumber: "CR-88219",
    taxCardNumber: "TX-44210",
    authorizationNotes: "مفوض بالتوقيع عن الشركة",
    documentThumbnails: ["thumb_base64_sample"],
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

function validProductPayload(merchantId, ownerUid, overrides = {}) {
  return {
    merchantId,
    ownerUid,
    merchantName: "مجوهرات الشريف",
    name: "سبيكة عيار 24 وزن 10 جم",
    description: "سبيكة مغلفة ومعتمدة بدون مصنعية إضافية للمشتري",
    category: "جنيهات وسبائك",
    karat: 24,
    weightMg: 10000,
    weightGrams: 10.0,
    sellingPricePiasters: 6200000,
    stockQuantity: 5,
    stamped: true,
    imageThumbnails: ["img_1"],
    status: "PENDING_REVIEW",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

function validPricingPayload(adminUid, overrides = {}) {
  return {
    reference24kPerGramPiasters: 620000,
    reference21kPerGramPiasters: 542500,
    reference18kPerGramPiasters: 465000,
    valuation24kPerGramPiasters: 615000,
    valuation21kPerGramPiasters: 538000,
    valuation18kPerGramPiasters: 461000,
    merchantPolicyNote: "تمت تسوية المصنعية مسبقاً بين المنصة والتاجر",
    updatedByAdminUid: adminUid,
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

test("Unauthenticated user is denied across existing and new collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/orders`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/orders/KG-10001`).set(validOrderPayload(ALICE_UID)));
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/preferences/settings`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/wallet/summary`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/cart/current`).get());
  await assertFails(unauthDb.collection("merchants").where("status", "==", "APPROVED").get());
  await assertFails(unauthDb.collection("products").where("status", "==", "ACTIVE").get());
  await assertFails(unauthDb.doc("platform_config/pricing").get());
  await assertFails(unauthDb.collection("sell_requests").where("customerUid", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("audit_logs").get());
});

test("Orders & preferences backward-compatible rules and lifecycle locks still work", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const orderRef = aliceDb.doc(`users/${ALICE_UID}/orders/KG-20485`);
  await assertSucceeds(orderRef.set(validOrderPayload(ALICE_UID)));
  await assertFails(bobDb.doc(`users/${ALICE_UID}/orders/KG-20485`).get());

  await assertFails(orderRef.update({ status: "COMPLETED", goldValue: 1, updatedAt: serverTimestamp() }));
  await assertSucceeds(orderRef.update({ status: "COMPLETED", updatedAt: serverTimestamp() }));
  await assertSucceeds(orderRef.update({ ratingGiven: 5, updatedAt: serverTimestamp() }));
  await assertFails(orderRef.update({ ratingGiven: 1, updatedAt: serverTimestamp() }));
  await assertFails(orderRef.delete());

  const prefsRef = aliceDb.doc(`users/${ALICE_UID}/preferences/settings`);
  await assertSucceeds(prefsRef.set(validPrefsPayload(ALICE_UID)));
  await assertFails(bobDb.doc(`users/${ALICE_UID}/preferences/settings`).get());
});

test("User persistent cart CRUD is strictly scoped to owner and 'current' doc", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const cartPayload = {
    userId: ALICE_UID,
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
        quantity: 2,
      },
    ],
    pickupSlotIndex: 0,
    priceLockExpiresAtMs: Date.now() + 600000,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };

  await assertFails(aliceDb.doc(`users/${ALICE_UID}/cart/other`).set(cartPayload));
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/cart/current`).set(cartPayload));
  await assertFails(bobDb.doc(`users/${ALICE_UID}/cart/current`).get());
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/cart/current`).delete());
});

test("Merchant registration, KYC PII isolation, admin verification & product isolation", async () => {
  const merchantDb = testEnv.authenticatedContext(MERCHANT_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const admDb = adminContext(true).firestore();
  const unverifiedAdmDb = adminContext(false).firestore();

  // Unverified email spoof of admin fails
  await assertFails(unverifiedAdmDb.doc("platform_config/pricing").set(validPricingPayload(ADMIN_UID)));

  // Merchant cannot self-approve upon creation
  await assertFails(
    merchantDb.doc("merchants/m_1").set(validMerchantPayload(MERCHANT_UID, { status: "APPROVED" }))
  );

  // Merchant registers in PENDING state with private KYC doc + initial audit log
  await assertSucceeds(merchantDb.doc("merchants/m_1").set(validMerchantPayload(MERCHANT_UID)));
  await assertSucceeds(merchantDb.doc("merchants/m_1/private/kyc").set(validMerchantKycPayload(MERCHANT_UID)));
  await assertSucceeds(
    merchantDb.doc("merchants/m_1/audit_logs/log_1").set({
      merchantId: "m_1",
      merchantOwnerUid: MERCHANT_UID,
      actorUid: MERCHANT_UID,
      action: "SUBMITTED",
      note: "تم تقديم مستندات المتجر",
      createdAt: serverTimestamp(),
    })
  );

  // Other user cannot read private KYC docs or list audit logs
  await assertFails(aliceDb.doc("merchants/m_1/private/kyc").get());
  await assertFails(aliceDb.doc("merchants/m_1").get()); // Not approved yet!

  // Pending merchant CANNOT publish or create products
  await assertFails(merchantDb.doc("products/prod_1").set(validProductPayload("m_1", MERCHANT_UID)));

  // Merchant CANNOT self-approve
  await assertFails(
    merchantDb.doc("merchants/m_1").update({ status: "APPROVED", updatedAt: serverTimestamp() })
  );

  // Bootstrap Admin inspects KYC and approves merchant with prepaid making budget
  await assertSucceeds(admDb.doc("merchants/m_1/private/kyc").get());
  await assertSucceeds(
    admDb.doc("merchants/m_1").update({
      status: "APPROVED",
      prepaidMakingApproved: true,
      prepaidMakingBudgetPiasters: 50000000,
      pricingPolicyNote: "تم استلام دفعة المصنعية مقدماً",
      updatedAt: serverTimestamp(),
    })
  );

  // Approved merchant now visible to customers
  await assertSucceeds(aliceDb.doc("merchants/m_1").get());

  // Approved merchant creates product in PENDING_REVIEW, cannot set directly ACTIVE
  await assertFails(
    merchantDb.doc("products/prod_1").set(validProductPayload("m_1", MERCHANT_UID, { status: "ACTIVE" }))
  );
  await assertSucceeds(
    merchantDb.doc("products/prod_1").set(validProductPayload("m_1", MERCHANT_UID))
  );

  // Admin approves product -> ACTIVE
  await assertSucceeds(
    admDb.doc("products/prod_1").update({ status: "ACTIVE", updatedAt: serverTimestamp() })
  );

  // Another user cannot edit merchant's product without purchasing
  await assertFails(
    aliceDb.doc("products/prod_1").update({ sellingPricePiasters: 100, updatedAt: serverTimestamp() })
  );

  // Buyer atomic purchase: create order + merchant_order + decrement stock atomically
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      const orderRef = aliceDb.doc(`users/${ALICE_UID}/orders/ORD-M1-001`);
      const mOrderRef = aliceDb.doc("merchant_orders/ORD-M1-001");
      const prodRef = aliceDb.doc("products/prod_1");
      tx.set(
        orderRef,
        validOrderPayload(ALICE_UID, {
          shopId: "m_1",
          shopName: "مجوهرات الشريف",
          merchantOwnerUid: MERCHANT_UID,
          totalPiasters: 6200000,
          paymentSource: "ESCROW_METHOD",
        })
      );
      tx.set(mOrderRef, {
        orderId: "ORD-M1-001",
        merchantId: "m_1",
        merchantOwnerUid: MERCHANT_UID,
        buyerUid: ALICE_UID,
        items: [
          {
            productId: "prod_1",
            shopId: "m_1",
            name: "سبيكة عيار 24 وزن 10 جم",
            category: "جنيهات وسبائك",
            karat: 24,
            weightGrams: 10.0,
            stamped: true,
            quantity: 1,
          },
        ],
        totalPiasters: 6200000,
        pickupWindow: "اليوم · 4:00–8:00 م",
        status: "READY_FOR_PICKUP",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      tx.update(prodRef, {
        stockQuantity: 4,
        lastOrderId: "ORD-M1-001",
        updatedAt: serverTimestamp(),
      });
    })
  );
});

test("Sell-Your-Gold workflow: admin custom making-charge valuation & atomic customer cash settlement", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const admDb = adminContext(true).firestore();

  // Initialize Alice wallet
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`).set(emptyWalletPayload(ALICE_UID)));

  // Customer cannot submit pre-evaluated offer
  await assertFails(
    aliceDb.doc("sell_requests/sell_1").set({
      customerUid: ALICE_UID,
      customerEmail: "alice@example.com",
      itemTitle: "غويشة عيار 21",
      category: "أساور",
      karat: 21,
      estimatedWeightMg: 15000,
      conditionNotes: "حالة ممتازة",
      photoThumbnails: ["photo_1"],
      status: "SUBMITTED",
      verifiedWeightMg: 15000,
      confirmedKarat: 21,
      referencePricePerGramPiasters: 542500,
      evaluatedGoldValuePiasters: 8137500,
      makingChargeDeductionPiasters: 0,
      finalOfferPiasters: 8137500,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Valid submission
  await assertSucceeds(
    aliceDb.doc("sell_requests/sell_1").set({
      customerUid: ALICE_UID,
      customerEmail: "alice@example.com",
      itemTitle: "غويشة عيار 21",
      category: "أساور",
      karat: 21,
      estimatedWeightMg: 15000,
      conditionNotes: "حالة ممتازة",
      photoThumbnails: ["photo_1"],
      status: "SUBMITTED",
      verifiedWeightMg: 0,
      confirmedKarat: 21,
      referencePricePerGramPiasters: 0,
      evaluatedGoldValuePiasters: 0,
      makingChargeDeductionPiasters: 0,
      finalOfferPiasters: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Bob cannot view or modify Alice's sell request
  await assertFails(bobDb.doc("sell_requests/sell_1").get());

  // Customer cannot self-evaluate
  await assertFails(
    aliceDb.doc("sell_requests/sell_1").update({
      status: "OFFER_SENT",
      finalOfferPiasters: 8000000,
      updatedAt: serverTimestamp(),
    })
  );

  // Admin inspects & sets verified weight + custom making-charge deduction (not fixed rate) -> OFFER_SENT
  await assertSucceeds(
    admDb.doc("sell_requests/sell_1").update({
      status: "OFFER_SENT",
      evaluatorAdminUid: ADMIN_UID,
      verifiedWeightMg: 14850,
      confirmedKarat: 21,
      referencePricePerGramPiasters: 542500,
      evaluatedGoldValuePiasters: 8056125,
      makingChargeDeductionPiasters: 156125, // Custom admin deduction
      finalOfferPiasters: 7900000,
      inspectionNotes: "الوزن الفعلي بعد إزالة فص صغير 14.85 جم",
      updatedAt: serverTimestamp(),
    })
  );

  // Forged settlement with wrong offer amount FAILS
  await assertFails(
    aliceDb.runTransaction(async (tx) => {
      tx.update(aliceDb.doc("sell_requests/sell_1"), {
        status: "COMPLETED",
        settlementTransactionId: "settle_bad",
        updatedAt: serverTimestamp(),
      });
      tx.set(aliceDb.doc(`users/${ALICE_UID}/ledger/settle_bad`), {
        userId: ALICE_UID,
        idempotencyKey: "settle_bad",
        txType: "GOLD_SALE_PROCEEDS",
        referenceId: "sell_1",
        karat: 21,
        cashDeltaPiasters: 9999999, // forged!
        pendingCashDeltaPiasters: 0,
        goldDeltaMg: 0,
        reservedDeltaMg: 0,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 542500,
        notes: "forged",
        createdAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        cashAvailablePiasters: 9999999,
        cashTotalCreditedPiasters: 9999999,
        lastSettlementRef: "settle_bad",
        updatedAt: serverTimestamp(),
      });
    })
  );

  // Valid atomic customer acceptance credits exact 7,900,000 piasters
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      tx.update(aliceDb.doc("sell_requests/sell_1"), {
        status: "COMPLETED",
        settlementTransactionId: "settle_ok_1",
        updatedAt: serverTimestamp(),
      });
      tx.set(aliceDb.doc(`users/${ALICE_UID}/ledger/settle_ok_1`), {
        userId: ALICE_UID,
        idempotencyKey: "settle_ok_1",
        txType: "GOLD_SALE_PROCEEDS",
        referenceId: "sell_1",
        karat: 21,
        cashDeltaPiasters: 7900000,
        pendingCashDeltaPiasters: 0,
        goldDeltaMg: 0,
        reservedDeltaMg: 0,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 542500,
        notes: "إيداع صافي بيع قطعة غويشة عيار 21",
        createdAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        cashAvailablePiasters: 7900000,
        cashTotalCreditedPiasters: 7900000,
        lastSettlementRef: "settle_ok_1",
        updatedAt: serverTimestamp(),
      });
    })
  );

  // Immutable ledger cannot be modified or deleted
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/ledger/settle_ok_1`).update({ cashDeltaPiasters: 9000000 })
  );
  await assertFails(aliceDb.doc(`users/${ALICE_UID}/ledger/settle_ok_1`).delete());
});

test("Fractional gold (100mg + 250mg), pricing verification, and >=1000mg physical withdrawal rule", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const admDb = adminContext(true).firestore();

  // Set platform pricing config
  await assertSucceeds(admDb.doc("platform_config/pricing").set(validPricingPayload(ADMIN_UID)));
  // Non-admin cannot modify pricing
  await assertFails(
    aliceDb.doc("platform_config/pricing").update({ reference24kPerGramPiasters: 100000, updatedAt: serverTimestamp() })
  );

  // Initialize wallet & credit 10,000 EGP (1,000,000 piasters) via Admin
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`).set(emptyWalletPayload(ALICE_UID)));
  await assertSucceeds(
    admDb.doc(`users/${ALICE_UID}/wallet/summary`).update({
      cashAvailablePiasters: 1000000,
      cashTotalCreditedPiasters: 1000000,
      lastSettlementRef: "adm_seed_1",
      updatedAt: serverTimestamp(),
    })
  );

  // Direct balance tampering by customer FAILS
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/wallet/summary`).update({
      cashAvailablePiasters: 99999999,
      updatedAt: serverTimestamp(),
    })
  );

  // Buy 100 mg of 24K gold (at 620,000 piasters/g -> 62,000 piasters)
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      tx.set(aliceDb.doc("fractional_orders/frac_1"), {
        customerUid: ALICE_UID,
        karat: 24,
        weightMg: 100,
        pricePerGramPiasters: 620000,
        totalPiasters: 62000,
        paymentSource: "CASH_WALLET",
        status: "CONFIRMED",
        ledgerEntryId: "led_frac_1",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      tx.set(aliceDb.doc(`users/${ALICE_UID}/ledger/led_frac_1`), {
        userId: ALICE_UID,
        idempotencyKey: "led_frac_1",
        txType: "FRACTIONAL_GOLD_BUY",
        referenceId: "frac_1",
        karat: 24,
        cashDeltaPiasters: -62000,
        pendingCashDeltaPiasters: 0,
        goldDeltaMg: 100,
        reservedDeltaMg: 0,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 620000,
        notes: "شراء 100 ملليجرام ذهب عيار 24",
        createdAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        cashAvailablePiasters: 938000,
        cashTotalDebitedPiasters: 62000,
        goldConfirmedMg: 100,
        gold24kMg: 100,
        totalInvestedPiasters: 62000,
        lastSettlementRef: "led_frac_1",
        updatedAt: serverTimestamp(),
      });
    })
  );

  // Buy another 250 mg of 24K gold (155,000 piasters) -> cumulative 350 mg
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      tx.set(aliceDb.doc("fractional_orders/frac_2"), {
        customerUid: ALICE_UID,
        karat: 24,
        weightMg: 250,
        pricePerGramPiasters: 620000,
        totalPiasters: 155000,
        paymentSource: "CASH_WALLET",
        status: "CONFIRMED",
        ledgerEntryId: "led_frac_2",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      tx.set(aliceDb.doc(`users/${ALICE_UID}/ledger/led_frac_2`), {
        userId: ALICE_UID,
        idempotencyKey: "led_frac_2",
        txType: "FRACTIONAL_GOLD_BUY",
        referenceId: "frac_2",
        karat: 24,
        cashDeltaPiasters: -155000,
        pendingCashDeltaPiasters: 0,
        goldDeltaMg: 250,
        reservedDeltaMg: 0,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 620000,
        notes: "شراء 250 ملليجرام ذهب عيار 24",
        createdAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        cashAvailablePiasters: 783000,
        cashTotalDebitedPiasters: 217000,
        goldConfirmedMg: 350,
        gold24kMg: 350,
        totalInvestedPiasters: 217000,
        lastSettlementRef: "led_frac_2",
        updatedAt: serverTimestamp(),
      });
    })
  );

  // Attempt physical withdrawal of 350 mg (< 1000 mg) FAILS in rules!
  await assertFails(
    aliceDb.runTransaction(async (tx) => {
      tx.set(aliceDb.doc("withdrawal_requests/w_small"), {
        customerUid: ALICE_UID,
        customerEmail: "alice@example.com",
        karat: 24,
        amountMg: 350,
        deliveryType: "PICKUP_HUB",
        deliveryAddress: "فرع المعادي الرئيسي",
        withdrawalRef: "WD-SMALL",
        status: "PENDING",
        ledgerEntryId: "led_wd_small",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      tx.set(aliceDb.doc(`users/${ALICE_UID}/ledger/led_wd_small`), {
        userId: ALICE_UID,
        idempotencyKey: "led_wd_small",
        txType: "GOLD_RESERVATION",
        referenceId: "w_small",
        karat: 24,
        cashDeltaPiasters: 0,
        pendingCashDeltaPiasters: 0,
        goldDeltaMg: 0,
        reservedDeltaMg: 350,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 620000,
        notes: "test",
        createdAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        goldReservedMg: 350,
        lastSettlementRef: "led_wd_small",
        updatedAt: serverTimestamp(),
      });
    })
  );

  // Accumulate another 650 mg (403,000 piasters) -> exact 1000 mg (1.000 g)!
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      tx.set(aliceDb.doc("fractional_orders/frac_3"), {
        customerUid: ALICE_UID,
        karat: 24,
        weightMg: 650,
        pricePerGramPiasters: 620000,
        totalPiasters: 403000,
        paymentSource: "CASH_WALLET",
        status: "CONFIRMED",
        ledgerEntryId: "led_frac_3",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      tx.set(aliceDb.doc(`users/${ALICE_UID}/ledger/led_frac_3`), {
        userId: ALICE_UID,
        idempotencyKey: "led_frac_3",
        txType: "FRACTIONAL_GOLD_BUY",
        referenceId: "frac_3",
        karat: 24,
        cashDeltaPiasters: -403000,
        pendingCashDeltaPiasters: 0,
        goldDeltaMg: 650,
        reservedDeltaMg: 0,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 620000,
        notes: "شراء 650 ملليجرام ذهب عيار 24",
        createdAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        cashAvailablePiasters: 380000,
        cashTotalDebitedPiasters: 620000,
        goldConfirmedMg: 1000,
        gold24kMg: 1000,
        totalInvestedPiasters: 620000,
        lastSettlementRef: "led_frac_3",
        updatedAt: serverTimestamp(),
      });
    })
  );

  // Attempt 999 mg physical withdrawal STILL FAILS in rules!
  await assertFails(
    aliceDb.runTransaction(async (tx) => {
      tx.set(aliceDb.doc("withdrawal_requests/w_999"), {
        customerUid: ALICE_UID,
        customerEmail: "alice@example.com",
        karat: 24,
        amountMg: 999,
        deliveryType: "PICKUP_HUB",
        deliveryAddress: "فرع المعادي الرئيسي",
        withdrawalRef: "WD-999",
        status: "PENDING",
        ledgerEntryId: "led_wd_999",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      tx.set(aliceDb.doc(`users/${ALICE_UID}/ledger/led_wd_999`), {
        userId: ALICE_UID,
        idempotencyKey: "led_wd_999",
        txType: "GOLD_RESERVATION",
        referenceId: "w_999",
        karat: 24,
        cashDeltaPiasters: 0,
        pendingCashDeltaPiasters: 0,
        goldDeltaMg: 0,
        reservedDeltaMg: 999,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 620000,
        notes: "test",
        createdAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        goldReservedMg: 999,
        lastSettlementRef: "led_wd_999",
        updatedAt: serverTimestamp(),
      });
    })
  );

  // Withdrawal at exact threshold 1000 mg SUCCEEDS and reserves gold atomically
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      tx.set(aliceDb.doc("withdrawal_requests/w_1000"), {
        customerUid: ALICE_UID,
        customerEmail: "alice@example.com",
        karat: 24,
        amountMg: 1000,
        deliveryType: "PICKUP_HUB",
        deliveryAddress: "فرع المعادي الرئيسي، القاهرة",
        withdrawalRef: "WD-1000",
        status: "PENDING",
        ledgerEntryId: "led_wd_1000",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      tx.set(aliceDb.doc(`users/${ALICE_UID}/ledger/led_wd_1000`), {
        userId: ALICE_UID,
        idempotencyKey: "led_wd_1000",
        txType: "GOLD_RESERVATION",
        referenceId: "w_1000",
        karat: 24,
        cashDeltaPiasters: 0,
        pendingCashDeltaPiasters: 0,
        goldDeltaMg: 0,
        reservedDeltaMg: 1000,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 620000,
        notes: "حجز سحب فعلي 1.000 جم",
        createdAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        goldReservedMg: 1000,
        lastSettlementRef: "led_wd_1000",
        updatedAt: serverTimestamp(),
      });
    })
  );

  // Double-spending the reserved 1000 mg FAILS because goldReservedMg <= goldConfirmedMg
  await assertFails(
    aliceDb.runTransaction(async (tx) => {
      tx.set(aliceDb.doc("withdrawal_requests/w_dup"), {
        customerUid: ALICE_UID,
        customerEmail: "alice@example.com",
        karat: 24,
        amountMg: 1000,
        deliveryType: "PICKUP_HUB",
        deliveryAddress: "فرع المعادي الرئيسي",
        withdrawalRef: "WD-DUP",
        status: "PENDING",
        ledgerEntryId: "led_wd_dup",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      tx.set(aliceDb.doc(`users/${ALICE_UID}/ledger/led_wd_dup`), {
        userId: ALICE_UID,
        idempotencyKey: "led_wd_dup",
        txType: "GOLD_RESERVATION",
        referenceId: "w_dup",
        karat: 24,
        cashDeltaPiasters: 0,
        pendingCashDeltaPiasters: 0,
        goldDeltaMg: 0,
        reservedDeltaMg: 1000,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 620000,
        notes: "duplicate",
        createdAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        goldReservedMg: 2000,
        lastSettlementRef: "led_wd_dup",
        updatedAt: serverTimestamp(),
      });
    })
  );

  const walletSnap = await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`).get());
  assert.equal(walletSnap.data().goldConfirmedMg, 1000);
  assert.equal(walletSnap.data().goldReservedMg, 1000);
});

test("Deposit requests, admin approval, withdrawal fulfillment/rejection, and client query filters", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const merchantDb = testEnv.authenticatedContext(MERCHANT_UID).firestore();
  const admDb = adminContext(true).firestore();

  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`).set(emptyWalletPayload(ALICE_UID)));

  // 1. Customer cannot submit already-APPROVED deposit request
  await assertFails(
    aliceDb.doc("deposit_requests/dep_bad").set({
      customerUid: ALICE_UID,
      customerEmail: "alice@example.com",
      amountPiasters: 500000,
      channel: "INSTAPAY",
      paymentReference: "IP-999",
      status: "APPROVED",
      ledgerEntryId: "led_dep_bad",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // 2. Customer submits PENDING deposit request -> increments cashPendingPiasters only
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      tx.set(aliceDb.doc("deposit_requests/dep_1"), {
        customerUid: ALICE_UID,
        customerEmail: "alice@example.com",
        amountPiasters: 500000,
        channel: "INSTAPAY",
        paymentReference: "IP-1001",
        status: "PENDING",
        ledgerEntryId: "led_dep_1",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      tx.set(aliceDb.doc(`users/${ALICE_UID}/ledger/led_dep_1`), {
        userId: ALICE_UID,
        idempotencyKey: "led_dep_1",
        txType: "CASH_DEPOSIT_PENDING",
        referenceId: "dep_1",
        karat: 0,
        cashDeltaPiasters: 0,
        pendingCashDeltaPiasters: 500000,
        goldDeltaMg: 0,
        reservedDeltaMg: 0,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 0,
        notes: "طلب إيداع إنستاباي رقم IP-1001",
        createdAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        cashPendingPiasters: 500000,
        lastSettlementRef: "led_dep_1",
        updatedAt: serverTimestamp(),
      });
    })
  );

  // 3. Admin approves deposit atomically -> moves cashPending -> cashAvailable
  await assertSucceeds(
    admDb.runTransaction(async (tx) => {
      tx.update(admDb.doc("deposit_requests/dep_1"), {
        status: "APPROVED",
        adminUid: ADMIN_UID,
        adminNote: "تمت مطابقة التحويل البنكي",
        updatedAt: serverTimestamp(),
      });
      tx.set(admDb.doc(`users/${ALICE_UID}/ledger/led_dep_ok_1`), {
        userId: ALICE_UID,
        idempotencyKey: "led_dep_ok_1",
        txType: "CASH_DEPOSIT",
        referenceId: "dep_1",
        karat: 0,
        cashDeltaPiasters: 500000,
        pendingCashDeltaPiasters: -500000,
        goldDeltaMg: 0,
        reservedDeltaMg: 0,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 0,
        notes: "اعتماد إيداع إنستاباي رقم IP-1001",
        createdAt: serverTimestamp(),
      });
      tx.update(admDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        cashAvailablePiasters: 500000,
        cashPendingPiasters: 0,
        cashTotalCreditedPiasters: 500000,
        lastSettlementRef: "led_dep_ok_1",
        updatedAt: serverTimestamp(),
      });
    })
  );

  // 4. Client query alignment checks (matching Kotlin repository queries)
  await assertSucceeds(aliceDb.collection("merchants").where("status", "==", "APPROVED").get());
  await assertFails(aliceDb.collection("merchants").get()); // unfiltered denied
  await assertSucceeds(merchantDb.collection("merchants").where("ownerUid", "==", MERCHANT_UID).get());
  await assertSucceeds(aliceDb.collection("products").where("status", "==", "ACTIVE").get());
  await assertFails(aliceDb.collection("products").get()); // unfiltered denied
  await assertSucceeds(aliceDb.collection("sell_requests").where("customerUid", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("sell_requests").get()); // unfiltered denied
  await assertSucceeds(aliceDb.collection("withdrawal_requests").where("customerUid", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("deposit_requests").where("customerUid", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("fractional_orders").where("customerUid", "==", ALICE_UID).get());
  await assertSucceeds(merchantDb.collection("merchant_orders").where("merchantOwnerUid", "==", MERCHANT_UID).get());
  await assertSucceeds(admDb.collection("merchants").get());
  await assertSucceeds(admDb.collection("sell_requests").get());
  await assertSucceeds(admDb.collection("withdrawal_requests").get());
  await assertSucceeds(admDb.collection("deposit_requests").get());
});

test("Atomic MARKETPLACE_ORDER_PAID checkout debits cash wallet, creates merchant order, decrements stock, and blocks over-spend or forgery", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const merchantDb = testEnv.authenticatedContext(MERCHANT_UID).firestore();
  const admDb = adminContext(true).firestore();

  // Seed approved merchant & active product (price = 620,000 piasters, stock = 2)
  await assertSucceeds(merchantDb.doc("merchants/m_pay").set(validMerchantPayload(MERCHANT_UID)));
  await assertSucceeds(
    admDb.doc("merchants/m_pay").update({
      status: "APPROVED",
      prepaidMakingApproved: true,
      prepaidMakingBudgetPiasters: 1000000,
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    merchantDb.doc("products/prod_pay").set(
      validProductPayload("m_pay", MERCHANT_UID, {
        sellingPricePiasters: 620000,
        stockQuantity: 2,
      })
    )
  );
  await assertSucceeds(
    admDb.doc("products/prod_pay").update({ status: "ACTIVE", updatedAt: serverTimestamp() })
  );

  // Initialize Alice wallet with 1,000,000 piasters (10,000 EGP) via Admin
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`).set(emptyWalletPayload(ALICE_UID)));
  await assertSucceeds(
    admDb.doc(`users/${ALICE_UID}/wallet/summary`).update({
      cashAvailablePiasters: 1000000,
      cashTotalCreditedPiasters: 1000000,
      lastSettlementRef: "adm_fund_mkt",
      updatedAt: serverTimestamp(),
    })
  );

  // Forged order where ledger cashDelta doesn't match order totalPiasters (-1 vs 620000) FAILS
  await assertFails(
    aliceDb.runTransaction(async (tx) => {
      tx.set(
        aliceDb.doc(`users/${ALICE_UID}/orders/ORD-BAD-1`),
        validOrderPayload(ALICE_UID, {
          shopId: "m_pay",
          shopName: "مجوهرات الشريف",
          merchantOwnerUid: MERCHANT_UID,
          totalPiasters: 620000,
          paymentSource: "CASH_WALLET",
          idempotencyKey: "led_mkt_bad",
        })
      );
      tx.set(aliceDb.doc(`users/${ALICE_UID}/ledger/led_mkt_bad`), {
        userId: ALICE_UID,
        idempotencyKey: "led_mkt_bad",
        txType: "MARKETPLACE_ORDER_PAID",
        referenceId: "ORD-BAD-1",
        karat: 0,
        cashDeltaPiasters: -1, // mismatch!
        pendingCashDeltaPiasters: 0,
        goldDeltaMg: 0,
        reservedDeltaMg: 0,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 0,
        notes: "forged debit",
        createdAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        cashAvailablePiasters: 999999,
        cashTotalDebitedPiasters: 1,
        lastSettlementRef: "led_mkt_bad",
        updatedAt: serverTimestamp(),
      });
    })
  );

  // Valid atomic wallet purchase of 1 piece (620,000 piasters, zero making/service fees)
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      const orderId = "ORD-MKT-101";
      const ledgerId = "led_mkt_101";
      tx.set(
        aliceDb.doc(`users/${ALICE_UID}/orders/${orderId}`),
        validOrderPayload(ALICE_UID, {
          shopId: "m_pay",
          shopName: "مجوهرات الشريف",
          merchantOwnerUid: MERCHANT_UID,
          goldValue: 6200.0,
          makingFee: 0.0,
          serviceFee: 0.0,
          totalPiasters: 620000,
          paymentSource: "CASH_WALLET",
          idempotencyKey: ledgerId,
          status: "PAID",
        })
      );
      tx.set(aliceDb.doc(`merchant_orders/${orderId}`), {
        orderId,
        merchantId: "m_pay",
        merchantOwnerUid: MERCHANT_UID,
        buyerUid: ALICE_UID,
        items: [
          {
            productId: "prod_pay",
            shopId: "m_pay",
            name: "سبيكة عيار 24 وزن 10 جم",
            category: "جنيهات وسبائك",
            karat: 24,
            weightGrams: 10.0,
            stamped: true,
            quantity: 1,
          },
        ],
        totalPiasters: 620000,
        pickupWindow: "اليوم · 4:00–8:00 م",
        status: "PAID",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc("products/prod_pay"), {
        stockQuantity: 1,
        lastOrderId: orderId,
        updatedAt: serverTimestamp(),
      });
      tx.set(aliceDb.doc(`users/${ALICE_UID}/ledger/${ledgerId}`), {
        userId: ALICE_UID,
        idempotencyKey: ledgerId,
        txType: "MARKETPLACE_ORDER_PAID",
        referenceId: orderId,
        karat: 0,
        cashDeltaPiasters: -620000,
        pendingCashDeltaPiasters: 0,
        goldDeltaMg: 0,
        reservedDeltaMg: 0,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 0,
        notes: "سداد طلب متجر من المحفظة النقدية",
        createdAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        cashAvailablePiasters: 380000,
        cashTotalDebitedPiasters: 620000,
        lastSettlementRef: ledgerId,
        updatedAt: serverTimestamp(),
      });
    })
  );

  // Attempting second purchase of 620,000 when only 380,000 remains FAILS
  await assertFails(
    aliceDb.runTransaction(async (tx) => {
      const orderId = "ORD-MKT-102";
      const ledgerId = "led_mkt_102";
      tx.set(
        aliceDb.doc(`users/${ALICE_UID}/orders/${orderId}`),
        validOrderPayload(ALICE_UID, {
          shopId: "m_pay",
          shopName: "مجوهرات الشريف",
          merchantOwnerUid: MERCHANT_UID,
          goldValue: 6200.0,
          totalPiasters: 620000,
          paymentSource: "CASH_WALLET",
          idempotencyKey: ledgerId,
          status: "PAID",
        })
      );
      tx.set(aliceDb.doc(`users/${ALICE_UID}/ledger/${ledgerId}`), {
        userId: ALICE_UID,
        idempotencyKey: ledgerId,
        txType: "MARKETPLACE_ORDER_PAID",
        referenceId: orderId,
        karat: 0,
        cashDeltaPiasters: -620000,
        pendingCashDeltaPiasters: 0,
        goldDeltaMg: 0,
        reservedDeltaMg: 0,
        pendingGoldDeltaMg: 0,
        unitPricePerGramPiasters: 0,
        notes: "over-spend",
        createdAt: serverTimestamp(),
      });
      tx.update(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`), {
        cashAvailablePiasters: -240000,
        cashTotalDebitedPiasters: 1240000,
        lastSettlementRef: ledgerId,
        updatedAt: serverTimestamp(),
      });
    })
  );

  const walletAfter = await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/wallet/summary`).get());
  assert.equal(walletAfter.data().cashAvailablePiasters, 380000);
  const prodAfter = await assertSucceeds(aliceDb.doc("products/prod_pay").get());
  assert.equal(prodAfter.data().stockQuantity, 1);
});


