package com.example.feastvibe

/**
 * In-memory cache of real (Mapbox-sourced) places, keyed by their Mapbox ID.
 * DummyData.byId() checks this first. Without it, a real search result's ID
 * wouldn't be found later (e.g. from RestaurantDetailFragment or MapFragment),
 * and would silently fall back to the wrong dummy place.
 *
 * Not persisted — resets on app restart, same as FavouritesStore.
 */
object PlaceCache {
    private val cache = mutableMapOf<String, Place>()

    fun put(place: Place) {
        cache[place.id] = place
    }

    fun putAll(places: List<Place>) {
        places.forEach { put(it) }
    }

    fun get(id: String): Place? = cache[id]

    fun all(): List<Place> = cache.values.toList()
}