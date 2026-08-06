package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val success: Boolean?,
    val data: SearchData?
)

@JsonClass(generateAdapter = true)
data class SearchData(
    val results: List<ApiSong>?
)

@JsonClass(generateAdapter = true)
data class ApiSong(
    val id: String,
    val name: String,
    val duration: String?, 
    val playCount: String?,
    val image: List<ApiImage>?,
    val downloadUrl: List<ApiDownloadUrl>?,
    val primaryArtists: String?,
    val album: ApiAlbum?
)

@JsonClass(generateAdapter = true)
data class ApiImage(
    val quality: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class ApiDownloadUrl(
    val quality: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class ApiAlbum(
    val id: String?,
    val name: String?,
    val url: String?
)
