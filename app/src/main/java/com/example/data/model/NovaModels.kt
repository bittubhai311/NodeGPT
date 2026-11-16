package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "New Conversation",
    val personaId: String = "assistant",
    val modelName: String = "gemini-3.8-flash",
    val lastMessagePreview: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val threadId: String = "",
    val role: String = "user", // "user" or "model"
    val content: String = "",
    val hasImage: Boolean = false,
    val imageLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "preferences",
    val userId: String = "",
    val selectedModel: String = "gemini-3.8-flash",
    val selectedPersonaId: String = "assistant",
    val customSystemPrompt: String = "",
    val temperature: Double = 0.7,
    val useCustomApiKey: Boolean = false,
    val customApiKeyHint: String = "",
    val autoSpeakResponses: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AiPersona(
    val id: String,
    val name: String,
    val badge: String,
    val tagline: String,
    val systemPrompt: String
)

data class GeminiModelOption(
    val modelId: String,
    val displayName: String,
    val badge: String,
    val description: String
)

data class StarterPrompt(
    val title: String,
    val subtitle: String,
    val prompt: String,
    val personaId: String = "assistant"
)

object NovaPresets {
    val models = listOf(
        GeminiModelOption(
            modelId = "gemini-3.8-flash",
            displayName = "Gemini 3.8 Flash",
            badge = "DEFAULT • FREE",
            description = "Frontier intelligence & multimodal vision with fast streaming."
        ),
        GeminiModelOption(
            modelId = "gemini-3.5-flash-lite",
            displayName = "Gemini 3.5 Flash-Lite",
            badge = "ULTRA-FAST • FREE",
            description = "Instant low-latency responses for quick chat and everyday tasks."
        ),
        GeminiModelOption(
            modelId = "gemini-4-argon",
            displayName = "Gemini 4 Argon",
            badge = "PRO REASONING",
            description = "Deep analytical reasoning, complex coding architecture, and math."
        )
    )

    val personas = listOf(
        AiPersona(
            id = "assistant",
            name = "Nova Default",
            badge = "Balanced GPT",
            tagline = "Smart, clear, and adaptable across English, Hindi & Hinglish",
            systemPrompt = "You are NovaGPT, a friendly, articulate, and highly capable AI assistant. Format responses cleanly with markdown headings, bullet points, and fenced code blocks where helpful. Automatically match the user's language (English, Hindi, or Hinglish)."
        ),
        AiPersona(
            id = "coder",
            name = "Senior Architect",
            badge = "Code & Debug",
            tagline = "Production-ready code, clean architecture, and bug analysis",
            systemPrompt = "You are a Staff Software Engineer. Write complete, idiomatic, production-grade code with brief explanations of time/space complexity and edge cases. Always specify the language in markdown code blocks."
        ),
        AiPersona(
            id = "hinglish",
            name = "Desi Dost AI",
            badge = "Hinglish / Hindi",
            tagline = "Natural conversational Hinglish with relatable examples",
            systemPrompt = "Tum ek smart, friendly Hinglish AI dost ho. User ke har sawaal ka jawab aasaan aur natural Hinglish/Hindi mein do, taaki complex topics bhi ekdum simple aur mazedaar lagein."
        ),
        AiPersona(
            id = "tutor",
            name = "Socratic Tutor",
            badge = "Study & Exam",
            tagline = "Step-by-step breakdowns, analogies, and quick self-check quizzes",
            systemPrompt = "You are an encouraging master educator. Break complex concepts down from first principles using analogies, step-by-step examples, and end with a quick 1-question check for understanding."
        ),
        AiPersona(
            id = "creative",
            name = "Creative Studio",
            badge = "Writer & Ideas",
            tagline = "Storytelling, YouTube scripts, viral hooks, and copywriting",
            systemPrompt = "You are an imaginative creative director and writer. Craft vivid, engaging scripts, stories, social media hooks, and brainstorms with strong emotional resonance."
        )
    )

    val starterPrompts = listOf(
        StarterPrompt(
            title = "Write & Debug Code",
            subtitle = "Build a clean Kotlin coroutine Flow pipeline with retry logic",
            prompt = "Write a clean Kotlin Coroutines Flow pipeline that fetches data with exponential backoff retry and maps errors to a sealed UiState class.",
            personaId = "coder"
        ),
        StarterPrompt(
            title = "Samjhao in Hinglish",
            subtitle = "AI aur Large Language Models kaise kaam karte hain?",
            prompt = "Bhai aasaan Hinglish mein samjhao ki GPT aur Gemini jaise Large Language Models andar se kaise kaam karte hain, ekdum real-life example ke saath!",
            personaId = "hinglish"
        ),
        StarterPrompt(
            title = "Master a Concept",
            subtitle = "Quantum computing vs classical bits in 3 minutes",
            prompt = "Explain quantum computing vs classical computing in 3 intuitive levels: Beginner analogy, Engineering reality, and Real-world impact.",
            personaId = "tutor"
        ),
        StarterPrompt(
            title = "YouTube Script & Hooks",
            subtitle = "High-retention tech video intro & outline",
            prompt = "Create 3 high-retention opening hooks and a crisp 5-minute video script outline about the future of AI smartphones.",
            personaId = "creative"
        )
    )

    fun personaById(id: String): AiPersona =
        personas.firstOrNull { it.id == id } ?: personas.first()

    fun modelById(id: String): GeminiModelOption =
        models.firstOrNull { it.modelId == id } ?: models.first()
}
