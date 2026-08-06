package com.example.api

interface MusicPlayerListener {
    fun onPlaybackStateChanged(state: ApiPlaybackState)
    fun onPlaylistUpdated(playlist: Playlist)
    fun onVolumeChanged(volume: Float)
    fun onShuffleChanged(enabled: Boolean)
    fun onRepeatModeChanged(mode: RepeatMode)
    fun onError(message: String)
}
