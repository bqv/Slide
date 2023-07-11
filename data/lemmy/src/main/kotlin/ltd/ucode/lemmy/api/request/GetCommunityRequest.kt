package ltd.ucode.lemmy.api.request

import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.id.CommunityId

@Serializable
data class GetCommunityRequest(
    override var auth: String? = null,
    val id: CommunityId? = null,
    val name: String? = null,
) : Authenticated
