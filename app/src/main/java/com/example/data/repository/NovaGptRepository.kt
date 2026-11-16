package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.UserSettings
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NovaGptRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeThreads(userId: String = requireUserId()): Flow<List<ChatThread>> = flow {
        val path = "users/$userId/threads"
        emitAll(
            db.collection("users").document(userId).collection("threads")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatThread::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMessages(threadId: String, userId: String = requireUserId()): Flow<List<ChatMessage>> = flow {
        val path = "users/$userId/threads/$threadId/messages"
        emitAll(
            db.collection("users").document(userId)
                .collection("threads").document(threadId)
                .collection("messages")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeUserSettings(userId: String = requireUserId()): Flow<UserSettings> = flow {
        val path = "users/$userId/settings/preferences"
        emitAll(
            db.collection("users").document(userId)
                .collection("settings").document("preferences")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: UserSettings(userId = userId)
                    } else {
                        UserSettings(userId = userId)
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createThread(
        title: String,
        personaId: String,
        modelName: String,
        lastMessagePreview: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("threads").document()
        val safeTitle = title.trim().ifBlank { "New Conversation" }.take(150)
        val payload = mapOf(
            "userId" to uid,
            "title" to safeTitle,
            "personaId" to personaId.take(64),
            "modelName" to modelName.take(64),
            "lastMessagePreview" to lastMessagePreview.take(450),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getThreadById(ownerUid: String, threadId: String): Result<ChatThread?> = runCatching {
        val docRef = db.collection("users").document(ownerUid).collection("threads").document(threadId)
        try {
            docRef.get().await().toObject(
                ChatThread::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateThreadMetadata(
        threadId: String,
        title: String,
        personaId: String,
        modelName: String,
        lastMessagePreview: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("threads").document(threadId)
        val updates = mapOf(
            "title" to title.trim().ifBlank { "Conversation" }.take(150),
            "personaId" to personaId.take(64),
            "modelName" to modelName.take(64),
            "lastMessagePreview" to lastMessagePreview.take(450),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteThread(threadId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val threadRef = db.collection("users").document(uid).collection("threads").document(threadId)
        try {
            val msgsSnapshot = threadRef.collection("messages").get().await()
            for (msgDoc in msgsSnapshot.documents) {
                msgDoc.reference.delete().await()
            }
            threadRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, threadRef.path)
            throw e
        }
    }

    suspend fun addMessage(
        threadId: String,
        role: String,
        content: String,
        hasImage: Boolean = false,
        imageLabel: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val msgRef = db.collection("users").document(uid)
            .collection("threads").document(threadId)
            .collection("messages").document()
        val safeContent = content.ifBlank { "..." }.take(29000)
        val payload = mapOf(
            "userId" to uid,
            "threadId" to threadId,
            "role" to role,
            "content" to safeContent,
            "hasImage" to hasImage,
            "imageLabel" to imageLabel.take(190),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            msgRef.set(payload).await()
            msgRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    suspend fun saveUserSettings(settings: UserSettings): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("settings").document("preferences")
        val payload = mapOf(
            "userId" to uid,
            "selectedModel" to settings.selectedModel.take(64),
            "selectedPersonaId" to settings.selectedPersonaId.take(64),
            "customSystemPrompt" to settings.customSystemPrompt.take(3900),
            "temperature" to settings.temperature.coerceIn(0.0, 2.0),
            "useCustomApiKey" to settings.useCustomApiKey,
            "customApiKeyHint" to settings.customApiKeyHint.take(240),
            "autoSpeakResponses" to settings.autoSpeakResponses,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }
}
