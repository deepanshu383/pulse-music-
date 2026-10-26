package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.AppDatabase
import com.example.data.local.sessionDataStore
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
import kotlinx.coroutines.awaitAll
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
    ).addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6)
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
    private val sessionStateStore = com.example.data.local.SessionStateStore(application.sessionDataStore)

    val allPlaylists = playlistRepository.getAllPlaylists().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val jioSaavnRepository = com.example.data.repository.JioSaavnRepository()

    private val recommendationEngine = com.example.recommendation.DefaultRecommendationEngine(
        timeOfDayScorer = com.example.recommendation.TimeOfDayScorer(db.playbackHistoryDao()),
        fatigueScorer = com.example.recommendation.FatigueScorer(db.playbackHistoryDao()),
        userPreferencesManager = com.example.data.local.UserPreferencesManager(application)
    )
    
    private val weightAnalyticsManager = com.example.recommendation.WeightAnalyticsManager()
    private var userAffinityProfile = com.example.data.remote.UserAffinityProfile()

    private var lastAutoEnqueuedForSongId: String? = null
    
    // Caches for API songs to avoid re-fetching metadata for score features
    private val onlineSongYearCache = mutableMapOf<String, Int>()
    private val onlineSongLangCache = mutableMapOf<String, String>()
    private val onlineSongTagsCache = mutableMapOf<String, List<String>>()
    private var lockedSessionLanguage: String? = null

    init {
        viewModelScope.launch {
            val history = db.playbackHistoryDao().getRecentHistory(20)
            val trackIds = history.map { it.trackId }
            val canonicalKeys = history.map { it.canonicalSongKey }.filter { it.isNotBlank() }
            com.example.recommendation.RecommendationSessionManager.prepopulateHistory(trackIds, canonicalKeys)
            
            val (moodLock, langLock) = sessionStateStore.restoreLock()
            if (moodLock != null) {
                com.example.recommendation.RecommendationSessionManager.registerManualPick(moodLock)
            }
            if (langLock != null) {
                lockedSessionLanguage = langLock
            }

            // Temporary instrumentation run
            if (com.example.BuildConfig.DEBUG) {
                runInstrumentationLoop()
            }
        }
    }

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
                
                // ═══════════════════════════════════════════════
                // Auto-enqueue next recommended track
                // ═══════════════════════════════════════════════
                if (state.currentSongId != null
                    && state.queue.isNotEmpty()
                    && state.currentQueueIndex >= 0
                    && state.currentQueueIndex >= state.queue.size - 1
                    && state.currentSongId != lastAutoEnqueuedForSongId
                    && _isAutoPlayNextEnabled.value
                ) {
                    lastAutoEnqueuedForSongId = state.currentSongId
                    viewModelScope.launch {
                        try {
                            val allLocalSongs = repository.cachedSongs.first()
                            
                            // Find current song
                            val currentSong = allLocalSongs.find { it.filePath == state.currentSongId }
                                ?: state.queue.getOrNull(state.currentQueueIndex)?.let { mediaItem ->
                                    SongEntity(
                                        filePath = mediaItem.mediaId,
                                        title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                                        artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                                        album = mediaItem.mediaMetadata.albumTitle?.toString() ?: "Unknown",
                                        duration = 0L,
                                        albumArtPath = mediaItem.mediaMetadata.artworkUri?.toString(),
                                        isOnline = true
                                    )
                                }
                            
                            if (currentSong != null) {
                                val currentFeatures = currentSong.toTrackFeatures()
                                // Register current song as played
                                com.example.recommendation.RecommendationSessionManager.registerPlayedTrack(
                                    currentSong.filePath, currentSong.title, currentSong.artist
                                )
                                val lockedGroup = com.example.recommendation.RecommendationSessionManager.getLockedMoodGroup() ?: "None"
                                android.util.Log.d("RecommendationEngine", "Session Locked Mood Group: $lockedGroup")
                                
                                val localCandidates = allLocalSongs.filter { it.filePath != currentSong.filePath }
                                
                                // ═══════════════════════════════════════════════
                                // BROADENED CANDIDATE POOL (Phase A)
                                // Multi-source: JioSaavn mood+lang, mood-only, artist, Last.fm cross-search
                                // ═══════════════════════════════════════════════
                                val currentMood = com.example.recommendation.MoodExtractionStrategy.getPrimaryMood(
                                    currentSong.title, currentSong.album, currentSong.artist
                                )
                                // Priority: Fetch candidates using the session's LOCKED mood to prevent pool drift
                                val fetchMood = com.example.recommendation.RecommendationSessionManager.getLockedMoodGroup() ?: currentMood
                                var currentLang = lockedSessionLanguage ?: onlineSongLangCache[currentSong.filePath] ?: currentFeatures.language?.lowercase()
                                val targetLang = determineTargetLanguage(currentLang)
                                
                                val moodLangCandidates = mutableListOf<SongEntity>()
                                val moodOnlyCandidates = mutableListOf<SongEntity>()
                                val artistCandidates = mutableListOf<SongEntity>()
                                val lastFmCrossCandidates = mutableListOf<SongEntity>()
                                
                                try {
                                    // Fire 3 parallel JioSaavn searches + 1 Last.fm search
                                    val moodLangDeferred = viewModelScope.async {
                                        try {
                                            jioSaavnRepository.searchSongsByMood(fetchMood, targetLang, 25)
                                        } catch (e: Exception) { Result.failure(e) }
                                    }
                                    
                                    val moodOnlyDeferred = viewModelScope.async {
                                        try {
                                            jioSaavnRepository.searchSongsByMood(fetchMood, targetLang, 25)
                                        } catch (e: Exception) { Result.failure(e) }
                                    }
                                    
                                    val artistDeferred = viewModelScope.async {
                                        try {
                                            jioSaavnRepository.searchSongs(currentSong.artist)
                                        } catch (e: Exception) { Result.failure(e) }
                                    }
                                    
                                    val lastFmDeferred = viewModelScope.async {
                                        try {
                                            com.example.data.remote.NetworkModule.lastFmApiService.getSimilarTracks(
                                                track = currentSong.title,
                                                artist = currentSong.artist,
                                                apiKey = com.example.data.remote.NetworkModule.lastFmApiKey
                                            )
                                        } catch (e: Exception) { null }
                                    }
                                    
                                    // Await all results
                                    val artistResult = artistDeferred.await()
                                    val artistApiSongs = artistResult.getOrNull() ?: emptyList()
                                    if (artistApiSongs.isNotEmpty()) {
                                        // Infer language from artist results (most reliable source)
                                        currentLang = artistApiSongs.firstOrNull()?.language
                                    }
                                    
                                    // Process artist results
                                    artistApiSongs.forEach { apiSong ->
                                        apiSong.year?.toIntOrNull()?.let { onlineSongYearCache[apiSong.id] = it }
                                        apiSong.language?.let { onlineSongLangCache[apiSong.id] = it }
                                    }
                                    artistCandidates.addAll(artistApiSongs.map { mapApiSongToEntity(it) })
                                    
                                    // Now re-fire mood+lang with the inferred language if we got it
                                    val moodLangResult = if (currentLang != null) {
                                        try {
                                            jioSaavnRepository.searchSongsByMood(fetchMood, currentLang, 25)
                                        } catch (e: Exception) { moodLangDeferred.await() }
                                    } else {
                                        moodLangDeferred.await()
                                    }
                                    val moodLangApiSongs = moodLangResult.getOrNull() ?: emptyList()
                                    moodLangApiSongs.forEach { apiSong ->
                                        apiSong.year?.toIntOrNull()?.let { onlineSongYearCache[apiSong.id] = it }
                                        apiSong.language?.let { onlineSongLangCache[apiSong.id] = it }
                                    }
                                    moodLangCandidates.addAll(moodLangApiSongs.map { mapApiSongToEntity(it) })
                                    
                                    // Process mood-only results
                                    val moodOnlyApiSongs = moodOnlyDeferred.await().getOrNull() ?: emptyList()
                                    moodOnlyApiSongs.forEach { apiSong ->
                                        apiSong.year?.toIntOrNull()?.let { onlineSongYearCache[apiSong.id] = it }
                                        apiSong.language?.let { onlineSongLangCache[apiSong.id] = it }
                                    }
                                    moodOnlyCandidates.addAll(moodOnlyApiSongs.map { mapApiSongToEntity(it) })
                                    
                                    // Process Last.fm similar tracks → cross-search on JioSaavn
                                    val lastFmResponse = lastFmDeferred.await()
                                    val similarTracks = lastFmResponse?.similartracks?.track?.take(5) ?: emptyList()
                                    for (lfmTrack in similarTracks) {
                                        val lfmName = lfmTrack.name ?: continue
                                        val lfmArtist = lfmTrack.artist?.name ?: continue
                                        try {
                                            val crossResult = jioSaavnRepository.searchSongs("$lfmName $lfmArtist")
                                            val crossSongs = crossResult.getOrNull()?.take(3) ?: emptyList()
                                            crossSongs.forEach { apiSong ->
                                                apiSong.year?.toIntOrNull()?.let { onlineSongYearCache[apiSong.id] = it }
                                                apiSong.language?.let { onlineSongLangCache[apiSong.id] = it }
                                            }
                                            lastFmCrossCandidates.addAll(crossSongs.map { mapApiSongToEntity(it) })
                                        } catch (_: Exception) { /* skip failed cross-search */ }
                                    }
                                    
                                } catch (e: Exception) {
                                    android.util.Log.w("RecommendationEngine", "Broadened pool fetch failed", e)
                                }
                                
                                // ═══════════════════════════════════════════════
                                // PRIORITY MERGE, DEDUP, CAP AT 50
                                // ═══════════════════════════════════════════════
                                val POOL_CAP = 50
                                val seenIds = mutableSetOf<String>()
                                val mergedCandidates = mutableListOf<SongEntity>()
                                
                                // Helper to add without duplicates
                                fun addUnique(songs: List<SongEntity>) {
                                    for (song in songs) {
                                        if (mergedCandidates.size >= POOL_CAP) break
                                        if (seenIds.add(song.filePath)) {
                                            mergedCandidates.add(song)
                                        }
                                    }
                                }
                                
                                // Priority order: mood+lang > lastFm cross > mood-only > artist > local
                                addUnique(moodLangCandidates)
                                addUnique(lastFmCrossCandidates)
                                addUnique(moodOnlyCandidates)
                                addUnique(artistCandidates)
                                addUnique(localCandidates)
                                
                                // ═══════════════════════════════════════════════
                                // TAG FETCHING (For Devotional Filter and Language)
                                // ═══════════════════════════════════════════════
                                val apiKey = com.example.data.remote.NetworkModule.lastFmApiKey
                                if (!apiKey.startsWith("PLACEHOLDER")) {
                                    val tagJobs = mergedCandidates.map { candidate ->
                                        viewModelScope.async {
                                            if (!onlineSongTagsCache.containsKey(candidate.filePath)) {
                                                try {
                                                    val res = com.example.data.remote.NetworkModule.lastFmApiService.getTrackInfo(
                                                        candidate.title, candidate.artist, apiKey
                                                    )
                                                    val tags = res.track?.toptags?.tag?.map { it.name.lowercase() } ?: emptyList()
                                                    onlineSongTagsCache[candidate.filePath] = tags
                                                } catch (e: Exception) {
                                                    onlineSongTagsCache[candidate.filePath] = emptyList()
                                                }
                                            }
                                        }
                                    }
                                    tagJobs.awaitAll()
                                }

                                // ═══════════════════════════════════════════════
                                // DEVOTIONAL CONTENT HARD EXCLUSION (before scoring)
                                // ═══════════════════════════════════════════════
                                val preFilterSize = mergedCandidates.size
                                mergedCandidates.removeAll { candidate ->
                                    val mood = com.example.recommendation.MoodExtractionStrategy.getPrimaryMood(
                                        candidate.title, candidate.album, candidate.artist
                                    )
                                    val apiTags = onlineSongTagsCache[candidate.filePath] ?: emptyList()
                                    com.example.recommendation.DevotionalContentFilter.shouldExclude(
                                        title = candidate.title,
                                        album = candidate.album,
                                        artist = candidate.artist,
                                        moodGenreTag = mood,
                                        apiTags = apiTags
                                    )
                                }
                                val devotionalRemoved = preFilterSize - mergedCandidates.size
                                if (devotionalRemoved > 0) {
                                    android.util.Log.i("PoolComposition", "DevotionalFilter: removed $devotionalRemoved devotional candidates")
                                }
                                
                                // ═══════════════════════════════════════════════
                                // LOG POOL COMPOSITION
                                // ═══════════════════════════════════════════════
                                val moodDistribution = mergedCandidates.groupingBy { candidate ->
                                    com.example.recommendation.MoodExtractionStrategy.getPrimaryMood(
                                        candidate.title, candidate.album, candidate.artist
                                    )
                                }.eachCount()
                                val onlineCount = mergedCandidates.count { it.isOnline }
                                val localCount = mergedCandidates.size - onlineCount
                                android.util.Log.i(
                                    "PoolComposition",
                                    "currentMood=$currentMood | total=${mergedCandidates.size}, " +
                                    moodDistribution.entries.sortedByDescending { it.value }.joinToString(", ") { "${it.key}=${it.value}" } +
                                    " | localCount=$localCount, onlineCount=$onlineCount"
                                )
                                
                                // 1. HARD EXCLUSION: Never replay a song played in this session or recently
                                val relaxedRecentCanonicalKeys = state.queue.takeLast(5).map { 
                                    com.example.recommendation.RecommendationSessionManager.getCanonicalSongKey(
                                        it.mediaMetadata.title?.toString() ?: "",
                                        it.mediaMetadata.artist?.toString() ?: ""
                                    ) 
                                }.toSet()
                                
                                var validCandidates = mergedCandidates.filter { candidate ->
                                    val notPlayedInSession = !com.example.recommendation.RecommendationSessionManager.isTrackPlayed(
                                        candidate.filePath, candidate.title, candidate.artist
                                    )
                                    val canonicalKey = com.example.recommendation.RecommendationSessionManager.getCanonicalSongKey(candidate.title, candidate.artist)
                                    notPlayedInSession && canonicalKey !in relaxedRecentCanonicalKeys && candidate.filePath != currentSong.filePath
                                }
                                
                                // Extreme fallback if empty
                                if (validCandidates.isEmpty()) {
                                    validCandidates = mergedCandidates.filter { candidate -> 
                                        !com.example.recommendation.RecommendationSessionManager.isTrackPlayed(candidate.filePath, candidate.title, candidate.artist)
                                    }
                                }
                                
                                var finalCandidates = validCandidates
                                
                                // Language Fallback: Try Last.fm toptags if missing
                                val knownLanguages = setOf("hindi", "tamil", "telugu", "kannada", "malayalam", "bengali", "marathi", "punjabi", "haryanvi", "rajasthani", "urdu", "english", "gujarati")
                                for (candidate in finalCandidates) {
                                    if (onlineSongLangCache[candidate.filePath].isNullOrBlank()) {
                                        try {
                                            val apiKey = com.example.data.remote.NetworkModule.lastFmApiKey
                                            if (!apiKey.startsWith("PLACEHOLDER")) {
                                                val res = com.example.data.remote.NetworkModule.lastFmApiService.getTrackInfo(candidate.title, candidate.artist, apiKey)
                                                val tags = res.track?.toptags?.tag?.map { it.name.lowercase() } ?: emptyList()
                                                val inferredLang = tags.firstOrNull { it in knownLanguages }
                                                if (inferredLang != null) {
                                                    onlineSongLangCache[candidate.filePath] = inferredLang
                                                }
                                            }
                                        } catch (e: Exception) { /* skip */ }
                                    }
                                }
                                
                                // PROGRESSIVE HARD FILTERS
                                // Language Filter
                                val langFiltered = finalCandidates.filter { candidate ->
                                    val candidateLang = onlineSongLangCache[candidate.filePath]?.lowercase()
                                    val currentLangVal = targetLang?.lowercase() ?: onlineSongLangCache[currentSong.filePath]?.lowercase()
                                    if (currentLangVal != null && candidateLang != null) {
                                        candidateLang == currentLangVal ||
                                        (currentLangVal == "hindi" && candidateLang == "urdu") ||
                                        (currentLangVal == "urdu" && candidateLang == "hindi") ||
                                        (currentLangVal == "haryanvi" && candidateLang == "hindi") ||
                                        (currentLangVal == "punjabi" && candidateLang == "hindi")
                                    } else false // HARD FILTER: exclude if language is still unknown
                                }
                                if (langFiltered.isNotEmpty()) finalCandidates = langFiltered
                                
                                // Mood Filter (currentMood already extracted above in pool section)
                                val conflictingMoods = getConflictingMoodsSet(currentMood)
                                val moodFiltered = finalCandidates.filter { candidate ->
                                    val candidateMood = com.example.recommendation.MoodExtractionStrategy.getPrimaryMood(
                                        candidate.title, candidate.album, candidate.artist
                                    )
                                    candidateMood !in conflictingMoods
                                }
                                if (moodFiltered.isNotEmpty()) finalCandidates = moodFiltered
                                
                                // Category Filter
                                val categoryFiltered = finalCandidates.filter { candidate ->
                                    val currentIsMovie = currentSong.album.let { alb ->
                                        alb.isNotBlank() && !alb.equals("Unknown Album", ignoreCase = true) && !alb.equals(currentSong.title, ignoreCase = true)
                                    }
                                    val candidateIsMovie = candidate.album.let { alb ->
                                        alb.isNotBlank() && !alb.equals("Unknown Album", ignoreCase = true) && !alb.equals(candidate.title, ignoreCase = true)
                                    }
                                    if (currentIsMovie) candidateIsMovie else true
                                }
                                if (categoryFiltered.isNotEmpty()) finalCandidates = categoryFiltered
                                
                                if (finalCandidates.isNotEmpty()) {
                                    val candidateFeatures = finalCandidates.map { it.toTrackFeatures() }
                                    var scores = recommendationEngine.scoreNext(
                                        currentFeatures, 
                                        candidateFeatures, 
                                        candidateFeatures.size < 20,
                                        userAffinityProfile.factorMultipliers,
                                        userAffinityProfile.explorationMultiplier,
                                        effectiveLanguage = targetLang
                                    )

                                    // Artist diversity penalty
                                    val recentArtists = state.queue.takeLast(5).mapNotNull { it.mediaMetadata.artist?.toString()?.lowercase() }
                                    scores = scores.map { score ->
                                        val song = finalCandidates.find { it.filePath == score.trackId }
                                        val artistLower = song?.artist?.lowercase() ?: ""
                                        val artistRepeatCount = recentArtists.count { it == artistLower }
                                        if (artistRepeatCount > 0) {
                                            score.copy(fatiguePenalty = score.fatiguePenalty + (artistRepeatCount * 0.15f))
                                        } else {
                                            score
                                        }
                                    }.sortedByDescending { it.totalScore }
                                    
                                    // ═══════════════════════════════════════════════
                                    // LOG TOP 10 CANDIDATES SCORES (DEV ONLY)
                                    // ═══════════════════════════════════════════════
                                    if (com.example.BuildConfig.DEBUG) {
                                        android.util.Log.d("RecommendationScores", "--- Top 10 Candidate Scores (Session Locked Mood: $lockedGroup) ---")
                                        scores.take(10).forEachIndexed { index, score ->
                                            val song = finalCandidates.find { it.filePath == score.trackId }
                                            val name = song?.title ?: score.trackId
                                            val canonicalKey = com.example.recommendation.RecommendationSessionManager.getCanonicalSongKey(
                                                song?.title ?: "", song?.artist ?: ""
                                            )
                                            
                                            val hardFilters = mutableListOf<String>()
                                            if (score.isHardFiltered) {
                                                hardFilters.add(score.hardFilterReason ?: "UNKNOWN")
                                            }
                                            
                                            val filterStr = if (hardFilters.isNotEmpty()) " [BLOCKED: ${hardFilters.joinToString()}]" else ""
                                            
                                            android.util.Log.d(
                                                "RecommendationScores",
                                                "#${index + 1}: '$name' -> Total: ${score.totalScore}$filterStr\n" +
                                                "    CanonicalKey: $canonicalKey\n" +
                                                "    Mood: ${score.moodConsistency} | Acoustic: ${score.acousticSimilarity} | Era: ${score.eraProximity}\n" +
                                                "    Lang: ${score.languageConsistency} | Pref: ${score.userPreference} | Fatigue: ${score.fatiguePenalty}\n" +
                                                "    Jitter: ${score.jitterBonus}"
                                            )
                                        }
                                        android.util.Log.d("RecommendationScores", "-------------------------------")
                                    }
                                    
                                    val topScore = scores.firstOrNull()
                                    if (topScore != null) {
                                        // Update the locked language only if we successfully picked a track
                                        val pickedLang = onlineSongLangCache[topScore.trackId] ?: targetLang
                                        if (pickedLang != null) {
                                            lockedSessionLanguage = pickedLang
                                        }

                                        viewModelScope.launch {
                                            sessionStateStore.persistLock(
                                                com.example.recommendation.RecommendationSessionManager.getLockedMoodGroup(),
                                                lockedSessionLanguage
                                            )
                                        }
                                    }

                                    val bestTrackId = topScore?.trackId
                                    if (bestTrackId != null) {
                                        val bestSong = finalCandidates.find { it.filePath == bestTrackId }
                                        if (bestSong != null) {
                                            musicController.addToQueue(bestSong)
                                            
                                            val mood = com.example.recommendation.MoodExtractionStrategy.getPrimaryMood(bestSong.title, bestSong.album, bestSong.artist)
                                            com.example.recommendation.RecommendationSessionManager.registerAutoPick(mood)
                                            
                                            com.example.recommendation.RecommendationSessionManager.registerPlayedTrack(
                                                bestSong.filePath, bestSong.title, bestSong.artist
                                            )
                                            
                                            val bestScore = scores.first()
                                            val factorScores = mapOf(
                                                "moodConsistency" to bestScore.moodConsistency,
                                                "acousticSimilarity" to bestScore.acousticSimilarity,
                                                "timeOfDayFit" to bestScore.timeOfDayFit,
                                                "eraProximity" to bestScore.eraProximity,
                                                "userPreference" to bestScore.userPreference,
                                                "fatiguePenalty" to bestScore.fatiguePenalty,
                                                "jitterBonus" to bestScore.jitterBonus
                                            )
                                            val reason = when {
                                                bestScore.moodConsistency >= 0.8f && bestScore.acousticSimilarity >= 0.6f -> "Perfect Match"
                                                bestScore.acousticSimilarity >= 0.8f -> "Similar Artist"
                                                bestScore.moodConsistency >= 0.8f -> "Mood Match"
                                                bestScore.userPreference > 0.5f -> "Highly Preferred"
                                                else -> "General Fallback"
                                            }
                                            com.example.recommendation.ListeningEventLogger.registerRecommendation(
                                                trackId = bestSong.filePath,
                                                context = com.example.recommendation.RecommendationContext(
                                                    reason = reason,
                                                    totalScore = bestScore.totalScore,
                                                    factorScores = factorScores,
                                                    moodTags = listOf(bestSong.toTrackFeatures().moodGenreTag),
                                                    genreTags = emptyList()
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("RecommendationEngine", "Auto-enqueue failed", e)
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
        
        // Anti-drift: Anchor session to manual pick
        val mood = com.example.recommendation.MoodExtractionStrategy.getPrimaryMood(song.title, song.album, song.artist)
        com.example.recommendation.RecommendationSessionManager.registerManualPick(mood)
        
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

    fun SongEntity.toTrackFeatures(): com.example.recommendation.TrackFeatures {
        val year = onlineSongYearCache[filePath] ?: 2020 // fallback year
        val lang = onlineSongLangCache[filePath]
        return com.example.recommendation.TrackFeatures(
            trackId = filePath,
            title = title,
            artist = artist,
            album = album,
            acousticClusterId = "default",
            playCount = 1,
            skipCount = 0,
            lastPlayedTimestamp = System.currentTimeMillis(),
            averageCompletionPercentage = 1.0f,
            userRating = null,
            moodGenreTag = com.example.recommendation.MoodExtractionStrategy.getPrimaryMood(title, album, artist),
            releaseYear = year,
            language = lang
        )
    }

    private fun getConflictingMoodsSet(mood: String): Set<String> {
        return when (mood.lowercase()) {
            "romantic", "lullaby", "sleep", "sad" -> setOf("party", "hip-hop", "workout", "edm", "rock", "devotional", "happy", "energetic")
            "party", "hip-hop", "workout", "edm", "rock", "happy", "energetic" -> setOf("romantic", "lullaby", "sleep", "sad", "classical", "devotional")
            "devotional" -> setOf("party", "hip-hop", "workout", "edm", "rock", "romantic", "sad", "lullaby", "bollywood", "pop", "happy", "energetic")
            else -> emptySet()
        }
    }

    private fun determineTargetLanguage(lockedLang: String?): String? {
        if (lockedLang == null) return null
        val roll = (1..100).random()
        return when (lockedLang) {
            "hindi" -> {
                if (roll <= 87) "hindi"
                else if (roll <= 97) listOf("punjabi", "haryanvi", "rajasthani").random()
                else listOf("tamil", "telugu", "english").random()
            }
            "punjabi", "haryanvi", "rajasthani" -> {
                if (roll <= 87) lockedLang
                else if (roll <= 97) "hindi"
                else listOf("tamil", "telugu", "english").random()
            }
            "tamil", "telugu", "kannada", "malayalam" -> {
                if (roll <= 87) lockedLang
                else if (roll <= 97) listOf("tamil", "telugu", "kannada", "malayalam").filter { it != lockedLang }.random()
                else listOf("hindi", "english").random()
            }
            else -> lockedLang
        }
    }

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
    // ═══════════════════════════════════════════════
    // DEV INSTRUMENTATION LOOP
    // ═══════════════════════════════════════════════
    private suspend fun runInstrumentationLoop() {
        android.util.Log.i("DevInstrumentation", "=== STARTING 20-TRACK REAL SESSION SIMULATION ===")
        
        // Seed track: Random non-Romantic, non-English
        val seedSongsResult = jioSaavnRepository.searchSongs("Diljit Dosanjh Lover")
        var currentSong = seedSongsResult.getOrNull()?.firstOrNull()?.let { mapApiSongToEntity(it) }
        
        if (currentSong == null) {
            android.util.Log.e("DevInstrumentation", "Failed to fetch seed track.")
            return
        }
        
        // Manual pick locks the mood
        com.example.recommendation.RecommendationSessionManager.registerManualPick(
            com.example.recommendation.MoodExtractionStrategy.getPrimaryMood(currentSong.title, currentSong.album, currentSong.artist)
        )
        
        val sb = StringBuilder()
        sb.append("=== STARTING 20-TRACK REAL SESSION SIMULATION ===\n")
        
        for (i in 1..20) {
            sb.append("\n--- ITERATION $i ---\n")
            sb.append("Current Track: '${currentSong!!.title}' by ${currentSong.artist}\n")
            val lockedGroup = com.example.recommendation.RecommendationSessionManager.getLockedMoodGroup()
            sb.append("Locked Mood Group: $lockedGroup\n")
            
            val fetchMood = lockedGroup ?: "Party"
            
            val moodLangSongs = jioSaavnRepository.searchSongsByMood(fetchMood, "punjabi", 25).getOrNull() ?: emptyList()
            val moodOnlySongs = jioSaavnRepository.searchSongsByMood(fetchMood, null, 25).getOrNull() ?: emptyList()
            val artistSongs = jioSaavnRepository.searchSongs(currentSong.artist).getOrNull() ?: emptyList()
            
            val mergedApi = (moodLangSongs + moodOnlySongs + artistSongs).distinctBy { it.id }.take(50).map { mapApiSongToEntity(it) }
            
            val apiKey = com.example.data.remote.NetworkModule.lastFmApiKey
            if (!apiKey.startsWith("PLACEHOLDER")) {
                val tagJobs = mergedApi.map { candidate ->
                    viewModelScope.async {
                        if (!onlineSongTagsCache.containsKey(candidate.filePath)) {
                            try {
                                val res = com.example.data.remote.NetworkModule.lastFmApiService.getTrackInfo(
                                    candidate.title, candidate.artist, apiKey
                                )
                                val tags = res.track?.toptags?.tag?.map { it.name.lowercase() } ?: emptyList()
                                onlineSongTagsCache[candidate.filePath] = tags
                            } catch (e: Exception) {
                                onlineSongTagsCache[candidate.filePath] = emptyList()
                            }
                        }
                    }
                }
                tagJobs.awaitAll()
            }
            
            val validCandidates = mergedApi.filter { 
                val apiTags = onlineSongTagsCache[it.filePath] ?: emptyList()
                !com.example.recommendation.DevotionalContentFilter.shouldExclude(it.title, it.album, it.artist, 
                    com.example.recommendation.MoodExtractionStrategy.getPrimaryMood(it.title, it.album, it.artist), apiTags)
            }.toMutableList()
            
            val devotionalExcluded = mergedApi.size - validCandidates.size
            if (devotionalExcluded > 0) {
                sb.append("Devotional Filter: Excluded $devotionalExcluded candidates.\n")
            }
            
            val candidateFeatures = validCandidates.map { it.toTrackFeatures() }
            val scores = recommendationEngine.scoreNext(
                currentSong.toTrackFeatures(),
                candidateFeatures,
                false,
                emptyMap(),
                0.5f
            ).sortedByDescending { it.totalScore }
            
            val top5 = scores.take(5)
            sb.append("Top 5 Candidates:\n")
            for ((idx, score) in top5.withIndex()) {
                val song = validCandidates.find { it.filePath == score.trackId }
                val title = song?.title ?: "Unknown"
                val artist = song?.artist ?: "Unknown"
                val canonical = com.example.recommendation.RecommendationSessionManager.getCanonicalSongKey(title, artist)
                
                val hardFilters = mutableListOf<String>()
                if (score.languageConsistency <= -9999.0f) hardFilters.add("LANG")
                if (score.eraProximity <= -9999.0f) hardFilters.add("ERA")
                if (score.moodConsistency <= -9999.0f) hardFilters.add("MOOD")
                val filterStr = if (hardFilters.isNotEmpty()) " [BLOCKED: ${hardFilters.joinToString()}]" else ""
                
                sb.append("  ${idx+1}. '$title' by $artist (Canonical: $canonical)\n")
                sb.append("     TotalScore: ${score.totalScore}$filterStr | Mood: ${score.moodConsistency} | Era: ${score.eraProximity} | Lang: ${score.languageConsistency}\n")
            }
            
            val winnerScore = top5.firstOrNull()
            if (winnerScore == null || winnerScore.totalScore <= -9999.0f) {
                sb.append(">> HALT: Pool exhausted or all blocked.\n")
                break
            }
            
            val winnerSong = validCandidates.find { it.filePath == winnerScore.trackId }!!
            sb.append(">> WINNER PICKED: '${winnerSong.title}'\n")
            
            // Advance simulation
            com.example.recommendation.RecommendationSessionManager.registerPlayedTrack(winnerSong.filePath, winnerSong.title, winnerSong.artist)
            com.example.recommendation.RecommendationSessionManager.registerAutoPick(
                com.example.recommendation.MoodExtractionStrategy.getPrimaryMood(winnerSong.title, winnerSong.album, winnerSong.artist)
            )
            currentSong = winnerSong
        }
        
        sb.append("\n=== END SIMULATION ===\n")
        android.util.Log.i("DevInstrumentation", sb.toString())
    }

    override fun onCleared() {
        super.onCleared()
        musicController.release()
    }
}
            ).sortedByDescending { it.totalScore }
            
            val top5 = scores.take(5)
            sb.append("Top 5 Candidates:\n")
            for ((idx, score) in top5.withIndex()) {
                val song = validCandidates.find { it.filePath == score.trackId }
                val title = song?.title ?: "Unknown"
                val artist = song?.artist ?: "Unknown"
                val canonical = com.example.recommendation.RecommendationSessionManager.getCanonicalSongKey(title, artist)
                
                val hardFilters = mutableListOf<String>()
                if (score.languageConsistency <= -9999.0f) hardFilters.add("LANG")
                if (score.eraProximity <= -9999.0f) hardFilters.add("ERA")
                if (score.moodConsistency <= -9999.0f) hardFilters.add("MOOD")
                val filterStr = if (hardFilters.isNotEmpty()) " [BLOCKED: ${hardFilters.joinToString()}]" else ""
                
                sb.append("  ${idx+1}. '$title' by $artist (Canonical: $canonical)\n")
                sb.append("     TotalScore: ${score.totalScore}$filterStr | Mood: ${score.moodConsistency} | Era: ${score.eraProximity} | Lang: ${score.languageConsistency}\n")
            }
            
            val winnerScore = top5.firstOrNull()
            if (winnerScore == null || winnerScore.totalScore <= -9999.0f) {
                sb.append(">> HALT: Pool exhausted or all blocked.\n")
                break
            }
            
            val winnerSong = validCandidates.find { it.filePath == winnerScore.trackId }!!
            sb.append(">> WINNER PICKED: '${winnerSong.title}'\n")
            
            // Advance simulation
            com.example.recommendation.RecommendationSessionManager.registerPlayedTrack(winnerSong.filePath, winnerSong.title, winnerSong.artist)
            com.example.recommendation.RecommendationSessionManager.registerAutoPick(
                com.example.recommendation.MoodExtractionStrategy.getPrimaryMood(winnerSong.title, winnerSong.album, winnerSong.artist)
            )
            currentSong = winnerSong
        }
        
        sb.append("\n=== END SIMULATION ===\n")
        android.util.Log.i("DevInstrumentation", sb.toString())
    }

    override fun onCleared() {
        super.onCleared()
        musicController.release()
    }
}
