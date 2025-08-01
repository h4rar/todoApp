package h4rar.space.td2

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs")
    fun getAllTabs(): Flow<List<Tab>>

    @Insert
    suspend fun insert(tab: Tab)

    @Update
    suspend fun update(tab: Tab)

    @Delete
    suspend fun delete(tab: Tab)
}