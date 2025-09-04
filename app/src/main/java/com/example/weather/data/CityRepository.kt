package com.example.weather.data

import kotlinx.coroutines.flow.Flow

class CityRepository(private val dao: CityDao) {
    fun observeAll(): Flow<List<City>> = dao.observeAll()
    fun observePinned(): Flow<List<City>> = dao.observePinned()

    suspend fun insertAll(cities: List<City>) = dao.insertAll(cities)
    suspend fun togglePin(city: City) = dao.setPinned(city.id, !city.pinned)

    suspend fun togglePinByName(name: String) {
        val key = name.trim()
        val existing = dao.findByName(key)
        if (existing == null) {
            dao.insert(City(name = key, pinned = true))
        } else {
            dao.setPinned(existing.id, !existing.pinned)
        }
    }

    suspend fun count(): Long = dao.count()
}
