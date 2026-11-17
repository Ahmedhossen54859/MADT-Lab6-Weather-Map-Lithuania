package com.madt.lab6.weathermap.ui.map

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.madt.lab6.weathermap.R
import com.madt.lab6.weathermap.data.model.ForecastTimestamp
import com.madt.lab6.weathermap.data.model.LocationWeather
import com.madt.lab6.weathermap.data.model.Place
import com.madt.lab6.weathermap.databinding.ActivityMapBinding
import com.madt.lab6.weathermap.ui.auth.AuthActivity

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private val viewModel: WeatherMapViewModel by viewModels()

    private var googleMap: GoogleMap? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<MaterialCardView>

    // Maps placeCode -> Google Maps Marker
    private val activeMarkers = HashMap<String, Marker>()
    // Maps Marker -> LocationWeather for fast click lookups
    private val markerLocationMap = HashMap<Marker, LocationWeather>()

    companion object {
        // Center of Lithuania
        private val LITHUANIA_CENTER = LatLng(55.1694, 23.8813)
        private const val DEFAULT_ZOOM = 7.2f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMap)

        setupBottomSheet()
        setupSearchAndFilterUI()
        setupMap()
        observeViewModel()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isCompassEnabled = true

        // Move camera to center of Lithuania
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LITHUANIA_CENTER, DEFAULT_ZOOM))

        // Marker Click Listener
        googleMap?.setOnMarkerClickListener { marker ->
            val location = markerLocationMap[marker]
            if (location != null) {
                viewModel.selectPlace(location.place)
                // Center slightly on tapped marker
                googleMap?.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
            }
            true
        }

        // Tap on empty map closes the bottom sheet
        googleMap?.setOnMapClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        // Clean marker cache references when map is ready
        activeMarkers.clear()
        markerLocationMap.clear()

        // Render any locations that may have loaded prior to map initialization
        viewModel.filteredLocations.value?.let { renderMarkers(it) }
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.includedBottomSheet.bottomSheetDetail)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.isHideable = true

        binding.includedBottomSheet.btnCloseSheet.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun setupSearchAndFilterUI() {
        // 1. Search View
        binding.searchViewPlaces.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.onSearchQueryChanged(query.orEmpty())
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.onSearchQueryChanged(newText.orEmpty())
                return true
            }
        })

        // 2. Weather Condition Spinner
        val conditions = listOf(
            getString(R.string.filter_condition_all),
            getString(R.string.filter_condition_clear),
            getString(R.string.filter_condition_isolated_clouds),
            getString(R.string.filter_condition_scattered_clouds),
            getString(R.string.filter_condition_overcast),
            getString(R.string.filter_condition_light_rain),
            getString(R.string.filter_condition_rain),
            getString(R.string.filter_condition_snow),
            getString(R.string.filter_condition_fog)
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, conditions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerConditionFilter.adapter = adapter

        binding.spinnerConditionFilter.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.onConditionFilterSelected(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun observeViewModel() {
        // Observes filtered locations to update map markers
        viewModel.filteredLocations.observe(this) { locations ->
            renderMarkers(locations)
            binding.tvLocationsCount.text = getString(R.string.locations_count_format, locations.size)
        }

        // Places loading progress
        viewModel.isLoadingPlaces.observe(this) { isLoading ->
            binding.progressSync.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Background Weather Sync progress
        viewModel.syncProgress.observe(this) { progress ->
            if (progress != null) {
                val (completed, total) = progress
                binding.progressSync.visibility = View.VISIBLE
                binding.progressSync.isIndeterminate = false
                binding.progressSync.max = total
                binding.progressSync.progress = completed

                binding.tvSyncStatus.visibility = View.VISIBLE
                binding.tvSyncStatus.text = getString(R.string.status_syncing_weather, completed, total)
            } else {
                binding.progressSync.visibility = View.GONE
                binding.tvSyncStatus.visibility = View.GONE
            }
        }

        // Detail Bottom Sheet state
        viewModel.selectedDetail.observe(this) { detailPair ->
            if (detailPair != null) {
                val (place, forecastResponse) = detailPair
                showPlaceDetail(place, forecastResponse)
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }

        // Detail loading indicator
        viewModel.isDetailLoading.observe(this) { isLoading ->
            binding.includedBottomSheet.layoutDetailLoading.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }

        // Error message events with retry action
        viewModel.errorMessage.observe(this) { errorMsg ->
            if (!errorMsg.isNullOrBlank()) {
                Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.btn_retry) {
                        viewModel.loadPlaces()
                    }
                    .show()
            }
        }

        // Logout state navigation
        viewModel.isLoggedOut.observe(this) { loggedOut ->
            if (loggedOut) {
                val intent = Intent(this, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun renderMarkers(locations: List<LocationWeather>) {
        val map = googleMap ?: return

        val currentPlaceCodes = locations.map { it.place.code }.toSet()

        // 1. Remove markers that are no longer part of the filtered list
        val iterator = activeMarkers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in currentPlaceCodes) {
                markerLocationMap.remove(entry.value)
                entry.value.remove()
                iterator.remove()
            }
        }

        // 2. Add new markers or update existing markers in-place
        for (item in locations) {
            val snippetText = item.currentForecast?.let {
                val tempStr = getString(R.string.detail_temperature_format, it.airTemperature)
                val condStr = getString(getConditionStringRes(it.conditionCode))
                "$tempStr • $condStr"
            } ?: item.place.administrativeDivision ?: ""

            val existingMarker = activeMarkers[item.place.code]
            if (existingMarker != null) {
                // Update snippet if changed
                if (existingMarker.snippet != snippetText) {
                    existingMarker.snippet = snippetText
                    if (existingMarker.isInfoWindowShown) {
                        existingMarker.showInfoWindow()
                    }
                }
                // Update marker icon
                existingMarker.setIcon(getMarkerIcon(item.currentForecast?.conditionCode))
                markerLocationMap[existingMarker] = item
            } else {
                val lat = item.place.coordinates.latitude
                val lon = item.place.coordinates.longitude
                val position = LatLng(lat, lon)

                val markerOptions = MarkerOptions()
                    .position(position)
                    .title(item.place.name)
                    .snippet(snippetText)
                    .icon(getMarkerIcon(item.currentForecast?.conditionCode))

                val marker = map.addMarker(markerOptions)
                if (marker != null) {
                    activeMarkers[item.place.code] = marker
                    markerLocationMap[marker] = item
                }
            }
        }
    }

    private fun getMarkerIcon(conditionCode: String?): com.google.android.gms.maps.model.BitmapDescriptor {
        val hue = when (conditionCode?.lowercase()) {
            "clear" -> BitmapDescriptorFactory.HUE_ORANGE
            "isolated-clouds", "scattered-clouds" -> BitmapDescriptorFactory.HUE_YELLOW
            "overcast" -> BitmapDescriptorFactory.HUE_AZURE
            "light-rain", "moderate-rain", "heavy-rain" -> BitmapDescriptorFactory.HUE_BLUE
            "light-snow", "moderate-snow", "heavy-snow", "sleet" -> BitmapDescriptorFactory.HUE_CYAN
            "fog" -> BitmapDescriptorFactory.HUE_VIOLET
            else -> BitmapDescriptorFactory.HUE_RED
        }
        return BitmapDescriptorFactory.defaultMarker(hue)
    }

    private fun showPlaceDetail(place: Place, forecast: com.madt.lab6.weathermap.data.model.PlaceForecastResponse?) {
        val bs = binding.includedBottomSheet
        bs.tvDetailPlaceName.text = place.name
        bs.tvDetailDivision.text = place.administrativeDivision ?: getString(R.string.weather_cond_na)
        bs.tvDetailCoordinates.text = getString(
            R.string.detail_coordinates_format,
            place.coordinates.latitude,
            place.coordinates.longitude
        )

        val latest = forecast?.forecastTimestamps?.firstOrNull()
        if (latest != null) {
            bindForecastDetails(latest)
        } else {
            bs.tvDetailTemperature.text = getString(R.string.weather_cond_na)
            bs.tvDetailCondition.text = getString(R.string.detail_loading_forecast, place.name)
            bs.tvDetailFeelsLike.text = ""
            bs.tvDetailWind.text = ""
            bs.tvDetailHumidity.text = ""
            bs.tvDetailPrecipitation.text = ""
            bs.tvDetailPressure.text = ""
            bs.tvDetailCloudCover.text = ""
            bs.tvDetailForecastTime.text = ""
        }

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun bindForecastDetails(forecast: ForecastTimestamp) {
        val bs = binding.includedBottomSheet

        bs.tvDetailTemperature.text = getString(R.string.detail_temperature_format, forecast.airTemperature)
        bs.tvDetailCondition.text = getString(getConditionStringRes(forecast.conditionCode))

        val feelsLike = forecast.feelsLikeTemperature ?: forecast.airTemperature
        bs.tvDetailFeelsLike.text = getString(R.string.detail_feels_like_format, feelsLike)

        val windDir = forecast.windDirection ?: 0
        bs.tvDetailWind.text = getString(R.string.detail_wind_format, forecast.windSpeed, windDir)

        val humidity = forecast.relativeHumidity ?: 0
        bs.tvDetailHumidity.text = getString(R.string.detail_humidity_format, humidity)

        val precip = forecast.totalPrecipitation ?: 0.0
        bs.tvDetailPrecipitation.text = getString(R.string.detail_precipitation_format, precip)

        val pressure = forecast.seaLevelPressure ?: 0
        bs.tvDetailPressure.text = getString(R.string.detail_pressure_format, pressure)

        val cloudCover = forecast.cloudCover ?: 0
        bs.tvDetailCloudCover.text = getString(R.string.detail_cloud_cover_format, cloudCover)

        bs.tvDetailForecastTime.text = getString(R.string.detail_forecast_time, forecast.forecastTimeUtc)
    }

    private fun getConditionStringRes(conditionCode: String): Int {
        return when (conditionCode.lowercase()) {
            "clear" -> R.string.weather_cond_clear
            "isolated-clouds" -> R.string.weather_cond_isolated_clouds
            "scattered-clouds" -> R.string.weather_cond_scattered_clouds
            "overcast" -> R.string.weather_cond_overcast
            "light-rain" -> R.string.weather_cond_light_rain
            "moderate-rain" -> R.string.weather_cond_moderate_rain
            "heavy-rain" -> R.string.weather_cond_heavy_rain
            "sleet" -> R.string.weather_cond_sleet
            "light-snow" -> R.string.weather_cond_light_snow
            "moderate-snow" -> R.string.weather_cond_moderate_snow
            "heavy-snow" -> R.string.weather_cond_heavy_snow
            "fog" -> R.string.weather_cond_fog
            else -> R.string.weather_cond_na
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map, menu)
        val userItem = menu.findItem(R.id.action_user_email)
        userItem?.title = getString(R.string.user_signed_in_as, viewModel.currentUserEmail)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.loadPlaces()
                true
            }
            R.id.action_logout -> {
                viewModel.logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
