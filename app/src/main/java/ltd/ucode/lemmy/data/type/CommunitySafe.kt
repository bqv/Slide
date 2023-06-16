package ltd.ucode.lemmy.data.type

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunitySafe (
    @SerialName("actor_id") val actorId: String,
    val banner: String?,
    @SerialName("deleted") val isDeleted: Boolean,
    val description: String?,
    @SerialName("hidden") val isHidden: Boolean,
    val icon: String?,
    val id: Int,
    @SerialName("instance_id") val instanceId: Int,
    @SerialName("local") val isLocal: Boolean,
    val name: String,
    @SerialName("nsfw") val isNsfw: Boolean,
    @SerialName("posting_restricted_to_mods") val isPostingRestricted: Boolean,
    val published: Instant,
    @SerialName("removed") val isRemoved: Boolean,
    val title: String,
    val updated: Instant?,
)
