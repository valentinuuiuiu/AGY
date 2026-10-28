package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreSyncRepo"

class FirestoreSyncRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeCloudItineraries(): Flow<List<CloudItinerary>> = flow {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("itineraries")
        collectionRef.snapshots()
            .map { it.toObjects<CloudItinerary>() }
            .catch { e ->
                Log.e(TAG, "Error observing itineraries in Firestore", e)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun saveItinerary(
        title: String,
        destination: String,
        startDate: Long,
        endDate: Long,
        notes: String,
        latitude: Double?,
        longitude: Double?
    ) {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("itineraries")
        val data = hashMapOf(
            "title" to title,
            "destination" to destination,
            "startDate" to startDate,
            "endDate" to endDate,
            "notes" to notes,
            "latitude" to latitude,
            "longitude" to longitude,
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        collectionRef.add(data).await()
    }

    fun observeCloudPhrases(): Flow<List<CloudSavedPhrase>> = flow {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("savedPhrases")
        collectionRef.snapshots()
            .map { it.toObjects<CloudSavedPhrase>() }
            .catch { e ->
                Log.e(TAG, "Error observing saved phrases in Firestore", e)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun savePhrase(
        originalText: String,
        translatedText: String,
        sourceLanguage: String,
        targetLanguage: String,
        phonetic: String,
        culturalNote: String,
        category: String,
        isFavorite: Boolean
    ) {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("savedPhrases")
        val data = hashMapOf(
            "originalText" to originalText,
            "translatedText" to translatedText,
            "sourceLanguage" to sourceLanguage,
            "targetLanguage" to targetLanguage,
            "phonetic" to phonetic,
            "culturalNote" to culturalNote,
            "category" to category,
            "isFavorite" to isFavorite,
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        collectionRef.add(data).await()
    }
}
