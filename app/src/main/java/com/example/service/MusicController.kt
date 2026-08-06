package com.example.service

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import com.example.data.local.SongEntity
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MusicController(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var controller: MediaController? = null
        private set

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    fun initialize(onReady: () -> Unit = {}) {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync().apply {
            addListener({
                controller = this.get()
                setupController()
                onReady()
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private fun setupController() {
        controller?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateState()
            }
            
            override fun onRepeatModeChanged(repeatMode: Int) {
                updateState()
            }
            
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                updateState()
            }
            
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                updateState()
            }
        })
    }

    fun playSong(song: SongEntity, allSongs: List<SongEntity> = emptyList(), autoAddAll: Boolean = false) {
        controller?.let { player ->
            val currentQueue = mutableListOf<MediaItem>()
            val currentIndex = player.currentMediaItemIndex
            
            if (autoAddAll && allSongs.isNotEmpty()) {
                val songIdx = allSongs.indexOfFirst { it.filePath == song.filePath }
                if (songIdx != -1) {
                    for (i in songIdx + 1 until allSongs.size) {
                        currentQueue.add(allSongs[i].toMediaItem())
                    }
                }
            } else {
                if (currentIndex != -1 && currentIndex < player.mediaItemCount) {
                    for (i in currentIndex + 1 until player.mediaItemCount) {
                        currentQueue.add(player.getMediaItemAt(i))
                    }
                }
            }
            
            val mediaItems = mutableListOf(song.toMediaItem())
            mediaItems.addAll(currentQueue)
            
            player.setMediaItems(mediaItems, 0, C.TIME_UNSET)
            player.prepare()
            player.play()
        }
    }
    
    fun setInitialQueue(songId: String?, queueIds: List<String>, allSongs: List<SongEntity>) {
        controller?.let { player ->
            if (player.mediaItemCount == 0) {
                val mediaItems = mutableListOf<MediaItem>()
                songId?.let { id ->
                    allSongs.find { it.filePath == id }?.let { mediaItems.add(it.toMediaItem()) }
                }
                queueIds.forEach { qid ->
                    allSongs.find { it.filePath == qid }?.let { mediaItems.add(it.toMediaItem()) }
                }
                if (mediaItems.isNotEmpty()) {
                    player.setMediaItems(mediaItems, 0, C.TIME_UNSET)
                    player.prepare()
                }
            }
        }
    }
    
    fun repeatQueueItem(index: Int) {
        controller?.let { player ->
            if (index >= 0 && index < player.mediaItemCount) {
                val item = player.getMediaItemAt(index)
                player.addMediaItem(index + 1, item)
            }
        }
    }

    fun addToQueue(song: SongEntity) {
        controller?.addMediaItem(song.toMediaItem())
    }
    
    fun moveQueueItem(from: Int, to: Int) {
        controller?.moveMediaItem(from, to)
    }
    
    fun removeQueueItem(index: Int) {
        controller?.removeMediaItem(index)
    }
    
    fun clearQueue() {
        controller?.let { player ->
            val currentIndex = player.currentMediaItemIndex
            if (currentIndex != -1 && currentIndex < player.mediaItemCount - 1) {
                player.removeMediaItems(currentIndex + 1, player.mediaItemCount)
            } else if (currentIndex == -1) {
                player.clearMediaItems()
            }
        }
    }
    
    fun skipToQueueItem(index: Int) {
        controller?.seekToDefaultPosition(index)
    }
    
    fun play() = controller?.play()
    fun pause() = controller?.pause()
    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)
    fun setShuffleMode(enabled: Boolean) { controller?.shuffleModeEnabled = enabled }
    fun setRepeatMode(mode: Int) { controller?.repeatMode = mode }
    
    private fun updateState() {
        controller?.let { c ->
            val queueList = mutableListOf<MediaItem>()
            for (i in 0 until c.mediaItemCount) {
                queueList.add(c.getMediaItemAt(i))
            }
            
            _playbackState.update { 
                it.copy(
                    currentSongId = c.currentMediaItem?.mediaId,
                    isPlaying = c.isPlaying,
                    duration = c.duration.coerceAtLeast(0L),
                    currentPosition = c.currentPosition.coerceAtLeast(0L),
                    repeatMode = c.repeatMode,
                    shuffleModeEnabled = c.shuffleModeEnabled,
                    queue = queueList,
                    currentQueueIndex = c.currentMediaItemIndex
                ) 
            }
            if (c.isPlaying) {
                startProgressUpdate()
            } else {
                stopProgressUpdate()
            }
        }
    }

    private fun startProgressUpdate() {
        if (progressJob?.isActive == true) return
        progressJob = coroutineScope.launch {
            while (isActive) {
                controller?.let { c ->
                    _playbackState.update {
                        it.copy(currentPosition = c.currentPosition.coerceAtLeast(0L))
                    }
                }
                delay(100L)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressUpdate()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }
}

fun SongEntity.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(albumArtPath?.let { Uri.parse(it) })
        .build()

    return MediaItem.Builder()
        .setMediaId(filePath)
        .setUri(filePath)
        .setMediaMetadata(metadata)
        .build()
}

data class PlaybackState(
    val currentSongId: String? = null,
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleModeEnabled: Boolean = false,
    val queue: List<MediaItem> = emptyList(),
    val currentQueueIndex: Int = -1
)
