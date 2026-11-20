package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.SongEntity
import com.example.data.remote.ApiOnlinePlaylistDetail
import com.example.ui.theme.*

@Composable
fun OnlinePlaylistScreen(
    playlistDetail: ApiOnlinePlaylistDetail?,
    songs: List<SongEntity>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSongClick: (SongEntity, List<SongEntity>) -> Unit,
    onAddToQueue: (SongEntity) -> Unit = {},
    onAddToPlaylist: (SongEntity) -> Unit = {},
    onAddOnlinePlaylistToPlaylist: (String, String) -> Unit = { _, _ -> },
    currentSongId: String? = null,
    isPlaying: Boolean = false,
    onTogglePlayPause: () -> Unit = {}
) {
    FluidBackdrop {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RanjhaNeonPink)
            }
        } else if (playlistDetail == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Could not load playlist", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Header
                item(key = "header") {
                    PlaylistHeader(
                        detail = playlistDetail,
                        onBack = onBack,
                        onAddToPlaylist = { onAddOnlinePlaylistToPlaylist(playlistDetail.id, playlistDetail.name) }
                    )
                }

                // Song list
                items(songs, key = { "song_${it.filePath}" }) { song ->
                    val isCurrentTrack = currentSongId != null && song.filePath == currentSongId
                    val isActivelyPlaying = isCurrentTrack && isPlaying
                    OnlinePlaylistSongGlassItem(
                        song = song,
                        isCurrentTrack = isCurrentTrack,
                        isActivelyPlaying = isActivelyPlaying,
                        onClick = {
                            if (isCurrentTrack) {
                                onTogglePlayPause()
                            } else {
                                onSongClick(song, songs)
                            }
                        },
                        onAddToQueue = { onAddToQueue(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistHeader(
    detail: ApiOnlinePlaylistDetail,
    onBack: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Background image
        val imageUrl = detail.image?.lastOrNull()?.url
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = detail.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MidnightNavy.copy(alpha = 0.7f),
                            MidnightNavy
                        ),
                        startY = 60f
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularGlassButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack,
                size = 42.dp,
                iconSize = 20.dp
            )

            Box {
                CircularGlassButton(
                    icon = Icons.Default.MoreVert,
                    onClick = { menuExpanded = true },
                    size = 42.dp,
                    iconSize = 20.dp
                )

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

        // Playlist info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = detail.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))

            val subtitle = buildString {
                detail.songCount?.let { append("$it songs") }
                detail.language?.takeIf { it.isNotBlank() }?.let { lang ->
                    if (isNotEmpty()) append(" • ")
                    append(lang.replaceFirstChar { it.uppercase() })
                }
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun OnlinePlaylistSongGlassItem(
    song: SongEntity,
    isCurrentTrack: Boolean = false,
    isActivelyPlaying: Boolean = false,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        fillAlpha = if (isCurrentTrack) 0.30f else 0.20f,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art
            AsyncImage(
                model = song.albumArtPath ?: android.R.drawable.ic_media_play,
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrentTrack) RanjhaNeonPink else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isActivelyPlaying) "Playing • ${song.artist}" else "${song.artist} • ${formatDuration(song.duration)}",
                    fontSize = 12.sp,
                    color = if (isActivelyPlaying) RanjhaCyan else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isActivelyPlaying) {
                AudioWaveformVisualizer(
                    isPlaying = true,
                    barCount = 10,
                    activeColor = RanjhaCyan,
                    minHeight = 3.dp,
                    maxHeight = 16.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else if (isCurrentTrack) {
                AudioWaveformVisualizer(
                    isPlaying = false,
                    barCount = 10,
                    activeColor = TextSecondary,
                    minHeight = 3.dp,
                    maxHeight = 16.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
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
