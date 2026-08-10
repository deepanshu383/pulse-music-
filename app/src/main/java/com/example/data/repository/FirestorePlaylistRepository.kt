package com.example.data.repository

import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import com.example.data.local.SongEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Manages playlist persistence in Firebase Firestore.
 *
 * Firestore structure:
 *   users/{uid}/playlists/{playlistId}/
 *     name: String
 *     createdDate: Long
 *     modifiedDate: Long
 *     songs: List<Map<String, Any>>  — full song metadata embedded
 */
class FirestorePlaylistRepository {

    private val db = FirebaseFirestore.getInstance()

    // Returns the Firestore collection for the current user's playlists.
    // Returns null when no user is logged in (safe — all methods check for this).
    private fun userPlaylistsRef(uid: String) =
        db.collection("users").document(uid).collection("playlists")

    // ---------- Push (write) ----------

    /**
     * Pushes a full playlist snapshot (metadata + songs) to Firestore.
     * Uses the Room local ID as the Firestore document ID for a 1:1 mapping.
     */
    suspend fun pushPlaylist(
        uid: String,
        playlist: PlaylistEntity,
        songs: List<SongEntity>,
        songEntities: List<PlaylistSongEntity>   // for position ordering
    ) {
        val songsData = songs.mapIndexed { idx, song ->
            val position = songEntities.indexOfFirst { it.songPath == song.filePath }
                .takeIf { it != -1 } ?: idx
            mapOf(
                "songPath"     to song.filePath,
                "title"        to song.title,
                "artist"       to song.artist,
                "album"        to song.album,
                "duration"     to song.duration,
                "albumArtPath" to (song.albumArtPath ?: ""),
                "isOnline"     to song.isOnline,
                "downloadUrl"  to (song.downloadUrl ?: ""),
                "position"     to position
            )
        }

        val docData = mapOf(
            "localId"      to playlist.id,
            "name"         to playlist.name,
            "description"  to playlist.description,
            "createdDate"  to playlist.createdDate,
            "modifiedDate" to System.currentTimeMillis(),
            "songs"        to songsData
        )

        userPlaylistsRef(uid)
            .document(playlist.id.toString())
            .set(docData, SetOptions.merge())
            .await()
    }

    /**
     * Pushes only the name/modifiedDate metadata of a playlist (no song resync needed).
     */
    suspend fun pushPlaylistMetadata(uid: String, playlist: PlaylistEntity) {
        val docData = mapOf(
            "localId"      to playlist.id,
            "name"         to playlist.name,
            "description"  to playlist.description,
            "modifiedDate" to System.currentTimeMillis()
        )
        userPlaylistsRef(uid)
            .document(playlist.id.toString())
            .set(docData, SetOptions.merge())
            .await()
    }

    /** Deletes a playlist document from Firestore. */
    suspend fun deletePlaylist(uid: String, playlistId: Long) {
        userPlaylistsRef(uid)
            .document(playlistId.toString())
            .delete()
            .await()
    }

    // ---------- Pull (read / sync) ----------

    /**
     * Fetches all playlists for the user from Firestore and converts them to
     * Room-compatible entity pairs for local insertion.
     */
    suspend fun fetchPlaylists(uid: String): List<FirestorePlaylistData> {
        return try {
            val snapshot = userPlaylistsRef(uid).get().await()
            snapshot.documents.mapNotNull { doc ->
                val localId = (doc.getLong("localId") ?: return@mapNotNull null)
                val name = doc.getString("name") ?: return@mapNotNull null
                val description = doc.getString("description") ?: ""
                val createdDate = doc.getLong("createdDate") ?: System.currentTimeMillis()
                val modifiedDate = doc.getLong("modifiedDate") ?: System.currentTimeMillis()

                @Suppress("UNCHECKED_CAST")
                val rawSongs = doc.get("songs") as? List<Map<String, Any>> ?: emptyList()
                val songs = rawSongs.mapNotNull { s ->
                    val path = s["songPath"] as? String ?: return@mapNotNull null
                    SongEntity(
                        filePath     = path,
                        title        = s["title"] as? String ?: "Unknown",
                        artist       = s["artist"] as? String ?: "Unknown Artist",
                        album        = s["album"] as? String ?: "Unknown Album",
                        duration     = (s["duration"] as? Number)?.toLong() ?: 0L,
                        albumArtPath = (s["albumArtPath"] as? String)?.takeIf { it.isNotBlank() },
                        isOnline     = s["isOnline"] as? Boolean ?: false,
                        downloadUrl  = (s["downloadUrl"] as? String)?.takeIf { it.isNotBlank() }
                    )
                }
                val songPositions = rawSongs.mapNotNull { s ->
                    val path = s["songPath"] as? String ?: return@mapNotNull null
                    val pos  = (s["position"] as? Number)?.toInt() ?: 0
                    path to pos
                }

                FirestorePlaylistData(
                    playlist = PlaylistEntity(
                        id           = localId,
                        name         = name,
                        description  = description,
                        createdDate  = createdDate,
                        modifiedDate = modifiedDate
                    ),
                    songs         = songs,
                    songPositions = songPositions
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

/** DTO returned by [FirestorePlaylistRepository.fetchPlaylists]. */
data class FirestorePlaylistData(
    val playlist: PlaylistEntity,
    val songs: List<SongEntity>,
    /** (songPath → position) used to reconstruct [PlaylistSongEntity]. */
    val songPositions: List<Pair<String, Int>>
)
