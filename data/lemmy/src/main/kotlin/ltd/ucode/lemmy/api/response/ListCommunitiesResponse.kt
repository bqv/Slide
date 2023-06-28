package ltd.ucode.lemmy.api.response

import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.type.CommunityView

@Serializable
data class ListCommunitiesResponse(
    val communities: List<CommunityView>,
) {
}
