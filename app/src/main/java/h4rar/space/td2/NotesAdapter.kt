package h4rar.space.td2

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesAdapter(
    private val repository: NotesRepository,
    private var isLongPressed: Boolean = false,
    private val onEditNote: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private var notes: List<Note> = emptyList()

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteText: TextView = itemView.findViewById(R.id.noteText)
        val editNoteIcon: ImageView = itemView.findViewById(R.id.editNoteIcon)
        val deleteNoteIcon: ImageView = itemView.findViewById(R.id.deleteNoteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.noteText.text = note.text

        // Настройка зачёркивания и цвета текста
        if (note.isCompleted) {
            holder.noteText.paintFlags = holder.noteText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.noteText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.light_gray))
        } else {
            holder.noteText.paintFlags = holder.noteText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.noteText.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
        }

        holder.editNoteIcon.visibility = if (isLongPressed) View.VISIBLE else View.GONE
        holder.deleteNoteIcon.visibility = if (isLongPressed) View.VISIBLE else View.GONE

        holder.itemView.setOnLongClickListener {
            isLongPressed = !isLongPressed
            notifyDataSetChanged()
            true
        }

        holder.itemView.setOnClickListener {
            if (isLongPressed) {
                isLongPressed = false
                notifyDataSetChanged()
            } else {
                // Переключаем статус isCompleted
                CoroutineScope(Dispatchers.IO).launch {
                    repository.updateNote(note.copy(isCompleted = !note.isCompleted))
                }
            }
        }

        holder.deleteNoteIcon.setOnClickListener {
            MaterialAlertDialogBuilder(holder.itemView.context)
                .setTitle("Delete note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Ok") { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.deleteNote(note)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        holder.editNoteIcon.setOnClickListener {
            onEditNote(note)
        }
    }

    override fun getItemCount(): Int = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    fun setLongPressed(longPressed: Boolean) {
        isLongPressed = longPressed
        notifyDataSetChanged()
    }
}