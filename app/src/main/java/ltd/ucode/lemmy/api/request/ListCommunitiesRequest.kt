package ltd.ucode.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.type.ListingType
import ltd.ucode.lemmy.data.type.SortType

@Serializable
data class ListCommunitiesRequest (
    val auth: String? = null,
    val limit: Int? = null,
    val page: Int? = null,
    val sort: SortType? = null,
    @SerialName("type_") val type: ListingType? = null,
)
