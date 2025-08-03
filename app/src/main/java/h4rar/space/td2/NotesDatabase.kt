package h4rar.space.td2

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Tab::class, Note::class], version = 2)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun noteDao(): NoteDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавляем поле position с значением по умолчанию 0
                database.execSQL("ALTER TABLE notes ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                
                // Обновляем позиции существующих записей
                database.execSQL("""
                    UPDATE notes 
                    SET position = (
                        SELECT COUNT(*) 
                        FROM notes n2 
                        WHERE n2.tabId = notes.tabId AND n2.id <= notes.id
                    ) - 1
                """)
            }
        }
    }
}