package com.boodschappen.app

/**
 * Vul deze 3 waarden in na het aanmaken van een Firebase project.
 * Zie README.md voor stap-voor-stap instructies (5 minuten).
 */
object FirebaseConfig {
    const val PROJECT_ID = ""   // bijv. "boodschappen-app-abc12"
    const val APP_ID     = ""   // bijv. "1:123456789:android:abcdef1234"
    const val API_KEY    = ""   // bijv. "AIzaSyXXXXXXXXXXXXXXXXXXXX"

    fun isConfigured() = PROJECT_ID.isNotBlank() && APP_ID.isNotBlank() && API_KEY.isNotBlank()
}
