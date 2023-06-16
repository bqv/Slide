package ltd.ucode.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunityFollowerView (
    val community: CommunitySafe,
    val follower: PersonSafe,
)
