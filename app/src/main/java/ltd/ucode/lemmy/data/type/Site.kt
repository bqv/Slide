package ltd.ucode.lemmy.data.type

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Site (
    @SerialName("actor_id") val actorId: String,
    val banner: String?,
    val description: String?,
    val icon: String?,
    val id: Int,
    @SerialName("inbox_url") val inboxUrl: String,
    @SerialName("instance_id") val instanceId: Int,
    @SerialName("last_refreshed_at") val lastRefreshedAt: Instant,
    val name: String,
    @SerialName("private_key") val privateKey: String?,
    @SerialName("public_key") val publicKey: String,
    val published: Instant,
    val sidebar: String?,
    val updated: Instant?,
)
