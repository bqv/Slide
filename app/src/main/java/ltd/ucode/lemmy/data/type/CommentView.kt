package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentView (
    val comment: Comment,
    val creator: Person,
    val post: Post,
    val community: Community,
    val counts: CommentAggregates,
    @SerialName("creator_banned_from_community") val isCreatorBanned: Boolean,
    val subscribed: SubscribedType,
    @SerialName("saved") val isSaved: Boolean,
    @SerialName("creator_blocked") val isCreatorBlocked: Boolean,
    @SerialName("my_vote") val myVote: Int? = null,
) {
    val permalink: String
        get() = comment.apId
}
