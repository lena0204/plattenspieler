package com.lk.musicservicelibrary.main

import android.app.NotificationManager
import android.content.*
import android.media.AudioManager
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import com.lk.musicservicelibrary.R
import com.lk.musicservicelibrary.database.PlaylistRepository
import com.lk.musicservicelibrary.database.room.PlaylistRoomRepository
import com.lk.musicservicelibrary.models.*
import com.lk.musicservicelibrary.playback.PlaybackCallback
import com.lk.musicservicelibrary.system.*
import com.lk.musicservicelibrary.utils.SharedPrefsWrapper

/**
 * Erstellt von Lena am 02.09.18.
 * MediaBrowserService; zusammen mit PlaybackController (ControllerCallback)
 * mögliche Controller-Aktionen: playFromId, play, pause, next, previous, stop, Command: addAll (als shuffle)
 */
class MusicService : MediaBrowserService(), Observer<Any> {

    private val TAG = MusicService::class.java.simpleName
    private val NOTIFICATION_ID = 9880

    private lateinit var musicDataRepo: MusicDataRepository
    private lateinit var playlistRepo: PlaylistRepository
    private lateinit var session: MediaSession
    private lateinit var sessionCallback: PlaybackCallback

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: MusicNotificationBuilder
    private lateinit var nbReceiver: NotificationActionReceiver

    private var serviceStarted = false

    companion object {
        const val ACTION_MEDIA_PLAY = "com.lk.pl-ACTION_MEDIA_PLAY"
        const val ACTION_MEDIA_PAUSE = "com.lk.pl-ACTION_MEDIA_PAUSE"
        const val ACTION_MEDIA_NEXT = "com.lk.pl-ACTION_MEDIA_NEXT"
        const val SHUFFLE_KEY = "shuffle"
        const val QUEUE_KEY = "queue"
        const val METADATA_KEY = "metadata"
        var PLAYBACK_STATE = PlaybackState.STATE_STOPPED
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        initializeComponents()
        registerBroadcastReceiver()
        prepareSession()
        registerPlaybackObserver()
    }

    // - - - Setup - - -
    private fun initializeComponents() {
        musicDataRepo = LocalMusicFileRepository(this.applicationContext)
        playlistRepo = PlaylistRoomRepository(this.application)
        sessionCallback = PlaybackCallback(musicDataRepo, playlistRepo, this.applicationContext)
        notificationManager = this.getSystemService<NotificationManager>() as NotificationManager
        notificationBuilder = MusicNotificationBuilder(this)
        nbReceiver = NotificationActionReceiver(sessionCallback)
        val am = this.getSystemService<AudioManager>() as AudioManager
        // AudioFocusRequester.setup(sessionCallback.audioFocusChanged, am)
    }

    private fun registerBroadcastReceiver() {
        val ifilter = IntentFilter()
        ifilter.addAction(ACTION_MEDIA_PLAY)
        ifilter.addAction(ACTION_MEDIA_PAUSE)
        ifilter.addAction(ACTION_MEDIA_NEXT)
        this.registerReceiver(nbReceiver, ifilter)
    }

    private fun prepareSession() {
        session = MediaSession(applicationContext, TAG)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            session.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        }
        session.setCallback(sessionCallback)
        session.setQueueTitle(getString(R.string.queue_title))
        sessionToken = session.sessionToken
    }

    private fun registerPlaybackObserver() {
        sessionCallback.getPlaybackState().observeForever(this)
        sessionCallback.getPlayingList().observeForever(this)
    }

    // - - - Media Browser capabilities - - -
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        if (this.packageName == clientPackageName) {
            return BrowserRoot(MusicDataRepository.ROOT_ID, null)
        }
        return BrowserRoot("", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowser.MediaItem>>) {
        when {
            parentId == MusicDataRepository.ROOT_ID -> {
                result.sendResult(musicDataRepo.queryAlbums().getMediaItemList())
            }
            parentId.contains("ALBUM-") -> {
                result.sendResult(getTitles(parentId))
            }
            else -> Log.e(TAG, "No known parent ID")
        }
    }

    private fun getTitles(albumId: String): MutableList<MediaBrowser.MediaItem> {
        val id = albumId.replace("ALBUM-", "")
        val playingList = musicDataRepo.queryTitlesByAlbumID(id)
        sessionCallback.setQueriedMediaList(playingList)
        return playingList.getMediaItemList()
    }

    // - - - Clean up - - -
    override fun onUnbind(intent: Intent?): Boolean {
        val bool = super.onUnbind(intent)
        Log.v(TAG, "onUnbind")
        val state = session.controller.playbackState?.state
        if (state == PlaybackState.STATE_PAUSED) {
            Log.d(TAG, "Playback paused ($state), so stop")
            val metadata = MusicMetadata.createFromMediaMetadata(session.controller.metadata!!)
            playlistRepo.savePlayingQueue(sessionCallback.getPlayingList().value!!, metadata)
            sessionCallback.onStop()
        }
        return bool
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        sessionCallback.onStop()
        this.unregisterReceiver(nbReceiver)
        unregisterPlaybackObserver()
        session.release()
        session.isActive = false
    }

    private fun unregisterPlaybackObserver(){
        sessionCallback.getPlayingList().removeObserver(this)
        sessionCallback.getPlaybackState().removeObserver(this)
    }

    // - - - Changes handler - - -

    override fun onChanged(update: Any?) {
        if(update is MusicList) {
            updatePlayingList(update)
        } else if (update is PlaybackState) {
            updatePlaybackState(update)
        }
    }

    private fun updatePlayingList(playingList: MusicList) {
        val playingTitle = playingList.getItemAtCurrentPlaying()
        val shortQueue = getShortenedQueue(playingList)
        if(playingTitle != null ){
            playingTitle.nr_of_songs_left = shortQueue.count().toLong()
            session.setMetadata(playingTitle.getMediaMetadata())
            sendBroadcastForLightningLauncher(playingTitle)
        }
        session.setQueue(shortQueue.getQueueItemList())
    }

    private fun getShortenedQueue(playingList: MusicList) : MusicList {
        val shortedQueue = MusicList()
        val firstAfterPlaying = playingList.getCurrentPlaying() + 1
        for (i in firstAfterPlaying until playingList.size()) {
            shortedQueue.addItem(playingList.getItemAt(i))
        }
        return shortedQueue
    }

    private fun sendBroadcastForLightningLauncher(metadata: MusicMetadata) {
        val track = bundleOf(
            "title" to metadata.title,
            "album" to metadata.album,
            "artist" to metadata.artist)
        val extras = bundleOf(
            "track" to track,
            "aaPath" to metadata.cover_uri)
        this.sendBroadcast(Intent("com.lk.plattenspieler.metachanged").putExtras(extras))
    }

    private fun updatePlaybackState(state: PlaybackState) {
        PLAYBACK_STATE = state.state
        session.setPlaybackState(state)
        adaptServiceToPlaybackState()
    }

    private fun adaptServiceToPlaybackState() {
        when (PLAYBACK_STATE) {
            PlaybackState.STATE_PLAYING -> adaptToPlayingPaused()
            PlaybackState.STATE_PAUSED -> {
                adaptToPlayingPaused()
                this.stopForeground(false)
            }
            PlaybackState.STATE_STOPPED -> {
                notificationManager.cancel(NOTIFICATION_ID)
                stopService()
            }
        }
    }

    private fun adaptToPlayingPaused() {
        val shuffleOn = SharedPrefsWrapper.readShuffle(applicationContext)
        val metadata = if(session.controller.metadata != null) {
            MusicMetadata.createFromMediaMetadata(session.controller.metadata!!)
        } else {
            MusicMetadata()
        }
        val noti = notificationBuilder.showNotification(PLAYBACK_STATE, metadata, shuffleOn)
        if(PLAYBACK_STATE == PlaybackState.STATE_PLAYING){
            startServiceIfNecessary()
            this.startForeground(NOTIFICATION_ID, noti)
        } else {
            notificationManager.notify(NOTIFICATION_ID, noti)
        }
    }

    private fun startServiceIfNecessary() {
        if (!serviceStarted) {
            this.startService(Intent(applicationContext,
                com.lk.musicservicelibrary.main.MusicService::class.java))
            Log.d(TAG, "started service")
            serviceStarted = true
        }
        if (!session.isActive)
            session.isActive = true
    }

    private fun stopService() {
        Log.d(TAG, "stopService in service")
        this.stopSelf()
        serviceStarted = false
        this.stopForeground(true)
    }
}