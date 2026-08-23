package com.quaderno.appmeteo.data

import com.google.gson.annotations.SerializedName

data class ForecastResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    @SerializedName("current_weather") val currentWeather: CurrentWeather?,
    val hourly: Hourly?,
    val daily: Daily?
)

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int,
    val time: String
)

data class Hourly(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val weathercode: List<Int>
)

data class Daily(
    val time: List<String>,
    val weathercode: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val precipitation_probability_max: List<Int>?
)

// --- Geocoding ---

data class GeocodingResponse(
    val results: List<GeoResult>?
)

data class GeoResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val admin1: String?
)
