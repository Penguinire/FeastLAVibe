package com.example.feastvibe

/**
 * Stores the filters selected by the user.
 * Filters are only applied after the user taps Show Results.
 */
object FilterState {

    var type: PlaceType? = null
    var cuisines: Set<String> = emptySet()
    var price: String? = null
    var minRating: Double = 0.0
    var openNowOnly: Boolean = false
    var within5kmOnly: Boolean = false
    var isActive: Boolean = false

    fun matches(place: Place): Boolean {
        if (!isActive) return true

        val matchesType =
            type == null || place.type == type

        val matchesCuisine =
            cuisines.isEmpty() || cuisines.any { cuisine ->
                place.tags.any {
                    it.contains(cuisine, ignoreCase = true)
                } || place.cuisine.contains(cuisine, ignoreCase = true)
            }

        val matchesPrice =
            price == null || place.price == price

        // A venue without a verified rating cannot pass a rating filter.
        val matchesRating =
            minRating == 0.0 ||
                    (place.rating != null && place.rating >= minRating)

        // Unknown opening hours cannot pass Open Now.
        val matchesOpen =
            !openNowOnly || place.isOpen == true

        val matchesDistance =
            !within5kmOnly || place.distanceKm <= 5.0

        return matchesType &&
                matchesCuisine &&
                matchesPrice &&
                matchesRating &&
                matchesOpen &&
                matchesDistance
    }

    fun reset() {
        type = null
        cuisines = emptySet()
        price = null
        minRating = 0.0
        openNowOnly = false
        within5kmOnly = false
        isActive = false
    }
}