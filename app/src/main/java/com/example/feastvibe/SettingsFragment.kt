package com.example.feastvibe

import android.os.Bundle
import android.view.View
import android.widget.Switch
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment(R.layout.activity_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferences = requireContext().getSharedPreferences(
            "feast_vibe_settings",
            android.content.Context.MODE_PRIVATE
        )

        val notificationSwitch =
            view.findViewById<Switch>(R.id.switch_notifications)

        notificationSwitch.isChecked =
            preferences.getBoolean("notifications_enabled", true)

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit()
                .putBoolean("notifications_enabled", isChecked)
                .apply()
        }
    }
}