package com.example.service

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.example.data.local.SongEntity

class MusicDownloader(private val context: Context) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    
    // Track pending downloads: downloadId -> callback
    private val pendingDownloads = mutableMapOf<Long, (String) -> Unit>()

    init {
        // Register BroadcastReceiver for download completion (Bug A3 fix)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
                val callback = pendingDownloads.remove(downloadId) ?: return
                
                // Verify the download was successful
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        if (statusIndex != -1 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                            val localUri = if (localUriIndex != -1) cursor.getString(localUriIndex) else null
                            val path = localUri?.let { Uri.parse(it).path } 
                                ?: "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)}/PulseMusic/"
                            callback(path)
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    fun downloadSong(song: SongEntity, onDownloadComplete: (String) -> Unit): Long {
        if (song.downloadUrl == null) return -1L

        val fileName = "${song.title} - ${song.artist}.mp3"
            .replace("[^a-zA-Z0-9._\\- ]".toRegex(), "") // Sanitize filename

        val request = DownloadManager.Request(Uri.parse(song.downloadUrl))
            .setTitle(song.title)
            .setDescription("Downloading ${song.title} by ${song.artist}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "PulseMusic/$fileName")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)
        
        // Store the callback — it will be invoked only when the download actually completes
        val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)}/PulseMusic/$fileName"
        pendingDownloads[downloadId] = { _ -> onDownloadComplete(path) }
        
        return downloadId
    }
}
