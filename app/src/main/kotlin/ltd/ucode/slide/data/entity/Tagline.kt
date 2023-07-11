package ltd.ucode.slide.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ltd.ucode.lemmy.api.response.GetSiteResponse
import ltd.ucode.lemmy.data.type.Tagline as LemmyTagline

@Entity(tableName = "taglines", indices = [
    Index(value = ["instance_rowid", "tagline_id"], unique = true),
], foreignKeys = [
    ForeignKey(entity = Site::class,
        parentColumns = ["rowid"],
        childColumns = ["instance_rowid"])
])
data class Tagline(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = -1,
        @ColumnInfo(name = "instance_rowid") val instanceRowId: Int,
        @ColumnInfo(name = "tagline_id") val taglineId: Int,
        val content: String,
        val discovered: Instant = Clock.System.now(),
        val created: Instant = Instant.DISTANT_PAST,
        val updated: Instant? = null,
) {
    override fun toString(): String {
        return content
    }

    companion object {
        fun from(other: GetSiteResponse, site: Site): List<Tagline> {
            return other.taglines.orEmpty().map { from(it, site) }
        }

        fun from(other: LemmyTagline, site: Site): Tagline {
            return Tagline(
                instanceRowId = site.rowId,
                taglineId = other.id,
                content = other.content,
                created = other.published.toInstant(TimeZone.UTC),
                updated = other.updated?.toInstant(TimeZone.UTC),
            )
        }
    }
}
