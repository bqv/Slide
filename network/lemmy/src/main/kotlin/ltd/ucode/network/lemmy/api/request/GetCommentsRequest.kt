package ltd.ucode.network.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.id.CommentId
import ltd.ucode.network.lemmy.data.type.CommentListingType
import ltd.ucode.network.lemmy.data.type.CommentSortType
import ltd.ucode.network.lemmy.data.id.CommunityId
import ltd.ucode.network.lemmy.data.id.PostId

@Serializable
data class GetCommentsRequest (
    override var auth: String? = null,
    @SerialName("type_") val type: CommentListingType? = null,
    val sort: CommentSortType? = null,
    @SerialName("max_depth") val maxDepth: Int? = null,
    val page: Int? = null,
    val limit: Int? = null,
    @SerialName("community_id") val communityId: CommunityId? = null,
    @SerialName("community_name") val communityName: String? = null,
    @SerialName("post_id") val postId: PostId? = null,
    @SerialName("parent_id") val parentId: CommentId? = null,
    @SerialName("saved_only") val savedOnly: Boolean? = null,
) : Authenticated
