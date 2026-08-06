package com.example.ui

import androidx.compose.animation.animateColorAsState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    song: SongEntity?,
    playbackState: PlaybackState,
    queueSongCount: Int,
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
    onRepeatQueueItem: (Int) -> Unit,
    currentLyrics: String? = null,
    recommendations: List<SongEntity> = emptyList()
) {
    var showQueue by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (song == null) {
            // Empty state
            EmptyHomeState()
        } else {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp)
            ) {
                // Top Player Section
                Column(
                    modifier = Modifier.height(screenHeight - 160.dp)
                ) {
                    // Header
                    HomeHeader(queueSongCount = queueSongCount)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hero Artwork with swipe gestures
                    HeroArtwork(
                        song = song,
                        onSwipeLeft = onNext,
                        onSwipeRight = onPrevious,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Song Info
                    SongInfoSection(song = song)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Bar (Bug A5 fix: use live duration from ExoPlayer)
                    ProgressSection(
                        playbackState = playbackState,
                        duration = if (playbackState.duration > 0) playbackState.duration else song.duration,
                        onSeek = onSeek
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Playback Controls
                    PlaybackControls(
                        playbackState = playbackState,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onShuffleToggle = onShuffleToggle,
                        onRepeatToggle = onRepeatToggle
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Swipe up indicator
                    SwipeUpIndicator(onClick = { showQueue = true })

                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Lyrics Section
                if (currentLyrics != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    LyricsCard(lyrics = currentLyrics)
                }
                
                // Recommendations Section
                if (recommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    RecommendationsSection(recommendations = recommendations)
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // Queue Bottom Sheet
    if (showQueue) {
        QueueBottomSheet(
            playbackState = playbackState,
            onDismiss = { showQueue = false },
            onClearQueue = onClearQueue,
            onRemoveFromQueue = onRemoveFromQueue,
            onGoToSong = onGoToSong,
            onMoveQueueItem = onMoveQueueItem,
            onRepeatQueueItem = onRepeatQueueItem,
            onShuffleToggle = onShuffleToggle
        )
    }
}

@Composable
private fun EmptyHomeState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TextTertiary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Ready to Play",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Browse your library to start listening",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun HomeHeader(queueSongCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "🎵 Discover",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        if (queueSongCount > 0) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PulseAccent.copy(alpha = 0.15f),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Queue: $queueSongCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = PulseAccent,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroArtwork(
    song: SongEntity,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    var swipeOffset by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "swipeOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffset > 100f) {
                            onSwipeRight()
                        } else if (swipeOffset < -100f) {
                            onSwipeLeft()
                        }
                        swipeOffset = 0f
                    },
                    onDragCancel = { swipeOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset += dragAmount
                    }
                )
            }
    ) {
        if (song.albumArtPath != null) {
            AsyncImage(
                model = song.albumArtPath,
                contentDescription = "Album Art",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = animatedOffset },
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                DarkSurfaceContainerHigh,
                                DarkSurfaceContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(96.dp)
                )
            }
        }
        // Bottom gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DarkBackground.copy(alpha = 0.6f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun SongInfoSection(song: SongEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${song.artist} • ${formatDuration(song.duration)}",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProgressSection(
    playbackState: PlaybackState,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }

    val displayProgress = if (isDragging) dragProgress else {
        if (duration > 0) (playbackState.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    }

    Slider(
        value = displayProgress,
        onValueChange = { newValue ->
            isDragging = true
            dragProgress = newValue
        },
        onValueChangeFinished = {
            isDragging = false
            onSeek((dragProgress * duration).toLong())
        },
        colors = SliderDefaults.colors(
            thumbColor = PulseAccent,
            activeTrackColor = PulseAccent,
            inactiveTrackColor = DarkSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )

    val displayPosition = if (isDragging) (dragProgress * duration).toLong() else playbackState.currentPosition

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            formatDuration(displayPosition),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            formatDuration(duration),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun PlaybackControls(
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        IconButton(onClick = onShuffleToggle) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (playbackState.shuffleModeEnabled) PulseAccent else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Previous
        IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }

        // Play/Pause
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(PulseAccent)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.Black,
                modifier = Modifier.size(36.dp)
            )
        }

        // Next
        IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }

        // Repeat
        IconButton(onClick = onRepeatToggle) {
            val icon = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
            val tint = if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) PulseAccent else TextSecondary
            Icon(icon, contentDescription = "Repeat", tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SwipeUpIndicator(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Queue & Shuffle",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}

// =====================================================================
// Queue Bottom Sheet (extracted/reusable)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QueueBottomSheet(
    playbackState: PlaybackState,
    onDismiss: () -> Unit,
    onClearQueue: () -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onGoToSong: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRepeatQueueItem: (Int) -> Unit,
    onShuffleToggle: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var activeDraggedIndex by remember { mutableStateOf<Int?>(null) }
    var activeDragOffsetY by remember { mutableStateOf(0f) }

    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TextTertiary)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp)
        ) {
            // Title
            Text(
                text = "Queue & Shuffle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val startIndex = if (playbackState.currentQueueIndex != -1) playbackState.currentQueueIndex + 1 else 0
                val queueCount = if (startIndex <= playbackState.queue.size) playbackState.queue.size - startIndex else 0

                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Queue ($queueCount)") },
                    leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PulseAccent.copy(alpha = 0.2f),
                        selectedLabelColor = PulseAccent,
                        selectedLeadingIconColor = PulseAccent
                    )
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onShuffleToggle()
                    },
                    label = { Text(if (playbackState.shuffleModeEnabled) "Shuffle ON" else "Shuffle OFF") },
                    leadingIcon = { Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PulseAccent.copy(alpha = 0.2f),
                        selectedLabelColor = PulseAccent,
                        selectedLeadingIconColor = PulseAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Currently Playing
            val currentSongItem = playbackState.queue.getOrNull(playbackState.currentQueueIndex)
            if (currentSongItem != null) {
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PulseAccent.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = currentSongItem.mediaMetadata.artworkUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSongItem.mediaMetadata.title?.toString() ?: "Unknown",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PulseAccent,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSongItem.mediaMetadata.artist?.toString() ?: "Unknown",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = PulseAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Queue list
            val startIndex = if (playbackState.currentQueueIndex != -1) playbackState.currentQueueIndex + 1 else 0
            val queueItems = if (startIndex <= playbackState.queue.size) playbackState.queue.subList(startIndex, playbackState.queue.size) else emptyList()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Up Next (${queueItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (queueItems.isNotEmpty()) {
                        Text(
                            text = "Long press to reorder",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
                if (queueItems.isNotEmpty()) {
                    TextButton(onClick = onClearQueue) {
                        Text("Clear All", color = PulseAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (queueItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Queue is empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Swipe right on any song to add it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    itemsIndexed(
                        queueItems,
                        key = { index, item -> item.mediaId.ifEmpty { "${item.mediaMetadata.title}_$index" } }
                    ) { localIndex, item ->
                        val globalIndex = startIndex + localIndex
                        val isBeingDragged = activeDraggedIndex == localIndex
                        val itemTranslationY = if (isBeingDragged) activeDragOffsetY else 0f
                        val itemZIndex = if (isBeingDragged) 10f else 0f
                        val itemScale = if (isBeingDragged) 1.04f else 1f

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    onRemoveFromQueue(globalIndex)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            modifier = Modifier.animateItem(),
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ErrorRed.copy(alpha = 0.15f))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = ErrorRed
                                    )
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(itemZIndex)
                                    .graphicsLayer {
                                        translationY = itemTranslationY
                                        scaleX = itemScale
                                        scaleY = itemScale
                                        shadowElevation = if (isBeingDragged) 12f else 0f
                                    }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isBeingDragged) DarkSurfaceContainerHighest else Color.Transparent
                                    )
                                    .pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                activeDraggedIndex = localIndex
                                                activeDragOffsetY = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                var currIndex = activeDraggedIndex
                                                    ?: return@detectDragGesturesAfterLongPress
                                                activeDragOffsetY += dragAmount.y

                                                val visibleItems = listState.layoutInfo.visibleItemsInfo
                                                val itemInfo =
                                                    visibleItems.firstOrNull { it.index == currIndex }
                                                val itemHeight = itemInfo?.size?.toFloat() ?: 140f

                                                while (activeDragOffsetY > itemHeight / 2f && currIndex < queueItems.size - 1) {
                                                    val nextIndex = currIndex + 1
                                                    onMoveQueueItem(
                                                        startIndex + currIndex,
                                                        startIndex + nextIndex
                                                    )
                                                    currIndex = nextIndex
                                                    activeDraggedIndex = currIndex
                                                    activeDragOffsetY -= itemHeight
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }

                                                while (activeDragOffsetY < -itemHeight / 2f && currIndex > 0) {
                                                    val prevIndex = currIndex - 1
                                                    onMoveQueueItem(
                                                        startIndex + currIndex,
                                                        startIndex + prevIndex
                                                    )
                                                    currIndex = prevIndex
                                                    activeDraggedIndex = currIndex
                                                    activeDragOffsetY += itemHeight
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }

                                                if (itemInfo != null) {
                                                    val currentTop =
                                                        itemInfo.offset + activeDragOffsetY
                                                    if (currentTop < 50f && listState.firstVisibleItemIndex > 0) {
                                                        coroutineScope.launch {
                                                            listState.scrollBy(-30f)
                                                        }
                                                    } else if (currentTop > listState.layoutInfo.viewportEndOffset - 150f) {
                                                        coroutineScope.launch {
                                                            listState.scrollBy(30f)
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                activeDraggedIndex = null
                                                activeDragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                activeDraggedIndex = null
                                                activeDragOffsetY = 0f
                                            }
                                        )
                                    }
                                    .clickable { onGoToSong(globalIndex) }
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${localIndex + 1}",
                                    color = if (isBeingDragged) PulseAccent else TextTertiary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isBeingDragged) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.width(24.dp)
                                )
                                AsyncImage(
                                    model = item.mediaMetadata.artworkUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.mediaMetadata.title?.toString() ?: "Unknown",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isBeingDragged) PulseAccent else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isBeingDragged) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.mediaMetadata.artist?.toString() ?: "Unknown",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { onRemoveFromQueue(globalIndex) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = TextTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = if (isBeingDragged) PulseAccent else TextTertiary,
                                    modifier = Modifier
                                        .padding(start = 2.dp)
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun LyricsCard(lyrics: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = lyrics,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun RecommendationsSection(recommendations: List<SongEntity>) {
    Column {
        Text(
            text = "Recommended for You",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(recommendations.size) { index ->
                val song = recommendations[index]
                Column(
                    modifier = Modifier.width(120.dp)
                ) {
                    AsyncImage(
                        model = song.albumArtPath ?: android.R.drawable.ic_media_play,
                        contentDescription = "Album Art",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
