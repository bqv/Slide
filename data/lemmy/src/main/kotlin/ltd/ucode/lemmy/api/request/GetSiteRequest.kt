package ltd.ucode.lemmy.api.request

import kotlinx.serialization.Serializable

@Serializable
data class GetSiteRequest (
    override var auth: String? = null
) : Authenticated
