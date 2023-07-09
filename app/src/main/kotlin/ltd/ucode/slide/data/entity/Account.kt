package ltd.ucode.slide.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ltd.ucode.lemmy.data.type.Person
import ltd.ucode.lemmy.data.type.PersonAggregates
import ltd.ucode.lemmy.data.type.PersonView
import ltd.ucode.slide.data.IUser

@Entity(tableName = "accounts", indices = [
    Index(value = ["username"], unique = true)
], foreignKeys = [
    ForeignKey(entity = Site::class,
        parentColumns = ["rowid"],
        childColumns = ["instance_rowid"])
])
data class Account(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = -1,
    @ColumnInfo(name = "instance_rowid") val instanceRowId: Int,
    @ColumnInfo(name = "username") val username: String,

    val discovered: Instant = Clock.System.now(),
    val updated: Instant? = null, // home instance
) {
}
