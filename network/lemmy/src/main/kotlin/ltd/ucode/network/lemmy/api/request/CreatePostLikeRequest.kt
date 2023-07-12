package ltd.ucode.network.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.id.PostId
import ltd.ucode.network.SingleVote

@Serializable
data class CreatePostLikeRequest (
        override var auth: String? = null,
        @SerialName("post_id") val postId: PostId,
        val score: SingleVote,
) : Authenticated
