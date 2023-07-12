package ltd.ucode.network.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.type.PostListingType
import ltd.ucode.network.lemmy.data.type.PostSortType

@Serializable
data class ListCommunitiesRequest (
    override var auth: String? = null,
    val limit: Int? = null,
    val page: Int? = null,
    val sort: PostSortType? = null,
    @SerialName("type_") val type: PostListingType? = null,
) : Authenticated
