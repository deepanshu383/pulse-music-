package com.example.recommendation

import com.example.data.remote.ListeningEvent
import com.example.data.remote.SkipCategory
import com.example.data.remote.UserAffinityProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class WeightAnalyticsManager {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    suspend fun getUserAffinityProfile(): UserAffinityProfile {
        val uid = auth.currentUser?.uid ?: return UserAffinityProfile()
        
        return try {
            val snapshot = firestore.collection("user_affinity")
                .document(uid)
                .get()
                .await()
            
            if (snapshot.exists()) {
                snapshot.toObject(UserAffinityProfile::class.java) ?: UserAffinityProfile()
            } else {
                UserAffinityProfile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UserAffinityProfile()
        }
    }
    
    suspend fun analyzeAndAdjustWeights() {
        val uid = auth.currentUser?.uid ?: return
        
        try {
            // Get last 50 events
            val eventsSnapshot = firestore.collection("listening_events")
                .document(uid)
                .collection("events")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            
            val events = eventsSnapshot.toObjects(ListeningEvent::class.java)
            if (events.size < 30) return // Not enough data to make meaningful changes
            
            val profile = getUserAffinityProfile()
            
            // Calculate skip rates per recommendation reason
            val reasonStats = mutableMapOf<String, Pair<Int, Int>>() // Pair(total, skips)
            var totalCount = 0
            var totalSkips = 0
            
            events.forEach { event ->
                val isSkip = event.skipCategory == SkipCategory.INSTANT.name || 
                             event.skipCategory == SkipCategory.EARLY.name
                
                val currentStats = reasonStats[event.recommendationReason] ?: Pair(0, 0)
                reasonStats[event.recommendationReason] = Pair(
                    currentStats.first + 1,
                    currentStats.second + (if (isSkip) 1 else 0)
                )
                
                totalCount++
                if (isSkip) totalSkips++
            }
            
            val averageSkipRate = if (totalCount > 0) totalSkips.toFloat() / totalCount else 0f
            
            val updatedMultipliers = profile.factorMultipliers.toMutableMap()
            var weightsChanged = false
            
            // Analyze specific factors
            // We map generic reasons to factor multipliers
            // NOTE: DevotionalContentFilter is structurally isolated from this.
            // It is a hard pre-scoring boolean gate (0% tolerance), meaning no auto-tuned multiplier
            // changes here can ever bypass, weaken, or interact with the devotional exclusion.
            val reasonToFactor = mapOf(
                "Mood Match" to "moodConsistency",
                "Similar Artist" to "acousticSimilarity",
                "Highly Preferred" to "userPreference"
            )
            
            reasonStats.forEach { (reason, stats) ->
                if (stats.first >= 5) { // Minimum sample per reason
                    val skipRate = stats.second.toFloat() / stats.first
                    val factorName = reasonToFactor[reason]
                    
                    if (factorName != null && skipRate > (averageSkipRate + 0.15f)) {
                        // This factor is causing 15% more skips than average! Reduce it.
                        val currentMultiplier = updatedMultipliers[factorName] ?: 1.0f
                        val newMultiplier = (currentMultiplier - 0.1f).coerceAtLeast(0.5f)
                        if (newMultiplier != currentMultiplier) {
                            updatedMultipliers[factorName] = newMultiplier
                            weightsChanged = true
                        }
                    }
                }
            }
            
            if (weightsChanged) {
                // Determine exploration vs familiarity
                // Very rough heuristic: if they skip a lot generally, reduce exploration to be safe.
                // If they have very few skips, increase exploration to introduce new things.
                val explorationShift = when {
                    averageSkipRate > 0.4f -> -0.1f // Too many skips, play it safe
                    averageSkipRate < 0.1f -> 0.1f // Very few skips, try new things
                    else -> 0.0f
                }
                
                val newExploration = (profile.explorationMultiplier + explorationShift).coerceIn(0.1f, 1.0f)
                
                val updatedProfile = profile.copy(
                    factorMultipliers = updatedMultipliers,
                    explorationMultiplier = newExploration,
                    lastUpdated = System.currentTimeMillis()
                )
                
                firestore.collection("user_affinity")
                    .document(uid)
                    .set(updatedProfile)
                    .await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
