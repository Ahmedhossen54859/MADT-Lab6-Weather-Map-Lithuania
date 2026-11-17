package com.madt.lab6.weathermap.data.repository

import com.madt.lab6.weathermap.data.api.MeteoApiService
import com.madt.lab6.weathermap.data.api.RetrofitClient
import com.madt.lab6.weathermap.data.model.Place
import com.madt.lab6.weathermap.data.model.PlaceForecastResponse
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository responsible for fetching, caching, and managing weather data.
 *
 * FIREBASE AUTHENTICATION ENFORCEMENT:
 * Evaluates Firebase authentication status prior to initiating any network requests.
 * Note: While Meteo.lt is an open public API and does not validate Firebase JWTs on the server side,
 * this client-side gate enforces that within the application, only an authenticated user
 * session can dispatch requests to the remote API.
 *
 * CACHING POLICY:
 * In-memory cache with an application-level Time-To-Live (TTL) of 20 minutes.
 * This is an application performance and bandwidth optimization policy, not the underlying
 * Meteo.lt NWP model update frequency (which updates 2-4 times daily).
 */
class WeatherRepository(
    private val apiService: MeteoApiService = RetrofitClient.apiService,
    private val authRepository: AuthRepository = AuthRepository()
) {

    data class CachedForecast(
        val forecast: PlaceForecastResponse,
        val timestampMillis: Long
    ) {
        fun isExpired(ttlMillis: Long = CACHE_TTL_MILLIS): Boolean {
            return (System.currentTimeMillis() - timestampMillis) > ttlMillis
        }
    }

    companion object {
        // Application-level caching policy: 20 minutes TTL
        const val CACHE_TTL_MILLIS = 20 * 60 * 1000L
        // Concurrency limit: maximum 3 concurrent HTTP requests
        private const val MAX_CONCURRENT_REQUESTS = 3
    }

    private val forecastCache = ConcurrentHashMap<String, CachedForecast>()
    private val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

    // Supervisor job managing background sync tasks, cancelable upon logout
    private var repositoryJob: CompletableJob = SupervisorJob()
    private var repositoryScope: CoroutineScope = CoroutineScope(Dispatchers.IO + repositoryJob)

    private fun ensureAuthenticated() {
        if (!authRepository.isAuthenticated) {
            throw SecurityException("Unauthorized: Only authenticated Firebase users can send API requests.")
        }
    }

    /**
     * Retrieves all available Lithuanian places from Meteo.lt.
     */
    suspend fun getPlaces(): Result<List<Place>> = withContext(Dispatchers.IO) {
        try {
            ensureAuthenticated()
            val places = apiService.getPlaces()
            Result.success(places)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves the forecast for a specific place.
     * Checks in-memory cache first to avoid duplicate network calls.
     */
    suspend fun getForecastForPlace(
        placeCode: String,
        forceRefresh: Boolean = false
    ): Result<PlaceForecastResponse> = withContext(Dispatchers.IO) {
        try {
            ensureAuthenticated()

            // 1. Inspect Cache
            if (!forceRefresh) {
                val cached = forecastCache[placeCode]
                if (cached != null && !cached.isExpired()) {
                    return@withContext Result.success(cached.forecast)
                }
            }

            // 2. Fetch with Concurrency Control
            val response = semaphore.withPermit {
                apiService.getLongTermForecast(placeCode)
            }

            // 3. Store in Cache
            forecastCache[placeCode] = CachedForecast(
                forecast = response,
                timestampMillis = System.currentTimeMillis()
            )

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns cached forecast if available and unexpired, without making a network call.
     */
    fun getCachedForecast(placeCode: String): PlaceForecastResponse? {
        val cached = forecastCache[placeCode] ?: return null
        return if (!cached.isExpired()) cached.forecast else null
    }

    /**
     * Performs session cleanup on user sign-out:
     * - Cancels all active background weather tasks immediately.
     * - Clears the in-memory weather cache.
     * - Resets the coroutine scope for future sessions.
     */
    fun onUserSignOut() {
        repositoryJob.cancel()
        forecastCache.clear()
        // Reinitialize job and scope for any subsequent authenticated session
        repositoryJob = SupervisorJob()
        repositoryScope = CoroutineScope(Dispatchers.IO + repositoryJob)
    }
}
