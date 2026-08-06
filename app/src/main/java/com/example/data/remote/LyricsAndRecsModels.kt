package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Musixmatch Models
@JsonClass(generateAdapter = true)
data class MusixmatchResponse(
    val message: MusixmatchMessage?
)

@JsonClass(generateAdapter = true)
data class MusixmatchMessage(
    val body: MusixmatchBody?
)

@JsonClass(generateAdapter = true)
data class MusixmatchBody(
    val lyrics: MusixmatchLyrics?
)

@JsonClass(generateAdapter = true)
data class MusixmatchLyrics(
    @Json(name = "lyrics_body") val lyricsBody: String?
)


// Last.fm Models
@JsonClass(generateAdapter = true)
data class LastFmResponse(
    val similartracks: SimilarTracksData?
)

@JsonClass(generateAdapter = true)
data class SimilarTracksData(
    val track: List<LastFmTrack>?
)

@JsonClass(generateAdapter = true)
data class LastFmTrack(
    val name: String?,
    val artist: LastFmArtist?,
    val image: List<LastFmImage>?
)

@JsonClass(generateAdapter = true)
data class LastFmArtist(
    val name: String?
)

@JsonClass(generateAdapter = true)
data class LastFmImage(
    @Json(name = "#text") val url: String?,
    val size: String?
)
