# 🎵 Pulse Music

<div align="center">

![Pulse Music Banner](https://raw.githubusercontent.com/deepanshu383/pulse-music-/main/art/banner.png)

**A high-fidelity native Android music streaming and local player built with Android Studio, Jetpack Compose, Material Design 3, Media3 (ExoPlayer), and an intelligent context-aware recommendation engine.**

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2024%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Audio](https://img.shields.io/badge/Engine-AndroidX%20Media3%20ExoPlayer-E53935?style=for-the-badge)](https://developer.android.com/media/media3)
[![Cloud](https://img.shields.io/badge/Backend-Firebase%20Auth%20%26%20Firestore-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com)

</div>

---

## 📖 Table of Contents
1. [Overview](#-overview)
2. [Key Features](#-key-features)
3. [Architecture & Design Pattern](#-architecture--design-pattern)
4. [Data Flow Diagrams](#-data-flow-diagrams)
   - [Local Media Scanning Flow](#1-local-media-scanning-flow)
   - [Online Streaming & Search Flow](#2-online-search--streaming-flow)
   - [Auto-Play & Recommendation Pipeline Flow](#3-auto-play-next--recommendation-pipeline-flow)
   - [Playlist & Firebase Cloud Sync Flow](#4-playlist--cloud-sync-flow)
   - [Offline Song Download Flow](#5-offline-song-download-flow)
5. [Recommendation Algorithm & Methods](#-recommendation-algorithm--methods)
   - [Multi-Factor Scoring Engine](#multi-factor-scoring-engine)
   - [Hard Pre-Scoring Exclusion Filters](#hard-pre-scoring-exclusion-filters)
   - [Anti-Drift Mood Locking](#anti-drift-mood-locking)
   - [Dynamic User Affinity Analytics](#dynamic-user-affinity-analytics)
6. [API Integrations](#-api-integrations)
7. [Core Classes & Key Functions](#-core-classes--key-functions)
8. [Database Schema (Room & Firestore)](#-database-schema)
9. [Project Directory Structure](#-project-directory-structure)
10. [Setup & Running Locally](#-setup--running-locally)

---

## 🌟 Overview

**Pulse Music** is a native Android music application created in **Android Studio**. It harmoniously combines local audio file playback (via Storage Access Framework & MediaStore) with high-res cloud streaming and downloading (powered by JioSaavn, Last.fm, and Musixmatch).

Unlike generic music players, Pulse Music incorporates a custom-built, anti-drift **Recommendation & Next-Song Prediction Engine** that guarantees seamless genre, language, era, and acoustic continuity without abrupt mood jumps or repetitive cycles.

---

## 🚀 Key Features

- **Fluid Modern UI**: Built 100% with Jetpack Compose, Material Design 3, dynamic fluid animations, dark glassmorphic styling, and edge-to-edge rendering.
- **Hybrid Player (Local + Cloud)**:
  - Scans and indexes local audio folders (`.mp3`, `.m4a`, `.flac`, `.wav`, `.ogg`, `.opus`) with `DocumentFile` and Android `MediaStore`.
  - Searches and streams millions of online tracks, artists, and playlists.
- **Robust Background Playback**:
  - Implements `AndroidX Media3` (`ExoPlayer`, `MediaSession`, `MediaSessionService`).
  - System notification media controls, lock screen metadata, audio focus management, and headphone unplug disconnect handling.
- **Intelligent Next-Song Prediction**:
  - Prevents session drift by locking the mood to the user's manual selection.
  - Multi-factor scoring (30% Mood, 20% Era, 25% Language, 10% Acoustics, 10% Category, 5% Time of Day).
  - Whole-word zero-tolerance Devotional/Bhajan content exclusion filter.
  - Anti-fatigue 7-day play history penalty and canonical song version deduplication.
- **Cloud Playlist Sync & Authentication**:
  - Sign in with Google via Android Credential Manager & Firebase Authentication.
  - Cross-device bi-directional sync with Cloud Firestore.
- **Queue Management**: Drag-to-reorder, swipe/delete with a 5-step undo stack, queue repeat, and auto-queue persistence.
- **Offline Downloader**: Integrated Android `DownloadManager` saves tracks directly into public Music storage and indexes them in Room DB for offline listening.
- **Lyrics & Context**: Real-time track lyrics via Musixmatch and artist metadata from Last.fm.

---

## 🏛️ Architecture & Design Pattern

The application follows the official **Android Clean Architecture (MVVM - Model-View-ViewModel)** guidelines with unidirectional data flow (UDF):

```
┌────────────────────────────────────────────────────────┐
│                   UI Layer (Compose)                   │
│   MainScreen, PlayerScreen, HomeScreen, LibraryScreen  │
└───────────────────────────▲────────────────────────────┘
                            │ (StateFlow / Events)
┌───────────────────────────▼────────────────────────────┐
│                    ViewModel Layer                     │
│                     MainViewModel                      │
└───────┬───────────────────┬───────────────────┬────────┘
        │                   │                   │
┌───────▼────────┐  ┌───────▼────────┐  ┌───────▼────────┐
│ Recommendation │  │ Media & Audio  │  │ Repository /   │
│ Engine & State │  │ Service Layer  │  │ Data Layer     │
│ • DefaultEngine│  │ • MusicService │  │ • MusicRepo    │
│ • Devotional   │  │ • Controller   │  │ • JioSaavnRepo │
│ • SessionMgr   │  │ • Downloader   │  │ • PlaylistRepo │
└────────────────┘  └────────────────┘  └───────┬────────┘
                                                │
                    ┌───────────────────────────┴──────────┐
                    ▼                                      ▼
        ┌───────────────────────┐              ┌───────────────────────┐
        │  Local Storage Layer  │              │  Remote Network Layer │
        │ • Room Database (SQLite)│              │ • JioSaavn REST API   │
        │ • Preferences DataStore│              │ • Last.fm REST API    │
        │ • MediaStore / SAF    │              │ • Musixmatch API      │
        │                       │              │ • Firebase Firestore  │
        └───────────────────────┘              └───────────────────────┘
```

---

## 🔄 Data Flow Diagrams

### 1. Local Media Scanning Flow
```
User Selects Folder (SAF / MediaStore)
        │
        ▼
UserPreferencesRepository.saveFolderUri()
        │
        ▼
MusicRepository.scanMusicFiles(folderUri)
        │
        ├── DocumentFile.fromTreeUri() / MediaStore.Audio Query
        ├── MediaMetadataRetriever extracts Title, Artist, Album, Duration, Bitrate
        │
        ▼
SongDao.insertSongs(songs) [Room DB: song_metadata]
        │
        ▼
Flow<List<SongEntity>> updates MainViewModel.songs
        │
        ▼
Jetpack Compose LibraryScreen renders categorized songs
```

---

### 2. Online Search & Streaming Flow
```
User enters query in SearchScreen
        │
        ▼
MainViewModel.updateSearchQuery(query)  [400ms Debounce]
        │
        ├─► JioSaavnApiService.searchSongs(query)
        ├─► JioSaavnApiService.searchPlaylists(query)
        └─► JioSaavnApiService.searchArtists(query)
        │
        ▼
Moshi converts JSON to ApiSong / ApiPlaylistItem / ApiArtistItem
        │
        ▼
Map to SongEntity domain objects -> Update StateFlows
        │
        ▼
User taps track -> MainViewModel.playSong()
        │
        ▼
MusicController converts SongEntity -> Media3 MediaItem
        │
        ▼
ExoPlayer sets stream URI (160kbps / 320kbps MP4/AAC) & starts playback
        │
        ▼
MusicService displays Foreground Notification with media controls
```

---

### 3. Auto-Play Next & Recommendation Pipeline Flow
```
Now-playing track reaches the end of current playback queue
        │
        ▼
MainViewModel detects: currentQueueIndex == queue.size - 1
        │
        ▼
Step 1: Extract Features & Locks
        ├── Extract current song's Mood, Language, Era, and Artist
        └── Retrieve Session Locked Mood (RecommendationSessionManager)
        │
        ▼
Step 2: Candidate Pool Broadening (Multi-Source Parallel Fetch)
        ├── JioSaavn: searchSongsByMood(lockedMood, targetLang)
        ├── JioSaavn: searchSongs(currentArtist)
        ├── Last.fm: getSimilarTracks() -> Cross-query on JioSaavn
        └── Local Library: candidate songs from Room DB
        │
        ▼
Step 3: Pool Merge & Deduplication
        └── Merge results, deduplicate by filePath, cap candidate pool at 50 tracks
        │
        ▼
Step 4: Hard Pre-Scoring Exclusion Gates (Zero Tolerance)
        ├── DevotionalContentFilter: Hard tokenized filter against deity / bhajan keywords
        ├── Canonical Key Check: Strip version suffixes ("remix", "lofi") to eliminate repeats
        ├── Session Skip Check: Exclude tracks skipped in the active session
        └── Language & Mood Gate: Exclude conflicting languages or incompatible mood groups
        │
        ▼
Step 5: Composite Multi-Factor Scoring (DefaultRecommendationEngine)
        Score = (Mood * 0.30) + (Language * 0.25) + (Era * 0.20) + (Acoustics * 0.10)
                + (Category * 0.10) + (TimeOfDay * 0.05) + (UserPref * 0.10)
                - FatiguePenalty + JitterBonus + ExplorationBoost
        │
        ▼
Step 6: Winner Selection & Enqueue
        ├── Highest total score candidate selected
        ├── MediaController.addToQueue(winnerSong)
        ├── RecommendationSessionManager.registerAutoPick(winnerSong.mood)
        └── ListeningEventLogger.registerRecommendation(winnerSong.id, context)
```

---

### 4. Playlist & Cloud Sync Flow
```
User creates or modifies a playlist
        │
        ▼
PlaylistRepository.createPlaylist() / addSongToPlaylist()
        │
        ├── Room DB write (playlists & playlist_songs tables)
        │   └── Immediate local UI emission via Flow<List<PlaylistEntity>>
        │
        └── Check FirebaseAuth.currentUser
                │
                ├─► User is logged in:
                │     FirestorePlaylistRepository.pushPlaylist()
                │     Persists to: users/{uid}/playlists/{playlistId}
                │     (Includes full embedded song metadata and position ordering)
                │
                └─► User is guest:
                      Local storage only
```

---

### 5. Offline Song Download Flow
```
User clicks 'Download' icon on any online song
        │
        ▼
MainViewModel.downloadSong(song)
        │
        ▼
MusicDownloader creates DownloadManager.Request
        ├── Destination: Environment.DIRECTORY_MUSIC/PulseMusic/{Title} - {Artist}.mp3
        ├── Sets VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        └── Enqueues with system DownloadManager
        │
        ▼
BroadcastReceiver catches DownloadManager.ACTION_DOWNLOAD_COMPLETE
        │
        ▼
SongDao.insertSongs(song.copy(isDownloaded = true, localDownloadPath = downloadedPath))
        │
        ▼
Future play requests for this song automatically route to local file URI
```

---

## 🧠 Recommendation Algorithm & Methods

The core innovation of Pulse Music is its custom recommendation engine designed specifically to eliminate **pool drift** (where consecutive auto-plays drift into unintended genres like devotional or party tracks).

### Multi-Factor Scoring Engine
Located in [`DefaultRecommendationEngine.kt`](file:///app/src/main/java/com/example/recommendation/DefaultRecommendationEngine.kt):

| Factor | Weight | Method | Purpose |
| :--- | :--- | :--- | :--- |
| **Mood Consistency** | `30%` (`0.30f`) | `calculateMoodConsistency()` | Matches macro mood groups (`party`, `romantic`, `sad`, `neutral`). Prevents cross-group jumps. |
| **Language Consistency** | `25%` (`0.25f`) | `calculateLanguageConsistency()` | Enforces language continuity (Hindi, Punjabi, English, etc.) with soft affinity for Hindi ↔ Urdu. |
| **Era Proximity** | `20%` (`0.20f`) | `calculateEraProximity()` | Keeps songs within a ±5-year window. Up to ±15 years allowed only if acoustic similarity is high with steep linear decay. |
| **Acoustic Similarity** | `10%` (`0.10f`) | `calculateAcousticSimilarity()` | Compares artist vocal textures using static cluster mapping (`ArtistSimilarityEngine`). |
| **Category Consistency** | `10%` (`0.10f`) | `calculateCategoryConsistency()` | Ensures movie/soundtrack songs transition to other movie songs and independent singles stay with singles. |
| **Time of Day Fit** | `5%` (`0.05f`) | `calculateTimeOfDayFit()` | Boosts energetic tracks in morning/afternoon and ambient/chill tracks at night based on user habits. |
| **User Preference** | `10%` (`0.10f`) | Additive Modifier | Adds bonuses for cold-start selected artists and user rating. |
| **Jitter Bonus** | Stochastic | `(Random - 0.5) * 0.06f` | Sub-perceptual randomization preventing rigid, repetitive deterministic loops. |

---

### Hard Pre-Scoring Exclusion Filters

Before scoring begins, candidates must pass hard boolean filters:

1. **Devotional Content Filter (`DevotionalContentFilter`)**:
   - Zero tolerance (0%) exclusion for devotional tracks in secular listening sessions.
   - Uses whole-word regex tokenization to check `title`, `album`, `artist`, and API tags against deity names (`ram`, `krishna`, `shiva`, `hanuman`, `waheguru`, `vitthal`, etc.) and devotional forms (`bhajan`, `kirtan`, `aarti`, `chalisa`, `abhang`, `stotram`, `gurbani`).
   - Prevents false positives (e.g., `"Krishnamurthy"` will not trigger on `"Krishna"`).
2. **Canonical Song Key Deduplication (`RecommendationSessionManager`)**:
   - Strips version tags such as `(Remix)`, `(Acoustic)`, `[Lofi]`, `(Reprise)` to form a normalized key: `lowercase(title)|lowercase(artist)`.
   - Prevents the engine from re-recommending different versions of the same song in the same session.
3. **Session Replay & Skip Protection**:
   - Tracks all songs played during the current listening session and excludes them from being picked again.
   - Early skips (< 15 seconds) penalize the artist's affinity by `-0.20f`.
4. **Fatigue Scorer (`FatigueScorer`)**:
   - Queries Room `playback_history` for the last 7 days.
   - Subtracts a cumulative penalty of `-0.10f` for every time the song was played recently.

---

### Anti-Drift Mood Locking
In [`RecommendationSessionManager.kt`](file:///app/src/main/java/com/example/recommendation/RecommendationSessionManager.kt):
- When a user manually selects and plays a track, `registerManualPick(mood)` **locks the session mood group**.
- Subsequent auto-queued songs are strictly required to remain within that locked mood group. Auto-shift algorithms are disabled to ensure the vibe selected by the user never drifts.

---

### Dynamic User Affinity Analytics
In [`WeightAnalyticsManager.kt`](file:///app/src/main/java/com/example/recommendation/WeightAnalyticsManager.kt):
- Evaluates the user's last 50 listening events stored in Cloud Firestore.
- Computes skip ratios associated with specific recommendation reasons (`"Mood Match"`, `"Similar Artist"`, etc.).
- If a factor produces a skip rate >15% higher than average, its multiplier is decreased in real time.
- Adjusts the **Exploration vs. Familiarity** balance based on overall skipping tendencies.

---

## 🌐 API Integrations

Pulse Music communicates with multiple external web services via **Retrofit 2** and **Moshi**:

| Service | Base URL | Endpoints Used | Description |
| :--- | :--- | :--- | :--- |
| **JioSaavn API** | `https://jiosaavn-api-2s17.onrender.com/` | `GET api/search/songs`<br>`GET api/search/playlists`<br>`GET api/search/artists`<br>`GET api/artists/{id}`<br>`GET api/playlists?id={id}` | Core catalog for song search, trending songs, high-res audio streaming URLs, artist discography, and online playlist tracks. |
| **Last.fm API** | `https://ws.audioscrobbler.com/2.0/` | `?method=track.getsimilar`<br>`?method=track.getInfo` | Generates candidate similarity graphs, discovers related tracks, and extracts genre/language tags. |
| **Musixmatch API** | `https://api.musixmatch.com/ws/1.1/` | `GET matcher.lyrics.get` | Retrieves synchronized and plain text lyrics for display in the Now Playing screen. |
| **Firebase Auth** | `Google Play Services` | `CredentialManager` Google Sign-In | Secure user authentication and token exchange. |
| **Cloud Firestore** | Google Cloud | `users/{uid}/playlists`<br>`listening_events/{uid}/events`<br>`user_affinity/{uid}` | Real-time database for playlists, listening telemetry, and personalization profiles. |

---

## 💻 Core Classes & Key Functions

### 1. `MainViewModel.kt`
The central coordinator managing UI state, player controls, data persistence, and background tasks:
- `playSong(song: SongEntity, allSongs: List<SongEntity>)`: Starts playback, locks session mood, triggers lyrics & recommendations fetching.
- `updateSearchQuery(query: String)`: Debounced (400ms) parallel query across songs, playlists, and artists.
- `fetchHomeData()`: Parallel retrieval of featured playlists and trending songs for the Discover page.
- `scanMusic(folderUriString: String?)`: Initiates local audio file indexing through `MusicRepository`.
- `addSongToPlaylist(playlistId: Long, song: SongEntity)`: Adds song locally and triggers background Firestore sync.
- `downloadSong(song: SongEntity)`: Dispatches download job to `MusicDownloader`.
- `removeQueueItem(index: Int)` & `undoDelete()`: Manages player queue and 5-step LIFO undo history.

### 2. `MusicController.kt` & `MusicService.kt`
Wraps Media3 `MediaController` and `MediaSessionService`:
- `initialize(onReady: () -> Unit)`: Asynchronously binds to `MusicService` via `SessionToken`.
- `playSong(song: SongEntity, allSongs: List<SongEntity>, autoAddAll: Boolean)`: Sets ExoPlayer queue and initiates playback.
- `addToQueue()`, `insertQueueItem()`, `moveQueueItem()`, `removeQueueItem()`, `clearQueue()`: Manipulates active playback queue.
- `setShuffleMode(enabled: Boolean)` & `setRepeatMode(mode: Int)`: Manages player repeat/shuffle states.

### 3. `MusicDownloader.kt`
- `downloadSong(song: SongEntity, onDownloadComplete: (String) -> Unit)`: Sanitizes track names, enqueues request with Android `DownloadManager`, registers dynamic `BroadcastReceiver` for completion, and updates Room DB.

### 4. `PlaylistRepository.kt` & `FirestorePlaylistRepository.kt`
- `createPlaylist(name, description)`: Inserts into Room and pushes initial document to Firestore.
- `reorderSongs(playlistId, fromIndex, toIndex)`: Updates sequence positions locally and remotely.
- `syncFromFirestore()`: Pulls remote playlists on login and merges them into local Room database.

### 5. `DevotionalContentFilter.kt`
- `shouldExclude(title, album, artist, moodGenreTag, apiTags)`: Zero-tolerance boolean filter utilizing tokenized word boundary inspection against deity and devotional keywords.

---

## 🗄️ Database Schema

### Room Database (`pulse_music_db`)

```sql
-- Playlists Table
CREATE TABLE playlists (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    coverArtPath TEXT,
    createdDate INTEGER NOT NULL,
    modifiedDate INTEGER NOT NULL
);

-- Playlist-Song Cross-Reference Table
CREATE TABLE playlist_songs (
    playlistId INTEGER NOT NULL,
    songPath TEXT NOT NULL,
    position INTEGER NOT NULL,
    addedDate INTEGER NOT NULL,
    PRIMARY KEY(playlistId, songPath)
);

-- Cached Song Metadata Table (Local & Online)
CREATE TABLE song_metadata (
    filePath TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    album TEXT NOT NULL,
    duration INTEGER NOT NULL,
    bitrate INTEGER NOT NULL DEFAULT 0,
    albumArtPath TEXT,
    lastModified INTEGER NOT NULL,
    isOnline INTEGER NOT NULL DEFAULT 0,
    downloadUrl TEXT,
    isDownloaded INTEGER NOT NULL DEFAULT 0,
    localDownloadPath TEXT
);

-- Playback History Table (Fatigue & Habit Scoring)
CREATE TABLE playback_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    trackId TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    timeSegment TEXT NOT NULL, -- Morning, Afternoon, Evening, Night
    title TEXT NOT NULL DEFAULT '',
    artist TEXT NOT NULL DEFAULT '',
    canonicalSongKey TEXT NOT NULL DEFAULT ''
);

-- Search History Table
CREATE TABLE search_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL,        -- SONG, PLAYLIST, ARTIST
    referenceId TEXT NOT NULL,
    timestamp INTEGER NOT NULL
);
```

### Cloud Firestore Collections
- **`users/{uid}/playlists/{playlistId}`**: Stores playlist metadata and serialized song maps with index ordering.
- **`listening_events/{uid}/events/{eventId}`**: Stores listen duration, skip category (`INSTANT`, `EARLY`, `NORMAL`, `COMPLETED`), completion %, and factor scores.
- **`user_affinity/{uid}`**: Stores user-specific factor multipliers (`moodConsistency`, `acousticSimilarity`, etc.) and exploration multipliers.

---

## 📁 Project Directory Structure

```
pulse-music/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml          # Permissions, Activity, & MediaSessionService
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt          # App entry point, permissions, cold-start trigger
│   │   │   │   ├── api/
│   │   │   │   │   ├── Models.kt            # Legacy player models
│   │   │   │   │   └── MusicPlayerService.kt
│   │   │   │   ├── auth/
│   │   │   │   │   └── FirebaseAuthManager.kt # Credential Manager & Google Sign-In
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/               # Room Entities, DAOs, Database, DataStore
│   │   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   │   ├── Daos.kt
│   │   │   │   │   │   ├── Entities.kt
│   │   │   │   │   │   ├── PlaybackHistoryDao.kt
│   │   │   │   │   │   ├── PlaybackHistoryEntity.kt
│   │   │   │   │   │   ├── PlaylistPreferencesRepository.kt
│   │   │   │   │   │   ├── SessionStateStore.kt
│   │   │   │   │   │   ├── UserPreferencesManager.kt
│   │   │   │   │   │   └── UserPreferencesRepository.kt
│   │   │   │   │   ├── remote/              # Retrofit interfaces, Moshi DTOs, OkHttp
│   │   │   │   │   │   ├── JioSaavnApiService.kt
│   │   │   │   │   │   ├── JioSaavnModels.kt
│   │   │   │   │   │   ├── ListeningEventModels.kt
│   │   │   │   │   │   ├── LyricsAndRecsApiServices.kt
│   │   │   │   │   │   ├── LyricsAndRecsModels.kt
│   │   │   │   │   │   ├── NetworkModule.kt
│   │   │   │   │   │   └── UserAffinityModels.kt
│   │   │   │   │   └── repository/          # Repository implementations
│   │   │   │   │       ├── FirestorePlaylistRepository.kt
│   │   │   │   │       ├── JioSaavnRepository.kt
│   │   │   │   │       ├── MusicRepository.kt
│   │   │   │   │       └── PlaylistRepository.kt
│   │   │   │   ├── recommendation/          # Next-song scoring & anti-drift engine
│   │   │   │   │   ├── ArtistSimilarityEngine.kt
│   │   │   │   │   ├── DefaultRecommendationEngine.kt
│   │   │   │   │   ├── DevotionalContentFilter.kt
│   │   │   │   │   ├── FatigueScorer.kt
│   │   │   │   │   ├── ListeningEventLogger.kt
│   │   │   │   │   ├── MoodExtractionStrategy.kt
│   │   │   │   │   ├── RecommendationModels.kt
│   │   │   │   │   ├── RecommendationSessionManager.kt
│   │   │   │   │   ├── TimeOfDayScorer.kt
│   │   │   │   │   └── WeightAnalyticsManager.kt
│   │   │   │   ├── service/                 # Media3 audio service & downloader
│   │   │   │   │   ├── MusicController.kt
│   │   │   │   │   ├── MusicDownloader.kt
│   │   │   │   │   └── MusicService.kt
│   │   │   │   ├── sync/                    # Cloud synchronization coordinator
│   │   │   │   │   └── FirebaseSyncManager.kt
│   │   │   │   └── ui/                      # Jetpack Compose UI screens & ViewModels
│   │   │   │       ├── AddToPlaylistBottomSheet.kt
│   │   │   │       ├── ArtistProfileScreen.kt
│   │   │   │       ├── ColdStartDialog.kt
│   │   │   │       ├── HomeScreen.kt
│   │   │   │       ├── LibraryScreen.kt
│   │   │   │       ├── LoginScreen.kt
│   │   │   │       ├── MainScreen.kt
│   │   │   │       ├── MainViewModel.kt
│   │   │   │       ├── OnlinePlaylistScreen.kt
│   │   │   │       ├── PlayerScreen.kt
│   │   │   │       ├── PlaylistDetailsScreen.kt
│   │   │   │       ├── SearchScreen.kt
│   │   │   │       ├── SelectSongsScreen.kt
│   │   │   │       ├── SettingsScreen.kt
│   │   │   │       ├── SetupScreen.kt
│   │   │   │       └── theme/               # Color, typography, and styling tokens
│   │   │   └── res/                         # Drawables, layouts, values, mipmaps
│   │   └── test/                            # Unit and algorithm verification tests
│   │       └── java/com/example/recommendation/
│   │           ├── EngineStressTest.kt
│   │           ├── IntegrationTest.kt
│   │           ├── RecommendationEngineTest.kt
│   │           └── VerificationTest.kt
│   └── build.gradle.kts                     # App module configuration & dependencies
├── build.gradle.kts                         # Root project build configuration
├── settings.gradle.kts                      # Dependency resolution & plugins
├── firestore.rules                          # Cloud Firestore security rules
└── README.md                                # Project documentation
```

---

## 🛠️ Setup & Running Locally

### Prerequisites
- **Android Studio** (Koala / Ladybug or newer recommended).
- **JDK 17** or **JDK 21** configured in Android Studio Gradle settings.
- Physical device or Android Emulator running **Android 8.0 (API 24)** or higher.

### Step-by-Step Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/deepanshu383/pulse-music-.git
   cd pulse-music-
   ```

2. **Open in Android Studio**:
   - Open Android Studio, select **Open**, and navigate to the `pulse-music-` folder.
   - Wait for Gradle sync to complete.

3. **Firebase Configuration**:
   - Create a project on the [Firebase Console](https://console.firebase.google.com).
   - Enable **Google Sign-In** under **Authentication > Sign-in method**.
   - Enable **Cloud Firestore** and apply the rules defined in [`firestore.rules`](file:///firestore.rules).
   - Download your `google-services.json` and place it in the `app/` folder (`app/google-services.json`).

4. **API Keys Configuration**:
   - Create a `.env` file in the root project directory (see `.env.example`):
     ```env
     GEMINI_API_KEY=your_gemini_api_key_here
     ```
   - *(Optional)* If you wish to configure your own Musixmatch or Last.fm keys, update them in [`NetworkModule.kt`](file:///app/src/main/java/com/example/data/remote/NetworkModule.kt).

5. **Run the App**:
   - Select your target device or emulator in Android Studio.
   - Click the **Run** button (or press `Shift + F10`).
   - Grant audio/storage permissions upon the initial setup prompt.

---

## 🧪 Testing

Run recommendation engine unit tests and algorithm simulations:
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
