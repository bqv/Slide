package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.ucode.lemmy.api.request.GetCommentsRequest
import ltd.ucode.lemmy.data.id.CommentId
import ltd.ucode.lemmy.data.id.CommunityId
import ltd.ucode.lemmy.data.id.PostId
import ltd.ucode.lemmy.data.type.CommentListingType
import ltd.ucode.lemmy.data.type.CommentSortType
import ltd.ucode.lemmy.data.type.CommentView
import ltd.ucode.slide.PagedData
import javax.inject.Inject

class CommentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val instanceRepository: InstanceRepository,
) {
    fun getComments(instance: String?,
                    communityId: CommunityId? = null,
                    communityName: String? = null, // community, or community@instance.tld
                    parentId: CommentId? = null,
                    postId: PostId? = null,
                    maxDepth: Int? = null,
                    limit: Int? = null,
                    fromPage: Int? = null,
                    savedOnly: Boolean? = null,
                    sort: CommentSortType? = null,
                    type: CommentListingType? = null
    ): PagedData<CommentView> {
        return PagedData(fromPage ?: 1) { page: Int -> {
            val response = instanceRepository[instance].getComments(
                GetCommentsRequest(
                    communityId = communityId,
                    communityName = communityName,
                    parentId = parentId,
                    postId = postId,
                    maxDepth = maxDepth,
                    limit = limit,
                    page = page,
                    savedOnly = savedOnly,
                    sort = sort,
                    type = type
                )
            )

            response.comments
        } }
    }
}
