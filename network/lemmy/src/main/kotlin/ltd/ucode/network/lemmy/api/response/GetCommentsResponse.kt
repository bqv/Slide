package ltd.ucode.network.lemmy.api.response

import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.type.CommentView

@Serializable
data class GetCommentsResponse (
    val comments: List<CommentView>,
) : IResponse() {
}
