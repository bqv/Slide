package ltd.ucode.lemmy.data.type.webfinger

import kotlinx.serialization.Serializable

typealias Uri = String
typealias MimeType = String

@Serializable
data class Link (
    val rel: Uri,
    val href: Uri,
    val type: MimeType? = null,
    val titles: Map<String, String>? = null,
)
