
package com.example.feastvibe

import android.util.Log
import kotlinx.coroutines.CancellationException
import java.util.Locale

/**
 * Live place search powered by Geoapify.
 *
 * This class does not depend on  MapLibre.
 * The caller supplies the search coordinates and API key.
 */
object GeoapifyPlaceSearch {

    private const val TAG = "GeoapifyPlaceSearch"
    private const val DEFAULT_RADIUS_METERS = 5000
    private const val DEFAULT_LIMIT = 20

    private data class QueryPlan(
        val categories: String,
        val name: String? = null
    )

    suspend fun search(
        query: String,
        origin: GeoPoint,
        apiKey: String,
        radiusMeters: Int = DEFAULT_RADIUS_METERS,
        limit: Int = DEFAULT_LIMIT
    ): List<Place> {

        val cleanQuery = query.trim()

        if (cleanQuery.isBlank() || apiKey.isBlank()) {
            Log.w(TAG, "Search skipped: query or API key is empty")
            return emptyList()
        }

        val plan = createQueryPlan(cleanQuery)
        val radius = radiusMeters.coerceIn(500, 25000)
        val resultLimit = limit.coerceIn(1, 50)

        return try {
            val response = GeoapifyClient.api.searchPlaces(
                categories = plan.categories,
                filter = "circle:${origin.longitude}," +
                        "${origin.latitude},$radius",
                limit = resultLimit,
                apiKey = apiKey,
                name = plan.name,
                bias = "proximity:${origin.longitude}," +
                        "${origin.latitude}"
            )

            response.features
                .mapNotNull { feature ->
                    convertToPlace(feature.properties, origin)
                }
                .distinctBy { it.id }
                .sortedBy { it.distanceKm }
                .take(resultLimit)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Geoapify search failed for '$cleanQuery'", e)
            emptyList()
        }
    }

    private fun createQueryPlan(query: String): QueryPlan {
        val q = query.trim().lowercase(Locale.ROOT)

        return when (q) {
            "restaurant", "restaurants", "food", "eating",
            "dinner", "lunch" ->
                QueryPlan("catering.restaurant")

            "cafe", "cafes", "café", "coffee", "coffee shop",
            "coffee shops" ->
                QueryPlan("catering.cafe,catering.cafe.coffee_shop")

            "fast food", "fastfood" ->
                QueryPlan("catering.fast_food")

            "bar", "bars", "pub", "pubs", "cocktails", "drinks" ->
                QueryPlan("catering.bar,catering.pub")

            "club", "clubs", "nightclub", "nightclubs", "nightlife" ->
                QueryPlan("adult.nightclub")

            "pizza" ->
                QueryPlan(
                    "catering.restaurant.pizza," +
                            "catering.fast_food.pizza"
                )

            "burger", "burgers" ->
                QueryPlan(
                    "catering.restaurant.burger," +
                            "catering.fast_food.burger"
                )

            "sushi" ->
                QueryPlan("catering.restaurant.sushi")

            "chicken" ->
                QueryPlan(
                    "catering.restaurant.chicken," +
                            "catering.fast_food"
                )

            "steak", "steakhouse", "steakhouses" ->
                QueryPlan("catering.restaurant.steak_house")

            "sushi restaurant" ->
                QueryPlan("catering.restaurant.sushi")

            else ->
                QueryPlan(
                    categories = "catering.restaurant," +
                            "catering.fast_food," +
                            "catering.cafe," +
                            "catering.bar," +
                            "catering.pub," +
                            "adult.nightclub",
                    name = query
                )
        }
    }

    private fun convertToPlace(
        properties: GeoapifyPlaceProperties,
        origin: GeoPoint
    ): Place? {

        val name = properties.name
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val lat = properties.latitude ?: return null
        val lng = properties.longitude ?: return null

        if (!lat.isFinite() || !lng.isFinite()) return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            return null
        }

        val categories = properties.categories
        val type = inferType(categories)

        val area = properties.addressLine2
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: properties.formattedAddress
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            ?: properties.addressLine1
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            ?: "Address unavailable"

        val cuisine = categoryLabel(categories, type)

        val id = properties.placeId
            ?.takeIf { it.isNotBlank() }
            ?: "geoapify_${lat}_${lng}"

        val tags = categories
            .map { it.substringAfterLast('.') }
            .map { it.replace('_', ' ') }
            .distinct()
            .take(5)

        return Place(
            id = id,
            name = name,
            cuisine = cuisine,
            price = null,
            rating = null,
            reviewCount = null,
            distanceKm = distanceBetweenKm(
                origin.latitude,
                origin.longitude,
                lat,
                lng
            ),
            area = area,
            isOpen = null,
            type = type,
            phone = properties.phone,
            website = properties.website,
            lat = lat,
            lng = lng,
            tags = tags,
            generatorBackup = false
        )
    }

    private fun inferType(categories: List<String>): PlaceType {
        return when {
            categories.any {
                it.startsWith("adult.nightclub")
            } -> PlaceType.CLUB

            categories.any {
                it == "catering.bar" ||
                        it == "catering.pub" ||
                        it.startsWith("catering.bar.")
            } -> PlaceType.BAR

            categories.any {
                it.startsWith("catering.fast_food")
            } -> PlaceType.FAST_FOOD

            else -> PlaceType.RESTAURANT
        }
    }

    private fun categoryLabel(
        categories: List<String>,
        type: PlaceType
    ): String {
        val specificCategory = categories.firstOrNull {
            it.startsWith("catering.") ||
                    it.startsWith("adult.nightclub")
        }

        if (specificCategory != null) {
            val label = specificCategory
                .substringAfterLast('.')
                .replace('_', ' ')
                .replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT)
                    else it.toString()
                }

            if (label.isNotBlank()) return label
        }

        return when (type) {
            PlaceType.RESTAURANT -> "Restaurant"
            PlaceType.FAST_FOOD -> "Fast Food"
            PlaceType.BAR -> "Bar"
            PlaceType.CLUB -> "Nightclub"
        }
    }
}
