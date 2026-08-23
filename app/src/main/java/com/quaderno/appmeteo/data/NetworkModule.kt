package com.quaderno.appmeteo.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    private val weatherRetrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val geocodingRetrofit = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val weatherApi: OpenMeteoApi = weatherRetrofit.create(OpenMeteoApi::class.java)
    val geocodingApi: GeocodingApi = geocodingRetrofit.create(GeocodingApi::class.java)
}
