package com.example.api

data class Song(
    val id: String,              // Unique identifier (typically file path)
    val title: String,           // Song name
    val artist: String,          // Artist name
    val path: String,            // File path on device
    val duration: Long           // Duration in ms
)

data class Playlist(
    val id: String,              // Unique playlist ID
    val name: String,            // Playlist name
    val songs: MutableList<Song> // List of songs
)

enum class ApiPlaybackState {
    IDLE,      // Not playing
    PLAYING,   // Currently playing
    PAUSED,    // Paused (can resume)
    STOPPED    // Stopped (can play again)
}

enum class RepeatMode {
    OFF,       // No repeat
    ALL,       // Repeat entire playlist
    ONE        // Repeat single song
}
