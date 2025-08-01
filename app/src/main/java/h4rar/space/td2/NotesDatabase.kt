package h4rar.space.td2

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Tab::class, Note::class], version = 1)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun noteDao(): NoteDao
}