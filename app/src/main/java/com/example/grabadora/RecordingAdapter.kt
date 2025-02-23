package com.example.grabadora

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import java.io.File

class RecordingAdapter(private val context: Context, private val recordings: MutableList<String>) : BaseAdapter() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPosition: Int = -1 // Para rastrear el audio que se está reproduciendo

    override fun getCount(): Int = recordings.size

    override fun getItem(position: Int): Any = recordings[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_recording, parent, false)

        val recordingName: TextView = view.findViewById(R.id.recordingName)
        val playButton: Button = view.findViewById(R.id.playButton)
        val stopButton: Button = view.findViewById(R.id.stopButton)
        val editButton: Button = view.findViewById(R.id.editButton)
        val shareButton: Button = view.findViewById(R.id.shareButton) // Botón de compartir
        val deleteButton: Button = view.findViewById(R.id.deleteButton) // Botón de eliminar

        val filePath = recordings[position]
        val file = File(filePath)
        recordingName.text = file.name // Mostrar solo el nombre del archivo

        // Si este es el audio que se está reproduciendo, mostrar el botón de "stop"
        if (position == currentlyPlayingPosition) {
            playButton.visibility = Button.GONE
            stopButton.visibility = Button.VISIBLE
        } else {
            playButton.visibility = Button.VISIBLE
            stopButton.visibility = Button.GONE
        }

        // Reproducir audio cuando el botón de "play" sea presionado
        playButton.setOnClickListener {
            playRecording(filePath, position, playButton, stopButton)
        }

        // Detener reproducción cuando el botón de "stop" sea presionado
        stopButton.setOnClickListener {
            stopRecording(playButton, stopButton)
        }

        // Editar nombre cuando se presione el botón de editar
        editButton.setOnClickListener {
            showEditDialog(position, filePath)
        }

        // Compartir el audio cuando se presione el botón de compartir
        shareButton.setOnClickListener {
            shareRecording(filePath)
        }

        // Eliminar el audio cuando se presione el botón de eliminar
        deleteButton.setOnClickListener {
            deleteRecording(position, filePath)
        }

        return view
    }

    private fun playRecording(filePath: String, position: Int, playButton: Button, stopButton: Button) {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
                release()
            }
        }

        notifyDataSetChanged()

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                start()

                playButton.visibility = Button.GONE
                stopButton.visibility = Button.VISIBLE
                currentlyPlayingPosition = position

                setOnCompletionListener {
                    playButton.visibility = Button.VISIBLE
                    stopButton.visibility = Button.GONE
                    currentlyPlayingPosition = -1
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording(playButton: Button, stopButton: Button) {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
                release()
            }
            playButton.visibility = Button.VISIBLE
            stopButton.visibility = Button.GONE
        }
        mediaPlayer = null
        currentlyPlayingPosition = -1
    }

    private fun showEditDialog(position: Int, filePath: String) {
        val file = File(filePath)
        val currentName = file.nameWithoutExtension

        val builder = AlertDialog.Builder(context)
        val input = EditText(context).apply {
            setText(currentName)
        }

        builder.setTitle("Editar nombre")
            .setView(input)
            .setPositiveButton("Aceptar") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    renameFile(file, newName)
                    recordings[position] = "${file.parent}/$newName.3gp"
                    notifyDataSetChanged()
                    Toast.makeText(context, "Nombre actualizado a $newName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun renameFile(file: File, newName: String) {
        val newFile = File(file.parent, "$newName.3gp")
        if (!file.renameTo(newFile)) {
            Toast.makeText(context, "Error al renombrar el archivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteRecording(position: Int, filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            if (file.delete()) {
                recordings.removeAt(position)
                notifyDataSetChanged()
                Toast.makeText(context, "Audio eliminado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No se pudo eliminar el archivo", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "El archivo no existe", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareRecording(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "El archivo no existe", Toast.LENGTH_SHORT).show()
            return
        }

        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Escucha este audio grabado en mi app!")
        }

        if (shareIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(Intent.createChooser(shareIntent, "Compartir audio"))
        } else {
            Toast.makeText(context, "No hay aplicaciones disponibles para compartir el audio", Toast.LENGTH_SHORT).show()
        }
    }
}
