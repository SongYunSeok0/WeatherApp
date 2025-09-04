package com.example.weather.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CityDao {
    @Query("SELECT * FROM cities ORDER BY pinned DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<City>>

    @Query("SELECT * FROM cities WHERE pinned = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observePinned(): Flow<List<City>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cities: List<City>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(city: City): Long

    @Query("SELECT * FROM cities WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): City?

    @Query("UPDATE cities SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("SELECT COUNT(*) FROM cities")
    suspend fun count(): Long
}
