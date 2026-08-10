package com.example.data.repository

import com.example.data.local.PlaylistDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import com.example.data.local.SongDao
import com.example.data.local.SongEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class PlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao
) {
    private val firestoreRepo = FirestorePlaylistRepository()

    // Convenience: current Firebase UID (null = not logged in, skip Firestore)
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    // ───────────────────────── Read ─────────────────────────

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    fun getPlaylistById(id: Long): Flow<PlaylistEntity?> = playlistDao.getPlaylistById(id)

    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>> =
        playlistDao.getSongsForPlaylist(playlistId)

    // ───────────────────────── Write ─────────────────────────

    suspend fun createPlaylist(name: String, description: String = ""): Long =
        withContext(Dispatchers.IO) {
            val playlist = PlaylistEntity(name = name, description = description)
            val localId = playlistDao.insertPlaylist(playlist)

            // Sync the newly created (empty) playlist to Firestore
            uid?.let { u ->
                firestoreRepo.pushPlaylist(
                    uid = u,
                    playlist = playlist.copy(id = localId),
                    songs = emptyList(),
                    songEntities = emptyList()
                )
            }
            localId
        }

    suspend fun updatePlaylistName(id: Long, newName: String) = withContext(Dispatchers.IO) {
        val existing = playlistDao.getPlaylistById(id).first()
        val updated = (existing ?: PlaylistEntity(id = id, name = newName))
            .copy(name = newName, modifiedDate = System.currentTimeMillis())
        playlistDao.updatePlaylist(updated)

        uid?.let { u -> firestoreRepo.pushPlaylistMetadata(u, updated) }
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deleteAllSongsForPlaylist(id)
        playlistDao.deletePlaylistById(id)

        uid?.let { u -> firestoreRepo.deletePlaylist(u, id) }
    }

    // ───────────────────────── Song mutations ─────────────────────────

    /**
     * Adds a song to a playlist.
     * If [songMetadata] is provided (for online songs), it is first inserted into
     * the local song_metadata table so the INNER JOIN query can find it.
     */
    suspend fun addSongToPlaylist(
        playlistId: Long,
        songPath: String,
        songMetadata: SongEntity? = null
    ) = withContext(Dispatchers.IO) {
        // Persist online song metadata so Room JOIN works
        songMetadata?.let { songDao.insertSong(it) }

        val currentSongs = playlistDao.getPlaylistSongEntities(playlistId)
        // Avoid duplicate entries
        if (currentSongs.any { it.songPath == songPath }) return@withContext

        val position = currentSongs.size
        playlistDao.insertPlaylistSong(PlaylistSongEntity(playlistId, songPath, position))
        syncPlaylistToFirestore(playlistId)
    }

    suspend fun addSongsToPlaylist(
        playlistId: Long,
        songPaths: List<String>,
        songMetadataMap: Map<String, SongEntity> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        // Persist any online song metadata
        songMetadataMap.values.forEach { songDao.insertSong(it) }

        val currentSongs = playlistDao.getPlaylistSongEntities(playlistId)
        val existingPaths = currentSongs.map { it.songPath }.toSet()
        var position = currentSongs.size

        val entities = songPaths
            .filter { it !in existingPaths }
            .map { path -> PlaylistSongEntity(playlistId, path, position++) }

        playlistDao.insertPlaylistSongs(entities)
        syncPlaylistToFirestore(playlistId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songPath: String) =
        withContext(Dispatchers.IO) {
            playlistDao.deletePlaylistSong(playlistId, songPath)

            // Re-index positions
            val remainingSongs = playlistDao.getPlaylistSongEntities(playlistId)
            val reindexed = remainingSongs.mapIndexed { idx, e -> e.copy(position = idx) }
            playlistDao.insertPlaylistSongs(reindexed)

            syncPlaylistToFirestore(playlistId)
        }

    suspend fun reorderSongs(playlistId: Long, fromIndex: Int, toIndex: Int) =
        withContext(Dispatchers.IO) {
            val current = playlistDao.getPlaylistSongEntities(playlistId).toMutableList()
            if (fromIndex in current.indices && toIndex in current.indices) {
                val item = current.removeAt(fromIndex)
                current.add(toIndex, item)
                val updated = current.mapIndexed { idx, e -> e.copy(position = idx) }
                playlistDao.insertPlaylistSongs(updated)
                syncPlaylistToFirestore(playlistId)
            }
        }

    // ───────────────────────── Firebase Sync ─────────────────────────

    /**
     * Pushes the current state of [playlistId] to Firestore.
     * Must be called from IO dispatcher (already ensured by callers above).
     */
    private suspend fun syncPlaylistToFirestore(playlistId: Long) {
        val u = uid ?: return
        val playlist = playlistDao.getPlaylistById(playlistId).first() ?: return
        val songs = playlistDao.getSongsForPlaylist(playlistId).first()
        val songEntities = playlistDao.getPlaylistSongEntities(playlistId)
        firestoreRepo.pushPlaylist(u, playlist, songs, songEntities)
    }

    /**
     * Pulls all playlists from Firestore and merges them into the local Room DB.
     * Call this once when the user logs in or the app starts.
     */
    suspend fun syncFromFirestore() = withContext(Dispatchers.IO) {
        val u = uid ?: return@withContext
        val firestorePlaylists = firestoreRepo.fetchPlaylists(u)

        firestorePlaylists.forEach { data ->
            // Upsert playlist metadata
            playlistDao.insertPlaylist(data.playlist)

            // Upsert all song metadata (online songs need to be in song_metadata for JOIN)
            data.songs.forEach { song -> songDao.insertSong(song) }

            // Upsert playlist_songs join table, respecting position order
            val songEntities = data.songPositions.map { (path, pos) ->
                PlaylistSongEntity(
                    playlistId = data.playlist.id,
                    songPath   = path,
                    position   = pos
                )
            }
            playlistDao.insertPlaylistSongs(songEntities)
        }
    }
}
