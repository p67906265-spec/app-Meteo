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
        if (intent.action == ACTION_REFRESH || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            updateAll(context)
        }
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
            val data = WeatherCache.read(context)
            val now = LocalDateTime.now()

            val launch = PendingIntent.getActivity(
                context,
                1000 + id,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, launch)

            views.setTextViewText(R.id.widget_hour_1, now.format(DateTimeFormatter.ofPattern("HH")))
            views.setTextViewText(R.id.widget_hour_2, now.format(DateTimeFormatter.ofPattern("mm")))
            views.setTextViewText(
                R.id.widget_date,
                now.format(DateTimeFormatter.ofPattern("d/M EEE", Locale.ITALIAN))
            )

            if (data == null) {
                views.setTextViewText(R.id.widget_temperature, "--°")
                views.setTextViewText(R.id.widget_condition, "Apri Meteo")
                views.setTextViewText(R.id.widget_minmax, "--° / --°")
                views.setTextViewText(R.id.widget_location, "Posizione")
                views.setTextViewText(R.id.widget_weather_icon, "☀️")
            } else {
                val temp = data.temperature.roundToInt()
                val min = data.min?.roundToInt()?.toString() ?: "--"
                val max = data.max?.roundToInt()?.toString() ?: "--"
                views.setTextViewText(R.id.widget_temperature, "$temp°")
                views.setTextViewText(R.id.widget_condition, data.condition.ifBlank { WeatherCode.description(data.weatherCode) })
                views.setTextViewText(R.id.widget_minmax, "$min° / $max°")
                views.setTextViewText(R.id.widget_location, data.city.substringBefore(",").ifBlank { "Posizione attuale" })
                views.setTextViewText(R.id.widget_weather_icon, WeatherCode.emoji(data.weatherCode))
            }

            manager.updateAppWidget(id, views)
        }
    }
}
