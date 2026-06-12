package com.example.aplicacionmovil.presentation

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import com.example.aplicacionmovil.R

class Prueba : Activity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.prueba)

        val botonAudio: Button = findViewById(R.id.botonAudio)
        val volver: Button = findViewById(R.id.volver)
        val btnChat: Button = findViewById(R.id.btnChat)

        botonAudio.setOnClickListener {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, R.raw.sonido)
                mediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        btnChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        volver.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}