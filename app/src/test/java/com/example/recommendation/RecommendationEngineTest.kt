package com.example.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecommendationEngineTest {

    private lateinit var engine: DefaultRecommendationEngine

    @Before
    fun setup() {
        RecommendationSessionManager.resetSession()
        engine = DefaultRecommendationEngine()
    }

    private fun createTrack(
        id: String,
        title: String,
        mood: String,
        artist: String,
        year: Int = 2020,
        clusterId: String = "",
        language: String? = "hindi"
    ): TrackFeatures {
        return TrackFeatures(
            trackId = id,
            title = title,
            moodGenreTag = mood,
            artist = artist,
            releaseYear = year,
            acousticClusterId = clusterId,
            playCount = 10,
            skipCount = 2,
            lastPlayedTimestamp = System.currentTimeMillis(),
            averageCompletionPercentage = 0.8f,
            userRating = 0.5f,
            language = language
        )
    }

    @Test
    fun testMoodConsistency_sameMood() {
        val current = createTrack("1", "Song A", "Romantic", "Unknown")
        val candidate = createTrack("2", "Song B", "romantic", "Unknown") // Case insensitive

        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate)) }
        
        assertEquals(1, results.size)
        // Locked at 90%
        assertEquals(0.9f, results[0].moodConsistency, 0.001f)
    }

    @Test
    fun testMoodConsistency_conflictingMoodGroup() {
        // Manually lock session to Party
        RecommendationSessionManager.registerManualPick("Party")
        
        val current = createTrack("1", "Song A", "Party", "Unknown")
        val candidate = createTrack("2", "Song B", "Romantic", "Unknown")

        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate)) }
        
        // Out-of-group moods get hard-filtered
        assertEquals(-9999.0f, results[0].moodConsistency, 0.001f)
    }

    @Test
    fun testMoodAntiDrift_hardFilterForOutGroup() {
        // User manually starts a "Party" track -> Locks session to "party" group
        RecommendationSessionManager.registerManualPick("Party")
        
        val current = createTrack("1", "Song A", "Party", "Unknown")
        
        // 4 Romantic candidates, 1 Dance candidate (Dance is in Party group)
        val candidate1 = createTrack("c1", "Rom 1", "Romantic", "Unknown")
        val candidate2 = createTrack("c2", "Rom 2", "Love", "Unknown")
        val candidate3 = createTrack("c3", "Rom 3", "Romantic", "Unknown")
        val candidate4 = createTrack("c4", "Rom 4", "Love", "Unknown")
        val candidate5 = createTrack("c5", "Dance 1", "Dance", "Unknown") // In party group

        val pool = listOf(candidate1, candidate2, candidate3, candidate4, candidate5)
        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, pool) }
        
        // Find the Dance candidate in the results
        val danceScore = results.find { it.trackId == "c5" }!!
        assertTrue("In-group candidate should have a positive mood score", danceScore.moodConsistency > 0f)
        
        // All romantic/love candidates should be hard-filtered to -9999.0f
        val outOfGroupScores = results.filter { it.trackId != "c5" }
        assertEquals(4, outOfGroupScores.size)
        outOfGroupScores.forEach { 
            assertEquals("Out-of-group candidate ${it.trackId} must be hard-filtered", -9999.0f, it.moodConsistency, 0.001f)
            assertTrue("Total score should reflect hard filter", it.totalScore <= -9999.0f)
        }
    }

    @Test
    fun testAcousticSimilarity_sameCluster() {
        val current = createTrack("1", "Song A", "Romantic", "Atif Aslam")
        val candidate = createTrack("2", "Song B", "Romantic", "Arijit Singh") // In same vocal_texture_1 cluster

        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate)) }
        
        assertEquals(0.8f, results[0].acousticSimilarity, 0.001f)
    }
    
    @Test
    fun testAcousticSimilarity_sameArtist() {
        val current = createTrack("1", "Song A", "Romantic", "Unknown Artist")
        val candidate = createTrack("2", "Song B", "Romantic", "Unknown Artist") 

        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate)) }
        
        assertEquals(1.0f, results[0].acousticSimilarity, 0.001f)
    }

    @Test
    fun testAcousticSimilarity_differentCluster() {
        val current = createTrack("1", "Song A", "Romantic", "Atif Aslam")
        val candidate = createTrack("2", "Song B", "Romantic", "Badshah") // Different cluster

        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate)) }
        
        assertEquals(0.3f, results[0].acousticSimilarity, 0.001f)
    }
    
    @Test
    fun testTotalScoreCalculation_sameClusterArtists() {
        val current = createTrack("1", "Song A", "Romantic", "Atif Aslam")
        // Same mood (0.9), same cluster (0.8), user rating (0.5)
        val candidate = createTrack("2", "Song B", "Romantic", "KK") 

        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate)) }
        val score = results[0]
        
        // Verify the score is positive and reasonable for a good match
        assertTrue("Total score should be positive for same mood/cluster match", score.totalScore > 0f)
        assertEquals(0.9f, score.moodConsistency, 0.001f)
        assertEquals(0.8f, score.acousticSimilarity, 0.001f)
    }

    @Test
    fun testLanguageConsistency_nullLanguageBlocked() {
        // candidate has null language
        val current = createTrack("1", "Song A", "Party", "Artist", year = 2020, language = "hindi")
        val candidate = createTrack("2", "Song B", "Party", "Artist", year = 2020, language = null)
        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate)) }
        assertEquals(-9999.0f, results[0].languageConsistency, 0.001f)
    }

    @Test
    fun testLanguageConsistency_crossRegionalBlocked() {
        // Tamil to Telugu should be blocked
        val current = createTrack("1", "Song A", "Party", "Artist", year = 2020, language = "tamil")
        val candidate = createTrack("2", "Song B", "Party", "Artist", year = 2020, language = "telugu")
        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate)) }
        assertEquals(-9999.0f, results[0].languageConsistency, 0.001f)
    }

    @Test
    fun testEraScoring_remixFilteredInLegacyLoop() {
        val current = createTrack("1", "Classic Hit", "Romantic", "Unknown", 2005)
        val candidate = createTrack("2", "Classic Hit (Remix)", "Romantic", "DJ Snake", 2022) // Avoid acoustic match

        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate)) }
        // Remix penalized heavily → -9999.0f era proximity (hard filter)
        assertEquals(-9999.0f, results[0].eraProximity, 0.001f)
        assertTrue("Total score should be heavily negative", results[0].totalScore < 0f)
    }

    @Test
    fun testEraScoring_KKTrackOutranksFillerTrack() {
        // Current: 2005 Atif Aslam track
        val current = createTrack("1", "Aadat", "Romantic", "Atif Aslam", 2005)
        
        // Candidate 1: KK track from 2008 (within 5-year window), strong acoustic match
        val kkCandidate = createTrack("2", "Khuda Jaane", "Romantic", "KK", 2008)
        
        // Candidate 2: Same-era filler track (from 2005), no acoustic match
        val fillerCandidate = createTrack("3", "Random 2005 Song", "Romantic", "Random Artist", 2005)

        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(kkCandidate, fillerCandidate)) }
        val kkScore = results.find { it.trackId == "2" }!!
        val fillerScore = results.find { it.trackId == "3" }!!

        assertTrue("KK candidate score (${kkScore.totalScore}) should be higher than filler candidate score (${fillerScore.totalScore})", 
            kkScore.totalScore > fillerScore.totalScore)
    }
    @Test
    fun testTimeOfDayFit() {
        val current = createTrack("1", "Current", "Pop", "Unknown")
        val candidate = createTrack("2", "Candidate", "workout", "Unknown") // energetic

        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate)) }
        val score = results[0]
        
        // TimeOfDay scoring depends on actual time, but it should return *some* float
        assertTrue("Time of day score should be >= 0", score.timeOfDayFit >= 0f)
    }

    @Test
    fun testEraScoring_strictProximityPenalty() {
        // Current track = year 2000
        val current = createTrack("1", "Song A", "Romantic", "Unknown 1", 2000)
        
        // Candidate 1: 2006 track, NO acoustic cluster match
        val candidate2006 = createTrack("2", "Song B", "Romantic", "Unknown 2", 2006)
        
        // Candidate 2: 2004 track, NO acoustic cluster match (within 5 years)
        val candidate2004 = createTrack("3", "Song C", "Romantic", "Unknown 3", 2004)

        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate2006, candidate2004)) }
        val score2006 = results.find { it.trackId == "2" }!!
        val score2004 = results.find { it.trackId == "3" }!!

        println("==============================")
        println("BEFORE/AFTER FIX TEST (Era)")
        println("2000 -> 2006: total=${score2006.totalScore}, era=${score2006.eraProximity}")
        println("2000 -> 2004: total=${score2004.totalScore}, era=${score2004.eraProximity}")
        println("==============================")

        assertTrue("2006 track should be penalized by Era and rank below 2004 track", score2006.totalScore < score2004.totalScore)
        // With the fix, 2006 gets a steep penalty (-5.0), so its total score will be heavily negative.
    }

    @Test
    fun testEarlySkipPenalty() {
        val current = createTrack("1", "Song A", "Romantic", "Atif Aslam")
        val candidate = createTrack("2", "Song B", "Romantic", "KK")
        
        // Base score without penalty
        val baseResults = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate)) }
        val baseScore = baseResults[0].totalScore
        
        // Apply skip penalty
        RecommendationSessionManager.registerEarlySkip("2", "KK")
        
        val penaltyResults = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate)) }
        val penaltyScore = penaltyResults[0]
        
        assertTrue(penaltyScore.sessionSkipPenalty)
        assertEquals(0.30f, penaltyScore.userPreference, 0.001f)
        assertTrue(penaltyScore.totalScore < baseScore) // Score should be halved (plus userPreference drop)
    }

    @Test
    fun testLanguageHardFilter_punjabiNeverInTop5WhenCurrentIsHindi() {
        val current = createTrack(
            id = "1", title = "Tum Hi Ho", mood = "Romantic", artist = "Arijit Singh",
            year = 2020, language = "hindi"
        )

        // Punjabi candidate with perfect mood/acoustic scores — should be hard-filtered
        val punjabi = createTrack(
            id = "punjabi-1", title = "Romantic Punjabi Hit", mood = "Romantic", artist = "Arijit Singh",
            year = 2020, language = "punjabi"
        )

        // 5 Hindi candidates with mediocre scores to fill top 5
        val hindiCandidates = (2..6).map { i ->
            createTrack(
                id = "hindi-$i", title = "Hindi Song $i", mood = "Romantic", artist = "Unknown Artist $i",
                year = 2020, language = "hindi"
            )
        }

        val allCandidates = listOf(punjabi) + hindiCandidates
        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, allCandidates) }
        val sorted = results.sortedByDescending { it.totalScore }
        val top5Ids = sorted.take(5).map { it.trackId }

        assertFalse(
            "Punjabi candidate should never appear in top 5 when current track is Hindi",
            top5Ids.contains("punjabi-1")
        )

        // Verify the Punjabi candidate got hard-filtered with -9999 totalScore
        val punjabiScore = results.find { it.trackId == "punjabi-1" }!!
        assertEquals(-9999.0f, punjabiScore.totalScore, 0.001f)
    }

    @Test
    fun testLanguageHardFilter_whenLanguageIsNull() {
        val current = createTrack(
            id = "1", title = "Song A", mood = "Romantic", artist = "Unknown",
            year = 2020, language = null
        )
        val candidate = createTrack(
            id = "2", title = "Song B", mood = "Romantic", artist = "Unknown",
            year = 2020, language = "punjabi"
        )

        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(candidate)) }
        val score = results[0]

        // SHOULD be hard-filtered
        assertEquals(-9999.0f, score.languageConsistency, 0.001f)
        assertTrue("Score should be heavily penalized when language is unknown", score.totalScore < 0f)
    }

    @Test
    fun testEraHardFilter_noClusterMatch_excludedFromTop5() {
        // Current = year 2000, no acoustic cluster match with candidates
        val current = createTrack(
            id = "1", title = "Song 2000", mood = "Romantic", artist = "Unknown 1", year = 2000
        )

        // Candidate from 2006 (6 year gap, > 5), NO acoustic cluster match — should be hard-filtered
        val candidate2006 = createTrack(
            id = "era-2006", title = "Song 2006", mood = "Romantic", artist = "Unknown 2", year = 2006
        )

        // 5 candidates within 5-year window to fill top 5
        val nearCandidates = (1..5).map { i ->
            createTrack(
                id = "near-$i", title = "Song ${2000 + i}", mood = "Romantic", artist = "Unknown Near $i", year = 2000 + i
            )
        }

        val allCandidates = listOf(candidate2006) + nearCandidates
        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, allCandidates) }
        val sorted = results.sortedByDescending { it.totalScore }
        val top5Ids = sorted.take(5).map { it.trackId }

        assertFalse(
            "2006 candidate (no cluster match) should never appear in top 5 when current is 2000",
            top5Ids.contains("era-2006")
        )

        // Verify hard filter applied
        val score2006 = results.find { it.trackId == "era-2006" }!!
        assertEquals(-9999.0f, score2006.totalScore, 0.001f)
    }

    @Test
    fun testEraRelaxedWindow_withClusterMatch_allowed() {
        // Current = year 2000, Atif Aslam (in vocal_texture_1 cluster)
        val current = createTrack(
            id = "1", title = "Song 2000", mood = "Romantic", artist = "Atif Aslam", year = 2000
        )

        // Candidate from 2015 (15 year gap), KK (same vocal_texture_1 cluster) — should be allowed
        val clusterCandidate2015 = createTrack(
            id = "cluster-2015", title = "Song 2015", mood = "Romantic", artist = "KK", year = 2015
        )

        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(clusterCandidate2015)) }
        val score = results[0]

        // Should NOT be hard-filtered — acoustic cluster match allows up to 15 years
        assertTrue(
            "Cluster-matched candidate from 2015 should not be hard-filtered (totalScore=${score.totalScore})",
            score.totalScore > -9999.0f
        )
        // Era proximity should have a proportional penalty but not -9999
        assertTrue(
            "Era proximity should be penalized but not hard-filtered (eraProximity=${score.eraProximity})",
            score.eraProximity > -9999.0f && score.eraProximity < 1.0f
        )
    }

    // ═══════════════════════════════════════════════
    // Devotional Content Filter Tests
    // ═══════════════════════════════════════════════

    @Test
    fun testDevotionalFilter_hanumanChalisa_excluded() {
        val result = DevotionalContentFilter.shouldExclude(
            title = "Hanuman Chalisa",
            album = "Devotional Hits",
            artist = "Hariharan",
            moodGenreTag = "neutral"
        )
        assertTrue("Track titled 'Hanuman Chalisa' should be excluded", result)
    }

    @Test
    fun testDevotionalFilter_devotionalMoodTag_excluded() {
        val result = DevotionalContentFilter.shouldExclude(
            title = "Some Random Song",
            album = "Some Album",
            artist = "Some Artist",
            moodGenreTag = "devotional"
        )
        assertTrue("Track with moodGenreTag='devotional' should be excluded", result)
    }

    @Test
    fun testDevotionalFilter_bhajanMoodTag_excluded() {
        val result = DevotionalContentFilter.shouldExclude(
            title = "Some Song",
            album = "Some Album",
            artist = "Some Artist",
            moodGenreTag = "Bhajan"
        )
        assertTrue("Track with moodGenreTag='Bhajan' should be excluded", result)
    }

    @Test
    fun testDevotionalFilter_wholeWordMatching_noFalsePositive() {
        // "Krishnamurthy" should NOT match "Krishna" — whole-word matching required
        val result = DevotionalContentFilter.shouldExclude(
            title = "Song by Krishnamurthy",
            album = "Pop Hits",
            artist = "Krishnamurthy",
            moodGenreTag = "romantic"
        )
        assertFalse("'Krishnamurthy' should NOT trigger 'Krishna' match", result)
    }

    @Test
    fun testDevotionalFilter_normalTrack_notExcluded() {
        val result = DevotionalContentFilter.shouldExclude(
            title = "Tum Hi Ho",
            album = "Aashiqui 2",
            artist = "Arijit Singh",
            moodGenreTag = "romantic"
        )
        assertFalse("Normal romantic track should NOT be excluded", result)
    }

    @Test
    fun testDevotionalFilter_absentFromScoredOutput() {
        // End-to-end: devotional candidates should never appear in scored output
        val current = createTrack("1", "Love Song", "Romantic", "Arijit Singh")

        val devotionalByTitle = createTrack("d1", "Hanuman Chalisa", "devotional", "Hariharan")
        val devotionalByTag = createTrack("d2", "Peaceful Melody", "devotional", "Unknown")
        val normalCandidate = createTrack("n1", "Romantic Hit", "Romantic", "Unknown")

        // Filter candidates the same way MainViewModel does
        val pool = listOf(devotionalByTitle, devotionalByTag, normalCandidate)
        val filtered = pool.filter { candidate ->
            !DevotionalContentFilter.shouldExclude(candidate)
        }

        // Score only filtered candidates
        val results = kotlinx.coroutines.runBlocking { engine.scoreNext(current, filtered) }

        assertFalse(
            "Devotional track by title should be absent from scored output",
            results.any { it.trackId == "d1" }
        )
        assertFalse(
            "Devotional track by tag should be absent from scored output",
            results.any { it.trackId == "d2" }
        )
        assertTrue(
            "Normal candidate should be present in scored output",
            results.any { it.trackId == "n1" }
        )
    }

    @Test
    fun testCanonicalSongDeduplication() {
        // Pool contains "Tum Hi Ho", "Tum Hi Ho (Lofi Mix)", and "Tum Hi Ho (Acoustic)"
        val current = createTrack("1", "Some Song", "Romantic", "Unknown")
        
        val base = createTrack("t1", "Tum Hi Ho", "Romantic", "Arijit Singh")
        val lofi = createTrack("t2", "Tum Hi Ho (Lofi Mix)", "Romantic", "Arijit Singh")
        val acoustic = createTrack("t3", "Tum Hi Ho [Acoustic]", "Romantic", "Arijit Singh")
        val normal = createTrack("n1", "Different Song", "Romantic", "Arijit Singh")

        // 1. Assert only ONE version of Tum Hi Ho appears in the output
        val results1 = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(base, lofi, acoustic, normal)) }
        
        val tumHiHoCount = results1.count { it.trackId in listOf("t1", "t2", "t3") }
        assertEquals("Only one version of Tum Hi Ho should survive deduplication", 1, tumHiHoCount)
        assertEquals("Different song should also be present", 1, results1.count { it.trackId == "n1" })

        // 2. Register one version as played, assert ALL versions are excluded
        RecommendationSessionManager.registerPlayedTrack("t2", "Tum Hi Ho (Lofi Mix)", "Arijit Singh")
        
        val results2 = kotlinx.coroutines.runBlocking { engine.scoreNext(current, listOf(base, lofi, acoustic, normal)) }
        
        // They should all be considered "played" by the session manager
        assertTrue("Base version should be considered played", RecommendationSessionManager.isTrackPlayed("t1", "Tum Hi Ho", "Arijit Singh"))
        assertTrue("Lofi version should be considered played", RecommendationSessionManager.isTrackPlayed("t2", "Tum Hi Ho (Lofi Mix)", "Arijit Singh"))
        assertTrue("Acoustic version should be considered played", RecommendationSessionManager.isTrackPlayed("t3", "Tum Hi Ho [Acoustic]", "Arijit Singh"))
        assertFalse("Different song should NOT be considered played", RecommendationSessionManager.isTrackPlayed("n1", "Different Song", "Arijit Singh"))
    }

    @Test
    fun testCanonicalSongDedup_persistsAcrossAppRestart() {
        // 1. Compute the canonical key for "Tum Hi Ho" by Arijit Singh
        val canonicalKey = RecommendationSessionManager.getCanonicalSongKey("Tum Hi Ho", "Arijit Singh")
        
        // 2. Simulate app restart: fresh session, then prepopulate from "persisted DB"
        //    as if PlaybackHistoryEntity had canonicalSongKey stored from a previous session.
        RecommendationSessionManager.resetSession()
        RecommendationSessionManager.prepopulateHistory(
            trackIds = listOf("original-tum-hi-ho-id"),
            canonicalKeys = listOf(canonicalKey)
        )
        
        // 3. "Tum Hi Ho (Lofi Mix)" by same artist should be excluded because it
        //    normalizes to the same canonical key as "Tum Hi Ho"
        val lofiExcluded = RecommendationSessionManager.isTrackPlayed(
            "lofi-mix-id", "Tum Hi Ho (Lofi Mix)", "Arijit Singh"
        )
        assertTrue(
            "Tum Hi Ho (Lofi Mix) should be excluded because its canonicalSongKey matches persisted 'Tum Hi Ho'",
            lofiExcluded
        )
        
        // 4. Verify a completely different song is NOT excluded
        val differentSong = RecommendationSessionManager.isTrackPlayed(
            "different-id", "Channa Mereya", "Arijit Singh"
        )
        assertFalse(
            "A different song should NOT be excluded",
            differentSong
        )
        
        // 5. Verify other alternate versions are also excluded
        val acousticExcluded = RecommendationSessionManager.isTrackPlayed(
            "acoustic-id", "Tum Hi Ho [Acoustic]", "Arijit Singh"
        )
        assertTrue(
            "Tum Hi Ho [Acoustic] should also be excluded via canonical key",
            acousticExcluded
        )
    }
}
