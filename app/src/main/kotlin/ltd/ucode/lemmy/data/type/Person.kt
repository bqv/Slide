package ltd.ucode.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Person (
    @SerialName("actor_id") val actorId: String,
    @SerialName("admin") val isAdmin: Boolean,
    val avatar: String? = null,
    @SerialName("ban_expires") val banExpires: LocalDateTime? = null,
    @SerialName("banned") val isBanned: Boolean,
    val banner: String? = null,
    val bio: String? = null,
    @SerialName("bot_account") val isBotAccount: Boolean,
    @SerialName("deleted") val isDeleted: Boolean,
    @SerialName("display_name") val displayName: String? = null,
    val id: PersonId,
    @SerialName("inbox_url") val inboxUrl: String? = null,
    @SerialName("shared_inbox_url") val sharedInboxUrl: String? = null,
    @SerialName("instance_id") val instanceId: InstanceId,
    @SerialName("local") val isLocal: Boolean,
    @SerialName("matrix_user_id") val matrixUserId: String? = null,
    val name: String,
    val published: String,
    val updated: String? = null,
)
