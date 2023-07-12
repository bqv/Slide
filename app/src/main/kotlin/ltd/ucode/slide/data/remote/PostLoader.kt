package ltd.ucode.slide.data.remote

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import ltd.ucode.network.lemmy.api.ApiResult
import ltd.ucode.network.lemmy.api.InstanceDataSource
import ltd.ucode.network.lemmy.api.request.GetPostsRequest
import ltd.ucode.network.lemmy.data.type.PostListingType
import ltd.ucode.network.lemmy.data.type.PostSortType
import ltd.ucode.slide.data.ContentDatabase
import ltd.ucode.slide.data.entity.Post
import ltd.ucode.slide.repository.GroupRepository
import ltd.ucode.slide.repository.SiteRepository
import ltd.ucode.slide.repository.UserRepository

class PostLoader(
        private val database: ContentDatabase,
        private val siteRepository: SiteRepository,
        private val groupRepository: GroupRepository,
        private val userRepository: UserRepository,
        private val api: InstanceDataSource,
        private val block: PostRequestBuilder.() -> Unit,
) : PagingSource<Int, Post>() {
    private val postDao = database.posts

    data class PostRequestBuilder (
        var sort: PostSortType? = null,
        var type: PostListingType? = null,
    )

    fun GetPostsRequest.copy(builder: PostRequestBuilder): GetPostsRequest
        = copy(sort = builder.sort ?: sort, type = builder.type ?: type)

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, Post> {
        val pageNumber = params.key ?: 1

        val response = api.getPosts(
            GetPostsRequest().copy(PostRequestBuilder().apply(block)).copy(
                limit = params.loadSize,
                page = pageNumber,
            )
        )

        return response
            .bindSuccess<LoadResult<Int, Post>> {
                val posts: List<Post> = data.posts.map { post ->
                    val site = siteRepository.getSiteByName(api.instance).single()
                    val group = groupRepository.getGroupBySiteId(post.community.id, site).single()
                    val user = userRepository.getUserBySiteId(post.creator.id, site).single()
                    val language = siteRepository.getLanguageBySiteId(post.post.languageId, site).single()

                    Post.from(post,
                            site = site,
                            group = group,
                            user = user,
                            language = language,
                    )
                }

                database.withTransaction {
                    postDao.addAll(posts)
                }

                LoadResult.Page(
                    data = posts,
                    prevKey = null,
                    nextKey = pageNumber + 1,
                ).let { ApiResult.Success(api.instance, it) }
            }
            .coalesce { LoadResult.Error(exception.upcast()) }
    }

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage: LoadResult.Page<Int, Post>? =
                    state.closestPageToPosition(anchorPosition)

            anchorPage?.run {
                prevKey?.plus(1) ?: nextKey?.minus(1)
            }
        }
    }
}

fun example(
    database: ContentDatabase,
    siteRepository: SiteRepository,
    groupRepository: GroupRepository,
    userRepository: UserRepository,
    api: InstanceDataSource,
    block: PostLoader.PostRequestBuilder.() -> Unit,
): Flow<PagingData<Post>> {
    val pager = Pager(
        config = PagingConfig(pageSize = 50, prefetchDistance = 2),
    ) {
        PostLoader(database, siteRepository, groupRepository, userRepository, api, block)
    }

    val viewModelScope = runBlocking { this } //! delete
    return pager.flow.cachedIn(viewModelScope)
}
