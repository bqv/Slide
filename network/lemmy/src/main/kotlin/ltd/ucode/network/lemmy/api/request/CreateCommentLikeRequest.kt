package ltd.ucode.network.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.id.CommentId
import ltd.ucode.network.SingleVote

@Serializable
data class CreateCommentLikeRequest (
        override var auth: String? = null,
        @SerialName("comment_id") val commentId: CommentId,
        val score: SingleVote,
) : Authenticated
