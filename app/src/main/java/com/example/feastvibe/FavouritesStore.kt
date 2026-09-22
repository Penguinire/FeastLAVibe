package com.example.feastvibe

/**
 * In-memory favourites store shared across the app for this session only.
 * Not persisted — resets on app restart until Firebase favourites are connected.
 */
object FavouritesStore {
    private val favouriteIds = mutableSetOf<String>()

    fun isFavourite(placeId: String) = favouriteIds.contains(placeId)

    /** Returns the new state (true = now favourited). */
    fun toggle(placeId: String): Boolean {
        if (favouriteIds.contains(placeId)) favouriteIds.remove(placeId) else favouriteIds.add(
            placeId
        )
        return favouriteIds.contains(placeId)
    }

    fun all(): List<Place> = favouriteIds.mapNotNull { id ->
        PlaceCache.get(id) ?: DummyData.places.firstOrNull { it.id == id }
    }
}