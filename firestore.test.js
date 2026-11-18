const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot list, read, write, or delete transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const txCol = unauthDb.collection(`users/${ALICE_UID}/transactions`);

  await assertFails(txCol.get());
  await assertFails(txCol.doc("tx_1").get());
  await assertFails(
    txCol.doc("tx_1").set({
      userId: ALICE_UID,
      type: "INCOME",
      amount: 15000.0,
      category: "SALARY",
      description: "Monthly salary",
      transactionDateMillis: Date.now(),
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(txCol.doc("tx_1").delete());
});

test("Authenticated owner can create, read, list, update, and delete valid income and expense transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txCol = aliceDb.collection(`users/${ALICE_UID}/transactions`);
  const created = new Date(Date.now() - 5000);
  const txMillis = Date.now();

  // Create income (mirrors TransactionRepository.addTransaction)
  await assertSucceeds(
    txCol.doc("tx_income_1").set({
      userId: ALICE_UID,
      type: "INCOME",
      amount: 25000.5,
      category: "SALARY",
      description: "Monthly salary",
      transactionDateMillis: txMillis,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create expense (mirrors TransactionRepository.addTransaction)
  await assertSucceeds(
    txCol.doc("tx_expense_1").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 450,
      category: "GROCERIES",
      description: "",
      transactionDateMillis: txMillis,
      createdAt: created,
      updatedAt: created,
    })
  );

  // List & order query (mirrors TransactionRepository.observeTransactions)
  await assertSucceeds(
    txCol.orderBy("transactionDateMillis", "desc").get()
  );

  // Partial update (mirrors TransactionRepository.updateTransaction)
  await assertSucceeds(
    txCol.doc("tx_expense_1").update({
      type: "EXPENSE",
      amount: 520.0,
      category: "FOOD",
      description: "Lunch with colleagues",
      transactionDateMillis: txMillis,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Delete transaction
  await assertSucceeds(txCol.doc("tx_income_1").delete());
});

test("Cross-user isolation: Bob cannot read, list, write, update, or delete Alice transactions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection(`users/${ALICE_UID}/transactions`)
      .doc("alice_tx_1")
      .set({
        userId: ALICE_UID,
        type: "EXPENSE",
        amount: 300.0,
        category: "TRANSPORT",
        description: "CNG ride",
        transactionDateMillis: Date.now(),
        createdAt: new Date(Date.now() - 5000),
        updatedAt: new Date(Date.now() - 5000),
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceTxCol = bobDb.collection(`users/${ALICE_UID}/transactions`);

  await assertFails(aliceTxCol.get());
  await assertFails(aliceTxCol.doc("alice_tx_1").get());
  await assertFails(
    aliceTxCol.doc("alice_tx_1").update({
      amount: 1.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceTxCol.doc("bob_spoof").set({
      userId: BOB_UID,
      type: "INCOME",
      amount: 100.0,
      category: "SALARY",
      description: "Spoof",
      transactionDateMillis: Date.now(),
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceTxCol.doc("alice_tx_1").delete());
});

test("Validation bounds reject ghost fields, @DocumentId 'id' field, negative amounts, immutable field mutations, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txCol = aliceDb.collection(`users/${ALICE_UID}/transactions`);
  const validPast = new Date(Date.now() - 5000);

  // Reject @DocumentId 'id' key inside document body
  await assertFails(
    txCol.doc("tx_bad_id").set({
      id: "tx_bad_id",
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 100.0,
      category: "FOOD",
      description: "Dinner",
      transactionDateMillis: Date.now(),
      createdAt: validPast,
      updatedAt: validPast,
    })
  );

  // Reject ghost field
  await assertFails(
    txCol.doc("tx_ghost").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 100.0,
      category: "FOOD",
      description: "Dinner",
      transactionDateMillis: Date.now(),
      createdAt: validPast,
      updatedAt: validPast,
      isAdmin: true,
    })
  );

  // Reject non-positive amount
  await assertFails(
    txCol.doc("tx_zero").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 0.0,
      category: "FOOD",
      description: "Zero",
      transactionDateMillis: Date.now(),
      createdAt: validPast,
      updatedAt: validPast,
    })
  );

  // Reject invalid type
  await assertFails(
    txCol.doc("tx_type").set({
      userId: ALICE_UID,
      type: "TRANSFER",
      amount: 100.0,
      category: "FOOD",
      description: "Invalid type",
      transactionDateMillis: Date.now(),
      createdAt: validPast,
      updatedAt: validPast,
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    txCol.doc("tx_future").set({
      userId: ALICE_UID,
      type: "INCOME",
      amount: 500.0,
      category: "SALARY",
      description: "Future",
      transactionDateMillis: Date.now(),
      createdAt: new Date(Date.now() + 60000),
      updatedAt: validPast,
    })
  );

  // Create valid doc, then verify immutable createdAt/userId cannot be mutated
  await assertSucceeds(
    txCol.doc("tx_valid").set({
      userId: ALICE_UID,
      type: "INCOME",
      amount: 1000.0,
      category: "SALARY",
      description: "Bonus",
      transactionDateMillis: Date.now(),
      createdAt: validPast,
      updatedAt: validPast,
    })
  );

  await assertFails(
    txCol.doc("tx_valid").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    txCol.doc("tx_valid").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
