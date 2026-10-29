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
    private val geminiLiveService = GeminiLiveService(application)

    val status: StateFlow<String> = geminiLiveService.status
    val transcript: StateFlow<String> = geminiLiveService.transcript
    val lastToolAction: StateFlow<String?> = geminiLiveService.lastToolAction
    val audioAmplitude: StateFlow<Float> = geminiLiveService.audioAmplitude

    private val _isAnalyzingFrames = MutableStateFlow(false)
    val isAnalyzingFrames: StateFlow<Boolean> = _isAnalyzingFrames.asStateFlow()

    private val _framesSentCount = MutableStateFlow(0)
    val framesSentCount: StateFlow<Int> = _framesSentCount.asStateFlow()

    private var isRecording = false
    private var audioJob: Job? = null

    // Frame rate throttle: ~1.5 FPS for live vision streaming
    private var lastFrameTime = 0L
    private val frameIntervalMs = 700L

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

            try {
                audioRecord.startRecording()
                val buffer = ByteArray(bufferSize)

                while (isActive && isRecording) {
                    val read = audioRecord.read(buffer, 0, bufferSize)
                    if (read > 0) {
                        val pcmData = buffer.copyOf(read)
                        geminiLiveService.sendAudioChunk(pcmData)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    audioRecord.stop()
                    audioRecord.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
            }
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
                // Scale bitmap for real-time live vision optimization (max 640px)
                val maxDim = 640
                val scale = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val factor = maxDim.toFloat() / kotlin.math.max(bitmap.width, bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * factor).toInt(),
                        (bitmap.height * factor).toInt(),
                        true
                    )
                } else {
                    bitmap
                }

                val stream = ByteArrayOutputStream()
                scale.compress(Bitmap.CompressFormat.JPEG, 65, stream)
                val byteArray = stream.toByteArray()

                geminiLiveService.sendVideoFrame(byteArray)
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
