package com.example.services

import android.content.Context
import com.example.data.config.ApiConfigManager
import com.example.data.room.AppDatabase
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class BrainMetrics(
    val totalNotes: Int,
    val totalPhrases: Int,
    val totalItineraries: Int,
    val estimatedTokens: Int
)

data class BrainAnswer(
    val answer: String,
    val memoryContextUsed: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MemoryRagService private constructor(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val configManager = ApiConfigManager.getInstance(context)

    companion object {
        @Volatile
        private var INSTANCE: MemoryRagService? = null

        fun getInstance(context: Context): MemoryRagService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MemoryRagService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun getMetrics(): BrainMetrics = withContext(Dispatchers.IO) {
        val notes = db.travelNoteDao().getAllNotes().first()
        val phrases = db.savedPhraseDao().getAllPhrases().first()
        val itineraries = db.itineraryDao().getAllItineraries().first()

        // Estimate tokens ~ 4 chars per token
        val notesChars: Int = notes.sumOf { (it.title.length + it.transcriptOrPrompt.length + it.aiResponse.length) }
        val phraseChars: Int = phrases.sumOf { (it.originalText.length + it.translatedText.length) }
        val itnChars: Int = itineraries.sumOf { (it.destination.length + it.notes.length) }
        val charCount = notesChars + phraseChars + itnChars
        val tokens = (charCount / 4).coerceAtLeast(120)

        BrainMetrics(
            totalNotes = notes.size,
            totalPhrases = phrases.size,
            totalItineraries = itineraries.size,
            estimatedTokens = tokens
        )
    }

    suspend fun buildSemanticMemoryContext(): String = withContext(Dispatchers.IO) {
        val notes = db.travelNoteDao().getAllNotes().first()
        val phrases = db.savedPhraseDao().getAllPhrases().first()
        val itineraries = db.itineraryDao().getAllItineraries().first()

        buildString {
            appendLine("=== JULES AGY INFINITE MEMORY REPOSITORY ===")
            appendLine("USER PERSISTENT TRAVEL NOTES & CONVERSATION TRANSCRIPTS:")
            if (notes.isEmpty()) {
                appendLine("- No custom notes yet. Initial memory baseline.")
            } else {
                notes.take(40).forEachIndexed { i, note ->
                    appendLine("[Note #${i + 1}] Title: ${note.title} | Category/Destination: ${note.destination} | Tags: ${note.tags}")
                    appendLine("Prompt: ${note.transcriptOrPrompt}")
                    appendLine("Detail: ${note.aiResponse}")
                    appendLine("---")
                }
            }

            appendLine("\nSAVED OFFLINE PHRASES & CULTURAL VOCABULARY:")
            if (phrases.isEmpty()) {
                appendLine("- No phrases recorded yet.")
            } else {
                phrases.take(30).forEachIndexed { i, p ->
                    appendLine("[Phrase #${i + 1}] '${p.originalText}' -> '${p.translatedText}' (${p.sourceLanguage} -> ${p.targetLanguage}) [Cat: ${p.category}]")
                }
            }

            appendLine("\nSAVED TRIP ITINERARIES:")
            if (itineraries.isEmpty()) {
                appendLine("- No structured itineraries recorded.")
            } else {
                itineraries.take(10).forEachIndexed { i, itn ->
                    appendLine("[Itinerary #${i + 1}] Destination: ${itn.destination} | Title: ${itn.title} | Notes: ${itn.notes}")
                }
            }
        }
    }

    suspend fun queryInfiniteBrain(userQuery: String): BrainAnswer = withContext(Dispatchers.IO) {
        val memoryContext = buildSemanticMemoryContext()
        val apiKey = configManager.getSettings().geminiApiKey

        val systemInstruction = Content(
            parts = listOf(
                Part(
                    text = "You are Jules' Infinite Memory Brain powered by Gemini 1.5 Pro with a massive context window. " +
                            "You have synthesized the user's entire travel history, personal notes, offline phrases, and itineraries. " +
                            "When the user asks questions (such as 'Where should I go next?', 'What did I like in Tokyo?', or 'Synthesize my trip'), " +
                            "deeply reference their saved memories, correlate their preferences, and provide ultra-personalized, proactive recommendations. " +
                            "Format your answer with clear markdown bullet points and highlight specific notes they saved."
                )
            )
        )

        val promptContent = Content(
            role = "user",
            parts = listOf(
                Part(
                    text = "HERE IS MY COMPLETE LOCAL MEMORY DATABASE:\n\n$memoryContext\n\nUSER INQUIRY:\n$userQuery"
                )
            )
        )

        val request = GenerateContentRequest(
            contents = listOf(promptContent),
            systemInstruction = systemInstruction
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = "gemini-1.5-pro",
                apiKey = apiKey,
                request = request
            )
            val answerText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No response generated from Infinite Brain."

            BrainAnswer(
                answer = answerText,
                memoryContextUsed = "Synthesized ${getMetrics().estimatedTokens} context tokens across ${getMetrics().totalNotes} notes & ${getMetrics().totalPhrases} phrases."
            )
        } catch (e: Exception) {
            // Fallback to gemini-2.0-flash if 1.5-pro has rate limit or latency issues
            try {
                val fallbackResponse = RetrofitClient.service.generateContent(
                    model = "gemini-2.0-flash",
                    apiKey = apiKey,
                    request = request
                )
                val fallbackText = fallbackResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Error retrieving memory."
                BrainAnswer(
                    answer = fallbackText,
                    memoryContextUsed = "Synthesized via Flash 2.0 with context memory."
                )
            } catch (err: Exception) {
                BrainAnswer(
                    answer = "Infinite Brain Query Failed: ${err.message ?: e.message}. Check Gemini API Key in Dev Settings.",
                    memoryContextUsed = "Local memory offline."
                )
            }
        }
    }
}
