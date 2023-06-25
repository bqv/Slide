package ltd.ucode.lemmy.data.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
) {
    @delegate:Transient
    val pathIds: List<CommentId> by lazy {
        path.split(".").map { CommentId(it.toInt()) }
    }

    val parent: CommentId
        get() = pathIds.dropLast(1).last()

    val depth: Int
        get() = pathIds.size - 1

    val isTopLevel: Boolean
        get() = pathIds.size == 2
}
