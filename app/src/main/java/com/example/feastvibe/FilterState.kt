package com.example.feastvibe

/**
 * Holds whatever the user last selected in FiltersFragment, so Explore can
 * apply it after "Show Results" is tapped. Same in-memory singleton pattern
 * as PlaceCache/FavouritesStore — resets on app restart, which is fine here.
 */
object FilterState {
    var type: PlaceType? = null
    var cuisines: Set<String> = emptySet()
    var price: String? = null
    var minRating: Double = 0.0
    var openNowOnly: Boolean = false
    var within5kmOnly: Boolean = false
    var loadsheddingOnly: Boolean = false
    var isActive: Boolean = false // becomes true once the user actually applies a filter

    fun matches(place: Place): Boolean {
        if (!isActive) return true
        return (type == null || place.type == type) &&
                (cuisines.isEmpty() || cuisines.any { c ->
                    place.tags.any { it.contains(c, ignoreCase = true) } ||
                            place.cuisine.contains(c, ignoreCase = true)
                }) &&
                (price == null || place.price == price) &&
                (place.rating == null || place.rating >= minRating) &&
                (!openNowOnly || place.isOpen) &&
                (!within5kmOnly || place.distanceKm <= 5.0) &&
                (!loadsheddingOnly || place.generatorBackup)
    }

    fun reset() {
        type = null; cuisines = emptySet(); price = null; minRating = 0.0
        openNowOnly = false; within5kmOnly = false; loadsheddingOnly = false
        isActive = false
    }
}