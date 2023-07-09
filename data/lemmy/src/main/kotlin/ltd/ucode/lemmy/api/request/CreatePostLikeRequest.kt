package ltd.ucode.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.id.PostId
import ltd.ucode.slide.SingleVote

@Serializable
data class CreatePostLikeRequest (
        override var auth: String? = null,
        @SerialName("post_id") val postId: PostId,
        val score: SingleVote,
) : Authenticated
