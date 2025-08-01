package h4rar.space.td2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabs")
data class Tab(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)