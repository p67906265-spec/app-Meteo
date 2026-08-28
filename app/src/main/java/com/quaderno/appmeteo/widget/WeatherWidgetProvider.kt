package com.quaderno.appmeteo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.quaderno.appmeteo.MainActivity
import com.quaderno.appmeteo.R
import com.quaderno.appmeteo.data.WeatherCode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) updateAll(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.quaderno.appmeteo.widget.REFRESH"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, WeatherWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_weather_2x1)
            val cached = WeatherCache.read(context)

            val launch = PendingIntent.getActivity(
                context, 1000 + id,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, launch)

            if (cached == null) {
                views.setTextViewText(R.id.widget_city, "Apri Meteo")
                views.setTextViewText(R.id.widget_temperature, "--°")
                views.setTextViewText(R.id.widget_condition, "Nessun dato")
                views.setTextViewText(R.id.widget_minmax, "--° / --°")
                views.setTextViewText(R.id.widget_date, "--")
                views.setTextViewText(R.id.widget_location, "Posizione")
                views.setTextViewText(R.id.widget_weather_icon, "☀️")
            } else {
                val forecast = cached.forecast
                val current = forecast.currentWeather
                val daily = forecast.daily
                val temp = current?.temperature?.roundToInt()
                val code = current?.weathercode ?: -1
                val min = daily?.temperature_2m_min?.firstOrNull()?.roundToInt()
                val max = daily?.temperature_2m_max?.firstOrNull()?.roundToInt()

                val now = try {
                    LocalDateTime.parse(current?.time ?: "")
                } catch (_: Exception) { null }

                views.setTextViewText(R.id.widget_temperature, "${temp ?: "--"}°")
                views.setTextViewText(R.id.widget_condition, WeatherCode.description(code))
                views.setTextViewText(R.id.widget_weather_icon, WeatherCode.emoji(code))
                views.setTextViewText(R.id.widget_minmax, "${min ?: "--"}° / ${max ?: "--"}°")
                views.setTextViewText(
                    R.id.widget_date,
                    now?.format(DateTimeFormatter.ofPattern("d/M EEE", Locale.ITALIAN)) ?: "--"
                )
                views.setTextViewText(R.id.widget_location, cached.cityName.substringBefore(","))
            }

            manager.updateAppWidget(id, views)
        }
    }
}
