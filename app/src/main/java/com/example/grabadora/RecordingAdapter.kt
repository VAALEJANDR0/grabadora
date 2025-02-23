package com.example.grabadora

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import java.io.File

class RecordingAdapter(
    private val context: Context,
    private val recordings: MutableList<String>
) : RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPosition = -1

    inner class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recordingName: MaterialTextView = itemView.findViewById(R.id.recordingName)
        val playButton: MaterialButton = itemView.findViewById(R.id.playButton)
        val stopButton: MaterialButton = itemView.findViewById(R.id.stopButton)
        val editButton: MaterialButton = itemView.findViewById(R.id.editButton)
        val shareButton: MaterialButton = itemView.findViewById(R.id.shareButton)
        val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        return RecordingViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recording, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val file = File(recordings[position])
        holder.recordingName.text = file.name
        updateButtonVisibility(holder, position)

        holder.playButton.setOnClickListener { playAudio(file.absolutePath, position, holder) }
        holder.stopButton.setOnClickListener { stopAudio(holder) }
        holder.editButton.setOnClickListener { showRenameDialog(file, position) }
        holder.shareButton.setOnClickListener { shareAudio(file) }
        holder.deleteButton.setOnClickListener { deleteAudio(file, position) }
    }

    // --- Funciones modificadas ---
    private fun playAudio(path: String, position: Int, holder: RecordingViewHolder) {
        if (currentlyPlayingPosition != -1) {
            notifyItemChanged(currentlyPlayingPosition)
        }

        mediaPlayer?.release()

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
    }

    private fun stopAudio(holder: RecordingViewHolder) {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        val previousPosition = currentlyPlayingPosition
        currentlyPlayingPosition = -1
        notifyItemChanged(previousPosition)
    }

    private fun updateButtonVisibility(holder: RecordingViewHolder, position: Int) {
        if (position == currentlyPlayingPosition) {
            holder.playButton.visibility = View.GONE
            holder.stopButton.visibility = View.VISIBLE
        } else {
            holder.playButton.visibility = View.VISIBLE
            holder.stopButton.visibility = View.GONE
        }
    }
    // ----------------------------

    // --- Funciones sin cambios ---
    private fun showRenameDialog(file: File, position: Int) {
        val input = EditText(context).apply { setText(file.nameWithoutExtension) }
        AlertDialog.Builder(context)
            .setTitle("Renombrar")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotEmpty()) {
                    val newFile = File(file.parent, "$newName.3gp")
                    if (file.renameTo(newFile)) {
                        recordings[position] = newFile.absolutePath
                        notifyItemChanged(position)
                    } else {
                        Toast.makeText(context, "Error al renombrar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteAudio(file: File, position: Int) {
        if (file.delete()) {
            recordings.removeAt(position)
            notifyItemRemoved(position)
            Toast.makeText(context, "Archivo eliminado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareAudio(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir audio"))
    }

    override fun getItemCount(): Int = recordings.size
}