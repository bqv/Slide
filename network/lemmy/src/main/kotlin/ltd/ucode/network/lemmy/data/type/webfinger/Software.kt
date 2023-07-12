package ltd.ucode.network.lemmy.data.type.webfinger

import kotlinx.serialization.Serializable

@Serializable
data class Software (
    val name: String,
    val version: String,
)
