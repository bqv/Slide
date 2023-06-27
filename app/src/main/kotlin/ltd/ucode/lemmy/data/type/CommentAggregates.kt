package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentAggregates (
    val id: Int,
    @SerialName("comment_id") val commentId: CommentId,
    val score: Int,
    val upvotes: Int,
    val downvotes: Int,
    val published: String,
    @SerialName("child_count") val childCount: Int,
    @SerialName("hot_rank") val hotRank: Int = Int.MAX_VALUE, // ADDED in 0.17.4
)
