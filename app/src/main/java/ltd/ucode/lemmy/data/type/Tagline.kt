package ltd.ucode.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tagline (
    val content: String,
    val id: Int,
    @SerialName("local_site_id") val localSiteId: Int,
    val published: LocalDateTime,
    val updated: LocalDateTime?,
)
