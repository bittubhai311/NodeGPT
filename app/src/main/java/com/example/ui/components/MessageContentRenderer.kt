package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanShimmer
import com.example.ui.theme.ElectricIndigoSoft
import com.example.ui.theme.ElevatedSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBackground
import kotlinx.coroutines.delay

sealed interface ContentSegment {
    data class MarkdownText(val text: String) : ContentSegment
    data class CodeBlock(val language: String, val code: String) : ContentSegment
}

fun parseMessageSegments(raw: String): List<ContentSegment> {
    val segments = mutableListOf<ContentSegment>()
    val regex = Regex("```([a-zA-Z0-9_+\\-#.]*)\\n?([\\s\\S]*?)```")
    var lastIndex = 0
    for (match in regex.findAll(raw)) {
        if (match.range.first > lastIndex) {
            val preceding = raw.substring(lastIndex, match.range.first).trim('\n')
            if (preceding.isNotBlank()) {
                segments.add(ContentSegment.MarkdownText(preceding))
            }
        }
        val lang = match.groupValues[1].trim().ifEmpty { "code" }
        val code = match.groupValues[2].trimEnd()
        segments.add(ContentSegment.CodeBlock(language = lang, code = code))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < raw.length) {
        val remaining = raw.substring(lastIndex).trim('\n')
        if (remaining.isNotBlank()) {
            segments.add(ContentSegment.MarkdownText(remaining))
        }
    }
    if (segments.isEmpty() && raw.isNotBlank()) {
        segments.add(ContentSegment.MarkdownText(raw))
    }
    return segments
}

@Composable
fun FormattedMessageContent(
    content: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val segments = remember(content) { parseMessageSegments(content) }

    SelectionContainer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            segments.forEach { segment ->
                when (segment) {
                    is ContentSegment.MarkdownText -> {
                        MarkdownParagraphs(text = segment.text, textColor = textColor)
                    }
                    is ContentSegment.CodeBlock -> {
                        CodeBlockCard(language = segment.language, code = segment.code)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownParagraphs(
    text: String,
    textColor: Color
) {
    val lines = remember(text) { text.lines() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("### ") -> {
                    Text(
                        text = formatInlineMarkdown(trimmed.removePrefix("### ")),
                        style = MaterialTheme.typography.titleMedium,
                        color = CyanShimmer,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = formatInlineMarkdown(trimmed.removePrefix("## ")),
                        style = MaterialTheme.typography.titleLarge,
                        color = ElectricIndigoSoft,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = formatInlineMarkdown(trimmed.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineMedium,
                        color = ElectricIndigoSoft,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    val bulletBody = trimmed.drop(2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = CyanShimmer,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = formatInlineMarkdown(bulletBody),
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                    }
                }
                trimmed.isNotBlank() -> {
                    Text(
                        text = formatInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }
            }
        }
    }
}

private fun formatInlineMarkdown(input: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < input.length) {
            if (input.startsWith("**", i)) {
                val end = input.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(input.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            if (input[i] == '`') {
                val end = input.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            background = Color(0xFF1E293B),
                            color = CyanShimmer,
                            fontSize = 14.sp
                        )
                    ) {
                        append(" " + input.substring(i + 1, end) + " ")
                    }
                    i = end + 1
                    continue
                }
            }
            append(input[i])
            i++
        }
    }
}

@Composable
fun CodeBlockCard(
    language: String,
    code: String
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianBackground)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
    ) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ElevatedSurfaceVariant.copy(alpha = 0.75f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = CyanShimmer.copy(alpha = 0.14f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = language.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanShimmer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(code))
                    copied = true
                },
                modifier = Modifier.testTag("copy_code_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (copied) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Scrollable code body
        Text(
            text = code,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = Color(0xFFE2E8F0),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(14.dp)
        )
    }
}
