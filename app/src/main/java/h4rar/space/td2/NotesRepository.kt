package h4rar.space.td2

import kotlinx.coroutines.flow.Flow

class NotesRepository(
    private val tabDao: TabDao,
    private val noteDao: NoteDao
) {
    val allTabs: Flow<List<Tab>> = tabDao.getAllTabs()

    fun getNotesByTab(tabId: Int): Flow<List<Note>> = noteDao.getNotesByTab(tabId)

    suspend fun insertTab(tab: Tab) = tabDao.insert(tab)
    suspend fun updateTab(tab: Tab) = tabDao.update(tab)
    suspend fun deleteTab(tab: Tab) = tabDao.delete(tab)

    suspend fun insertNote(note: Note) = noteDao.insert(note)
    suspend fun updateNote(note: Note) = noteDao.update(note)
    suspend fun deleteNote(note: Note) = noteDao.delete(note)
}