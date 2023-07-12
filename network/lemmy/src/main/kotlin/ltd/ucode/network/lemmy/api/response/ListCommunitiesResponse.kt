package ltd.ucode.network.lemmy.api.response

import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.type.CommunityView

@Serializable
data class ListCommunitiesResponse(
    val communities: List<CommunityView>,
) {
}
