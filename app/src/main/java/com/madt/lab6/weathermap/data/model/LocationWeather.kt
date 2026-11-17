package com.madt.lab6.weathermap.data.model

/**
 * Combined presentation model representing a location and its current/latest forecast.
 */
data class LocationWeather(
    val place: Place,
    val currentForecast: ForecastTimestamp? = null,
    val isForecastLoading: Boolean = false
) {
    /**
     * Checks if this location matches a text search query (against city name or administrative division).
     */
    fun matchesSearchQuery(query: String): Boolean {
        if (query.isBlank()) return true
        val cleanQuery = query.trim().lowercase()
        val nameMatches = place.name.lowercase().contains(cleanQuery)
        val divisionMatches = place.administrativeDivision?.lowercase()?.contains(cleanQuery) == true
        return nameMatches || divisionMatches
    }

    /**
     * Checks if this location matches a selected weather condition filter.
     * Filter categories: ALL, CLEAR, ISOLATED_CLOUDS, SCATTERED_CLOUDS, OVERCAST, LIGHT_RAIN, RAIN, SNOW, FOG.
     */
    fun matchesConditionFilter(filterId: Int): Boolean {
        // Filter ID 0 = ALL conditions
        if (filterId == 0) return true

        // If forecast has not yet been loaded for this location, keep it visible under ALL only
        val code = currentForecast?.conditionCode?.lowercase() ?: return false

        return when (filterId) {
            1 -> code == "clear"
            2 -> code == "isolated-clouds"
            3 -> code == "scattered-clouds"
            4 -> code == "overcast"
            5 -> code == "light-rain"
            6 -> code in listOf("moderate-rain", "heavy-rain")
            7 -> code in listOf("light-snow", "moderate-snow", "heavy-snow", "sleet")
            8 -> code == "fog"
            else -> true
        }
    }
}
