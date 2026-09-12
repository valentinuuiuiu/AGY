package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "itineraries")
data class Itinerary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val destination: String,
    val startDate: Long,
    val endDate: Long,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
