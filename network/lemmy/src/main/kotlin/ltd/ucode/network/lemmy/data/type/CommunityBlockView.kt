package ltd.ucode.network.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class CommunityBlockView (
    val community: Community,
    val person: Person,
)
