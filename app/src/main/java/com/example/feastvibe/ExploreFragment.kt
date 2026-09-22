package com.example.feastvibe

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.geojson.Point
import com.mapbox.search.discover.Discover
import com.mapbox.search.discover.DiscoverQuery
import kotlinx.coroutines.launch

class ExploreFragment : Fragment(R.layout.activity_explore) {

    private lateinit var popularAdapter: RestaurantListAdapter

    // Whatever the Popular section is built from right now — starts as dummy data,
    // gets replaced once the Mapbox Discover call in loadRealPlaces() comes back.
    private var baseList: List<Place> = DummyData.places
    private var selectedChipIndex = 0

    // Order matches the six chips in activity_explore.xml exactly.
    private val chipFilters: List<(Place) -> Boolean> = listOf(
        { true },
        { it.type == PlaceType.RESTAURANT },
        { it.type == PlaceType.FAST_FOOD },
        { it.cuisine.contains("Coffee", ignoreCase = true) },
        { it.type == PlaceType.BAR },
        { it.type == PlaceType.CLUB }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.findViewById<View>(R.id.iv_explore_profile).setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
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

        val nearbyAdapter = NearbyAdapter(DummyData.nearby(), onClick = openDetail)
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

        val nightlifeAdapter = NearbyAdapter(DummyData.nightlife(), onClick = openDetail)
        view.findViewById<RecyclerView>(R.id.rv_nightlife).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = nightlifeAdapter
        }

        loadRealPlaces(nearbyAdapter, nightlifeAdapter)

        setupCategoryChips(view)
        setupMoodGrid(view)
        setupEvents(view)
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
                popularAdapter.submit(matches)
                view.findViewById<NestedScrollView>(R.id.explore_scroll)
                    .smoothScrollTo(0, view.findViewById<View>(R.id.tv_popular_header).top)
            }
        }
    }

    private fun setupEvents(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.events_list)
        lifecycleScope.launch {
            val result = EventRepository.getEvents(city = "Pretoria")
            result.onSuccess { events ->
                events.take(2).forEach { event ->
                    val (day, dateNum) = formatEventDate(event.date)
                    val row = layoutInflater.inflate(R.layout.item_event_row, container, false)
                    row.findViewById<TextView>(R.id.tv_event_day).text = day
                    row.findViewById<TextView>(R.id.tv_event_date).text = dateNum
                    row.findViewById<TextView>(R.id.tv_event_category).text = "Event"
                    row.findViewById<TextView>(R.id.tv_event_title).text = event.name
                    row.findViewById<TextView>(R.id.tv_event_venue).text =
                        listOf(event.venue, event.city).filter { it.isNotBlank() }.joinToString(" · ")
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
            java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(date).uppercase() to
                    java.text.SimpleDateFormat("d", java.util.Locale.getDefault()).format(date)
        } catch (e: Exception) {
            "" to ""
        }
    }

    private fun loadRealPlaces(nearbyAdapter: NearbyAdapter, nightlifeAdapter: NearbyAdapter) {
        val discover = Discover.create()
        val pretoriaCbd = Point.fromLngLat(28.1881, -25.7461)

        lifecycleScope.launch {
            val response = discover.search(
                query = DiscoverQuery.Category.RESTAURANTS,
                proximity = pretoriaCbd
            )
            if (response.isValue) {
                val results = response.value.orEmpty()
                    .map { it.toPlace(PlaceType.RESTAURANT, pretoriaCbd.latitude(), pretoriaCbd.longitude()) }
                if (results.isNotEmpty()) {
                    PlaceCache.putAll(results)
                    nearbyAdapter.submit(results.take(6))
                    baseList = results
                    applyFilters()
                }
            }
        }

        lifecycleScope.launch {
            val barsResponse = discover.search(
                query = DiscoverQuery.Category.BARS,
                proximity = pretoriaCbd
            )
            val clubsResponse = discover.search(
                query = DiscoverQuery.Category.NIGHT_CLUBS,
                proximity = pretoriaCbd
            )

            val bars = if (barsResponse.isValue) {
                barsResponse.value.orEmpty()
                    .map { it.toPlace(PlaceType.BAR, pretoriaCbd.latitude(), pretoriaCbd.longitude()) }
            } else emptyList()

            val clubs = if (clubsResponse.isValue) {
                clubsResponse.value.orEmpty()
                    .map { it.toPlace(PlaceType.CLUB, pretoriaCbd.latitude(), pretoriaCbd.longitude()) }
            } else emptyList()

            val nightlife = bars + clubs
            if (nightlife.isNotEmpty()) {
                PlaceCache.putAll(nightlife)
                nightlifeAdapter.submit(nightlife)
            }
        }
    }
}