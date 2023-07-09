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

@Entity(tableName = "post_votes", indices = [
    Index(value = ["post_rowid"], unique = true)
], foreignKeys = [
    ForeignKey(entity = Account::class,
        parentColumns = ["rowid"],
        childColumns = ["account_rowid"])
])
data class PostVote(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = -1,
    @ColumnInfo(name = "account_rowid") val accountRowId: Int,
    @ColumnInfo(name = "post_rowid") val postRowId: Int,
    @ColumnInfo(name = "vote") val vote: Int, // -1, 0, +1

    val discovered: Instant = Clock.System.now(),
    val updated: Instant? = null, // home instance
) {
}
