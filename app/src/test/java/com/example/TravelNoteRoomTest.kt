package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.room.AppDatabase
import com.example.data.room.TravelNote
import com.example.data.room.TravelNoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TravelNoteRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TravelNoteRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TravelNoteRepository(db.travelNoteDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveVoiceTranscriptOffline() = runBlocking {
        val note = TravelNote(
            title = "Transcriere Voce: Taxi Tokyo",
            destination = "Tokyo, Japonia",
            type = TravelNote.NOTE_TYPE_VOICE,
            transcriptOrPrompt = "Cât costă o cursă până la stația Shinjuku?",
            aiResponse = "新宿駅までいくらですか？",
            sourceLanguage = "ro",
            targetLanguage = "ja"
        )

        val id = repository.insertNote(note)
        assertTrue(id > 0)

        val retrievedNotes = repository.allNotes.first()
        assertEquals(1, retrievedNotes.size)
        assertEquals("Transcriere Voce: Taxi Tokyo", retrievedNotes[0].title)
        assertEquals(TravelNote.NOTE_TYPE_VOICE, retrievedNotes[0].type)
        assertEquals("Cât costă o cursă până la stația Shinjuku?", retrievedNotes[0].transcriptOrPrompt)

        // Search test
        val searchResults = repository.searchNotes("Shinjuku").first()
        assertEquals(1, searchResults.size)

        // Filter by type
        val voiceNotes = repository.getNotesByType(TravelNote.NOTE_TYPE_VOICE).first()
        assertEquals(1, voiceNotes.size)

        // Delete test
        repository.deleteNoteById(retrievedNotes[0].id)
        val afterDelete = repository.allNotes.first()
        assertEquals(0, afterDelete.size)
    }
}
