package ltd.ucode.slide.data.partial

import androidx.room.ColumnInfo
import ltd.ucode.slide.data.entity.Site

data class SiteMetadataPartial(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "software") val software: String?,
    @ColumnInfo(name = "version") val version: String?,
    @ColumnInfo(name = "country_code") val countryCode: String?,
    @ColumnInfo(name = "local_posts") val localPosts: Int?,
    @ColumnInfo(name = "local_comments") val localComments: Int?,
    @ColumnInfo(name = "users_total") val usersTotal: Int?,
    @ColumnInfo(name = "users_semiannually") val usersHalfYear: Int?,
    @ColumnInfo(name = "users_monthly") val usersMonthly: Int?,
    @ColumnInfo(name = "users_weekly") val usersWeekly: Int?,
) {
    companion object {
        fun of(other: Site): SiteMetadataPartial {
            return SiteMetadataPartial(
                name = other.name,
                software = other.software,
                version = other.version,
                countryCode = other.countryCode,
                localPosts = other.localPosts,
                localComments = other.localComments,
                usersTotal = other.usersTotal,
                usersHalfYear = other.usersHalfYear,
                usersMonthly = other.usersMonthly,
                usersWeekly = other.usersWeekly
            )
        }
    }

    fun Site.metadataPartial(other: SiteMetadataPartial): Site {
        return Site(
            name = other.name,
            software = other.software,
            version = other.version,
            countryCode = other.countryCode,
            localPosts = other.localPosts,
            localComments = other.localComments,
            usersTotal = other.usersTotal,
            usersHalfYear = other.usersHalfYear,
            usersMonthly = other.usersMonthly,
            usersWeekly = other.usersWeekly
        )
    }
}
