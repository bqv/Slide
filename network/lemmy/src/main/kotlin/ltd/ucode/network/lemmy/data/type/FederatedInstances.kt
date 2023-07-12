package ltd.ucode.network.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class FederatedInstances (
    val linked: List<String>,
    val allowed: List<String>? = null,
    val blocked: List<String>? = null,
)
