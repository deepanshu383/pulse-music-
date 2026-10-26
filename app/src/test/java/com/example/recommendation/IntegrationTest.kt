package com.example.recommendation

import com.example.data.remote.NetworkModule
import com.example.data.repository.JioSaavnRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class IntegrationTest {

    private val engine = DefaultRecommendationEngine()
    private val jioSaavnRepository = JioSaavnRepository()

    private fun createTrack(
        id: String,
        title: String,
        artist: String,
        mood: String,
        language: String,
        year: Int
    ) = TrackFeatures(
        trackId = id,
        title = title,
        artist = artist,
        moodGenreTag = mood,
        language = language,
        releaseYear = year,
        acousticClusterId = "",
        playCount = 1,
        skipCount = 0,
        lastPlayedTimestamp = System.currentTimeMillis(),
        averageCompletionPercentage = 1.0f,
        userRating = null
    )

    private val apiIdCounter = AtomicInteger(1000)

    @Test
    fun runManualTestPlaylist() = runBlocking {
        println("==========================================================")
        println("          MANUAL TEST PLAYLIST VERIFICATION RUN           ")
        println("==========================================================")
        
        RecommendationSessionManager.resetSession()

        val testTracks = listOf(
            createTrack("t1", "Kala Chashma", "Badshah", "Party", "punjabi", 2016),
            createTrack("t2", "Tum Hi Ho", "Arijit Singh", "Romantic", "hindi", 2013),
            createTrack("t3", "Shape of You", "Ed Sheeran", "Pop", "english", 2017)
        )

        for (currentSong in testTracks) {
            println("\n\n>>> PLAYING: '${currentSong.title}' by ${currentSong.artist} (${currentSong.releaseYear}, ${currentSong.language}, ${currentSong.moodGenreTag})")
            
            // Simulate manual pick
            RecommendationSessionManager.registerManualPick(currentSong.moodGenreTag)
            RecommendationSessionManager.registerPlayedTrack(currentSong.trackId, currentSong.title, currentSong.artist)
            
            val lockedGroup = RecommendationSessionManager.getLockedMoodGroup()
            println(">>> SESSION LOCKED TO MOOD GROUP: $lockedGroup")
            
            // Phase A: Pool Assembly (Mirrors MainViewModel)
            val currentMood = currentSong.moodGenreTag
            var currentLang: String? = currentSong.language
            
            val moodLangCandidates = mutableListOf<TrackFeatures>()
            val moodOnlyCandidates = mutableListOf<TrackFeatures>()
            val artistCandidates = mutableListOf<TrackFeatures>()
            val lastFmCrossCandidates = mutableListOf<TrackFeatures>()

            // Fire searches
            val moodLangDeferred = async { jioSaavnRepository.searchSongsByMood(currentMood, currentLang, 25) }
            val moodOnlyDeferred = async { jioSaavnRepository.searchSongsByMood(currentMood, null, 25) }
            val artistDeferred = async { jioSaavnRepository.searchSongs(currentSong.artist) }
            val lastFmDeferred = async {
                try {
                    NetworkModule.lastFmApiService.getSimilarTracks(
                        track = currentSong.title,
                        artist = currentSong.artist,
                        apiKey = NetworkModule.lastFmApiKey
                    )
                } catch (e: Exception) { null }
            }

            // Await and process
            val artistApiSongs = artistDeferred.await().getOrNull() ?: emptyList()
            artistCandidates.addAll(artistApiSongs.map {
                val artistStr = it.artists?.primary?.mapNotNull { a -> a.name }?.joinToString(", ") ?: "Unknown"
                createTrack("api_${it.id}", it.name.replace("&quot;", "\""), artistStr, currentMood, it.language ?: "unknown", it.year?.toIntOrNull() ?: 2020)
            })

            val moodLangApiSongs = moodLangDeferred.await().getOrNull() ?: emptyList()
            moodLangCandidates.addAll(moodLangApiSongs.map {
                val artistStr = it.artists?.primary?.mapNotNull { a -> a.name }?.joinToString(", ") ?: "Unknown"
                val mood = MoodExtractionStrategy.getPrimaryMood(it.name, "", artistStr)
                createTrack("api_${it.id}", it.name.replace("&quot;", "\""), artistStr, mood, it.language ?: "unknown", it.year?.toIntOrNull() ?: 2020)
            })

            val moodOnlyApiSongs = moodOnlyDeferred.await().getOrNull() ?: emptyList()
            moodOnlyCandidates.addAll(moodOnlyApiSongs.map {
                val artistStr = it.artists?.primary?.mapNotNull { a -> a.name }?.joinToString(", ") ?: "Unknown"
                val mood = MoodExtractionStrategy.getPrimaryMood(it.name, "", artistStr)
                createTrack("api_${it.id}", it.name.replace("&quot;", "\""), artistStr, mood, it.language ?: "unknown", it.year?.toIntOrNull() ?: 2020)
            })

            val similarTracks = lastFmDeferred.await()?.similartracks?.track?.take(5) ?: emptyList()
            for (lfmTrack in similarTracks) {
                try {
                    val crossSongs = jioSaavnRepository.searchSongs("${lfmTrack.name} ${lfmTrack.artist?.name}").getOrNull()?.take(3) ?: emptyList()
                    lastFmCrossCandidates.addAll(crossSongs.map {
                        val artistStr = it.artists?.primary?.mapNotNull { a -> a.name }?.joinToString(", ") ?: "Unknown"
                        val mood = MoodExtractionStrategy.getPrimaryMood(it.name, "", artistStr)
                        createTrack("api_${it.id}", it.name.replace("&quot;", "\""), artistStr, mood, it.language ?: "unknown", it.year?.toIntOrNull() ?: 2020)
                    })
                } catch (e: Exception) {}
            }

            val mergedCandidates = mutableListOf<TrackFeatures>()
            val seenIds = mutableSetOf<String>()
            fun addUnique(songs: List<TrackFeatures>) {
                for (song in songs) {
                    if (mergedCandidates.size >= 50) break
                    if (seenIds.add(song.trackId)) mergedCandidates.add(song)
                }
            }

            addUnique(moodLangCandidates)
            addUnique(lastFmCrossCandidates)
            addUnique(moodOnlyCandidates)
            addUnique(artistCandidates)

            // Devotional hard filter
            val preFilterSize = mergedCandidates.size
            mergedCandidates.removeAll { DevotionalContentFilter.shouldExclude(it) }
            val removed = preFilterSize - mergedCandidates.size
            
            println("--- POOL COMPOSITION ---")
            val dist = mergedCandidates.groupingBy { it.moodGenreTag }.eachCount()
            println("total=${mergedCandidates.size} | ${dist.entries.sortedByDescending { it.value }.joinToString(", ") { "${it.key}=${it.value}" }}")
            println("DevotionalFilter: removed $removed devotional candidates from pool")

            // Score and rank
            var scores = engine.scoreNext(currentSong, mergedCandidates)
            scores = scores.map { it.copy(jitterBonus = (0..200).random() / 1000f) }.sortedByDescending { it.totalScore }

            println("\n--- TOP 10 CANDIDATE SCORES ---")
            scores.take(10).forEachIndexed { index, score ->
                val song = mergedCandidates.find { it.trackId == score.trackId }!!
                val hardFilters = mutableListOf<String>()
                if (score.languageConsistency <= -9999.0f) hardFilters.add("LANG")
                if (score.eraProximity <= -9999.0f) hardFilters.add("ERA")
                if (score.moodConsistency <= -9999.0f) hardFilters.add("MOOD")
                
                val filterStr = if (hardFilters.isNotEmpty()) " [BLOCKED: ${hardFilters.joinToString()}]" else ""
                val canonicalKey = RecommendationSessionManager.getCanonicalSongKey(song.title, song.artist)
                
                println("#${index + 1}: '${song.title}' (${song.language}, ${song.releaseYear}) -> Total: ${score.totalScore}$filterStr")
                println("    CanonicalKey: $canonicalKey")
                println("    Mood: ${score.moodConsistency} | Acoustic: ${score.acousticSimilarity} | Era: ${score.eraProximity}")
                println("    Lang: ${score.languageConsistency} | Pref: ${score.userPreference} | Fatigue: ${score.fatiguePenalty}")
            }
        }
    }
}
