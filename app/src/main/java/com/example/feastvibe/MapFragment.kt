package com.example.feastvibe

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MapFragment : Fragment(R.layout.activity_map) {

    private var selected: Place? = null

    private lateinit var rootView: View

    private var mapView: MapView? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLocation: Point? = null

    private var annotationManager: PointAnnotationManager? = null

    private var markerBitmap: Bitmap? = null

    private val pretoriaCbd =
        Point.fromLngLat(
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
         * If we arrived here from Restaurant Details,
         * load the selected place.
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

        requestLocation()

        setupInlineSearch(view)

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

    /**
     * =========================================================
     * MAP SETUP
     * =========================================================
     */
    private fun setupMap(view: View) {

        val map =
            view.findViewById<MapView>(
                R.id.mapView
            )

        mapView = map

        map.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(
                    Point.fromLngLat(
                        selected?.lng
                            ?: pretoriaCbd.longitude(),

                        selected?.lat
                            ?: pretoriaCbd.latitude()
                    )
                )
                .zoom(15.5)
                .build()
        )

        // Markers are only created AFTER the style finishes loading — creating
        // the annotation manager or adding markers earlier is a race condition
        // that can silently show zero markers on a slower load.
        map.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {

            enableLocationPuck()

            annotationManager =
                map.annotations
                    .createPointAnnotationManager()

            val current = selected

            if (current != null) {
                placeMarker(current)
            } else {
                replaceMarkersWithSearchResults(defaultNearbyPlaces())
            }

            annotationManager
                ?.addClickListener(
                    OnPointAnnotationClickListener { annotation ->

                        val placeId =
                            annotation
                                .getData()
                                ?.asString

                        if (placeId != null) {

                            selected =
                                PlaceCache.get(placeId)
                                    ?: DummyData.byId(placeId)

                            bindCard()
                        }

                        true
                    }
                )
        }
    }

    /**
     * =========================================================
     * DEFAULT MAP PLACES
     * =========================================================
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
     * =========================================================
     * CREATE ONE MAP MARKER
     * =========================================================
     */
    private fun placeMarker(
        place: Place
    ) {

        val bitmap =
            markerBitmap
                ?: createMarkerBitmap()
                    .also {
                        markerBitmap = it
                    }

        val point =
            Point.fromLngLat(
                place.lng,
                place.lat
            )

        val options =
            PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withData(
                    com.google.gson.JsonPrimitive(
                        place.id
                    )
                )

        annotationManager
            ?.create(options)
    }

    /**
     * =========================================================
     * REPLACE MAP MARKERS
     * =========================================================
     */
    private fun replaceMarkersWithSearchResults(
        places: List<Place>
    ) {

        annotationManager?.deleteAll()

        places.forEach { place ->

            placeMarker(place)
        }
    }

    /**
     * =========================================================
     * CREATE PINK MARKER
     * =========================================================
     */
    private fun createMarkerBitmap(): Bitmap {

        val size = 64

        val bitmap =
            Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(bitmap)

        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG)
                .apply {

                    color =
                        Color.parseColor(
                            "#FF4D6D"
                        )
                }

        canvas.drawCircle(
            size / 2f,
            size / 2f,
            size / 2f - 4f,
            paint
        )

        val borderPaint =
            Paint(Paint.ANTI_ALIAS_FLAG)
                .apply {

                    color =
                        Color.WHITE

                    style =
                        Paint.Style.STROKE

                    strokeWidth =
                        4f
                }

        canvas.drawCircle(
            size / 2f,
            size / 2f,
            size / 2f - 4f,
            borderPaint
        )

        return bitmap
    }

    /**
     * =========================================================
     * MAP SEARCH
     * =========================================================
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
         * This Job is IMPORTANT.
         *
         * When the user types:
         *
         * piz
         * pizz
         * pizza
         *
         * the previous request is cancelled.
         */
        var suggestionJob: Job? = null

        val adapter =
            SearchResultAdapter(
                emptyList()
            ) { place ->

                selected = place

                /*
                 * Save the real Mapbox place.
                 */
                PlaceCache.putAll(
                    listOf(place)
                )

                /*
                 * Update bottom card.
                 */
                bindCard()

                /*
                 * Move map to selected place.
                 */
                mapView
                    ?.mapboxMap
                    ?.setCamera(

                        CameraOptions.Builder()
                            .center(
                                Point.fromLngLat(
                                    place.lng,
                                    place.lat
                                )
                            )
                            .zoom(15.5)
                            .build()
                    )

                /*
                 * Keep the selected place's marker visible.
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
         * -----------------------------------------------------
         * OPEN SEARCH
         * -----------------------------------------------------
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
         * -----------------------------------------------------
         * CANCEL SEARCH
         * -----------------------------------------------------
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
         * -----------------------------------------------------
         * LIVE SEARCH
         * -----------------------------------------------------
         */
        input.addTextChangedListener { text ->

            /*
             * Cancel the previous search.
             */
            suggestionJob?.cancel()

            val query =
                text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            /*
             * Don't search tiny queries.
             */
            if (query.length < 3) {

                resultsList.visibility =
                    View.GONE

                return@addTextChangedListener
            }

            /*
             * Start a new search job.
             */
            suggestionJob =
                viewLifecycleOwner
                    .lifecycleScope
                    .launch {

                        /*
                         * Wait until the user stops typing.
                         *
                         * This prevents:
                         *
                         * p
                         * pi
                         * piz
                         * pizz
                         * pizza
                         *
                         * from all immediately hitting Mapbox.
                         */
                        delay(650)

                        /*
                         * Use current device location when available.
                         *
                         * Otherwise use Pretoria CBD.
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
                         * Fragment may have been destroyed.
                         */
                        if (!isAdded) {
                            return@launch
                        }

                        /*
                         * Check whether the user typed something
                         * newer while the request was running.
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
                         * No results.
                         */
                        if (places.isEmpty()) {

                            resultsList.visibility =
                                View.GONE

                            /*
                             * Clear search markers.
                             */
                            replaceMarkersWithSearchResults(
                                emptyList()
                            )

                            return@launch
                        }

                        /*
                         * Save real Mapbox places.
                         */
                        PlaceCache.putAll(
                            places
                        )

                        /*
                         * Give the places to the RecyclerView.
                         */
                        resultsList.adapter =
                            adapter

                        adapter.submit(
                            places
                        )

                        /*
                         * IMPORTANT:
                         *
                         * The SAME real places are also put
                         * onto the map.
                         */
                        replaceMarkersWithSearchResults(
                            places
                        )

                        resultsList.visibility =
                            View.VISIBLE
                    }
        }

        /*
         * -----------------------------------------------------
         * KEYBOARD SEARCH BUTTON
         * -----------------------------------------------------
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
             * Cancel live autocomplete.
             */
            suggestionJob?.cancel()

            /*
             * Final search.
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
                     * Save results.
                     */
                    PlaceCache.putAll(
                        outcome.places
                    )

                    /*
                     * Show results in list.
                     */
                    resultsList.adapter =
                        adapter

                    adapter.submit(
                        outcome.places
                    )

                    /*
                     * Show same results on map.
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
     * =========================================================
     * EXIT SEARCH MODE
     * =========================================================
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
     * =========================================================
     * LOCATION PERMISSION
     * =========================================================
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
     * =========================================================
     * LOCATION PUCK
     * =========================================================
     */
    private fun enableLocationPuck() {

        if (!hasLocationPermission()) {
            return
        }

        mapView
            ?.location
            ?.updateSettings {

                enabled = true

                pulsingEnabled = true
            }
    }

    /**
     * =========================================================
     * CACHE CURRENT LOCATION
     * =========================================================
     */
    private fun cacheCurrentLocation() {

        if (!hasLocationPermission()) {
            return
        }

        fusedLocationClient
            .lastLocation
            .addOnSuccessListener { cached ->

                if (cached != null) {

                    currentLocation =
                        Point.fromLngLat(
                            cached.longitude,
                            cached.latitude
                        )

                } else {

                    requestLiveLocationUpdate(
                        recenter = false
                    )
                }
            }
    }

    /**
     * =========================================================
     * RECENTER
     * =========================================================
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

        fusedLocationClient
            .lastLocation
            .addOnSuccessListener { cached ->

                if (cached != null) {

                    recenterOn(
                        cached.latitude,
                        cached.longitude
                    )

                } else {

                    requestLiveLocationUpdate()
                }
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

                            currentLocation =
                                Point.fromLngLat(
                                    loc.longitude,
                                    loc.latitude
                                )
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
            Point.fromLngLat(
                longitude,
                latitude
            )

        currentLocation =
            point

        mapView
            ?.mapboxMap
            ?.setCamera(

                CameraOptions.Builder()
                    .center(point)
                    .zoom(15.5)
                    .build()
            )
    }

    /**
     * =========================================================
     * OPEN RESTAURANT DETAILS
     * =========================================================
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
     * =========================================================
     * CARD VISIBILITY
     * =========================================================
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
     * =========================================================
     * BOTTOM PLACE CARD
     * =========================================================
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

        rootView
            .findViewById<TextView>(
                R.id.tv_map_status
            )
            .text =
            "${if (place.isOpen) "OPEN NOW" else "CLOSED"} · " +
                    "${"%.1f".format(place.distanceKm)} km away · " +
                    place.area
    }

    /**
     * =========================================================
     * FILTER CHIPS
     * =========================================================
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
                     * Update selected chip.
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

                    selected = null

                    updateCardVisibility()

                    /*
                     * Filters already-known places.
                     *
                     * No new Mapbox request.
                     */
                    val merged =
                        (
                                PlaceCache.all() +
                                        DummyData.places
                                )
                            .distinctBy {
                                it.id
                            }

                    replaceMarkersWithSearchResults(
                        merged
                            .filter(filters[i])
                            .take(15)
                    )
                }
        }
    }

    /**
     * =========================================================
     * MAP LIFECYCLE
     * =========================================================
     */
    override fun onStart() {

        super.onStart()

        mapView?.onStart()
    }

    override fun onStop() {

        super.onStop()

        mapView?.onStop()
    }

    override fun onDestroyView() {

        super.onDestroyView()

        mapView?.onDestroy()

        mapView = null

        annotationManager = null
    }

    override fun onLowMemory() {

        super.onLowMemory()

        mapView?.onLowMemory()
    }
}