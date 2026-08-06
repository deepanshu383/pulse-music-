package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.example.data.local.SongDao
import com.example.data.local.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

import android.provider.DocumentsContract
import android.media.MediaMetadataRetriever
import androidx.documentfile.provider.DocumentFile

class MusicRepository(
    private val context: Context,
    private val songDao: SongDao
) {
    val cachedSongs = songDao.getAllCachedSongs()

    suspend fun deleteSong(song: SongEntity) = withContext(Dispatchers.IO) {
        songDao.deleteSong(song.filePath)
        
        try {
            if (song.filePath.startsWith("content://")) {
                val uri = Uri.parse(song.filePath)
                try {
                    DocumentsContract.deleteDocument(context.contentResolver, uri)
                } catch (e: Exception) {
                    val documentFile = DocumentFile.fromSingleUri(context, uri)
                    documentFile?.delete()
                }
            } else {
                val file = File(song.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun scanMusicFiles(folderUriString: String? = null) = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongEntity>()

        if (folderUriString != null) {
            val treeUri = Uri.parse(folderUriString)
            val documentFile = DocumentFile.fromTreeUri(context, treeUri)
            if (documentFile != null) {
                scanDocumentTree(documentFile, songs)
            }
        } else {
            // Fallback to MediaStore
            scanMediaStore(songs)
        }
        
        if (songs.isNotEmpty()) {
            songDao.insertSongs(songs)
        }
    }

    private fun scanDocumentTree(folder: DocumentFile, songs: MutableList<SongEntity>) {
        try {
            folder.listFiles().forEach { file ->
                if (file.isDirectory) {
                    scanDocumentTree(file, songs)
                } else {
                    val name = file.name ?: ""
                    val type = file.type ?: ""
                    val isAudio = type.startsWith("audio/") || 
                                  name.endsWith(".mp3", ignoreCase = true) ||
                                  name.endsWith(".m4a", ignoreCase = true) ||
                                  name.endsWith(".flac", ignoreCase = true) ||
                                  name.endsWith(".wav", ignoreCase = true) ||
                                  name.endsWith(".ogg", ignoreCase = true) ||
                                  name.endsWith(".aac", ignoreCase = true) ||
                                  name.endsWith(".opus", ignoreCase = true)
                                  
                    if (isAudio) {
                        try {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(context, file.uri)
                            
                            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: name
                            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
                            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            val duration = durationStr?.toLongOrNull() ?: 0L
                            
                            songs.add(
                                SongEntity(
                                    filePath = file.uri.toString(),
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    duration = duration,
                                    bitrate = 0,
                                    albumArtPath = null,
                                    lastModified = file.lastModified()
                                )
                            )
                            retriever.release()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // If metadata extraction fails, still add the file with basic info
                            songs.add(
                                SongEntity(
                                    filePath = file.uri.toString(),
                                    title = name,
                                    artist = "Unknown Artist",
                                    album = "Unknown Album",
                                    duration = 0L,
                                    bitrate = 0,
                                    albumArtPath = null,
                                    lastModified = file.lastModified()
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanMediaStore(songs: MutableList<SongEntity>) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.ALBUM_ID
        )
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        
        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                if (!File(path).exists()) continue
                
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val albumId = cursor.getLong(albumIdColumn)
                
                val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId).toString()

                songs.add(
                    SongEntity(
                        filePath = path,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        bitrate = 0, // Simplification
                        albumArtPath = albumArtUri,
                        lastModified = dateModified
                    )
                )
            }
        }
    }
}
