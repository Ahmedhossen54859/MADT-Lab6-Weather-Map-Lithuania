package com.madt.lab6.weathermap.data.model

import com.google.gson.annotations.SerializedName

/**
 * Weather forecast response from Meteo.lt API.
 * Endpoint: GET https://api.meteo.lt/v1/places/{place-code}/forecasts/long-term
 *
 * NOTE on update frequency:
 * Meteo.lt produces numerical weather prediction forecasts (from ECMWF/HIRLAM model runs)
 * that update periodically (2 to 4 times per day), distinct from real-time meteorological observations.
 */
data class PlaceForecastResponse(
    @SerializedName("place")
    val place: Place,

    @SerializedName("forecastType")
    val forecastType: String?,

    @SerializedName("forecastCreationTimeUtc")
    val forecastCreationTimeUtc: String?,

    @SerializedName("forecastTimestamps")
    val forecastTimestamps: List<ForecastTimestamp>
)

data class ForecastTimestamp(
    @SerializedName("forecastTimeUtc")
    val forecastTimeUtc: String,

    @SerializedName("airTemperature")
    val airTemperature: Double,

    @SerializedName("feelsLikeTemperature")
    val feelsLikeTemperature: Double?,

    @SerializedName("windSpeed")
    val windSpeed: Double,

    @SerializedName("windGust")
    val windGust: Double?,

    @SerializedName("windDirection")
    val windDirection: Int?,

    @SerializedName("cloudCover")
    val cloudCover: Int?,

    @SerializedName("seaLevelPressure")
    val seaLevelPressure: Int?,

    @SerializedName("relativeHumidity")
    val relativeHumidity: Int?,

    @SerializedName("totalPrecipitation")
    val totalPrecipitation: Double?,

    @SerializedName("conditionCode")
    val conditionCode: String
)
