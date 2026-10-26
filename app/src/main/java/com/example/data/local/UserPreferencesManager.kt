package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class UserPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pulse_user_prefs", Context.MODE_PRIVATE)

    fun hasCompletedColdStart(): Boolean {
        return prefs.getBoolean(KEY_COLD_START_COMPLETED, false)
    }

    fun setColdStartCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_COLD_START_COMPLETED, completed).apply()
    }

    fun getPreferredArtists(): Set<String> {
        return prefs.getStringSet(KEY_PREFERRED_ARTISTS, emptySet()) ?: emptySet()
    }

    fun setPreferredArtists(artists: Set<String>) {
        prefs.edit().putStringSet(KEY_PREFERRED_ARTISTS, artists).apply()
    }

    fun getPreferredLanguages(): Set<String> {
        return prefs.getStringSet(KEY_PREFERRED_LANGUAGES, emptySet()) ?: emptySet()
    }

    fun setPreferredLanguages(languages: Set<String>) {
        prefs.edit().putStringSet(KEY_PREFERRED_LANGUAGES, languages).apply()
    }

    companion object {
        private const val KEY_COLD_START_COMPLETED = "cold_start_completed"
        private const val KEY_PREFERRED_ARTISTS = "preferred_artists"
        private const val KEY_PREFERRED_LANGUAGES = "preferred_languages"
    }
}
