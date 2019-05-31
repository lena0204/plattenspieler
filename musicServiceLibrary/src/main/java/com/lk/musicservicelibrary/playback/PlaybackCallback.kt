package com.lk.musicservicelibrary.playback

import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.ResultReceiver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lk.musicservicelibrary.main.*
import com.lk.musicservicelibrary.models.*
import com.lk.musicservicelibrary.playback.state.BasicState
import com.lk.musicservicelibrary.playback.state.StoppedState
import com.lk.musicservicelibrary.system.MusicDataRepository
import com.lk.musicservicelibrary.utils.PlaybackStateBuilder

/**
 * Erstellt von Lena am 05/04/2019.
 */
class PlaybackCallback(private val dataRepository: MusicDataRepository):
    MediaSession.Callback(),
    MusicPlayer.PlaybackFinished {

    // private val TAG = "MusicPlayer"

    private var playerState: BasicState =
        StoppedState(this)
    private var commandResolver = CommandResolver(this)

    private var playingList = MutableLiveData<MusicList>()
    private var playbackState = MutableLiveData<PlaybackState>()
    private var player = SimpleMusicPlayer(this)
    private var queriedMediaList = MusicList()

    // TODO check audiofocus

    init {
        playingList.value = MusicList()
        playbackState.value = PlaybackStateBuilder.createStateForStopped()
    }

    fun getPlayingList(): LiveData<MusicList> = playingList
    fun setPlayingList(updatedList: MusicList) {
        playingList.value = updatedList
    }

    fun getQueriedMediaList(): MusicList = queriedMediaList
    fun setQueriedMediaList(updatedList: MusicList) {
        queriedMediaList = updatedList
    }

    fun getPlaybackState(): LiveData<PlaybackState> = playbackState
    fun setPlaybackState(updatedState: PlaybackState) {
        playbackState.value = updatedState
    }
    fun getShuffleFromPlaybackState(): Boolean {
        val extras = playbackState.value!!.extras
        return extras?.getBoolean(MusicService.SHUFFLE_KEY) ?: false
    }

    fun getPlayerState(): BasicState = playerState
    fun setPlayerState(state: BasicState) {
        playerState = state
    }

    fun getDataRepository(): MusicDataRepository = dataRepository

    fun getPlayer(): MusicPlayer = player

    override fun playbackFinished() {
        playerState.skipToNext()
    }

    override fun onPlay() {
        playerState.play()
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        playerState.playFromId(mediaId, extras)
    }

    override fun onPause() {
        playerState.pause()
    }

    override fun onSkipToNext() {
        playerState.skipToNext()
    }

    override fun onSkipToPrevious() {
        playerState.skipToPrevious()
    }

    override fun onStop() {
        playerState.stop()
    }

    override fun onCommand(command: String, args: Bundle?, cb: ResultReceiver?) {
        commandResolver.resolveCommand(command, args, cb)
    }

}