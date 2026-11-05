package com.example.feastvibe

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

object FavouritesStore {

    private const val PREFS_NAME = "feast_vibe_favourites"
    private const val KEY_PLACES = "saved_places"

    private val favouritePlaces = linkedMapOf<String, Place>()
    private var initialised = false
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (initialised) return

        appContext = context.applicationContext

        val prefs = appContext!!.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val savedJson = prefs.getString(KEY_PLACES, "[]") ?: "[]"

        try {
            val array = JSONArray(savedJson)

            for (i in 0 until array.length()) {
                val place = placeFromJson(array.getJSONObject(i))
                favouritePlaces[place.id] = place
                PlaceCache.put(place)
            }
        } catch (e: Exception) {
            Log.e("FavouritesStore", "Could not load local favourites", e)
        }

        initialised = true
        syncFromFirestore()
    }

    fun isFavourite(placeId: String): Boolean {
        return favouritePlaces.containsKey(placeId)
    }

    /**
     * Toggles a favourite and immediately saves it locally.
     * Firestore synchronises the change when connectivity is available.
     */
    fun toggle(place: Place): Boolean {
        val isNowFavourite = if (favouritePlaces.containsKey(place.id)) {
            favouritePlaces.remove(place.id)
            false
        } else {
            favouritePlaces[place.id] = place
            PlaceCache.put(place)
            true
        }

        saveLocally()
        syncChange(place, isNowFavourite)

        return isNowFavourite
    }

    fun all(): List<Place> {
        return favouritePlaces.values.toList()
    }

    private fun saveLocally() {
        val array = JSONArray()

        favouritePlaces.values.forEach { place ->
            array.put(placeToJson(place))
        }

        appContext?.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )?.edit()?.putString(KEY_PLACES, array.toString())?.apply()
    }

    private fun syncChange(place: Place, isFavourite: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val document = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("favourites")
            .document(place.id)

        if (isFavourite) {
            document.set(
                mapOf("placeJson" to placeToJson(place).toString())
            ).addOnFailureListener {
                Log.e("FavouritesStore", "Favourite sync failed", it)
            }
        } else {
            document.delete().addOnFailureListener {
                Log.e("FavouritesStore", "Favourite removal sync failed", it)
            }
        }
    }

    private fun syncFromFirestore() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("favourites")
            .get()
            .addOnSuccessListener { snapshot ->
                var changed = false

                for (document in snapshot.documents) {
                    val json = document.getString("placeJson") ?: continue

                    try {
                        val place = placeFromJson(JSONObject(json))

                        if (!favouritePlaces.containsKey(place.id)) {
                            favouritePlaces[place.id] = place
                            PlaceCache.put(place)
                            changed = true
                        }
                    } catch (e: Exception) {
                        Log.e("FavouritesStore", "Could not restore cloud favourite", e)
                    }
                }

                if (changed) saveLocally()
            }
            .addOnFailureListener {
                Log.e("FavouritesStore", "Could not sync cloud favourites", it)
            }
    }

    private fun placeToJson(place: Place): JSONObject {
        val tags = JSONArray()
        place.tags.forEach { tags.put(it) }

        return JSONObject().apply {
            put("id", place.id)
            put("name", place.name)
            put("cuisine", place.cuisine)
            put("price", place.price ?: JSONObject.NULL)
            put("rating", place.rating ?: JSONObject.NULL)
            put("reviewCount", place.reviewCount ?: JSONObject.NULL)
            put("distanceKm", place.distanceKm)
            put("area", place.area)
            put("isOpen", place.isOpen ?: JSONObject.NULL)
            put("type", place.type.name)
            put("phone", place.phone ?: JSONObject.NULL)
            put("website", place.website ?: JSONObject.NULL)
            put("lat", place.lat)
            put("lng", place.lng)
            put("tags", tags)
            put("generatorBackup", place.generatorBackup)
        }
    }

    private fun placeFromJson(json: JSONObject): Place {
        val tagsJson = json.optJSONArray("tags") ?: JSONArray()
        val tags = mutableListOf<String>()

        for (i in 0 until tagsJson.length()) {
            tags.add(tagsJson.optString(i))
        }

        return Place(
            id = json.getString("id"),
            name = json.getString("name"),
            cuisine = json.optString("cuisine", ""),
            price = if (json.isNull("price")) null else json.optString("price"),
            rating = if (json.isNull("rating")) null else json.optDouble("rating"),
            reviewCount = if (json.isNull("reviewCount")) null else json.optInt("reviewCount"),
            distanceKm = json.optDouble("distanceKm"),
            area = json.optString("area", ""),
            isOpen = if (json.isNull("isOpen")) null else json.optBoolean("isOpen"),
            type = PlaceType.valueOf(json.getString("type")),
            phone = if (json.isNull("phone")) null else json.optString("phone"),
            website = if (json.isNull("website")) null else json.optString("website"),
            lat = json.getDouble("lat"),
            lng = json.getDouble("lng"),
            tags = tags,
            generatorBackup = json.optBoolean("generatorBackup", true)
        )
    }
}