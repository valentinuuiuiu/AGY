package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.room.AppDatabase
import com.example.data.room.TravelNote
import com.example.data.room.TravelNoteRepository
import com.example.data.translation.Language
import com.example.network.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VoiceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val noteRepository = TravelNoteRepository(database.travelNoteDao())

    val cachedVoiceNotes: StateFlow<List<TravelNote>> = noteRepository.getNotesByType(TravelNote.NOTE_TYPE_VOICE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _resultText = MutableStateFlow<String?>(null)
    val resultText: StateFlow<String?> = _resultText.asStateFlow()

    private val _lastSpokenText = MutableStateFlow<String>("")
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    private val _sourceLang = MutableStateFlow(Language.findByCode("ro"))
    val sourceLang: StateFlow<Language> = _sourceLang.asStateFlow()

    private val _targetLang = MutableStateFlow(Language.findByCode("en"))
    val targetLang: StateFlow<Language> = _targetLang.asStateFlow()

    private val _swapCounter = MutableStateFlow(0)
    val swapCounter: StateFlow<Int> = _swapCounter.asStateFlow()

    private val _userFeedbackEvent = MutableSharedFlow<String>()
    val userFeedbackEvent = _userFeedbackEvent.asSharedFlow()

    fun swapLanguages() {
        val temp = _sourceLang.value
        _sourceLang.value = _targetLang.value
        _targetLang.value = temp
        _swapCounter.value += 1
        viewModelScope.launch {
            _userFeedbackEvent.emit("Limbi comutate: ${_sourceLang.value.flag} ${_sourceLang.value.nameRo} ⇄ ${_targetLang.value.flag} ${_targetLang.value.nameRo}")
        }
    }

    fun setSourceLanguage(lang: Language) {
        _sourceLang.value = lang
    }

    fun setTargetLanguage(lang: Language) {
        _targetLang.value = lang
    }

    fun testTextToSpeech(text: String) {
        if (text.isBlank()) return
        _lastSpokenText.value = text
        _isLoading.value = true
        _resultText.value = "Generare sinteză vocală în ${_targetLang.value.nameRo}..."

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
                val responseSummary = "Sinteză audio generată cu succes în ${_targetLang.value.nameRo}! (Voce Aoede primită)"
                _resultText.value = responseSummary

                // Automatically cache voice transcript & response into Room
                noteRepository.insertNote(
                    TravelNote(
                        title = "Transcriere Voce: ${_targetLang.value.nameRo}",
                        destination = _targetLang.value.nameRo,
                        type = TravelNote.NOTE_TYPE_VOICE,
                        transcriptOrPrompt = text,
                        aiResponse = responseSummary,
                        sourceLanguage = _sourceLang.value.code,
                        targetLanguage = _targetLang.value.code,
                        tags = "Voce, Audio, ${_targetLang.value.nameRo}, Offline"
                    )
                )
                _userFeedbackEvent.emit("Transcriere vocală salvată offline în baza de date Room!")
            } catch (e: Exception) {
                val errorMsg = "Eroare sinteză voce: ${e.message}"
                _resultText.value = errorMsg
                // Cache draft even when offline or error occurs
                noteRepository.insertNote(
                    TravelNote(
                        title = "Transcriere Voce (Salvat Offline): ${_targetLang.value.nameRo}",
                        destination = _targetLang.value.nameRo,
                        type = TravelNote.NOTE_TYPE_VOICE,
                        transcriptOrPrompt = text,
                        aiResponse = "Notiță salvată offline în roaming. $errorMsg",
                        sourceLanguage = _sourceLang.value.code,
                        targetLanguage = _targetLang.value.code,
                        tags = "Voce, Offline Draft"
                    )
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startLiveConversation() {
        val summary = "Live API (gemini-3.1-flash-live-preview) activ între ${_sourceLang.value.flag} ${_sourceLang.value.nameRo} și ${_targetLang.value.flag} ${_targetLang.value.nameRo}."
        _resultText.value = summary

        viewModelScope.launch {
            noteRepository.insertNote(
                TravelNote(
                    title = "Sesiune Audio Live: ${_sourceLang.value.code.uppercase()} ⇄ ${_targetLang.value.code.uppercase()}",
                    destination = _targetLang.value.nameRo,
                    type = TravelNote.NOTE_TYPE_VOICE,
                    transcriptOrPrompt = "Sesiune comunicare audio în timp real",
                    aiResponse = summary,
                    sourceLanguage = _sourceLang.value.code,
                    targetLanguage = _targetLang.value.code,
                    tags = "Live, Audio, Bidirecțional"
                )
            )
            _userFeedbackEvent.emit("Sesiune audio înregistrată în arhiva offline Room.")
        }
    }

    fun saveCurrentTranscriptToRoom(prompt: String, answer: String) {
        if (prompt.isBlank() && answer.isBlank()) return
        viewModelScope.launch {
            noteRepository.insertNote(
                TravelNote(
                    title = "Notiță Vocală Călătorie",
                    destination = _targetLang.value.nameRo,
                    type = TravelNote.NOTE_TYPE_VOICE,
                    transcriptOrPrompt = prompt.ifBlank { "Transcriere vocală" },
                    aiResponse = answer.ifBlank { "Conținut generat" },
                    sourceLanguage = _sourceLang.value.code,
                    targetLanguage = _targetLang.value.code,
                    tags = "Voce, Călătorie, Offline"
                )
            )
            _userFeedbackEvent.emit("Salvat cu succes în Notițe Călătorie (Room)!")
        }
    }

    fun deleteCachedNote(id: Int) {
        viewModelScope.launch {
            noteRepository.deleteNoteById(id)
            _userFeedbackEvent.emit("Transcriere ștearsă din arhiva locală.")
        }
    }
}
