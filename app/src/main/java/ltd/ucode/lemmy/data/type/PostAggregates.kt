package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostAggregates (
    val comments: Int,
    val downvotes: Int,
    @SerialName("featured_community") val isFeaturedCommunity: Boolean,
    @SerialName("featured_local") val isFeaturedLocal: Boolean,
    val id: Int,
    @SerialName("newest_comment_time") val newestCommentTime: String,
    @SerialName("newest_comment_time_necro") val newestCommentTimeNecro: String,
    @SerialName("post_id") val postId: Int,
    val score: Int,
    val upvotes: Int,
)
