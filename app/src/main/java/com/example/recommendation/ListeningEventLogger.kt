package com.example.recommendation

import com.example.data.remote.ListeningEvent
import com.example.data.remote.SkipCategory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class RecommendationContext(
    val reason: String = "",
    val totalScore: Float = 0f,
    val factorScores: Map<String, Float> = emptyMap(),
    val moodTags: List<String> = emptyList(),
    val genreTags: List<String> = emptyList()
)

object ListeningEventLogger {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Store context for enqueued tracks
    private val contextCache = mutableMapOf<String, RecommendationContext>()
    
    private var playbackHistoryDao: com.example.data.local.PlaybackHistoryDao? = null

    fun init(dao: com.example.data.local.PlaybackHistoryDao) {
        playbackHistoryDao = dao
    }
    
    // State tracking for the currently playing track
    private var currentTrackId: String? = null
    private var currentTrackDurationMs: Long = 0L
    private var maxPositionReachedMs: Long = 0L
    private var artistId: String = ""
    private var artistName: String = ""
    
    fun registerRecommendation(trackId: String, context: RecommendationContext) {
        contextCache[trackId] = context
    }
    
    fun onTrackStarted(trackId: String, title: String, artistId: String, artistName: String, durationMs: Long) {
        // If there was a previous track playing, finalize its event
        if (currentTrackId != null && currentTrackId != trackId) {
            finalizeCurrentTrack()
        }
        
        currentTrackId = trackId
        this.artistId = artistId
        this.artistName = artistName
        currentTrackDurationMs = durationMs
        maxPositionReachedMs = 0L

        // NEW: immediate lightweight history write
        scope.launch {
            val segment = com.example.recommendation.TimeOfDayScorer.getCurrentTimeSegment()
            val canonicalKey = RecommendationSessionManager.getCanonicalSongKey(title, artistName)
            playbackHistoryDao?.insertHistory(
                com.example.data.local.PlaybackHistoryEntity(
                    trackId = trackId,
                    title = title,
                    artist = artistName,
                    canonicalSongKey = canonicalKey,
                    timeSegment = segment,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    fun onProgressUpdated(positionMs: Long) {
        if (positionMs > maxPositionReachedMs) {
            maxPositionReachedMs = positionMs
        }
    }
    
    fun onTrackEndedNormally() {
        if (currentTrackId != null) {
            // Force 100% completion
            maxPositionReachedMs = currentTrackDurationMs
            finalizeCurrentTrack()
        }
    }
    
    fun onTrackSkipped() {
        finalizeCurrentTrack()
    }
    
    private fun finalizeCurrentTrack() {
        val trackId = currentTrackId ?: return
        val duration = currentTrackDurationMs
        val played = maxPositionReachedMs
        
        // Reset state immediately so it's not double-logged
        currentTrackId = null
        
        if (duration <= 0) return // Invalid duration, ignore
        
        val completionPercentage = (played.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        
        val skipCategory = when {
            completionPercentage < 0.1f -> SkipCategory.INSTANT
            completionPercentage < 0.35f -> SkipCategory.EARLY
            completionPercentage < 0.8f -> SkipCategory.NORMAL
            else -> SkipCategory.COMPLETED
        }
        
        val isSkipped = skipCategory != SkipCategory.COMPLETED
        val context = contextCache[trackId] ?: RecommendationContext()
        val uid = auth.currentUser?.uid
        
        if (uid != null) {
            val event = ListeningEvent(
                userId = uid,
                trackId = trackId,
                artistId = artistId,
                artistName = artistName,
                recommendationReason = context.reason,
                totalScore = context.totalScore,
                factorScores = context.factorScores,
                timestamp = System.currentTimeMillis(),
                playDurationMs = played,
                totalDurationMs = duration,
                completionPercentage = completionPercentage,
                isSkipped = isSkipped,
                skipCategory = skipCategory.name,
                moodTags = context.moodTags,
                genreTags = context.genreTags
            )
            
            scope.launch {
                try {
                    firestore.collection("listening_events")
                        .document(uid)
                        .collection("events")
                        .add(event)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // Cleanup cache
        contextCache.remove(trackId)
    }
}
