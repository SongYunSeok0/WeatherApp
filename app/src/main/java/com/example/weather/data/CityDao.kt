package com.example.weather.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CityDao {
    @Query("SELECT * FROM cities ORDER BY pinned DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<City>>

    @Query("SELECT * FROM cities WHERE pinned = 1 ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observePinned(): Flow<List<City>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cities: List<City>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(city: City): Long

    @Query("SELECT * FROM cities WHERE name = :q COLLATE NOCASE or name_ko = :q LIMIT 1")
    suspend fun findByAnyName(q: String): City?

    @Query("SELECT * FROM cities WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): City?

    @Update
    suspend fun update(city: City)

    @Query("UPDATE cities SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("SELECT COUNT(*) FROM cities")
    suspend fun count(): Long

    @Query("UPDATE cities SET sortOrder = :order WHERE id = :id")
    suspend fun updateOrder(id: Long, order: Int)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM cities")
    suspend fun getMaxOrder(): Int

    @Query("SELECT * FROM cities WHERE name LIKE :like OR name_ko LIKE :like ORDER BY pinned DESC, sortOrder ASC, name COLLATE NOCASE ASC LIMIT 20")
    suspend fun suggestByAnyNameLike(like: String): List<City>

    @Query("UPDATE cities SET name_ko = :nameKo, country = :country WHERE id = :id")
    suspend fun updateKoAndCountry(id: Long, nameKo: String?, country: String?)
}
