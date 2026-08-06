package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.SongEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onSongClick: (SongEntity, List<SongEntity>) -> Unit
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val localSongs by viewModel.localSearchResults.collectAsStateWithLifecycle()
    val onlineSongs by viewModel.onlineSearchResults.collectAsStateWithLifecycle()
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Local", "Online", "Artists", "Albums")

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

            val displayList = when (selectedTabIndex) {
                0 -> localSongs + onlineSongs // All
                1 -> localSongs
                2 -> onlineSongs
                3 -> { // Artists — group by artist, show one song per artist
                    val allSongs = localSongs + onlineSongs
                    allSongs.distinctBy { it.artist.lowercase() }
                }
                4 -> { // Albums — group by album, show one song per album
                    val allSongs = localSongs + onlineSongs
                    allSongs.distinctBy { it.album.lowercase() }
                }
                else -> emptyList()
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(displayList, key = { it.filePath }) { song ->
                    SearchResultCard(
                        song = song, 
                        onClick = { onSongClick(song, displayList) },
                        onDownload = { viewModel.downloadSong(song) }
                    )
                }
            }
        } else {
            // Default View when query is empty
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Search for songs, artists, or albums")
            }
        }
    }
}

@Composable
fun SearchResultCard(
    song: SongEntity,
    onClick: () -> Unit,
    onDownload: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.albumArtPath ?: android.R.drawable.ic_media_play,
            contentDescription = "Album Art",
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
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
                // Local/Online Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (song.isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = if (song.isOnline) "ONLINE" else "LOCAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (song.isOnline) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSecondary,
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
        if (song.isOnline) {
            IconButton(
                onClick = { if (!song.isDownloaded) onDownload() },
                enabled = !song.isDownloaded
            ) {
                Icon(
                    imageVector = if (song.isDownloaded) Icons.Filled.Check else Icons.Filled.Download,
                    contentDescription = if (song.isDownloaded) "Downloaded" else "Download",
                    tint = if (song.isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Quick Play",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
