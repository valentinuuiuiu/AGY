package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_phrases")
data class SavedPhrase(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val phonetic: String = "",
    val culturalNote: String = "",
    val category: String = "General", // "Urgențe", "Restaurant", "Transport", "Cazare", "General"
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
