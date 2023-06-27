package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class CommunityFollowerView (
    val community: Community,
    val follower: Person,
)
