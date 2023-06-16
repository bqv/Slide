package ltd.ucode.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunitySafe (
    @SerialName("actor_id") val actorId: String,
    val banner: String? = null,
    @SerialName("deleted") val isDeleted: Boolean,
    val description: String? = null,
    @SerialName("hidden") val isHidden: Boolean,
    val icon: String? = null,
    val id: Int,
    @SerialName("instance_id") val instanceId: Int,
    @SerialName("local") val isLocal: Boolean,
    val name: String,
    @SerialName("nsfw") val isNsfw: Boolean,
    @SerialName("posting_restricted_to_mods") val isPostingRestricted: Boolean,
    val published: LocalDateTime,
    @SerialName("removed") val isRemoved: Boolean,
    val title: String,
    val updated: LocalDateTime? = null,
)
