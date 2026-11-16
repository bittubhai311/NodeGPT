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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot list or read threads", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("threads").get());
});

test("Authenticated user: can create and list own threads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("threads").doc("thread_1").set({
      userId: ALICE_UID,
      title: "Kotlin Coroutines Help",
      personaId: "coder",
      modelName: "gemini-3.8-flash",
      lastMessagePreview: "How does Flow work?",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("threads").get()
  );
});

test("Authenticated user: cannot read or write another user's threads or messages", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("threads").doc("bob_thread").set({
      userId: BOB_UID,
      title: "Bob Private Thread",
      personaId: "assistant",
      modelName: "gemini-3.8-flash",
      lastMessagePreview: "Secret",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("threads").doc("bob_thread").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("threads").get()
  );
});

test("Shadow update test: rejects ghost fields on thread and settings creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("threads").doc("thread_ghost").set({
      userId: ALICE_UID,
      title: "Test",
      personaId: "assistant",
      modelName: "gemini-3.8-flash",
      lastMessagePreview: "Hi",
      createdAt: now,
      updatedAt: now,
      isAdmin: true,
    })
  );
});

test("Authenticated user: can create messages and update user settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("threads").doc("thread_1").collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      threadId: "thread_1",
      role: "user",
      content: "Explain Jetpack Compose animations",
      hasImage: false,
      imageLabel: "",
      createdAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").set({
      userId: ALICE_UID,
      selectedModel: "gemini-3.8-flash",
      selectedPersonaId: "assistant",
      customSystemPrompt: "Be helpful and concise.",
      temperature: 0.7,
      useCustomApiKey: false,
      customApiKeyHint: "",
      autoSpeakResponses: false,
      updatedAt: now,
    })
  );
});
