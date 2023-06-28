package ltd.ucode.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.id.InstanceId
import ltd.ucode.lemmy.data.id.SiteId

@Serializable
data class Site (
    @SerialName("actor_id") val actorId: String,
    val banner: String? = null,
    val description: String? = null,
    val icon: String? = null,
    val id: SiteId,
    @SerialName("inbox_url") val inboxUrl: String,
    @SerialName("instance_id") val instanceId: InstanceId,
    @SerialName("last_refreshed_at") val lastRefreshedAt: LocalDateTime,
    val name: String,
    @SerialName("private_key") val privateKey: String? = null,
    @SerialName("public_key") val publicKey: String,
    val published: LocalDateTime,
    val sidebar: String? = null,
    val updated: LocalDateTime? = null,
)
