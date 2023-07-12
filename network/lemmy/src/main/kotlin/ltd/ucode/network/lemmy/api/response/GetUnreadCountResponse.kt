package ltd.ucode.network.lemmy.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetUnreadCountResponse (
    val mentions: Int,
    @SerialName("private_messages") val privateMessages: Int,
    val replies: Int,
) {
}
