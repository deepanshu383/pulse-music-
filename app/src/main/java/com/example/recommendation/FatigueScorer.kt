package com.example.recommendation

import com.example.data.local.PlaybackHistoryDao
import com.example.data.local.PlaybackHistoryEntity

class FatigueScorer(private val historyDao: PlaybackHistoryDao? = null) {

    private var recentPlaysCache: List<PlaybackHistoryEntity>? = null
    private var historicalCanonicalCounts: Map<String, Int> = emptyMap()

    /**
     * Pre-fetch database data and calculate historical frequencies by canonical key.
     */
    suspend fun prepare(candidates: List<TrackFeatures>) {
        if (historyDao == null) return
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 3600 * 1000L)
        val history = historyDao.getHistorySince(sevenDaysAgo)
        recentPlaysCache = history
        
        // Build a fast lookup from trackId to canonicalKey using the candidate pool
        val idToCanonical = candidates.associate { 
            it.trackId to RecommendationSessionManager.getCanonicalSongKey(it.title, it.artist)
        }
        
        // Count frequencies of historical plays by canonical key
        // If a trackId isn't in candidates, we just count its raw trackId to be safe
        historicalCanonicalCounts = history.groupingBy { 
            idToCanonical[it.trackId] ?: it.trackId 
        }.eachCount()
    }

    /**
     * Calculates a fatigue penalty based on play frequency in the last 7 days.
     * Returns a penalty value to subtract from the total score.
     */
    fun calculateFatiguePenalty(candidate: TrackFeatures): Float {
        val canonicalKey = RecommendationSessionManager.getCanonicalSongKey(candidate.title, candidate.artist)
        val playCount = historicalCanonicalCounts[canonicalKey] ?: 0
        
        // Compound penalty: -0.10 for every play in the last 7 days
        return (playCount * 0.10f).coerceAtMost(1.0f)
    }
}
