plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.feastvibe"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.feastvibe"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")

    // Mapbox — Maps SDK (displays the map itself)
    // "-ndk27" variant required for 16 KB page-size compatibility (Google Play requirement
    // for apps targeting Android 15+ as of Nov 1, 2025)
    implementation("com.mapbox.maps:android-ndk27:11.31.0")

    // Mapbox — Search SDK (text search + category/nearby search only)
    implementation("com.mapbox.search:mapbox-search-android-ndk27:2.31.0")
    implementation("com.mapbox.search:place-autocomplete-ndk27:2.31.0")
    implementation("com.mapbox.search:discover-ndk27:2.31.0")
    implementation("com.google.android.gms:play-services-location:21.4.0")

    // Events — Ticketmaster (Bear)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Firebase (Fumani) — BoM keeps every Firebase library version in sync
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.google.firebase:firebase-analytics")

    testImplementation("junit:junit:4.13.2")
}
