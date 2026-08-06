package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun PlaylistDetailsScreen(
    initialSortOrder: PlaylistSortOrder,
    initialViewMode: PlaylistViewMode,
    onSortOrderChange: (PlaylistSortOrder) -> Unit,
    onViewModeChange: (PlaylistViewMode) -> Unit,
    playlist: PlaylistEntity,
    songs: List<SongEntity>,
    onBack: () -> Unit,
    onAddMoreSongs: () -> Unit,
    onRemoveSong: (SongEntity) -> Unit,
    onDeletePlaylist: () -> Unit,
    onEditPlaylistName: (String) -> Unit,
    onMoveSong: (Int, Int) -> Unit,
    onSongClick: (SongEntity) -> Unit,
    onAddToQueue: (SongEntity) -> Unit = {}
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var sortOrder by remember(initialSortOrder) { mutableStateOf(initialSortOrder) }
    var viewMode by remember(initialViewMode) { mutableStateOf(initialViewMode) }

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var playlistMenuExpanded by remember { mutableStateOf(false) }

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

    // Calculate total duration
    val totalDuration = remember(songs) {
        val totalMs = songs.sumOf { it.duration }
        val totalMinutes = totalMs / 1000 / 60
        if (totalMinutes >= 60) {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            "${hours}h ${mins}m"
        } else {
            "${totalMinutes} min"
        }
    }

    Scaffold(
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onAddMoreSongs) {
                    Icon(Icons.Default.Add, contentDescription = "Add Songs", tint = PulseAccent)
                }
                Box {
                    IconButton(onClick = { playlistMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextSecondary)
                    }
                    DropdownMenu(
                        expanded = playlistMenuExpanded,
                        onDismissRequest = { playlistMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Name") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                playlistMenuExpanded = false
                                showEditDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Playlist", color = ErrorRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                            onClick = {
                                playlistMenuExpanded = false
                                onDeletePlaylist()
                            }
                        )
                    }
                }
            }

            // Playlist Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Playlist icon
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PulseViolet.copy(alpha = 0.15f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = PulseViolet,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${songs.size} songs • $totalDuration",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Shuffle All button
                    if (songs.isNotEmpty()) {
                        Button(
                            onClick = {
                                if (songs.isNotEmpty()) {
                                    onSongClick(songs.random())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PulseAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Shuffle All", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Sort & View controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            Text("Sort", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
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

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(
                        PlaylistViewMode.LIST to Icons.Default.ViewList,
                        PlaylistViewMode.GRID to Icons.Default.GridView,
                        PlaylistViewMode.CARD to Icons.Default.ViewAgenda
                    ).forEach { (mode, icon) ->
                        IconButton(
                            onClick = {
                                viewMode = mode
                                onViewModeChange(mode)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                icon,
                                contentDescription = mode.name,
                                tint = if (viewMode == mode) PulseAccent else TextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Song list
            if (sortedSongs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                            "No songs in this playlist.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onAddMoreSongs) {
                            Text("Add Songs", color = PulseAccent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                when (viewMode) {
                    PlaylistViewMode.LIST -> {
                        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                            items(sortedSongs) { song ->
                                PlaylistSongItem(
                                    song = song,
                                    viewMode = PlaylistViewMode.LIST,
                                    onSongClick = { onSongClick(song) },
                                    onRemove = { onRemoveSong(song) },
                                    onAddToQueue = { onAddToQueue(song) },
                                    hapticFeedback = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                )
                            }
                        }
                    }
                    PlaylistViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(bottom = 120.dp, start = 12.dp, end = 12.dp, top = 8.dp)
                        ) {
                            items(sortedSongs) { song ->
                                PlaylistSongItem(
                                    song = song,
                                    viewMode = PlaylistViewMode.GRID,
                                    onSongClick = { onSongClick(song) },
                                    onRemove = { onRemoveSong(song) },
                                    onAddToQueue = { onAddToQueue(song) },
                                    hapticFeedback = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                )
                            }
                        }
                    }
                    PlaylistViewMode.CARD -> {
                        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp, start = 20.dp, end = 20.dp, top = 8.dp)) {
                            items(sortedSongs) { song ->
                                PlaylistSongItem(
                                    song = song,
                                    viewMode = PlaylistViewMode.CARD,
                                    onSongClick = { onSongClick(song) },
                                    onRemove = { onRemoveSong(song) },
                                    onAddToQueue = { onAddToQueue(song) },
                                    hapticFeedback = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Name Dialog
    if (showEditDialog) {
        var newName by remember { mutableStateOf(playlist.name) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = DarkSurfaceContainer,
            title = {
                Text(
                    "Edit Playlist Name",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
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
                        if (newName.isNotBlank()) {
                            onEditPlaylistName(newName)
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSongItem(
    song: SongEntity,
    viewMode: PlaylistViewMode,
    onSongClick: () -> Unit,
    onRemove: () -> Unit,
    onAddToQueue: () -> Unit = {},
    hapticFeedback: () -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onAddToQueue()
                Toast.makeText(context, "✓ Added to Queue", Toast.LENGTH_SHORT).show()
            }
            false // Always bounce back
        }
    )

    val formattedDuration = remember(song.duration) {
        val totalSeconds = song.duration / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    val modifier = Modifier
        .combinedClickable(
            onClick = { onSongClick() },
            onLongClick = {
                hapticFeedback()
                showContextMenu = true
            }
        )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        enableDismissFromStartToEnd = true,
        backgroundContent = {
            val color = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                PulseAccent.copy(alpha = 0.15f)
            } else {
                Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Add to Queue",
                    tint = PulseAccent
                )
            }
        }
    ) {
        Box {
            when (viewMode) {
                PlaylistViewMode.LIST -> {
                    Row(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Music note icon
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = PulseAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${song.artist} • $formattedDuration",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                PlaylistViewMode.GRID -> {
                    Card(
                        modifier = modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                PlaylistViewMode.CARD -> {
                    Card(
                        modifier = modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PulseCyan.copy(alpha = 0.12f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = PulseCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = formattedDuration,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Play Now") },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        onSongClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Queue") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        onAddToQueue()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Remove") },
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        onRemove()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        // Handle add to playlist
                    }
                )
                DropdownMenuItem(
                    text = { Text("Song Details") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    onClick = {
                        showContextMenu = false
                        // Handle details
                    }
                )
            }
        }
    }
}

enum class PlaylistSortOrder(val displayName: String) {
    DATE_ADDED_NEWEST("Date Added (newest)"),
    DATE_ADDED_OLDEST("Date Added (oldest)"),
    ARTIST_A_Z("Artist Name (A-Z)"),
    ARTIST_Z_A("Artist Name (Z-A)"),
    SONG_A_Z("Song Name (A-Z)"),
    SONG_Z_A("Song Name (Z-A)"),
    DURATION_SHORTEST("Duration (shortest)"),
    DURATION_LONGEST("Duration (longest)")
}

enum class PlaylistViewMode {
    LIST, GRID, CARD
}
