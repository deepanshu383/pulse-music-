package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {
    
    private val FOLDER_URI = stringPreferencesKey("folder_uri")

    val folderUriFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[FOLDER_URI]
    }

    suspend fun saveFolderUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[FOLDER_URI] = uri
        }
    }
}
