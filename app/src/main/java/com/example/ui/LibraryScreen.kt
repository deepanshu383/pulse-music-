package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PlaylistEntity
import com.example.data.local.SongEntity
import com.example.ui.theme.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    playlists: List<PlaylistEntity>,
    songs: List<SongEntity>,
    initialSortOrder: PlaylistSortOrder = PlaylistSortOrder.DATE_ADDED_NEWEST,
    initialViewMode: PlaylistViewMode = PlaylistViewMode.LIST,
    onSortOrderChange: (PlaylistSortOrder) -> Unit = {},
    onViewModeChange: (PlaylistViewMode) -> Unit = {},
    onCreatePlaylist: (String) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onSongClick: (SongEntity) -> Unit,
    onAddToQueue: (SongEntity) -> Unit = {}
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    var sortOrder by remember(initialSortOrder) { mutableStateOf(initialSortOrder) }
    var viewMode by remember(initialViewMode) { mutableStateOf(initialViewMode) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    val sortedSongs = remember(songs, sortOrder) {
        when (sortOrder) {
            PlaylistSortOrder.DATE_ADDED_NEWEST -> songs.reversed()
            PlaylistSortOrder.DATE_ADDED_OLDEST -> songs
            PlaylistSortOrder.ARTIST_A_Z -> songs.sortedBy { it.artist }
            PlaylistSortOrder.ARTIST_Z_A -> songs.sortedByDescending { it.artist }
            PlaylistSortOrder.SONG_A_Z -> songs.sortedBy { it.title }
            PlaylistSortOrder.SONG_Z_A -> songs.sortedByDescending { it.title }
            PlaylistSortOrder.DURATION_SHORTEST -> songs.sortedBy { it.duration }
            PlaylistSortOrder.DURATION_LONGEST -> songs.sortedByDescending { it.duration }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = PulseAccent,
                    contentColor = androidx.compose.ui.graphics.Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Create",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "📚 Your Music",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "All Playlists",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Downloaded Songs Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clickable { /* Could navigate to a filtered view */ },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PulseAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    tint = PulseAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Downloaded Songs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${songs.size} songs • Local Files",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextTertiary
                        )
                    }
                }
            }

            // Playlist Cards
            if (playlists.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(playlists) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }

            // Sort & View Bar
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sort button
                    Box {
                        Surface(
                            onClick = { sortMenuExpanded = true },
                            shape = RoundedCornerShape(20.dp),
                            color = DarkSurfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sort",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            PlaylistSortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.displayName) },
                                    onClick = {
                                        sortOrder = order
                                        onSortOrderChange(order)
                                        sortMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == order) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = PulseAccent)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // View mode toggles
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        ViewModeButton(
                            icon = Icons.Default.ViewList,
                            isSelected = viewMode == PlaylistViewMode.LIST,
                            onClick = {
                                viewMode = PlaylistViewMode.LIST
                                onViewModeChange(PlaylistViewMode.LIST)
                            }
                        )
                        ViewModeButton(
                            icon = Icons.Default.GridView,
                            isSelected = viewMode == PlaylistViewMode.GRID,
                            onClick = {
                                viewMode = PlaylistViewMode.GRID
                                onViewModeChange(PlaylistViewMode.GRID)
                            }
                        )
                        ViewModeButton(
                            icon = Icons.Default.ViewAgenda,
                            isSelected = viewMode == PlaylistViewMode.CARD,
                            onClick = {
                                viewMode = PlaylistViewMode.CARD
                                onViewModeChange(PlaylistViewMode.CARD)
                            }
                        )
                    }
                }
            }

            // All Songs section
            item {
                Text(
                    text = "All Songs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (sortedSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.MusicOff,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No songs found.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                when (viewMode) {
                    PlaylistViewMode.LIST -> {
                        items(sortedSongs) { song ->
                            PlaylistSongItem(
                                song = song,
                                viewMode = PlaylistViewMode.LIST,
                                onSongClick = { onSongClick(song) },
                                onRemove = { /* Not applicable here */ },
                                onAddToQueue = { onAddToQueue(song) },
                                hapticFeedback = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                            )
                        }
                    }
                    PlaylistViewMode.GRID -> {
                        val chunked = sortedSongs.chunked(2)
                        items(chunked) { rowSongs ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            ) {
                                rowSongs.forEach { song ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        PlaylistSongItem(
                                            song = song,
                                            viewMode = PlaylistViewMode.GRID,
                                            onSongClick = { onSongClick(song) },
                                            onRemove = { },
                                            onAddToQueue = { onAddToQueue(song) },
                                            hapticFeedback = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                        )
                                    }
                                }
                                if (rowSongs.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    PlaylistViewMode.CARD -> {
                        items(sortedSongs) { song ->
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                                PlaylistSongItem(
                                    song = song,
                                    viewMode = PlaylistViewMode.CARD,
                                    onSongClick = { onSongClick(song) },
                                    onRemove = { },
                                    onAddToQueue = { onAddToQueue(song) },
                                    hapticFeedback = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = DarkSurfaceContainer,
            title = {
                Text(
                    "New Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulseAccent,
                        focusedLabelColor = PulseAccent,
                        cursorColor = PulseAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onCreatePlaylist(playlistName)
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseAccent,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistCard(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PulseViolet.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = PulseViolet,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playlist.description.ifEmpty { "Playlist" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextTertiary
            )
        }
    }
}

@Composable
private fun ViewModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) PulseAccent else TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
