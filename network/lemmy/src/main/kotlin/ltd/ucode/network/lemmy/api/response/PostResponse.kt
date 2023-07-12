package ltd.ucode.network.lemmy.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.type.PostView

@Serializable
data class PostResponse(
    @SerialName("post_view") val postView: PostView,
)
