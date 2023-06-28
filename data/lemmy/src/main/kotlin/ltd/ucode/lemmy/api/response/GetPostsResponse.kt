package ltd.ucode.lemmy.api.response

import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.type.PostView

@Serializable
data class GetPostsResponse(
    val posts: List<PostView>,
) {
}
