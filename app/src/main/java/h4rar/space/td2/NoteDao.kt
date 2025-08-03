package h4rar.space.td2

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE tabId = :tabId ORDER BY position ASC")
    fun getNotesByTab(tabId: Int): Flow<List<Note>>

    @Insert
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("UPDATE notes SET position = :newPosition WHERE id = :noteId")
    suspend fun updatePosition(noteId: Int, newPosition: Int)

    @Query("UPDATE notes SET position = position + 1 WHERE tabId = :tabId AND position >= :fromPosition")
    suspend fun shiftPositionsUp(tabId: Int, fromPosition: Int)

    @Query("UPDATE notes SET position = position - 1 WHERE tabId = :tabId AND position > :fromPosition")
    suspend fun shiftPositionsDown(tabId: Int, fromPosition: Int)
}