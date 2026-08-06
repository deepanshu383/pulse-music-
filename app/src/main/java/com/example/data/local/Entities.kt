package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val coverArtPath: String? = null,
    val createdDate: Long = System.currentTimeMillis(),
    val modifiedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songPath"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songPath: String,
    val position: Int,
    val addedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "song_metadata")
data class SongEntity(
    @PrimaryKey val filePath: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val bitrate: Int = 0,
    val albumArtPath: String? = null,
    val lastModified: Long = System.currentTimeMillis()
)
