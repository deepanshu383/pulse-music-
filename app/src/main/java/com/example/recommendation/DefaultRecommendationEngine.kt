package com.example.recommendation

import com.example.data.local.UserPreferencesManager

class DefaultRecommendationEngine(
    private val acousticClusters: Map<String, Set<String>> = defaultAcousticClusters(),
    private val timeOfDayScorer: TimeOfDayScorer = TimeOfDayScorer(),
    private val fatigueScorer: FatigueScorer = FatigueScorer(),
    private val userPreferencesManager: UserPreferencesManager? = null
) : RecommendationEngine {

    override suspend fun scoreNext(
        current: TrackFeatures,
        candidates: List<TrackFeatures>,
        isStarvedPool: Boolean,
        factorMultipliers: Map<String, Float>,
        explorationMultiplier: Float,
        effectiveLanguage: String?
    ): List<RecommendationScore> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        
        // Pre-fetch database requirements once to avoid N+1 queries in the loop
        fatigueScorer.prepare(candidates)
        timeOfDayScorer.prepare()

        val scoredCandidates = candidates.map { candidate ->
            val moodScore = calculateMoodConsistency(current.moodGenreTag, candidate.moodGenreTag, isStarvedPool)
            val acousticScore = calculateAcousticSimilarity(current, candidate)

            val timeOfDayFit = timeOfDayScorer.calculateTimeOfDayFit(candidate)
            val eraProximityResult = calculateEraProximity(current, candidate, acousticScore)
            
            val langConsistencyResult = calculateLanguageConsistency(effectiveLanguage ?: current.language, candidate.language)
            val categoryConsistency = calculateCategoryConsistency(current, candidate)
            
            var userPreference = candidate.userRating ?: 0.0f
            
            // Add session artist penalty
            val artistPenalty = RecommendationSessionManager.getArtistPenalty(candidate.artist)
            userPreference += artistPenalty
            
            // Cold start preferences bonus
            if (userPreferencesManager != null) {
                if (userPreferencesManager.getPreferredArtists().contains(candidate.artist)) {
                    userPreference += 0.5f // arbitrary seed data bonus
                }
            }

            val fatiguePenalty = fatigueScorer.calculateFatiguePenalty(candidate)
            val isSkipped = RecommendationSessionManager.isTrackSkipped(candidate.trackId)

            var hardFiltered = false
            var filterReason: String? = null

            if (moodScore <= -9999.0f) {
                hardFiltered = true
                filterReason = "mood_mismatch"
            } else if (eraProximityResult.hardFiltered) {
                hardFiltered = true
                filterReason = eraProximityResult.reason
            } else if (langConsistencyResult.hardFiltered) {
                hardFiltered = true
                filterReason = langConsistencyResult.reason
            }

            RecommendationScore(
                trackId = candidate.trackId,
                moodConsistency = moodScore,
                acousticSimilarity = acousticScore,
                timeOfDayFit = timeOfDayFit,
                eraProximity = eraProximityResult.score,
                languageConsistency = langConsistencyResult.score,
                categoryConsistency = categoryConsistency,
                userPreference = userPreference,
                fatiguePenalty = fatiguePenalty,
                sessionSkipPenalty = isSkipped,
                jitterBonus = if (!hardFiltered) {
                    (java.util.Random().nextFloat() - 0.5f) * 0.06f
                } else 0.0f,
                factorMultipliers = factorMultipliers,
                explorationMultiplier = explorationMultiplier,
                isHardFiltered = hardFiltered,
                hardFilterReason = filterReason
            )
        }
        
        // Deduplicate: Keep only the highest scoring version of each canonical song
        val deduplicatedScores = scoredCandidates
            .groupBy { RecommendationSessionManager.getCanonicalSongKey(
                candidates.find { c -> c.trackId == it.trackId }?.title ?: "",
                candidates.find { c -> c.trackId == it.trackId }?.artist ?: ""
            ) }
            .map { (_, versions) ->
                versions.maxByOrNull { it.totalScore }!!
            }
            .sortedByDescending { it.totalScore }

        return@withContext deduplicatedScores
    }

    private fun calculateMoodConsistency(currentMood: String, candidateMood: String, isStarvedPool: Boolean = false): Float {
        val lockedGroup = RecommendationSessionManager.getLockedMoodGroup() 
            ?: MoodExtractionStrategy.getMoodGroup(currentMood)
        
        val candidateGroup = MoodExtractionStrategy.getMoodGroup(candidateMood)
        
        // HARD CONSTRAINT: Never jump out of the locked mood group
        if (lockedGroup != candidateGroup) {
            return -9999.0f
        }

        // Within the same group, exact mood matches get a slight edge
        if (currentMood.equals(candidateMood, ignoreCase = true)) {
            return 0.9f
        }
        return 0.7f
    }

    private fun calculateAcousticSimilarity(current: TrackFeatures, candidate: TrackFeatures): Float {
        if (current.acousticClusterId.isNotBlank() && current.acousticClusterId == candidate.acousticClusterId) {
            return 1.0f
        }
        return ArtistSimilarityEngine.calculateAcousticSimilarity(current.artist, candidate.artist)
    }

    private fun getConflictingMoods(mood: String): Set<String> {
        return when (mood.lowercase()) {
            "romantic", "lullaby", "sleep", "sad" -> setOf("party", "hip-hop", "workout", "edm", "rock", "devotional", "happy", "energetic", "neutral")
            "party", "hip-hop", "workout", "edm", "rock", "happy", "energetic" -> setOf("romantic", "lullaby", "sleep", "sad", "classical", "devotional", "neutral")
            "devotional" -> setOf("party", "hip-hop", "workout", "edm", "rock", "romantic", "sad", "lullaby", "bollywood", "pop", "happy", "energetic", "neutral")
            "bollywood", "pop" -> setOf("devotional")
            "neutral" -> setOf("devotional") // Neutral no longer sneaks past mood barriers
            else -> emptySet()
        }
    }

    private fun getEraBucket(year: Int): String {
        return when {
            year < 1990 -> "Classics"
            year < 2000 -> "90s Nostalgia"
            year < 2010 -> "2000s Melodies"
            year < 2020 -> "2010s Hits"
            else -> "Modern Pop"
        }
    }

    private fun isRemix(title: String): Boolean {
        val lower = title.lowercase()
        return lower.contains("remix") || lower.contains("reprise") || lower.contains("rework") || lower.contains("lo-fi") || lower.contains("lofi")
    }

    private fun calculateEraProximity(current: TrackFeatures, candidate: TrackFeatures, acousticScore: Float): RecommendationScore.Filtered {
        // Remix filtering in a legacy-era loop (legacy is < 2010)
        val isLegacyLoop = current.releaseYear < 2010
        if (isLegacyLoop && isRemix(candidate.title) && !isRemix(current.title)) {
            // Heavily penalize remixes in legacy loops (filter out)
            return RecommendationScore.Filtered(hardFiltered = true, reason = "legacy_remix_mismatch")
        }

        val yearDiff = Math.abs(current.releaseYear - candidate.releaseYear)
        val hasAcousticClusterMatch = acousticScore >= 0.8f // same cluster (0.8) or same artist (1.0)

        return when {
            yearDiff <= 5 -> RecommendationScore.Filtered(hardFiltered = false, score = 1.0f) // Within +/- 5 year window — always allowed
            hasAcousticClusterMatch && yearDiff <= 15 -> {
                // Acoustic cluster match allows wider window (up to 15 years)
                // but applies a steep proportional penalty: 1.0 at 5yr → 0.0 at 15yr
                val penaltyRange = yearDiff - 5 // 1..10
                RecommendationScore.Filtered(hardFiltered = false, score = 1.0f - (penaltyRange / 10.0f)) // linear decay from 1.0 to 0.0
            }
            else -> RecommendationScore.Filtered(hardFiltered = true, reason = "era_mismatch") // HARD CONSTRAINT: No era jumping beyond allowed window
        }
    }

    private fun calculateLanguageConsistency(effectiveLanguage: String?, candidateLang: String?): RecommendationScore.Filtered {
        if (effectiveLanguage == null || candidateLang == null) return RecommendationScore.Filtered(hardFiltered = true, reason = "unknown_language") // HARD CONSTRAINT: Unknown language blocked

        val currentLower = effectiveLanguage.lowercase()
        val candidateLower = candidateLang.lowercase()

        // Exact match is heavily boosted
        if (currentLower == candidateLower) return RecommendationScore.Filtered(hardFiltered = false, score = 1.0f)

        // Soft match for closely related languages to prevent complete starvation
        if ((currentLower == "hindi" && candidateLower == "urdu") || (currentLower == "urdu" && candidateLower == "hindi")) {
            return RecommendationScore.Filtered(hardFiltered = false, score = 0.2f) // Positive, but much lower than exact match (1.0) to heavily prefer exact matches
        }

        return RecommendationScore.Filtered(hardFiltered = true, reason = "language_mismatch")
    }

    /**
     * Category consistency: keeps movie/soundtrack songs together and freelance/singles together.
     * A song is considered a "movie/soundtrack" song if it has a proper album name
     * (not "Unknown Album" and album != title). Independent/freelance songs typically
     * have album == title or "Unknown Album".
     */
    private fun calculateCategoryConsistency(current: TrackFeatures, candidate: TrackFeatures): Float {
        val currentIsMovie = isMovieSong(current)
        val candidateIsMovie = isMovieSong(candidate)

        return when {
            // Both are movie songs → strong match
            currentIsMovie && candidateIsMovie -> 1.0f
            // Both are independent/singles → good match
            !currentIsMovie && !candidateIsMovie -> 0.8f
            // Mismatch: one is movie, other is independent → penalty
            else -> -9999.0f
        }
    }

    /**
     * Determines if a song is from a movie/soundtrack vs an independent single.
     * Movie songs have a distinct album name (the film name). Independent songs
     * often have album name == song title or "Unknown Album".
     */
    private fun isMovieSong(track: TrackFeatures): Boolean {
        val album = track.album.trim()
        val title = track.title.trim()
        if (album.isBlank() || album.equals("Unknown Album", ignoreCase = true)) return false
        // If album name is the same as the song title, it's likely a single release
        if (album.equals(title, ignoreCase = true)) return false
        return true
    }

    companion object {
        fun defaultAcousticClusters(): Map<String, Set<String>> {
            // Hardcoded initial cluster map, structured to later load from remote config or API
            return mapOf(
                "vocal_texture_1" to setOf("Atif Aslam", "KK", "Arijit Singh"),
                "vocal_texture_2" to setOf("Shreya Ghoshal", "Lata Mangeshkar", "Asha Bhosle"),
                "upbeat_pop" to setOf("Neha Kakkar", "Badshah", "Yo Yo Honey Singh")
            )
        }
    }
}
