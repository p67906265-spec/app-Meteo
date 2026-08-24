package com.quaderno.appmeteo.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponse(
    @SerialName("current_weather") val currentWeather: CurrentWeather,
    val hourly: HourlyData,
    val daily: DailyData
)

@Serializable
data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int,
    val time: String
)

@Serializable
data class HourlyData(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature2m: List<Double>,
    @SerialName("weathercode") val weatherCode: List<Int>
)

@Serializable
data class DailyData(
    val time: List<String>,
    @SerialName("weathercode") val weatherCode: List<Int>,
    @SerialName("temperature_2m_max") val tempMax: List<Double>,
    @SerialName("temperature_2m_min") val tempMin: List<Double>
)

/** Condizione meteo leggibile, ricavata dal codice WMO restituito da Open-Meteo. */
enum class Condition { SOLE, POCO_NUVOLOSO, NUVOLOSO, PIOGGIA, TEMPORALE, NEVE, NEBBIA }

fun weatherCodeToCondition(code: Int): Condition = when (code) {
    0 -> Condition.SOLE
    1, 2 -> Condition.POCO_NUVOLOSO
    3 -> Condition.NUVOLOSO
    45, 48 -> Condition.NEBBIA
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> Condition.PIOGGIA
    71, 73, 75, 77, 85, 86 -> Condition.NEVE
    95, 96, 99 -> Condition.TEMPORALE
    else -> Condition.NUVOLOSO
}

fun Condition.label(): String = when (this) {
    Condition.SOLE -> "Sereno"
    Condition.POCO_NUVOLOSO -> "Poco nuvoloso"
    Condition.NUVOLOSO -> "Coperto"
    Condition.PIOGGIA -> "Pioggia"
    Condition.TEMPORALE -> "Temporale"
    Condition.NEVE -> "Neve"
    Condition.NEBBIA -> "Nebbia"
}
