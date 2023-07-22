package ltd.ucode.slide.data.common.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Entity(tableName = "_comment_vote", indices = [
    Index(value = ["comment_rowid"])
], foreignKeys = [
    ForeignKey(entity = Account::class,
        parentColumns = ["rowid"],
        childColumns = ["account_rowid"])
])
data class CommentVote(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Long = 0,
    @ColumnInfo(name = "account_rowid") val accountRowId: Long,
    @ColumnInfo(name = "comment_rowid") val commentRowId: Long,
    @ColumnInfo(name = "vote") val vote: Int, // -1, 0, +1

    val discovered: Instant = Clock.System.now(),
    val updated: Instant? = null, // home instance
) {
}
