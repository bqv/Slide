package ltd.ucode.slide.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "languages", indices = [
    Index(value = ["name"], unique = true)
])
data class Language(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = -1,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "name") val name: String,
) {
    @Entity(tableName = "language_images", foreignKeys = [
        ForeignKey(entity = Site::class,
            parentColumns = ["rowid"],
            childColumns = ["site_rowid"])
    ])
    data class Image(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val id: Int = -1,
        @ColumnInfo(name = "language_rowid") val languageRowId: Int = -1,
        @ColumnInfo(name = "site_rowid") val siteRowId: Int,
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
