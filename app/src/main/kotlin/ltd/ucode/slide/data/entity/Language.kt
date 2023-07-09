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

@Entity(tableName = "languages", indices = [
    Index(value = ["name"], unique = true)
])
data class Language(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = -1,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "name") val name: String,
) {
    @Entity(tableName = "language_images")
    data class Image(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val id: Int = -1,
        @ColumnInfo(name = "language_rowid") val languageRowId: Int,
        @ColumnInfo(name = "instance_rowid") val instanceRowId: Int,
        @ColumnInfo(name = "language_id") val languageId: Int,
    )
}
