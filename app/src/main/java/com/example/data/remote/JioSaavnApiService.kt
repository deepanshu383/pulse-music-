package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JioSaavnApiService {
    @GET("api/search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("limit") limit: Int = 25
    ): SearchResponse

    @GET("api/search/playlists")
    suspend fun searchPlaylists(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20
    ): PlaylistSearchResponse

    @GET("api/search/artists")
    suspend fun searchArtists(
        @Query("query") query: String,
        @Query("limit") limit: Int = 10
    ): ArtistSearchResponse

    @GET("api/artists/{id}")
    suspend fun getArtistDetails(
        @Path("id") artistId: String
    ): ArtistDetailResponse

    @GET("api/playlists")
    suspend fun getPlaylistDetails(
        @Query("id") playlistId: String,
        @Query("limit") limit: Int = 50
    ): OnlinePlaylistDetailResponse
}
