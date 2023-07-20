package ltd.ucode.network.lemmy.data.type.webfinger

import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.api.response.IResponse

@Serializable
data class Resource (
    val links: List<Link>,
) : IResponse()
