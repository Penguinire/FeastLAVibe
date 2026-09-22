package com.example.feastvibe

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun usersCollection() = db.collection("users")

    suspend fun createProfile(profile: UserProfile): Result<Unit> {
        return try {
            usersCollection().document(profile.uid).set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(uid: String): Result<UserProfile> {
        return try {
            val snapshot = usersCollection().document(uid).get().await()
            val profile = snapshot.toObject(UserProfile::class.java)
                ?: return Result.failure(Exception("Profile not found."))
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}