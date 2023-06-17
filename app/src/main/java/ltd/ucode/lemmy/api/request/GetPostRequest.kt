package ltd.ucode.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetPostRequest (
    val auth: String? = null,
    val id: Int? = null,
    @SerialName("comment_id") val commentId: Int? = null,
)
