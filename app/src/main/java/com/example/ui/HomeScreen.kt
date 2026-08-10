package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.data.local.SongEntity
import com.example.service.PlaybackState
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    // Discover section data
    featuredPlaylists: List<com.example.data.remote.ApiPlaylistItem> = emptyList(),
    trendingSongs: List<SongEntity> = emptyList(),
    homeLoading: Boolean = false,
    homeError: String? = null,
    onRetryHomeLoad: () -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onTrendingSongClick: (SongEntity, List<SongEntity>) -> Unit = { _, _ -> },
    onAddToQueue: (SongEntity) -> Unit = {},
    onAddToPlaylist: (SongEntity) -> Unit = {},
    onAddOnlinePlaylistToPlaylist: (String, String) -> Unit = { _, _ -> }
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DiscoverSection(
            featuredPlaylists = featuredPlaylists,
            trendingSongs = trendingSongs,
            isLoading = homeLoading,
            error = homeError,
            onRetry = onRetryHomeLoad,
            onPlaylistClick = onPlaylistClick,
            onSongClick = onTrendingSongClick,
            onAddToQueue = onAddToQueue,
            onAddToPlaylist = onAddToPlaylist,
            onAddOnlinePlaylistToPlaylist = onAddOnlinePlaylistToPlaylist
        )
    }
}

@Composable
private fun DiscoverSection(
    featuredPlaylists: List<com.example.data.remote.ApiPlaylistItem>,
    trendingSongs: List<SongEntity>,
    isLoading: Boolean,
    error: String? = null,
    onRetry: () -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSongClick: (SongEntity, List<SongEntity>) -> Unit,
    onAddToQueue: (SongEntity) -> Unit,
    onAddToPlaylist: (SongEntity) -> Unit,
    onAddOnlinePlaylistToPlaylist: (String, String) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PulseAccent)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading music for you...", color = TextSecondary)
            }
        }
        return
    }

    if (error != null && featuredPlaylists.isEmpty() && trendingSongs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = TextTertiary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(error, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = PulseAccent)
                ) {
                    Text("Retry", color = Color.Black)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Header
        item(key = "discover_header") {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "🎵 Discover",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Find your next favorite song",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Featured Playlists Section
        if (featuredPlaylists.isNotEmpty()) {
            item(key = "playlist_header") {
                Text(
                    text = "Featured Playlists",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            item(key = "playlist_row") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(
                        count = featuredPlaylists.size,
                        key = { "fp_${featuredPlaylists[it].id}" }
                    ) { index ->
                        val playlist = featuredPlaylists[index]
                        FeaturedPlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.id) },
                            onAddToPlaylist = { onAddOnlinePlaylistToPlaylist(playlist.id, playlist.name) }
                        )
                    }
                }
            }

            item(key = "playlist_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Trending Songs Section
        if (trendingSongs.isNotEmpty()) {
            item(key = "trending_header") {
                Text(
                    text = "Trending Songs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            items(
                count = trendingSongs.size,
                key = { "ts_${trendingSongs[it].filePath}" }
            ) { index ->
                val song = trendingSongs[index]
                TrendingSongItem(
                    song = song,
                    rank = index + 1,
                    onClick = { onSongClick(song, trendingSongs) },
                    onAddToQueue = { onAddToQueue(song) },
                    onAddToPlaylist = { onAddToPlaylist(song) }
                )
            }
        }
    }
}

@Composable
private fun FeaturedPlaylistCard(
    playlist: com.example.data.remote.ApiPlaylistItem,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                AsyncImage(
                    model = playlist.image?.lastOrNull()?.url ?: android.R.drawable.ic_media_play,
                    contentDescription = playlist.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
                // Song count badge
                playlist.songCount?.let { count ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = DarkBackground.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "$count songs",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Save to Playlist") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendingSongItem(
    song: SongEntity,
    rank: Int,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onAddToQueue()
            }
            false
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank number
            Text(
                text = "$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (rank <= 3) PulseAccent else TextTertiary,
                modifier = Modifier.width(32.dp)
            )

            // Album art
            AsyncImage(
                model = song.albumArtPath ?: android.R.drawable.ic_media_play,
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceContainer),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${formatDuration(song.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = PulseAccent.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
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
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
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

