package ltd.ucode.slide.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ltd.ucode.lemmy.api.InstanceDataSource
import ltd.ucode.lemmy.api.request.GetPostsRequest
import ltd.ucode.slide.data.ContentDatabase
import ltd.ucode.slide.data.entity.Post
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalPagingApi::class)
class PostMediator(
    private val postId: Int,
    private val database: ContentDatabase,
    private val api: InstanceDataSource,
) : RemoteMediator<Int, Post>() {
    val postDao = database.posts

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Post>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> state.anchorPosition?.let(state::closestPageToPosition)?.nextKey ?: 1
        }

        val response = api.getPosts(GetPostsRequest(
                communityId = null,
                communityName = null,
                limit = state.config.pageSize,
                page = page,
                savedOnly = null,
                sort = null,
                type = null,
        ))

        return response
                .onSuccess {
                    database.withTransaction {
                        /*
                        val posts: List<Post> = it.posts.map { post ->
                            val instance = database.instances.get(api.instance).single()
                            val group = database.groups.get(post.community.id.id, instance.rowId).single()
                            val user = database.users.get(post.creator.id.id, instance.rowId).single()
                            val language = database.languages.get(post.post.languageId.id, instance.rowId).single()

                            Post.from(post,
                                    instance = instance,
                                    group = group,
                                    user = user,
                                    language = language,
                            )
                        }
                        postDao.addAll(posts)
                         */// TODO: try this
                    }
                }
                .mapSuccess<MediatorResult> {
                    MediatorResult.Success(endOfPaginationReached = false)
                }
                .coalesce { MediatorResult.Error(exception.upcast()) }
    }

    override suspend fun initialize(): InitializeAction {
        val cacheTimeout = 1.hours
        val lastRefresh: Instant = Instant.DISTANT_PAST
        return if (Clock.System.now() - lastRefresh <= cacheTimeout) {
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }
}

@OptIn(ExperimentalPagingApi::class)
fun example(
        postId: Int,
        database: ContentDatabase,
        api: InstanceDataSource,
) {
    val pager = Pager(config = PagingConfig(pageSize = 50),
            remoteMediator = PostMediator(postId, database, api)
    ) {
        database.posts.pagingSource(api.instance)
    }
}
