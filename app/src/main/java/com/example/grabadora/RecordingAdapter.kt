package com.example.grabadora

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import java.io.File
import java.io.IOException

class RecordingAdapter(
    private val context: Context,
    private val recordings: MutableList<String>
) : RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPosition = -1

    inner class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recordingName: MaterialTextView = itemView.findViewById(R.id.recordingName)
        val recordingDuration: MaterialTextView = itemView.findViewById(R.id.recordingDuration)
        val playButton: MaterialButton = itemView.findViewById(R.id.playButton)
        val stopButton: MaterialButton = itemView.findViewById(R.id.stopButton)
        val editButton: MaterialButton = itemView.findViewById(R.id.editButton)
        val shareButton: MaterialButton = itemView.findViewById(R.id.shareButton)
        val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)
        val icPlaying: ImageView = itemView.findViewById(R.id.icPlaying)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val intent = Intent(context, AudioDetailActivity::class.java).apply {
                        putExtra("audioPath", recordings[position])
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        return RecordingViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val file = File(recordings[position])
        holder.recordingName.text = file.name
        holder.recordingDuration.text = getAudioDuration(file.absolutePath)
        updateButtonVisibility(holder, position)

        holder.playButton.setOnClickListener { playAudio(file.absolutePath, position, holder) }
        holder.stopButton.setOnClickListener { stopAudio() }
        holder.editButton.setOnClickListener { showRenameDialog(file, position) }
        holder.shareButton.setOnClickListener { shareAudio(file) }
        holder.deleteButton.setOnClickListener { deleteAudio(file, position) }
    }

    private fun playAudio(path: String, position: Int, holder: RecordingViewHolder) {
        if (currentlyPlayingPosition != -1) {
            notifyItemChanged(currentlyPlayingPosition)
        }

        mediaPlayer?.release()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                currentlyPlayingPosition = position
                updateButtonVisibility(holder, position)

                setOnCompletionListener {
                    currentlyPlayingPosition = -1
                    notifyItemChanged(position)
                }
            }
        } catch (e: IOException) {
            Toast.makeText(context, "Error al reproducir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        val previousPosition = currentlyPlayingPosition
        currentlyPlayingPosition = -1
        if (previousPosition != -1) notifyItemChanged(previousPosition)
    }

    private fun getAudioDuration(path: String): String {
        return try {
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
            }
            val durationMs = mediaPlayer.duration
            mediaPlayer.release()

            val minutes = (durationMs / 1000) / 60
            val seconds = (durationMs / 1000) % 60
            String.format("%02d:%02d", minutes, seconds)
        } catch (e: Exception) {
            "00:00"
        }
    }

    private fun updateButtonVisibility(holder: RecordingViewHolder, position: Int) {
        if (position == currentlyPlayingPosition) {
            holder.playButton.visibility = View.GONE
            holder.stopButton.visibility = View.VISIBLE
            holder.icPlaying.visibility = View.VISIBLE
            holder.playButton.setBackgroundColor(ContextCompat.getColor(context, R.color.play_button_active))
        } else {
            holder.playButton.visibility = View.VISIBLE
            holder.stopButton.visibility = View.GONE
            holder.icPlaying.visibility = View.GONE
            holder.playButton.setBackgroundColor(ContextCompat.getColor(context, R.color.play_button_normal))
        }
    }

    private fun showRenameDialog(file: File, position: Int) {
        val input = EditText(context).apply { setText(file.nameWithoutExtension) }
        AlertDialog.Builder(context)
            .setTitle("Renombrar")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) return@setPositiveButton

                val newFile = File(file.parent, "$newName.m4a")  // Extensión .m4a
                if (newFile.exists()) {
                    Toast.makeText(context, "Nombre ya existe", Toast.LENGTH_SHORT).show()
                } else if (file.renameTo(newFile)) {
                    recordings[position] = newFile.absolutePath
                    notifyItemChanged(position)
                } else {
                    Toast.makeText(context, "Error al renombrar", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteAudio(file: File, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("¿Eliminar grabación?")
            .setMessage("No se puede deshacer esta acción")
            .setPositiveButton("Eliminar") { _, _ ->
                if (position == currentlyPlayingPosition) stopAudio()
                if (file.delete()) {
                    recordings.removeAt(position)
                    notifyItemRemoved(position)
                    Toast.makeText(context, "Eliminado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun shareAudio(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/mp4"  // Tipo MIME para .m4a
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error al compartir", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = recordings.size
}