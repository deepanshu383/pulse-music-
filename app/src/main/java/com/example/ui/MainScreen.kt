package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.PlaylistEntity
import com.example.data.local.SongEntity
import com.example.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private data class NavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)

private val navItems = listOf(
    NavItem("home", Icons.Filled.Home, Icons.Outlined.Home, "Home"),
    NavItem("library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "Playlists"),
    NavItem("search", Icons.Filled.Search, Icons.Outlined.Search, "Search"),
    NavItem("settings", Icons.Filled.Person, Icons.Outlined.Person, "Profile")
)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    currentThemeMode: AppThemeMode = AppThemeMode.MIDNIGHT,
    onThemeChange: (AppThemeMode) -> Unit = {}
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val folderUri by viewModel.folderUri.collectAsStateWithLifecycle()
    val onlineSearchResults by viewModel.onlineSearchResults.collectAsStateWithLifecycle()

    // Collect ONLY play/pause and active song identity at top-level (NOT currentPosition!)
    // so playback timer ticks never cause scrolling lists or NavHost to recompose.
    val isPlaying by remember(viewModel) {
        viewModel.playbackState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    val activeTrackFromController by remember(viewModel) {
        viewModel.playbackState.map { state ->
            val item = state.queue.getOrNull(state.currentQueueIndex)
            if (item != null) {
                SongEntity(
                    filePath = item.mediaId,
                    title = item.mediaMetadata.title?.toString() ?: "Unknown",
                    artist = item.mediaMetadata.artist?.toString() ?: "Unknown",
                    album = item.mediaMetadata.albumTitle?.toString() ?: "Unknown",
                    albumArtPath = item.mediaMetadata.artworkUri?.toString(),
                    duration = item.mediaMetadata.extras?.getLong("duration") ?: 0L,
                    isOnline = item.mediaMetadata.extras?.getBoolean("isOnline") ?: true
                )
            } else null
        }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)

    val activeSongFromController by remember(viewModel) {
        viewModel.playbackState.map { it.currentSong }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)

    val activeSongIdFromController by remember(viewModel) {
        viewModel.playbackState.map { it.currentSongId }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val lastPlayedSongId = remember { viewModel.playlistPrefs.getLastPlayedSongId() }
    val activeSongId = activeSongIdFromController ?: activeSongFromController?.filePath ?: lastPlayedSongId
    val nowPlayingSong = remember(activeSongFromController, activeTrackFromController, activeSongId, songs, onlineSearchResults) {
        activeSongFromController ?: activeTrackFromController ?: run {
            val sId = activeSongId ?: songs.firstOrNull()?.filePath
            songs.find { it.filePath == sId } ?: onlineSearchResults.find { it.filePath == sId }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                // B1: Mini Player Bar (visible when a song is playing/selected)
                val showMiniPlayer = nowPlayingSong != null && currentRoute != "player"
                if (showMiniPlayer) {
                    MiniPlayerBar(
                        song = nowPlayingSong!!,
                        isPlaying = isPlaying,
                        playbackStateFlow = viewModel.playbackState,
                        onPlayPause = {
                            if (isPlaying) viewModel.musicController.pause()
                            else viewModel.musicController.play()
                        },
                        onNext = { viewModel.musicController.next() },
                        onPrevious = { viewModel.musicController.previous() },
                        onClick = {
                            navController.navigate("player") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                PulseBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        val isSubScreen = currentRoute != null && currentRoute !in listOf("home", "search", "library", "settings")
                        navController.navigate(route) {
                            popUpTo("home") { saveState = !isSubScreen }
                            launchSingleTop = true
                            restoreState = !isSubScreen
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(110)) },
            exitTransition = { fadeOut(animationSpec = tween(110)) }
        ) {
            composable("home") {
                // Discover section state
                val featuredPlaylists by viewModel.featuredPlaylists.collectAsStateWithLifecycle()
                val trendingSongs by viewModel.trendingSongs.collectAsStateWithLifecycle()
                val homeLoading by viewModel.homeLoading.collectAsStateWithLifecycle()
                val homeError by viewModel.homeError.collectAsStateWithLifecycle()

                HomeScreen(
                    featuredPlaylists = featuredPlaylists,
                    trendingSongs = trendingSongs,
                    homeLoading = homeLoading,
                    homeError = homeError,
                    onRetryHomeLoad = { viewModel.fetchHomeData(forceRefresh = true) },
                    onPlaylistClick = { id -> navController.navigate("online_playlist/$id") },
                    onTrendingSongClick = { song, list ->
                        viewModel.playSong(song, list)
                        viewModel.playlistPrefs.saveLastPlayedSongId(song.filePath)
                    },
                    onAddToQueue = { song ->
                        if (song.isOnline) viewModel.addOnlineSongToQueue(song)
                        else viewModel.addToQueue(song)
                    },
                    currentSongId = activeSongId,
                    isPlaying = isPlaying,
                    onTogglePlayPause = {
                        if (isPlaying) viewModel.musicController.pause()
                        else viewModel.musicController.play()
                    }
                )
            }

            composable("player") {
                val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
                val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
                val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
                val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()

                // Only render if we have a song
                if (nowPlayingSong != null) {
                    PlayerScreen(
                        song = nowPlayingSong,
                        playbackState = playbackState,
                        queueSongCount = playbackState.queue.size,
                        canUndo = canUndo,
                        currentLyrics = currentLyrics,
                        recommendations = recommendations,
                        onPlayPause = {
                            if (playbackState.isPlaying) {
                                viewModel.musicController.pause()
                            } else {
                                if (playbackState.currentSongId == nowPlayingSong.filePath && playbackState.queue.isNotEmpty()) {
                                    viewModel.musicController.play()
                                } else {
                                    viewModel.playSong(nowPlayingSong, songs)
                                }
                            }
                        },
                        onNext = {
                            if (playbackState.queue.isNotEmpty()) {
                                viewModel.musicController.next()
                            } else {
                                val currentIndex = songs.indexOfFirst { it.filePath == nowPlayingSong.filePath }
                                if (currentIndex != -1 && currentIndex < songs.size - 1) {
                                    val nextSong = songs[currentIndex + 1]
                                    viewModel.playSong(nextSong, songs)
                                    viewModel.playlistPrefs.saveLastPlayedSongId(nextSong.filePath)
                                }
                            }
                        },
                        onPrevious = { viewModel.musicController.previous() },
                        onSeek = { position -> viewModel.musicController.seekTo(position) },
                        onShuffleToggle = {
                            viewModel.musicController.setShuffleMode(!playbackState.shuffleModeEnabled)
                        },
                        onRepeatToggle = {
                            val newMode = when (playbackState.repeatMode) {
                                androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                                androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                                else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                            }
                            viewModel.musicController.setRepeatMode(newMode)
                        },
                        onClearQueue = { viewModel.clearQueue() },
                        onRemoveFromQueue = { index -> viewModel.removeQueueItem(index) },
                        onGoToSong = { index -> viewModel.skipToQueueItem(index) },
                        onMoveQueueItem = { from, to -> viewModel.moveQueueItem(from, to) },
                        onInsertQueueItem = { index, item -> viewModel.insertQueueItem(index, item) },
                        onUndoDelete = { viewModel.undoDelete() },
                        onRepeatQueueItem = { index -> viewModel.repeatQueueItem(index) },
                        onBack = { navController.popBackStack() }
                    )
                }

            }

            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onSongClick = { song, list ->
                        viewModel.playSong(song, list)
                        viewModel.playlistPrefs.saveLastPlayedSongId(song.filePath)
                    },
                    onPlaylistClick = { id, name -> 
                        navController.navigate("online_playlist/$id")
                        viewModel.saveSearchHistory(name, "PLAYLIST", id)
                    },
                    onArtistClick = { id, name -> 
                        navController.navigate("artist_profile/$id")
                        viewModel.saveSearchHistory(name, "ARTIST", id)
                    },
                    currentSongId = activeSongId,
                    isPlaying = isPlaying,
                    onTogglePlayPause = {
                        if (isPlaying) viewModel.musicController.pause()
                        else viewModel.musicController.play()
                    }
                )
            }

            composable("artist_profile/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: return@composable
                
                LaunchedEffect(id) {
                    viewModel.fetchArtistDetail(id)
                }
                
                val artistDetail by viewModel.artistDetail.collectAsStateWithLifecycle()
                val artistSongs by viewModel.artistSongs.collectAsStateWithLifecycle()
                val isLoading by viewModel.artistLoading.collectAsStateWithLifecycle()
                
                ArtistProfileScreen(
                    artistDetail = artistDetail,
                    artistSongs = artistSongs,
                    isLoading = isLoading,
                    onBack = { navController.popBackStack() },
                    onSongClick = { song, list ->
                        viewModel.playSong(song, list)
                        viewModel.playlistPrefs.saveLastPlayedSongId(song.filePath)
                    },
                    onAddToQueue = { song ->
                        if (song.isOnline) viewModel.addOnlineSongToQueue(song)
                        else viewModel.addToQueue(song)
                    },
                    onAddToPlaylist = { song -> viewModel.openAddToPlaylist(AddToPlaylistTarget.Song(song)) },
                    currentSongId = activeSongId,
                    isPlaying = isPlaying,
                    onTogglePlayPause = {
                        if (isPlaying) viewModel.musicController.pause()
                        else viewModel.musicController.play()
                    }
                )
            }

            composable("online_playlist/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: return@composable
                
                LaunchedEffect(id) {
                    viewModel.fetchOnlinePlaylistDetail(id)
                }
                
                val playlistDetail by viewModel.onlinePlaylistDetail.collectAsStateWithLifecycle()
                val songs by viewModel.onlinePlaylistSongs.collectAsStateWithLifecycle()
                val isLoading by viewModel.onlinePlaylistLoading.collectAsStateWithLifecycle()
                
                OnlinePlaylistScreen(
                    playlistDetail = playlistDetail,
                    songs = songs,
                    isLoading = isLoading,
                    onBack = { navController.popBackStack() },
                    onSongClick = { song, list ->
                        viewModel.playSong(song, list)
                        viewModel.playlistPrefs.saveLastPlayedSongId(song.filePath)
                    },
                    onAddToQueue = { song ->
                        if (song.isOnline) viewModel.addOnlineSongToQueue(song)
                        else viewModel.addToQueue(song)
                    },
                    onAddToPlaylist = { song -> viewModel.openAddToPlaylist(AddToPlaylistTarget.Song(song)) },
                    onAddOnlinePlaylistToPlaylist = { pid, pname -> 
                        viewModel.openAddToPlaylist(AddToPlaylistTarget.OnlinePlaylist(pid, pname))
                    },
                    currentSongId = activeSongId,
                    isPlaying = isPlaying,
                    onTogglePlayPause = {
                        if (isPlaying) viewModel.musicController.pause()
                        else viewModel.musicController.play()
                    }
                )
            }

            composable("library") {
                val initialSortOrderStr = viewModel.playlistPrefs.getLibrarySortOrder()
                val initialViewModeStr = viewModel.playlistPrefs.getLibraryViewMode()

                val initialSortOrder = try { PlaylistSortOrder.valueOf(initialSortOrderStr) } catch(e: Exception) { PlaylistSortOrder.DATE_ADDED_NEWEST }
                val initialViewMode = try { PlaylistViewMode.valueOf(initialViewModeStr) } catch(e: Exception) { PlaylistViewMode.LIST }

                LibraryScreen(
                    playlists = allPlaylists,
                    songs = songs,
                    initialSortOrder = initialSortOrder,
                    initialViewMode = initialViewMode,
                    onSortOrderChange = { order -> viewModel.playlistPrefs.saveLibrarySortOrder(order.name) },
                    onViewModeChange = { mode -> viewModel.playlistPrefs.saveLibraryViewMode(mode.name) },
                    onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                    onPlaylistClick = { id -> navController.navigate("playlist_details/$id") },
                    onSongClick = { song ->
                        viewModel.playSong(song, songs)
                        viewModel.playlistPrefs.saveLastPlayedSongId(song.filePath)
                    },
                    onAddToQueue = { song -> viewModel.addToQueue(song) }
                )
            }

            composable("settings") {
                val isAutoAddEnabled by viewModel.isAutoAddEnabled.collectAsStateWithLifecycle()
                val isSaveQueueEnabled by viewModel.isSaveQueueEnabled.collectAsStateWithLifecycle()
                val isAutoPlayNextEnabled by viewModel.isAutoPlayNextEnabled.collectAsStateWithLifecycle()

                SettingsScreen(
                    onClearPreferences = { viewModel.playlistPrefs.clearAllPreferences() },
                    currentFolder = folderUri,
                    onChangeFolder = { uri -> viewModel.scanMusic(uri.toString()) },
                    onRescan = { folderUri?.let { viewModel.scanMusic(it) } },
                    isAutoAddEnabled = isAutoAddEnabled,
                    onAutoAddChange = { viewModel.setAutoAddEnabled(it) },
                    isSaveQueueEnabled = isSaveQueueEnabled,
                    onSaveQueueChange = { viewModel.setSaveQueueEnabled(it) },
                    isAutoPlayNextEnabled = isAutoPlayNextEnabled,
                    onAutoPlayNextChange = { viewModel.setAutoPlayNextEnabled(it) },
                    currentThemeMode = currentThemeMode,
                    onThemeChange = onThemeChange
                )
            }

            composable("playlist_details/{id}") { backStackEntry ->
                val idString = backStackEntry.arguments?.getString("id")
                val id = idString?.toLongOrNull() ?: 0L
                val isAllSongsPlaylist = id == -1L
                val isDownloadedPlaylist = id == -2L

                val playlist = when {
                    isAllSongsPlaylist -> PlaylistEntity(
                        id = -1L,
                        name = "All Songs",
                        description = "${songs.size} songs • Library",
                        createdDate = 0L
                    )
                    isDownloadedPlaylist -> {
                        val offline = songs.filter { !it.isOnline }
                        PlaylistEntity(
                            id = -2L,
                            name = "Downloaded Songs",
                            description = "${offline.size} songs • Offline Files",
                            createdDate = 0L
                        )
                    }
                    else -> allPlaylists.find { it.id == id }
                }

                val dbPlaylistSongs by viewModel.playlistRepository.getSongsForPlaylist(id)
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                val playlistSongs = when {
                    isAllSongsPlaylist -> songs
                    isDownloadedPlaylist -> songs.filter { !it.isOnline }
                    else -> dbPlaylistSongs
                }

                if (playlist != null) {
                    val initialSortOrderStr = viewModel.playlistPrefs.getSortOrder(id)
                    val initialViewModeStr = viewModel.playlistPrefs.getViewMode(id)

                    val initialSortOrder = try { PlaylistSortOrder.valueOf(initialSortOrderStr) } catch(e: Exception) { PlaylistSortOrder.DATE_ADDED_NEWEST }
                    val initialViewMode = try { PlaylistViewMode.valueOf(initialViewModeStr) } catch(e: Exception) { PlaylistViewMode.LIST }

                    PlaylistDetailsScreen(
                        playlist = playlist,
                        songs = playlistSongs,
                        initialSortOrder = initialSortOrder,
                        initialViewMode = initialViewMode,
                        onSortOrderChange = { order -> viewModel.playlistPrefs.saveSortOrder(id, order.name) },
                        onViewModeChange = { mode -> viewModel.playlistPrefs.saveViewMode(id, mode.name) },
                        onBack = { navController.popBackStack() },
                        onAddMoreSongs = { if (id > 0) navController.navigate("select_songs/$id") },
                        onRemoveSong = { song -> if (id > 0) viewModel.removeSongFromPlaylist(id, song.filePath) },
                        onMoveSong = { from, to -> if (id > 0) viewModel.reorderPlaylistSongs(id, from, to) },
                        onDeletePlaylist = {
                            if (id > 0) {
                                viewModel.deletePlaylist(id)
                                navController.popBackStack()
                            }
                        },
                        onEditPlaylistName = { name -> if (id > 0) viewModel.updatePlaylistName(id, name) },
                        onSongClick = { song ->
                            viewModel.playSong(song, playlistSongs)
                            viewModel.playlistPrefs.saveLastPlayedSongId(song.filePath)
                        },
                        onAddToQueue = { song -> viewModel.addToQueue(song) },
                        onAddToPlaylist = { song -> viewModel.openAddToPlaylist(AddToPlaylistTarget.Song(song)) },
                        currentSongId = activeSongId,
                        isPlaying = isPlaying,
                        onTogglePlayPause = {
                            if (isPlaying) viewModel.musicController.pause()
                            else viewModel.musicController.play()
                        }
                    )
                }
            }

            composable("select_songs/{id}") { backStackEntry ->
                val idString = backStackEntry.arguments?.getString("id")
                val id = idString?.toLongOrNull() ?: 0L

                val existingSongs by viewModel.playlistRepository.getSongsForPlaylist(id)
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                SelectSongsScreen(
                    allSongs = songs,
                    existingSongs = existingSongs,
                    onBack = { navController.popBackStack() },
                    onAddSongs = { selectedPaths ->
                        viewModel.addSongsToPlaylist(id, selectedPaths)
                    }
                )
            }
        }
    }

    val addToPlaylistTarget by viewModel.addToPlaylistTarget.collectAsStateWithLifecycle()
    if (addToPlaylistTarget != null) {
        AddToPlaylistBottomSheet(
            target = addToPlaylistTarget!!,
            playlists = allPlaylists,
            onDismiss = { viewModel.closeAddToPlaylist() },
            onPlaylistSelected = { playlist ->
                when (val target = addToPlaylistTarget!!) {
                    is AddToPlaylistTarget.Song -> viewModel.addSongToPlaylist(playlist.id, target.song)
                    is AddToPlaylistTarget.OnlinePlaylist -> viewModel.addOnlinePlaylistToPlaylist(playlist.id, target.id)
                }
            },
            onCreateNewPlaylist = { name ->
                viewModel.createNewPlaylistAndAdd(name, addToPlaylistTarget!!)
            }
        )
    }
}

@Composable
private fun PulseBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    // Always show bottom navigation bar except when full-screen song player is open
    val showNavBar = currentRoute != "player"
    val activeTabRoute = when {
        currentRoute == null -> "home"
        currentRoute.startsWith("playlist_details") || currentRoute.startsWith("select_songs") -> "library"
        currentRoute.startsWith("online_playlist") || currentRoute.startsWith("artist") -> "home"
        else -> currentRoute
    }

    AnimatedVisibility(
        visible = showNavBar,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .glassmorphic(shape = RoundedCornerShape(32.dp), fillAlpha = 0.38f)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEach { item ->
                        val isSelected = activeTabRoute == item.route
                        PulseNavItem(
                            icon = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            isSelected = isSelected,
                            onClick = { onNavigate(item.route) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseNavItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) RanjhaNeonPink else TextSecondary,
        animationSpec = tween(200),
        label = "navIconColor"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(RanjhaNeonPink)
                )
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    song: SongEntity,
    isPlaying: Boolean,
    playbackStateFlow: kotlinx.coroutines.flow.StateFlow<com.example.service.PlaybackState>,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .glassmorphic(shape = RoundedCornerShape(20.dp), fillAlpha = 0.32f)
            .clickable(onClick = onClick)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                coil.compose.AsyncImage(
                    model = song.albumArtPath ?: android.R.drawable.ic_media_play,
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Song Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Play/Pause Button
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Next Button
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Isolated leaf composable so ONLY the 2dp progress bar redraws on playback ticks!
            MiniPlayerProgressBar(playbackStateFlow = playbackStateFlow)
        }
    }
}

@Composable
private fun MiniPlayerProgressBar(
    playbackStateFlow: kotlinx.coroutines.flow.StateFlow<com.example.service.PlaybackState>
) {
    val progress by remember(playbackStateFlow) {
        playbackStateFlow.map { state ->
            if (state.duration > 0) {
                (state.currentPosition.toFloat() / state.duration.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = 0f)

    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp),
        color = RanjhaNeonPink,
        trackColor = Color.Transparent
    )
}
