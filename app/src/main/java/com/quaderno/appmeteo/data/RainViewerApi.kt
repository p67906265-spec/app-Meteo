package com.quaderno.appmeteo.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

data class RainViewerResponse(
    val version: String,
    val generated: Long,
    val host: String,
    val radar: RainViewerRadar
)

data class RainViewerRadar(
    val past: List<RainViewerFrame>
)

data class RainViewerFrame(
    val time: Long,
    val path: String
)

interface RainViewerApi {
    @GET("public/weather-maps.json")
    suspend fun getWeatherMaps(): RainViewerResponse
}

object RainViewerNetwork {
    val api: RainViewerApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.rainviewer.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RainViewerApi::class.java)
    }
}
