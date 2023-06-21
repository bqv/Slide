package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime

@Serializable
data class Comment (
    val id: CommentId,
    @SerialName("creator_id") val creatorId: PersonId,
    @SerialName("post_id") val postId: PostId,
    val content: String,
    @SerialName("removed") val isRemoved: Boolean,
    val published: LocalDateTime,
    val updated: LocalDateTime? = null,
    @SerialName("deleted") val isDeleted: Boolean,
    @SerialName("ap_id") val apId: String,
    @SerialName("local") val isLocal: Boolean,
    val path: String,
    @SerialName("distinguished") val isDistinguished: Boolean,
    @SerialName("language_id") val languageId: LanguageId,
)
