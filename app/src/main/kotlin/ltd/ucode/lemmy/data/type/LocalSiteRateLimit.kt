package ltd.ucode.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocalSiteRateLimit (
    val comment: Int,
    @SerialName("comment_per_second") val commentPerSecond: Int,
    val id: Int,
    val image: Int,
    @SerialName("image_per_second") val imagePerSecond: Int,
    @SerialName("local_site_id") val localSiteId: LocalSiteId,
    val message: Int,
    @SerialName("message_per_second") val messagePerSecond: Int,
    val post: Int,
    @SerialName("post_per_second") val postPerSecond: Int,
    val published: LocalDateTime,
    val register: Int,
    @SerialName("register_per_second") val registerPerSecond: Int,
    val search: Int,
    @SerialName("search_per_second") val searchPerSecond: Int,
    val updated: LocalDateTime? = null,
)
