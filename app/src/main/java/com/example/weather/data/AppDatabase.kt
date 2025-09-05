package com.example.weather.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(entities = [City::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cityDao(): CityDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // v1 → v2 : sortOrder 추가
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE cities " +
                    "ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // v2 → v3 : name_ko, country 추가
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cities ADD COLUMN name_ko TEXT")
                db.execSQL("ALTER TABLE cities ADD COLUMN country TEXT")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "weather.db" // ✅ 이름 고정
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // ✅ 마이그 등록
                // .fallbackToDestructiveMigration() // (개발 중 임시 우회가 필요하면 잠깐만 켜기)
                .build()
                .also { INSTANCE = it }
            }
    }
}
