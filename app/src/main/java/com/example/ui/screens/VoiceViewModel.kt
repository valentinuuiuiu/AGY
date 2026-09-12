package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.*
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _resultText = MutableStateFlow<String?>(null)
    val resultText: StateFlow<String?> = _resultText.asStateFlow()

    fun testTextToSpeech(text: String) {
        if (text.isBlank()) return
        _isLoading.value = true
        _resultText.value = "Generating speech audio..."

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = text)))),
                    generationConfig = GenerationConfig(
                        responseModalities = listOf("AUDIO"),
                        speechConfig = SpeechConfig(
                            voiceConfig = VoiceConfig(
                                prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = "Aoede")
                            )
                        )
                    )
                )

                // gemini-3.1-flash-tts-preview
                val response = RetrofitClient.service.generateContent("gemini-3.1-flash-tts-preview", apiKey, request)
                
                _resultText.value = "Speech generated successfully! (Base64 audio received)"
            } catch (e: Exception) {
                _resultText.value = "Failed to generate speech: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startLiveConversation() {
        _resultText.value = "Live API (gemini-3.1-flash-live-preview) requires WebSocket integration, which is currently simulated in this prototype. Live session active!"
    }
}
