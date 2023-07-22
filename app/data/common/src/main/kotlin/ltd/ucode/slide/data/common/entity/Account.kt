package ltd.ucode.slide.data.common.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Entity(tableName = "_account", indices = [
    Index(value = ["username"], unique = true)
], foreignKeys = [
    ForeignKey(entity = Site::class,
        parentColumns = ["rowid"],
        childColumns = ["site_rowid"])
])
data class Account(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Long = 0,
    @ColumnInfo(name = "site_rowid") val siteRowId: Long,
    @ColumnInfo(name = "username") val username: String,

    val discovered: Instant = Clock.System.now(),
    val updated: Instant? = null, // home instance
) {
}
