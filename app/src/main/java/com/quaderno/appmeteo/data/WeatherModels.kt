package com.quaderno.appmeteo.data

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class ForecastResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    @SerializedName("current_weather") val currentWeather: LegacyCurrentWeather? = null,
    val current: CurrentSnapshot? = null,
    val hourly: Hourly?,
    val daily: Daily?
)

data class LegacyCurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int,
    val time: String
) {
    fun toSnapshot(): CurrentSnapshot = CurrentSnapshot(
        time = runCatching { LocalDateTime.parse(time) }.getOrElse { LocalDateTime.now() },
        temperature = temperature,
        apparentTemperature = temperature,
        weatherCode = weathercode,
        windSpeed = windspeed,
        relativeHumidity = null,
        precipitation = null,
        rain = null,
        isDay = 1
    )
}

data class CurrentSnapshot(
    @SerializedName("time") private val timeRaw: String,
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("apparent_temperature") val apparentTemperature: Double?,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("wind_speed_10m") val windSpeed: Double,
    @SerializedName("relative_humidity_2m") val relativeHumidity: Int?,
    val precipitation: Double?,
    val rain: Double?,
    @SerializedName("is_day") val isDay: Int?
) {
    val time: LocalDateTime
        get() = runCatching { LocalDateTime.parse(timeRaw) }.getOrElse { LocalDateTime.now() }
}

data class Hourly(
    val time: List<String>,
    val temperature_2m: List<Double>,
    @SerializedName("weather_code") val weatherCode: List<Int>
)

data class Daily(
    val time: List<String>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
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
