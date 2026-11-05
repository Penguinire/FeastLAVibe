package com.example.feastvibe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MapFragment : Fragment(R.layout.activity_map) {

    private var selected: Place? = null

    private lateinit var rootView: View

    private var mapView: MapView? = null
    private var mapLibreMap: MapLibreMap? = null
    private val mapMarkers = mutableListOf<Marker>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLocation: GeoPoint? = null
    private var weatherJob: Job? = null


    private val pretoriaCbd =
        GeoPoint.fromLngLat(
            28.1881,
            -25.7461
        )

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grants ->

            val granted =
                grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {

                enableLocationPuck()

                cacheCurrentLocation()
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        rootView = view

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(
                requireContext()
            )

        /*
- If we arrived here from Restaurant Details,
- load the selected place.
         */
        arguments
            ?.getString("placeId")
            ?.let { incomingId ->

                selected =
                    PlaceCache.get(incomingId)
                        ?: DummyData.byId(incomingId)
            }

        updateCardVisibility()

        bindCard()

        setupMap(view)

        setupInlineSearch(view)

        // Show Pretoria weather initially until device location is available.
        updateMapWeather(pretoriaCbd)

        requestLocation()

        view
            .findViewById<View>(R.id.card_place)
            .setOnClickListener {
                openDetail()
            }

        view
            .findViewById<View>(R.id.btn_map_view_venue)
            .setOnClickListener {
                openDetail()
            }

        view
            .findViewById<View>(R.id.btn_map_directions)
            .setOnClickListener {

                selected?.let {
                    MapDirections.launch(
                        this,
                        it
                    )
                }
            }

        view
            .findViewById<View>(R.id.fab_recenter)
            .setOnClickListener {
                recenterOnMe()
            }

        setupChips(view)
    }

    override fun onDestroyView() {
        weatherJob?.cancel()
        weatherJob = null

        mapView?.onDestroy()
        mapView = null
        mapLibreMap = null
        mapMarkers.clear()

        super.onDestroyView()
    }

    private fun updateMapWeather(point: GeoPoint) {
        weatherJob?.cancel()

        val weatherView = rootView.findViewById<TextView>(
            R.id.tv_map_weather
        )

        weatherView.text = "🌡️ Updating..."

        weatherJob = viewLifecycleOwner.lifecycleScope.launch {
            val temperature = WeatherRepository.getTemperature(point)

            if (!isAdded) return@launch

            weatherView.text = if (temperature != null) {
                "🌡️ ${temperature.toInt()}°C"
            } else {
                "🌡️ Weather unavailable"
            }
        }
    }

    /**
    - =========================================================
    - MAP SETUP
    - =========================================================
     */
    private fun setupMap(view: View) {
        val map = view.findViewById<MapView>(R.id.mapView)
        mapView = map

        map.onCreate(null)

        map.getMapAsync { loadedMap ->
            mapLibreMap = loadedMap

            val styleUrl =
                "https://maps.geoapify.com/v1/styles/osm-carto/style.json" +
                        "?apiKey=${BuildConfig.GEOAPIFY_API_KEY}"

            loadedMap.setStyle(styleUrl) {
                val initialPoint = LatLng(
                    selected?.lat ?: pretoriaCbd.latitude(),
                    selected?.lng ?: pretoriaCbd.longitude()
                )

                loadedMap.cameraPosition = CameraPosition.Builder()
                    .target(initialPoint)
                    .zoom(15.5)
                    .build()

                val current = selected

                if (current != null) {
                    placeMarker(current)
                } else {
                    replaceMarkersWithSearchResults(defaultNearbyPlaces())
                }

                loadedMap.setOnMarkerClickListener { marker ->
                    val placeId = marker.snippet

                    if (!placeId.isNullOrBlank()) {
                        selected = PlaceCache.get(placeId)
                            ?: DummyData.byId(placeId)

                        bindCard()
                    }

                    true
                }

                enableLocationPuck()
            }
        }
    }

    /**
    - =========================================================
    - DEFAULT MAP PLACES
    - =========================================================
     */
    private fun defaultNearbyPlaces(): List<Place> {

        return (
                PlaceCache.all() +
                        DummyData.places
                )
            .distinctBy {
                it.id
            }
            .sortedBy {
                it.distanceKm
            }
            .take(10)
    }

    /**
    - =========================================================
    - CREATE ONE MAP MARKER
    - =========================================================
     */
    private fun placeMarker(place: Place) {
        val map = mapLibreMap ?: return

        val marker = map.addMarker(
            MarkerOptions()
                .position(LatLng(place.lat, place.lng))
                .title(place.name)
                .snippet(place.id)
        )

        if (marker != null) {
            mapMarkers.add(marker)
        }
    }

    /**
    - =========================================================
    - REPLACE MAP MARKERS
    - =========================================================
     */
    private fun replaceMarkersWithSearchResults(places: List<Place>) {
        val map = mapLibreMap ?: return

        mapMarkers.forEach { marker ->
            map.removeMarker(marker)
        }

        mapMarkers.clear()

        places.forEach { place ->
            placeMarker(place)
        }
    }

    /**
    - =========================================================
    - MAP SEARCH
    - =========================================================
     */
    private fun setupInlineSearch(
        view: View
    ) {

        val label =
            view.findViewById<TextView>(
                R.id.tv_map_search_label
            )

        val weather =
            view.findViewById<TextView>(
                R.id.tv_map_weather
            )

        val input =
            view.findViewById<EditText>(
                R.id.et_map_search
            )

        val cancel =
            view.findViewById<View>(
                R.id.iv_map_search_cancel
            )

        val resultsList =
            view.findViewById<RecyclerView>(
                R.id.rv_map_search_results
            )

        resultsList.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        val filterScroll =
            view.findViewById<View>(
                R.id.map_filter_scroll
            )

        /*
- This Job is IMPORTANT.
         *
- When the user types:
         *
- piz
- pizz
- pizza
         *
- the previous request is cancelled.
         */
        var suggestionJob: Job? = null

        val adapter =
            SearchResultAdapter(
                emptyList()
            ) { place ->

                selected = place


                PlaceCache.putAll(
                    listOf(place)
                )

                /*
- Update bottom card.
                 */
                bindCard()

                /*
- Move map to selected place.
                 */
                mapLibreMap?.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(place.lat, place.lng))
                    .zoom(15.5)
                    .build()

                /*
- Keep the selected place's marker visible.
                 */
                replaceMarkersWithSearchResults(
                    listOf(place)
                )

                exitSearchMode(
                    label,
                    weather,
                    input,
                    cancel,
                    resultsList,
                    filterScroll
                )
            }

        /*
- -----------------------------------------------------
- OPEN SEARCH
- -----------------------------------------------------
         */
        label.setOnClickListener {

            label.visibility =
                View.GONE

            weather.visibility =
                View.GONE

            input.visibility =
                View.VISIBLE

            cancel.visibility =
                View.VISIBLE

            filterScroll.visibility =
                View.GONE

            input.requestFocus()
        }

        /*
- -----------------------------------------------------
- CANCEL SEARCH
- -----------------------------------------------------
         */
        cancel.setOnClickListener {

            suggestionJob?.cancel()

            exitSearchMode(
                label,
                weather,
                input,
                cancel,
                resultsList,
                filterScroll
            )
        }

        /*
- -----------------------------------------------------
- LIVE SEARCH
- -----------------------------------------------------
         */
        input.addTextChangedListener { text ->

            /*
- Cancel the previous search.
             */
            suggestionJob?.cancel()

            val query =
                text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            /*
- Don't search tiny queries.
             */
            if (query.length < 3) {

                resultsList.visibility =
                    View.GONE

                return@addTextChangedListener
            }

            /*
- Start a new search job.
             */
            suggestionJob =
                viewLifecycleOwner
                    .lifecycleScope
                    .launch {

                        /*
- Wait until the user stops typing.
                         *
- This prevents:
                         *
- p
- pi
- piz
- pizz
- pizza
                         *
                         */
                        delay(650)

                        /*
- Use current device location when available.
                         *
- or use Pretoria CBD.
                         */
                        val origin =
                            currentLocation
                                ?: pretoriaCbd

                        val outcome =
                            try {

                                PlaceSearch.suggest(
                                    query = query,
                                    origin = origin,
                                    limit = 5
                                )

                            } catch (e: Exception) {

                                android.util.Log.e(
                                    "MapFragment",
                                    "Search failed for '$query'",
                                    e
                                )

                                null
                            }

                        /*
- Fragment may have been destroyed.
                         */
                        if (!isAdded) {
                            return@launch
                        }

                        /*
- Check whether the user typed something
- newer while the request was running.
                         */
                        if (
                            input.text
                                .toString()
                                .trim() != query
                        ) {

                            return@launch
                        }

                        val places =
                            outcome
                                ?.places
                                .orEmpty()

                        /*
- No results.
                         */
                        if (places.isEmpty()) {

                            resultsList.visibility =
                                View.GONE

                            /*
- Clear search markers.
                             */
                            replaceMarkersWithSearchResults(
                                emptyList()
                            )

                            return@launch
                        }


                        PlaceCache.putAll(
                            places
                        )

                        /*
- Give the places to the RecyclerView.
                         */
                        resultsList.adapter =
                            adapter

                        adapter.submit(
                            places
                        )

                        /*
- IMPORTANT:
                         *
- The SAME real places are also put
- onto the map.
                         */
                        replaceMarkersWithSearchResults(
                            places
                        )

                        resultsList.visibility =
                            View.VISIBLE
                    }
        }

        /*
- -----------------------------------------------------
- KEYBOARD SEARCH BUTTON
- -----------------------------------------------------
         */
        input.setOnEditorActionListener {
                _,
                actionId,
                event ->

            val pressedSearch =
                actionId ==
                        android.view.inputmethod
                            .EditorInfo
                            .IME_ACTION_SEARCH

            val pressedEnter =
                event?.keyCode ==
                        android.view.KeyEvent
                            .KEYCODE_ENTER

            if (
                !pressedSearch &&
                !pressedEnter
            ) {

                return@setOnEditorActionListener false
            }

            val query =
                input.text
                    .toString()
                    .trim()

            if (query.isBlank()) {

                return@setOnEditorActionListener true
            }

            /*
- Cancel live autocomplete.
             */
            suggestionJob?.cancel()

            /*
- Final search.
             */
            viewLifecycleOwner
                .lifecycleScope
                .launch {

                    val origin =
                        currentLocation
                            ?: pretoriaCbd

                    val outcome =
                        try {

                            PlaceSearch.search(
                                query = query,
                                origin = origin,
                                limit = 5
                            )

                        } catch (e: Exception) {

                            android.util.Log.e(
                                "MapFragment",
                                "Final search failed for '$query'",
                                e
                            )

                            null
                        }

                    if (!isAdded) {
                        return@launch
                    }

                    if (outcome == null) {

                        resultsList.visibility =
                            View.GONE

                        return@launch
                    }

                    /*
- Save results.
                     */
                    PlaceCache.putAll(
                        outcome.places
                    )

                    /*
- Show results in list.
                     */
                    resultsList.adapter =
                        adapter

                    adapter.submit(
                        outcome.places
                    )

                    /*
- Show same results on map.
                     */
                    replaceMarkersWithSearchResults(
                        outcome.places
                    )

                    resultsList.visibility =
                        if (
                            outcome.places.isEmpty()
                        ) {

                            View.GONE

                        } else {

                            View.VISIBLE
                        }
                }

            true
        }
    }

    /**
    - =========================================================
    - EXIT SEARCH MODE
    - =========================================================
     */
    private fun exitSearchMode(
        label: TextView,
        weather: TextView,
        input: EditText,
        cancel: View,
        resultsList: RecyclerView,
        filterScroll: View
    ) {

        input.setText("")

        input.visibility =
            View.GONE

        cancel.visibility =
            View.GONE

        label.visibility =
            View.VISIBLE

        weather.visibility =
            View.VISIBLE

        resultsList.visibility =
            View.GONE

        filterScroll.visibility =
            View.VISIBLE
    }

    /**
    - =========================================================
    - LOCATION PERMISSION
    - =========================================================
     */
    private fun requestLocation() {

        if (hasLocationPermission()) {

            enableLocationPuck()

            cacheCurrentLocation()

        } else {

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {

        val fine =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarse =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    /**
    - =========================================================
    - LOCATION PUCK
    - =========================================================
     */
    private fun enableLocationPuck() {
        val map = mapLibreMap ?: return

        // The location indicator needs a loaded map style.
        val style = map.style ?: return

        // Never activate the location component without permission.
        if (!hasLocationPermission()) return

        try {
            val locationComponent = map.locationComponent

            if (!locationComponent.isLocationComponentActivated) {
                locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions
                        .builder(requireContext(), style)
                        .build()
                )
            }

            locationComponent.isLocationComponentEnabled = true

            // Show the user's position without forcing the camera to follow it.
            locationComponent.cameraMode = CameraMode.NONE
            locationComponent.renderMode = RenderMode.NORMAL

        } catch (e: Exception) {
            android.util.Log.e(
                "MapFragment",
                "Unable to enable the location indicator",
                e
            )
        }
    }

    /**
    - =========================================================
    - CACHE CURRENT LOCATION
    - =========================================================
     */
    private fun cacheCurrentLocation() {

        if (!hasLocationPermission()) {
            return
        }

        fusedLocationClient
            .lastLocation
            .addOnSuccessListener { cached ->

                if (cached != null) {

                    currentLocation = GeoPoint.fromLngLat(
                        cached.longitude,
                        cached.latitude
                    )

                    updateMapWeather(currentLocation!!)

                } else {

                    requestLiveLocationUpdate(
                        recenter = false
                    )
                }
            }
    }

    /**
    - =========================================================
    - RECENTER
    - =========================================================
     */
    private fun recenterOnMe() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                recenterOn(
                    location.latitude,
                    location.longitude
                )
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Couldn't get your location. Check that device location is enabled.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }.addOnFailureListener { error ->
            android.util.Log.e(
                "MapFragment",
                "Failed to get current location",
                error
            )

            android.widget.Toast.makeText(
                requireContext(),
                "Unable to get your location. Please try again.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun requestLiveLocationUpdate(
        recenter: Boolean = true
    ) {

        if (!hasLocationPermission()) {
            return
        }

        val request =
            LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                2000L
            )
                .setMaxUpdates(1)
                .build()

        val callback =
            object : LocationCallback() {

                override fun onLocationResult(
                    result: LocationResult
                ) {

                    val loc =
                        result.lastLocation

                    if (loc != null) {

                        if (recenter) {

                            recenterOn(
                                loc.latitude,
                                loc.longitude
                            )

                        } else {

                            currentLocation = GeoPoint.fromLngLat(
                                loc.longitude,
                                loc.latitude
                            )

                            updateMapWeather(currentLocation!!)
                        }
                    }

                    fusedLocationClient
                        .removeLocationUpdates(
                            this
                        )
                }
            }

        fusedLocationClient
            .requestLocationUpdates(
                request,
                callback,
                requireContext().mainLooper
            )
    }

    private fun recenterOn(
        latitude: Double,
        longitude: Double
    ) {

        val point =
            GeoPoint.fromLngLat(
                longitude,
                latitude
            )

        currentLocation =
            point

        updateMapWeather(point)

        mapLibreMap?.cameraPosition = CameraPosition.Builder()
            .target(LatLng(latitude, longitude))
            .zoom(15.5)
            .build()
    }

    /**
    - =========================================================
    - OPEN RESTAURANT DETAILS
    - =========================================================
     */
    private fun openDetail() {

        val place =
            selected
                ?: return

        findNavController()
            .navigate(
                R.id.restaurantDetailFragment,
                bundleOf(
                    "placeId" to place.id
                )
            )
    }

    /**
    - =========================================================
    - CARD VISIBILITY
    - =========================================================
     */
    private fun updateCardVisibility() {

        rootView
            .findViewById<View>(
                R.id.card_place
            )
            .visibility =
            if (selected != null) {

                View.VISIBLE

            } else {

                View.GONE
            }
    }

    /**
    - =========================================================
    - BOTTOM PLACE CARD
    - =========================================================
     */
    private fun bindCard() {

        updateCardVisibility()

        val place =
            selected
                ?: return

        rootView
            .findViewById<TextView>(
                R.id.tv_map_name
            )
            .text =
            place.name

        rootView
            .findViewById<TextView>(
                R.id.tv_map_meta
            )
            .text =
            place.cuisine

        rootView
            .findViewById<TextView>(
                R.id.tv_map_price
            )
            .text =
            place.price ?: ""

        rootView
            .findViewById<TextView>(
                R.id.tv_map_rating
            )
            .text =
            if (place.rating != null) {

                "★ ${place.rating} (${place.reviewCount ?: 0})"

            } else {

                "New"
            }

        val statusPrefix = when (place.isOpen) {
            true -> "OPEN NOW · "
            false -> "CLOSED · "
            null -> ""
        }

        rootView
            .findViewById<TextView>(R.id.tv_map_status)
            .text = "$statusPrefix${"%.1f".format(place.distanceKm)} km away · ${place.area}"
    }

    /**
    - =========================================================
    - FILTER CHIPS
    - =========================================================
     */
    private fun setupChips(
        view: View
    ) {

        val row =
            view.findViewById<LinearLayout>(
                R.id.map_chip_row
            )

        val filters:
                List<(Place) -> Boolean> =
            listOf(

                {
                    true
                },

                {
                    it.type ==
                            PlaceType.RESTAURANT ||
                            it.type ==
                            PlaceType.FAST_FOOD
                },

                {
                    it.type ==
                            PlaceType.BAR
                },

                {
                    it.type ==
                            PlaceType.CLUB
                }
            )

        for (
        i in 0 until row.childCount
        ) {

            row
                .getChildAt(i)
                .setOnClickListener {

                    /*
- Update selected chip.
                     */
                    for (
                    j in 0 until row.childCount
                    ) {

                        val chip =
                            row.getChildAt(j)
                                    as TextView

                        if (j == i) {

                            chip.setBackgroundResource(
                                R.drawable.bg_chip_selected
                            )

                            chip.setTextColor(
                                resources.getColor(
                                    R.color.white,
                                    null
                                )
                            )

                        } else {

                            chip.setBackgroundResource(
                                R.drawable.bg_chip_unselected
                            )

                            chip.setTextColor(
                                resources.getColor(
                                    R.color.text_primary,
                                    null
                                )
                            )
                        }
                    }
                }
        }
    }
}