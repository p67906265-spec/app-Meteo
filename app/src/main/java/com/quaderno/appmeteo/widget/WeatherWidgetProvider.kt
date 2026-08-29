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

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
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
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return

            val data = WeatherCache.read(context)

            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.weather_widget)
                views.setTextViewText(R.id.widget_city, data.city)
                views.setTextViewText(R.id.widget_temp, "${data.temperature}°")
                views.setTextViewText(R.id.widget_range, "${data.min}° / ${data.max}°")
                views.setTextViewText(R.id.widget_condition, data.condition)
                views.setTextViewText(R.id.widget_icon, WeatherCode.emoji(data.weatherCode))

                val openIntent = Intent(context, MainActivity::class.java)
                val pending = PendingIntent.getActivity(
                    context, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pending)

                manager.updateAppWidget(id, views)
            }
        }
    }
}
