package com.example.feastvibe

import com.google.firebase.firestore.PropertyName

data class UserProfile(
    var uid: String = "",
    var fullName: String = "",
    var email: String = "",
    var location: String = "",
    @get:PropertyName("photoUrl") @set:PropertyName("photoUrl")
    var photoUrl: String = "",
    var memberSince: Long = System.currentTimeMillis(),
    var reviewCount: Int = 0,
    var favouritesCount: Int = 0,
    var visitedCount: Int = 0
)