package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: String,
    val timestamp: Long,
    val timeSegment: String, // "Morning", "Afternoon", "Evening", "Night"
    val title: String = "",
    val artist: String = "",
    val canonicalSongKey: String = "" // Normalized "title|artist" for cross-version dedup
)
