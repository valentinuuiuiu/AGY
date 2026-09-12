package com.example.data.room

import kotlinx.coroutines.flow.Flow

class ItineraryRepository(private val itineraryDao: ItineraryDao) {
    val allItineraries: Flow<List<Itinerary>> = itineraryDao.getAllItineraries()

    suspend fun insert(itinerary: Itinerary) = itineraryDao.insertItinerary(itinerary)

    suspend fun deleteById(id: Int) = itineraryDao.deleteItineraryById(id)
}
