package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.ucode.network.lemmy.api.ApiResult
import ltd.ucode.network.lemmy.api.PagedData
import ltd.ucode.network.lemmy.api.request.CreatePostLikeRequest
import ltd.ucode.network.lemmy.api.request.GetPostRequest
import ltd.ucode.network.lemmy.api.request.GetPostsRequest
import ltd.ucode.network.lemmy.api.response.GetPostResponse
import ltd.ucode.network.lemmy.api.response.PostResponse
import ltd.ucode.network.lemmy.data.id.CommentId
import ltd.ucode.network.lemmy.data.id.CommunityId
import ltd.ucode.network.lemmy.data.id.PostId
import ltd.ucode.network.lemmy.data.type.PostListingType
import ltd.ucode.network.lemmy.data.type.PostSortType
import ltd.ucode.network.lemmy.data.type.PostView
import ltd.ucode.network.SingleVote
import ltd.ucode.slide.data.ContentDatabase
import javax.inject.Inject

class PostRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDatabase: ContentDatabase,
    private val networkRepository: NetworkRepository,
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
            networkRepository[instance].getPosts(
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
                        commentId: CommentId? = null,
    ): ApiResult<GetPostResponse> {
        return networkRepository[instance].getPost(
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
        return networkRepository[instance].likePost(
            CreatePostLikeRequest(
                postId = id,
                score = score
            )
        )
    }
}
