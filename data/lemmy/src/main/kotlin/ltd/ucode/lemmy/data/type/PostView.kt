package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostView (
    val community: Community,
    val counts: PostAggregates,
    val creator: Person,
    @SerialName("creator_banned_from_community") val isCreatorBanned: Boolean,
    @SerialName("creator_blocked") val isCreatorBlocked: Boolean,
    @SerialName("my_vote") val myVote: Int? = null,
    val post: Post,
    @SerialName("read") val isRead: Boolean,
    @SerialName("saved") val isSaved: Boolean,
    val subscribed: SubscribedType,
    @SerialName("unread_comments") val unreadComments: Int,
) {
    val instanceName: String
        @Deprecated("there must be a better way")
        get() = community.instanceName
}
