package ltd.ucode.lemmy.api.response

import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.type.CommentView

@Serializable
data class GetCommentsResponse (
    val comments: List<CommentView>,
) {
}
