package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface MusixmatchApiService {
    @GET("matcher.lyrics.get")
    suspend fun getLyrics(
        @Query("q_track") track: String,
        @Query("q_artist") artist: String,
        @Query("apikey") apiKey: String = "PLACEHOLDER_MUSIXMATCH_KEY" // Will be updated
    ): MusixmatchResponse
}

interface LastFmApiService {
    @GET("?method=track.getsimilar&format=json")
    suspend fun getSimilarTracks(
        @Query("track") track: String,
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String = "PLACEHOLDER_LASTFM_KEY" // Will be updated
    ): LastFmResponse

    @GET("?method=track.getInfo&format=json")
    suspend fun getTrackInfo(
        @Query("track") track: String,
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String = "PLACEHOLDER_LASTFM_KEY" // Will be updated
    ): LastFmTrackInfoResponse
}
