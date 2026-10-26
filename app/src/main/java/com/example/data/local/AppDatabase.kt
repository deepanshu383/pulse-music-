package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistSongEntity::class, SongEntity::class, SearchHistoryEntity::class, PlaybackHistoryEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun songDao(): SongDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `search_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `referenceId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `playback_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `trackId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `timeSegment` TEXT NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `playback_history` ADD COLUMN `title` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `playback_history` ADD COLUMN `artist` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `playback_history` ADD COLUMN `canonicalSongKey` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
