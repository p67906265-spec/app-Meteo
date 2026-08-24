package com.quaderno.appmeteo.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class WeatherRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val api: OpenMeteoApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OpenMeteoApi::class.java)

    suspend fun fetchForecast(latitude: Double, longitude: Double): OpenMeteoResponse =
        api.getForecast(latitude = latitude, longitude = longitude)
}
