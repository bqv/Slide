package ltd.ucode.lemmy.data.type

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunityModeratorView (
    val community: CommunitySafe,
    val moderator: PersonSafe,
)
