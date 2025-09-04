package com.example.weather.data

data class CitySeedResponse(
    val version: Int,
    val cities: List<CitySeedItem>
)
data class CitySeedItem(
    val name: String,
    val pinned: Boolean? = null
)
