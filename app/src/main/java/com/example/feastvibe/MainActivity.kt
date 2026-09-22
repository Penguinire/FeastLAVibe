package com.example.feastvibe

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mapbox.common.MapboxOptions

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Give the Mapbox Maps + Search SDKs the project's access token
        MapboxOptions.accessToken =
            getString(R.string.mapbox_access_token)

        setContentView(R.layout.activity_main)

        PlaceSearch.initialize(this)

        val host = supportFragmentManager
            .findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = host.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        // Tabs keep the bar visible; pushed screens (search, detail, filters) hide it.
        val tabs = setOf(
            R.id.exploreFragment,
            R.id.mapFragment,
            R.id.favouritesFragment,
            R.id.eventsFragment,
            R.id.profileFragment
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.isVisible = destination.id in tabs
        }

        // Single place that handles status/navigation bar insets for the whole app.
        // This is why the fragment layouts no longer need a 24dp status_bar spacer.
        val root = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }
}