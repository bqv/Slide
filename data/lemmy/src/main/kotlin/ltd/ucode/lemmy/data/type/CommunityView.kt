package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunityView (
    @SerialName("blocked") val isBlocked: Boolean,
    val community: Community,
    val counts: CommunityAggregates,
    val subscribed: SubscribedType,
)
