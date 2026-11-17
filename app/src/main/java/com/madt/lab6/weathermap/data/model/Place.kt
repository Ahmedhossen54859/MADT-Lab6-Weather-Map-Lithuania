package com.madt.lab6.weathermap.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a geographic place in Lithuania provided by Meteo.lt API.
 * Endpoint: GET https://api.meteo.lt/v1/places
 */
data class Place(
    @SerializedName("code")
    val code: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("administrativeDivision")
    val administrativeDivision: String?,

    @SerializedName("countryCode")
    val countryCode: String?,

    @SerializedName("coordinates")
    val coordinates: Coordinates
)

data class Coordinates(
    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double
)
