package com.example.ui

// This file now only contains utility functions shared across the player UI.
// The NowPlayingScreen has been replaced by the HomeScreen as the primary player.
// The Queue Bottom Sheet has been extracted into HomeScreen.kt as QueueBottomSheet.

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
