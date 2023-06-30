package ltd.ucode.lemmy.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.id.LocalUserId
import ltd.ucode.lemmy.data.type.CommentView

@Serializable
data class CommentResponse(
    @SerialName("comment_view") val commentView: CommentView,
    @SerialName("recipient_ids") val recipientIds: List<LocalUserId>,
    @SerialName("form_id") val formId: String? = null,
)
