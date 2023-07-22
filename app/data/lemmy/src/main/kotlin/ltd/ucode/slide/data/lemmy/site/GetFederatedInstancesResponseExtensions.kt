package ltd.ucode.slide.data.lemmy.site

import ltd.ucode.network.lemmy.api.response.GetFederatedInstancesResponse
import ltd.ucode.network.lemmy.data.type.Instance
import ltd.ucode.slide.data.common.partial.SiteImageMetadataPartial

object GetFederatedInstancesResponseExtensions {
    fun GetFederatedInstancesResponse.toSites(software: String): List<SiteImageMetadataPartial> {
        return federatedInstances?.run {
            linked.toSortedSet { a, b -> a.domain.compareTo(b.domain) }.also {
                it.addAll(allowed)
                it.addAll(blocked)
            }
        }?.map {
            it.toSiteImageMetadataPartial()
        }.orEmpty()
    }

    private fun Instance.toSiteImageMetadataPartial(): SiteImageMetadataPartial {
        return SiteImageMetadataPartial(
            name = domain,
            siteId = id.id,
            software = software,
            version = version,
        )
    }
}
