package ltd.ucode.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.id.CommentId
import ltd.ucode.lemmy.data.value.SingleVote

@Serializable
data class CreateCommentLikeRequest (
    override var auth: String? = null,
    @SerialName("comment_id") val commentId: CommentId,
    val score: SingleVote,
) : Authenticated
