package ltd.ucode.slide.data.common.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ltd.ucode.network.data.ISite

@Entity(tableName = "sites", indices = [
    Index(value = ["name"], unique = true)
])
data class Site(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = -1,
    @ColumnInfo(collate = ColumnInfo.NOCASE) override val name: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) override val software: String? = null,
    @ColumnInfo(collate = ColumnInfo.NOCASE) override val version: String? = null,

    @ColumnInfo(name = "site_id") val ownSiteId: Int? = null,
    @ColumnInfo(name = "country_code",
        collate = ColumnInfo.NOCASE) override val countryCode: String? = null,
    @ColumnInfo(name = "local_posts") override val localPosts: Int? = 0,
    @ColumnInfo(name = "local_comments") override val localComments: Int? = 0,
    @ColumnInfo(name = "users_total") override val usersTotal: Int? = 0,
    @ColumnInfo(name = "users_semiannually") override val usersHalfYear: Int? = 0,
    @ColumnInfo(name = "users_monthly") override val usersMonthly: Int? = 0,
    @ColumnInfo(name = "users_weekly") override val usersWeekly: Int? = 0,
    @ColumnInfo(name = "users_daily") val usersDaily: Int? = 0,

    @ColumnInfo(defaultValue = "(CURRENT_TIMESTAMP * 1000)") val discovered: Instant = Clock.System.now(),
    @ColumnInfo(defaultValue = "0") val created: Instant = Instant.DISTANT_PAST,
    @ColumnInfo(defaultValue = "NULL") val updated: Instant? = null,
    @ColumnInfo(defaultValue = "0") val refreshed: Instant = Instant.DISTANT_PAST,
) : ISite {
    @Ignore lateinit var _taglines: MutableList<Tagline>
    @Ignore lateinit var admins: MutableList<out User>
    @Ignore var inaccessibleSince: Instant? = null

    override val taglines: List<String>
        get() = _taglines.map { it.content }

    @Entity(tableName = "site_images", indices = [
        Index(value = ["local_site_rowid", "remote_site_rowid"], unique = true),
        Index(value = ["local_site_rowid", "remote_site_id"], unique = true),
    ])
    data class Image(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = -1,
        @ColumnInfo(name = "local_site_rowid") val localSiteRowId: Int,
        @ColumnInfo(name = "remote_site_rowid") val remoteSiteRowId: Int,
        @ColumnInfo(name = "remote_site_id") val remoteSiteId: Int,

        val discovered: Instant = Clock.System.now(),
        val updated: Instant? = null,
    )
}

