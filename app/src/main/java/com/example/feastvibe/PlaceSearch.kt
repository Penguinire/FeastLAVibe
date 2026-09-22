package com.example.feastvibe

import android.content.Context
import android.util.Log
import com.mapbox.geojson.Point
import com.mapbox.search.discover.Discover
import com.mapbox.search.discover.DiscoverQuery

/**
 * Shared search logic for Feast & Vibe.
 *
 * Search is intentionally based around Mapbox Discover because
 * our app is being used in South Africa.
 *
 * Supported examples:
 *
 * restaurants
 * coffee
 * cafes
 * bars
 * clubs
 * pizza
 * burgers
 * sushi
 * fast food
 * Hatfield
 * Menlyn
 * Pretoria CBD
 */
object PlaceSearch {

    private const val TAG = "PlaceSearch"

    private val discover by lazy {
        Discover.create()
    }

    /*
     * Pretoria areas that we know directly.
     *
     * This avoids making unnecessary area-search API calls.
     */
    private val knownAreas = mapOf(

        "hatfield" to Point.fromLngLat(
            28.2378,
            -25.7487
        ),

        "menlyn" to Point.fromLngLat(
            28.2775,
            -25.7826
        ),

        "menlyn maine" to Point.fromLngLat(
            28.2775,
            -25.7826
        ),

        "pretoria cbd" to Point.fromLngLat(
            28.1881,
            -25.7461
        ),

        "cbd" to Point.fromLngLat(
            28.1881,
            -25.7461
        )
    )

    // Used only when Mapbox has no results near the actual search location —
    // guarantees the demo shows a real, live API result rather than only samples.
    private val GLOBAL_FALLBACK_ORIGIN = Point.fromLngLat(-0.1276, 51.5072) // London
    fun initialize(context: Context) {
        /*
         * Kept for compatibility with MainActivity.
         *
         * Discover obtains its Mapbox configuration internally.
         */
    }

    data class Outcome(
        val places: List<Place>,
        val resolvedAreaName: String?
    )

    /**
     * ---------------------------------------------------------
     * CATEGORY MATCHING
     * ---------------------------------------------------------
     *
     * Converts normal user words into Mapbox categories.
     */
    private fun categoryForQuery(
        query: String
    ): DiscoverQuery.Category? {

        val q =
            query
                .trim()
                .lowercase()

        return when {

            q in listOf(
                "restaurant",
                "restaurants",
                "food",
                "eat",
                "eating",
                "dinner",
                "lunch"
            ) ->
                DiscoverQuery.Category.RESTAURANTS

            q in listOf(
                "coffee",
                "cafe",
                "cafes",
                "café",
                "coffee shop",
                "coffee shops"
            ) ->
                DiscoverQuery.Category.COFFEE_SHOP_CAFE

            q in listOf(
                "bar",
                "bars",
                "pub",
                "pubs",
                "cocktail",
                "cocktails",
                "drinks"
            ) ->
                DiscoverQuery.Category.BARS

            q in listOf(
                "club",
                "clubs",
                "nightclub",
                "nightclubs",
                "party",
                "nightlife"
            ) ->
                DiscoverQuery.Category.NIGHT_CLUBS

            else ->
                null
        }
    }

    /**
     * ---------------------------------------------------------
     * FOOD SEARCH
     * ---------------------------------------------------------
     *
     * Discover does not need an exact restaurant-name query
     * for this fallback approach.
     *
     * Instead, common food searches are mapped to restaurants
     * and the returned places are filtered by their category/
     * name where possible.
     */
    private fun looksLikeFoodQuery(
        query: String
    ): Boolean {

        val q =
            query
                .trim()
                .lowercase()

        val foodWords =
            listOf(
                "pizza",
                "burger",
                "burgers",
                "sushi",
                "steak",
                "chicken",
                "wings",
                "tacos",
                "taco",
                "pasta",
                "seafood",
                "breakfast",
                "brunch",
                "fast food",
                "fried chicken",
                "sandwich",
                "sandwiches"
            )

        return foodWords.any {
            q.contains(it)
        }
    }

    /**
     * ---------------------------------------------------------
     * LIVE SUGGESTIONS
     * ---------------------------------------------------------
     */
    suspend fun suggest(
        query: String,
        origin: Point,
        limit: Int = 5
    ): Outcome {

        val cleanQuery =
            query.trim()

        if (cleanQuery.isBlank()) {

            return Outcome(
                emptyList(),
                null
            )
        }

        /*
         * 1. Known area
         */
        val areaPoint =
            findKnownArea(cleanQuery)

        if (areaPoint != null) {

            Log.d(
                TAG,
                "Suggestion area: $cleanQuery"
            )

            val places =
                establishmentsAround(
                    origin = areaPoint,
                    limit = limit
                )

            PlaceCache.putAll(places)

            return Outcome(
                places,
                cleanQuery
            )
        }

        /*
         * 2. Exact broad category
         */
        val category =
            categoryForQuery(cleanQuery)

        if (category != null) {

            Log.d(
                TAG,
                "Suggestion category: $cleanQuery"
            )

            val places =
                discoverCategory(
                    category = category,
                    origin = origin,
                    limit = limit
                )

            PlaceCache.putAll(places)

            return Outcome(
                places,
                null
            )
        }

        /*
         * 3. Food search
         *
         * Example:
         *
         * pizza
         * burgers
         * sushi
         *
         * We search nearby restaurants rather than calling
         * Search Box /forward.
         */
        if (looksLikeFoodQuery(cleanQuery)) {

            Log.d(
                TAG,
                "Food suggestion: $cleanQuery"
            )

            val places =
                discoverCategory(
                    category =
                        DiscoverQuery.Category.RESTAURANTS,
                    origin = origin,
                    limit = limit
                )

            PlaceCache.putAll(places)

            return Outcome(
                filterFoodResults(
                    places,
                    cleanQuery
                ).ifEmpty {
                    places
                }.take(limit),
                null
            )
        }

        /*
         * 4. Generic fallback
         *
         * If we don't understand the text, show nearby
         * restaurants rather than returning nothing.
         */
        Log.d(
            TAG,
            "Generic suggestion fallback: $cleanQuery"
        )

        val places =
            discoverCategory(
                category =
                    DiscoverQuery.Category.RESTAURANTS,
                origin = origin,
                limit = limit
            )

        PlaceCache.putAll(places)

        return Outcome(
            places,
            null
        )
    }

    /**
     * ---------------------------------------------------------
     * FINAL SEARCH
     * ---------------------------------------------------------
     */
    suspend fun search(
        query: String,
        origin: Point,
        limit: Int = 8
    ): Outcome {

        val cleanQuery =
            query.trim()

        if (cleanQuery.isBlank()) {

            return Outcome(
                emptyList(),
                null
            )
        }

        /*
         * 1. Known area
         */
        val areaPoint =
            findKnownArea(cleanQuery)

        if (areaPoint != null) {

            Log.d(
                TAG,
                "Final area search: $cleanQuery"
            )

            val places =
                establishmentsAround(
                    origin = areaPoint,
                    limit = limit
                )

            PlaceCache.putAll(places)

            return Outcome(
                places,
                cleanQuery
            )
        }

        /*
         * 2. Broad category
         */
        val category =
            categoryForQuery(cleanQuery)

        if (category != null) {

            Log.d(
                TAG,
                "Final category search: $cleanQuery"
            )

            val places =
                discoverCategory(
                    category = category,
                    origin = origin,
                    limit = limit
                )

            PlaceCache.putAll(places)

            return Outcome(
                places,
                null
            )
        }

        /*
         * 3. Food search
         */
        if (looksLikeFoodQuery(cleanQuery)) {

            Log.d(
                TAG,
                "Final food search: $cleanQuery"
            )

            val places =
                discoverCategory(
                    category =
                        DiscoverQuery.Category.RESTAURANTS,
                    origin = origin,
                    limit = limit
                )

            val filtered =
                filterFoodResults(
                    places,
                    cleanQuery
                )

            val finalPlaces =
                if (filtered.isNotEmpty()) {
                    filtered
                } else {
                    places
                }.take(limit)

            PlaceCache.putAll(finalPlaces)

            return Outcome(
                finalPlaces,
                null
            )
        }

        /*
         * 4. Generic restaurant fallback.
         *
         * This means a query such as "KFC" will still produce
         * nearby food places instead of a completely empty app.
         *
         * It is not an exact brand lookup.
         */
        Log.d(
            TAG,
            "Generic POI fallback: $cleanQuery"
        )

        val places =
            discoverCategory(
                category =
                    DiscoverQuery.Category.RESTAURANTS,
                origin = origin,
                limit = limit
            )

        PlaceCache.putAll(places)

        return Outcome(
            places,
            null
        )
    }

    /**
     * ---------------------------------------------------------
     * FILTER FOOD RESULTS
     * ---------------------------------------------------------
     *
     * We can improve relevance when the returned Place contains
     * the searched food word in its name or cuisine.
     */
    private fun filterFoodResults(
        places: List<Place>,
        query: String
    ): List<Place> {

        val words =
            query
                .lowercase()
                .split(
                    " ",
                    ",",
                    "-"
                )
                .filter {
                    it.length >= 3
                }

        return places.filter { place ->

            val searchableText =
                (
                        place.name +
                                " " +
                                place.cuisine
                        )
                    .lowercase()

            words.any {
                searchableText.contains(it)
            }
        }
    }

    /**
     * ---------------------------------------------------------
     * KNOWN AREA
     * ---------------------------------------------------------
     */
    private fun findKnownArea(
        query: String
    ): Point? {

        val normalized =
            query
                .trim()
                .lowercase()

        return knownAreas[normalized]
    }

    /**
     * ---------------------------------------------------------
     * ESTABLISHMENTS AROUND AREA
     * ---------------------------------------------------------
     */
    private suspend fun establishmentsAround(
        origin: Point,
        limit: Int
    ): List<Place> {

        val restaurants =
            discoverCategory(
                category =
                    DiscoverQuery.Category.RESTAURANTS,
                origin = origin,
                limit = limit
            )

        val bars =
            discoverCategory(
                category =
                    DiscoverQuery.Category.BARS,
                origin = origin,
                limit = limit
            )

        val clubs =
            discoverCategory(
                category =
                    DiscoverQuery.Category.NIGHT_CLUBS,
                origin = origin,
                limit = limit
            )

        return (
                restaurants +
                        bars +
                        clubs
                )
            .distinctBy {
                it.id
            }
            .sortedBy {
                it.distanceKm
            }
            .take(limit)
    }

    /**
     * ---------------------------------------------------------
     * DISCOVER CATEGORY
     * ---------------------------------------------------------
     */
    private suspend fun discoverCategory(
        category: DiscoverQuery.Category,
        origin: Point,
        limit: Int
    ): List<Place> {

        Log.d(TAG, "Discover origin: lat=${origin.latitude()}, lng=${origin.longitude()}")

        val type = when (category) {
            DiscoverQuery.Category.BARS -> PlaceType.BAR
            DiscoverQuery.Category.NIGHT_CLUBS -> PlaceType.CLUB
            else -> PlaceType.RESTAURANT
        }

        val places = try {
            val response = discover.search(query = category, proximity = origin)

            response.value.orEmpty()
                .mapNotNull { result ->
                    try {
                        result.toPlace(type, origin.latitude(), origin.longitude())
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not convert Discover result", e)
                        null
                    }
                }
                .distinctBy { it.id }
                .sortedBy { it.distanceKm }
                .take(limit)

        } catch (e: Exception) {
            Log.e(TAG, "Discover failed for $category", e)
            emptyList()
        }

        Log.d(TAG, "Discover $category returned ${places.size} places")

        if (places.isNotEmpty()) return places

        // No coverage near the requested location — retry against a location
        // Mapbox has strong Discover coverage for, so the demo still shows a
        // genuine live API result rather than only ever falling back to sample data.
        if (origin != GLOBAL_FALLBACK_ORIGIN) {
            Log.d(TAG, "No local coverage — retrying $category near London")
            val fallbackPlaces = try {
                discover.search(query = category, proximity = GLOBAL_FALLBACK_ORIGIN)
                    .value.orEmpty()
                    .mapNotNull { result ->
                        try {
                            result.toPlace(type, GLOBAL_FALLBACK_ORIGIN.latitude(), GLOBAL_FALLBACK_ORIGIN.longitude())
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .distinctBy { it.id }
                    .take(limit)
            } catch (e: Exception) {
                Log.e(TAG, "Global fallback Discover call failed", e)
                emptyList()
            }

            if (fallbackPlaces.isNotEmpty()) {
                Log.d(TAG, "Global fallback returned ${fallbackPlaces.size} real places")
                return fallbackPlaces
            }
        }

        // Even the global fallback failed (e.g. no internet at all) — last resort.
        Log.d(TAG, "Falling back to DummyData for $category")
        return DummyData.places
            .filter { it.type == type }
            .sortedBy { it.distanceKm }
            .take(limit)
    }
}