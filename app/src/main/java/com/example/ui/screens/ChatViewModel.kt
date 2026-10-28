package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.network.RetrofitClient
import com.example.services.AgyPrimeBridge
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

data class ChatMessage(val isUser: Boolean, val text: String)

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val conversationHistory = mutableListOf<Content>()
    private val agyPrimeBridge = AgyPrimeBridge()

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val userMessage = ChatMessage(isUser = true, text = userText)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        // --- AGY CLINCH DIRECT HOOK ---
        // Triggered if prompt starts with "/agy", "/prime", or "/spark"
        val trimmed = userText.trim()
        if (trimmed.startsWith("/agy ") || trimmed.startsWith("/prime ") || trimmed.startsWith("/spark ")) {
            val isSpark = trimmed.startsWith("/spark ")
            val cleanCmd = trimmed.substringAfter(" ")
            
            viewModelScope.launch {
                try {
                    val result = agyPrimeBridge.executeSovereignPrompt(
                        prompt = cleanCmd,
                        targetNode = if (isSpark) "spark" else "prime"
                    )
                    val responseText = result.getOrNull() ?: ("Error: " + result.exceptionOrNull()?.message)
                    _messages.value = _messages.value + ChatMessage(isUser = false, text = responseText)
                } catch (e: Exception) {
                    _messages.value = _messages.value + ChatMessage(isUser = false, text = "AGY Bridge Fault: ${e.message}")
                } finally {
                    _isLoading.value = false
                }
            }
            return
        }

        // --- STANDARD PUBLIC GEMINI TRAVEL ASSISTANT ---
        val userContent = Content(role = "user", parts = listOf(Part(text = userText)))
        conversationHistory.add(userContent)

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                // Real Gemini dynamic search & maps grounding tool
                val googleMapsTool = buildJsonObject {
                    putJsonObject("googleSearchRetrieval") {
                        putJsonObject("dynamicRetrievalConfig") {
                            put("mode", "MODE_DYNAMIC")
                            put("dynamicThreshold", 0.3)
                        }
                    }
                }

                val request = GenerateContentRequest(
                    contents = conversationHistory.toList(),
                    systemInstruction = Content(parts = listOf(Part(text = "You are WanderAI, an expert travel assistant. Provide accurate locations, culture tips, local navigation, and itineraries."))),
                    tools = listOf(googleMapsTool)
                )

                val response = RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response."

                val modelContent = Content(role = "model", parts = listOf(Part(text = responseText)))
                conversationHistory.add(modelContent)

                val modelMessage = ChatMessage(isUser = false, text = responseText)
                _messages.value = _messages.value + modelMessage

            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(isUser = false, text = "Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
