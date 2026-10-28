package com.example.data.room

import kotlinx.coroutines.flow.Flow

/**
 * Repository pattern implementation for managing cached travel notes,
 * past voice conversation transcripts, and generated travel recommendations.
 */
class TravelNoteRepository(private val travelNoteDao: TravelNoteDao) {

    val allNotes: Flow<List<TravelNote>> = travelNoteDao.getAllNotes()

    fun getNotesByType(type: String): Flow<List<TravelNote>> = travelNoteDao.getNotesByType(type)

    fun searchNotes(query: String): Flow<List<TravelNote>> = travelNoteDao.searchNotes(query)

    val noteCount: Flow<Int> = travelNoteDao.getNoteCount()

    suspend fun getNoteById(id: Int): TravelNote? = travelNoteDao.getNoteById(id)

    suspend fun insertNote(note: TravelNote): Long = travelNoteDao.insertNote(note)

    suspend fun updateNote(note: TravelNote) = travelNoteDao.updateNote(note)

    suspend fun toggleFavorite(id: Int, isFav: Boolean) = travelNoteDao.updateFavorite(id, isFav)

    suspend fun deleteNoteById(id: Int) = travelNoteDao.deleteNoteById(id)

    suspend fun clearAllNotes() = travelNoteDao.clearAllNotes()
}
