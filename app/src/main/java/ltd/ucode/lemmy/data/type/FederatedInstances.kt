package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class FederatedInstances (
    val linked: List<String>,
    val allowed: List<String>,
    val blocked: List<String>,
)
