package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.SongEntity
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onSongClick: (SongEntity, List<SongEntity>) -> Unit,
    onPlaylistClick: (String, String) -> Unit = { _, _ -> },
    onArtistClick: (String, String) -> Unit = { _, _ -> },
    currentSongId: String? = null,
    isPlaying: Boolean = false,
    onTogglePlayPause: () -> Unit = {}
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val localSongs by viewModel.localSearchResults.collectAsStateWithLifecycle()
    val onlineSongs by viewModel.onlineSearchResults.collectAsStateWithLifecycle()
    val playlistResults by viewModel.playlistSearchResults.collectAsStateWithLifecycle()
    val artistResults by viewModel.artistSearchResults.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val categories = listOf("All", "Songs", "Artists", "Playlists")

    val keyboardController = LocalSoftwareKeyboardController.current

    val allSongs = remember(localSongs, onlineSongs) { localSongs + onlineSongs }

    // Parse Spotify-style recently played songs from SearchHistory Room Flow
    val recentPlayedSongs = remember(searchHistory) {
        searchHistory.mapNotNull { entry ->
            if (entry.type == "SONG_ITEM") {
                try {
                    val json = org.json.JSONObject(entry.name)
                    val song = SongEntity(
                        filePath = entry.referenceId,
                        title = json.optString("title", "Unknown Song"),
                        artist = json.optString("artist", "Unknown Artist"),
                        album = json.optString("album", "Unknown"),
                        albumArtPath = json.optString("albumArtPath", "").takeIf { it.isNotBlank() },
                        duration = json.optLong("duration", 0L),
                        isOnline = json.optBoolean("isOnline", true),
                        downloadUrl = json.optString("downloadUrl", "").takeIf { it.isNotBlank() },
                        isDownloaded = json.optBoolean("isDownloaded", false),
                        localDownloadPath = json.optString("localDownloadPath", "").takeIf { it.isNotBlank() }
                    )
                    entry.id to song
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    FluidBackdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ─────────────────────────────────────────
            // 1. Centered Header: "SONG SEARCH"
            // ─────────────────────────────────────────
            item(key = "search_header") {
                Text(
                    text = "SONG SEARCH",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 14.dp)
                )
            }

            // ─────────────────────────────────────────
            // 2. Hovering Search Bar with Microphone Icon
            // ─────────────────────────────────────────
            item(key = "hovering_search_bar") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(52.dp)
                        .glassmorphic(shape = RoundedCornerShape(26.dp), fillAlpha = 0.28f)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search songs, artists, playlists...",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(RanjhaNeonPink),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    keyboardController?.hide()
                                }),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.updateSearchQuery("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Microphone Icon
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = "Voice Search",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ─────────────────────────────────────────
            // 3. Category Filter Chips Row ("All", "Songs", "Artists", "Playlists")
            // ─────────────────────────────────────────
            item(key = "filter_chips_row") {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    itemsIndexed(categories) { index, category ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedTabIndex = index }
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                RanjhaNeonPurple,
                                                RanjhaNeonPink
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0x3528385A),
                                                Color(0x18192338)
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color.White.copy(alpha = 0.5f) else Color(0x30FFFFFF),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────
            // When NO query is typed:
            // Spotify-style Recent Searches (played songs) -> Browse All
            // ─────────────────────────────────────────
            if (query.isEmpty()) {
                // 4. "Recent Searches" — Directly playable songs (like Spotify)
                if (recentPlayedSongs.isNotEmpty()) {
                    item(key = "recent_searches_header") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Searches",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(
                                onClick = { viewModel.clearSearchHistory() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = "Clear all",
                                    fontSize = 12.sp,
                                    color = RanjhaNeonPink
                                )
                            }
                        }
                    }

                    val recentSongEntities = recentPlayedSongs.map { it.second }
                    items(
                        items = recentPlayedSongs,
                        key = { (historyId, _) -> "recent_song_$historyId" }
                    ) { (historyId, song) ->
                        val isCurrentTrack = currentSongId != null && song.filePath == currentSongId
                        val isActivelyPlaying = isCurrentTrack && isPlaying

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            fillAlpha = if (isCurrentTrack) 0.30f else 0.20f,
                            onClick = {
                                if (isCurrentTrack) {
                                    onTogglePlayPause()
                                } else {
                                    viewModel.saveRecentPlayedSearchSong(song)
                                    onSongClick(song, recentSongEntities)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.albumArtPath ?: android.R.drawable.ic_media_play,
                                    contentDescription = song.title,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCurrentTrack) RanjhaNeonPink else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isActivelyPlaying) "Playing • ${song.artist}" else "Song • ${song.artist}",
                                        fontSize = 12.sp,
                                        color = if (isActivelyPlaying) RanjhaCyan else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isActivelyPlaying) {
                                    AudioWaveformVisualizer(
                                        isPlaying = true,
                                        barCount = 8,
                                        activeColor = RanjhaCyan,
                                        minHeight = 3.dp,
                                        maxHeight = 14.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else if (isCurrentTrack) {
                                    AudioWaveformVisualizer(
                                        isPlaying = false,
                                        barCount = 8,
                                        activeColor = TextSecondary,
                                        minHeight = 3.dp,
                                        maxHeight = 14.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                IconButton(
                                    onClick = { viewModel.removeSearchHistoryItem(historyId) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove from recent searches",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. "Browse All" 2-Column Grid
                item(key = "browse_all_section") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Browse All",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    val browseCategories = listOf(
                        BrowseCategory("Punjabi Folk", Brush.linearGradient(listOf(Color(0xFF1E3C72), Color(0xFF2A5298))), "🪕"),
                        BrowseCategory("Romantic", Brush.linearGradient(listOf(Color(0xFFB01D6E), Color(0xFFE84393))), "💖"),
                        BrowseCategory("Lofi Beats", Brush.linearGradient(listOf(Color(0xFF5B247A), Color(0xFF1BCEDF))), "🎧"),
                        BrowseCategory("Retro & Old", Brush.linearGradient(listOf(Color(0xFFBA531B), Color(0xFF7B3F00))), "📻")
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (i in browseCategories.indices step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BrowseCategoryCard(
                                    category = browseCategories[i],
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.updateSearchQuery(browseCategories[i].title) }
                                )
                                if (i + 1 < browseCategories.size) {
                                    BrowseCategoryCard(
                                        category = browseCategories[i + 1],
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.updateSearchQuery(browseCategories[i + 1].title) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ─────────────────────────────────────────
                // When Query is Typed: Show Search Results
                // ─────────────────────────────────────────
                when (selectedTabIndex) {
                    0, 1 -> { // All or Songs
                        items(allSongs, key = { "sr_${it.filePath}" }) { song ->
                            val isCurrentTrack = currentSongId != null && song.filePath == currentSongId
                            val isActivelyPlaying = isCurrentTrack && isPlaying
                            SearchResultGlassItem(
                                song = song,
                                isCurrentTrack = isCurrentTrack,
                                isActivelyPlaying = isActivelyPlaying,
                                onClick = {
                                    if (isCurrentTrack) {
                                        onTogglePlayPause()
                                    } else {
                                        viewModel.saveRecentPlayedSearchSong(song)
                                        onSongClick(song, allSongs)
                                    }
                                },
                                onAddToQueue = { viewModel.addToQueue(song) }
                            )
                        }
                    }
                    2 -> { // Artists
                        items(artistResults, key = { "art_${it.id}" }) { artist ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                onClick = { onArtistClick(artist.id, artist.name) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = artist.image?.lastOrNull()?.url ?: android.R.drawable.ic_media_play,
                                        contentDescription = artist.name,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = artist.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    3 -> { // Playlists
                        items(playlistResults, key = { "pl_${it.id}" }) { playlist ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                onClick = { onPlaylistClick(playlist.id, playlist.name) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = playlist.image?.lastOrNull()?.url ?: android.R.drawable.ic_media_play,
                                        contentDescription = playlist.name,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = playlist.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class BrowseCategory(
    val title: String,
    val gradient: Brush,
    val emoji: String
)

@Composable
private fun BrowseCategoryCard(
    category: BrowseCategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .background(category.gradient)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0x50FFFFFF), Color(0x10FFFFFF))),
                RoundedCornerShape(18.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = category.emoji,
                fontSize = 24.sp
            )
        }
    }
}

@Composable
private fun SearchResultGlassItem(
    song: SongEntity,
    isCurrentTrack: Boolean = false,
    isActivelyPlaying: Boolean = false,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        fillAlpha = if (isCurrentTrack) 0.30f else 0.20f,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.albumArtPath ?: android.R.drawable.ic_media_play,
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrentTrack) RanjhaNeonPink else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isActivelyPlaying) "Playing • ${song.artist}" else song.artist,
                    fontSize = 12.sp,
                    color = if (isActivelyPlaying) RanjhaCyan else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isActivelyPlaying) {
                AudioWaveformVisualizer(
                    isPlaying = true,
                    barCount = 8,
                    activeColor = RanjhaCyan,
                    minHeight = 3.dp,
                    maxHeight = 14.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
            } else if (isCurrentTrack) {
                AudioWaveformVisualizer(
                    isPlaying = false,
                    barCount = 8,
                    activeColor = TextSecondary,
                    minHeight = 3.dp,
                    maxHeight = 14.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (isActivelyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isActivelyPlaying) "Pause" else "Play",
                    tint = if (isCurrentTrack) RanjhaNeonPink else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
