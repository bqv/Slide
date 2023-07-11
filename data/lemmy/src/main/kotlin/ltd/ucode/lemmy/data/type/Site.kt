@file:UseSerializers(
    URLSerializer::class,
    PublicKeySerializer::class,
)
package ltd.ucode.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import ltd.ucode.lemmy.data.id.InstanceId
import ltd.ucode.lemmy.data.id.SiteId
import ltd.ucode.slide.PublicKeySerializer
import ltd.ucode.slide.URLSerializer
import java.net.URL
import java.security.PrivateKey
import java.security.PublicKey

@Serializable
data class Site (
    @SerialName("actor_id") val actorId: URL,
    val banner: URL? = null,
    val description: String? = null,
    val icon: URL? = null,
    val id: SiteId,
    @SerialName("inbox_url") val inboxUrl: URL,
    @SerialName("instance_id") val instanceId: InstanceId,
    @SerialName("last_refreshed_at") val lastRefreshedAt: LocalDateTime,
    val name: String,
    @SerialName("private_key") val privateKey: PrivateKey? = null,
    @SerialName("public_key") val publicKey: PublicKey,
    val published: LocalDateTime,
    val sidebar: String? = null,
    val updated: LocalDateTime? = null,
) {
    val domain: String
        get() = actorId.host
}
