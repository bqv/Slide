package ltd.ucode.lemmy.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.type.CommunityModeratorView
import ltd.ucode.lemmy.data.type.CommunityView
import ltd.ucode.lemmy.data.type.PostView

@Serializable
data class GetPostResponse(
    @SerialName("community_view") val communityView: CommunityView,
    val moderators: List<CommunityModeratorView>,
    val online: Int,
    @SerialName("post_view") val postView: PostView,
) {
    fun toResult(): GetPostResponse {
        return this
    }
}
