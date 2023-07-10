package ltd.ucode.slide.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import info.the_federation.graphql.generated.getlemmyserversquery.thefederation_node
import info.the_federation.graphql.generated.getlemmyserversquery.thefederation_stat
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ltd.ucode.slide.data.ISite

@Entity(tableName = "sites", indices = [
    Index(value = ["name"], unique = true)
])
data class Site(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = -1,
    @ColumnInfo(collate = ColumnInfo.NOCASE) override val name: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) override val software: String? = null,
    override val version: String? = null,

    @ColumnInfo(name = "country_code",
        collate = ColumnInfo.NOCASE) override val countryCode: String? = null,
    @ColumnInfo(name = "local_posts") override val localPosts: Int? = 0,
    @ColumnInfo(name = "local_comments") override val localComments: Int? = 0,
    @ColumnInfo(name = "users_total") override val usersTotal: Int? = 0,
    @ColumnInfo(name = "users_semiannual") override val usersHalfYear: Int? = 0,
    @ColumnInfo(name = "users_monthly") override val usersMonthly: Int? = 0,
    @ColumnInfo(name = "users_weekly") override val usersWeekly: Int? = 0,

    val discovered: Instant = Clock.System.now(),
    val updated: Instant? = null,
) : ISite {
    @Ignore lateinit var _taglines: MutableList<Tagline>
    @Ignore lateinit var admins: MutableList<out User>
    @Ignore var inaccessibleSince: Instant? = null

    override val taglines: List<String>
        get() = _taglines.map { it.content }

    @Entity(tableName = "site_images", indices = [
        Index(value = ["local_site_rowid", "remote_site_rowid"], unique = true)
    ])
    data class Image(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Int = -1,
        @ColumnInfo(name = "local_site_rowid") val localSiteRowId: Int,
        @ColumnInfo(name = "remote_site_rowid") val remoteSiteRowId: Int,
        @ColumnInfo(name = "remote_instance_id") val remoteInstanceId: Int,

        val discovered: Instant = Clock.System.now(),
        val updated: Instant? = null,
    )

    companion object {
        fun from(other: thefederation_node): Site {
            return Site(name = other.name)
                .copy(other)
        }
    }

    fun copy(other: thefederation_node): Site {
        val stats = other.thefederation_stats.single()
        return copy(
            software = other.thefederation_platform.name,
            version = other.version,
            countryCode = other.country,
        ).copy(stats)
    }

    fun copy(other: thefederation_stat): Site {
        return copy(
            localPosts = other.local_posts,
            localComments = other.local_comments,
            usersTotal = other.users_total,
            usersHalfYear = other.users_half_year,
            usersMonthly = other.users_monthly,
            usersWeekly = other.users_weekly,
        )
    }
}

