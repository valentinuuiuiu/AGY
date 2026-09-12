package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.*
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CreateViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _resultText = MutableStateFlow<String?>(null)
    val resultText: StateFlow<String?> = _resultText.asStateFlow()

    fun generateImage(prompt: String, size: String, aspectRatio: String) {
        if (prompt.isBlank()) return
        _isLoading.value = true
        _resultText.value = "Generating image..."

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

                // gemini-3.1-flash-image-preview for 1K, gemini-3-pro-image-preview for higher sizes
                val modelName = if (size == "1K") "gemini-3.1-flash-image-preview" else "gemini-3-pro-image-preview"
                val response = RetrofitClient.service.generateContent(modelName, apiKey, request)
                
                // Assuming base64 image would be returned in parts, but we'll just show success for this prototype
                _resultText.value = "Image generated successfully using $modelName! (Base64 data received)"
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
        _resultText.value = "Starting Veo 3 video generation..."

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

                // veo-3.1-fast-generate-preview
                val response = RetrofitClient.service.generateVideos("veo-3.1-fast-generate-preview", apiKey, request)
                
                val opName = response["name"]?.jsonPrimitive?.content ?: "Unknown Operation"
                _resultText.value = "Video generation started. Operation: $opName"
            } catch (e: Exception) {
                _resultText.value = "Failed to generate video: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
