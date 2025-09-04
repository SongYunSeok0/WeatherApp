package com.example.weather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class WeatherDTO(
    val name: String,
    val weather: List<Weather>,
    val main: Main,
    val sys: Sys?
)

data class Weather(val description: String, val icon: String)
data class Main(val temp: Float)
data class Sys(val country: String?)

data class GeoResult(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String?,
    val state: String?,
    val local_names: Map<String, String>?
)

interface WeatherApi {
    @GET("geo/1.0/direct")
    suspend fun geocode(
        @Query("q") query: String,
        @Query("limit") limit: Int = 1,
        @Query("appid") apiKey: String
    ): List<GeoResult>

    @GET("data/2.5/weather")
    suspend fun currentByCoord(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "kr"
    ): WeatherDTO

    @GET("data/2.5/weather")
    suspend fun currentWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "kr"
    ): WeatherDTO
}

object RetrofitClient {
    private const val BASE_URL = "https://api.openweathermap.org/"

    private val client = okhttp3.OkHttpClient.Builder().build()

    val api: WeatherApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApi::class.java)
}

class WeatherRepository(private val api: WeatherApi = RetrofitClient.api) {

    suspend fun getCurrentWithGeo(city: String): Pair<WeatherDTO, GeoResult?> {
        val key = BuildConfig.OWM_API_KEY
        val geo = api.geocode(city, limit = 1, apiKey = key)
        val g = geo.firstOrNull()
        val weather = if (g != null) {
            api.currentByCoord(g.lat, g.lon, key, units = "metric", lang = "kr")
        } else {
            api.currentWeather(city, key, units = "metric", lang = "kr")
        }
        return weather to g
    }
}