package com.madt.lab6.weathermap.data.api

import com.madt.lab6.weathermap.data.model.Place
import com.madt.lab6.weathermap.data.model.PlaceForecastResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for Meteo.lt API.
 * Base URL: https://api.meteo.lt/
 */
interface MeteoApiService {

    /**
     * Retrieves all available places in Lithuania with coordinates.
     */
    @GET("v1/places")
    suspend fun getPlaces(): List<Place>

    /**
     * Retrieves long-term forecast timestamps for a specific location.
     */
    @GET("v1/places/{placeCode}/forecasts/long-term")
    suspend fun getLongTermForecast(
        @Path("placeCode") placeCode: String
    ): PlaceForecastResponse
}
