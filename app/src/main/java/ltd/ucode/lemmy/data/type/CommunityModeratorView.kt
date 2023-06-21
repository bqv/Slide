package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class CommunityModeratorView (
    val community: Community,
    val moderator: Person,
)
