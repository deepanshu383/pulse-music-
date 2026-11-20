package com.example.data.repository

import com.example.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JioSaavnRepository {
    private val apiService = NetworkModule.jioSaavnApiService

    suspend fun searchSongs(query: String): Result<List<ApiSong>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchSongs(query)
            if (response.data?.results != null) {
                Result.success(response.data.results)
            } else {
                Result.failure(Exception("No results found"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun searchPlaylists(query: String): Result<List<ApiPlaylistItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchPlaylists(query)
            if (response.data?.results != null) {
                Result.success(response.data.results)
            } else {
                Result.failure(Exception("No playlist results found"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun searchArtists(query: String): Result<List<ApiArtistSearchItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchArtists(query)
            if (response.data?.results != null) {
                Result.success(response.data.results)
            } else {
                Result.failure(Exception("No artist results found"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getArtistDetails(artistId: String): Result<ApiArtistDetail> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getArtistDetails(artistId)
            if (response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("Artist details not found"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getPlaylistDetails(playlistId: String): Result<ApiOnlinePlaylistDetail> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPlaylistDetails(playlistId)
            if (response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("Playlist details not found"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getSongDetails(songId: String): Result<List<ApiSong>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSongDetails(songId)
            if (response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("Song details not found"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private val trendingQueryPool = listOf(
        "Top 50 Hindi Hits",
        "Trending Hindi Hits 2025",
        "Latest Punjabi Pop Hits",
        "Bollywood Top Chartbusters",
        "Arijit Singh Latest Hits",
        "Viral Instagram Reels Hindi",
        "Romantic Melodies Bollywood",
        "Desi Hip Hop Hits",
        "Superhit Punjabi 2025",
        "New Hindi Songs 2025",
        "Top Indian Pop Hits"
    )

    private val featuredPlaylistPool = listOf(
        "Top Hindi Hits",
        "Punjabi Chartbusters",
        "Bollywood Romance",
        "Acoustic Hindi",
        "Chill Lounge Bollywood",
        "Party Mashup Hindi",
        "Soulful Sufi Hits",
        "90s Romantic Hits"
    )

    /** Fetch trending songs deterministically for the current 1-hour time window */
    suspend fun getTrendingSongs(
        hourSeed: Long = System.currentTimeMillis() / 3_600_000L
    ): Result<List<ApiSong>> = withContext(Dispatchers.IO) {
        try {
            val idx1 = ((hourSeed % trendingQueryPool.size) + trendingQueryPool.size).toInt() % trendingQueryPool.size
            val idx2 = (idx1 + 3) % trendingQueryPool.size
            val query1 = trendingQueryPool[idx1]
            val query2 = trendingQueryPool[idx2]

            val res1 = try { apiService.searchSongs(query = query1, limit = 20).data?.results } catch (e: Exception) { null }
            val res2 = try { apiService.searchSongs(query = query2, limit = 20).data?.results } catch (e: Exception) { null }

            val combined = mutableListOf<ApiSong>()
            if (!res1.isNullOrEmpty()) combined.addAll(res1)
            if (!res2.isNullOrEmpty()) combined.addAll(res2)

            if (combined.isNotEmpty()) {
                val distinctSongs = combined.distinctBy { it.id.ifBlank { it.name } }
                val rng = kotlin.random.Random(hourSeed)
                Result.success(distinctSongs.shuffled(rng).take(25))
            } else {
                val fallback = apiService.searchSongs(query = "latest hindi hits", limit = 25)
                if (fallback.data?.results != null) {
                    Result.success(fallback.data.results)
                } else {
                    Result.failure(Exception("No trending songs found"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /** Search songs by mood/genre tag, optionally filtered by language */
    suspend fun searchSongsByMood(mood: String, language: String? = null, limit: Int = 25): Result<List<ApiSong>> = withContext(Dispatchers.IO) {
        try {
            val query = if (!language.isNullOrBlank()) "$mood $language songs" else "$mood songs"
            val response = apiService.searchSongs(query, limit)
            if (response.data?.results != null) {
                Result.success(response.data.results)
            } else {
                Result.failure(Exception("No mood-based results found"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /** Fetch featured playlists deterministically for the current 1-hour time window */
    suspend fun getFeaturedPlaylists(
        hourSeed: Long = System.currentTimeMillis() / 3_600_000L
    ): Result<List<ApiPlaylistItem>> = withContext(Dispatchers.IO) {
        try {
            val idx = ((hourSeed % featuredPlaylistPool.size) + featuredPlaylistPool.size).toInt() % featuredPlaylistPool.size
            val query = featuredPlaylistPool[idx]
            val response = apiService.searchPlaylists(query = query, limit = 15)
            if (response.data?.results != null && response.data.results.isNotEmpty()) {
                val rng = kotlin.random.Random(hourSeed + 997L)
                Result.success(response.data.results.shuffled(rng))
            } else {
                val fallback = apiService.searchPlaylists(query = "top bollywood hits", limit = 15)
                if (fallback.data?.results != null) {
                    Result.success(fallback.data.results)
                } else {
                    Result.failure(Exception("No featured playlists found"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
