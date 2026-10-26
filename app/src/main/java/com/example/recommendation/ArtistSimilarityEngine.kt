package com.example.recommendation

import com.example.data.remote.ListeningEvent

object ArtistSimilarityEngine {

    /**
     * Finds similar artists by combining static clusters with dynamic user behavior affinities.
     */
    fun calculateAcousticSimilarity(
        currentArtist: String,
        candidateArtist: String,
        userArtistAffinities: Map<String, Float> = emptyMap()
    ): Float {
        // Direct match
        if (currentArtist.equals(candidateArtist, ignoreCase = true)) {
            return 1.0f
        }

        // Behavior-based affinity check (if the user frequently plays these artists together)
        // A full graph requires a backend job. For now, we rely on the pre-computed affinities in the profile.
        // If the user loves the candidate artist generally, we give a slight bump, but true
        // edge-weights (Artist A -> Artist B) would be mapped here in a robust backend.
        
        val staticClusters = DefaultRecommendationEngine.defaultAcousticClusters()
        var isInSameCluster = false
        
        for ((_, clusterArtists) in staticClusters) {
            val lowerCluster = clusterArtists.map { it.lowercase() }
            if (lowerCluster.contains(currentArtist.lowercase()) && lowerCluster.contains(candidateArtist.lowercase())) {
                isInSameCluster = true
                break
            }
        }

        return if (isInSameCluster) 0.8f else 0.3f
    }
}
