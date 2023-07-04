package ltd.ucode.slide.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import info.the_federation.graphql.generated.getlemmyserversquery.thefederation_node
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ltd.ucode.slide.data.ISite

@Entity(tableName = "instances", indices = [
    Index(value = ["name"], unique = true)
])
data class Instance(
    @PrimaryKey @ColumnInfo(name = "rowid") val id: Int = -1,
    override val name: String,
    override val software: String?,
    override val version: String?,
    @ColumnInfo(name = "country_code") override val countryCode: String?,
    @ColumnInfo(name = "local_posts") override val localPosts: Int? = 0,
    @ColumnInfo(name = "local_comments") override val localComments: Int? = 0,
    @ColumnInfo(name = "users_total") override val usersTotal: Int? = 0,
    @ColumnInfo(name = "users_semiannual") override val usersHalfYear: Int? = 0,
    @ColumnInfo(name = "users_monthly") override val usersMonthly: Int? = 0,
    @ColumnInfo(name = "users_weekly") override val usersWeekly: Int? = 0,
    val discovered: Instant = Clock.System.now(),
    val updated: Instant? = null,
    @Ignore var inaccessibleSince: Instant? = null
) : ISite {
    companion object {
        fun from(other: thefederation_node): Instance {
            val stats = other.thefederation_stats.single()
            return Instance(
                name = other.name,
                software = other.thefederation_platform.name,
                version = other.version,
                countryCode = other.country,
                localPosts = stats.local_posts,
                localComments = stats.local_comments,
                usersTotal = stats.users_total,
                usersHalfYear = stats.users_half_year,
                usersMonthly = stats.users_monthly,
                usersWeekly = stats.users_weekly,
            )
        }
    }
}

