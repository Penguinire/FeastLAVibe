package com.example.feastvibe

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WeatherRepository {

    private val api: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    suspend fun getTemperature(point: GeoPoint): Double? {
        return try {
            val response = api.getCurrentWeather(
                latitude = point.latitude,
                longitude = point.longitude
            )

            response.current?.temperature
        } catch (exception: Exception) {
            null
        }
    }
}