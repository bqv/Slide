package ltd.ucode.slide.data.lemmy.site

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ltd.ucode.network.lemmy.api.response.GetSiteResponse
import ltd.ucode.network.lemmy.data.type.LocalSite
import ltd.ucode.network.lemmy.data.type.SiteAggregates
import ltd.ucode.network.lemmy.data.type.SiteView
import ltd.ucode.slide.data.common.entity.Site
import ltd.ucode.network.lemmy.data.type.Site as LemmySite

object GetSiteResponseExtensions {
    fun GetSiteResponse.toSite(): Site {
        return Site(name = this.domain)
            .copy(this)
    }

    internal fun Site.copy(other: GetSiteResponse): Site {
        return copy(version = other.version)
            .copy(other.siteView)
    }

    internal fun Site.copy(other: SiteView): Site {
        return copy(
        )
            .copy(other.site)
            .copy(other.localSite)
            .copy(other.counts)
    }

    internal fun Site.copy(other: LemmySite): Site {
        return copy(
            ownSiteId = other.instanceId.id,
            created = other.published.toInstant(TimeZone.UTC)
                .coerceAtLeast(created),
            updated = other.updated?.toInstant(TimeZone.UTC)
                ?.let { (updated ?: Instant.DISTANT_PAST).coerceAtLeast(it) }
                ?: updated,
            refreshed = other.lastRefreshedAt.toInstant(TimeZone.UTC)
                .coerceAtLeast(refreshed),
        )
    }

    internal fun Site.copy(other: LocalSite): Site {
        return copy(
            created = other.published.toInstant(TimeZone.UTC)
                .coerceAtLeast(created),
            updated = other.updated?.toInstant(TimeZone.UTC)
                ?.let { (updated ?: Instant.DISTANT_PAST).coerceAtLeast(it) }
                ?: updated,
        )
    }

    internal fun Site.copy(other: SiteAggregates): Site {
        return copy(
            localPosts = other.posts,
            localComments = other.comments,
            usersTotal = other.users,
            usersDaily = other.usersActiveDay,
            usersWeekly = other.usersActiveWeek,
            usersMonthly = other.usersActiveMonth,
            usersHalfYear = other.usersActiveHalfYear,
        )
    }
}
