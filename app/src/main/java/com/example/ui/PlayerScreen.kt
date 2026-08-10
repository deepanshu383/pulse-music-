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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                    HomeHeader(
                        queueSongCount = queueSongCount,
                        onBack = onBack,
                        onAddToPlaylist = onAddToPlaylist
                    )

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
        } // Closing Box

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

@Composable
private fun HomeHeader(
    queueSongCount: Int, 
    onBack: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column {
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (queueSongCount > 0) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PulseAccent.copy(alpha = 0.15f),
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                ) {
                    Text(
                        text = "Queue: $queueSongCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = PulseAccent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var swipingItemKey by remember { mutableStateOf<String?>(null) }
    
    var snackbarJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    data class DragItem(val key: String, val mediaItem: androidx.media3.common.MediaItem, val originalIndex: Int)
    
    fun generateDragQueue(queue: List<androidx.media3.common.MediaItem>): List<DragItem> {
        val counts = mutableMapOf<String, Int>()
        return queue.mapIndexed { index, item ->
            val count = counts.getOrDefault(item.mediaId, 0)
            counts[item.mediaId] = count + 1
            DragItem("${item.mediaId}_$count", item, index)
        }
    }

    var dragQueue by remember { mutableStateOf(generateDragQueue(playbackState.queue)) }

    androidx.compose.runtime.LaunchedEffect(playbackState.queue) {
        val currentIds = dragQueue.map { it.mediaItem.mediaId }
        val newIds = playbackState.queue.map { it.mediaId }
        if (currentIds != newIds) {
            dragQueue = generateDragQueue(playbackState.queue)
        }
    }

    // Centralized Undo Snackbar Logic
    LaunchedEffect(canUndo) {
        if (canUndo) {
            val result = snackbarHostState.showSnackbar(
                message = "Song removed",
                actionLabel = "Undo",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                onUndoDelete()
            }
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    var activeDraggedIndex by remember { mutableStateOf<Int?>(null) }
    var initialDraggedIndex by remember { mutableStateOf<Int?>(null) }
    var activeDragOffsetY by remember { mutableStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(TextTertiary.copy(alpha = 0.2f))
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎵 Queue (${playbackState.queue.size} songs)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    
                    if (playbackState.queue.isNotEmpty()) {
                        TextButton(onClick = onClearQueue) {
                            Text("Clear All", color = ErrorRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (playbackState.queue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Your queue is empty", color = TextTertiary)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // NOW PLAYING SECTION
                        val currentIdx = playbackState.currentQueueIndex
                        if (currentIdx != -1 && currentIdx < playbackState.queue.size) {
                            item {
                                Text(
                                    text = "[Now Playing]",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PulseAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                                
                                val currentSong = playbackState.queue[currentIdx]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(PulseAccent.copy(alpha = 0.1f))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = PulseAccent)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentSong.mediaMetadata.title?.toString() ?: "Unknown",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${currentSong.mediaMetadata.artist ?: "Unknown"} • Playing",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PulseAccent
                                        )
                                    }
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = PulseAccent)
                                }
                                
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                    color = TextTertiary.copy(alpha = 0.1f)
                                )
                            }
                        }

                        // NEXT UP SECTION
                        item {
                            Text(
                                text = "[Next Up - Drag to Reorder]",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }

                        itemsIndexed(
                            dragQueue,
                            key = { _, item -> item.key }
                        ) { index, dragItem ->
                            val item = dragItem.mediaItem
                            val isCurrent = dragItem.originalIndex == playbackState.currentQueueIndex
                            val isBeingDragged = activeDraggedIndex == index
                            val itemTranslationY = if (isBeingDragged) activeDragOffsetY else 0f
                            val itemZIndex = if (isBeingDragged) 100f else 0f
                            val itemScale by animateFloatAsState(if (isBeingDragged) 1.06f else 1f, label = "scale")
                            val itemElevation by animateDpAsState(if (isBeingDragged) 12.dp else 0.dp, label = "elevation")

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { state ->
                                if (state == SwipeToDismissBoxValue.EndToStart) {
                                    if (swipingItemKey != null && swipingItemKey != dragItem.key) {
                                        false
                                    } else {
                                        swipingItemKey = dragItem.key
                                        true
                                    }
                                } else if (state == SwipeToDismissBoxValue.Settled) {
                                    if (swipingItemKey == dragItem.key) {
                                        swipingItemKey = null
                                    }
                                    true
                                } else false 
                            }
                        )

                            androidx.compose.runtime.DisposableEffect(dragItem.key) {
                                onDispose {
                                    if (swipingItemKey == dragItem.key) {
                                        swipingItemKey = null
                                    }
                                }
                            }

                            androidx.compose.runtime.LaunchedEffect(dismissState.currentValue) {
                                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                    try {
                                        var targetIdx = -1
                                        val counts = mutableMapOf<String, Int>()
                                        for (i in playbackState.queue.indices) {
                                            val qItem = playbackState.queue[i]
                                            val c = counts.getOrDefault(qItem.mediaId, 0)
                                            counts[qItem.mediaId] = c + 1
                                            if ("${qItem.mediaId}_$c" == dragItem.key) {
                                                targetIdx = i
                                                break
                                            }
                                        }

                                        if (targetIdx != -1) {
                                            onRemoveFromQueue(targetIdx)
                                            
                                            snackbarJob?.cancel()
                                            snackbarJob = coroutineScope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Removed: ${dragItem.mediaItem.mediaMetadata.title}",
                                                    actionLabel = "Undo",
                                                    duration = androidx.compose.material3.SnackbarDuration.Short
                                                )
                                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                    onUndoDelete()
                                                }
                                            }
                                        }
                                    } finally {
                                        if (swipingItemKey == dragItem.key) {
                                            swipingItemKey = null
                                        }
                                    }
                                }
                            }

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = swipingItemKey == null || swipingItemKey == dragItem.key,
                                modifier = Modifier.animateItem(),
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(ErrorRed.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.padding(end = 20.dp))
                                    }
                                }
                            ) {
                                val currentDragItemKey by rememberUpdatedState(dragItem.key)
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .zIndex(itemZIndex)
                                        .graphicsLayer {
                                            translationY = itemTranslationY
                                            scaleX = itemScale
                                            scaleY = itemScale
                                            shadowElevation = itemElevation.toPx()
                                            clip = true
                                            shape = RoundedCornerShape(16.dp)
                                        }
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            when {
                                                isBeingDragged -> DarkSurfaceContainerHighest
                                                isCurrent -> PulseAccent.copy(alpha = 0.15f)
                                                else -> DarkSurfaceContainer.copy(alpha = 0.4f)
                                            }
                                        )
                                        .pointerInput(dragItem.key) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    val latestIndex = dragQueue.indexOfFirst { it.key == currentDragItemKey }
                                                    if (latestIndex != -1) {
                                                        activeDraggedIndex = latestIndex
                                                        initialDraggedIndex = latestIndex
                                                        activeDragOffsetY = 0f
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val currIdx = activeDraggedIndex ?: return@detectDragGesturesAfterLongPress
                                                    activeDragOffsetY += dragAmount.y
                                                    
                                                    val itemHeight = 72.dp.toPx()
                                                    val displacement = (activeDragOffsetY / itemHeight).toInt()
                                                    
                                                    if (displacement != 0) {
                                                        val newIdx = (currIdx + displacement).coerceIn(0, dragQueue.size - 1)
                                                        if (newIdx != currIdx) {
                                                            val newList = dragQueue.toMutableList()
                                                            val movedItem = newList.removeAt(currIdx)
                                                            newList.add(newIdx, movedItem)
                                                            dragQueue = newList
                                                            
                                                            activeDraggedIndex = newIdx
                                                            activeDragOffsetY -= displacement * itemHeight
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                    }

                                                    if (activeDragOffsetY < -20f && listState.firstVisibleItemIndex > 0) {
                                                        coroutineScope.launch { listState.scrollBy(-50f) }
                                                    } else if (activeDragOffsetY > 20f) {
                                                        coroutineScope.launch { listState.scrollBy(50f) }
                                                    }
                                                },
                                                onDragEnd = {
                                                    val from = initialDraggedIndex
                                                    val to = activeDraggedIndex
                                                    if (from != null && to != null && from != to) {
                                                        onMoveQueueItem(from, to)
                                                    }
                                                    activeDraggedIndex = null
                                                    initialDraggedIndex = null
                                                    activeDragOffsetY = 0f
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDragCancel = {
                                                    activeDraggedIndex = null
                                                    initialDraggedIndex = null
                                                    activeDragOffsetY = 0f
                                                }
                                            )
                                        }
                                        .clickable { onGoToSong(dragItem.originalIndex) }
                                        .padding(horizontal = 20.dp, vertical = 18.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = if (isCurrent) "♪" else "◉",
                                            color = if (isCurrent) PulseAccent else TextTertiary,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.mediaMetadata.title?.toString() ?: "Unknown Song",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (isCurrent) PulseAccent else TextPrimary,
                                                fontWeight = if (isCurrent || isBeingDragged) FontWeight.Bold else FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.mediaMetadata.artist ?: "Unknown Artist"} • ${formatDuration(item.mediaMetadata.extras?.getLong("duration") ?: 0L)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isCurrent) PulseAccent.copy(alpha = 0.7f) else TextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                        
                                        Icon(
                                            Icons.Default.DragHandle,
                                            contentDescription = null,
                                            tint = if (isBeingDragged) PulseAccent else TextTertiary.copy(alpha = 0.2f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
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
