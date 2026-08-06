package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.AppDatabase
import com.example.data.local.SongEntity
import com.example.data.repository.MusicRepository
import com.example.service.MusicController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri

enum class ViewMode { LIST, GRID, ALBUM, ARTIST }
enum class SortOrder { TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "pulse_music_db"
    ).build()

    private val userPreferences = UserPreferencesRepository(application)
    val folderUri = userPreferences.folderUriFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val repository = MusicRepository(application, db.songDao())
    val playlistRepository = com.example.data.repository.PlaylistRepository(db.playlistDao())
    val playlistPrefs = com.example.data.local.PlaylistPreferencesRepository(application)

    val allPlaylists = playlistRepository.getAllPlaylists().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val musicController = MusicController(application)

    init {
        musicController.initialize {
            val lastPlayed = playlistPrefs.getLastPlayedSongId()
            val savedQueue = playlistPrefs.getSavedQueue()
            if (lastPlayed != null || savedQueue.isNotEmpty()) {
                viewModelScope.launch {
                    val songsList = repository.cachedSongs.first()
                    if (songsList.isNotEmpty()) {
                        musicController.setInitialQueue(lastPlayed, savedQueue, songsList)
                    }
                }
            }
        }
        
        viewModelScope.launch {
            musicController.playbackState.collect { state ->
                if (playlistPrefs.isSaveQueueEnabled()) {
                    val qIds = mutableListOf<String>()
                    val startIdx = if (state.currentQueueIndex != -1) state.currentQueueIndex + 1 else 0
                    for (i in startIdx until state.queue.size) {
                        qIds.add(state.queue[i].mediaId)
                    }
                    playlistPrefs.saveQueue(qIds)
                    state.currentSongId?.let { playlistPrefs.saveLastPlayedSongId(it) }
                }
            }
        }
    }

    private val _isAutoAddEnabled = MutableStateFlow(playlistPrefs.isAutoAddEnabled())
    val isAutoAddEnabled = _isAutoAddEnabled.asStateFlow()

    private val _isSaveQueueEnabled = MutableStateFlow(playlistPrefs.isSaveQueueEnabled())
    val isSaveQueueEnabled = _isSaveQueueEnabled.asStateFlow()

    private val _isAutoPlayNextEnabled = MutableStateFlow(playlistPrefs.isAutoPlayNextEnabled())
    val isAutoPlayNextEnabled = _isAutoPlayNextEnabled.asStateFlow()

    fun setAutoAddEnabled(enabled: Boolean) {
        playlistPrefs.setAutoAddEnabled(enabled)
        _isAutoAddEnabled.value = enabled
    }

    fun setSaveQueueEnabled(enabled: Boolean) {
        playlistPrefs.setSaveQueueEnabled(enabled)
        _isSaveQueueEnabled.value = enabled
    }

    fun setAutoPlayNextEnabled(enabled: Boolean) {
        playlistPrefs.setAutoPlayNextEnabled(enabled)
        _isAutoPlayNextEnabled.value = enabled
    }

    fun playSong(song: SongEntity, allSongs: List<SongEntity> = emptyList()) {
        val autoAdd = playlistPrefs.isAutoAddEnabled()
        musicController.playSong(song, allSongs, autoAdd)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE)
    val sortOrder = _sortOrder.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode = _viewMode.asStateFlow()

    val songs = combine(
        repository.cachedSongs,
        _searchQuery,
        _sortOrder
    ) { songsList, query, sort ->
        var filtered = songsList
        if (query.isNotBlank()) {
            filtered = songsList.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.artist.contains(query, ignoreCase = true) || 
                it.album.contains(query, ignoreCase = true) 
            }
        }
        when (sort) {
            SortOrder.TITLE -> filtered.sortedBy { it.title }
            SortOrder.ARTIST -> filtered.sortedBy { it.artist }
            SortOrder.ALBUM -> filtered.sortedBy { it.album }
            SortOrder.DATE_ADDED -> filtered.sortedByDescending { it.lastModified }
            SortOrder.DURATION -> filtered.sortedByDescending { it.duration }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val playbackState = musicController.playbackState

    fun scanMusic(folderUriString: String? = null) {
        viewModelScope.launch {
            if (folderUriString != null) {
                userPreferences.saveFolderUri(folderUriString)
                repository.scanMusicFiles(folderUriString)
            } else {
                repository.scanMusicFiles(null)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun updateViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun repeatQueueItem(index: Int) {
        musicController.repeatQueueItem(index)
    }

    fun addToQueue(song: SongEntity) {
        musicController.addToQueue(song)
    }

    fun moveQueueItem(from: Int, to: Int) {
        musicController.moveQueueItem(from, to)
    }

    fun removeQueueItem(index: Int) {
        musicController.removeQueueItem(index)
    }

    fun clearQueue() {
        musicController.clearQueue()
    }

    fun skipToQueueItem(index: Int) {
        musicController.skipToQueueItem(index)
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(id)
        }
    }

    fun updatePlaylistName(id: Long, name: String) {
        viewModelScope.launch {
            playlistRepository.updatePlaylistName(id, name)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songPaths: List<String>) {
        viewModelScope.launch {
            playlistRepository.addSongsToPlaylist(playlistId, songPaths)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songPath: String) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, songPath)
        }
    }

    fun reorderPlaylistSongs(playlistId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            playlistRepository.reorderSongs(playlistId, fromIndex, toIndex)
        }
    }

    fun deleteSong(song: SongEntity) {
        viewModelScope.launch {
            repository.deleteSong(song)
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicController.release()
    }
}
