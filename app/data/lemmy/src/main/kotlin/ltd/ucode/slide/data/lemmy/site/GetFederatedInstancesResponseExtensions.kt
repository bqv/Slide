package ltd.ucode.slide.data.lemmy.site

import ltd.ucode.network.lemmy.api.response.GetFederatedInstancesResponse
import ltd.ucode.slide.data.entity.Site

object GetFederatedInstancesResponseExtensions {
    fun GetFederatedInstancesResponse.toSites(software: String): List<Site> {
        return federatedInstances?.run {
            linked.toSortedSet().also {
                it.addAll(allowed.orEmpty())
                it.addAll(blocked.orEmpty())
            }
        }?.map {
            Site(
                name = it,
                software = software,
            )
        }.orEmpty()
    }

}
