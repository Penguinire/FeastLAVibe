
package com.example.feastvibe

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface GeoapifyApi {

    @GET("v2/places")
    suspend fun searchPlaces(
        @Query("categories") categories: String,
        @Query("filter") filter: String,
        @Query("limit") limit: Int,
        @Query("apiKey") apiKey: String,
        @Query("name") name: String?,
        @Query("bias") bias: String?
    ): GeoapifyResponse
}

data class GeoapifyResponse(
    @SerializedName("features")
    val features: List<GeoapifyFeature> = emptyList()
)

data class GeoapifyFeature(
    @SerializedName("properties")
    val properties: GeoapifyPlaceProperties = GeoapifyPlaceProperties()
)

data class GeoapifyPlaceProperties(
    @SerializedName("place_id")
    val placeId: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("categories")
    val categories: List<String> = emptyList(),

    @SerializedName("lat")
    val latitude: Double? = null,

    @SerializedName("lon")
    val longitude: Double? = null,

    @SerializedName("formatted")
    val formattedAddress: String? = null,

    @SerializedName("address_line1")
    val addressLine1: String? = null,

    @SerializedName("address_line2")
    val addressLine2: String? = null,

    @SerializedName("website")
    val website: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("opening_hours")
    val openingHours: String? = null
)

object GeoapifyClient {

    private const val BASE_URL = "https://api.geoapify.com/"

    val api: GeoapifyApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeoapifyApi::class.java)
    }
}
