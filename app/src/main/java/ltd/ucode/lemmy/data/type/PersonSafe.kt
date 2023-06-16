package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonSafe (
    @SerialName("actor_id") val actorId: String,
    @SerialName("admin") val isAdmin: Boolean,
    val avatar: String,
    @SerialName("ban_expires") val banExpires: String,
    @SerialName("banned") val isBanned: Boolean,
    val banner: String,
    val bio: String,
    @SerialName("bot_account") val botAccount: String,
    @SerialName("deleted") val isDeleted: Boolean,
    @SerialName("display_name") val displayName: String,
    val id: Int,
    @SerialName("inbox_url") val inboxUrl: String,
    @SerialName("instance_id") val instanceId: Int,
    @SerialName("local") val isLocal: Boolean,
    @SerialName("matrix_user_id") val matrixUserId: String,
    val name: String,
    val published: String,
    @SerialName("shared_inbox_url") val sharedInboxUrl: String,
    val updated: String,
)
