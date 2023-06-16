package ltd.ucode.lemmy.api.request

import kotlinx.serialization.Serializable

@Serializable
data class GetSiteRequest (
    val auth: String? = null
)
