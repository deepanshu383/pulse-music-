package com.example.recommendation

import java.util.concurrent.ConcurrentHashMap

/**
 * Manages in-memory state for the current recommendation session.
 * Used to track early skips, played songs, and apply real-time penalties.
 */
object RecommendationSessionManager {
    // Stores trackIds that were skipped early
    private val skippedTracks = ConcurrentHashMap.newKeySet<String>()
    
    // Stores accumulated penalty per artist based on early skips
    private val artistPenalties = ConcurrentHashMap<String, Float>()

    // Stores ALL trackIds that were recommended/played in this session (never re-recommend)
    private val playedTrackIds = ConcurrentHashMap.newKeySet<String>()
    
    // Stores canonical keys (title + artist without version suffixes) to prevent repeating alternate versions
    private val playedCanonicalKeys = ConcurrentHashMap.newKeySet<String>()
    
    // Anti-drift: locks the session to the initial manually picked mood group
    @Volatile
    private var lockedMoodGroup: String? = null
    
    @Volatile
    private var consecutiveAutoPicks = 0

    /**
     * Derives a canonical key for a song by stripping out version strings and appending the artist.
     */
    fun getCanonicalSongKey(title: String, artist: String): String {
        val versionRegex = Regex("(?i)\\(.*mix.*\\)|\\[.*mix.*\\]|\\(.*remix.*\\)|\\[.*remix.*\\]|\\(.*acoustic.*\\)|\\[.*acoustic.*\\]|\\(.*instrumental.*\\)|\\(.*slowed.*\\)|\\(.*reverb.*\\)|\\(.*reprise.*\\)|-.*reprise.*")
        val strippedTitle = title.replace(versionRegex, "").replace(Regex("[^a-zA-Z0-9]"), "").trim().lowercase()
        val strippedArtist = artist.replace(Regex("[^a-zA-Z0-9]"), "").trim().lowercase()
        return "$strippedTitle|$strippedArtist"
    }

    /**
     * Call this when a recommended track is skipped early (e.g. < 15 seconds).
     */
    fun registerEarlySkip(trackId: String, artist: String) {
        skippedTracks.add(trackId)
        val currentPenalty = artistPenalties[artist] ?: 0.0f
        // Apply -0.20 to that artist's affinity
        artistPenalties[artist] = currentPenalty - 0.20f
    }

    /**
     * Register a track as played/enqueued in this session.
     * It will be hard-excluded from future recommendations.
     */
    fun registerPlayedTrack(trackId: String, title: String, artist: String) {
        playedTrackIds.add(trackId)
        playedCanonicalKeys.add(getCanonicalSongKey(title, artist))
    }

    /**
     * Checks if a track (or its canonical version) was already played/enqueued in this session.
     */
    fun isTrackPlayed(trackId: String, title: String, artist: String): Boolean {
        return playedTrackIds.contains(trackId) || playedCanonicalKeys.contains(getCanonicalSongKey(title, artist))
    }

    /**
     * Checks if a track was skipped in the current session.
     */
    fun isTrackSkipped(trackId: String): Boolean {
        return skippedTracks.contains(trackId)
    }

    /**
     * Gets the accumulated affinity penalty for an artist.
     */
    fun getArtistPenalty(artist: String): Float {
        return artistPenalties[artist] ?: 0.0f
    }

    /**
     * Returns the count of played tracks in this session.
     */
    fun getPlayedCount(): Int = playedTrackIds.size

    /**
     * Prepopulates the exclusion window with recently played tracks from the database on app start.
     */
    fun prepopulateHistory(trackIds: List<String>, canonicalKeys: List<String>) {
        playedTrackIds.addAll(trackIds)
        playedCanonicalKeys.addAll(canonicalKeys)
    }

    /**
     * Register a manual pick by the user. Locks the session mood to this track's mood group.
     */
    fun registerManualPick(mood: String) {
        lockedMoodGroup = MoodExtractionStrategy.getMoodGroup(mood)
        consecutiveAutoPicks = 0
    }

    /**
     * Register an auto-recommended pick. 
     * Auto-shift logic has been disabled to strictly enforce the manual mood lock.
     */
    fun registerAutoPick(mood: String) {
        val group = MoodExtractionStrategy.getMoodGroup(mood)
        if (group == lockedMoodGroup) {
            consecutiveAutoPicks = 0
        } else {
            consecutiveAutoPicks++
        }
    }

    /**
     * Gets the current locked mood group for the session.
     */
    fun getLockedMoodGroup(): String? = lockedMoodGroup

    /**
     * Resets the session state.
     */
    fun resetSession() {
        skippedTracks.clear()
        artistPenalties.clear()
        playedTrackIds.clear()
        playedCanonicalKeys.clear()
        lockedMoodGroup = null
        consecutiveAutoPicks = 0
    }
}
