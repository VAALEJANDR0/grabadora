package com.example.grabadora

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File

class AudioDetailActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var runnable: Runnable
    private var isPlaying = false
    private var playbackSpeed = 1.0f
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_detail)

        val audioPath = intent.getStringExtra("audioPath") ?: return
        val audioFile = File(audioPath)

        findViewById<TextView>(R.id.title).text = audioFile.nameWithoutExtension
        setupMediaPlayer(audioPath)
        setupControls()
    }

    private fun setupMediaPlayer(path: String) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepareAsync()
            setOnPreparedListener {
                findViewById<TextView>(R.id.totalTime).text = formatTime(it.duration)
            }
            setOnCompletionListener {
                resetPlayback() // Reset al finalizar
            }
        }
    }

    private fun setupControls() {
        val progressBar = findViewById<LinearProgressIndicator>(R.id.progressBar)
        val currentTime = findViewById<TextView>(R.id.currentTime)
        val speedIndicator = findViewById<TextView>(R.id.tvSpeedIndicator)

        runnable = Runnable {
            currentTime.text = formatTime(mediaPlayer.currentPosition)
            progressBar.progress = calculateProgress(
                mediaPlayer.currentPosition,
                mediaPlayer.duration
            )
            handler.postDelayed(runnable, 1000)
        }

        findViewById<MaterialButton>(R.id.btnPlayPause).setOnClickListener {
            if (isPlaying) pauseAudio() else playAudio()
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnSpeed).setOnClickListener {
            playbackSpeed = when (playbackSpeed) {
                1.0f -> 1.5f
                1.5f -> 2.0f
                else -> 1.0f
            }
            mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(playbackSpeed)
            speedIndicator.text = "Velocidad: ${playbackSpeed}x"
        }
    }

    private fun playAudio() {
        mediaPlayer.setOnCompletionListener { resetPlayback() } // Asegurar listener
        mediaPlayer.start()
        isPlaying = true
        findViewById<MaterialButton>(R.id.btnPlayPause).icon =
            ContextCompat.getDrawable(this, R.drawable.ic_pause)
        handler.post(runnable)
    }

    private fun pauseAudio() {
        mediaPlayer.pause()
        isPlaying = false
        findViewById<MaterialButton>(R.id.btnPlayPause).icon =
            ContextCompat.getDrawable(this, R.drawable.ic_play)
    }

    private fun resetPlayback() {
        handler.removeCallbacks(runnable) // Detener actualizaciones
        isPlaying = false
        mediaPlayer.seekTo(0) // Reiniciar posición
        findViewById<MaterialButton>(R.id.btnPlayPause).icon =
            ContextCompat.getDrawable(this, R.drawable.ic_play)
        findViewById<TextView>(R.id.currentTime).text = "00:00"
        findViewById<LinearProgressIndicator>(R.id.progressBar).progress = 0
    }

    private fun formatTime(ms: Int): String {
        val minutes = (ms / 1000) / 60
        val seconds = (ms / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun calculateProgress(current: Int, total: Int): Int {
        return if (total > 0) (current.toFloat() / total.toFloat() * 100).toInt() else 0
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable) // Limpiar Handler
        mediaPlayer.release() // Liberar recursos
    }
}