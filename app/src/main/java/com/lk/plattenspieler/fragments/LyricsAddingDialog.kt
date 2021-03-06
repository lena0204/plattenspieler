package com.lk.plattenspieler.fragments

import android.app.*
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivityNew

/**
 * Erstellt von Lena am 23.04.18.
 * Stellt einen Dialog mit Textfeld für das Hinzufügen von Liedtexten bereit
 */
class LyricsAddingDialog: DialogFragment(){

	private lateinit var listener: OnSaveLyrics

	interface OnSaveLyrics{
		fun onSaveLyrics(lyrics: String)
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		listener = context as MainActivityNew
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		super.onCreateDialog(savedInstanceState)
		val li = activity?.layoutInflater
		val view = li?.inflate(R.layout.dialog_lyrics_adding, null)
		val et = view?.findViewById(R.id.et_lyrics_add) as EditText
		val builder = AlertDialog.Builder(activity?.applicationContext)
		builder.setTitle(R.string.dialog_title)
		builder.setView(view)
		builder.setPositiveButton(R.string.dialog_yes) { _, _ ->
			if(!et.text.isNullOrEmpty()){
				listener.onSaveLyrics(et.text.toString())
			}
		}
		builder.setNegativeButton(R.string.dialog_no) {_, _ ->
			dismiss()
		}
		return builder.create()
	}
}