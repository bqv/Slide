package ltd.ucode.slide.data.common.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "_seen", primaryKeys = ["key"])
data class Seen(
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String,
)
