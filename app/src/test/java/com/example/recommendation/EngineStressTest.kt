package com.example.recommendation

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class EngineStressTest {

    private val idCounter = AtomicInteger(1000)

    private fun createTrack(
        title: String,
        artist: String,
        mood: String,
        language: String?,
        year: Int,
        clusterId: String = ""
    ): TrackFeatures {
        return TrackFeatures(
            trackId = "t${idCounter.incrementAndGet()}",
            title = title,
            artist = artist,
            moodGenreTag = mood,
            language = language,
            releaseYear = year,
            acousticClusterId = clusterId,
            playCount = 0,
            skipCount = 0,
            lastPlayedTimestamp = 0L,
            averageCompletionPercentage = 1.0f,
            userRating = null
        )
    }

    private fun generateSyntheticPool(): List<TrackFeatures> {
        val pool = mutableListOf<TrackFeatures>()

        val languages = listOf("hindi", "punjabi", "english")
        val moods = listOf("Party", "Romantic", "Sad", "Chill", "Energetic", "Lullaby")
        val artists = listOf("Arijit Singh", "Badshah", "Ed Sheeran", "Shreya Ghoshal", "Diljit Dosanjh", "Taylor Swift", "The Weeknd")

        // 1. Generate 100 normal tracks
        for (i in 1..100) {
            val lang = languages.random()
            val mood = moods.random()
            val artist = artists.random()
            val year = Random.nextInt(1990, 2025)
            pool.add(createTrack("Normal Track $i", artist, mood, lang, year, "cluster_${Random.nextInt(1, 5)}"))
        }

        // 2. Generate 15 multi-version tracks (45 tracks total)
        for (i in 1..15) {
            val lang = languages.random()
            val mood = moods.random()
            val artist = artists.random()
            val year = Random.nextInt(2010, 2025)
            val baseTitle = "Hit Song $i"
            val cluster = "cluster_${Random.nextInt(1, 5)}"

            pool.add(createTrack(baseTitle, artist, mood, lang, year, cluster))
            pool.add(createTrack("$baseTitle (Lofi Mix)", artist, mood, lang, year, cluster))
            pool.add(createTrack("$baseTitle [Remix]", artist, mood, lang, year, cluster))
        }

        // 3. Generate 15 devotional tracks
        val devotionalKeywords = listOf("Ram", "Krishna", "Shiva", "Bhajan", "Aarti", "Navratri")
        for (i in 1..15) {
            val lang = listOf("hindi", "punjabi").random()
            val keyword = devotionalKeywords.random()
            pool.add(createTrack("$keyword Song $i", "Unknown", "Devotional", lang, 2000))
        }

        return pool.shuffled()
    }

    @Test
    fun testStressSimulation() = runBlocking {
        val pool = generateSyntheticPool()

        // Select 20 different random seed tracks that are non-Romantic, non-English, and non-Devotional
        val seedTracks = pool.filter {
            !it.moodGenreTag.equals("Romantic", true) &&
            it.language != "english" &&
            !DevotionalContentFilter.shouldExclude(it)
        }.shuffled().take(20)

        var totalFailures = 0

        println("\n=========================================================================================")
        println(String.format("%-5s | %-20s | %-10s | %-15s | %-5s | %-10s", "RUN", "SEED TITLE", "LANGUAGE", "MOOD GROUP", "YEAR", "RESULT"))
        println("-----------------------------------------------------------------------------------------")

        for ((runIndex, seed) in seedTracks.withIndex()) {
            val engine = DefaultRecommendationEngine()
            RecommendationSessionManager.resetSession()

            var currentSong = seed
            RecommendationSessionManager.registerManualPick(currentSong.moodGenreTag)
            RecommendationSessionManager.registerPlayedTrack(currentSong.trackId, currentSong.title, currentSong.artist)

            var runFailed = false
            val errorLogs = mutableListOf<String>()

            val runHistory = mutableListOf<TrackFeatures>()
            runHistory.add(currentSong)

            for (step in 1..30) {
                // Pre-filter devotional as done in MainViewModel
                val filteredPool = pool.filter { !DevotionalContentFilter.shouldExclude(it) }

                // Exclude recent canonical tracks as done in MainViewModel
                val relaxedRecentCanonicalKeys = runHistory.takeLast(5).map {
                    RecommendationSessionManager.getCanonicalSongKey(it.title, it.artist)
                }.toSet()

                val validCandidates = filteredPool.filter { candidate ->
                    val canonicalKey = RecommendationSessionManager.getCanonicalSongKey(candidate.title, candidate.artist)
                    val notPlayed = !RecommendationSessionManager.isTrackPlayed(candidate.trackId, candidate.title, candidate.artist)
                    notPlayed && canonicalKey !in relaxedRecentCanonicalKeys && candidate.trackId != currentSong.trackId
                }

                // Fallback
                val candidatesToScore = validCandidates.ifEmpty {
                    filteredPool.filter { !RecommendationSessionManager.isTrackPlayed(it.trackId, it.title, it.artist) }
                }

                if (candidatesToScore.isEmpty()) {
                    errorLogs.add("Step $step: Pool exhausted!")
                    runFailed = true
                    break
                }

                val scores = engine.scoreNext(currentSong, candidatesToScore)
                val topScore = scores.firstOrNull()

                if (topScore == null) {
                    errorLogs.add("Step $step: Engine returned 0 candidates!")
                    runFailed = true
                    break
                }
                
                if (topScore.totalScore <= -9999.0f) {
                    // All remaining candidates in the synthetic pool are hard-blocked (exhausted valid tracks).
                    // This is expected in a small synthetic pool when playing a narrow niche (e.g. Punjabi Lullaby).
                    break
                }

                val nextSong = candidatesToScore.find { it.trackId == topScore.trackId }!!

                // --- ASSERTIONS ---
                val lockedGroup = RecommendationSessionManager.getLockedMoodGroup()
                val nextGroup = MoodExtractionStrategy.getMoodGroup(nextSong.moodGenreTag)

                // 1. Devotional Exclusion
                if (DevotionalContentFilter.shouldExclude(nextSong)) {
                    errorLogs.add("Step $step: Devotional track selected: ${nextSong.title}")
                    runFailed = true
                }

                // 2. Language Consistency
                if (currentSong.language != null && nextSong.language != null && currentSong.language != nextSong.language) {
                    errorLogs.add("Step $step: Language jumped from ${currentSong.language} to ${nextSong.language}")
                    runFailed = true
                }

                // 3. Era Consistency
                val yearGap = Math.abs(currentSong.releaseYear - nextSong.releaseYear)
                val hasAcousticMatch = (currentSong.acousticClusterId.isNotBlank() && currentSong.acousticClusterId == nextSong.acousticClusterId) || 
                                       ArtistSimilarityEngine.calculateAcousticSimilarity(currentSong.artist, nextSong.artist) >= 0.8f
                
                if (yearGap > 5 && !hasAcousticMatch) {
                    errorLogs.add("Step $step: Era jumped by $yearGap years without acoustic match")
                    runFailed = true
                }

                // 4. Mood Drift
                if (nextGroup != lockedGroup) {
                    errorLogs.add("Step $step: Mood drifted to $nextGroup while locked to $lockedGroup")
                    runFailed = true
                }

                // 5. Canonical Key Repeat within 15 tracks
                val nextCanonical = RecommendationSessionManager.getCanonicalSongKey(nextSong.title, nextSong.artist)
                val recent15Canonical = runHistory.takeLast(15).map { RecommendationSessionManager.getCanonicalSongKey(it.title, it.artist) }
                if (nextCanonical in recent15Canonical) {
                    errorLogs.add("Step $step: Canonical key $nextCanonical repeated within 15 tracks")
                    runFailed = true
                }

                // 6. Exact TrackId Repeat within 20 tracks
                val recent20Ids = runHistory.takeLast(20).map { it.trackId }
                if (nextSong.trackId in recent20Ids) {
                    errorLogs.add("Step $step: Exact trackId ${nextSong.trackId} repeated within 20 tracks")
                    runFailed = true
                }

                if (runFailed) {
                    errorLogs.add("--- FULL SCORE BREAKDOWN FOR STEP $step ---")
                    scores.take(10).forEachIndexed { i, s ->
                        val c = candidatesToScore.find { it.trackId == s.trackId }!!
                        val blocked = mutableListOf<String>()
                        if (s.languageConsistency <= -9999f) blocked.add("LANG")
                        if (s.eraProximity <= -9999f) blocked.add("ERA")
                        if (s.moodConsistency <= -9999f) blocked.add("MOOD")
                        val bStr = if (blocked.isNotEmpty()) " [BLOCKED: ${blocked.joinToString()}]" else ""
                        errorLogs.add("  #${i+1}: '${c.title}' (Lang: ${c.language}, Era: ${c.releaseYear}, Mood: ${c.moodGenreTag}) -> Total: ${s.totalScore}$bStr")
                    }
                    break
                }

                currentSong = nextSong
                RecommendationSessionManager.registerAutoPick(currentSong.moodGenreTag)
                RecommendationSessionManager.registerPlayedTrack(currentSong.trackId, currentSong.title, currentSong.artist)
                runHistory.add(currentSong)
            }

            if (runFailed) {
                totalFailures++
                println(String.format("%-5d | %-20s | %-10s | %-15s | %-5d | FAIL", runIndex + 1, seed.title.take(20), seed.language ?: "null", MoodExtractionStrategy.getMoodGroup(seed.moodGenreTag), seed.releaseYear))
                errorLogs.forEach { println("   -> $it") }
            } else {
                println(String.format("%-5d | %-20s | %-10s | %-15s | %-5d | PASS", runIndex + 1, seed.title.take(20), seed.language ?: "null", MoodExtractionStrategy.getMoodGroup(seed.moodGenreTag), seed.releaseYear))
            }
        }
        println("=========================================================================================\n")

        assertEquals("Total failures in stress test should be 0", 0, totalFailures)
    }
}
