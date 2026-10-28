package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.*
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

class CreateViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _resultText = MutableStateFlow<String?>(null)
    val resultText: StateFlow<String?> = _resultText.asStateFlow()

    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    fun generateImage(prompt: String, size: String, aspectRatio: String) {
        if (prompt.isBlank()) return
        _isLoading.value = true
        _resultText.value = "Generating image via Gemini Vision Core..."
        _generatedBitmap.value = null

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(
                        imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = size),
                        responseModalities = listOf("TEXT", "IMAGE")
                    )
                )

                val modelName = if (size == "1K") "gemini-3.1-flash-image-preview" else "gemini-3-pro-image-preview"
                val response = RetrofitClient.service.generateContent(modelName, apiKey, request)
                
                // Real decoding of inline base64 image data
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
                    _resultText.value = "Image generated successfully ($modelName)!"
                } else {
                    val textPart = parts?.firstOrNull { it.text != null }?.text
                    _resultText.value = textPart ?: "Completed (no visual bytes returned)."
                }
            } catch (e: Exception) {
                _resultText.value = "Failed to generate image: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateVideo(prompt: String, aspectRatio: String) {
        if (prompt.isBlank()) return
        _isLoading.value = true
        _resultText.value = "Starting Veo 3.1 video generation..."

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                val request = GenerateVideosRequest(
                    prompt = prompt,
                    config = VeoConfig(
                        numberOfVideos = 1,
                        resolution = "1080p",
                        aspectRatio = aspectRatio
                    )
                )

                val response = RetrofitClient.service.generateVideos("veo-3.1-fast-generate-preview", apiKey, request)
                val opName = response["name"]?.jsonPrimitive?.content ?: "Operation Submitted"
                _resultText.value = "Veo 3.1 generation started.\nTracking ID: $opName"
            } catch (e: Exception) {
                _resultText.value = "Failed to generate video: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
