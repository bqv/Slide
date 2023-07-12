package ltd.ucode.network.lemmy.api.request

import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.id.CommunityId

@Serializable
data class GetCommunityRequest(
    override var auth: String? = null,
    val id: CommunityId? = null,
    val name: String? = null,
) : Authenticated
