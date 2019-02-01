package com.lk.plattenspieler.fragments

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.os.bundleOf
import androidx.fragment.app.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.lk.musicservicelibrary.models.MusicList
import com.lk.musicservicelibrary.models.MusicMetadata
import com.lk.plattenspieler.R
import com.lk.plattenspieler.observables.PlaybackViewModel
import com.lk.plattenspieler.utils.LyricsAccess
import com.lk.plattenspieler.main.ThemeChanger
import kotlinx.android.synthetic.main.fragment_playing.*
import kotlinx.android.synthetic.main.fragment_playing.view.*
import java.util.*

/**
 * Created by Lena on 08.06.17.
 * Zeigt Informationen zum aktuell spielenden Lied (Metadaten) und die Wiedergabeliste an
 */
class PlayingFragment : Fragment(), Observer<Any>{

    private val TAG = "com.lk.pl-PlayingFrag"
	private var lyrics: String? = null

    private lateinit var playbackViewModel: PlaybackViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_playing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setActionbarTitle()
        playbackViewModel = ViewModelProviders.of(requireActivity()).get(PlaybackViewModel::class.java)
        playbackViewModel.setObserverToAll(this, this)
        writeMetadata(playbackViewModel.metadata.value!!)
        setPlaylist(playbackViewModel.getQueueLimitedTo30())
        view.iv_playing_lyrics.setOnClickListener { onClickLyrics(it)}
    }

    override fun onChanged(update: Any?) {
        when(update) {
            is MusicMetadata -> writeMetadata(update)
            is MusicList -> setPlaylist(playbackViewModel.getQueueLimitedTo30())
        }
    }

    private fun setActionbarTitle(){
        if(ThemeChanger.themeIsLineage(activity))
            (activity?.actionBar?.customView as TextView).text = resources.getString(R.string.action_playing)
        activity?.actionBar?.title = getString(R.string.action_playing)
    }

    private fun onClickLyrics(view: View?){
        if (!lyrics.isNullOrEmpty()) {
            val args = createBundleForLyricsFragment()
            val lyricsf = LyricsFragment()
            lyricsf.arguments = args
            fragmentManager?.transaction {
                addToBackStack(null)
                replace(R.id.fl_main_content, lyricsf, "TAG_LYRICS")
            }
        }
    }

    private fun createBundleForLyricsFragment(): Bundle {
        val args = bundleOf("L" to lyrics)
        if(playbackViewModel.metadata.value?.isEmpty() == false) {
            args.putString("C", playbackViewModel.metadata.value!!.cover_uri)
        }
        return args
    }

    private fun writeMetadata(data: MusicMetadata){
        if(!data.isEmpty()) {
            tv_playing_title.text = data.title
            tv_playing_artist.text = data.artist
            tv_playing_album.text = data.album
            tv_playing_songnumber.text = data.nr_of_songs_left.toString()
            tv_playing_songnumber.append(" " + getString(R.string.songs))
            tv_playing_duration.text = data.getDurationAsFormattedText()
            // TODO cover wird noch nicht gut eingesetzt weil je nachdem Bitmap oder Drawable erforderlich ist -> Factory
            var cover = Drawable.createFromPath(data.cover_uri)
            if (cover == null){
                cover = resources.getDrawable(R.mipmap.ic_no_cover, activity?.theme)
            }
            ll_playing_fragment.background = cover
            if(shallShowLyrics()) {
                lyricsAbfragen(data.path)
            }
        }
    }

    private fun shallShowLyrics(): Boolean
            = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("PREF_LYRICSSHOW",true)

    private fun lyricsAbfragen(filepath: String){
        iv_playing_lyrics.alpha = 0.3f
        this.lyrics = null
        val texte = LyricsAccess.readLyrics(filepath)
        if(texte != ""){
            this.lyrics = texte
            iv_playing_lyrics.alpha = 1.0f
        }
    }

    private fun setPlaylist(queue: MusicList) {
        val liste = ArrayList<String>()
        for (item in queue) {
            liste.add(item.title + "\n - " + item.artist)
        }
        lv_playing_list.adapter = ArrayAdapter(requireActivity().applicationContext,
                R.layout.row_playlist_tv, liste.toTypedArray())
    }

    override fun onResume() {
        super.onResume()
        setAccentColorIfLineageTheme()
        if(lyrics != null && lyrics != ""){
            iv_playing_lyrics.alpha = 1.0f
        }
    }

    private fun setAccentColorIfLineageTheme(){
        val color = ThemeChanger.getAccentColorLinage(activity)
        if(color != 0){
            iv_playing_lyrics.backgroundTintList = ColorStateList.valueOf(color)
            iv_playing_lyrics.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
        }
    }
}
