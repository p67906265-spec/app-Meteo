package com.quaderno.appmeteo.widget

import android.content.Context

/**
 * Cache minimale condivisa tra app e widget.
 * Non dipende da Gson o dai modelli dell'app: salva solo i valori necessari al widget.
 */
object WeatherCache {
    private const val PREFS = "weather_widget_cache"
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
        min: Double?,
        max: Double?,
        condition: String
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CITY, city)
            .putFloat(TEMP, temperature.toFloat())
            .putInt(CODE, weatherCode)
            .putString(MIN, min?.toString())
            .putString(MAX, max?.toString())
            .putString(CONDITION, condition)
            .apply()
    }

    data class Data(
        val city: String,
        val temperature: Float,
        val weatherCode: Int,
        val min: Float?,
        val max: Float?,
        val condition: String
    )

    fun read(context: Context): Data? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(TEMP)) return null
        return Data(
            city = prefs.getString(CITY, "Posizione attuale") ?: "Posizione attuale",
            temperature = prefs.getFloat(TEMP, Float.NaN),
            weatherCode = prefs.getInt(CODE, -1),
            min = prefs.getString(MIN, null)?.toFloatOrNull(),
            max = prefs.getString(MAX, null)?.toFloatOrNull(),
            condition = prefs.getString(CONDITION, "") ?: ""
        )
    }
}
