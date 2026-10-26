# 🎵 Pulse Music — Architecture & Project Presentation

<div align="center">

![Pulse Music Banner](https://raw.githubusercontent.com/deepanshu383/pulse-music-/main/art/banner.png)

### 📊 Project Architecture, API Deployment & System Flow Deck
*A Presentation-Style Technical Overview of Pulse Music Native Android App*

[![Platform](https://img.shields.io/badge/Platform-Android%20Studio-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/studio)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Audio](https://img.shields.io/badge/Audio%20Engine-AndroidX%20Media3%20ExoPlayer-E53935?style=for-the-badge)](https://developer.android.com/media/media3)
[![Cloud](https://img.shields.io/badge/Cloud%20Hosting-Render%20%26%20Google%20Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com)

---

</div>

## 📑 Presentation Index
- [Slide 1: App Overview & Purpose (App Ka Kaam)](#-slide-1-app-overview--purpose-app-ka-kaam)
- [Slide 2: System Architecture (High-Level Design)](#-slide-2-system-architecture-high-level-design)
- [Slide 3: API Ecosystem & Deployment (APIs Kaha Aur Kaise Deploy Hai)](#-slide-3-api-ecosystem--deployment-apis-kaha-aur-kaise-deploy-hai)
- [Slide 4: API Functions & Responsibilities (APIs Ka Kaam)](#-slide-4-api-functions--responsibilities-apis-ka-kaam)
- [Slide 5: Complete User & Data Flow (App Ka Working Flow)](#-slide-5-complete-user--data-flow-app-ka-working-flow)
- [Slide 6: Smart Recommendation & Anti-Drift Engine](#-slide-6-smart-recommendation--anti-drift-engine)
- [Slide 7: Core App Screens & UI Experience](#-slide-7-core-app-screens--ui-experience)
- [Slide 8: Database & Local Storage Design](#-slide-8-database--local-storage-design)
- [Slide 9: Key Highlights & Takeaways](#-slide-9-key-highlights--takeaways)

---

## 🎯 Slide 1: App Overview & Purpose (App Ka Kaam)

### 📌 What is Pulse Music?
**Pulse Music** is a native Android music streaming and local player built entirely using **Android Studio**, **Kotlin**, and **Jetpack Compose (Material Design 3)**.

### 🌟 Core Objectives:
1. **Hybrid Playback Experience**:
   - Plays local offline songs stored on the device (`.mp3`, `.m4a`, `.flac`, `.wav`, `.ogg`, `.opus`).
   - Streams millions of online songs, public playlists, and artist profiles in high-definition audio.
2. **True Background Playback**:
   - Runs as a persistent Android foreground media service with lock-screen controls, status-bar notification media session, and audio-focus handling.
3. **Drift-Free Music Recommendation**:
   - Features an intelligent, context-aware next-song engine that keeps the listening vibe consistent without jumping between conflicting genres or languages.
4. **Cloud Backup & Sync**:
   - Users can log in using their Google account to synchronize their playlists across devices seamlessly.
5. **Offline Music Downloader**:
   - Directly downloads streaming tracks into the device's public Music storage for offline access.

---

## 🏗️ Slide 2: System Architecture (High-Level Design)

The app follows the industry-standard **Model-View-ViewModel (MVVM)** clean architecture with unidirectional reactive data flow:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER (UI)                         │
│   • Jetpack Compose & Material 3 Design System                         │
│   • Edge-to-Edge Immersive Theme (Dark Glassmorphism)                  │
│   • Screens: Home, Player, Search, Library, Playlist Details, Artist   │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │  Observes StateFlows / Sends Events
┌───────────────────────────────────▼────────────────────────────────────┐
│                           VIEWMODEL LAYER                              │
│   • MainViewModel (Central State Coordinator)                          │
│   • Kotlin Coroutines & StateFlow (Reactive UI State Management)       │
│   • Queue Undo Stack (5-step LIFO memory)                              │
└───────┬───────────────────────────┬────────────────────────────┬───────┘
        │                           │                            │
┌───────▼──────────────┐   ┌────────▼─────────────┐   ┌──────────▼───────┐
│ RECOMMENDATION ENGINE│   │ MEDIA & BACKGROUND   │   │ REPOSITORY LAYER │
│ • Scoring Multiplier │   │ • AndroidX Media3    │   │ • MusicRepo      │
│ • Devotional Filter  │   │ • ExoPlayer Engine   │   │ • JioSaavnRepo   │
│ • Mood Session Lock  │   │ • MediaSessionService│   │ • PlaylistRepo   │
│ • Fatigue Analytics  │   │ • MusicDownloader    │   │ • FirestoreRepo  │
└──────────────────────┘   └──────────────────────┘   └──────────┬───────┘
                                                                 │
                                ┌────────────────────────────────┴────────┐
                                ▼                                         ▼
                    ┌───────────────────────┐                 ┌───────────────────────┐
                    │  LOCAL STORAGE LAYER  │                 │ CLOUD & NETWORK APIS  │
                    │ • SQLite Room DB      │                 │ • JioSaavn REST API   │
                    │ • DataStore Settings  │                 │ • Last.fm AudioScrob  │
                    │ • MediaStore & SAF    │                 │ • Musixmatch Lyrics   │
                    │ • SharedPreferences   │                 │ • Firebase Cloud DB   │
                    └───────────────────────┘                 └───────────────────────┘
```

---

## 🌐 Slide 3: API Ecosystem & Deployment (APIs Kaha Aur Kaise Deploy Hai)

Pulse Music connects with four specialized cloud services. Here is how and where each backend API is deployed:

| Service / API | Hosting & Deployment Location | Deployment Method & Technology | Access Type |
| :--- | :--- | :--- | :--- |
| **JioSaavn Music API** | **Render Cloud Platform**<br>`https://jiosaavn-api-2s17.onrender.com/` | Node.js / Express microservice containerized and deployed as a scalable Web Service on **Render** (Frankfurt/US region). Direct proxy scraper to JioSaavn content delivery network. | Public REST API (No API Key Required) |
| **Last.fm AudioScrobbler API** | **Last.fm Global Server Network**<br>`https://ws.audioscrobbler.com/2.0/` | Enterprise cloud infrastructure managed by CBS Interactive / Last.fm globally distributed servers. | REST API with API Key & Shared Secret |
| **Musixmatch Lyrics API** | **Musixmatch Enterprise Cloud**<br>`https://api.musixmatch.com/ws/1.1/` | Multi-region cloud servers managed by Musixmatch, delivering cataloged lyrics and timestamps. | REST API with API Key authentication |
| **Firebase Cloud Firestore** | **Google Cloud Platform (GCP)**<br>Serverless NoSQL Database | Distributed serverless NoSQL database deployed on Google Cloud Platform, secured via custom Firestore Security Rules. | Firebase SDK with TLS 1.3 encryption |
| **Firebase Authentication** | **Google Identity Platform**<br>Google Cloud IAM | Managed Google Identity infrastructure integrated with Android 14+ Credential Manager. | OAuth 2.0 / Google IdToken Credential |

---

## ⚙️ Slide 4: API Functions & Responsibilities (APIs Ka Kaam)

Each API serves a distinct role in creating the complete music streaming experience:

### 1. JioSaavn API (`Render Cloud`) — *Primary Audio & Metadata Engine*
- **Song Search**: Searches millions of tracks by title, artist, or album.
- **Direct Audio Streaming**: Provides direct CDN audio stream links in multiple qualities (160 kbps standard, 320 kbps high-definition AAC/MP4).
- **Public Playlists**: Fetches curated top charts (Bollywood Hits, Trending Punjabi, Indie Pop, etc.).
- **Artist Discography**: Fetches artist bios, verification status, follower count, top singles, and full albums.
- **Mood & Language Catalog**: Powers mood-based recommendation searches (`"romantic hindi"`, `"party punjabi"`, etc.).

### 2. Last.fm Scrobbler API (`AudioScrobbler Cloud`) — *Music Intelligence & Similarities*
- **Acoustic Similarity Discovery**: Finds similar tracks based on global community listening behavior.
- **Top Tags & Genre Classification**: Identifies musical genres, emotional attributes, and language tags.
- **Cross-Discovery Pipeline**: Helps discover lesser-known songs matching the current artist's vocal texture.

### 3. Musixmatch API (`Musixmatch Cloud`) — *Synchronized Lyrics Engine*
- **Lyrics Retrieval**: Matches artist and song title to retrieve full verified lyrics for display on the Now Playing screen.

### 4. Firebase Authentication (`Google Cloud Identity`) — *User Profiles*
- **One-Tap Google Sign-In**: Uses Android Credential Manager for passwordless, secure authentication.
- **User Identity**: Issues unique user tokens (`uid`) to isolate individual playlist collections.

### 5. Cloud Firestore (`Google Cloud Firestore`) — *Cloud Backup & Personalization*
- **Cross-Device Playlist Sync**: Stores user-created playlists and exact song sequence orders.
- **Listening Telemetry**: Records duration, skip category (`INSTANT`, `EARLY`, `COMPLETED`), and completion percentage.
- **Adaptive Affinity Profiles**: Stores dynamic user weights (exploration factor, favorite artist affinities).

---

## 🔄 Slide 5: Complete User & Data Flow (App Ka Working Flow)

### Step 1: App Launch & Authentication
- App boots with an edge-to-edge dark UI theme.
- Checks Firebase Auth state:
  - If logged in, automatically pulls saved playlists from Cloud Firestore into the local Room database.
  - If first-time user, displays a clean Google Sign-In prompt.
- Checks Cold-Start state: Prompts user to pick favorite artists and languages to seed the recommendation engine.

### Step 2: Local Storage Access & Scanning
- Checks runtime storage permissions (`READ_MEDIA_AUDIO` on Android 13+, `READ_EXTERNAL_STORAGE` on older versions).
- Scans user audio directories via Storage Access Framework (`DocumentFile`) and Android `MediaStore`.
- Extracts metadata (Title, Artist, Album, Bitrate, Duration, Cover Art) via `MediaMetadataRetriever` and saves to SQLite Room DB.

### Step 3: Discovery & Search Flow
- Home screen parallelly requests featured playlists and trending songs from the JioSaavn API on Render.
- Search screen debounces input by 400ms to avoid spamming the network, then searches songs, artists, and playlists simultaneously.

### Step 4: Background Playback & Media3 Architecture
- When a song is clicked, `MusicController` creates an `androidx.media3.common.MediaItem`.
- `MusicService` (Foreground Service) streams the track using `ExoPlayer`.
- Audio attributes are configured for high-quality music playback with automatic audio pause when headphones are unplugged.
- System notification renders interactive controls (Play, Pause, Skip, Previous, Seekbar).

### Step 5: Auto-Play Next Track Pipeline
- When the current queue reaches its last song:
  1. The app extracts the current song's mood, language, release year, and artist.
  2. Queries 5 sources in parallel (JioSaavn mood+lang, artist songs, Last.fm cross-similarity, and local tracks).
  3. Deduplicates and caps candidate pool to 50 songs.
  4. Runs hard pre-scoring gates (Devotional exclusion filter, canonical version check, session skip check).
  5. Scores candidates using the Multi-Factor Engine.
  6. Enqueues the winning candidate automatically to provide non-stop playback.

### Step 6: Playlist Management & Firestore Sync
- Users create, rename, reorder, or delete playlists.
- Changes are immediately applied to local SQLite Room tables (`playlists`, `playlist_songs`).
- If signed in, changes are synced asynchronously to Cloud Firestore (`users/{uid}/playlists/{playlistId}`).

### Step 7: Offline Music Downloader
- Tapping 'Download' routes the stream URL to Android's native `DownloadManager`.
- File is saved into `Music/PulseMusic/` on device storage.
- A dynamic `BroadcastReceiver` catches the completion event and marks the track as `isDownloaded = true` in Room DB.

---

## 🧠 Slide 6: Smart Recommendation & Anti-Drift Engine

```
                          CANDIDATE TRACK POOL (50 Songs)
                                         │
                                         ▼
             ┌───────────────────────────────────────────────────────┐
             │         ZERO-TOLERANCE HARD FILTERS (0% Bypass)       │
             │  • Devotional Exclusion Filter (Bhajan / Aarti / God) │
             │  • Canonical Key Dedup ("title|artist" normalized)    │
             │  • Active Session Replay & Early Skip Guard           │
             │  • Strict Language Consistency Check                  │
             └───────────────────────────┬───────────────────────────┘
                                         │
                                         ▼
             ┌───────────────────────────────────────────────────────┐
             │            MULTI-FACTOR SCORING ENGINE                │
             │  • Mood Consistency (30%)    • Acoustic Similarity (10%)│
             │  • Language Match (25%)      • Category/Movie Single (10%)│
             │  • Era Proximity (20%)       • Time of Day Fit (5%)   │
             │  • User Preference (+10%)    • Jitter Randomizer      │
             └───────────────────────────┬───────────────────────────┘
                                         │
                                         ▼
                            WINNING TRACK ENQUEUED
```

### 1. Multi-Factor Scoring Formula
Every candidate track is evaluated with the following weighted breakdown:
- **30% Mood Consistency**: Matches the emotional group (Party, Romantic, Sad, Chill).
- **25% Language Consistency**: Keeps the language aligned with the listener's session (e.g. Hindi, Punjabi, English) with soft cross-affinity (Hindi ↔ Urdu).
- **20% Era Proximity**: Enforces a ±5-year window. Legacy loops (pre-2010) strictly block modern remix intrusion.
- **10% Acoustic Similarity**: Matches vocal textures (e.g. Arijit Singh / Atif Aslam vocal cluster).
- **10% Category Consistency**: Keeps movie/soundtrack songs with soundtracks, and independent singles with singles.
- **5% Time of Day Fit**: Adapts to user habits (morning energy boost, nighttime lo-fi/ambient preference).
- **Additive User Preference**: Boosts user-favorite artists and cold-start seed selections.
- **Stochastic Jitter**: Micro-randomization that ensures the playlist never repeats identical sequences.

### 2. Anti-Drift Session Mood Lock
- When a user manually taps a track, the engine locks the session to that song's mood.
- Auto-shift logic is disabled so that background auto-play never slowly drifts from a romantic session into high-energy dance or devotional tracks.

### 3. Zero-Tolerance Devotional Filter
- Employs whole-word regex tokenization across `title`, `album`, and `artist`.
- Checks against deity keywords (`ram`, `krishna`, `shiva`, `hanuman`, `waheguru`, `vitthal`, etc.) and devotional formats (`bhajan`, `kirtan`, `aarti`, `chalisa`, `abhang`, `stotram`, `gurbani`).
- Isolates secular and devotional listening without false positives.

### 4. 7-Day Fatigue Penalty
- Inspects local playback history for the previous 7 days.
- Automatically docks points (`-0.10f` per play) for songs played repeatedly to encourage freshness.

---

## 📱 Slide 7: Core App Screens & UI Experience

| Screen Name | Visual Layout & UI Role |
| :--- | :--- |
| **Home Screen** | Curated Discover page with horizontal carousels for featured playlists, trending tracks, quick-play buttons, and hero banners. |
| **Now Playing Screen** | Full-screen immersive player featuring large rotating album art, animated play/pause buttons, interactive seekbar, volume controls, and real-time synchronized lyrics card. |
| **Mini Player Bar** | Persistent bottom audio control dock visible across all screens, allowing playback control while navigating other sections. |
| **Search Screen** | Instant search with segmented tabs (Songs, Playlists, Artists), previous search history chips, and auto-complete suggestions. |
| **Library Screen** | Toggleable view (List / Grid) of local songs and custom playlists with multiple sort options (Title, Artist, Date Added, Duration). |
| **Playlist Details** | Header with cover art, track count, total duration, song reordering (drag-and-drop), and add/remove track controls. |
| **Artist Profile** | Artist banner image, follower statistics, biography snippet, top popular songs, and complete album catalogue. |
| **Settings Screen** | Storage folder picker, cache management, auto-add queue toggle, queue persistence toggle, and account sign-out. |

---

## 🗄️ Slide 8: Database & Local Storage Design

### Local SQLite (Room Database: `pulse_music_db`)
1. **`playlists`**: Stores local playlist metadata (name, description, cover image, creation timestamp).
2. **`playlist_songs`**: Relational join table mapping songs to playlists with explicit position ordering indices.
3. **`song_metadata`**: Master cache table storing all track attributes (file path, title, artist, album, duration, online stream URL, download status, local file path).
4. **`playback_history`**: Chronological log of played songs, timestamps, time segments, and canonical song keys for fatigue calculations.
5. **`search_history`**: Stores the user's recent queries for instant recall.

### Cloud Storage (Google Cloud Firestore)
- **`users/{uid}/playlists/{playlistId}`**: Remote backup of user playlists with embedded song metadata for instant multi-device restore.
- **`listening_events/{uid}/events/{eventId}`**: Anonymous playback telemetry for skip analysis.
- **`user_affinity/{uid}`**: User preference profile containing learned weight multipliers.

---

## 💎 Slide 9: Key Highlights & Takeaways

- **Built with Native Android Best Practices**: 100% Kotlin, Jetpack Compose, Material 3, Coroutines, Flow, Room, and Media3 ExoPlayer.
- **True Cloud Microservice Architecture**: Free public JioSaavn API hosted as a scalable Node.js container on Render cloud platform.
- **No API Keys Needed for Core Music**: High-quality 320 kbps streaming and downloads work directly out of the box.
- **Advanced Recommendation System**: Protects user immersion with strict language matching, anti-drift session locking, and devotional filtering.
- **Cross-Platform Cloud Sync**: Google Sign-In with automated Cloud Firestore synchronization.
- **Complete Offline Capability**: Both plays local storage audio and downloads online tracks directly to the device.

---

<div align="center">

**Pulse Music — Native Android Music Experience**  
*Developed in Android Studio • Powered by Jetpack Compose & Media3*

</div>
