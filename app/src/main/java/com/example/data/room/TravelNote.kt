package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Room entity representing cached voice conversation transcripts,
 * generated travel guides, itineraries, and AI recommendations.
 * Ensures the user can review all their travel notes offline.
 */
@Serializable
@Entity(tableName = "travel_notes")
data class TravelNote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val destination: String = "Global",
    val type: String = NOTE_TYPE_VOICE, // VOICE, GENERATED, ITINERARY, CHAT
    val transcriptOrPrompt: String,
    val aiResponse: String,
    val sourceLanguage: String = "ro",
    val targetLanguage: String = "en",
    val tags: String = "Voce, Offline",
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val NOTE_TYPE_VOICE = "VOICE"
        const val NOTE_TYPE_GENERATED = "GENERATED"
        const val NOTE_TYPE_ITINERARY = "ITINERARY"
        const val NOTE_TYPE_CHAT = "CHAT"
    }
}
