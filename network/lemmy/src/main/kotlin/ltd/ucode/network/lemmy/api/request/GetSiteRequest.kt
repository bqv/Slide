package ltd.ucode.network.lemmy.api.request

import kotlinx.serialization.Serializable

@Serializable
data class GetSiteRequest (
    override var auth: String? = null
) : Authenticated
