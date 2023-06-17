package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
enum class ListingType {
    All,
    Community,
    Local,
    Subscribed,
}
