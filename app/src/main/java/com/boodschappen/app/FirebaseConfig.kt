package com.boodschappen.app

/**
 * Vul deze 3 waarden in na het aanmaken van een Firebase project.
 * Zie README.md voor stap-voor-stap instructies (5 minuten).
 */
object FirebaseConfig {
    const val PROJECT_ID = "boodschappenlijst-app-claude"
    const val APP_ID     = "1:775098056780:android:8dc1b60af2b6bc496434e9"
    const val API_KEY    = "AIzaSyCpA73G3J0u4qWVKGOHsENYnXBzLGpuof4"

    fun isConfigured() = PROJECT_ID.isNotBlank() && APP_ID.isNotBlank() && API_KEY.isNotBlank()
}
