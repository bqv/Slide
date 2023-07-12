package ltd.ucode.network.lemmy.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.type.ImageFile

@Serializable
data class UploadImageResponse(
    @SerialName("delete_url") val deleteUrl: String? = null,
    val files: List<ImageFile>? = null,
    val msg: String,
    val url: String? = null,
) {
}
