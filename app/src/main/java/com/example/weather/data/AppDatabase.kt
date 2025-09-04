package com.example.weather.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.*

@Database(entities = [City::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cityDao(): CityDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "weather.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // 앱 최초 생성 시, 원격 시딩 시도
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val provider = CitySeedProvider()
                                if (provider.isEnabled()) {
                                    val seed = provider.fetch()
                                    val cities = seed.cities.map {
                                        City(name = it.name.trim(), pinned = it.pinned ?: false)
                                    }.distinctBy { it.name.lowercase() }
                                    get(context).cityDao().insertAll(cities)
                                } else {
                                    // URL이 없으면 아무 것도 안 넣음(완전 비하드코딩)
                                }
                            } catch (e: Exception) {
                                // 실패 시에도 앱은 구동되도록: 로깅만 하고 넘어감
                                e.printStackTrace()
                            }
                        }
                    }
                }).build().also { INSTANCE = it }
            }
    }
}
