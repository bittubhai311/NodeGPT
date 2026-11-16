package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.NovaPresets
import com.example.data.model.UserSettings
import com.example.data.repository.NovaGptRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "ChatViewModel"

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface InferenceState {
    data object Idle : InferenceState
    data object Loading : InferenceState
    data class Streaming(val partialText: String) : InferenceState
    data class Success(val finalText: String) : InferenceState
    data class Error(val message: String) : InferenceState
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val repository: NovaGptRepository,
    private val currentUserId: String
) : ViewModel() {

    // Default lazy model per Firebase AI Logic guidelines
    private val defaultModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val _selectedThreadId = MutableStateFlow<String?>(null)
    val selectedThreadId: StateFlow<String?> = _selectedThreadId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _inferenceState = MutableStateFlow<InferenceState>(InferenceState.Idle)
    val inferenceState: StateFlow<InferenceState> = _inferenceState.asStateFlow()

    private val _attachedBitmap = MutableStateFlow<Bitmap?>(null)
    val attachedBitmap: StateFlow<Bitmap?> = _attachedBitmap.asStateFlow()

    private val _attachedImageLabel = MutableStateFlow("")
    val attachedImageLabel: StateFlow<String> = _attachedImageLabel.asStateFlow()

    private val _lastCompletedReplyForTts = MutableStateFlow<String?>(null)
    val lastCompletedReplyForTts: StateFlow<String?> = _lastCompletedReplyForTts.asStateFlow()

    // Two-tier Kotlin Flow error handling for threads
    val threadsState: StateFlow<DataUiState<List<ChatThread>>> = repository.observeThreads(currentUserId)
        .map<List<ChatThread>, DataUiState<List<ChatThread>>> { list ->
            if (_selectedThreadId.value == null && list.isNotEmpty()) {
                _selectedThreadId.value = list.first().id
            }
            DataUiState.Success(list)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.e(TAG, "Error observing threads", rootError)
            emit(DataUiState.Error(rootError.message ?: "Failed to load conversations"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    // Two-tier Kotlin Flow error handling for active thread's messages
    val messagesState: StateFlow<DataUiState<List<ChatMessage>>> = _selectedThreadId
        .flatMapLatest { threadId ->
            if (threadId.isNullOrBlank()) {
                flowOf(DataUiState.Success(emptyList()))
            } else {
                repository.observeMessages(threadId, currentUserId)
                    .map<List<ChatMessage>, DataUiState<List<ChatMessage>>> { DataUiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.e(TAG, "Error observing messages", rootError)
                        emit(DataUiState.Error(rootError.message ?: "Failed to load messages"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Success(emptyList())
        )

    // Two-tier Kotlin Flow error handling for user settings
    val settingsState: StateFlow<UserSettings> = repository.observeUserSettings(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.e(TAG, "Error observing user settings", firestoreError ?: error)
            emit(UserSettings(userId = currentUserId))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UserSettings(userId = currentUserId)
        )

    fun selectThread(threadId: String?) {
        _selectedThreadId.value = threadId
        _inferenceState.value = InferenceState.Idle
    }

    fun startNewChat() {
        _selectedThreadId.value = null
        _inferenceState.value = InferenceState.Idle
        _attachedBitmap.value = null
        _attachedImageLabel.value = ""
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun attachImage(bitmap: Bitmap?, label: String = "Attached Photo") {
        _attachedBitmap.value = bitmap
        _attachedImageLabel.value = if (bitmap != null) label else ""
    }

    fun clearTtsEvent() {
        _lastCompletedReplyForTts.value = null
    }

    fun clearError() {
        if (_inferenceState.value is InferenceState.Error) {
            _inferenceState.value = InferenceState.Idle
        }
    }

    fun saveSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            repository.saveUserSettings(newSettings)
        }
    }

    fun selectQuickPersona(personaId: String) {
        val current = settingsState.value
        saveSettings(current.copy(selectedPersonaId = personaId))
    }

    fun deleteThread(threadId: String) {
        viewModelScope.launch {
            if (_selectedThreadId.value == threadId) {
                _selectedThreadId.value = null
            }
            repository.deleteThread(threadId)
        }
    }

    fun sendMessage(promptText: String) {
        val trimmedPrompt = promptText.trim()
        val bitmapToSend = _attachedBitmap.value
        val imageLabel = _attachedImageLabel.value

        if (trimmedPrompt.isEmpty() && bitmapToSend == null) return

        // Clear attachment from composer immediately
        _attachedBitmap.value = null
        _attachedImageLabel.value = ""

        val effectivePrompt = trimmedPrompt.ifEmpty { "Analyze this image in detail and explain what you see." }
        val currentSettings = settingsState.value
        val persona = NovaPresets.personaById(currentSettings.selectedPersonaId)
        val systemInstructionText = currentSettings.customSystemPrompt.ifBlank { persona.systemPrompt }
        val activeModelName = currentSettings.selectedModel.ifBlank { "gemini-3.8-flash" }

        viewModelScope.launch {
            _inferenceState.value = InferenceState.Loading

            try {
                // 1. Ensure active thread exists in Firestore
                var threadId = _selectedThreadId.value
                val isNewThread = threadId.isNullOrBlank()
                val generatedTitle = effectivePrompt
                    .replace("\n", " ")
                    .take(44)
                    .let { if (effectivePrompt.length > 44) "$it…" else it }

                if (isNewThread) {
                    val createResult = repository.createThread(
                        title = generatedTitle,
                        personaId = persona.id,
                        modelName = activeModelName,
                        lastMessagePreview = effectivePrompt.take(120)
                    )
                    threadId = createResult.getOrThrow()
                    _selectedThreadId.value = threadId
                }

                val targetThreadId = checkNotNull(threadId)

                // 2. Save user message to Firestore
                repository.addMessage(
                    threadId = targetThreadId,
                    role = "user",
                    content = effectivePrompt,
                    hasImage = bitmapToSend != null,
                    imageLabel = if (bitmapToSend != null) imageLabel.ifBlank { "Photo attached" } else ""
                ).getOrThrow()

                // 3. Build history context from current messages
                val existingMessages = (messagesState.value as? DataUiState.Success)?.data.orEmpty()
                val recentHistory = existingMessages.takeLast(10)

                val configuredModel = if (
                    activeModelName == "gemini-3.8-flash" &&
                    currentSettings.temperature == 0.7 &&
                    systemInstructionText.isBlank()
                ) {
                    defaultModel
                } else {
                    Firebase.ai.generativeModel(
                        modelName = activeModelName,
                        generationConfig = generationConfig {
                            temperature = currentSettings.temperature.toFloat().coerceIn(0f, 2f)
                        },
                        systemInstruction = content { text(systemInstructionText) }
                    )
                }

                // 4. Stream response with 60s timeout
                val accumulatedText = StringBuilder()
                withTimeout(60_000L) {
                    if (bitmapToSend != null) {
                        val multimodalInput = content {
                            image(bitmapToSend)
                            text(effectivePrompt)
                        }
                        configuredModel.generateContentStream(multimodalInput).collect { chunk ->
                            val chunkText = chunk.text.orEmpty()
                            if (chunkText.isNotEmpty()) {
                                accumulatedText.append(chunkText)
                                _inferenceState.value = InferenceState.Streaming(accumulatedText.toString())
                            }
                        }
                    } else {
                        val historyContents = recentHistory
                            .filter { it.content.isNotBlank() }
                            .map { msg ->
                                content(role = if (msg.role == "model") "model" else "user") {
                                    text(msg.content)
                                }
                            }
                        val chat = configuredModel.startChat(history = historyContents)
                        chat.sendMessageStream(effectivePrompt).collect { chunk ->
                            val chunkText = chunk.text.orEmpty()
                            if (chunkText.isNotEmpty()) {
                                accumulatedText.append(chunkText)
                                _inferenceState.value = InferenceState.Streaming(accumulatedText.toString())
                            }
                        }
                    }
                }

                val finalReply = accumulatedText.toString().trim()
                if (finalReply.isNotEmpty()) {
                    repository.addMessage(
                        threadId = targetThreadId,
                        role = "model",
                        content = finalReply
                    )
                    val activeThread = (threadsState.value as? DataUiState.Success)?.data
                        ?.firstOrNull { it.id == targetThreadId }
                    repository.updateThreadMetadata(
                        threadId = targetThreadId,
                        title = activeThread?.title ?: generatedTitle,
                        personaId = persona.id,
                        modelName = activeModelName,
                        lastMessagePreview = finalReply.replace("\n", " ").take(140)
                    )
                    _inferenceState.value = InferenceState.Success(finalReply)
                    if (currentSettings.autoSpeakResponses) {
                        _lastCompletedReplyForTts.value = finalReply
                    }
                } else {
                    _inferenceState.value = InferenceState.Error("Model returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Streaming generation timed out", e)
                _inferenceState.value = InferenceState.Error(
                    "Generation timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                _inferenceState.value = InferenceState.Error(
                    e.localizedMessage ?: "AI generation failed."
                )
            }
        }
    }

    fun regenerateLastResponse() {
        val currentMessages = (messagesState.value as? DataUiState.Success)?.data.orEmpty()
        val lastUserMessage = currentMessages.lastOrNull { it.role == "user" } ?: return
        sendMessage(lastUserMessage.content)
    }
}
