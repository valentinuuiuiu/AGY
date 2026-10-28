package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.config.ApiConfigManager
import com.example.services.GeminiLiveService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class GeminiLiveViewModel(application: Application) : AndroidViewModel(application) {
    private val configManager = ApiConfigManager.getInstance(application)
    private val geminiLiveService = GeminiLiveService()
    
    val status: StateFlow<String> = geminiLiveService.status
    val transcript: StateFlow<String> = geminiLiveService.transcript
    
    private val _isAnalyzingFrames = MutableStateFlow(false)
    val isAnalyzingFrames: StateFlow<Boolean> = _isAnalyzingFrames.asStateFlow()

    private val _framesSentCount = MutableStateFlow(0)
    val framesSentCount: StateFlow<Int> = _framesSentCount.asStateFlow()

    private var isRecording = false
    private var audioJob: Job? = null
    
    // Limits frame rate sent to Gemini Live API
    private var lastFrameTime = 0L
    private val frameIntervalMs = 800L // ~1.2 FPS for responsive live vision

    fun toggleLive() {
        if (status.value == "Connected") {
            stopLive()
        } else {
            startLive()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLive() {
        val apiKey = configManager.getSettings().geminiApiKey
        geminiLiveService.connect(apiKey)
        isRecording = true
        _isAnalyzingFrames.value = true
        
        audioJob = viewModelScope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            audioRecord.startRecording()
            val buffer = ByteArray(bufferSize)
            
            while (isActive && isRecording) {
                val read = audioRecord.read(buffer, 0, bufferSize)
                if (read > 0) {
                    val pcmData = buffer.copyOf(read)
                    geminiLiveService.sendAudioChunk(pcmData)
                }
            }
            
            audioRecord.stop()
            audioRecord.release()
        }
    }

    fun stopLive() {
        isRecording = false
        _isAnalyzingFrames.value = false
        audioJob?.cancel()
        geminiLiveService.disconnect()
    }
    
    fun processImageProxy(image: ImageProxy) {
        if (status.value != "Connected") {
            image.close()
            return
        }
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameTime < frameIntervalMs) {
            image.close()
            return
        }
        lastFrameTime = currentTime
        
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val bitmap = image.toBitmap()
                val scaled = Bitmap.createScaledBitmap(bitmap, 480, 640, true)
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 60, out)
                geminiLiveService.sendVideoFrame(out.toByteArray())
                _framesSentCount.value += 1
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                image.close()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLive()
    }
}
