package com.example.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.config.ApiConfigManager
import com.example.data.room.AppDatabase
import com.example.data.room.TravelNote
import com.example.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

data class StylePreset(val name: String, val promptSuffix: String)

class CreateViewModel(application: Application) : AndroidViewModel(application) {
    private val configManager = ApiConfigManager.getInstance(application)
    private val db = AppDatabase.getDatabase(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isEnhancing = MutableStateFlow(false)
    val isEnhancing: StateFlow<Boolean> = _isEnhancing.asStateFlow()

    private val _resultText = MutableStateFlow<String?>(null)
    val resultText: StateFlow<String?> = _resultText.asStateFlow()

    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    private val _savedToVault = MutableStateFlow<String?>(null)
    val savedToVault: StateFlow<String?> = _savedToVault.asStateFlow()

    val stylePresets = listOf(
        StylePreset("Cyberpunk 2077", ", neon glow, wet asphalt reflections, futuristic holograms, volumetric fog, octane render 8k"),
        StylePreset("Hyper-Realistic", ", photorealistic 35mm photograph, shot on Hasselblad, natural lighting, ultra-detailed, depth of field"),
        StylePreset("Retro Synthwave", ", 80s aesthetic, neon grid, sunset horizon, laser lights, chrome details, vaporwave vibe"),
        StylePreset("Cinematic Travel", ", National Geographic style, dramatic golden hour, atmospheric perspective, high dynamic range"),
        StylePreset("Ukiyo-e Sci-Fi", ", traditional Japanese woodblock print fused with cyberpunk mecha and neon typography"),
        StylePreset("Blade Runner Noir", ", dark rain-soaked megacity, dystopian high-contrast shadows, amber and cyan lighting")
    )

    private fun getApiKey(): String {
        val override = configManager.getSettings().geminiApiKey
        return if (override.isNotBlank()) override else BuildConfig.GEMINI_API_KEY
    }

    fun enhancePrompt(basePrompt: String, onEnhanced: (String) -> Unit) {
        if (basePrompt.isBlank()) return
        _isEnhancing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = getApiKey()
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(
                                    text = "Enhance and expand this image prompt for Imagen 3/Veo into a vivid, descriptive prompt with artistic composition, lens details, and cinematic atmosphere. Return ONLY the enhanced prompt string without explanations:\n\n$basePrompt"
                                )
                            )
                        )
                    )
                )
                val response = RetrofitClient.service.generateContent("gemini-3-pro-image-preview", apiKey, request)
                val enhanced = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!enhanced.isNullOrBlank()) {
                    onEnhanced(enhanced)
                }
            } catch (e: Exception) {
                _resultText.value = "Prompt enhancement error: ${e.message}"
            } finally {
                _isEnhancing.value = false
            }
        }
    }

    fun generateImage(prompt: String, size: String, aspectRatio: String, selectedStyle: StylePreset?, negativePrompt: String) {
        if (prompt.isBlank()) return
        _isLoading.value = true
        _resultText.value = "Synthesizing visual asset via Imagen 3 Core..."
        _generatedBitmap.value = null
        _savedToVault.value = null

        val fullPrompt = buildString {
            append(prompt)
            if (selectedStyle != null) append(selectedStyle.promptSuffix)
            if (negativePrompt.isNotBlank()) append(" [Negative: $negativePrompt]")
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = getApiKey()
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = fullPrompt)))),
                    generationConfig = GenerationConfig(
                        imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = size),
                        responseModalities = listOf("TEXT", "IMAGE")
                    )
                )

                val modelName = if (size == "1K") "gemini-3.1-flash-image-preview" else "gemini-3-pro-image-preview"
                val response = RetrofitClient.service.generateContent(modelName, apiKey, request)

                var decoded: Bitmap? = null
                val parts = response.candidates?.firstOrNull()?.content?.parts
                if (parts != null) {
                    for (part in parts) {
                        if (part.inlineData != null && part.inlineData.data.isNotBlank()) {
                            val bytes = Base64.decode(part.inlineData.data, Base64.DEFAULT)
                            decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            break
                        }
                    }
                }

                if (decoded != null) {
                    _generatedBitmap.value = decoded
                    _resultText.value = "Asset generated successfully ($aspectRatio, $size)!"
                } else {
                    val textPart = parts?.firstOrNull { it.text != null }?.text
                    _resultText.value = textPart ?: "Vision synthesis complete."
                }
            } catch (e: Exception) {
                _resultText.value = "Failed to generate asset: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateVideo(prompt: String, aspectRatio: String) {
        if (prompt.isBlank()) return
        _isLoading.value = true
        _resultText.value = "Dispatching Veo 3.1 Neural Video Pipeline..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = getApiKey()
                val request = GenerateVideosRequest(
                    prompt = prompt,
                    config = VeoConfig(
                        numberOfVideos = 1,
                        resolution = "1080p",
                        aspectRatio = aspectRatio
                    )
                )

                val response = RetrofitClient.service.generateVideos("veo-3.1-fast-generate-preview", apiKey, request)
                val opName = response["name"]?.jsonPrimitive?.content ?: "Veo Task Queued"
                _resultText.value = "Veo 3.1 video task initiated.\nJob ID: $opName"
            } catch (e: Exception) {
                _resultText.value = "Veo video generation error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveAssetToNotes(prompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = TravelNote(
                title = "Creative Studio Asset",
                destination = "Studio",
                type = TravelNote.NOTE_TYPE_GENERATED,
                transcriptOrPrompt = prompt,
                aiResponse = _resultText.value ?: "Generated Visual Asset",
                tags = "Studio, Imagen3",
                isFavorite = true
            )
            db.travelNoteDao().insertNote(note)
            _savedToVault.value = "Saved to Infinite Memory Vault!"
        }
    }
}
