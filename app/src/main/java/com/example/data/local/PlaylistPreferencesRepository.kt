package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class PlaylistPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("playlist_prefs", Context.MODE_PRIVATE)

    fun getSortOrder(playlistId: Long): String {
        return prefs.getString("playlist_${playlistId}_sort", "DATE_ADDED_NEWEST") ?: "DATE_ADDED_NEWEST"
    }

    fun getViewMode(playlistId: Long): String {
        return prefs.getString("playlist_${playlistId}_view", "LIST") ?: "LIST"
    }

    fun saveSortOrder(playlistId: Long, sortOrder: String) {
        prefs.edit().putString("playlist_${playlistId}_sort", sortOrder).apply()
    }

    fun saveViewMode(playlistId: Long, viewMode: String) {
        prefs.edit().putString("playlist_${playlistId}_view", viewMode).apply()
    }

    fun getLibrarySortOrder(): String {
        return prefs.getString("library_sort_order", "DATE_ADDED_NEWEST") ?: "DATE_ADDED_NEWEST"
    }

    fun saveLibrarySortOrder(sortOrder: String) {
        prefs.edit().putString("library_sort_order", sortOrder).apply()
    }

    fun getLibraryViewMode(): String {
        return prefs.getString("library_view_mode", "LIST") ?: "LIST"
    }

    fun saveLibraryViewMode(viewMode: String) {
        prefs.edit().putString("library_view_mode", viewMode).apply()
    }

    fun getLastPlayedSongId(): String? {
        return prefs.getString("last_played_song_id", null)
    }

    fun saveLastPlayedSongId(songId: String) {
        prefs.edit().putString("last_played_song_id", songId).apply()
    }

    fun getSavedQueue(): List<String> {
        val queueStr = prefs.getString("saved_queue", "") ?: ""
        if (queueStr.isEmpty()) return emptyList()
        return queueStr.split("|||")
    }

    fun saveQueue(queueIds: List<String>) {
        if (queueIds.isEmpty()) {
            prefs.edit().remove("saved_queue").apply()
        } else {
            prefs.edit().putString("saved_queue", queueIds.joinToString("|||")).apply()
        }
    }

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
    }

    fun isAutoAddEnabled(): Boolean {
        return prefs.getBoolean("auto_add_enabled", false)
    }

    fun setAutoAddEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_add_enabled", enabled).apply()
    }

    fun isSaveQueueEnabled(): Boolean {
        return prefs.getBoolean("save_queue_enabled", true)
    }

    fun setSaveQueueEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("save_queue_enabled", enabled).apply()
    }

    fun isAutoPlayNextEnabled(): Boolean {
        return prefs.getBoolean("auto_play_next_enabled", true)
    }

    fun setAutoPlayNextEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_play_next_enabled", enabled).apply()
    }
}
