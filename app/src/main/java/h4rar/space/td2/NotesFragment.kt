package h4rar.space.td2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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

        recyclerView.layoutManager = LinearLayoutManager(context)
        notesAdapter = NotesAdapter(repository, isNoteLongPressed) { note ->
            showEditNoteDialog(note)
        }
        recyclerView.adapter = notesAdapter

        CoroutineScope(Dispatchers.Main).launch {
            repository.getNotesByTab(tabId).collect { notes ->
                notesAdapter.updateNotes(notes)
            }
        }

        return view
    }

    fun resetLongPressMode() {
        if (isNoteLongPressed) {
            isNoteLongPressed = false
            notesAdapter.setLongPressed(false)
        }
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
                            isNoteLongPressed = false
                            notesAdapter.setLongPressed(false)
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