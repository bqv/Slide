package ltd.ucode.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.type.CommunityId
import ltd.ucode.lemmy.data.type.PostListingType
import ltd.ucode.lemmy.data.type.PostSortType

@Serializable
data class GetPostsRequest (
    override var auth: String? = null,
    @SerialName("community_id") val communityId: CommunityId? = null,
    @SerialName("community_name") val communityName: String? = null, // community, or community@instance.tld
    val limit: Int? = null,
    val page: Int? = null,
    @SerialName("saved_only") val savedOnly: Boolean? = null,
    val sort: PostSortType? = null,
    @SerialName("type_") val type: PostListingType? = null,
) : Authenticated
