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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Search
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
import com.example.data.local.SongEntity
import com.example.ui.theme.*

private data class NavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)

private val navItems = listOf(
    NavItem("home", Icons.Filled.Home, Icons.Outlined.Home, "Home"),
    NavItem("search", Icons.Filled.Search, Icons.Outlined.Search, "Search"),
    NavItem("library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "Library"),
    NavItem("settings", Icons.Filled.Settings, Icons.Outlined.Settings, "Settings")
)

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val folderUri by viewModel.folderUri.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val lastPlayedSongId = remember { viewModel.playlistPrefs.getLastPlayedSongId() }
    val activeSongId = playbackState.currentSongId ?: lastPlayedSongId ?: songs.firstOrNull()?.filePath
    val activeSong = songs.find { it.filePath == activeSongId }
    val currentSong = songs.find { it.filePath == playbackState.currentSongId }
    val nowPlayingSong = currentSong ?: activeSong

    // Compute queue count for the Home screen badge
    val queueStartIndex = if (playbackState.currentQueueIndex != -1) playbackState.currentQueueIndex + 1 else 0
    val queueSongCount = if (queueStartIndex <= playbackState.queue.size) playbackState.queue.size - queueStartIndex else 0

    // Save last played song
    LaunchedEffect(playbackState.currentSongId) {
        playbackState.currentSongId?.let { id ->
            viewModel.playlistPrefs.saveLastPlayedSongId(id)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            Column {
                // B1: Mini Player Bar (visible when not on Home and a song is playing)
                val showMiniPlayer = currentRoute != "home" && nowPlayingSong != null
                if (showMiniPlayer) {
                    MiniPlayerBar(
                        song = nowPlayingSong!!,
                        isPlaying = playbackState.isPlaying,
                        onPlayPause = {
                            if (playbackState.isPlaying) viewModel.musicController.pause()
                            else viewModel.musicController.play()
                        },
                        onClick = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                PulseBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
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
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable("home") {
                val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
                val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()

                HomeScreen(
                    song = playbackState.currentSongId?.let { id -> songs.find { it.filePath == id } },
                    playbackState = playbackState,
                    queueSongCount = playbackState.queue.size,
                    currentLyrics = currentLyrics,
                    recommendations = recommendations,
                    onPlayPause = {
                        if (playbackState.isPlaying) {
                            viewModel.musicController.pause()
                        } else {
                            if (nowPlayingSong != null) {
                                if (playbackState.currentSongId == nowPlayingSong.filePath && playbackState.queue.isNotEmpty()) {
                                    viewModel.musicController.play()
                                } else {
                                    viewModel.playSong(nowPlayingSong, songs)
                                }
                            }
                        }
                    },
                    onNext = {
                        if (playbackState.queue.isNotEmpty()) {
                            viewModel.musicController.next()
                        } else if (nowPlayingSong != null) {
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
                    onRepeatQueueItem = { index -> viewModel.repeatQueueItem(index) }
                )
            }

            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onSongClick = { song, list ->
                        viewModel.playSong(song, list)
                        viewModel.playlistPrefs.saveLastPlayedSongId(song.filePath)
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
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
                        // Navigate to Home to show the player
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
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
                    onAutoPlayNextChange = { viewModel.setAutoPlayNextEnabled(it) }
                )
            }

            composable("playlist_details/{id}") { backStackEntry ->
                val idString = backStackEntry.arguments?.getString("id")
                val id = idString?.toLongOrNull() ?: 0L
                val playlist = allPlaylists.find { it.id == id }
                val playlistSongs by viewModel.playlistRepository.getSongsForPlaylist(id)
                    .collectAsStateWithLifecycle(initialValue = emptyList())

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
                        onAddMoreSongs = { navController.navigate("select_songs/$id") },
                        onRemoveSong = { song -> viewModel.removeSongFromPlaylist(id, song.filePath) },
                        onMoveSong = { from, to -> viewModel.reorderPlaylistSongs(id, from, to) },
                        onDeletePlaylist = {
                            viewModel.deletePlaylist(id)
                            navController.popBackStack()
                        },
                        onEditPlaylistName = { name -> viewModel.updatePlaylistName(id, name) },
                        onSongClick = { song ->
                            viewModel.playSong(song, playlistSongs)
                            viewModel.playlistPrefs.saveLastPlayedSongId(song.filePath)
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onAddToQueue = { song -> viewModel.addToQueue(song) }
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
}

@Composable
private fun PulseBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    // Only show for main tabs, hide during sub-screens
    val isMainTab = currentRoute in listOf("home", "search", "library", "settings")

    AnimatedVisibility(
        visible = isMainTab,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val isSelected = currentRoute == item.route
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

@Composable
private fun PulseNavItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) PulseAccent else TextTertiary,
        animationSpec = tween(200),
        label = "navIconColor"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(PulseAccent)
                )
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    song: SongEntity,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = DarkSurfaceContainer,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            coil.compose.AsyncImage(
                model = song.albumArtPath,
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp)),
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
        }
    }
}
