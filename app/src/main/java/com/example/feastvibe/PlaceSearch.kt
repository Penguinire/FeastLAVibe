
package com.example.feastvibe

import android.content.Context
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Shared search logic for Feast & Vibe.
 *
 * Geoapify provides live place search.
 * Firestore provides additional verified curated venues.
 * Both are converted into the existing Place model.
 */
object PlaceSearch {

    private const val TAG = "PlaceSearch"

    private val knownAreas = mapOf(
        "hatfield" to GeoPoint.fromLngLat(28.2378, -25.7487),
        "menlyn" to GeoPoint.fromLngLat(28.2775, -25.7826),
        "menlyn maine" to GeoPoint.fromLngLat(28.2775, -25.7826),
        "pretoria cbd" to GeoPoint.fromLngLat(28.1881, -25.7461),
        "cbd" to GeoPoint.fromLngLat(28.1881, -25.7461)
    )

    fun initialize(context: Context) {
        // Geoapify is configured through BuildConfig.GEOAPIFY_API_KEY.
    }

    data class Outcome(
        val places: List<Place>,
        val resolvedAreaName: String?
    )

    private suspend fun searchGeoapify(
        query: String,
        origin: GeoPoint,
        limit: Int
    ): List<Place> {
        return GeoapifyPlaceSearch.search(
            query = query,
            origin = origin,
            apiKey = BuildConfig.GEOAPIFY_API_KEY,
            limit = limit
        )
    }

    /**
     * Converts the existing callback-based Firestore repository
     * into a suspend function so it works with PlaceSearch.
     *
     * If Firestore fails, return an empty list and allow
     * Geoapify results to remain available.
     */
    private suspend fun fetchCuratedVenues(
        origin: GeoPoint
    ): List<Place> = suspendCancellableCoroutine { continuation ->

        CuratedVenueRepository.fetchVerifiedVenues(
            originLat = origin.latitude,
            originLng = origin.longitude,

            onSuccess = { venues ->
                if (continuation.isActive) {
                    continuation.resume(venues)
                }
            },

            onError = { exception ->
                Log.e(TAG, "Could not load curated venues", exception)

                if (continuation.isActive) {
                    continuation.resume(emptyList())
                }
            }
        )
    }

    private fun categoryForQuery(query: String): String? {
        return when (query.trim().lowercase(Locale.ROOT)) {
            "restaurant", "restaurants", "food", "eat",
            "eating", "dinner", "lunch" -> "restaurants"

            "coffee", "cafe", "cafes", "café",
            "coffee shop", "coffee shops" -> "cafes"

            "bar", "bars", "pub", "pubs",
            "cocktail", "cocktails", "drinks" -> "bars"

            "club", "clubs", "nightclub", "nightclubs",
            "party", "nightlife" -> "clubs"

            "fast food", "fastfood" -> "fast food"

            else -> null
        }
    }

    private fun looksLikeFoodQuery(query: String): Boolean {
        val q = query.trim().lowercase(Locale.ROOT)

        val foodWords = listOf(
            "pizza", "burger", "burgers", "sushi", "steak",
            "chicken", "wings", "tacos", "taco", "pasta",
            "seafood", "breakfast", "brunch", "fast food",
            "fried chicken", "sandwich", "sandwiches"
        )

        return foodWords.any { q.contains(it) }
    }

    /**
     * Filters curated venues according to the search query.
     */
    private fun filterCuratedVenues(
        venues: List<Place>,
        query: String,
        category: String?
    ): List<Place> {

        val cleanQuery = query.trim().lowercase(Locale.ROOT)

        if (cleanQuery.isBlank()) return emptyList()

        // Category searches use the existing PlaceType model.
        if (category != null) {
            return when (category) {
                "restaurants" -> venues.filter {
                    it.type == PlaceType.RESTAURANT
                }

                "cafes" -> venues.filter {
                    it.type == PlaceType.RESTAURANT
                }

                "bars" -> venues.filter {
                    it.type == PlaceType.BAR
                }

                "clubs" -> venues.filter {
                    it.type == PlaceType.CLUB
                }

                "fast food" -> venues.filter {
                    it.type == PlaceType.FAST_FOOD
                }

                else -> emptyList()
            }
        }

        // Match names, cuisine, address/area and tags.
        val words = cleanQuery
            .split(" ", ",", "-")
            .filter { it.length >= 2 }

        return venues.filter { place ->

            val searchableText = (
                    "${place.name} " +
                            "${place.cuisine} " +
                            "${place.area} " +
                            place.tags.joinToString(" ")
                    ).lowercase(Locale.ROOT)

            words.any { word ->
                searchableText.contains(word)
            }
        }
    }

    suspend fun suggest(
        query: String,
        origin: GeoPoint,
        limit: Int = 5
    ): Outcome {
        return runSearch(
            query = query,
            origin = origin,
            limit = limit,
            isSuggestion = true
        )
    }

    suspend fun search(
        query: String,
        origin: GeoPoint,
        limit: Int = 8
    ): Outcome {
        return runSearch(
            query = query,
            origin = origin,
            limit = limit,
            isSuggestion = false
        )
    }

    private suspend fun runSearch(
        query: String,
        origin: GeoPoint,
        limit: Int,
        isSuggestion: Boolean
    ): Outcome {

        val cleanQuery = query.trim()

        if (cleanQuery.isBlank()) {
            return Outcome(emptyList(), null)
        }

        val safeLimit = limit.coerceIn(1, 50)
        val normalized = cleanQuery.lowercase(Locale.ROOT)

        // Check whether the query is a known area.
        val areaPoint = knownAreas[normalized]

        if (areaPoint != null) {
            Log.d(TAG, "Searching known area: $cleanQuery")

            val geoapifyPlaces = establishmentsAround(
                origin = areaPoint,
                limit = safeLimit
            )

            val curatedPlaces = fetchCuratedVenues(areaPoint)
                .filterCuratedForArea(cleanQuery)

            val combinedPlaces = (
                    geoapifyPlaces + curatedPlaces
                    )
                .distinctBy { it.id }
                .sortedBy { it.distanceKm }
                .take(safeLimit)

            PlaceCache.putAll(combinedPlaces)

            return Outcome(
                places = combinedPlaces,
                resolvedAreaName = cleanQuery
            )
        }

        val category = categoryForQuery(cleanQuery)

        // Search Geoapify using the existing search rules.
        val geoapifyPlaces = when {
            category != null -> {
                Log.d(TAG, "Searching category: $category")
                searchGeoapify(category, origin, safeLimit)
            }

            looksLikeFoodQuery(cleanQuery) -> {
                Log.d(TAG, "Searching food query: $cleanQuery")

                val results = searchGeoapify(
                    cleanQuery,
                    origin,
                    safeLimit
                )

                val filtered = filterFoodResults(results, cleanQuery)

                if (filtered.isNotEmpty()) filtered else results
            }

            else -> {
                Log.d(TAG, "Searching general query: $cleanQuery")
                searchGeoapify(cleanQuery, origin, safeLimit)
            }
        }

        // Fetch and filter verified Firestore venues.
        val allCuratedPlaces = fetchCuratedVenues(origin)

        Log.d(
            TAG,
            "Firestore venues loaded: ${allCuratedPlaces.map { it.name }}"
        )

        val curatedPlaces = filterCuratedVenues(
            venues = allCuratedPlaces,
            query = cleanQuery,
            category = category
        )

        Log.d(
            TAG,
            "Curated matches for '$cleanQuery': ${curatedPlaces.map { it.name }}"
        )

        // Combine both sources without duplicate IDs.
        val combinedBeforeLimit = (
                geoapifyPlaces + curatedPlaces
                )
            .distinctBy { it.id }
            .sortedBy { it.distanceKm }

        Log.d(
            TAG,
            "Combined results before limit: ${combinedBeforeLimit.map { it.name }}"
        )

        val combinedPlaces = combinedBeforeLimit.take(safeLimit)

        Log.d(
            TAG,
            "Final results after limit: ${combinedPlaces.map { it.name }}"
        )

        // Keep real results available to the detail screens.
        PlaceCache.putAll(combinedPlaces)

        return Outcome(
            places = combinedPlaces,
            resolvedAreaName = null
        )
    }

    /**
     * Filters curated venues to match a known area search.
     */
    private fun List<Place>.filterCuratedForArea(
        areaName: String
    ): List<Place> {

        val query = areaName.lowercase(Locale.ROOT)

        return filter { place ->
            place.area.lowercase(Locale.ROOT).contains(query) ||
                    query.contains(place.area.lowercase(Locale.ROOT))
        }
    }

    private suspend fun establishmentsAround(
        origin: GeoPoint,
        limit: Int
    ): List<Place> {

        val restaurants = searchGeoapify("restaurants", origin, limit)
        val bars = searchGeoapify("bars", origin, limit)
        val clubs = searchGeoapify("clubs", origin, limit)
        val fastFood = searchGeoapify("fast food", origin, limit)

        return (
                restaurants + bars + clubs + fastFood
                )
            .distinctBy { it.id }
            .sortedBy { it.distanceKm }
            .take(limit)
    }

    private fun filterFoodResults(
        places: List<Place>,
        query: String
    ): List<Place> {

        val words = query
            .lowercase(Locale.ROOT)
            .split(" ", ",", "-")
            .filter { it.length >= 3 }

        if (words.isEmpty()) return places

        return places.filter { place ->

            val searchableText =
                "${place.name} ${place.cuisine} ${place.tags.joinToString(" ")}"
                    .lowercase(Locale.ROOT)

            words.any { word ->
                searchableText.contains(word)
            }
        }
    }
}
