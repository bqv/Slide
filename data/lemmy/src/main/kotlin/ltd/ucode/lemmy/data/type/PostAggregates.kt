package ltd.ucode.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.id.PostId

@Serializable
data class PostAggregates (
    val comments: Int,
    val downvotes: Int,
    @SerialName("featured_community") val isFeaturedCommunity: Boolean,
    @SerialName("featured_local") val isFeaturedLocal: Boolean,
    val id: Int,
    val published: LocalDateTime,
    @SerialName("newest_comment_time") val newestCommentTime: LocalDateTime,
    @SerialName("newest_comment_time_necro") val newestCommentTimeNecro: LocalDateTime,
    @SerialName("post_id") val postId: PostId,
    val score: Int,
    val upvotes: Int,
    @SerialName("hot_rank") val hotRank: Int = Int.MAX_VALUE, // ADDED in 0.17.4
    @SerialName("hot_rank_active") val activeRank: Int = Int.MAX_VALUE, // ADDED in 0.17.4
)
