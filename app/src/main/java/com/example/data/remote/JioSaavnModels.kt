package com.example.data.remote

import com.squareup.moshi.JsonClass

// ═══════════════════════════════════════════════
// Song Search Models (existing)
// ═══════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val success: Boolean?,
    val data: SearchData?
)

@JsonClass(generateAdapter = true)
data class SearchData(
    val total: Int?,
    val start: Int?,
    val results: List<ApiSong>?
)

@JsonClass(generateAdapter = true)
data class ApiSong(
    val id: String,
    val name: String,
    val type: String?,
    val year: String?,
    val releaseDate: String?,
    val duration: Int?,          // Duration in SECONDS (from the API)
    val label: String?,
    val explicitContent: Boolean?,
    val playCount: Int?,
    val language: String?,
    val hasLyrics: Boolean?,
    val lyricsId: String?,
    val url: String?,
    val copyright: String?,
    val album: ApiAlbum?,
    val artists: ApiArtists?,
    val image: List<ApiImage>?,
    val downloadUrl: List<ApiDownloadUrl>?
)

@JsonClass(generateAdapter = true)
data class ApiAlbum(
    val id: String?,
    val name: String?,
    val url: String?
)

@JsonClass(generateAdapter = true)
data class ApiArtists(
    val primary: List<ApiArtistItem>?,
    val featured: List<ApiArtistItem>?,
    val all: List<ApiArtistItem>?
)

@JsonClass(generateAdapter = true)
data class ApiArtistItem(
    val id: String?,
    val name: String,
    val role: String?,
    val type: String?,
    val image: List<ApiImage>?,
    val url: String?
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

// ═══════════════════════════════════════════════
// Playlist Search Models
// ═══════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class PlaylistSearchResponse(
    val success: Boolean?,
    val data: PlaylistSearchData?
)

@JsonClass(generateAdapter = true)
data class PlaylistSearchData(
    val total: Int?,
    val start: Int?,
    val results: List<ApiPlaylistItem>?
)

@JsonClass(generateAdapter = true)
data class ApiPlaylistItem(
    val id: String,
    val name: String,
    val type: String?,
    val image: List<ApiImage>?,
    val url: String?,
    val songCount: Int?,
    val language: String?,
    val explicitContent: Boolean?
)

// ═══════════════════════════════════════════════
// Artist Search Models
// ═══════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class ArtistSearchResponse(
    val success: Boolean?,
    val data: ArtistSearchData?
)

@JsonClass(generateAdapter = true)
data class ArtistSearchData(
    val total: Int?,
    val start: Int?,
    val results: List<ApiArtistSearchItem>?
)

@JsonClass(generateAdapter = true)
data class ApiArtistSearchItem(
    val id: String,
    val name: String,
    val role: String?,
    val image: List<ApiImage>?,
    val type: String?,
    val url: String?
)

// ═══════════════════════════════════════════════
// Artist Detail Models
// ═══════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class ArtistDetailResponse(
    val success: Boolean?,
    val data: ApiArtistDetail?
)

@JsonClass(generateAdapter = true)
data class ApiArtistDetail(
    val id: String,
    val name: String,
    val url: String?,
    val type: String?,
    val followerCount: Long?,
    val fanCount: String?,
    val isVerified: Boolean?,
    val dominantLanguage: String?,
    val dominantType: String?,
    val bio: List<ApiArtistBio>?,
    val image: List<ApiImage>?,
    val topSongs: List<ApiSong>?,
    val topAlbums: List<ApiAlbumItem>?,
    val singles: List<ApiSong>?,
    val availableLanguages: List<String>?
)

@JsonClass(generateAdapter = true)
data class ApiArtistBio(
    val text: String?,
    val title: String?,
    val sequence: Int?
)

@JsonClass(generateAdapter = true)
data class ApiAlbumItem(
    val id: String?,
    val name: String?,
    val year: String?,
    val type: String?,
    val image: List<ApiImage>?,
    val url: String?,
    val language: String?
)

// ═══════════════════════════════════════════════
// Online Playlist Detail Models
// ═══════════════════════════════════════════════

@JsonClass(generateAdapter = true)
data class OnlinePlaylistDetailResponse(
    val success: Boolean?,
    val data: ApiOnlinePlaylistDetail?
)

@JsonClass(generateAdapter = true)
data class ApiOnlinePlaylistDetail(
    val id: String,
    val name: String,
    val description: String?,
    val type: String?,
    val year: String?,
    val songCount: Int?,
    val language: String?,
    val url: String?,
    val image: List<ApiImage>?,
    val songs: List<ApiSong>?
)
