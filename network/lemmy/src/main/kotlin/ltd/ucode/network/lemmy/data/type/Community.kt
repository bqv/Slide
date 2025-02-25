package ltd.ucode.network.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.id.CommunityId
import ltd.ucode.network.lemmy.data.id.InstanceId
import java.net.URL

@Serializable
data class Community (
    @SerialName("actor_id") val actorId: String,
    val banner: String? = null,
    @SerialName("deleted") val isDeleted: Boolean,
    val description: String? = null,
    @SerialName("hidden") val isHidden: Boolean,
    val icon: String? = null,
    val id: CommunityId,
    @SerialName("instance_id") val instanceId: InstanceId,
    @SerialName("local") val isLocal: Boolean,
    val name: String,
    @SerialName("nsfw") val isNsfw: Boolean,
    @SerialName("posting_restricted_to_mods") val isPostingRestricted: Boolean,
    val published: LocalDateTime,
    @SerialName("removed") val isRemoved: Boolean,
    val title: String,
    val updated: LocalDateTime? = null,
) {
    val instanceName: String
        @Deprecated("there must be a better way")
        get() = URL(actorId).host
}
