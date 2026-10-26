package com.example.recommendation

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VerificationTest {

    private lateinit var engine: DefaultRecommendationEngine

    @Before
    fun setup() {
        engine = DefaultRecommendationEngine()
    }

    private fun createTrack(id: String, title: String, artist: String, language: String, isMovie: Boolean = false, releaseYear: Int = 2022): TrackFeatures {
        return TrackFeatures(
            trackId = id,
            title = title,
            moodGenreTag = "party",
            artist = artist,
            releaseYear = releaseYear,
            acousticClusterId = "pop1",
            playCount = 10,
            skipCount = 2,
            lastPlayedTimestamp = System.currentTimeMillis(),
            averageCompletionPercentage = 0.8f,
            userRating = 0.5f,
            language = language
        )
    }

    @Test
    fun testSimulationAndJitter() = runBlocking {
        val currentTrack = createTrack("song1", "Seed Song", "Seed Artist", "hindi")
        val candidates = listOf(
            createTrack("song2", "Candidate 1", "Artist 1", "hindi"),
            createTrack("song3", "Candidate 2", "Artist 1", "hindi"),
            createTrack("song4", "Candidate 3", "Artist 2", "hindi"),
            createTrack("song5", "Candidate 4", "Artist 3", "punjabi"),
            createTrack("song6", "Candidate 5", "Artist 4", "hindi")
        )

        // Run scoring first time
        val scores1 = engine.scoreNext(
            current = currentTrack,
            candidates = candidates,
            isStarvedPool = false,
            factorMultipliers = emptyMap(),
            explorationMultiplier = 0.5f,
            effectiveLanguage = "hindi"
        )

        // Run scoring second time exactly the same
        val scores2 = engine.scoreNext(
            current = currentTrack,
            candidates = candidates,
            isStarvedPool = false,
            factorMultipliers = emptyMap(),
            explorationMultiplier = 0.5f,
            effectiveLanguage = "hindi"
        )

        // Verify they are different because of jitter
        val scoresDiffer = scores1.zip(scores2).any { it.first.jitterBonus != it.second.jitterBonus }
        assertTrue("Jitter should produce different scores for the same input", scoresDiffer)

        // Verify devotional filter works
        val devotionalExcluded = DevotionalContentFilter.shouldExclude(
            title = "Aarti Sangrah",
            album = "Best of Bhajans",
            artist = "Anup Jalota",
            moodGenreTag = "party"
        )
        assertTrue("Devotional filter must exclude aarti/bhajan", devotionalExcluded)

        val devotionalExcludedRegional = DevotionalContentFilter.shouldExclude(
            title = "Abhang Vani",
            album = "Vitthal Vitthal",
            artist = "Suresh Wadkar",
            moodGenreTag = "party"
        )
        assertTrue("Devotional filter must exclude regional bhakti (Vitthal)", devotionalExcludedRegional)

        // Verify hard filter short-circuits
        val scoresWithMismatchLang = engine.scoreNext(
            current = currentTrack,
            candidates = listOf(createTrack("song7", "Tamil Track", "Artist 5", "tamil")),
            isStarvedPool = false,
            factorMultipliers = emptyMap(),
            explorationMultiplier = 0.5f,
            effectiveLanguage = "hindi"
        )
        assertTrue("Language mismatch should be hard-filtered", scoresWithMismatchLang[0].isHardFiltered)
        assertEquals("language_mismatch", scoresWithMismatchLang[0].hardFilterReason)
    }
}
