package com.madt.lab6.weathermap.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madt.lab6.weathermap.data.model.ForecastTimestamp
import com.madt.lab6.weathermap.data.model.LocationWeather
import com.madt.lab6.weathermap.data.model.Place
import com.madt.lab6.weathermap.data.model.PlaceForecastResponse
import com.madt.lab6.weathermap.data.repository.AuthRepository
import com.madt.lab6.weathermap.data.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WeatherMapViewModel(
    private val weatherRepository: WeatherRepository = WeatherRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _allLocations = mutableListOf<LocationWeather>()

    private val _filteredLocations = MutableLiveData<List<LocationWeather>>(emptyList())
    val filteredLocations: LiveData<List<LocationWeather>> = _filteredLocations

    private val _selectedDetail = MutableLiveData<Pair<Place, PlaceForecastResponse?>?>()
    val selectedDetail: LiveData<Pair<Place, PlaceForecastResponse?>?> = _selectedDetail

    private val _isLoadingPlaces = MutableLiveData<Boolean>(false)
    val isLoadingPlaces: LiveData<Boolean> = _isLoadingPlaces

    private val _isDetailLoading = MutableLiveData<Boolean>(false)
    val isDetailLoading: LiveData<Boolean> = _isDetailLoading

    private val _syncProgress = MutableLiveData<Pair<Int, Int>?>()
    val syncProgress: LiveData<Pair<Int, Int>?> = _syncProgress

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoggedOut = MutableLiveData<Boolean>(false)
    val isLoggedOut: LiveData<Boolean> = _isLoggedOut

    val currentUserEmail: String
        get() = authRepository.currentUser?.email.orEmpty()

    private var currentSearchQuery = ""
    private var currentConditionFilter = 0 // 0 = All

    private var backgroundSyncJob: Job? = null

    init {
        loadPlaces()
    }

    /**
     * Phase 1: Load all places and immediately display map markers.
     */
    fun loadPlaces() {
        if (!authRepository.isAuthenticated) {
            _isLoggedOut.value = true
            return
        }

        backgroundSyncJob?.cancel()
        _isLoadingPlaces.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = weatherRepository.getPlaces()
            _isLoadingPlaces.value = false

            result.fold(
                onSuccess = { places ->
                    _allLocations.clear()
                    _allLocations.addAll(places.map { LocationWeather(it) })
                    applyFilters()
                    startBackgroundSync(places)
                },
                onFailure = { error ->
                    if (error is SecurityException) {
                        _isLoggedOut.value = true
                    } else {
                        _errorMessage.value = error.localizedMessage ?: "Failed to load places"
                    }
                }
            )
        }
    }

    /**
     * Phase 2: Controlled background weather synchronization.
     * Fetches forecast data concurrently in chunks (controlled concurrency = 3),
     * updating location data and progress without overwhelming the network or device.
     */
    private fun startBackgroundSync(places: List<Place>) {
        backgroundSyncJob?.cancel()
        backgroundSyncJob = viewModelScope.launch {
            val total = places.size
            var completed = 0
            _syncProgress.postValue(Pair(completed, total))

            // Chunk requests into batches of 3 for rate-limiting and stability
            val chunks = places.chunked(3)
            for (chunk in chunks) {
                if (!isActive) break

                val deferreds = chunk.map { place ->
                    async {
                        val forecastResult = weatherRepository.getForecastForPlace(place.code)
                        forecastResult.getOrNull()
                    }
                }

                val results = deferreds.awaitAll()

                chunk.forEachIndexed { index, place ->
                    val forecast = results[index]
                    val latestTimestamp = forecast?.forecastTimestamps?.firstOrNull()
                    if (latestTimestamp != null) {
                        updateLocationWeather(place.code, latestTimestamp)
                    }
                    completed++
                }

                _syncProgress.postValue(Pair(completed, total))
                applyFilters()
            }

            _syncProgress.postValue(null)
        }
    }

    private fun updateLocationWeather(placeCode: String, forecast: ForecastTimestamp) {
        val index = _allLocations.indexOfFirst { it.place.code == placeCode }
        if (index != -1) {
            val current = _allLocations[index]
            _allLocations[index] = current.copy(currentForecast = forecast)
        }
    }

    fun onSearchQueryChanged(query: String) {
        currentSearchQuery = query
        applyFilters()
    }

    fun onConditionFilterSelected(filterId: Int) {
        currentConditionFilter = filterId
        applyFilters()
    }

    private fun applyFilters() {
        val filtered = _allLocations.filter { item ->
            item.matchesSearchQuery(currentSearchQuery) &&
                    item.matchesConditionFilter(currentConditionFilter)
        }
        _filteredLocations.postValue(filtered)
    }

    /**
     * Fetch or retrieve from cache the detailed forecast for a clicked place marker.
     */
    fun selectPlace(place: Place) {
        _isDetailLoading.value = true
        _selectedDetail.value = Pair(place, null)

        viewModelScope.launch {
            val result = weatherRepository.getForecastForPlace(place.code)
            _isDetailLoading.value = false
            result.fold(
                onSuccess = { forecast ->
                    _selectedDetail.value = Pair(place, forecast)
                },
                onFailure = { error ->
                    _selectedDetail.value = Pair(place, null)
                    _errorMessage.value = error.localizedMessage
                }
            )
        }
    }

    fun clearSelectedPlace() {
        _selectedDetail.value = null
    }

    /**
     * Complete cleanup on logout:
     * - Cancels background jobs
     * - Clears repository cache
     * - Signs out from Firebase
     * - Notifies UI to return to AuthActivity
     */
    fun logout() {
        backgroundSyncJob?.cancel()
        weatherRepository.onUserSignOut()
        authRepository.signOut()
        _isLoggedOut.value = true
    }
}
