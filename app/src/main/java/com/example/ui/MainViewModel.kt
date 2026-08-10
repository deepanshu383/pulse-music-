package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.AppDatabase
import com.example.data.local.SongEntity
import com.example.data.remote.ApiArtistDetail
import com.example.data.remote.ApiArtistSearchItem
import com.example.data.remote.ApiOnlinePlaylistDetail
import com.example.data.remote.ApiPlaylistItem
import com.example.data.remote.ApiSong
import com.example.data.repository.MusicRepository
import com.example.service.MusicController
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri

sealed class AddToPlaylistTarget {
    data class Song(val song: com.example.data.local.SongEntity) : AddToPlaylistTarget()
    data class OnlinePlaylist(val id: String, val name: String) : AddToPlaylistTarget()
}

enum class ViewMode { LIST, GRID, ALBUM, ARTIST }
enum class SortOrder { TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "pulse_music_db"
    ).addMigrations(AppDatabase.MIGRATION_3_4)
    .fallbackToDestructiveMigration().build()

    private val userPreferences = UserPreferencesRepository(application)
    val folderUri = userPreferences.folderUriFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val repository = MusicRepository(application, db.songDao())
    val playlistRepository = com.example.data.repository.PlaylistRepository(db.playlistDao(), db.songDao())
    val playlistPrefs = com.example.data.local.PlaylistPreferencesRepository(application)

    val allPlaylists = playlistRepository.getAllPlaylists().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val jioSaavnRepository = com.example.data.repository.JioSaavnRepository()

    // ═══════════════════════════════════════════════
    // Helper: Map ApiSong → SongEntity
    // ═══════════════════════════════════════════════
    private fun mapApiSongToEntity(apiSong: ApiSong): SongEntity {
        val artistName = apiSong.artists?.primary
            ?.mapNotNull { it.name.takeIf { n -> n.isNotBlank() } }
            ?.joinToString(", ")
            ?.takeIf { it.isNotBlank() }
            ?: "Unknown Artist"
        return SongEntity(
            filePath = apiSong.id,
            title = apiSong.name,
            artist = artistName,
            album = apiSong.album?.name ?: "Unknown Album",
            duration = (apiSong.duration ?: 0).toLong() * 1000L,
            albumArtPath = apiSong.image?.lastOrNull()?.url,
            isOnline = true,
            downloadUrl = apiSong.downloadUrl?.lastOrNull()?.url
        )
    }

    // ═══════════════════════════════════════════════
    // Add to Playlist State
    // ═══════════════════════════════════════════════
    private val _addToPlaylistTarget = MutableStateFlow<AddToPlaylistTarget?>(null)
    val addToPlaylistTarget = _addToPlaylistTarget.asStateFlow()

    fun openAddToPlaylist(target: AddToPlaylistTarget) {
        _addToPlaylistTarget.value = target
    }

    fun closeAddToPlaylist() {
        _addToPlaylistTarget.value = null
    }

    fun addSongToPlaylist(playlistId: Long, song: SongEntity) {
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, song.filePath, song)
            _addToPlaylistTarget.value = null
        }
    }

    fun addOnlinePlaylistToPlaylist(playlistId: Long, onlinePlaylistId: String) {
        viewModelScope.launch {
            val result = jioSaavnRepository.getPlaylistDetails(onlinePlaylistId)
            if (result.isSuccess) {
                val apiSongs = result.getOrNull()?.songs ?: emptyList()
                val entities = apiSongs.map { mapApiSongToEntity(it) }
                val paths = entities.map { it.filePath }
                val map = entities.associateBy { it.filePath }
                playlistRepository.addSongsToPlaylist(playlistId, paths, map)
                _addToPlaylistTarget.value = null
            }
        }
    }

    fun createNewPlaylistAndAdd(name: String, target: AddToPlaylistTarget) {
        viewModelScope.launch {
            val newPlaylistId = playlistRepository.createPlaylist(name)
            when (target) {
                is AddToPlaylistTarget.Song -> {
                    addSongToPlaylist(newPlaylistId, target.song)
                }
                is AddToPlaylistTarget.OnlinePlaylist -> {
                    addOnlinePlaylistToPlaylist(newPlaylistId, target.id)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════
    // Home Page State
    // ═══════════════════════════════════════════════
    private val _featuredPlaylists = MutableStateFlow<List<ApiPlaylistItem>>(emptyList())
    val featuredPlaylists = _featuredPlaylists.asStateFlow()

    private val _trendingSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val trendingSongs = _trendingSongs.asStateFlow()

    private val _homeLoading = MutableStateFlow(true)
    val homeLoading = _homeLoading.asStateFlow()

    private val _homeError = MutableStateFlow<String?>(null)
    val homeError = _homeError.asStateFlow()

    fun fetchHomeData() {
        viewModelScope.launch {
            _homeLoading.value = true
            _homeError.value = null
            try {
                val playlistsDeferred = async { jioSaavnRepository.getFeaturedPlaylists() }
                val trendingDeferred = async { jioSaavnRepository.getTrendingSongs() }

                val playlistsResult = playlistsDeferred.await()
                val trendingResult = trendingDeferred.await()

                if (playlistsResult.isSuccess) {
                    _featuredPlaylists.value = playlistsResult.getOrNull() ?: emptyList()
                }
                if (trendingResult.isSuccess) {
                    _trendingSongs.value = (trendingResult.getOrNull() ?: emptyList()).map { mapApiSongToEntity(it) }
                }
                if (playlistsResult.isFailure && trendingResult.isFailure) {
                    _homeError.value = "Failed to load content. Check your connection."
                }
            } catch (e: Exception) {
                _homeError.value = "Failed to load content. Check your connection."
            } finally {
                _homeLoading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════
    // Search State
    // ═══════════════════════════════════════════════
    private val _onlineSearchResults = MutableStateFlow<List<SongEntity>>(emptyList())
    val onlineSearchResults = _onlineSearchResults.asStateFlow()

    private val _playlistSearchResults = MutableStateFlow<List<ApiPlaylistItem>>(emptyList())
    val playlistSearchResults = _playlistSearchResults.asStateFlow()

    private val _artistSearchResults = MutableStateFlow<List<ApiArtistSearchItem>>(emptyList())
    val artistSearchResults = _artistSearchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var searchDebounceJob: Job? = null

    val localSearchResults = combine(repository.cachedSongs, _searchQuery) { songs, query ->
        if (query.isBlank()) emptyList()
        else songs.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchDebounceJob?.cancel()
        if (query.isNotBlank()) {
            searchDebounceJob = viewModelScope.launch {
                delay(400L)
                // Launch all three searches in parallel
                val songsDeferred = async { jioSaavnRepository.searchSongs(query) }
                val playlistsDeferred = async { jioSaavnRepository.searchPlaylists(query) }
                val artistsDeferred = async { jioSaavnRepository.searchArtists(query) }

                val songsResult = songsDeferred.await()
                if (songsResult.isSuccess) {
                    _onlineSearchResults.value = (songsResult.getOrNull() ?: emptyList()).map { mapApiSongToEntity(it) }
                } else {
                    _onlineSearchResults.value = emptyList()
                }

                val playlistsResult = playlistsDeferred.await()
                _playlistSearchResults.value = if (playlistsResult.isSuccess) {
                    playlistsResult.getOrNull() ?: emptyList()
                } else emptyList()

                val artistsResult = artistsDeferred.await()
                _artistSearchResults.value = if (artistsResult.isSuccess) {
                    artistsResult.getOrNull() ?: emptyList()
                } else emptyList()
            }
        } else {
            _onlineSearchResults.value = emptyList()
            _playlistSearchResults.value = emptyList()
            _artistSearchResults.value = emptyList()
        }
    }

    // ═══════════════════════════════════════════════
    // Search History State
    // ═══════════════════════════════════════════════
    val searchHistory = db.searchHistoryDao().getSearchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveSearchHistory(name: String, type: String, referenceId: String) {
        viewModelScope.launch {
            val dao = db.searchHistoryDao()
            // Remove previous entry for same reference to avoid duplicates
            dao.deleteByReference(referenceId, type)
            dao.insertSearchHistory(
                com.example.data.local.SearchHistoryEntity(
                    name = name,
                    type = type,
                    referenceId = referenceId,
                    timestamp = System.currentTimeMillis()
                )
            )
            // Limit to top 15 entries
            dao.deleteOldEntries(15)
        }
    }

    fun removeSearchHistoryItem(id: Long) {
        viewModelScope.launch {
            db.searchHistoryDao().deleteSearchHistory(id)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            db.searchHistoryDao().clearSearchHistory()
        }
    }

    fun playHistorySong(songId: String, songName: String) {
        viewModelScope.launch {
            // First check local cache
            val localSong = db.songDao().getSongByPath(songId)
            if (localSong != null) {
                playSong(localSong, listOf(localSong))
                playlistPrefs.saveLastPlayedSongId(songId)
                return@launch
            }
            // Fallback to searching the song name and playing the first match
            val searchRes = jioSaavnRepository.searchSongs(songName)
            if (searchRes.isSuccess) {
                val apiSongs = searchRes.getOrNull() ?: emptyList()
                val match = apiSongs.find { it.id == songId } ?: apiSongs.firstOrNull()
                if (match != null) {
                    val entity = mapApiSongToEntity(match)
                    playSong(entity, listOf(entity))
                    playlistPrefs.saveLastPlayedSongId(entity.filePath)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════
    // Artist Profile State
    // ═══════════════════════════════════════════════
    private val _artistDetail = MutableStateFlow<ApiArtistDetail?>(null)
    val artistDetail = _artistDetail.asStateFlow()

    private val _artistSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val artistSongs = _artistSongs.asStateFlow()

    private val _artistLoading = MutableStateFlow(false)
    val artistLoading = _artistLoading.asStateFlow()

    fun fetchArtistDetail(artistId: String) {
        viewModelScope.launch {
            _artistLoading.value = true
            _artistDetail.value = null
            _artistSongs.value = emptyList()
            val result = jioSaavnRepository.getArtistDetails(artistId)
            if (result.isSuccess) {
                val detail = result.getOrNull()
                _artistDetail.value = detail
                _artistSongs.value = (detail?.topSongs ?: emptyList()).map { mapApiSongToEntity(it) }
            }
            _artistLoading.value = false
        }
    }

    // ═══════════════════════════════════════════════
    // Online Playlist Detail State
    // ═══════════════════════════════════════════════
    private val _onlinePlaylistDetail = MutableStateFlow<ApiOnlinePlaylistDetail?>(null)
    val onlinePlaylistDetail = _onlinePlaylistDetail.asStateFlow()

    private val _onlinePlaylistSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val onlinePlaylistSongs = _onlinePlaylistSongs.asStateFlow()

    private val _onlinePlaylistLoading = MutableStateFlow(false)
    val onlinePlaylistLoading = _onlinePlaylistLoading.asStateFlow()

    fun fetchOnlinePlaylistDetail(playlistId: String) {
        viewModelScope.launch {
            _onlinePlaylistLoading.value = true
            _onlinePlaylistDetail.value = null
            _onlinePlaylistSongs.value = emptyList()
            val result = jioSaavnRepository.getPlaylistDetails(playlistId)
            if (result.isSuccess) {
                val detail = result.getOrNull()
                _onlinePlaylistDetail.value = detail
                _onlinePlaylistSongs.value = (detail?.songs ?: emptyList()).map { mapApiSongToEntity(it) }
            }
            _onlinePlaylistLoading.value = false
        }
    }

    val musicController = MusicController(application)

    init {
        musicController.initialize {
            val lastPlayed = playlistPrefs.getLastPlayedSongId()
            val lastPosition = playlistPrefs.getLastPlaybackPosition()
            val savedQueue = playlistPrefs.getSavedQueue()
            if (lastPlayed != null || savedQueue.isNotEmpty()) {
                viewModelScope.launch {
                    val songsList = repository.cachedSongs.first()
                    if (songsList.isNotEmpty()) {
                        musicController.setInitialQueue(lastPlayed, savedQueue, songsList, lastPosition)
                    }
                }
            }
        }

        // Save playback state (ID and position) whenever it updates
        viewModelScope.launch {
            musicController.playbackState.collect { state ->
                state.currentSongId?.let { id ->
                    playlistPrefs.saveLastPlayedSongId(id)
                }
                // Save position, but avoid saving 0 if it just started or resets
                if (state.currentPosition > 0) {
                    playlistPrefs.saveLastPlaybackPosition(state.currentPosition)
                }
            }
        }

        // Sync playlists from Firebase Firestore on startup / login
        viewModelScope.launch {
            playlistRepository.syncFromFirestore()
        }

        fetchHomeData()

        var lastFetchedSongId: String? = null
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
                
                // Fetch Phase 4 data if the song changed
                if (state.currentSongId != null && state.currentSongId != lastFetchedSongId) {
                    lastFetchedSongId = state.currentSongId
                    viewModelScope.launch {
                        val allSongs = repository.cachedSongs.first()
                        allSongs.find { it.filePath == state.currentSongId }?.let { currentSong ->
                            fetchLyrics(currentSong)
                            fetchRecommendations(currentSong)
                        }
                    }
                }
            }
        }
    }

    private val _isAutoAddEnabled = MutableStateFlow(playlistPrefs.isAutoAddEnabled())
    val isAutoAddEnabled = _isAutoAddEnabled.asStateFlow()

    fun syncPlaylists() {
        viewModelScope.launch {
            playlistRepository.syncFromFirestore()
        }
    }

    fun clearLocalData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Delete all playlist songs
            db.query("DELETE FROM playlist_songs", null)
            // Delete all playlists
            db.query("DELETE FROM playlists", null)
            // Delete all online song metadata
            db.query("DELETE FROM song_metadata", null)
        }
    }

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
        
        // Fetch Phase 4 data
        fetchLyrics(song)
        fetchRecommendations(song)
    }


    private val _sortOrder = MutableStateFlow(SortOrder.TITLE)
    val sortOrder = _sortOrder.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode = _viewMode.asStateFlow()

    val songs = combine(
        repository.cachedSongs,
        _sortOrder
    ) { songsList, sort ->
        when (sort) {
            SortOrder.TITLE -> songsList.sortedBy { it.title }
            SortOrder.ARTIST -> songsList.sortedBy { it.artist }
            SortOrder.ALBUM -> songsList.sortedBy { it.album }
            SortOrder.DATE_ADDED -> songsList.sortedByDescending { it.lastModified }
            SortOrder.DURATION -> songsList.sortedByDescending { it.duration }
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

    // Undo stack for recently deleted items (max 5)
    private val _deletedSongsHistory = java.util.ArrayDeque<Pair<Int, androidx.media3.common.MediaItem>>(5)
    
    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    fun removeQueueItem(index: Int) {
        val queue = musicController.playbackState.value.queue
        if (index in queue.indices) {
            val item = queue[index]
            // Add to history stack
            if (_deletedSongsHistory.size >= 5) {
                _deletedSongsHistory.removeLast() // Remove oldest
            }
            _deletedSongsHistory.push(index to item) // Add newest to top
            _canUndo.value = true
            
            musicController.removeQueueItem(index)
        }
    }

    fun undoDelete() {
        if (_deletedSongsHistory.isNotEmpty()) {
            val (index, item) = _deletedSongsHistory.pop()
            musicController.insertQueueItem(index, item)
            _canUndo.value = _deletedSongsHistory.isNotEmpty()
        }
    }

    fun insertQueueItem(index: Int, mediaItem: androidx.media3.common.MediaItem) {
        musicController.insertQueueItem(index, mediaItem)
    }

    fun clearQueue() {
        _deletedSongsHistory.clear()
        _canUndo.value = false
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

    /**
     * Adds a single online (JioSaavn) song to a playlist.
     * The song metadata is persisted in Room so it survives the session and appears in the playlist.
     */
    fun addOnlineSongToPlaylist(playlistId: Long, song: SongEntity) {
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(
                playlistId   = playlistId,
                songPath     = song.filePath,
                songMetadata = song
            )
        }
    }

    /** Adds an online (JioSaavn) song directly to the current playback queue. */
    fun addOnlineSongToQueue(song: SongEntity) {
        musicController.addToQueue(song)
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

    private val downloader = com.example.service.MusicDownloader(application)

    fun downloadSong(song: SongEntity) {
        downloader.downloadSong(song) { downloadedPath ->
            viewModelScope.launch {
                val updatedSong = song.copy(
                    isDownloaded = true,
                    localDownloadPath = downloadedPath
                )
                // Reuse existing db instance (Bug A2 fix)
                db.songDao().insertSongs(listOf(updatedSong))
            }
        }
    }

    private val _currentLyrics = MutableStateFlow<String?>(null)
    val currentLyrics = _currentLyrics.asStateFlow()

    private val _recommendations = MutableStateFlow<List<SongEntity>>(emptyList())
    val recommendations = _recommendations.asStateFlow()

    fun fetchLyrics(song: SongEntity) {
        viewModelScope.launch {
            try {
                // Check if API key is a placeholder (Bug A7 fix)
                val apiKey = com.example.data.remote.NetworkModule.musixmatchApiKey
                if (apiKey.startsWith("PLACEHOLDER")) {
                    _currentLyrics.value = null // Don't show lyrics section
                    return@launch
                }
                _currentLyrics.value = "Loading lyrics..."
                val response = com.example.data.remote.NetworkModule.musixmatchApiService.getLyrics(
                    track = song.title,
                    artist = song.artist,
                    apiKey = apiKey
                )
                _currentLyrics.value = response.message?.body?.lyrics?.lyricsBody ?: "Lyrics not found."
            } catch (e: Exception) {
                _currentLyrics.value = null // Hide section on error
            }
        }
    }

    fun fetchRecommendations(song: SongEntity) {
        viewModelScope.launch {
            try {
                // Check if API key is a placeholder (Bug A7 fix)
                val apiKey = com.example.data.remote.NetworkModule.lastFmApiKey
                if (apiKey.startsWith("PLACEHOLDER")) {
                    _recommendations.value = emptyList()
                    return@launch
                }
                val response = com.example.data.remote.NetworkModule.lastFmApiService.getSimilarTracks(
                    track = song.title,
                    artist = song.artist,
                    apiKey = apiKey
                )
                
                val tracks = response.similartracks?.track ?: emptyList()
                val mapped = tracks.map { track ->
                    SongEntity(
                        filePath = track.name ?: "",
                        title = track.name ?: "Unknown",
                        artist = track.artist?.name ?: "Unknown",
                        album = "Similar Track",
                        duration = 0L,
                        albumArtPath = track.image?.lastOrNull()?.url,
                        isOnline = true
                    )
                }
                _recommendations.value = mapped
            } catch (e: Exception) {
                _recommendations.value = emptyList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicController.release()
    }
}
