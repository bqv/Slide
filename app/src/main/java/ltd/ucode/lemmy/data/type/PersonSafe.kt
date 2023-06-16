package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonSafe (
    @SerialName("actor_id") val actorId: String,
    @SerialName("admin") val isAdmin: Boolean,
    val avatar: String? = null,
    @SerialName("ban_expires") val banExpires: String? = null,
    @SerialName("banned") val isBanned: Boolean,
    val banner: String? = null,
    val bio: String? = null,
    @SerialName("bot_account") val isBotAccount: Boolean,
    @SerialName("deleted") val isDeleted: Boolean,
    @SerialName("display_name") val displayName: String? = null,
    val id: Int,
    @SerialName("inbox_url") val inboxUrl: String,
    @SerialName("instance_id") val instanceId: Int,
    @SerialName("local") val isLocal: Boolean,
    @SerialName("matrix_user_id") val matrixUserId: String? = null,
    val name: String,
    val published: String,
    @SerialName("shared_inbox_url") val sharedInboxUrl: String? = null,
    val updated: String? = null,
)
