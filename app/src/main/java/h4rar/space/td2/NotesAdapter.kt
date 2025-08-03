package h4rar.space.td2

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
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
    private var itemTouchHelper: ItemTouchHelper? = null
    private var originalPosition: Int = -1
    private var currentPosition: Int = -1
    private var isNoteLongPressed: Boolean = false

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteText: TextView = itemView.findViewById(R.id.noteText)
        val editNoteIcon: ImageView = itemView.findViewById(R.id.editNoteIcon)
        val deleteNoteIcon: ImageView = itemView.findViewById(R.id.deleteNoteIcon)
        val dragHandle: ImageView = itemView.findViewById(R.id.dragHandle)
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

        holder.editNoteIcon.visibility = if (isNoteLongPressed) View.VISIBLE else View.GONE
        holder.deleteNoteIcon.visibility = if (isNoteLongPressed) View.VISIBLE else View.GONE
        holder.dragHandle.visibility = if (isNoteLongPressed) View.VISIBLE else View.GONE

        // Настройка drag handle для начала перетаскивания
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                itemTouchHelper?.startDrag(holder)
            }
            false
        }

        holder.itemView.setOnLongClickListener {
            isNoteLongPressed = !isNoteLongPressed
            notifyDataSetChanged()
            true
        }

        holder.itemView.setOnClickListener {
            if (isNoteLongPressed) {
                isNoteLongPressed = false
                notifyDataSetChanged()
            } else {
                // Переключаем статус isCompleted
                CoroutineScope(Dispatchers.IO).launch {
                    repository.updateNote(note.copy(isCompleted = !note.isCompleted))
                }
            }
        }
        
        // Предотвращаем скрытие кнопок при нажатии на сами кнопки
        holder.editNoteIcon.setOnClickListener {
            onEditNote(note)
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
        

    }

    override fun getItemCount(): Int = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    fun setLongPressed(longPressed: Boolean) {
        isNoteLongPressed = longPressed
        notifyDataSetChanged()
    }
    
    fun hideButtons() {
        if (isNoteLongPressed) {
            isNoteLongPressed = false
            notifyDataSetChanged()
        }
    }

    fun setItemTouchHelper(touchHelper: ItemTouchHelper) {
        itemTouchHelper = touchHelper
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= notes.size || toPosition >= notes.size) {
            return
        }
        
        // Если это первое перемещение, запоминаем исходную позицию
        if (originalPosition == -1) {
            originalPosition = fromPosition
        }
        
        currentPosition = toPosition
        
        // Просто перемещаем элемент в списке для отображения
        val movedNote = notes[fromPosition]
        val updatedNotes = notes.toMutableList()
        updatedNotes.removeAt(fromPosition)
        updatedNotes.add(toPosition, movedNote)
        notes = updatedNotes
        
        notifyItemMoved(fromPosition, toPosition)
    }
    
    fun onItemDrop() {
        if (originalPosition != -1 && currentPosition != -1 && originalPosition != currentPosition) {
            // Обновляем позиции всех элементов
            val updatedNotes = notes.toMutableList()
            for (i in updatedNotes.indices) {
                updatedNotes[i] = updatedNotes[i].copy(position = i)
            }
            notes = updatedNotes
            
            // Сохраняем все позиции в БД асинхронно
            CoroutineScope(Dispatchers.IO).launch {
                repository.updateMultipleNotePositions(notes)
            }
        }
        
        // Сбрасываем позиции
        originalPosition = -1
        currentPosition = -1
    }
}