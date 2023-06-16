package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SiteView (
    val counts: SiteAggregates,
    @SerialName("local_site") val localSite: LocalSite,
    @SerialName("local_site_rate_limit") val localSiteRateLimit: LocalSiteRateLimit,
    val site: Site,
    val taglines: List<Tagline>?,
)
