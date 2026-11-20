package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.SongEntity
import com.example.service.PlaybackState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    song: SongEntity,
    playbackState: PlaybackState,
    queueSongCount: Int,
    canUndo: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onClearQueue: () -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onGoToSong: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onInsertQueueItem: (Int, androidx.media3.common.MediaItem) -> Unit,
    onUndoDelete: () -> Unit,
    onRepeatQueueItem: (Int) -> Unit,
    currentLyrics: String? = null,
    recommendations: List<SongEntity> = emptyList(),
    onBack: () -> Unit,
    onAddToPlaylist: () -> Unit = {}
) {
    var showQueue by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    val effectiveDuration = if (playbackState.duration > 0) playbackState.duration else song.duration

    FluidBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ─────────────────────────────────────────
            // 1. Top Bar: Down Chevron, "Now Playing", 3-Dots Menu
            // ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minimize Chevron Down
                CircularGlassButton(
                    icon = Icons.Outlined.KeyboardArrowDown,
                    onClick = onBack,
                    size = 44.dp,
                    iconSize = 26.dp
                )

                Text(
                    text = "Now Playing",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                // 3-Dots Menu
                CircularGlassButton(
                    icon = Icons.Outlined.MoreHoriz,
                    onClick = { showQueue = true },
                    size = 44.dp,
                    iconSize = 22.dp,
                    badgeCount = queueSongCount
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─────────────────────────────────────────
            // 2. Center Hero: Circular Artwork with Glowing Glass Ring & Waveform Overlay
            // ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(270.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -50) onNext()
                            else if (dragAmount > 50) onPrevious()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Ambient Outer Glow Ring
                Box(
                    modifier = Modifier
                        .size(270.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0x3538E5FF),
                                    Color(0x181E3A8A),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Outer Glass Frame Ring
                Box(
                    modifier = Modifier
                        .size(255.dp)
                        .clip(CircleShape)
                        .background(Color(0x202B3C5A))
                        .border(
                            2.5.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x65FFFFFF),
                                    Color(0x2038E5FF),
                                    Color(0x10FFFFFF)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Circular Album Artwork
                    AsyncImage(
                        model = song.albumArtPath ?: android.R.drawable.ic_media_play,
                        contentDescription = "Album Art",
                        modifier = Modifier
                            .size(238.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    // Center Glass Overlay with Animated Waveform
                    Box(
                        modifier = Modifier
                            .size(238.dp)
                            .clip(CircleShape)
                            .background(Color(0x55090D1A)),
                        contentAlignment = Alignment.Center
                    ) {
                        AudioWaveformVisualizer(
                            isPlaying = playbackState.isPlaying,
                            barCount = 20,
                            activeColor = Color.White,
                            minHeight = 8.dp,
                            maxHeight = 42.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─────────────────────────────────────────
            // 3. Song Info & Favorite / Bookmark Actions
            // ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Heart Favorite Button
                CircularGlassButton(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    onClick = {
                        isFavorite = !isFavorite
                        onAddToPlaylist()
                    },
                    tint = if (isFavorite) RanjhaNeonPink else Color.White,
                    size = 46.dp,
                    iconSize = 22.dp
                )

                // Centered Title & Artist
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = song.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bookmark / Queue Button
                CircularGlassButton(
                    icon = Icons.Outlined.BookmarkBorder,
                    onClick = { showQueue = true },
                    size = 46.dp,
                    iconSize = 22.dp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─────────────────────────────────────────
            // 4. Glowing Progress Bar / Slider & Timestamps
            // ─────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth()) {
                var isDraggingSlider by remember { mutableStateOf(false) }
                var dragSliderFraction by remember { mutableFloatStateOf(0f) }

                val currentFraction = if (effectiveDuration > 0) {
                    (playbackState.currentPosition.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
                } else 0f

                val displayedFraction = if (isDraggingSlider) dragSliderFraction else currentFraction
                val displayedPositionMs = if (isDraggingSlider) {
                    (dragSliderFraction * effectiveDuration).toLong()
                } else {
                    playbackState.currentPosition
                }

                Slider(
                    value = displayedFraction,
                    onValueChange = { frac ->
                        isDraggingSlider = true
                        dragSliderFraction = frac
                    },
                    onValueChangeFinished = {
                        val targetMs = (dragSliderFraction * effectiveDuration).toLong()
                        onSeek(targetMs)
                        isDraggingSlider = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = RanjhaNeonPink,
                        inactiveTrackColor = Color(0x35384F75)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatPlayerTime(displayedPositionMs),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = formatPlayerTime(effectiveDuration),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ─────────────────────────────────────────
            // 5. Playback Controls Row (Shuffle, Prev, Big Play/Pause, Next, Repeat)
            // ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = onShuffleToggle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.shuffleModeEnabled) RanjhaNeonPink else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Big Circular Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPlayPause)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF4A6591),
                                    Color(0xFF223555)
                                )
                            ),
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x80FFFFFF),
                                    Color(0x2038E5FF)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Repeat
                IconButton(onClick = onRepeatToggle) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (playbackState.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) RanjhaNeonPink else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─────────────────────────────────────────
            // 6. Bottom Wide Glass Pill Button: "View Lyrics"
            // ─────────────────────────────────────────
            GlassButton(
                text = "View Lyrics",
                onClick = { showLyricsSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
        }
    }

    // Lyrics Bottom Sheet
    if (showLyricsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLyricsSheet = false },
            containerColor = MidnightNavyCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Lyrics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = currentLyrics ?: "Lyrics not available for this track.",
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Queue Bottom Sheet
    if (showQueue) {
        QueueBottomSheet(
            playbackState = playbackState,
            canUndo = canUndo,
            onDismiss = { showQueue = false },
            onClearQueue = onClearQueue,
            onRemoveFromQueue = onRemoveFromQueue,
            onGoToSong = onGoToSong,
            onMoveQueueItem = onMoveQueueItem,
            onInsertQueueItem = onInsertQueueItem,
            onUndoDelete = onUndoDelete,
            onRepeatQueueItem = onRepeatQueueItem,
            onShuffleToggle = onShuffleToggle
        )
    }
}

private fun formatPlayerTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    playbackState: PlaybackState,
    canUndo: Boolean,
    onDismiss: () -> Unit,
    onClearQueue: () -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onGoToSong: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onInsertQueueItem: (Int, androidx.media3.common.MediaItem) -> Unit,
    onUndoDelete: () -> Unit,
    onRepeatQueueItem: (Int) -> Unit,
    onShuffleToggle: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MidnightNavyCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playing Queue (${playbackState.queue.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canUndo) {
                        TextButton(onClick = onUndoDelete) {
                            Text("Undo", color = RanjhaCyan)
                        }
                    }
                    TextButton(onClick = onClearQueue) {
                        Text("Clear", color = ErrorRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playbackState.queue.size) { index ->
                    val mediaItem = playbackState.queue[index]
                    val isCurrent = index == playbackState.currentQueueIndex

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        fillAlpha = if (isCurrent) 0.35f else 0.15f,
                        onClick = { onGoToSong(index) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) RanjhaNeonPink else TextSecondary,
                                modifier = Modifier.width(24.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mediaItem.mediaMetadata.title?.toString() ?: "Unknown Song",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCurrent) RanjhaNeonPink else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isCurrent) {
                                AudioWaveformVisualizer(
                                    isPlaying = playbackState.isPlaying,
                                    barCount = 10,
                                    activeColor = RanjhaCyan,
                                    minHeight = 3.dp,
                                    maxHeight = 14.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            IconButton(
                                onClick = { onRemoveFromQueue(index) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
