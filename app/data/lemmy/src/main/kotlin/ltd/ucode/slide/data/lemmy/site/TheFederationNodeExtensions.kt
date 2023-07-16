package ltd.ucode.slide.data.lemmy.site

import info.the_federation.graphql.generated.getlemmyserversquery.thefederation_node
import ltd.ucode.slide.data.partial.SiteMetadataPartial

object TheFederationNodeExtensions {
    fun thefederation_node.toSiteMetadataPartial(): SiteMetadataPartial {
        val stats = thefederation_stats.single()
        return SiteMetadataPartial(
            name = this.name,
            software = this.thefederation_platform.name,
            version = this.version,
            countryCode = this.country,
            localPosts = stats.local_posts,
            localComments = stats.local_comments,
            usersTotal = stats.users_total,
            usersHalfYear = stats.users_half_year,
            usersMonthly = stats.users_monthly,
            usersWeekly = stats.users_weekly,
        )
    }
}
