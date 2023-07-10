package ltd.ucode.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.id.CommentId

@Serializable
data class CommentAggregates (
    val id: Int,
    @SerialName("comment_id") val commentId: CommentId,
    val score: Int,
    val upvotes: Int,
    val downvotes: Int,
    val published: LocalDateTime,
    @SerialName("child_count") val childCount: Int,
    @SerialName("hot_rank") val hotRank: Int = Int.MAX_VALUE, // ADDED in 0.17.4
) {
    init {
        assert(score == upvotes - downvotes) {
            "Score $score != Upvotes $upvotes - Downvotes $downvotes"
        }
    }
}
