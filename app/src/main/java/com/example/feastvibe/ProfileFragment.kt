package com.example.feastvibe

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment(R.layout.activity_profile) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setMenu(view, R.id.menu_reviews, "My Reviews")
        setMenu(view, R.id.menu_settings, "Settings")
        setMenu(view, R.id.menu_help, "Help & Support")

        val user = authRepository.currentUser
        if (user != null) {
            // Initial UI from Firebase Auth
            view.findViewById<TextView>(R.id.tv_profile_name).text =
                user.displayName ?: (user.email?.substringBefore("@") ?: "Feast & Vibe user")
            view.findViewById<TextView>(R.id.tv_profile_meta).text = user.email ?: ""

            // Fetch detailed profile from Firestore
            lifecycleScope.launch {
                val result = userRepository.getProfile(user.uid)
                if (result.isSuccess) {
                    result.getOrNull()?.let { updateUI(view, it) }
                }
            }
        } else {
            // Redirect to Login if not signed in
            findNavController().navigate(R.id.loginFragment)
        }

        view.findViewById<TextView>(R.id.tv_log_out).setOnClickListener {
            authRepository.logout()
            findNavController().navigate(R.id.loginFragment)
        }
    }

    private fun updateUI(view: View, profile: UserProfile) {
        view.findViewById<TextView>(R.id.tv_profile_name).text = profile.fullName
        
        val date = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(profile.memberSince))
        val meta = if (profile.location.isNotEmpty()) {
            "${profile.location} · Member since $date"
        } else {
            "Member since $date"
        }
        view.findViewById<TextView>(R.id.tv_profile_meta).text = meta

        view.findViewById<TextView>(R.id.tv_reviews_count).text = profile.reviewCount.toString()
        view.findViewById<TextView>(R.id.tv_favourites_count).text = profile.favouritesCount.toString()
        view.findViewById<TextView>(R.id.tv_visited_count).text = profile.visitedCount.toString()
    }

    private fun setMenu(root: View, id: Int, label: String) {
        root.findViewById<View>(id).findViewById<TextView>(R.id.tv_menu_label).text = label
    }
}