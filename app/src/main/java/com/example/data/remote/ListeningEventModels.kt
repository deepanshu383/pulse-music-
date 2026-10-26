package com.example.data.remote

enum class SkipCategory {
    INSTANT, // under 10%
    EARLY,   // 10-35%
    NORMAL,  // 35-80%
    COMPLETED // over 80%
}

data class ListeningEvent(
    val userId: String = "",
    val trackId: String = "",
    val artistId: String = "",
    val artistName: String = "",
    val recommendationReason: String = "", // e.g. "Similar Artist", "Mood Match"
    val totalScore: Float = 0f,
    val factorScores: Map<String, Float> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val playDurationMs: Long = 0,
    val totalDurationMs: Long = 0,
    val completionPercentage: Float = 0f,
    val isSkipped: Boolean = false,
    val skipCategory: String = SkipCategory.COMPLETED.name,
    val replayCount: Int = 0,
    val isLiked: Boolean = false,
    val moodTags: List<String> = emptyList(),
    val genreTags: List<String> = emptyList()
)
