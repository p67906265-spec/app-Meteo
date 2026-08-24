package com.quaderno.appmeteo.data

import com.google.gson.annotations.SerializedName

data class ForecastResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val current: CurrentWeather?,
    val hourly: Hourly?,
    val daily: Daily?
)

data class CurrentWeather(
    val time: String,
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("apparent_temperature") val apparentTemperature: Double?,
    @SerializedName("relative_humidity_2m") val humidity: Int?,
    @SerializedName("weather_code") val weathercode: Int,
    @SerializedName("wind_speed_10m") val windspeed: Double,
    @SerializedName("wind_direction_10m") val windDirection: Int?,
    @SerializedName("surface_pressure") val pressure: Double?,
    @SerializedName("is_day") val isDay: Int?
)

data class Hourly(
    val time: List<String>,
    val temperature_2m: List<Double>,
    @SerializedName("weather_code") val weathercode: List<Int>
)

data class Daily(
    val time: List<String>,
    @SerializedName("weather_code") val weathercode: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val precipitation_probability_max: List<Int>?
)

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
