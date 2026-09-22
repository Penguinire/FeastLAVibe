package com.example.feastvibe

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Our own Event model — adapted from Bear's events/model/Event.kt.
 * Everything else in the app (EventsFragment) only ever touches this,
 * never the raw Ticketmaster response shapes below.
 */
data class Event(
    val id: String,
    val name: String,
    val date: String,
    val time: String,
    val venue: String,
    val address: String,
    val city: String,
    val latitude: Double?,
    val longitude: Double?,
    val imageUrl: String?,
    val description: String?,
    val ticketUrl: String?,
    val status: String?
)

// ---- Raw Ticketmaster API response shapes (private to this file) ----

private data class TicketmasterResponse(@SerializedName("_embedded") val embedded: EmbeddedEvents?)
private data class EmbeddedEvents(@SerializedName("events") val events: List<TicketmasterEvent>?)
private data class TicketmasterEvent(
    val id: String?, val name: String?, val url: String?,
    val description: String?, val info: String?, val pleaseNote: String?,
    val images: List<TicketmasterImage>?, val dates: TicketmasterDates?,
    @SerializedName("_embedded") val embedded: TicketmasterEmbedded?
)
private data class TicketmasterImage(val url: String?, val width: Int?, val height: Int?, val fallback: Boolean?)
private data class TicketmasterDates(val start: TicketmasterStart?, val status: TicketmasterStatus?)
private data class TicketmasterStart(val localDate: String?, val localTime: String?)
private data class TicketmasterStatus(val code: String?)
private data class TicketmasterEmbedded(val venues: List<TicketmasterVenue>?)
private data class TicketmasterVenue(
    val name: String?, val address: TicketmasterAddress?,
    val city: TicketmasterCity?, val location: TicketmasterLocation?
)
private data class TicketmasterAddress(val line1: String?)
private data class TicketmasterCity(val name: String?)
private data class TicketmasterLocation(val latitude: String?, val longitude: String?)

private interface TicketmasterApi {
    @GET("events.json")
    suspend fun getEvents(
        @Query("apikey") apiKey: String,
        @Query("countryCode") countryCode: String = "ZA",
        @Query("city") city: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("size") size: Int = 50,
        @Query("sort") sort: String = "date,asc"
    ): TicketmasterResponse
}

/**
 * Fetches real events from Ticketmaster and converts them into Event —
 * adapted from Bear's EventRepository.
 *
 * The API key here is the team's shared dev key, kept inline for coursework
 * convenience. Beyond a student project this belongs in a non-committed
 * local.properties/BuildConfig value instead — same caution as the Mapbox token.
 */
object EventRepository {

    private const val API_KEY = "7KBlkqnAXqNU43vWELIqmqmlRTsUUYh6"

    private val api: TicketmasterApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://app.ticketmaster.com/discovery/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TicketmasterApi::class.java)
    }

    suspend fun getEvents(city: String? = "Pretoria", keyword: String? = null): Result<List<Event>> {
        return try {
            val response = api.getEvents(apiKey = API_KEY, city = city, keyword = keyword)
            Result.success(response.embedded?.events.orEmpty().map { toEvent(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun toEvent(raw: TicketmasterEvent): Event {
        val venue = raw.embedded?.venues?.firstOrNull()
        val image = raw.images
            ?.filter { it.fallback != true }
            ?.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }
            ?.url
        return Event(
            id = raw.id ?: "",
            name = raw.name ?: "Unnamed Event",
            date = raw.dates?.start?.localDate ?: "",
            time = raw.dates?.start?.localTime ?: "",
            venue = venue?.name ?: "Venue unavailable",
            address = venue?.address?.line1 ?: "",
            city = venue?.city?.name ?: "",
            latitude = venue?.location?.latitude?.toDoubleOrNull(),
            longitude = venue?.location?.longitude?.toDoubleOrNull(),
            imageUrl = image,
            description = raw.description ?: raw.info ?: raw.pleaseNote,
            ticketUrl = raw.url,
            status = raw.dates?.status?.code
        )
    }
}

/** Keeps whichever real Ticketmaster events were last fetched, keyed by id, so
 *  EventDetailFragment can look one up without re-hitting the API. Same
 *  pattern as PlaceCache — resets on app restart, which is fine here. */
object EventCache {
    private val cache = mutableMapOf<String, Event>()
    fun put(event: Event) { cache[event.id] = event }
    fun putAll(events: List<Event>) = events.forEach { put(it) }
    fun get(id: String): Event? = cache[id]
}