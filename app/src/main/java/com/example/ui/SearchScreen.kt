package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.data.local.PlaylistEntity
import com.example.data.local.SongEntity
import com.example.data.remote.ApiArtistSearchItem
import com.example.data.remote.ApiPlaylistItem
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onSongClick: (SongEntity, List<SongEntity>) -> Unit,
    onPlaylistClick: (String, String) -> Unit = { _, _ -> },
    onArtistClick: (String, String) -> Unit = { _, _ -> }
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val localSongs by viewModel.localSearchResults.collectAsStateWithLifecycle()
    val onlineSongs by viewModel.onlineSearchResults.collectAsStateWithLifecycle()
    val playlistResults by viewModel.playlistSearchResults.collectAsStateWithLifecycle()
    val artistResults by viewModel.artistSearchResults.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Songs", "Playlists", "Artists")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Bar
        SearchBar(
            query = query,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onSearch = { viewModel.updateSearchQuery(it) },
            active = false,
            onActiveChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("What do you want to listen to?") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            }
        ) {}

        if (query.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.background,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> { // ALL — show top artist, playlists, then songs
                        // Top Artist Card
                        if (artistResults.isNotEmpty()) {
                            item(key = "top_artist_header") {
                                Text(
                                    text = "Top Artist",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                            item(key = "top_artist_card") {
                                ArtistSearchCard(
                                    artist = artistResults.first(),
                                    onClick = { onArtistClick(artistResults.first().id, artistResults.first().name ?: "Unknown Artist") }
                                )
                            }
                        }

                        // Playlist Results
                        if (playlistResults.isNotEmpty()) {
                            item(key = "playlists_header") {
                                Text(
                                    text = "Playlists",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                            item(key = "playlists_row") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(playlistResults, key = { "pl_${it.id}" }) { playlist ->
                                        PlaylistSearchCard(
                                            playlist = playlist,
                                            onClick = { onPlaylistClick(playlist.id, playlist.name) }
                                        )
                                    }
                                }
                            }
                        }

                        // Songs
                        val allSongsList = localSongs + onlineSongs
                        if (allSongsList.isNotEmpty()) {
                            item(key = "songs_header") {
                                Text(
                                    text = "Songs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                            items(allSongsList, key = { "song_${it.filePath}" }) { song ->
                                SearchResultCard(
                                    song = song,
                                    playlists = allPlaylists,
                                    onClick = { onSongClick(song, allSongsList) },
                                    onDownload = { viewModel.downloadSong(song) },
                                    onAddToQueue = {
                                        if (song.isOnline) viewModel.addOnlineSongToQueue(song)
                                        else viewModel.addToQueue(song)
                                        Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                                    },
                                    onAddToPlaylist = { viewModel.openAddToPlaylist(AddToPlaylistTarget.Song(song)) }
                                )
                            }
                        }
                    }
                    1 -> { // SONGS
                        val songsList = localSongs + onlineSongs
                        items(songsList, key = { "songs_tab_${it.filePath}" }) { song ->
                            SearchResultCard(
                                song = song,
                                playlists = allPlaylists,
                                onClick = { onSongClick(song, songsList) },
                                onDownload = { viewModel.downloadSong(song) },
                                onAddToQueue = {
                                    if (song.isOnline) viewModel.addOnlineSongToQueue(song)
                                    else viewModel.addToQueue(song)
                                    Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                                },
                                onAddToPlaylist = { viewModel.openAddToPlaylist(AddToPlaylistTarget.Song(song)) }
                            )
                        }
                    }
                    2 -> { // PLAYLISTS
                        items(playlistResults, key = { "pl_tab_${it.id}" }) { playlist ->
                            PlaylistSearchListItem(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist.id, playlist.name) }
                            )
                        }
                        if (playlistResults.isEmpty()) {
                            item(key = "no_playlists") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No playlists found", color = TextSecondary)
                                }
                            }
                        }
                    }
                    3 -> { // ARTISTS
                        items(artistResults, key = { "art_tab_${it.id}" }) { artist ->
                            ArtistSearchCard(
                                artist = artist,
                                onClick = { onArtistClick(artist.id, artist.name ?: "Unknown Artist") }
                            )
                        }
                        if (artistResults.isEmpty()) {
                            item(key = "no_artists") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No artists found", color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Default View when query is empty: Search History
            val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
            if (searchHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Search for songs, artists, or playlists", color = TextSecondary)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(onClick = { viewModel.clearSearchHistory() }) {
                            Text("Clear All", color = PulseAccent)
                        }
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(searchHistory, key = { it.id }) { history ->
                            SearchHistoryItem(
                                history = history,
                                onClick = {
                                    when (history.type) {
                                        "SONG" -> viewModel.playHistorySong(history.referenceId, history.name)
                                        "PLAYLIST" -> onPlaylistClick(history.referenceId, history.name)
                                        "ARTIST" -> onArtistClick(history.referenceId, history.name)
                                    }
                                },
                                onDelete = { viewModel.removeSearchHistoryItem(history.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Artist Search Card
// ═══════════════════════════════════════════════════════

@Composable
fun ArtistSearchCard(
    artist: ApiArtistSearchItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageUrl = artist.image?.lastOrNull()?.url
        if (imageUrl != null && !imageUrl.contains("default")) {
            AsyncImage(
                model = imageUrl,
                contentDescription = artist.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceContainer),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist.role ?: "Artist",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "View Artist",
            tint = PulseAccent,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════
// Playlist Search Card (Horizontal Row)
// ═══════════════════════════════════════════════════════

@Composable
fun PlaylistSearchCard(
    playlist: ApiPlaylistItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            AsyncImage(
                model = playlist.image?.lastOrNull()?.url ?: android.R.drawable.ic_media_play,
                contentDescription = playlist.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                playlist.songCount?.let { count ->
                    Text(
                        text = "$count songs",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Playlist Search List Item (for Playlists tab)
// ═══════════════════════════════════════════════════════

@Composable
fun PlaylistSearchListItem(
    playlist: ApiPlaylistItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playlist.image?.lastOrNull()?.url ?: android.R.drawable.ic_media_play,
            contentDescription = playlist.name,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurfaceContainer),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row {
                playlist.songCount?.let { count ->
                    Text(
                        text = "$count songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                playlist.language?.takeIf { it.isNotBlank() }?.let { lang ->
                    Text(
                        text = " • ${lang.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Song Search Result Card (existing, preserved)
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultCard(
    song: SongEntity,
    playlists: List<PlaylistEntity> = emptyList(),
    onClick: () -> Unit,
    onDownload: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {}
) {
    // State for the overflow menu
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onAddToQueue()
            }
            false // Always bounce back
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
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
                    Icons.Default.QueueMusic,
                    contentDescription = "Add to Queue",
                    tint = PulseAccent
                )
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Album art
        AsyncImage(
            model = song.albumArtPath ?: android.R.drawable.ic_media_play,
            contentDescription = "Album Art",
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Local / Online badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (song.isOnline) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = if (song.isOnline) "ONLINE" else "LOCAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (song.isOnline) MaterialTheme.colorScheme.onTertiary
                                else MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = song.artist,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Download button — only for online songs not yet downloaded
        if (song.isOnline) {
            IconButton(
                onClick = { if (!song.isDownloaded) onDownload() },
                enabled = !song.isDownloaded
            ) {
                Icon(
                    imageVector = if (song.isDownloaded) Icons.Filled.Check else Icons.Filled.Download,
                    contentDescription = if (song.isDownloaded) "Downloaded" else "Download",
                    tint = if (song.isDownloaded) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Quick Play button
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Quick Play",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // ··· More options menu (Add to Queue / Add to Playlist)
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                // ── Add to Queue ──
                DropdownMenuItem(
                    text = { Text("Add to Queue") },
                    leadingIcon = {
                        Icon(Icons.Default.QueueMusic, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onAddToQueue()
                    }
                )

                // ── Add to Playlist ──
                DropdownMenuItem(
                    text = { Text("Add to Playlist…") },
                    leadingIcon = {
                        Icon(Icons.Default.Add, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onAddToPlaylist()
                    }
                )
            }
        }
    }
}
}

// ═══════════════════════════════════════════════════════
// Search History Item
// ═══════════════════════════════════════════════════════

@Composable
fun SearchHistoryItem(
    history: com.example.data.local.SearchHistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = "History",
            tint = TextTertiary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = history.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = history.type.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
