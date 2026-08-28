package com.quaderno.appmeteo.widget

import android.content.Context
import com.google.gson.Gson
import com.quaderno.appmeteo.data.ForecastResponse

/** Cache condivisa tra app e widget. Il widget legge gli ultimi dati caricati dall'app. */
object WeatherCache {
    private const val PREFS = "weather_cache"
    private const val KEY_FORECAST = "forecast_json"
    private const val KEY_CITY = "city_name"
    private val gson = Gson()

    fun save(context: Context, forecast: ForecastResponse, cityName: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FORECAST, gson.toJson(forecast))
            .putString(KEY_CITY, cityName)
            .apply()
    }

    fun read(context: Context): CachedWeather? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FORECAST, null) ?: return null
        return try {
            CachedWeather(
                forecast = gson.fromJson(json, ForecastResponse::class.java),
                cityName = prefs.getString(KEY_CITY, "Posizione attuale") ?: "Posizione attuale"
            )
        } catch (_: Exception) {
            null
        }
    }
}

data class CachedWeather(
    val forecast: ForecastResponse,
    val cityName: String
)
