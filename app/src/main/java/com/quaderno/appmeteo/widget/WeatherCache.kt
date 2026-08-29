package com.quaderno.appmeteo.widget

import android.content.Context

data class WeatherWidgetData(
    val city: String,
    val temperature: Int,
    val weatherCode: Int,
    val min: Int,
    val max: Int,
    val condition: String
)

object WeatherCache {
    private const val PREFS = "weather_widget"
    private const val CITY = "city"
    private const val TEMP = "temp"
    private const val CODE = "code"
    private const val MIN = "min"
    private const val MAX = "max"
    private const val CONDITION = "condition"

    fun save(
        context: Context,
        city: String,
        temperature: Double,
        weatherCode: Int,
        min: Double,
        max: Double,
        condition: String
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CITY, city)
            .putInt(TEMP, temperature.toInt())
            .putInt(CODE, weatherCode)
            .putInt(MIN, min.toInt())
            .putInt(MAX, max.toInt())
            .putString(CONDITION, condition)
            .apply()
    }

    fun read(context: Context): WeatherWidgetData {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return WeatherWidgetData(
            city = p.getString(CITY, "Meteo") ?: "Meteo",
            temperature = p.getInt(TEMP, 0),
            weatherCode = p.getInt(CODE, 0),
            min = p.getInt(MIN, 0),
            max = p.getInt(MAX, 0),
            condition = p.getString(CONDITION, "Nessun dato") ?: "Nessun dato"
        )
    }
}
