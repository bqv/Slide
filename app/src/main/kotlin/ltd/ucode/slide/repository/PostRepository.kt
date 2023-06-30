package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.ucode.lemmy.api.ApiResult
import ltd.ucode.lemmy.api.PagedData
import ltd.ucode.lemmy.api.request.CreatePostLikeRequest
import ltd.ucode.lemmy.api.request.GetPostRequest
import ltd.ucode.lemmy.api.request.GetPostsRequest
import ltd.ucode.lemmy.api.response.GetPostResponse
import ltd.ucode.lemmy.api.response.PostResponse
import ltd.ucode.lemmy.data.id.CommentId
import ltd.ucode.lemmy.data.id.CommunityId
import ltd.ucode.lemmy.data.id.PostId
import ltd.ucode.lemmy.data.type.PostListingType
import ltd.ucode.lemmy.data.type.PostSortType
import ltd.ucode.lemmy.data.type.PostView
import ltd.ucode.lemmy.data.value.SingleVote
import javax.inject.Inject

class PostRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val instanceRepository: InstanceRepository,
) {
    fun getPosts(instance: String?,
                 communityId: CommunityId? = null,
                 communityName: String? = null, // community, or community@instance.tld
                 limit: Int? = null,
                 fromPage: Int? = null,
                 savedOnly: Boolean? = null,
                 sort: PostSortType? = null,
                 type: PostListingType? = null
    ): PagedData<PostView> {
        return PagedData(fromPage ?: 1) { page: Int -> {
            instanceRepository[instance].getPosts(
                GetPostsRequest(
                    communityId = communityId,
                    communityName = communityName,
                    limit = limit,
                    page = page,
                    savedOnly = savedOnly,
                    sort = sort,
                    type = type
                )
            ).mapSuccess {
                data.posts
            }
        } }
    }

    suspend fun getPost(instance: String?,
                        id: PostId? = null,
                        commentId: CommentId? = null
    ): ApiResult<GetPostResponse> {
        return instanceRepository[instance].getPost(
            GetPostRequest(
                id = id,
                commentId = commentId
            )
        )
    }

    suspend fun likePost(instance: String?,
                         id: PostId,
                         score: SingleVote,
    ): ApiResult<PostResponse> {
        return instanceRepository[instance].likePost(
            CreatePostLikeRequest(
                postId = id,
                score = score
            )
        )
    }
}
