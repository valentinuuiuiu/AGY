package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItineraryDao {
    @Query("SELECT * FROM itineraries ORDER BY timestamp DESC")
    fun getAllItineraries(): Flow<List<Itinerary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItinerary(itinerary: Itinerary)

    @Query("DELETE FROM itineraries WHERE id = :id")
    suspend fun deleteItineraryById(id: Int)
}
