package com.example.feastvibe

import android.content.Context

/*
 * Search Box /forward is currently not suitable for our
 * South African POI search.
 *
 * Keep this object only as a compatibility layer so the rest
 * of the project does not need to know that we removed the
 * Search Box implementation.
 *
 * PlaceSearch now uses Mapbox Discover instead.
 */
object MapboxForwardSearch {

    fun initialize(context: Context) {
        /*
         * Nothing is required here anymore.
         *
         * Kept so existing:
         *
         * PlaceSearch.initialize(this)
         *
         * does not break.
         */
    }
}