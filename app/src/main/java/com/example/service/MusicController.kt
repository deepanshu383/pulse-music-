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

    var onPlaybackError: ((String, androidx.media3.common.PlaybackException) -> Unit)? = null

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

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                updateState()
                val currentId = controller?.currentMediaItem?.mediaId
                if (currentId != null) {
                    onPlaybackError?.invoke(currentId, error)
                }
            }
        })
    }

    fun playSong(song: SongEntity, allSongs: List<SongEntity> = emptyList(), autoAddAll: Boolean = false) {
        controller?.let { player ->
            // If the song is ALREADY loaded in ExoPlayer, toggle play/pause immediately without rebuffering
            if (player.currentMediaItem?.mediaId == song.filePath) {
                if (player.isPlaying) {
                    player.pause()
                    _playbackState.update { it.copy(isPlaying = false) }
                } else {
                    if (player.playbackState == Player.STATE_IDLE) {
                        player.prepare()
                    }
                    player.play()
                    _playbackState.update { it.copy(isPlaying = true) }
                }
                return
            }

            // Immediately set active song and playing state so UI responds in 0ms (waveform + highlight)
            _playbackState.update {
                it.copy(
                    currentSong = song,
                    currentSongId = song.filePath,
                    isPlaying = true
                )
            }

            val currentQueue = mutableListOf<MediaItem>()
            val currentIndex = player.currentMediaItemIndex
            
            if (allSongs.isNotEmpty()) {
                val songIdx = allSongs.indexOfFirst { it.filePath == song.filePath }
                if (songIdx != -1) {
                    // Queue subsequent songs in the playlist
                    for (i in songIdx + 1 until allSongs.size) {
                        val s = allSongs[i]
                        if (s.isDownloaded || !s.downloadUrl.isNullOrBlank() || s.filePath.startsWith("http")) {
                            currentQueue.add(s.toMediaItem())
                        }
                    }
                    // Loop around and add prior songs so the entire playlist remains in queue
                    for (i in 0 until songIdx) {
                        val s = allSongs[i]
                        if (s.isDownloaded || !s.downloadUrl.isNullOrBlank() || s.filePath.startsWith("http")) {
                            currentQueue.add(s.toMediaItem())
                        }
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
    
    fun setInitialQueue(songId: String?, queueIds: List<String>, allSongs: List<SongEntity>, playbackPosition: Long = 0L) {
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
                    player.setMediaItems(mediaItems, 0, playbackPosition)
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
    
    fun insertQueueItem(index: Int, mediaItem: MediaItem) {
        controller?.let { player ->
            // Create a clean copy of the MediaItem to avoid any stale internal state
            val cleanItem = mediaItem.buildUpon()
                .setMediaId(mediaItem.mediaId)
                .build()
            
            val targetIndex = index.coerceIn(0, player.mediaItemCount)
            player.addMediaItem(targetIndex, cleanItem)
            
            // Explicitly trigger a state update
            updateState()
        }
    }
    
    fun removeQueueItem(index: Int) {
        controller?.removeMediaItem(index)
    }
    
    fun clearQueue() {
        controller?.let { player ->
            val currentIndex = player.currentMediaItemIndex
            if (currentIndex != -1) {
                // Clear items after current
                if (currentIndex < player.mediaItemCount - 1) {
                    player.removeMediaItems(currentIndex + 1, player.mediaItemCount)
                }
                // Clear items before current
                if (currentIndex > 0) {
                    player.removeMediaItems(0, currentIndex)
                }
            } else {
                player.clearMediaItems()
            }
        }
    }
    
    fun skipToQueueItem(index: Int) {
        controller?.seekToDefaultPosition(index)
    }
    
    fun play() {
        controller?.let { player ->
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
            _playbackState.update { it.copy(isPlaying = true) }
        }
    }
    fun pause() {
        controller?.let { player ->
            player.pause()
            _playbackState.update { it.copy(isPlaying = false) }
        }
    }
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
            
            val currentMediaId = c.currentMediaItem?.mediaId
            _playbackState.update { old ->
                val resolvedSong = if (old.currentSong?.filePath == currentMediaId) {
                    old.currentSong
                } else {
                    c.currentMediaItem?.let { mi ->
                        SongEntity(
                            filePath = mi.mediaId,
                            title = mi.mediaMetadata.title?.toString() ?: (old.currentSong?.title ?: "Unknown"),
                            artist = mi.mediaMetadata.artist?.toString() ?: (old.currentSong?.artist ?: "Unknown Artist"),
                            album = mi.mediaMetadata.albumTitle?.toString() ?: (old.currentSong?.album ?: "Unknown Album"),
                            duration = c.duration.coerceAtLeast(0L),
                            albumArtPath = mi.mediaMetadata.artworkUri?.toString() ?: old.currentSong?.albumArtPath,
                            isOnline = old.currentSong?.isOnline ?: true,
                            downloadUrl = old.currentSong?.downloadUrl,
                            isDownloaded = old.currentSong?.isDownloaded ?: false,
                            localDownloadPath = old.currentSong?.localDownloadPath
                        )
                    } ?: old.currentSong
                }

                old.copy(
                    currentSong = resolvedSong,
                    currentSongId = currentMediaId ?: old.currentSongId,
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

    // Exhaustive, rock-solid URI resolution for all song sources:
    // Local storage paths, Scoped Storage SAF, Downloaded songs, and Online streams.
    val resolvedUri: Uri? = when {
        // 1. Downloaded file on disk
        isDownloaded && !localDownloadPath.isNullOrBlank() -> {
            val file = java.io.File(localDownloadPath)
            if (file.exists()) Uri.fromFile(file) else Uri.parse(localDownloadPath)
        }
        // 2. Local file on device storage (/storage/..., /sdcard/..., /data/...)
        filePath.startsWith("/") -> {
            val file = java.io.File(filePath)
            if (file.exists()) Uri.fromFile(file) else Uri.parse("file://$filePath")
        }
        // 3. Explicit file scheme (file:///...)
        filePath.startsWith("file://") -> {
            Uri.parse(filePath)
        }
        // 4. Android MediaStore or Scoped Storage content URI (content://...)
        filePath.startsWith("content://") -> {
            Uri.parse(filePath)
        }
        // 5. Valid online download URL
        !downloadUrl.isNullOrBlank() && (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) -> {
            Uri.parse(downloadUrl)
        }
        // 6. Direct HTTP/HTTPS audio URL
        filePath.startsWith("http://") || filePath.startsWith("https://") -> {
            Uri.parse(filePath)
        }
        // 7. General fallback if downloadUrl is present
        !downloadUrl.isNullOrBlank() -> {
            Uri.parse(downloadUrl)
        }
        else -> null
    }

    val builder = MediaItem.Builder()
        .setMediaId(filePath) // Use filePath/ID as unique identifier
        .setMediaMetadata(metadata)

    if (resolvedUri != null) {
        builder.setUri(resolvedUri)
    }

    return builder.build()
}

data class PlaybackState(
    val currentSong: SongEntity? = null,
    val currentSongId: String? = null,
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleModeEnabled: Boolean = false,
    val queue: List<MediaItem> = emptyList(),
    val currentQueueIndex: Int = -1
)
