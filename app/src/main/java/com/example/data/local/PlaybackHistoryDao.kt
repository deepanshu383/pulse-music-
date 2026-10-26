package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaybackHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistoryEntity)

    @Query("SELECT * FROM playback_history WHERE timeSegment = :segment ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistoryBySegment(segment: String, limit: Int = 10): List<PlaybackHistoryEntity>

    @Query("SELECT * FROM playback_history WHERE timestamp >= :timestamp")
    suspend fun getHistorySince(timestamp: Long): List<PlaybackHistoryEntity>

    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 20): List<PlaybackHistoryEntity>

    @Query("SELECT * FROM playback_history WHERE canonicalSongKey IN (:keys) AND timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getRecentHistoryByCanonicalKey(keys: List<String>, sinceTimestamp: Long): List<PlaybackHistoryEntity>
}
