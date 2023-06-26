package ltd.ucode.lemmy.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.type.FederatedInstances

@Serializable
data class GetFederatedInstancesResponse (
    @SerialName("federated_instances") val federatedInstances: FederatedInstances? = null,
)
