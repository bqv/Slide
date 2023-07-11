package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.ucode.lemmy.api.ApiResult
import ltd.ucode.lemmy.api.PagedData
import ltd.ucode.lemmy.api.request.CreateCommentLikeRequest
import ltd.ucode.lemmy.api.request.GetCommentsRequest
import ltd.ucode.lemmy.api.response.CommentResponse
import ltd.ucode.lemmy.data.id.CommentId
import ltd.ucode.lemmy.data.id.CommunityId
import ltd.ucode.lemmy.data.id.PostId
import ltd.ucode.lemmy.data.type.CommentListingType
import ltd.ucode.lemmy.data.type.CommentSortType
import ltd.ucode.lemmy.data.type.CommentView
import ltd.ucode.slide.SingleVote
import ltd.ucode.slide.data.ContentDatabase
import javax.inject.Inject

class CommentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDatabase: ContentDatabase,
    private val networkRepository: NetworkRepository,
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
            networkRepository[instance].getComments(
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
            ).mapSuccess {
                data.comments
            }
        } }
    }

    suspend fun likeComment(instance: String?,
                            id: CommentId,
                            score: SingleVote,
    ): ApiResult<CommentResponse> {
        return networkRepository[instance].likeComment(
            CreateCommentLikeRequest(
                commentId = id,
                score = score
            )
        )
    }
}
