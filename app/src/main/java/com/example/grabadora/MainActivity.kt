package com.example.grabadora

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private lateinit var fileName: String
    private lateinit var recordingsRecyclerView: RecyclerView
    private lateinit var statusTextView: MaterialTextView
    private lateinit var recordingCard: MaterialCardView
    private lateinit var btnStopRecording: MaterialButton
    private lateinit var fabRecord: FloatingActionButton
    private lateinit var emptyState: View
    private val recordings = mutableListOf<String>()
    private lateinit var recordingAdapter: RecordingAdapter

    private val storageDir: File by lazy {
        File(getExternalFilesDir(null), "recordings").apply { mkdirs() }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startRecording()
        } else {
            Toast.makeText(this, "Permisos requeridos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        fabRecord = findViewById(R.id.fabRecord)
        recordingCard = findViewById(R.id.recordingCard)
        statusTextView = findViewById(R.id.statusTextView)
        btnStopRecording = findViewById(R.id.btnStopRecording)
        recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView)
        emptyState = findViewById(R.id.emptyState)

        // Configurar RecyclerView
        recordingsRecyclerView.layoutManager = LinearLayoutManager(this)
        recordingAdapter = RecordingAdapter(this, recordings)
        recordingsRecyclerView.adapter = recordingAdapter

        loadRecordings()
        checkEmptyState()

        // Listeners
        fabRecord.setOnClickListener {
            if (checkPermissions()) {
                startRecording()
            } else {
                requestPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }

        btnStopRecording.setOnClickListener {
            stopRecording()
        }
    }

    private fun startRecording() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        fileName = "${storageDir.absolutePath}/recording_$timeStamp.m4a"

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            try {
                prepare()
                start()
                updateUIForRecording(true)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al grabar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al detener: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        mediaRecorder = null

        if (::fileName.isInitialized) {
            recordings.add(fileName)
            saveRecordings()
            recordingAdapter.notifyDataSetChanged()
            updateUIForRecording(false)
            checkEmptyState()
            Toast.makeText(this, "Grabación guardada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkEmptyState() {
        if (recordings.isEmpty()) {
            recordingsRecyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            recordingsRecyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun updateUIForRecording(isRecording: Boolean) {
        if (isRecording) {
            fabRecord.hide()
            recordingCard.visibility = View.VISIBLE
            btnStopRecording.visibility = View.VISIBLE
            statusTextView.text = "Grabando..."
        } else {
            fabRecord.show()
            recordingCard.visibility = View.GONE
            btnStopRecording.visibility = View.GONE
            statusTextView.text = "Listo para grabar"
        }
    }

    private fun saveRecordings() {
        val sharedPref = getSharedPreferences("recordings_prefs", MODE_PRIVATE)
        sharedPref.edit().putStringSet("recordings", recordings.toSet()).apply()
    }

    private fun loadRecordings() {
        val sharedPref = getSharedPreferences("recordings_prefs", MODE_PRIVATE)
        recordings.clear()
        recordings.addAll(sharedPref.getStringSet("recordings", emptySet()) ?: emptySet())
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
    }
}