package com.example.data.repository

import com.example.data.remote.ApiSong
import com.example.data.remote.NetworkModule
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
}
