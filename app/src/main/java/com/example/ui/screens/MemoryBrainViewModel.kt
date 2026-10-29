package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.room.AppDatabase
import com.example.data.room.TravelNote
import com.example.services.BrainAnswer
import com.example.services.BrainMetrics
import com.example.services.MemoryRagService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MemoryBrainViewModel(application: Application) : AndroidViewModel(application) {

    private val ragService = MemoryRagService.getInstance(application)
    private val db = AppDatabase.getDatabase(application)

    private val _metrics = MutableStateFlow(BrainMetrics(0, 0, 0, 0))
    val metrics: StateFlow<BrainMetrics> = _metrics.asStateFlow()

    private val _isQuerying = MutableStateFlow(false)
    val isQuerying: StateFlow<Boolean> = _isQuerying.asStateFlow()

    private val _lastAnswer = MutableStateFlow<BrainAnswer?>(null)
    val lastAnswer: StateFlow<BrainAnswer?> = _lastAnswer.asStateFlow()

    val travelNotes: StateFlow<List<TravelNote>> = db.travelNoteDao()
        .getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshMetrics()
    }

    fun refreshMetrics() {
        viewModelScope.launch {
            _metrics.value = ragService.getMetrics()
        }
    }

    fun queryBrain(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isQuerying.value = true
            val answer = ragService.queryInfiniteBrain(query)
            _lastAnswer.value = answer
            _isQuerying.value = false
            refreshMetrics()
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            db.travelNoteDao().deleteNoteById(id)
            refreshMetrics()
        }
    }

    fun toggleFavorite(id: Int, currentFav: Boolean) {
        viewModelScope.launch {
            db.travelNoteDao().updateFavorite(id, !currentFav)
        }
    }

    fun addQuickNote(title: String, content: String, destination: String = "Global") {
        viewModelScope.launch {
            val note = TravelNote(
                title = title,
                destination = destination,
                type = TravelNote.NOTE_TYPE_GENERATED,
                transcriptOrPrompt = "Manual Entry",
                aiResponse = content,
                tags = "Personal, Memory",
                isFavorite = true
            )
            db.travelNoteDao().insertNote(note)
            refreshMetrics()
        }
    }
}
