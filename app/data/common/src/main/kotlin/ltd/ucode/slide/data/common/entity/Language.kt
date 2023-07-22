package ltd.ucode.slide.data.common.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "_language", indices = [
    Index(value = ["name"], unique = true)
])
data class Language(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Long = 0,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "name") val name: String,
) {
    @Entity(tableName = "_language_image", foreignKeys = [
        ForeignKey(entity = Site::class,
            parentColumns = ["rowid"],
            childColumns = ["site_rowid"])
    ])
    data class Image(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Long = 0,
        @ColumnInfo(name = "language_rowid") val languageRowId: Long = 0,
        @ColumnInfo(name = "site_rowid") val siteRowId: Long,
        @ColumnInfo(name = "language_id") val languageId: Int,
    )

    /*
    companion object {
        fun from(other: GetSiteResponse, site: Site): List<Pair<Language, Image>> {
            return other.allLanguages.map { from(it, site) }
        }

        fun from(other: LemmyLanguage, site: Site): Pair<Language, Image> {
            return Pair(
                Language(
                    code = other.code.code,
                    name = other.name,
                ),
                Image(
                    siteRowId = site.rowId,
                    languageId = other.id.id,
                )
            )
        }
    }
     */
}
