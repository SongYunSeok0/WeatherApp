// City.kt
package com.example.weather.data

import androidx.room.*

@Entity(
    tableName = "cities",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["name_ko"]) // 한국어 검색 성능 위해 인덱스
    ]
)
data class City(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                               // 카논 이름(OWM name)
    val pinned: Boolean = false,
    @ColumnInfo(defaultValue = "0") val sortOrder: Int = 0,

    @ColumnInfo(name = "name_ko") val nameKo: String? = null,   // ✅ 추가
    @ColumnInfo(name = "country") val country: String? = null   // ✅ 추가
)
