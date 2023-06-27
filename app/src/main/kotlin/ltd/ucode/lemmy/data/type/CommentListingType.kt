package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
enum class CommentListingType {
    All,
    Community,
    Subscribed,
}
