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

    /** Fetch trending songs by searching a popular keyword */
    suspend fun getTrendingSongs(): Result<List<ApiSong>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchSongs(query = "trending", limit = 20)
            if (response.data?.results != null) {
                Result.success(response.data.results)
            } else {
                Result.failure(Exception("No trending songs found"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /** Fetch featured playlists by searching popular terms */
    suspend fun getFeaturedPlaylists(): Result<List<ApiPlaylistItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchPlaylists(query = "bollywood hits", limit = 20)
            if (response.data?.results != null) {
                Result.success(response.data.results)
            } else {
                Result.failure(Exception("No featured playlists found"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
