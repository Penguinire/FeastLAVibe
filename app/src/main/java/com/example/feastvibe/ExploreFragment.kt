package com.example.feastvibe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ExploreFragment : Fragment(R.layout.activity_explore) {

    private lateinit var popularAdapter: RestaurantListAdapter
    private lateinit var nearbyAdapter: NearbyAdapter
    private lateinit var nightlifeAdapter: NearbyAdapter

    private var baseList: List<Place> = DummyData.places
    private var selectedChipIndex = 0

    private var currentArea = UserArea(
        GeoPoint.fromLngLat(28.1881, -25.7461),
        "Pretoria"
    )

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            requestLocationAndLoad()
        }

    // Order matches the six chips in activity_explore.xml exactly.
    private val chipFilters: List<(Place) -> Boolean> = listOf(
        { true },
        { it.type == PlaceType.RESTAURANT },
        { it.type == PlaceType.FAST_FOOD },
        {
            it.cuisine.contains("Coffee", ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains("Coffee", ignoreCase = true) }
        },
        { it.type == PlaceType.BAR },
        { it.type == PlaceType.CLUB }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.findViewById<View>(R.id.iv_explore_profile).setOnClickListener {
            val auth = FirebaseAuth.getInstance()

            if (auth.currentUser != null) {
                val popup = PopupMenu(
                    requireContext(),
                    view.findViewById(R.id.iv_explore_profile)
                )

                popup.menu.add("Sign Out")

                popup.setOnMenuItemClickListener { item ->
                    if (item.title == "Sign Out") {
                        auth.signOut()
                        true
                    } else {
                        false
                    }
                }

                popup.show()
            } else {
                findNavController().navigate(R.id.signupFragment)
            }
        }
        view.findViewById<View>(R.id.search_bar).setOnClickListener {
            findNavController().navigate(R.id.searchFragment)
        }
        view.findViewById<View>(R.id.iv_filter).setOnClickListener {
            findNavController().navigate(R.id.filtersFragment)
        }

        val openDetail: (Place) -> Unit = { place ->
            findNavController().navigate(
                R.id.restaurantDetailFragment,
                bundleOf("placeId" to place.id)
            )
        }

        nearbyAdapter = NearbyAdapter(DummyData.nearby(), onClick = openDetail)
        view.findViewById<RecyclerView>(R.id.rv_nearby).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = nearbyAdapter
        }

        popularAdapter = RestaurantListAdapter(DummyData.places, openDetail)
        view.findViewById<RecyclerView>(R.id.rv_popular).apply {
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
            adapter = popularAdapter
        }

        nightlifeAdapter = NearbyAdapter(DummyData.nightlife(), onClick = openDetail)
        view.findViewById<RecyclerView>(R.id.rv_nightlife).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = nightlifeAdapter
        }

        requestLocationAndLoad()

        setupCategoryChips(view)
        setupMoodGrid(view)
    }

    override fun onResume() {
        super.onResume()
        // Covers: user opens Filters, applies something, comes back to this tab.
        if (::popularAdapter.isInitialized) applyFilters()
    }

    /** Combines the category chip AND whatever FiltersFragment last applied. */
    private fun applyFilters() {
        val chipFiltered = baseList.filter(chipFilters[selectedChipIndex])
        popularAdapter.submit(chipFiltered.filter { FilterState.matches(it) })
    }

    private fun requestLocationAndLoad() {
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            loadExploreUsingLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun loadExploreUsingLocation() {
        viewLifecycleOwner.lifecycleScope.launch {

            currentArea = DeviceLocation.getUserArea(requireContext())

            val currentView = view ?: return@launch

            // Update the location labels
            currentView.findViewById<TextView>(R.id.tv_location_chip).text =
                "📍 ${currentArea.city} ▾"

            currentView.findViewById<TextView>(R.id.tv_popular_header).text =
                "Popular in ${currentArea.city}"

            // Show a loading message while weather is being fetched
            val weatherChip =
                currentView.findViewById<TextView>(R.id.tv_weather_chip)

            weatherChip.text = "🌡️ Loading weather..."

            // Load venues using the current coordinates
            loadRealPlaces(nearbyAdapter, nightlifeAdapter)

            // Load events using the current city
            setupEvents(currentView)

            // Fetch live weather
            val temperature = WeatherRepository.getTemperature(currentArea.point)

            if (temperature != null) {
                weatherChip.text =
                    "🌡️ ${temperature.toInt()}°C in ${currentArea.city}"
            } else {
                weatherChip.text =
                    "🌡️ Weather unavailable"
            }
        }
    }

    private fun setupCategoryChips(view: View) {
        val row = view.findViewById<LinearLayout>(R.id.chip_row)

        fun paint(selectedIndex: Int) {
            for (i in 0 until row.childCount) {
                val chip = row.getChildAt(i) as TextView
                if (i == selectedIndex) {
                    chip.setBackgroundResource(R.drawable.bg_chip_selected)
                    chip.setTextColor(resources.getColor(R.color.white, null))
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                    chip.setTextColor(resources.getColor(R.color.text_primary, null))
                }
            }
        }

        for (i in 0 until row.childCount) {
            row.getChildAt(i).setOnClickListener {
                paint(i)
                selectedChipIndex = i
                applyFilters()
            }
        }
        paint(0)
    }

    private fun setupMoodGrid(view: View) {
        val tiles = listOf(
            Triple(R.id.mood_1, "🔥", "Traditional Braai & Shisanyama"),
            Triple(R.id.mood_2, "☕", "Artisan Coffee & Cafes"),
            Triple(R.id.mood_3, "🍕", "Woodfired Pizza"),
            Triple(R.id.mood_4, "🌭", "Street Food & Kota")
        )
        val keywords = mapOf(
            R.id.mood_1 to "Braai",
            R.id.mood_2 to "Coffee",
            R.id.mood_3 to "Pizza",
            R.id.mood_4 to "Kota"
        )
        tiles.forEach { (id, icon, label) ->
            val tile = view.findViewById<View>(id)
            tile.findViewById<TextView>(R.id.tv_mood_icon).text = icon
            tile.findViewById<TextView>(R.id.tv_mood_label).text = label
            tile.setOnClickListener {
                val keyword = keywords.getValue(id)
                val matches = baseList.filter {
                    it.cuisine.contains(keyword, ignoreCase = true) ||
                            it.tags.any { tag -> tag.contains(keyword, ignoreCase = true) }
                }
                // Falls back to Search if nothing local matches that keyword yet
                // (e.g. before the Discover results for that category have loaded).
                if (matches.isEmpty()) {
                    findNavController().navigate(R.id.searchFragment)
                    return@setOnClickListener
                }
                popularAdapter.submit(
                    matches.filter { FilterState.matches(it) }
                )
                view.findViewById<NestedScrollView>(R.id.explore_scroll)
                    .smoothScrollTo(0, view.findViewById<View>(R.id.tv_popular_header).top)
            }
        }
    }

    private fun setupEvents(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.events_list)
        lifecycleScope.launch {
            val result = EventRepository.getEvents(
                city = currentArea.city
            )
            result.onSuccess { events ->
                events.take(2).forEach { event ->
                    val (day, dateNum) = formatEventDate(event.date)
                    val row = layoutInflater.inflate(R.layout.item_event_row, container, false)
                    row.findViewById<TextView>(R.id.tv_event_day).text = day
                    row.findViewById<TextView>(R.id.tv_event_date).text = dateNum
                    row.findViewById<TextView>(R.id.tv_event_category).text = "Event"
                    row.findViewById<TextView>(R.id.tv_event_title).text = event.name
                    row.findViewById<TextView>(R.id.tv_event_venue).text =
                        listOf(event.venue, event.city).filter { it.isNotBlank() }
                            .joinToString(" · ")
                    row.findViewById<TextView>(R.id.tv_event_price).text = "Details"
                    row.setOnClickListener {
                        findNavController().navigate(
                            R.id.eventDetailFragment,
                            bundleOf("eventId" to event.id)
                        )
                    }
                    container.addView(row)
                }
            }
        }
    }

    private fun formatEventDate(isoDate: String): Pair<String, String> {
        return try {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = parser.parse(isoDate) ?: return "" to ""
            java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(date)
                .uppercase() to
                    java.text.SimpleDateFormat("d", java.util.Locale.getDefault()).format(date)
        } catch (e: Exception) {
            "" to ""
        }
    }

    private fun loadRealPlaces(
        nearbyAdapter: NearbyAdapter,
        nightlifeAdapter: NearbyAdapter
    ) {
        val origin = currentArea.point

        lifecycleScope.launch {
            try {
                val restaurantResults = PlaceSearch.search(
                    query = "restaurants",
                    origin = origin,
                    limit = 20
                ).places

                val fastFoodResults = PlaceSearch.search(
                    query = "fast food",
                    origin = origin,
                    limit = 20
                ).places

                val coffeeResults = PlaceSearch.search(
                    query = "coffee cafes",
                    origin = origin,
                    limit = 15
                ).places

                val barResults = PlaceSearch.search(
                    query = "bars",
                    origin = origin,
                    limit = 20
                ).places

                val clubResults = PlaceSearch.search(
                    query = "nightclubs",
                    origin = origin,
                    limit = 20
                ).places

                val allPlaces = (
                        restaurantResults +
                                fastFoodResults +
                                coffeeResults +
                                barResults +
                                clubResults
                        ).distinctBy { it.id }

                if (allPlaces.isNotEmpty()) {
                    PlaceCache.putAll(allPlaces)

                    baseList = allPlaces.sortedBy { it.distanceKm }

                    nearbyAdapter.submit(baseList.take(6))

                    val nightlife = baseList.filter {
                        it.type == PlaceType.BAR ||
                                it.type == PlaceType.CLUB
                    }

                    nightlifeAdapter.submit(nightlife)

                    applyFilters()
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "ExploreFragment",
                    "Failed to load Explore places",
                    e
                )
            }
        }
    }
}