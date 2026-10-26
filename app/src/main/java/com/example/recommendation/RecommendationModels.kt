package com.example.recommendation

/**
 * Captures features of a track for recommendation purposes.
 */
data class TrackFeatures(
    val trackId: String,
    val title: String,
    val moodGenreTag: String,
    val artist: String,
    val releaseYear: Int,
    val acousticClusterId: String,
    val playCount: Int,
    val skipCount: Int,
    val lastPlayedTimestamp: Long,
    val averageCompletionPercentage: Float,
    val userRating: Float?,
    val language: String? = null,
    val album: String = "Unknown Album"
)

/**
 * Weights for the recommendation scoring engine.
 * User preference is treated as a base modifier (additive, not part of the base 100%).
 */
object ScoringWeights {
    const val MOOD = 0.30f
    const val ACOUSTIC = 0.10f
    const val TIME_OF_DAY = 0.05f
    const val ERA = 0.20f
    const val LANG = 0.25f
    const val CATEGORY = 0.10f
    const val USER_PREF = 0.10f
}

/**
 * Represents the computed recommendation score for a candidate track.
 */
data class RecommendationScore(
    val trackId: String,
    val moodConsistency: Float,
    val acousticSimilarity: Float,
    val timeOfDayFit: Float,
    val eraProximity: Float,
    val languageConsistency: Float = 0.5f,
    val categoryConsistency: Float = 0.5f,
    val userPreference: Float,
    val fatiguePenalty: Float = 0.0f,
    val sessionSkipPenalty: Boolean = false,
    val jitterBonus: Float = 0.0f,
    val factorMultipliers: Map<String, Float> = emptyMap(),
    val explorationMultiplier: Float = 0.5f,
    val isHardFiltered: Boolean = false,
    val hardFilterReason: String? = null
) {
    data class Filtered(val hardFiltered: Boolean, val score: Float = 0.0f, val reason: String? = null)

    /**
     * Computed total score (weighted sum).
     */
    val totalScore: Float
        get() {
            if (isHardFiltered) return -9999.0f // true short-circuit, nothing below can override this

            val moodMult = factorMultipliers["moodConsistency"] ?: 1.0f
            val acousticMult = factorMultipliers["acousticSimilarity"] ?: 1.0f
            val todMult = factorMultipliers["timeOfDayFit"] ?: 1.0f
            val eraMult = factorMultipliers["eraProximity"] ?: 1.0f
            val langMult = factorMultipliers["languageConsistency"] ?: 1.0f
            val categoryMult = factorMultipliers["categoryConsistency"] ?: 1.0f
            val prefMult = factorMultipliers["userPreference"] ?: 1.0f
            val fatigueMult = factorMultipliers["fatiguePenalty"] ?: 1.0f

            val baseScore = (moodConsistency * ScoringWeights.MOOD * moodMult) +
                    (acousticSimilarity * ScoringWeights.ACOUSTIC * acousticMult) +
                    (timeOfDayFit * ScoringWeights.TIME_OF_DAY * todMult) +
                    (eraProximity * ScoringWeights.ERA * eraMult) +
                    (languageConsistency * ScoringWeights.LANG * langMult) +
                    (categoryConsistency * ScoringWeights.CATEGORY * categoryMult) +
                    (userPreference * ScoringWeights.USER_PREF * prefMult)
            
            // Exploration boost: if the user hasn't played it much (fatigue is 0) 
            // and the exploration mult is high, give a small boost to new tracks.
            // If they prefer familiarity (exploration low), give a boost to things they've played but aren't fatigued by.
            val explorationBoost = if (fatiguePenalty == 0f) (explorationMultiplier * 0.1f) else ((1f - explorationMultiplier) * 0.1f)
                   
            val penalizedScore = baseScore - (fatiguePenalty * fatigueMult) + jitterBonus + explorationBoost
            return if (sessionSkipPenalty) penalizedScore * 0.5f else penalizedScore
        }
}

/**
 * Engine responsible for scoring next-song candidates based on the current track and features.
 */
interface RecommendationEngine {
    suspend fun scoreNext(
        current: TrackFeatures,
        candidates: List<TrackFeatures>,
        isStarvedPool: Boolean = false,
        factorMultipliers: Map<String, Float> = emptyMap(),
        explorationMultiplier: Float = 0.5f,
        effectiveLanguage: String? = null
    ): List<RecommendationScore>
}
