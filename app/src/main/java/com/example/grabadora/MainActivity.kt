package com.example.grabadora

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var fileName: String
    private lateinit var recordingsListView: ListView
    private lateinit var statusTextView: TextView
    private val recordings = mutableListOf<String>()
    private lateinit var recordingAdapter: RecordingAdapter
    private val storageDir: File by lazy {
        File(getExternalFilesDir(null), "recordings").apply { mkdirs() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recordButton: Button = findViewById(R.id.recordButton)
        val stopButton: Button = findViewById(R.id.stopButton)
        statusTextView = findViewById(R.id.statusTextView)
        recordingsListView = findViewById(R.id.recordingsListView)

        loadRecordings()  // Cargar grabaciones almacenadas

        recordingAdapter = RecordingAdapter(this, recordings)
        recordingsListView.adapter = recordingAdapter

        if (!checkPermissions()) requestPermissions()

        recordButton.setOnClickListener {
            if (checkPermissions()) {
                startRecording()
                recordButton.isEnabled = false
                stopButton.isEnabled = true
                stopButton.visibility = Button.VISIBLE
                statusTextView.text = "Grabando..."
            } else {
                requestPermissions()
            }
        }

        stopButton.setOnClickListener {
            stopRecording()
            recordButton.isEnabled = true
            stopButton.isEnabled = false
            stopButton.visibility = Button.GONE
            statusTextView.text = "Grabación finalizada"
        }
    }

    private fun startRecording() {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        fileName = "${storageDir.absolutePath}/recording_$timeStamp.3gp"

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Error al iniciar la grabación", Toast.LENGTH_SHORT).show()
            }
        }

        statusTextView.text = "Grabando..."
        Toast.makeText(this, "Grabando audio...", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Error al detener la grabación", Toast.LENGTH_SHORT).show()
            }
        }
        mediaRecorder = null

        if (::fileName.isInitialized) {
            recordings.add(fileName)
            saveRecordings()
            recordingAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Grabación guardada en: $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRecordings() {
        val sharedPreferences = getSharedPreferences("recordings_prefs", MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("recordings", recordings.toSet()).apply()
    }

    private fun loadRecordings() {
        val sharedPreferences = getSharedPreferences("recordings_prefs", MODE_PRIVATE)
        val savedRecordings = sharedPreferences.getStringSet("recordings", emptySet()) ?: emptySet()

        recordings.clear()
        savedRecordings.forEach { filePath ->
            if (File(filePath).exists()) {
                recordings.add(filePath)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        requestPermissionsLauncher.launch(
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        )
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true && permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true) {
            Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()
        }
    }
}
