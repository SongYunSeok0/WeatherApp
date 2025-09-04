package com.example.weather.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "cities",
        indices = [Index(value = ["name"], unique = true)]
)
data class City(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pinned: Boolean = false
)