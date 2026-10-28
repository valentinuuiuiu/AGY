package com.example.ui.screens

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.room.AppDatabase
import com.example.data.room.Itinerary
import com.example.data.room.ItineraryRepository
import com.example.data.room.TravelNote
import com.example.data.room.TravelNoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ItineraryViewModel(application: Application) : AndroidViewModel(application) {
    private val itineraryRepo: ItineraryRepository
    private val noteRepo: TravelNoteRepository

    val itineraries: StateFlow<List<Itinerary>>

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("ALL") // ALL, VOICE, GENERATED, ITINERARY
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _isOffline = MutableStateFlow(!isNetworkAvailable())
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()

    // Filtered / searched travel notes flow
    val travelNotes: StateFlow<List<TravelNote>>

    init {
        val database = AppDatabase.getDatabase(application)
        itineraryRepo = ItineraryRepository(database.itineraryDao())
        noteRepo = TravelNoteRepository(database.travelNoteDao())

        itineraries = itineraryRepo.allItineraries.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        // Combine notes from Room with search query and filter
        travelNotes = combine(
            noteRepo.allNotes,
            _searchQuery,
            _selectedFilter
        ) { notes, query, filter ->
            notes.filter { note ->
                val matchesFilter = when (filter) {
                    "ALL" -> true
                    "VOICE" -> note.type == TravelNote.NOTE_TYPE_VOICE
                    "GENERATED" -> note.type == TravelNote.NOTE_TYPE_GENERATED || note.type == TravelNote.NOTE_TYPE_CHAT
                    "ITINERARY" -> note.type == TravelNote.NOTE_TYPE_ITINERARY
                    else -> true
                }
                val matchesQuery = if (query.isBlank()) {
                    true
                } else {
                    note.title.contains(query, ignoreCase = true) ||
                    note.transcriptOrPrompt.contains(query, ignoreCase = true) ||
                    note.aiResponse.contains(query, ignoreCase = true) ||
                    note.destination.contains(query, ignoreCase = true)
                }
                matchesFilter && matchesQuery
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        checkNetworkAndLoad()
        seedSampleNotesIfEmpty()
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun checkNetworkAndLoad() {
        val offline = !isNetworkAvailable()
        _isOffline.value = offline
        viewModelScope.launch {
            if (offline) {
                _snackbarEvent.emit("Mod Offline Activ: Acces direct la notițele și transcrierile vocale din Room.")
            } else {
                _snackbarEvent.emit("Conexiune activă: Datele sunt sincronizate și salvate local în Room.")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun deleteTravelNote(id: Int) {
        viewModelScope.launch {
            noteRepo.deleteNoteById(id)
            _snackbarEvent.emit("Notiță ștearsă din baza de date Room.")
        }
    }

    fun toggleFavorite(id: Int, currentFav: Boolean) {
        viewModelScope.launch {
            noteRepo.toggleFavorite(id, !currentFav)
        }
    }

    fun addCustomTravelNote(
        title: String,
        transcriptOrPrompt: String,
        aiResponse: String,
        destination: String,
        type: String = TravelNote.NOTE_TYPE_GENERATED
    ) {
        viewModelScope.launch {
            noteRepo.insertNote(
                TravelNote(
                    title = title,
                    destination = destination,
                    type = type,
                    transcriptOrPrompt = transcriptOrPrompt,
                    aiResponse = aiResponse,
                    tags = "Notițe, Offline, Călătorie"
                )
            )
            _snackbarEvent.emit("Notiță adăugată și salvată offline în Room!")
        }
    }

    fun addDummyItinerary() {
        viewModelScope.launch {
            itineraryRepo.insert(
                Itinerary(
                    title = "Weekend Getaway Kyoto",
                    destination = "Kyoto, Japan",
                    startDate = System.currentTimeMillis(),
                    endDate = System.currentTimeMillis() + (86400000 * 2),
                    notes = "Vizită altare Fushimi Inari și pădurea de bambus Arashiyama.",
                    latitude = 35.0116,
                    longitude = 135.7681
                )
            )
            _snackbarEvent.emit("Itinerariu adăugat în Room!")
        }
    }

    fun deleteItinerary(id: Int) {
        viewModelScope.launch {
            itineraryRepo.deleteById(id)
            _snackbarEvent.emit("Itinerariu șters din Room.")
        }
    }

    private fun seedSampleNotesIfEmpty() {
        viewModelScope.launch {
            val existing = noteRepo.allNotes.first()
            if (existing.isEmpty()) {
                noteRepo.insertNote(
                    TravelNote(
                        title = "Transcriere Voce: Întrebare Taxi Tokyo",
                        destination = "Tokyo, Japonia",
                        type = TravelNote.NOTE_TYPE_VOICE,
                        transcriptOrPrompt = "Cât costă o cursă până la stația Shinjuku?",
                        aiResponse = "新宿駅までいくらですか？ (Shinjuku-eki made ikura desu ka?) — Tariful mediu este de 1.200 yeni.",
                        sourceLanguage = "ro",
                        targetLanguage = "ja",
                        tags = "Voce, Taxi, Tokyo, Offline"
                    )
                )
                noteRepo.insertNote(
                    TravelNote(
                        title = "Ghid Generat AI: Ziua 1 în Kyoto",
                        destination = "Kyoto, Japonia",
                        type = TravelNote.NOTE_TYPE_GENERATED,
                        transcriptOrPrompt = "Itinerariu matinal pentru temple și gastronomie",
                        aiResponse = "08:00 - Fushimi Inari-taisha (evită aglomerația).\n11:30 - Prânz la Piața Nishiki (ramen tradițional).\n14:00 - Templul Kiyomizu-dera cu panoramă asupra orașului.",
                        sourceLanguage = "ro",
                        targetLanguage = "ja",
                        tags = "Itinerariu, AI, Kyoto, Offline"
                    )
                )
                noteRepo.insertNote(
                    TravelNote(
                        title = "Transcriere Voce: Cafenea Paris",
                        destination = "Paris, Franța",
                        type = TravelNote.NOTE_TYPE_VOICE,
                        transcriptOrPrompt = "O cafea cu lapte și un croissant, vă rog.",
                        aiResponse = "Un café au lait et un croissant, s'il vous plaît. Pronunție: [o kafe o le e un krwasan sil vu ple].",
                        sourceLanguage = "ro",
                        targetLanguage = "fr",
                        tags = "Voce, Restaurant, Franța"
                    )
                )
            }
        }
    }
}
