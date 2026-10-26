package com.example.data.remote

data class UserAffinityProfile(
    val factorMultipliers: Map<String, Float> = mapOf(
        "moodConsistency" to 1.0f,
        "acousticSimilarity" to 1.0f,
        "timeOfDayFit" to 1.0f,
        "eraProximity" to 1.0f,
        "userPreference" to 1.0f,
        "fatiguePenalty" to 1.0f
    ),
    val explorationMultiplier: Float = 0.5f, // 0.0 = highly familiar, 1.0 = highly explorative
    val artistAffinities: Map<String, Float> = emptyMap(),
    val lastUpdated: Long = System.currentTimeMillis()
)
