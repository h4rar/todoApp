package h4rar.space.td2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesFragment : Fragment() {
    private var tabId: Int = 0
    private lateinit var repository: NotesRepository
    private var isNoteLongPressed = false
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tabId = it.getInt(ARG_TAB_ID)
        }
        repository = NotesRepository(
            (activity as MainActivity).db.tabDao(),
            (activity as MainActivity).db.noteDao()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notes, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.notesRecyclerView)
        val fragmentRoot = view.findViewById<View>(R.id.fragmentRoot)

        recyclerView.layoutManager = LinearLayoutManager(context)
        notesAdapter = NotesAdapter(repository, isNoteLongPressed) { note ->
            showEditNoteDialog(note)
        }
        recyclerView.adapter = notesAdapter

        // Настройка ItemTouchHelper для drag & drop
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition
                
                // Проверяем, что позиции валидны
                if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                    return false
                }
                
                notesAdapter.onItemMove(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Не используется
            }
            
            override fun isLongPressDragEnabled(): Boolean {
                return false // Отключаем автоматическое перетаскивание по долгому нажатию
            }
            
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    // Перетаскивание завершено
                    notesAdapter.onItemDrop()
                }
            }
        }
        
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
        notesAdapter.setItemTouchHelper(itemTouchHelper)

        // Обработчики нажатий для скрытия кнопок записей
        recyclerView.setOnClickListener {
            notesAdapter.hideButtons()
        }

        fragmentRoot.setOnClickListener {
            notesAdapter.hideButtons()
        }

        view.setOnClickListener {
            notesAdapter.hideButtons()
        }

        // TouchListener для более надежного скрытия кнопок
        view.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                notesAdapter.hideButtons()
            }
            false // Позволяем событию продолжить обработку
        }

        // Обработчик нажатия на пустую область RecyclerView
        recyclerView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                notesAdapter.hideButtons()
            }
            false // Позволяем событию продолжить обработку
        }

        CoroutineScope(Dispatchers.Main).launch {
            repository.getNotesByTab(tabId).collect { notes ->
                notesAdapter.updateNotes(notes)
            }
        }

        return view
    }

    fun resetLongPressMode() {
        notesAdapter.hideButtons()
    }

    private fun showEditNoteDialog(note: Note) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_note, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.noteInput)
        editText.setText(note.text)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Note")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.updateNote(note.copy(text = newText))
                        requireActivity().runOnUiThread {
                            notesAdapter.hideButtons()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val ARG_TAB_ID = "tab_id"

        fun newInstance(tabId: Int): NotesFragment {
            val fragment = NotesFragment()
            val args = Bundle()
            args.putInt(ARG_TAB_ID, tabId)
            fragment.arguments = args
            return fragment
        }
    }
}