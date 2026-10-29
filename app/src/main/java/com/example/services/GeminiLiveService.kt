package com.example.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveService(private val context: Context? = null) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var webSocket: WebSocket? = null

    private val _status = MutableStateFlow("Disconnected")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _lastToolAction = MutableStateFlow<String?>(null)
    val lastToolAction: StateFlow<String?> = _lastToolAction.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private val agenticTools = context?.let { AgenticToolsService.getInstance(it) }

    fun connect(apiKeyOverride: String? = null) {
        val apiKey = if (!apiKeyOverride.isNullOrBlank()) apiKeyOverride else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            _status.value = "Error: Cheia API Gemini lipsește! Configureaz-o în Dev Console."
            return
        }

        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        initAudioTrack()
        _status.value = "Connecting..."

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _status.value = "Connected"
                Log.i("GeminiLive", "WebSocket Connected! Sending Multimodal Live setup...")

                // Setup message with Multimodal Live config + Agentic Tool Calling
                val setupMsg = JSONObject().apply {
                    put("setup", JSONObject().apply {
                        put("model", "models/gemini-2.0-flash-exp")
                        put("systemInstruction", JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put(
                                        "text",
                                        "You are Jules AGY, an ultra-advanced multimodal AI copilot and proactive travel companion. " +
                                                "You have real-time camera vision and audio ears. You can observe the world continuously. " +
                                                "You have access to Android tools: 'save_to_memory' (save notes/recommendations to local Room DB), " +
                                                "'search_places_and_navigate' (open Google Maps or launch turn-by-turn navigation), " +
                                                "'get_device_telemetry' (check battery, time, network), and 'open_app'. " +
                                                "Use your tools autonomously whenever appropriate. Be concise, witty, sharp, and helpful like Jarvis."
                                    )
                                })
                            })
                        })
                        put("generationConfig", JSONObject().apply {
                            put("responseModalities", JSONArray().apply {
                                put("AUDIO")
                            })
                            put("speechConfig", JSONObject().apply {
                                put("voiceConfig", JSONObject().apply {
                                    put("prebuiltVoiceConfig", JSONObject().apply {
                                        put("voiceName", "Puck")
                                    })
                                })
                            })
                        })
                        // Register Agentic Tools
                        put("tools", JSONArray().apply {
                            put(JSONObject().apply {
                                put("functionDeclarations", AgenticToolsService.getGeminiToolDeclarations())
                            })
                        })
                    })
                }
                webSocket.send(setupMsg.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)

                    // 1. Process serverContent (Audio stream + Transcript)
                    if (json.has("serverContent")) {
                        val serverContent = json.getJSONObject("serverContent")
                        if (serverContent.has("modelTurn")) {
                            val parts = serverContent.getJSONObject("modelTurn").getJSONArray("parts")
                            for (i in 0 until parts.length()) {
                                val part = parts.getJSONObject(i)
                                if (part.has("text")) {
                                    _transcript.value = part.getString("text")
                                }
                                if (part.has("inlineData")) {
                                    val inlineData = part.getJSONObject("inlineData")
                                    val data = inlineData.getString("data")
                                    val audioBytes = Base64.decode(data, Base64.DEFAULT)
                                    computeAmplitude(audioBytes)
                                    audioTrack?.write(audioBytes, 0, audioBytes.size)
                                }
                            }
                        }
                    }

                    // 2. Process toolCall (Agentic Function Calling)
                    if (json.has("toolCall")) {
                        val toolCall = json.getJSONObject("toolCall")
                        val functionCalls = toolCall.getJSONArray("functionCalls")
                        handleIncomingFunctionCalls(webSocket, functionCalls)
                    }
                } catch (e: Exception) {
                    Log.e("GeminiLive", "Error parsing msg", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errBody = try { response?.body?.string() } catch (e: Exception) { null }
                val code = response?.code
                val detail = if (code != null) " (HTTP $code)" else ""
                val msg = "Error$detail: ${t.message ?: errBody ?: "Conexiune eșuată"}"
                Log.e("GeminiLive", msg, t)
                _status.value = msg
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _status.value = "Disconnected"
            }
        })
    }

    private fun handleIncomingFunctionCalls(ws: WebSocket, functionCalls: JSONArray) {
        serviceScope.launch {
            val functionResponses = JSONArray()

            for (i in 0 until functionCalls.length()) {
                val call = functionCalls.getJSONObject(i)
                val name = call.getString("name")
                val callId = call.optString("id", java.util.UUID.randomUUID().toString())
                val args = call.optJSONObject("args") ?: JSONObject()

                _lastToolAction.value = "Agent executed: $name"

                val result = agenticTools?.executeTool(name, args) ?: JSONObject().apply {
                    put("success", true)
                    put("message", "Executed $name")
                }

                functionResponses.put(JSONObject().apply {
                    put("id", callId)
                    put("response", JSONObject().apply {
                        put("output", result)
                    })
                })
            }

            // Send toolResponse back to Gemini WebSocket
            val toolResponseMsg = JSONObject().apply {
                put("toolResponse", JSONObject().apply {
                    put("functionResponses", functionResponses)
                })
            }
            ws.send(toolResponseMsg.toString())
        }
    }

    private fun computeAmplitude(pcmBytes: ByteArray) {
        if (pcmBytes.isEmpty()) return
        var maxAmp = 0
        var i = 0
        while (i < pcmBytes.size - 1) {
            val sample = (pcmBytes[i].toInt() and 0xFF) or (pcmBytes[i + 1].toInt() shl 8)
            val absSample = kotlin.math.abs(sample.toShort().toInt())
            if (absSample > maxAmp) maxAmp = absSample
            i += 4 // Subsample
        }
        val normalized = (maxAmp / 32768f).coerceIn(0f, 1f)
        _audioAmplitude.value = normalized
    }

    fun sendAudioChunk(pcmData: ByteArray) {
        if (webSocket == null || _status.value != "Connected") return
        computeAmplitude(pcmData)

        val base64Data = Base64.encodeToString(pcmData, Base64.NO_WRAP)
        val msg = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", JSONArray().apply {
                    put(JSONObject().apply {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", base64Data)
                    })
                })
            })
        }
        webSocket?.send(msg.toString())
    }

    fun sendVideoFrame(jpegData: ByteArray) {
        if (webSocket == null || _status.value != "Connected") return

        val base64Data = Base64.encodeToString(jpegData, Base64.NO_WRAP)
        val msg = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", JSONArray().apply {
                    put(JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Data)
                    })
                })
            })
        }
        webSocket?.send(msg.toString())
    }

    private fun initAudioTrack() {
        val sampleRate = 24000 // Gemini returns 24kHz PCM by default
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack?.play()
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        _status.value = "Disconnected"
        _audioAmplitude.value = 0f
    }
}
