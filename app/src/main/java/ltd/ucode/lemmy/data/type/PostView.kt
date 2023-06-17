package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostView (
    val community: CommunitySafe,
    val counts: PostAggregates,
    val creator: PersonSafe,
    @SerialName("creator_banned_from_community") val isCreatorBanned: Boolean,
    @SerialName("creator_blocked") val isCreatorBlocked: Boolean,
    @SerialName("my_vote") val my_vote: Int? = null,
    val post: Post,
    @SerialName("read") val isRead: Boolean,
    @SerialName("saved") val isSaved: Boolean,
    val subscribed: SubscribedType,
    @SerialName("unread_comments") val unreadComments: Int,
)
