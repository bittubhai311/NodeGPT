package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NovaGptRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createThreadAndAddMessage_authenticatedOwner_succeedsAndEmitsInFlows() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NovaGptRepository(firestore)

        val threadId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createThread(
                title = "Compose Shimmer Help",
                personaId = "coder",
                modelName = "gemini-3.8-flash",
                lastMessagePreview = "How do I animate a radial gradient?"
            ).getOrThrow()
        }
        assertTrue(threadId.isNotBlank())

        val messageId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addMessage(
                threadId = threadId,
                role = "user",
                content = "How do I animate a radial gradient in Compose?"
            ).getOrThrow()
        }
        assertTrue(messageId.isNotBlank())

        val threads = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeThreads(aliceUid).first { list -> list.any { it.id == threadId } }
        }
        assertTrue(threads.any { it.id == threadId })

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages(threadId, aliceUid).first { list -> list.any { it.id == messageId } }
        }
        assertTrue(messages.any { it.id == messageId })
    }

    @Test
    fun saveUserSettings_authenticatedOwner_persistsPreferences() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = NovaGptRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserSettings(
                UserSettings(
                    userId = aliceUid,
                    selectedModel = "gemini-3.5-flash-lite",
                    selectedPersonaId = "hinglish",
                    customSystemPrompt = "Speak in friendly Hinglish",
                    temperature = 0.85,
                    useCustomApiKey = false,
                    customApiKeyHint = "",
                    autoSpeakResponses = true
                )
            )
        }
        assertTrue(saveResult.isSuccess)

        val observedSettings = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserSettings(aliceUid).first { it.selectedPersonaId == "hinglish" }
        }
        assertEquals("gemini-3.5-flash-lite", observedSettings.selectedModel)
        assertTrue(observedSettings.autoSpeakResponses)
    }

    @Test
    fun getThreadById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = NovaGptRepository(firestore)
        val threadId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createThread(
                title = "Alice Private Chat",
                personaId = "assistant",
                modelName = "gemini-3.8-flash"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = NovaGptRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getThreadById(aliceUid, threadId)
        }
        assertTrue(result.isFailure)
        val firestoreException = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeThreads_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = NovaGptRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeThreads(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED exception for unauthenticated user")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@novagpt.test"
        const val BOB_EMAIL = "bob@novagpt.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
