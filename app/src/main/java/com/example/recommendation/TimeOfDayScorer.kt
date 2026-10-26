package com.example.recommendation

import com.example.data.local.PlaybackHistoryDao

class TimeOfDayScorer(private val historyDao: PlaybackHistoryDao? = null) {

    companion object {
        fun getCurrentTimeSegment(): String {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 6..11 -> "Morning"
                in 12..16 -> "Afternoon"
                in 17..20 -> "Evening"
                else -> "Night"
            }
        }
    }

    private var currentSegment: String = "Morning"
    private var hasSegmentHistory: Boolean = false

    suspend fun prepare() {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        currentSegment = when (hour) {
            in 6..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }
        val history = historyDao?.getRecentHistoryBySegment(currentSegment, 5) ?: emptyList()
        hasSegmentHistory = history.isNotEmpty()
    }

    fun calculateTimeOfDayFit(candidate: TrackFeatures): Float {
        val mood = candidate.moodGenreTag.lowercase()
        val isEnergetic = mood in listOf("workout", "party", "edm", "rock", "upbeat", "motivational")
        val isAmbient = mood in listOf("sleep", "lullaby", "ambient", "sad", "lo-fi", "lofi", "devotional") // Ensure devotional counts as ambient

        var score = 0.5f // Default

        if (currentSegment == "Morning" && isEnergetic) {
            score = 0.9f
        } else if (currentSegment == "Night" && isAmbient) {
            score = 0.9f
        } else if (currentSegment == "Morning" && isAmbient) {
            score = 0.2f
        } else if (currentSegment == "Night" && isEnergetic) {
            score = 0.2f
        }
        
        // Slightly boost if we have history for this segment, representing user's proven habit
        if (hasSegmentHistory) {
            score = (score + 0.1f).coerceAtMost(1.0f)
        }

        return score
    }
}
