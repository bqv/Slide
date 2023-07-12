package ltd.ucode.network.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageFile(
    @SerialName("delete_token") val deleteToken: String,
    val file: String,
)
