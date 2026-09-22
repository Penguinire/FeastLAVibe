package com.example.feastvibe

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


enum class PlaceType { RESTAURANT, FAST_FOOD, BAR, CLUB }

/**
 * Sample data model for a place shown in Explore/Search/Map/Favourites/Detail.
 * Nullable fields represent data a real Places API call might not return —
 * do not backfill these with fake defaults once the real API is connected.
 */
data class Place(
    val id: String,
    val name: String,
    val cuisine: String,
    val price: String?,
    val rating: Double?,
    val reviewCount: Int?,
    val distanceKm: Double,
    val area: String,
    val isOpen: Boolean,
    val type: PlaceType,
    val phone: String? = null,
    val website: String? = null,
    val lat: Double,
    val lng: Double,
    val tags: List<String> = emptyList(),
    val generatorBackup: Boolean = true
)

data class Review(
    val author: String,
    val initials: String,
    val verifiedTag: String?,   // e.g. "Local Guide · 42 reviews" — null if unverified
    val timeAgo: String,
    val rating: Double,
    val body: String,
    val vibeTag: String? = null,
    val helpfulCount: Int = 0
)

/**
 * SAMPLE DEVELOPMENT DATA ONLY.
 * All names, coordinates, ratings and reviews below are fictional placeholders
 * for UI development around Pretoria CBD, and are not real businesses.
 * Replace this object's contents with real Google Places SDK results later —
 * every Fragment/Adapter reads through DummyData, so nothing else needs to change.
 */
object DummyData {

    val places = listOf(
        Place("central_012", "012 Central Social Hub", "Urban Grill & Bar", "R150-R300",
            4.8, 142, 0.4, "Pretoria CBD", true, PlaceType.BAR,
            lat = -25.7461, lng = 28.1881,
            tags = listOf("Braai", "Cocktails", "Live DJ Sets")),

        Place("afro_lounge", "Afro Lounge", "African & Grill", "R150-R300",
            4.6, 98, 0.9, "Pretoria CBD", true, PlaceType.RESTAURANT,
            lat = -25.7449, lng = 28.1893,
            tags = listOf("African Cuisine", "Live Music")),

        Place("blue_room", "The Blue Room Hatfield", "Fine Dining & Jazz Bar", "R300+",
            4.9, 310, 3.1, "Hatfield, Pretoria", true, PlaceType.RESTAURANT,
            lat = -25.7487, lng = 28.2378,
            tags = listOf("Jazz", "Fine Dining")),

        Place("kream", "Kream Restaurant", "Continental & Steaks", "R300+",
            4.9, 420, 4.5, "Brooklyn, Pretoria", true, PlaceType.RESTAURANT,
            lat = -25.7715, lng = 28.2465,
            tags = listOf("Steaks", "Fine Dining")),

        Place("culture_club", "Culture Club Tapas", "Tapas & Wine", "R150-R300",
            4.7, 185, 3.8, "Hazelwood, Pretoria", true, PlaceType.RESTAURANT,
            lat = -25.7699, lng = 28.2521,
            tags = listOf("Tapas", "Wine Bar")),

        Place("propaganda", "Propaganda Nightclub", "Nightclub", "R150-R300",
            4.7, 520, 0.6, "Pretoria CBD", true, PlaceType.CLUB,
            lat = -25.7438, lng = 28.1902,
            tags = listOf("Amapiano", "VIP Booths")),

        Place("menlyn_maine", "Menlyn Maine Social", "Rooftop & Cocktails", "R150-R300",
            4.5, 210, 6.2, "Menlyn Maine, Pretoria", true, PlaceType.BAR,
            lat = -25.7826, lng = 28.2775,
            tags = listOf("Rooftop", "Cocktails")),

        Place("mashamplane", "Mashamplane Shisanyama & Lounge", "Traditional Braai & Afro Beats", "R150-R300",
            4.8, 194, 0.6, "Pretoria CBD", true, PlaceType.RESTAURANT,
            lat = -25.7502, lng = 28.1899,
            tags = listOf("Braai", "Outdoor Seating", "Live DJ Sets")),

        Place("chisa_nyama", "Chisa Nyama Sunnyside", "Braai, Wings & Pap", null,
            4.6, 142, 1.2, "Sunnyside, Pretoria", true, PlaceType.FAST_FOOD,
            lat = -25.7558, lng = 28.2039,
            tags = listOf("Braai", "Outdoor Seating", "Casual Spot")),

        Place("capital_smokehouse", "Capital Smokehouse Braai Bar", "Smoked Meats & Craft Beer", "R150-R300",
            4.7, 89, 2.4, "Hatfield, Pretoria", true, PlaceType.RESTAURANT,
            lat = -25.7479, lng = 28.2361,
            tags = listOf("Braai", "Outdoor Seating", "Craft Tap")),

        Place("jacaranda_coffee", "Jacaranda Coffee House", "Coffee & Light Bites", "R50-R150",
            4.4, 67, 0.3, "Pretoria CBD", true, PlaceType.FAST_FOOD,
            lat = -25.7455, lng = 28.1886,
            tags = listOf("Coffee", "Wi-Fi"), generatorBackup = false),

        Place("union_grill", "Union Street Grill", "Grill & Steaks", "R150-R300",
            4.3, 76, 1.8, "Arcadia, Pretoria", true, PlaceType.RESTAURANT,
            lat = -25.7419, lng = 28.2012,
            tags = listOf("Grill", "Steaks")),

        Place("sunset_rooftop", "Sunset Rooftop Kitchen", "Rooftop & Grill", "R300+",
            4.5, 133, 5.9, "Menlyn, Pretoria", true, PlaceType.BAR,
            lat = -25.7822, lng = 28.2761,
            tags = listOf("Rooftop", "Sunset Views")),

        Place("hatfield_burger", "Hatfield Burger Lab", "Burgers", "R50-R150",
            3.9, 54, 3.2, "Hatfield, Pretoria", false, PlaceType.FAST_FOOD,
            lat = -25.7491, lng = 28.2385,
            tags = listOf("Burgers", "Student Friendly"), generatorBackup = false),

        Place("centurion_grill", "Centurion Flame Grill", "Braai & Grill", "R150-R300",
            4.2, 61, 12.4, "Centurion", true, PlaceType.RESTAURANT,
            lat = -25.8601, lng = 28.1894,
            tags = listOf("Braai", "Family Friendly"))
    )

    fun byId(id: String): Place =
        PlaceCache.get(id) ?: places.firstOrNull { it.id == id } ?: places[0]

    fun nearby() = places.filter { it.distanceKm <= 1.0 }

    fun popular() = places.filter { it.type == PlaceType.RESTAURANT }

    fun nightlife() = places.filter { it.type == PlaceType.CLUB || it.type == PlaceType.BAR }

    fun ofType(type: PlaceType) = places.filter { it.type == type }

    fun search(query: String): List<Place> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return places.filter {
            it.name.lowercase().contains(q) ||
                    it.cuisine.lowercase().contains(q) ||
                    it.area.lowercase().contains(q) ||
                    it.tags.any { tag -> tag.lowercase().contains(q) }
        }
    }

    val recentSearches = listOf("Braai", "Restaurants in Pretoria CBD", "Nightlife", "Sushi")

    val suggestions = listOf(
        "Trending restaurants", "New openings", "Happy hour spots", "Weekend events"
    )

    // Shown only when a search returns no results — quick shortcuts back into browsing.
    val quickSuggestions = listOf(
        "Traditional Braai", "Rooftop Bars in Menlyn", "Live Amapiano",
        "012 Central Pretoria CBD", "Hatfield Student Strip"
    )

    // Sample reviews only — not real Feast & Vibe user submissions.
    // Sample reviews only — not real Feast & Vibe user submissions.
    fun reviewsFor(placeId: String) = listOf(
        Review("Thabo M.", "TM", "Verified Diner", "2 days ago", 5.0,
            "The smoked brisket and craft drinks are incredible. The Friday night DJ set had Pretoria CBD buzzing. Definitely getting a VIP booth next time.",
            vibeTag = "Friday Vibes", helpfulCount = 18),
        Review("Lerato Khumalo", "LK", "Verified Diner", "1 week ago", 5.0,
            "Celebrated my birthday here with friends! Great courtyard setup, secure parking, and friendly staff. Try the signature Pretoria sunset cocktail.",
            vibeTag = "Birthday Group", helpfulCount = 34),
        Review("Sipho Ndlovu", "SN", null, "2 weeks ago", 4.0,
            "Superb atmosphere and generous platters. Service was a little busy around 8 PM, but the live amapiano and deep house sets made up for it.",
            vibeTag = "Live Amapiano", helpfulCount = 11)
    )

    // Star -> percentage. Sample distribution shared across places until
    // real per-place review aggregates come from Firebase.
    fun ratingBreakdown(): List<Pair<Int, Int>> = listOf(
        5 to 78, 4 to 15, 3 to 4, 2 to 2, 1 to 1
    )
}

/**
 * Converts a Mapbox PlaceAutocompleteResult into our own Place model.
 * This is the ONLY place in the app that should know about Mapbox's result
 * classes — every Fragment/Adapter downstream only ever sees Place, so we can
 * swap search providers later without touching UI code.
 *
 * Fields Mapbox doesn't return (price, generatorBackup, most tags) fall back
 * to sensible defaults rather than fake specifics — we don't invent data.
 */
fun com.mapbox.search.autocomplete.PlaceAutocompleteResult.toPlace(): Place {
    val category = categories?.firstOrNull() ?: "Place"
    return Place(
        id = id,
        name = name,
        cuisine = category,
        price = null,
        rating = averageRating,
        reviewCount = reviewCount,
        distanceKm = (distanceMeters ?: 0.0) / 1000.0,
        area = address?.place ?: address?.formattedAddress ?: "Pretoria",
        isOpen = true,
        type = inferPlaceType(categories),
        phone = phone,
        website = website,
        lat = coordinate.latitude(),
        lng = coordinate.longitude(),
        tags = emptyList(),
        generatorBackup = false
    )
}

/**
 * Partial conversion for an autocomplete suggestion.
 * This is used for live typing results. Since it's a suggestion,
 * it may not have all fields populated yet.
 */
fun com.mapbox.search.autocomplete.PlaceAutocompleteSuggestion.toPlace(): Place {
    val category = categories?.firstOrNull() ?: "Place"
    return Place(
        id = name + "_" + coordinate?.longitude() + "_" + coordinate?.latitude(),
        name = name,
        cuisine = category,
        price = null,
        rating = null,
        reviewCount = null,
        distanceKm = (distanceMeters ?: 0.0) / 1000.0,
        area = formattedAddress ?: "Pretoria",
        isOpen = true,
        type = inferPlaceType(categories),
        phone = null,
        website = null,
        lat = coordinate?.latitude() ?: 0.0,
        lng = coordinate?.longitude() ?: 0.0,
        tags = emptyList(),
        generatorBackup = false
    )
}

/**
 * Guesses a PlaceType from Mapbox's category strings (e.g. "night_club", "bar",
 * "fast_food"). Falls back to RESTAURANT only when nothing more specific matches —
 * this replaces the old hardcoded `type = PlaceType.RESTAURANT` that made every
 * real search result look like a restaurant regardless of what it actually was.
 */
private fun inferPlaceType(categories: List<String>?): PlaceType {
    val text = categories.orEmpty().joinToString(" ").lowercase()
    return when {
        text.contains("night_club") || text.contains("nightclub") || text.contains("club") -> PlaceType.CLUB
        text.contains("bar") || text.contains("pub") || text.contains("cocktail") || text.contains("brewery") -> PlaceType.BAR
        text.contains("fast_food") || text.contains("fast food") || text.contains("burger") || text.contains("takeaway") -> PlaceType.FAST_FOOD
        else -> PlaceType.RESTAURANT
    }
}

/**
 * Converts a Mapbox Discover (category search) result into our Place model.
 * Same reasoning as toPlace() above — this is the only place that should know
 * about DiscoverResult's shape.
 */
fun com.mapbox.search.discover.DiscoverResult.toPlace(
    inferredType: PlaceType,
    originLat: Double,
    originLng: Double
): Place {
    return Place(
        id = id,
        name = name,
        cuisine = categories.firstOrNull() ?: "Place",
        price = null,
        rating = null,
        reviewCount = null,
        distanceKm = distanceBetweenKm(originLat, originLng, coordinate.latitude(), coordinate.longitude()),
        area = address.place ?: address.formattedAddress ?: "Pretoria",
        isOpen = true,
        type = inferredType,
        phone = null,
        website = null,
        lat = coordinate.latitude(),
        lng = coordinate.longitude(),
        tags = emptyList(),
        generatorBackup = false
    )
}

/** Straight-line distance in km between two coordinates (Haversine formula). */
fun distanceBetweenKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}






