package com.lk.musicservicelibrary.database

import android.content.Context
import androidx.room.*

/**
 * Erstellt von Lena am 02/04/2019.
 */
@Database(entities = [PlayingItemEntity::class], version = 1, exportSchema = true)
abstract class PlaylistDatabase: RoomDatabase() {

    abstract fun playlistDao(): PlaylistDAO

    companion object {
        @Volatile private var INSTANCE: PlaylistDatabase? = null

        fun getInstance(context: Context): PlaylistDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
                Room.databaseBuilder(context.applicationContext,
                    PlaylistDatabase::class.java, "playlists.db").build()
    }

}