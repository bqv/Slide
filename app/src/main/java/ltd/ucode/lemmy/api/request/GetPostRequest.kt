package ltd.ucode.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.type.CommentId
import ltd.ucode.lemmy.data.type.PostId

@Serializable
data class GetPostRequest (
    override var auth: String? = null,
    val id: PostId? = null,
    @SerialName("comment_id") val commentId: CommentId? = null,
) : Authenticated
