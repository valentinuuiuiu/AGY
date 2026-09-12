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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItineraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ItineraryRepository
    
    val itineraries: StateFlow<List<Itinerary>>

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ItineraryRepository(database.itineraryDao())
        
        itineraries = repository.allItineraries.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        
        checkNetworkAndLoad()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun checkNetworkAndLoad() {
        viewModelScope.launch {
            if (!isNetworkAvailable()) {
                _snackbarEvent.emit("No network connectivity. Loading cached travel itineraries.")
            } else {
                _snackbarEvent.emit("Network connected. Synced with travel cloud.")
                // In a real app, we would fetch from remote and save to Room here.
            }
        }
    }

    fun addDummyItinerary() {
        viewModelScope.launch {
            repository.insert(Itinerary(
                title = "Weekend Getaway",
                destination = "Kyoto, Japan",
                startDate = System.currentTimeMillis(),
                endDate = System.currentTimeMillis() + (86400000 * 2),
                notes = "Visit shrines and bamboo forest.",
                latitude = 35.0116,
                longitude = 135.7681
            ))
        }
    }
}
