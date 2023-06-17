package ltd.ucode.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.type.ListingType
import ltd.ucode.lemmy.data.type.SortType

@Serializable
data class GetPostsRequest (
    val auth: String? = null,
    @SerialName("community_id") val communityId: Int? = null,
    @SerialName("community_name") val communityName: String? = null, // community, or community@instance.tld
    val limit: Int? = null,
    val page: Int? = null,
    @SerialName("saved_only") val savedOnly: Boolean? = null,
    val sort: SortType? = null,
    @SerialName("type_") val type: ListingType? = null,
)
