package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPhraseDao {
    @Query("SELECT * FROM saved_phrases ORDER BY isFavorite DESC, timestamp DESC")
    fun getAllPhrases(): Flow<List<SavedPhrase>>

    @Query("SELECT * FROM saved_phrases WHERE category = :category ORDER BY timestamp DESC")
    fun getPhrasesByCategory(category: String): Flow<List<SavedPhrase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: SavedPhrase): Long

    @Query("DELETE FROM saved_phrases WHERE id = :id")
    suspend fun deletePhrase(id: Int)

    @Query("UPDATE saved_phrases SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFav: Boolean)
}
