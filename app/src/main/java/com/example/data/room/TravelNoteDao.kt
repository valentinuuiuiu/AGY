package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelNoteDao {

    @Query("SELECT * FROM travel_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<TravelNote>>

    @Query("SELECT * FROM travel_notes WHERE type = :type ORDER BY timestamp DESC")
    fun getNotesByType(type: String): Flow<List<TravelNote>>

    @Query("SELECT * FROM travel_notes WHERE title LIKE '%' || :query || '%' OR transcriptOrPrompt LIKE '%' || :query || '%' OR aiResponse LIKE '%' || :query || '%' OR destination LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchNotes(query: String): Flow<List<TravelNote>>

    @Query("SELECT * FROM travel_notes WHERE id = :id")
    suspend fun getNoteById(id: Int): TravelNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: TravelNote): Long

    @Update
    suspend fun updateNote(note: TravelNote)

    @Query("UPDATE travel_notes SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFav: Boolean)

    @Query("DELETE FROM travel_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    @Query("DELETE FROM travel_notes")
    suspend fun clearAllNotes()

    @Query("SELECT COUNT(*) FROM travel_notes")
    fun getNoteCount(): Flow<Int>
}
