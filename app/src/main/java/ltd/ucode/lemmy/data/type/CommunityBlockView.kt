package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class CommunityBlockView (
    val community: CommunitySafe,
    val person: PersonSafe,
)
