package com.example.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.config.ApiConfigManager
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.InlineData
import com.example.network.Part
import com.example.network.RetrofitClient
import com.example.services.AgenticToolsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream

data class ArLensResult(
    val title: String,
    val translationOrInsight: String,
    val category: String, // "Menu", "Street Sign", "Landmark", "Culture"
    val dietaryTags: List<String> = emptyList(),
    val allergenWarning: String? = null,
    val culturalNote: String? = null,
    val rawImageBase64: String? = null
)

enum class ArLensMode(val label: String) {
    MENU_SCANNER("MENU SCANNER"),
    WORLD_LENS("WORLD LENS"),
    CULTURE_HUD("CULTURE HUD")
}

class ArLensViewModel(application: Application) : AndroidViewModel(application) {

    private val configManager = ApiConfigManager.getInstance(application)
    private val agenticTools = AgenticToolsService.getInstance(application)

    private val _selectedMode = MutableStateFlow(ArLensMode.MENU_SCANNER)
    val selectedMode: StateFlow<ArLensMode> = _selectedMode.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _lensResult = MutableStateFlow<ArLensResult?>(null)
    val lensResult: StateFlow<ArLensResult?> = _lensResult.asStateFlow()

    private val _savedToMemoryMessage = MutableStateFlow<String?>(null)
    val savedToMemoryMessage: StateFlow<String?> = _savedToMemoryMessage.asStateFlow()

    fun setMode(mode: ArLensMode) {
        _selectedMode.value = mode
        _lensResult.value = null
    }

    fun analyzeCapturedBitmap(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAnalyzing.value = true
            _savedToMemoryMessage.value = null

            try {
                // Compress bitmap to JPEG
                val stream = ByteArrayOutputStream()
                val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
                    val scaleFactor = 1024f / kotlin.math.max(bitmap.width, bitmap.height)
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * scaleFactor).toInt(), (bitmap.height * scaleFactor).toInt(), true)
                } else bitmap
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val base64Data = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

                val modePrompt = when (_selectedMode.value) {
                    ArLensMode.MENU_SCANNER ->
                        "Analyze this food menu image. Identify the dishes, ingredients, translate names to English, " +
                        "and clearly flag dietary tags (Vegan, Halal, Gluten-Free) and ANY possible allergens (peanuts, shellfish, dairy, gluten)."
                    ArLensMode.WORLD_LENS ->
                        "Analyze this street sign, landmark, or public text. Translate any foreign text into English, " +
                        "provide navigational meaning, and explain what this place or sign indicates."
                    ArLensMode.CULTURE_HUD ->
                        "Analyze this scene or object. Provide deep cultural context, local etiquette rules, " +
                        "and historical significance for a traveler visiting this region."
                }

                val systemPrompt = "You are Jules AR Lens. Return your response in clean JSON format matching this schema: " +
                        "{\"title\": string, \"translationOrInsight\": string, \"category\": string, \"dietaryTags\": [string], \"allergenWarning\": string, \"culturalNote\": string}"

                val content = Content(
                    parts = listOf(
                        Part(text = modePrompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Data))
                    )
                )

                val apiKey = configManager.getSettings().geminiApiKey
                val response = RetrofitClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = apiKey,
                    request = GenerateContentRequest(
                        contents = listOf(content),
                        systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                    )
                )

                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanJson = responseText.replace("```json", "").replace("```", "").trim()

                val json = try {
                    JSONObject(cleanJson)
                } catch (e: Exception) {
                    JSONObject().apply {
                        put("title", "AR Analysis")
                        put("translationOrInsight", responseText)
                        put("category", _selectedMode.value.label)
                    }
                }

                val dietaryTagsList = mutableListOf<String>()
                val tagsArr = json.optJSONArray("dietaryTags")
                if (tagsArr != null) {
                    for (i in 0 until tagsArr.length()) {
                        dietaryTagsList.add(tagsArr.getString(i))
                    }
                }

                _lensResult.value = ArLensResult(
                    title = json.optString("title", "Detected Object"),
                    translationOrInsight = json.optString("translationOrInsight", responseText),
                    category = json.optString("category", _selectedMode.value.label),
                    dietaryTags = dietaryTagsList,
                    allergenWarning = json.optString("allergenWarning").takeIf { it.isNotBlank() },
                    culturalNote = json.optString("culturalNote").takeIf { it.isNotBlank() },
                    rawImageBase64 = base64Data
                )
            } catch (e: Exception) {
                _lensResult.value = ArLensResult(
                    title = "Analysis Error",
                    translationOrInsight = "Failed to analyze frame: ${e.message}. Ensure Gemini API key is configured.",
                    category = "Error"
                )
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun saveResultToMemory() {
        val result = _lensResult.value ?: return
        viewModelScope.launch {
            val args = JSONObject().apply {
                put("title", result.title)
                put("content", "${result.translationOrInsight}\n\nCultural: ${result.culturalNote ?: "None"}\nAllergens: ${result.allergenWarning ?: "None"}")
                put("category", result.category)
            }
            val exec = agenticTools.executeTool("save_to_memory", args)
            _savedToMemoryMessage.value = exec.optString("message", "Saved to Infinite Memory")
        }
    }

    fun clearResult() {
        _lensResult.value = null
        _savedToMemoryMessage.value = null
    }
}
