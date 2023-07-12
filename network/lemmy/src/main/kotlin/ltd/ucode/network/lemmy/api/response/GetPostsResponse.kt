package ltd.ucode.network.lemmy.api.response

import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.type.PostView

@Serializable
data class GetPostsResponse(
    val posts: List<PostView>,
) {
}
