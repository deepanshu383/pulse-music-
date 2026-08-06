package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface JioSaavnApiService {
    @GET("api/search/songs")
    suspend fun searchSongs(@Query("query") query: String): SearchResponse
}
