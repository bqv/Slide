package ltd.ucode.slide.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Entity(tableName = "taglines", indices = [
    Index(value = ["name"], unique = true),
], foreignKeys = [
    ForeignKey(entity = Site::class,
        parentColumns = ["rowid"],
        childColumns = ["instance_rowid"])
])
data class Tagline(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "instance_rowid") val instanceRowId: Int,
    val content: String,
    val discovered: Instant = Clock.System.now(),
    val updated: Instant? = null,
) {
    override fun toString(): String {
        return content
    }
}
