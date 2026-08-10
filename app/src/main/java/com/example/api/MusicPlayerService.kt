package com.example.api

import android.content.Context
import androidx.media3.common.Player
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.SongEntity
import com.example.data.repository.MusicRepository
import com.example.data.repository.PlaylistRepository
import com.example.service.MusicController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MusicPlayerService(private val context: Context) {

    private val musicController = MusicController(context)
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "pulse_music_db"
    ).addMigrations(AppDatabase.MIGRATION_3_4)
    .fallbackToDestructiveMigration().build()
    
    private val musicRepo = MusicRepository(context.applicationContext, db.songDao())
    private val playlistRepo = PlaylistRepository(db.playlistDao(), db.songDao())

    private val listeners = mutableListOf<MusicPlayerListener>()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var stateObserverJob: Job? = null

    init {
        musicController.initialize {
            startObservingState()
        }
    }

    fun addListener(listener: MusicPlayerListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: MusicPlayerListener) {
        listeners.remove(listener)
    }

    private fun startObservingState() {
        stateObserverJob?.cancel()
        stateObserverJob = scope.launch {
            musicController.playbackState.collect { state ->
                val apiState = when {
                    state.isPlaying -> ApiPlaybackState.PLAYING
                    state.currentPosition > 0 && !state.isPlaying -> ApiPlaybackState.PAUSED
                    else -> ApiPlaybackState.IDLE
                }
                
                listeners.forEach {
                    it.onPlaybackStateChanged(apiState)
                }

                val apiRepeatMode = when (state.repeatMode) {
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    else -> RepeatMode.OFF
                }
                
                listeners.forEach {
                    it.onRepeatModeChanged(apiRepeatMode)
                    it.onShuffleChanged(state.shuffleModeEnabled)
                }
            }
        }
    }

    fun searchSongs(query: String): List<Song> {
        var results = emptyList<SongEntity>()
        runBlocking {
            val allSongs = musicRepo.cachedSongs.first()
            results = allSongs.filter {
                it.title.contains(query, ignoreCase = true) || 
                it.artist.contains(query, ignoreCase = true)
            }
        }
        return results.map { it.toApiSong() }
    }

    fun playSong(song: Song) {
        val songEntity = song.toEntity()
        musicController.playSong(songEntity, listOf(songEntity))
    }

    fun createPlaylist(name: String): Playlist {
        var playlistId = 0L
        runBlocking {
            playlistId = playlistRepo.createPlaylist(name = name)
        }
        return Playlist(id = playlistId.toString(), name = name, songs = mutableListOf())
    }

    fun addSongToPlaylist(playlist: Playlist, song: Song) {
        playlist.songs.add(song)
        scope.launch(Dispatchers.IO) {
            val dbId = playlist.id.toLongOrNull() ?: return@launch
            playlistRepo.addSongToPlaylist(dbId, song.path)
            
            // Notify on main thread
            launch(Dispatchers.Main) {
                listeners.forEach { it.onPlaylistUpdated(playlist) }
            }
        }
    }

    fun addSongsToPlaylist(playlist: Playlist, songs: List<Song>) {
        playlist.songs.addAll(songs)
        scope.launch(Dispatchers.IO) {
            val dbId = playlist.id.toLongOrNull() ?: return@launch
            val paths = songs.map { it.path }
            playlistRepo.addSongsToPlaylist(dbId, paths)
            
            // Notify on main thread
            launch(Dispatchers.Main) {
                listeners.forEach { it.onPlaylistUpdated(playlist) }
            }
        }
    }

    fun setPlaylist(playlist: Playlist) {
        musicController.clearQueue()
        if (playlist.songs.isNotEmpty()) {
            val entities = playlist.songs.map { it.toEntity() }
            musicController.playSong(entities.first(), entities, autoAddAll = true)
            musicController.pause() 
        }
    }

    fun playSongAtIndex(index: Int) {
        musicController.skipToQueueItem(index)
        musicController.play()
    }

    fun pause() = musicController.pause()
    fun resume() = musicController.play()
    fun stop() {
        musicController.pause()
        listeners.forEach { it.onPlaybackStateChanged(ApiPlaybackState.STOPPED) }
    }
    fun playNext() = musicController.next()
    fun playPrevious() = musicController.previous()
    fun seek(positionMs: Long) = musicController.seekTo(positionMs)
    
    fun getCurrentPosition(): Long {
        return musicController.playbackState.value.currentPosition
    }
    
    fun getDuration(): Long {
        return musicController.playbackState.value.duration
    }
    
    fun setVolume(volume: Float) {
        musicController.controller?.volume = volume
        listeners.forEach { it.onVolumeChanged(volume) }
    }
    
    fun increaseVolume(amount: Float) {
        val current = musicController.controller?.volume ?: 0f
        setVolume((current + amount).coerceAtMost(1f))
    }
    
    fun decreaseVolume(amount: Float) {
        val current = musicController.controller?.volume ?: 0f
        setVolume((current - amount).coerceAtLeast(0f))
    }
    
    fun setShuffle(enabled: Boolean) {
        musicController.setShuffleMode(enabled)
    }
    
    fun toggleShuffle() {
        val current = musicController.playbackState.value.shuffleModeEnabled
        setShuffle(!current)
    }
    
    val isShuffleEnabled: Boolean
        get() = musicController.playbackState.value.shuffleModeEnabled
        
    fun setRepeatMode(mode: RepeatMode) {
        val media3Mode = when (mode) {
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        }
        musicController.setRepeatMode(media3Mode)
    }
    
    fun cycleRepeatMode() {
        val current = musicController.playbackState.value.repeatMode
        val nextMode = when (current) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        musicController.setRepeatMode(nextMode)
    }

    fun release() {
        stateObserverJob?.cancel()
        musicController.release()
        listeners.clear()
    }

    // --- Mappers ---
    private fun SongEntity.toApiSong() = Song(
        id = this.filePath,
        title = this.title,
        artist = this.artist,
        path = this.filePath,
        duration = this.duration
    )

    private fun Song.toEntity() = SongEntity(
        filePath = this.path,
        title = this.title,
        artist = this.artist,
        album = "Unknown",
        duration = this.duration,
        bitrate = 0,
        albumArtPath = null,
        lastModified = System.currentTimeMillis()
    )
}
