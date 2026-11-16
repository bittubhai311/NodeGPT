package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.signOut
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.NovaPresets
import com.example.data.model.UserSettings
import com.example.data.repository.NovaGptRepository
import com.example.ui.components.FormattedMessageContent
import com.example.ui.components.SettingsAndPersonaSheet
import com.example.ui.components.ShimmerDarkBackground
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanShimmer
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElectricIndigoSoft
import com.example.ui.theme.ElevatedSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.NightSlateSurface
import com.example.ui.theme.ObsidianBackground
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.DataUiState
import com.example.viewmodel.InferenceState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun NovaGptAppShell(
    currentUserId: String
) {
    val viewModel: ChatViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ChatViewModel(NovaGptRepository(db), currentUserId)
            }
        }
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val threadsState by viewModel.threadsState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val selectedThreadId by viewModel.selectedThreadId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val inferenceState by viewModel.inferenceState.collectAsStateWithLifecycle()
    val attachedBitmap by viewModel.attachedBitmap.collectAsStateWithLifecycle()
    val attachedImageLabel by viewModel.attachedImageLabel.collectAsStateWithLifecycle()
    val lastCompletedReplyForTts by viewModel.lastCompletedReplyForTts.collectAsStateWithLifecycle()

    var promptInput by rememberSaveable { mutableStateOf("") }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var speakingMessageId by remember { mutableStateOf<String?>(null) }

    // TextToSpeech lifecycle
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.getDefault()
            }
        }
        ttsEngine = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    // Auto-speak completed reply if enabled in settings
    LaunchedEffect(lastCompletedReplyForTts) {
        val textToSpeak = lastCompletedReplyForTts
        if (!textToSpeak.isNullOrBlank()) {
            val cleanSpeech = textToSpeak.replace(Regex("```[\\s\\S]*?```"), " code block ")
                .replace("*", "")
                .replace("#", "")
            ttsEngine?.speak(cleanSpeech.take(3500), TextToSpeech.QUEUE_FLUSH, null, "auto_tts")
            viewModel.clearTtsEvent()
        }
    }

    // BackHandler when drawer is open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // Photo Picker launcher (Zero-Permission Google Play compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = decodeUriToScaledBitmap(context, uri)
            if (bitmap != null) {
                viewModel.attachImage(bitmap, "Gallery Image")
            } else {
                Toast.makeText(context, "Could not load selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera capture preview launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.attachImage(bitmap, "Camera Snapshot")
        }
    }

    // Voice recognition launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                promptInput = if (promptInput.isBlank()) spoken else "$promptInput $spoken"
            }
        }
    }

    val isStreaming = inferenceState is InferenceState.Loading || inferenceState is InferenceState.Streaming

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = NightSlateSurface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(310.dp)
            ) {
                DrawerContent(
                    threadsState = threadsState,
                    selectedThreadId = selectedThreadId,
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onNewChat = {
                        viewModel.startNewChat()
                        scope.launch { drawerState.close() }
                    },
                    onSelectThread = { id ->
                        viewModel.selectThread(id)
                        scope.launch { drawerState.close() }
                    },
                    onDeleteThread = viewModel::deleteThread,
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        showSettingsSheet = true
                    },
                    onSignOut = {
                        ttsEngine?.stop()
                        signOut(credentialManager, scope)
                    }
                )
            }
        }
    ) {
        ShimmerDarkBackground(isStreaming = isStreaming) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    NovaTopBar(
                        settings = settings,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNewChat = viewModel::startNewChat,
                        onOpenSettings = { showSettingsSheet = true }
                    )
                },
                bottomBar = {
                    MultimodalComposerBar(
                        promptInput = promptInput,
                        onPromptChange = { promptInput = it },
                        attachedBitmap = attachedBitmap,
                        attachedImageLabel = attachedImageLabel,
                        isStreaming = isStreaming,
                        onRemoveAttachment = { viewModel.attachImage(null) },
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onCaptureCamera = {
                            try {
                                cameraLauncher.launch(null)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Camera unavailable", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onStartVoiceInput = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your prompt for NovaGPT…")
                                }
                                voiceLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Voice input is not supported on this device",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onSendMessage = {
                            viewModel.sendMessage(promptInput)
                            promptInput = ""
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Quick Persona Switcher Row
                    PersonaQuickBar(
                        selectedPersonaId = settings.selectedPersonaId,
                        onSelectPersona = viewModel::selectQuickPersona,
                        onOpenCustomPrompt = { showSettingsSheet = true }
                    )

                    val messages = (messagesState as? DataUiState.Success)?.data.orEmpty()

                    if (messages.isEmpty() && inferenceState is InferenceState.Idle) {
                        EmptyChatWelcome(
                            settings = settings,
                            onSelectStarter = { starter ->
                                if (starter.personaId != settings.selectedPersonaId) {
                                    viewModel.selectQuickPersona(starter.personaId)
                                }
                                viewModel.sendMessage(starter.prompt)
                            },
                            onPickPhoto = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onOpenSettings = { showSettingsSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        ConversationList(
                            messages = messages,
                            inferenceState = inferenceState,
                            speakingMessageId = speakingMessageId,
                            onToggleSpeak = { msg ->
                                if (speakingMessageId == msg.id) {
                                    ttsEngine?.stop()
                                    speakingMessageId = null
                                } else {
                                    speakingMessageId = msg.id
                                    val cleanSpeech = msg.content
                                        .replace(Regex("```[\\s\\S]*?```"), " code block ")
                                        .replace("*", "")
                                        .replace("#", "")
                                    ttsEngine?.speak(
                                        cleanSpeech.take(3500),
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        msg.id
                                    )
                                }
                            },
                            onRegenerate = viewModel::regenerateLastResponse,
                            onDismissError = viewModel::clearError,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        SettingsAndPersonaSheet(
            currentSettings = settings,
            onSaveSettings = viewModel::saveSettings,
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@Composable
private fun NovaTopBar(
    settings: UserSettings,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val activeModel = NovaPresets.modelById(settings.selectedModel)
    val activePersona = NovaPresets.personaById(settings.selectedPersonaId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier.testTag("drawer_menu_button")
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open chat history drawer",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        // Center Model + Connection Pill
        Surface(
            color = GlassCardSurface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onOpenSettings() }
                .testTag("open_settings_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (!settings.useCustomApiKey) EmeraldSuccess else CyanShimmer)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${activeModel.displayName} • ${activePersona.name}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configure model and settings",
                    tint = CyanShimmer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNewChat,
                modifier = Modifier.testTag("top_new_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_chat),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun PersonaQuickBar(
    selectedPersonaId: String,
    onSelectPersona: (String) -> Unit,
    onOpenCustomPrompt: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NovaPresets.personas.forEach { persona ->
            val isSelected = persona.id == selectedPersonaId
            Surface(
                color = if (isSelected) ElectricIndigo.copy(alpha = 0.24f) else GlassCardSurface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) ElectricIndigo else BorderSubtle
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onSelectPersona(persona.id) }
                    .testTag("quick_persona_${persona.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanShimmer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = persona.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Surface(
            color = GlassCardSurface.copy(alpha = 0.7f),
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onOpenCustomPrompt() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = CyanShimmer,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "System Prompt & API Key",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanShimmer
                )
            }
        }
    }
}

@Composable
private fun EmptyChatWelcome(
    settings: UserSettings,
    onSelectStarter: (com.example.data.model.StarterPrompt) -> Unit,
    onPickPhoto: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePersona = NovaPresets.personaById(settings.selectedPersonaId)

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(listOf(ElectricIndigo, CyanShimmer)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_nova_orb_1794588646979),
                        contentDescription = "NovaGPT AI Orb",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = if (!settings.useCustomApiKey) EmeraldSuccess.copy(alpha = 0.14f) else ElectricIndigo.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (!settings.useCustomApiKey) EmeraldSuccess.copy(alpha = 0.4f) else CyanShimmer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.clickable { onOpenSettings() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (!settings.useCustomApiKey) EmeraldSuccess else CyanShimmer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (!settings.useCustomApiKey) {
                                "FREE BUILT-IN AI KEY • TEMP ${String.format(Locale.US, "%.1f", settings.temperature)}"
                            } else {
                                "CUSTOM API PROFILE • TEMP ${String.format(Locale.US, "%.1f", settings.temperature)}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "How can I help you today?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${activePersona.name}: ${activePersona.tagline}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Multimodal Vision Quick Action Banner
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                ElectricIndigo.copy(alpha = 0.22f),
                                CyanShimmer.copy(alpha = 0.16f)
                            )
                        )
                    )
                    .border(1.dp, ElectricIndigo.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .clickable { onPickPhoto() }
                    .padding(14.dp)
                    .testTag("vision_quick_card"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricIndigo.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = CyanShimmer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Analyze a Photo or Screenshot",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Upload any diagram, code screenshot, homework, or object for instant Gemini Vision breakdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Starter Prompt Grid Cards
        items(NovaPresets.starterPrompts.size) { index ->
            val starter = NovaPresets.starterPrompts[index]
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassCardSurface.copy(alpha = 0.85f))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .clickable { onSelectStarter(starter) }
                    .padding(15.dp)
                    .testTag("starter_card_$index")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = starter.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = ElevatedSurfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = NovaPresets.personaById(starter.personaId).badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanShimmer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = starter.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConversationList(
    messages: List<ChatMessage>,
    inferenceState: InferenceState,
    speakingMessageId: String?,
    onToggleSpeak: (ChatMessage) -> Unit,
    onRegenerate: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(messages.size, inferenceState) {
        val totalItems = messages.size + (if (inferenceState !is InferenceState.Idle && inferenceState !is InferenceState.Success) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(messages, key = { it.id.ifEmpty { it.hashCode().toString() } }) { msg ->
            val isUser = msg.role == "user"
            val isLastModelMessage = !isUser && messages.lastOrNull()?.id == msg.id

            if (isUser) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = 18.dp,
                                    bottomEnd = 4.dp
                                )
                            )
                            .background(
                                Brush.linearGradient(
                                    listOf(ElectricIndigo, IndigoContainer)
                                )
                            )
                            .border(
                                1.dp,
                                ElectricIndigoSoft.copy(alpha = 0.35f),
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = 18.dp,
                                    bottomEnd = 4.dp
                                )
                            )
                            .padding(14.dp)
                    ) {
                        if (msg.hasImage) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = CyanShimmer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = msg.imageLabel.ifBlank { "Attached Photo" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanShimmer
                                    )
                                }
                            }
                        }
                        Text(
                            text = msg.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Assistant message card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 580.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(GlassCardSurface.copy(alpha = 0.92f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanShimmer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NovaGPT",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanShimmer
                        )
                    }

                    FormattedMessageContent(
                        content = msg.content,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))

                    // Action row: Speak, Copy, Share, Regenerate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isSpeakingThis = speakingMessageId == msg.id
                        IconButton(
                            onClick = { onToggleSpeak(msg) },
                            modifier = Modifier.testTag("tts_button_${msg.id}")
                        ) {
                            Icon(
                                imageVector = if (isSpeakingThis) Icons.Default.Stop else Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = if (isSpeakingThis) {
                                    stringResource(R.string.stop_speaking)
                                } else {
                                    stringResource(R.string.speak_response)
                                },
                                tint = if (isSpeakingThis) CyanShimmer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(msg.content))
                                Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_msg_${msg.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.copy_text),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg.content)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share NovaGPT Reply"))
                            },
                            modifier = Modifier.testTag("share_msg_${msg.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share response",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (isLastModelMessage) {
                            IconButton(
                                onClick = onRegenerate,
                                modifier = Modifier.testTag("regenerate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.regenerate_response),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Streaming / Loading / Error cards
        when (inferenceState) {
            is InferenceState.Loading -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassCardSurface.copy(alpha = 0.85f))
                            .border(1.dp, ElectricIndigo.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CyanShimmer,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "NovaGPT is thinking…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is InferenceState.Streaming -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(GlassCardSurface)
                            .border(1.dp, CyanShimmer.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = CyanShimmer,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NovaGPT • Streaming live…",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyanShimmer
                            )
                        }
                        FormattedMessageContent(
                            content = inferenceState.partialText,
                            textColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            is InferenceState.Error -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = inferenceState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onRegenerate,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                                modifier = Modifier.testTag("retry_inference_button")
                            ) {
                                Text("Retry")
                            }
                            Button(
                                onClick = onDismissError,
                                colors = ButtonDefaults.buttonColors(containerColor = ElevatedSurfaceVariant)
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun MultimodalComposerBar(
    promptInput: String,
    onPromptChange: (String) -> Unit,
    attachedBitmap: Bitmap?,
    attachedImageLabel: String,
    isStreaming: Boolean,
    onRemoveAttachment: () -> Unit,
    onPickPhoto: () -> Unit,
    onCaptureCamera: () -> Unit,
    onStartVoiceInput: () -> Unit,
    onSendMessage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Attached image preview pill
        AnimatedVisibility(visible = attachedBitmap != null) {
            if (attachedBitmap != null) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassCardSurface)
                        .border(1.dp, CyanShimmer.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = attachedBitmap.asImageBitmap(),
                        contentDescription = "Attached preview",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = attachedImageLabel.ifBlank { "Attached Photo" },
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanShimmer
                        )
                        Text(
                            text = "Ready for Gemini Vision analysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onRemoveAttachment,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove attached photo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Composer Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(GlassCardSurface.copy(alpha = 0.96f))
                .border(1.dp, BorderSubtle, RoundedCornerShape(26.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPickPhoto,
                modifier = Modifier.testTag("attach_photo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = stringResource(R.string.attach_image),
                    tint = CyanShimmer
                )
            }

            IconButton(
                onClick = onCaptureCamera,
                modifier = Modifier.testTag("camera_capture_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture photo with camera",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = promptInput,
                onValueChange = onPromptChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.prompt_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_prompt_input")
            )

            IconButton(
                onClick = onStartVoiceInput,
                modifier = Modifier.testTag("voice_input_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = stringResource(R.string.voice_input),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val canSend = !isStreaming && (promptInput.isNotBlank() || attachedBitmap != null)
            IconButton(
                onClick = onSendMessage,
                enabled = canSend,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) {
                            Brush.linearGradient(listOf(ElectricIndigo, CyanShimmer))
                        } else {
                            Brush.linearGradient(listOf(ElevatedSurfaceVariant, ElevatedSurfaceVariant))
                        }
                    )
                    .testTag("send_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send_message),
                    tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DrawerContent(
    threadsState: DataUiState<List<ChatThread>>,
    selectedThreadId: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNewChat: () -> Unit,
    onSelectThread: (String) -> Unit,
    onDeleteThread: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit
) {
    val user = Firebase.auth.currentUser

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        // Brand Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CyanShimmer,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NovaGPT Cloud",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Button(
                onClick = onNewChat,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("new_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.new_chat),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search threads input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.search_chats_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricIndigo,
                unfocusedBorderColor = BorderSubtle,
                focusedContainerColor = ObsidianBackground,
                unfocusedContainerColor = ObsidianBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_chats_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.chat_history).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Threads list
        Box(modifier = Modifier.weight(1f)) {
            when (threadsState) {
                is DataUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyanShimmer, strokeWidth = 2.dp)
                    }
                }
                is DataUiState.Error -> {
                    Text(
                        text = threadsState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is DataUiState.Success -> {
                    val filtered = remember(threadsState.data, searchQuery) {
                        if (searchQuery.isBlank()) {
                            threadsState.data
                        } else {
                            threadsState.data.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.lastMessagePreview.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No conversations yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start a chat to automatically sync your history to the cloud.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered, key = { it.id }) { thread ->
                                val isSelected = thread.id == selectedThreadId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) ElectricIndigo.copy(alpha = 0.2f) else GlassCardSurface
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) ElectricIndigo else BorderSubtle,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onSelectThread(thread.id) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("thread_item_${thread.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = thread.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (thread.lastMessagePreview.isNotBlank()) {
                                            Text(
                                                text = thread.lastMessagePreview,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteThread(thread.id) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_thread_${thread.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = stringResource(R.string.delete_thread),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))

        // Settings & User Account Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassCardSurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .clickable { onOpenSettings() }
                .padding(12.dp)
                .testTag("drawer_settings_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = CyanShimmer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Free API Key, Models & Personas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.displayName?.ifBlank { "Google Account" } ?: "Signed In User",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user?.email ?: "Cloud Sync Active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(R.string.sign_out),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun decodeUriToScaledBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val maxDim = 1024
        val width = rawBitmap.width
        val height = rawBitmap.height
        if (width <= maxDim && height <= maxDim) {
            rawBitmap
        } else {
            val ratio = width.toFloat() / height.toFloat()
            val targetWidth = if (ratio > 1f) maxDim else (maxDim * ratio).toInt().coerceAtLeast(1)
            val targetHeight = if (ratio > 1f) (maxDim / ratio).toInt().coerceAtLeast(1) else maxDim
            Bitmap.createScaledBitmap(rawBitmap, targetWidth, targetHeight, true)
        }
    } catch (e: Exception) {
        null
    }
}
