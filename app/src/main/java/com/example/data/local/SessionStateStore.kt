package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_state")

class SessionStateStore(private val dataStore: DataStore<Preferences>) {
    companion object {
        val LOCKED_MOOD_GROUP = stringPreferencesKey("locked_mood_group")
        val LOCKED_LANGUAGE = stringPreferencesKey("locked_language")
    }

    suspend fun persistLock(moodGroup: String?, language: String?) {
        dataStore.edit { preferences ->
            if (moodGroup != null) {
                preferences[LOCKED_MOOD_GROUP] = moodGroup
            } else {
                preferences.remove(LOCKED_MOOD_GROUP)
            }
            if (language != null) {
                preferences[LOCKED_LANGUAGE] = language
            } else {
                preferences.remove(LOCKED_LANGUAGE)
            }
        }
    }

    suspend fun restoreLock(): Pair<String?, String?> {
        val prefs = dataStore.data.first()
        return prefs[LOCKED_MOOD_GROUP] to prefs[LOCKED_LANGUAGE]
    }
}
