package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.NovaPresets
import com.example.data.model.UserSettings
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanShimmer
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElevatedSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.NightSlateSurface
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsAndPersonaSheet(
    currentSettings: UserSettings,
    onSaveSettings: (UserSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedModel by remember(currentSettings) { mutableStateOf(currentSettings.selectedModel) }
    var selectedPersonaId by remember(currentSettings) { mutableStateOf(currentSettings.selectedPersonaId) }
    var customSystemPrompt by remember(currentSettings) { mutableStateOf(currentSettings.customSystemPrompt) }
    var temperature by remember(currentSettings) { mutableDoubleStateOf(currentSettings.temperature) }
    var useCustomApiKey by remember(currentSettings) { mutableStateOf(currentSettings.useCustomApiKey) }
    var customApiKeyInput by remember(currentSettings) { mutableStateOf(currentSettings.customApiKeyHint) }
    var showApiKey by remember { mutableStateOf(false) }
    var autoSpeakResponses by remember(currentSettings) { mutableStateOf(currentSettings.autoSpeakResponses) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NightSlateSurface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyanShimmer,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GPT Studio & API Settings",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Configure free AI engine, custom API key vault, persona & parameters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close settings"
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle)

            // 1. AI Connection & Free API Key Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassCardSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = if (!useCustomApiKey) EmeraldSuccess else CyanShimmer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Connection & API Key Mode",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Surface(
                        color = if (!useCustomApiKey) EmeraldSuccess.copy(alpha = 0.16f) else ElectricIndigo.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (!useCustomApiKey) "FREE BUILT-IN AI" else "CUSTOM KEY PROFILE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (!useCustomApiKey) EmeraldSuccess else CyanShimmer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = if (!useCustomApiKey) {
                        "Built-in Free Gemini AI (Firebase AI Logic + App Check) is active. Zero API key setup is required."
                    } else {
                        "Custom API key profile enabled alongside Firebase AI Logic fallback."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Use Custom API Key Label / Override",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = useCustomApiKey,
                        onCheckedChange = { useCustomApiKey = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricIndigo
                        ),
                        modifier = Modifier.testTag("custom_api_key_switch")
                    )
                }

                if (useCustomApiKey) {
                    OutlinedTextField(
                        value = customApiKeyInput,
                        onValueChange = { customApiKeyInput = it },
                        label = { Text("Custom API Key / Profile Tag") },
                        placeholder = { Text("AIzaSy... or leave blank for built-in free AI") },
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showApiKey) "Hide key" else "Show key"
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanShimmer,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_api_key_input")
                    )
                }
            }

            // 2. Model Selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = ElectricIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini Model Engine",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                NovaPresets.models.forEach { model ->
                    val isSelected = selectedModel == model.modelId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) ElectricIndigo.copy(alpha = 0.16f) else GlassCardSurface)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) ElectricIndigo else BorderSubtle,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedModel = model.modelId }
                            .padding(14.dp)
                            .testTag("model_option_${model.modelId}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = CyanShimmer.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = model.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanShimmer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = model.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = CyanShimmer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // 3. AI Persona Presets & Custom System Prompt
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PersonPin,
                        contentDescription = null,
                        tint = CyanShimmer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Persona & System Instructions",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NovaPresets.personas.forEach { persona ->
                        val active = selectedPersonaId == persona.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) ElectricIndigo else ElevatedSurfaceVariant)
                                .border(
                                    width = 1.dp,
                                    color = if (active) CyanShimmer else BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedPersonaId = persona.id
                                    customSystemPrompt = persona.systemPrompt
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("persona_chip_${persona.id}")
                        ) {
                            Column {
                                Text(
                                    text = persona.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = persona.badge,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (active) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customSystemPrompt,
                    onValueChange = { customSystemPrompt = it },
                    label = { Text("Custom System Prompt (Optional override)") },
                    placeholder = {
                        Text(NovaPresets.personaById(selectedPersonaId).systemPrompt)
                    },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricIndigo,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_system_prompt_input")
                )
            }

            // 4. Temperature Slider & Voice Output Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassCardSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = CyanShimmer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Creativity (Temperature)",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.2f", temperature),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanShimmer
                    )
                }

                Slider(
                    value = temperature.toFloat(),
                    onValueChange = { temperature = it.toDouble() },
                    valueRange = 0.0f..1.5f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanShimmer,
                        activeTrackColor = ElectricIndigo
                    ),
                    modifier = Modifier.testTag("temperature_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0.0 Precise & Deterministic",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "1.5 Highly Creative",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = ElectricIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Auto-Speak AI Responses (TTS)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Automatically read aloud new replies as soon as generation finishes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = autoSpeakResponses,
                        onCheckedChange = { autoSpeakResponses = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricIndigo
                        ),
                        modifier = Modifier.testTag("auto_speak_switch")
                    )
                }
            }

            // Save button
            Button(
                onClick = {
                    onSaveSettings(
                        currentSettings.copy(
                            selectedModel = selectedModel,
                            selectedPersonaId = selectedPersonaId,
                            customSystemPrompt = customSystemPrompt.trim(),
                            temperature = temperature,
                            useCustomApiKey = useCustomApiKey,
                            customApiKeyHint = customApiKeyInput.trim(),
                            autoSpeakResponses = autoSpeakResponses
                        )
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricIndigo,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_settings_button")
            ) {
                Text(
                    text = "Save & Apply Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
