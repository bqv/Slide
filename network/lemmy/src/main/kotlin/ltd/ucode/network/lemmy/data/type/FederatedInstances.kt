package ltd.ucode.network.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class FederatedInstances (
    val linked: List<Instance>,
    val allowed: List<Instance> = emptyList(),
    val blocked: List<Instance> = emptyList(),
)
