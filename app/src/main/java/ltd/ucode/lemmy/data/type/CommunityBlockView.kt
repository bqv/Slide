package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class CommunityBlockView (
    val community: Community,
    val person: Person,
)
