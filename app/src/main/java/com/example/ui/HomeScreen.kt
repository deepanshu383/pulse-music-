package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
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
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    featuredPlaylists: List<com.example.data.remote.ApiPlaylistItem> = emptyList(),
    trendingSongs: List<SongEntity> = emptyList(),
    homeLoading: Boolean = false,
    homeError: String? = null,
    onRetryHomeLoad: () -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onTrendingSongClick: (SongEntity, List<SongEntity>) -> Unit = { _, _ -> },
    onAddToQueue: (SongEntity) -> Unit = {},
    onAddToPlaylist: (SongEntity) -> Unit = {},
    onAddOnlinePlaylistToPlaylist: (String, String) -> Unit = { _, _ -> },
    currentSongId: String? = null,
    isPlaying: Boolean = false,
    onTogglePlayPause: () -> Unit = {}
) {
    FluidBackdrop {
        if (homeLoading && featuredPlaylists.isEmpty() && trendingSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = RanjhaNeonPink)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading music for you...", color = TextSecondary)
                }
            }
            return@FluidBackdrop
        }

        val greeting = remember {
            when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> "Good Morning,"
                in 12..16 -> "Good Afternoon,"
                else -> "Good Evening,"
            }
        }

        // Current Firebase User Name or default "Samar" as in mockup
        val userName = remember {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName
                ?.split(" ")?.firstOrNull() ?: "Samar"
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ─────────────────────────────────────────
            // 1. Top Header: Greeting + Notification Bell with Red Dot
            // ─────────────────────────────────────────
            item(key = "header_greeting") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 24.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = greeting,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = userName,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Notification Bell with Red Badge Dot
                    CircularGlassButton(
                        icon = Icons.Outlined.Notifications,
                        onClick = { /* Open notifications */ },
                        size = 46.dp,
                        iconSize = 22.dp,
                        hasRedDot = true
                    )
                }
            }

            // ─────────────────────────────────────────
            // 2. Hero Card: "Trending Now" (Hovering Glass Card)
            // ─────────────────────────────────────────
            item(key = "hero_trending_now") {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    fillAlpha = 0.28f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Trending Now",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mood: Soulful & Romantic",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Embossed Glass "Play Now" Button
                        GlassButton(
                            text = "Play Now",
                            onClick = {
                                if (trendingSongs.isNotEmpty()) {
                                    onTrendingSongClick(trendingSongs.first(), trendingSongs)
                                }
                            },
                            highlight = false,
                            modifier = Modifier.width(130.dp)
                        )
                    }
                }
            }

            // ─────────────────────────────────────────
            // 3. Section: "Play For You" (Horizontal Glass Cards)
            // ─────────────────────────────────────────
            item(key = "play_for_you_header") {
                Text(
                    text = "Play For You",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }

            item(key = "play_for_you_row") {
                // Curated fallback cards matching mockup if featured list is loading
                val displayPlaylists = if (featuredPlaylists.isNotEmpty()) {
                    featuredPlaylists
                } else {
                    listOf(
                        com.example.data.remote.ApiPlaylistItem(
                            id = "soulful_hits",
                            name = "Soulful Hits",
                            songCount = 24,
                            image = emptyList()
                        ),
                        com.example.data.remote.ApiPlaylistItem(
                            id = "modern_punjabi",
                            name = "Modern Punjabi",
                            songCount = 30,
                            image = emptyList()
                        ),
                        com.example.data.remote.ApiPlaylistItem(
                            id = "romantic_hits",
                            name = "Romantic Hits",
                            songCount = 18,
                            image = emptyList()
                        )
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(
                        count = displayPlaylists.size,
                        key = { "pfy_${displayPlaylists[it].id}_$it" }
                    ) { index ->
                        val item = displayPlaylists[index]
                        PlayForYouCard(
                            item = item,
                            index = index,
                            onClick = { onPlaylistClick(item.id) }
                        )
                    }
                }
            }

            // ─────────────────────────────────────────
            // 4. Section: "Trending Songs"
            // ─────────────────────────────────────────
            if (trendingSongs.isNotEmpty()) {
                item(key = "trending_songs_header") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trending Songs",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(
                            onClick = onRetryHomeLoad,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Trending",
                                tint = RanjhaCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = trendingSongs,
                    key = { _, song -> "ts_${song.filePath}" }
                ) { index, song ->
                    val isCurrentTrack = currentSongId != null && song.filePath == currentSongId
                    val isActivelyPlaying = isCurrentTrack && isPlaying
                    TrendingSongGlassItem(
                        song = song,
                        rank = index + 1,
                        isCurrentTrack = isCurrentTrack,
                        isActivelyPlaying = isActivelyPlaying,
                        onClick = {
                            if (isCurrentTrack) {
                                onTogglePlayPause()
                            } else {
                                onTrendingSongClick(song, trendingSongs)
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

private val PlayForYouFallbackGradients = listOf(
    Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF000000))),
    Brush.verticalGradient(listOf(Color(0xFFF39C12), Color(0xFFE67E22))),
    Brush.verticalGradient(listOf(Color(0xFF8E44AD), Color(0xFF2C3E50)))
)

/**
 * Card for "Play For You" section with rounded glass frame and bottom overlay
 */
@Composable
private fun PlayForYouCard(
    item: com.example.data.remote.ApiPlaylistItem,
    index: Int,
    onClick: () -> Unit
) {
    val cardGradient = PlayForYouFallbackGradients[index % PlayForYouFallbackGradients.size]

    Column(
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick)
    ) {
        // Thumbnail with Glass Border
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(cardGradient)
                .glassmorphic(shape = RoundedCornerShape(20.dp), fillAlpha = 0.15f)
        ) {
            val imageUrl = item.image?.lastOrNull()?.url
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Aesthetic icon placeholder
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }

            // Bottom glass pill overlay label inside the card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0x990A0E1A))
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label below card
        Text(
            text = item.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Glassmorphic row item for trending songs
 */
@Composable
private fun TrendingSongGlassItem(
    song: SongEntity,
    rank: Int,
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
        fillAlpha = if (isCurrentTrack) 0.30f else 0.18f,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = "$rank",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentTrack || rank <= 3) RanjhaNeonPink else TextSecondary,
                modifier = Modifier.width(26.dp)
            )

            // Album art
            AsyncImage(
                model = song.albumArtPath ?: android.R.drawable.ic_media_play,
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist
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

            // Quick Play/Pause Button
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isActivelyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isActivelyPlaying) "Pause" else "Play",
                    tint = if (isCurrentTrack) RanjhaNeonPink else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // More Options
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
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
