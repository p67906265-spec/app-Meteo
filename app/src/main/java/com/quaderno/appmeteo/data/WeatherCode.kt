package com.quaderno.appmeteo.data

import com.quaderno.appmeteo.WeatherScene

object WeatherCode {
    fun description(code: Int): String = when (code) {
        0 -> "Sereno"
        1 -> "Prevalentemente sereno"
        2 -> "Parzialmente nuvoloso"
        3 -> "Coperto"
        45, 48 -> "Nebbia"
        51, 53, 55 -> "Pioviggine"
        56, 57 -> "Pioviggine gelata"
        61, 63, 65 -> "Pioggia"
        66, 67 -> "Pioggia gelata"
        71, 73, 75 -> "Neve"
        77 -> "Granelli di neve"
        80, 81, 82 -> "Rovesci di pioggia"
        85, 86 -> "Rovesci di neve"
        95 -> "Temporale"
        96, 99 -> "Temporale con grandine"
        else -> "Condizioni variabili"
    }

    fun emoji(code: Int): String = when (code) {
        0 -> "☀️"
        1 -> "🌤️"
        2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57 -> "🌦️"
        61, 63, 65, 66, 67 -> "🌧️"
        71, 73, 75, 77, 85, 86 -> "🌨️"
        80, 81, 82 -> "🌦️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }

    fun scene(code: Int): WeatherScene = when (code) {
        0, 1 -> WeatherScene.SUN
        2, 3 -> WeatherScene.CLOUDS
        45, 48 -> WeatherScene.FOG
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherScene.RAIN
        71, 73, 75, 77, 85, 86 -> WeatherScene.SNOW
        95, 96, 99 -> WeatherScene.STORM
        else -> WeatherScene.CLOUDS
    }
}
