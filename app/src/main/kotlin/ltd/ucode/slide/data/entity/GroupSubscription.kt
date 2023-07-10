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

@Entity(tableName = "group_subscription", indices = [
    Index(value = ["account_rowid"])
], foreignKeys = [
    ForeignKey(entity = Account::class,
        parentColumns = ["rowid"],
        childColumns = ["account_rowid"])
])
data class GroupSubscription(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = -1,
    @ColumnInfo(name = "account_rowid") val accountRowId: Int,
    @ColumnInfo(name = "group_rowid") val groupRowId: Int,
    @ColumnInfo(name = "pending") val pending: Boolean,

    val discovered: Instant = Clock.System.now(),
    val updated: Instant? = null, // home instance
) {
}
