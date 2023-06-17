package ltd.ucode.lemmy.data.type.webfinger

import kotlinx.serialization.Serializable

@Serializable
data class Resource (
    val links: List<Link>,
)
