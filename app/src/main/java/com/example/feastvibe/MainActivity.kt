
package com.example.feastvibe

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.maplibre.android.MapLibre
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FavouritesStore.initialize(this)
        Log.d("FeastStartup", "1. MainActivity onCreate started")


        MapLibre.getInstance(this)

        setContentView(R.layout.activity_main)
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result

                    getSharedPreferences("feast_vibe_settings", MODE_PRIVATE)
                        .edit()
                        .putString("fcm_token", token)
                        .apply()

                    Log.d("FeastVibeFCM", "FCM TOKEN: $token")
                } else {
                    Log.e("FeastVibeFCM", "Failed to get FCM token", task.exception)
                }
            }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        Log.d("FeastStartup", "3. activity_main layout loaded")

        PlaceSearch.initialize(this)
        Log.d("FeastStartup", "4. PlaceSearch initialized")

        val host = supportFragmentManager
            .findFragmentById(R.id.nav_host) as NavHostFragment
        Log.d("FeastStartup", "5. Navigation host found")

        val navController = host.navController
        Log.d(
            "FeastStartup",
            "6. Current destination ID: ${navController.currentDestination?.id}"
        )

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        val tabs = setOf(
            R.id.exploreFragment,
            R.id.mapFragment,
            R.id.favouritesFragment,
            R.id.eventsFragment,
            R.id.profileFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.isVisible = destination.id in tabs
            Log.d("FeastStartup", "Destination changed: ${destination.id}")

            val root = findViewById<View>(R.id.main)
            ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }

            Log.d("FeastStartup", "7. MainActivity setup completed")
        }
    }
}
