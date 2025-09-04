package com.example.weather.data

import com.example.weather.BuildConfig
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request

class CitySeedProvider(
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
    private val url: String = BuildConfig.CITY_SEED_URL
) {

    fun isEnabled(): Boolean = url.isNotBlank()

    @Throws(Exception::class)
    fun fetch(): CitySeedResponse {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Seed HTTP ${resp.code}")
            }
            val body = resp.body?.string() ?: throw IllegalStateException("Empty body")
            return gson.fromJson(body, CitySeedResponse::class.java)
        }
    }
}
