package com.example.services

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveService {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
        
    private var webSocket: WebSocket? = null
    
    private val _status = MutableStateFlow("Disconnected")
    val status: StateFlow<String> = _status
    
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript
    
    private var audioTrack: AudioTrack? = null

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
                
                // Send setup message for real-time multimodal live
                val setupMsg = JSONObject().apply {
                    put("setup", JSONObject().apply {
                        put("model", "models/gemini-2.0-flash-exp")
                        put("systemInstruction", JSONObject().apply {
                            put("parts", org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "You are Jules, a proactive, high-intelligence travel agent. You have vision capabilities. Help the user in real-time by observing their surroundings and providing context-aware advice, translations, and safety tips. Be concise and proactive.")
                                })
                            })
                        })
                        put("generationConfig", JSONObject().apply {
                            put("responseModalities", org.json.JSONArray().apply {
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
                    })
                }
                webSocket.send(setupMsg.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
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
                                    audioTrack?.write(audioBytes, 0, audioBytes.size)
                                }
                            }
                        }
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
    
    fun sendAudioChunk(pcmData: ByteArray) {
        if (webSocket == null || _status.value != "Connected") return
        
        val base64Data = Base64.encodeToString(pcmData, Base64.NO_WRAP)
        
        val msg = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", org.json.JSONArray().apply {
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
                put("mediaChunks", org.json.JSONArray().apply {
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
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
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
    }
}
