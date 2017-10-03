package com.lk.plattenspieler.background

import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.browse.MediaBrowser
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log

/**
 * Created by Lena on 08.06.17.
 */
class MusicProvider(context: Context) {

    // Zugriff auf die Datenbanken mit der Musik und Aufbereitung der Daten

    val c: Context
    init { c = context }

    val ROOT_ID = "__ ROOT__"
    val TAG = "MusicProvider"

    fun getTitles(c: Cursor, list: MutableList<MediaBrowserCompat.MediaItem>, cover: String): MutableList<MediaBrowserCompat.MediaItem>{
        var i = 0
        c.moveToFirst()
        do {
            val trackid = c.getString(c.getColumnIndex(MediaStore.Audio.Media._ID))
            val tracktitle = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE))
            val interpret = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST))
            val album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM))
            val description = MediaDescriptionCompat.Builder()
                    .setMediaId(trackid)
                    .setDescription(album + "__" + cover)
                    .setTitle(tracktitle)
                    .setSubtitle(interpret)
            list.set(i, MediaBrowserCompat.MediaItem(description.build(), MediaBrowser.MediaItem.FLAG_PLAYABLE))
            i++
        } while(c.moveToNext())
        return list
    }
    fun getAlbums(c: Cursor, list: MutableList<MediaBrowserCompat.MediaItem>): MutableList<MediaBrowserCompat.MediaItem>{
        var i = 0
        c.moveToFirst()
        do {
            var albumid = c.getString(c.getColumnIndex(MediaStore.Audio.Media._ID))
            val albumtitle = c.getString(c.getColumnIndex(MediaStore.Audio.Albums.ALBUM))
            val albumtracks = c.getString(c.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS))
            val albumartist = c.getString(c.getColumnIndex(MediaStore.Audio.Albums.ARTIST))
            val albumart = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART))
            albumid = "ALBUM-" + albumid
            val description = MediaDescriptionCompat.Builder()
                    .setMediaId(albumid)
                    .setTitle(albumtitle)
                    .setSubtitle(albumartist)
                    .setDescription(albumart + "__" + albumtracks)
            list.set(i, MediaBrowserCompat.MediaItem(description.build(), MediaBrowser.MediaItem.FLAG_BROWSABLE))
            i++
        } while(c.moveToNext())
        return list
    }
    fun getFileFromMediaId(mediaId: String?): String{
        var selection: String? = null
        if(mediaId != null){
            // eine spezifische ID abfragen, sonst den ersten Titel
            selection = MediaStore.Audio.Media._ID + "='" + mediaId + "'"
        }
        var result = "ERROR"
        val projection = Array<String>(2, init = { i -> "" } )
        projection[0] = MediaStore.Audio.Media._ID
        projection[1] = MediaStore.Audio.Media.DATA
        val cursor = c.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null)
        if(cursor != null){
            cursor.moveToFirst()
            result = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        }
        cursor.close()
        return result
    }
    fun getMediaDescription(mediaId: String?, songnumber: String?): MediaMetadataCompat?{
        var selection: String?
        if(mediaId != null){
            val result = MediaMetadataCompat.Builder()
            // eine spezifische ID abfragen, sonst den ersten Titel
            selection = MediaStore.Audio.Media._ID + "='" + mediaId + "'"
            val projection = Array<String>(6, init = { i -> "" } )
            projection[0] = MediaStore.Audio.Media._ID
            projection[1] = MediaStore.Audio.Media.TITLE
            projection[2] = MediaStore.Audio.Media.ALBUM
            projection[3] = MediaStore.Audio.Media.ARTIST
            projection[4] = MediaStore.Audio.Media.DURATION
            projection[5] = MediaStore.Audio.Media.ALBUM_ID
            val cursor = c.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null)
            if(cursor != null){
                cursor.moveToFirst()
                result.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                result.putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)))
                result.putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)))
                result.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)))
                // Duration verlangt einen Long
                result.putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)))
                // Liederanzahl
                if(!songnumber.isNullOrEmpty()){
                    val nr = songnumber!!.toLong()
                    result.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, nr)
                }
                // Albumcover abfragen, um es in die Wiedergabe einzubinden
                val albumid = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                val projection_album = Array<String>(2, init = { _ -> "" } )
                projection_album[0] = MediaStore.Audio.Albums._ID
                projection_album[1] = MediaStore.Audio.Albums.ALBUM_ART
                val select = MediaStore.Audio.Albums._ID + "='" + albumid + "'"
                val calbums = c.contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, projection_album, select, null, null)
                if(calbums != null){
                    calbums.moveToFirst()
                    result.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, calbums.getString(calbums.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)))
                    val bitmap = BitmapFactory.decodeFile(calbums.getString(calbums.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)))
                    result.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                }
                calbums.close()
            }
            cursor.close()
            return result.build()
        }
        return null
    }

}