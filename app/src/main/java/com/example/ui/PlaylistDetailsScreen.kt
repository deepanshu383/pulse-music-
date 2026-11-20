package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.PlaylistEntity
import com.example.data.local.SongEntity
import com.example.ui.theme.*

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
    onAddToQueue: (SongEntity) -> Unit = {},
    onAddToPlaylist: (SongEntity) -> Unit = {},
    currentSongId: String? = null,
    isPlaying: Boolean = false,
    onTogglePlayPause: () -> Unit = {}
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var playlistMenuExpanded by remember { mutableStateOf(false) }

    // Total Duration formatting
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

    FluidBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ─────────────────────────────────────────
            // 1. Top Bar: Back Button, "PLAYLIST", Search Button
            // ─────────────────────────────────────────
            item(key = "playlist_top_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularGlassButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        size = 42.dp,
                        iconSize = 20.dp
                    )

                    Text(
                        text = "PLAYLIST",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )

                    if (playlist.id > 0L) {
                        Box {
                            CircularGlassButton(
                                icon = Icons.Default.MoreVert,
                                onClick = { playlistMenuExpanded = true },
                                size = 42.dp,
                                iconSize = 20.dp
                            )

                            DropdownMenu(
                                expanded = playlistMenuExpanded,
                                onDismissRequest = { playlistMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add Songs") },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                    onClick = {
                                        playlistMenuExpanded = false
                                        onAddMoreSongs()
                                    }
                                )
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
                    } else {
                        Spacer(modifier = Modifier.size(42.dp))
                    }
                }
            }

            // ─────────────────────────────────────────
            // 2. Hovering Glass Banner Card
            // ─────────────────────────────────────────
            item(key = "playlist_hero_card") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(24.dp),
                        fillAlpha = 0.28f
                    ) {
                        // Ambient banner background image or gradient
                        val firstArtwork = songs.firstOrNull()?.albumArtPath
                        if (!firstArtwork.isNullOrBlank()) {
                            AsyncImage(
                                model = firstArtwork,
                                contentDescription = "Banner",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.Crop
                            )
                            // Dark glass gradient overlay for readability
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0x70090D1C),
                                                Color(0xCC090D1C)
                                            )
                                        )
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF1E3A8A),
                                                Color(0xFF3B1D78),
                                                Color(0xFF0F172A)
                                            )
                                        )
                                    )
                            )
                        }

                        // Text content inside banner
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = playlist.name.ifEmpty { "Romantic & Soulful Hits" },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${songs.size} Songs • $totalDuration",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Floating circular play button on bottom-right of the card
                    if (songs.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 12.dp)
                        ) {
                            CircularGlassButton(
                                icon = Icons.Default.PlayArrow,
                                onClick = { onSongClick(songs.first()) },
                                size = 52.dp,
                                iconSize = 28.dp,
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────
            // 3. Transparent Overlay Controls: "Play" & "Shuffle"
            // ─────────────────────────────────────────
            item(key = "playlist_action_buttons") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Play Button
                    GlassButton(
                        text = "Play",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            if (songs.isNotEmpty()) {
                                onSongClick(songs.first())
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Shuffle Button
                    GlassButton(
                        text = "Shuffle",
                        icon = Icons.Default.Shuffle,
                        onClick = {
                            if (songs.isNotEmpty()) {
                                onSongClick(songs.random())
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ─────────────────────────────────────────
            // 4. Song List (Hovering Glass Cards)
            // ─────────────────────────────────────────
            itemsIndexed(
                items = songs,
                key = { _, song -> "pl_song_${song.filePath}" }
            ) { _, song ->
                val isCurrentTrack = currentSongId != null && song.filePath == currentSongId
                val isActivelyPlaying = isCurrentTrack && isPlaying

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(16.dp),
                    fillAlpha = if (isCurrentTrack) 0.30f else 0.18f,
                    onClick = {
                        if (isCurrentTrack) {
                            onTogglePlayPause()
                        } else {
                            onSongClick(song)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail
                        AsyncImage(
                            model = song.albumArtPath ?: android.R.drawable.ic_media_play,
                            contentDescription = "Song Art",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Title, Artist, Duration
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCurrentTrack) RanjhaNeonPink else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isActivelyPlaying) "Playing • ${song.artist}" else "${song.artist} • ${formatSongDuration(song.duration)}",
                                fontSize = 12.sp,
                                color = if (isActivelyPlaying) RanjhaCyan else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Right side: Show animated live audio waveform ONLY when THIS song is actively playing!
                        if (isActivelyPlaying) {
                            AudioWaveformVisualizer(
                                isPlaying = true,
                                barCount = 12,
                                activeColor = RanjhaCyan,
                                minHeight = 4.dp,
                                maxHeight = 18.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else if (isCurrentTrack) {
                            AudioWaveformVisualizer(
                                isPlaying = false,
                                barCount = 12,
                                activeColor = TextSecondary,
                                minHeight = 4.dp,
                                maxHeight = 18.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatSongDuration(song.duration),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onAddToPlaylist(song) }
                            )
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
            title = { Text("Rename Playlist", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onEditPlaylistName(newName.trim())
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save", color = RanjhaNeonPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = MidnightNavyCard
        )
    }
}

private fun formatSongDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

enum class PlaylistSortOrder(val displayName: String) {
    DATE_ADDED_NEWEST("Date Added (Newest)"),
    DATE_ADDED_OLDEST("Date Added (Oldest)"),
    ARTIST_A_Z("Artist (A-Z)"),
    ARTIST_Z_A("Artist (Z-A)"),
    SONG_A_Z("Song (A-Z)"),
    SONG_Z_A("Song (Z-A)"),
    DURATION_SHORTEST("Duration (Shortest)"),
    DURATION_LONGEST("Duration (Longest)")
}

enum class PlaylistViewMode {
    LIST,
    GRID,
    CARD
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistSongItem(
    song: SongEntity,
    viewMode: PlaylistViewMode = PlaylistViewMode.LIST,
    onSongClick: () -> Unit,
    onRemove: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    hapticFeedback: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    when (viewMode) {
        PlaylistViewMode.LIST -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp), fillAlpha = 0.22f)
                    .clickable(onClick = onSongClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.albumArtPath ?: android.R.drawable.ic_media_play,
                    contentDescription = null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = formatSongDuration(song.duration),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to Queue") },
                            leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onAddToQueue()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onAddToPlaylist()
                            }
                        )
                    }
                }
            }
        }
        PlaylistViewMode.GRID -> {
            Column(
                modifier = Modifier
                    .padding(6.dp)
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp), fillAlpha = 0.22f)
                    .clickable(onClick = onSongClick)
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = song.albumArtPath ?: android.R.drawable.ic_media_play,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = song.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        PlaylistViewMode.CARD -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(18.dp), fillAlpha = 0.25f)
                    .clickable(onClick = onSongClick)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.albumArtPath ?: android.R.drawable.ic_media_play,
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${song.artist} • ${song.album}",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onAddToQueue) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = RanjhaNeonPink,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
