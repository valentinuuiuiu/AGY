package com.example.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CloudItinerary(
    @DocumentId val id: String = "",
    val title: String = "",
    val destination: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudSavedPhrase(
    @DocumentId val id: String = "",
    val originalText: String = "",
    val translatedText: String = "",
    val sourceLanguage: String = "",
    val targetLanguage: String = "",
    val phonetic: String = "",
    val culturalNote: String = "",
    val category: String = "General",
    val isFavorite: Boolean = false,
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
