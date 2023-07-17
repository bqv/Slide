package ltd.ucode.slide.repository

import android.content.Context
import androidx.paging.PagingData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import ltd.ucode.network.SingleVote
import ltd.ucode.network.lemmy.data.id.CommentId
import ltd.ucode.network.lemmy.data.id.PostId
import ltd.ucode.slide.data.ContentDatabase
import ltd.ucode.slide.data.entity.Post
import ltd.ucode.slide.data.value.Feed
import ltd.ucode.slide.data.value.Period
import ltd.ucode.slide.data.value.Sorting
import javax.inject.Inject

class PostRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDatabase: ContentDatabase,
    private val networkRepository: NetworkRepository,
) {
    fun getPosts(instance: String?,
                 feed: Feed,
                 pageSize: Int,
                 sort: Sorting? = null,
                 period: Period? = null,
    ): Flow<List<Post>> {
        return networkRepository.dataSource
            .getPosts(instance ?: networkRepository.defaultInstance,
                feed = feed,
                pageSize = pageSize,
                period = period ?: Period.All,
                order = sort ?: Sorting.New(false),
            )
    }

    fun getPosts(instance: String?,
                 feed: Feed,
                 sort: Sorting? = null,
                 period: Period? = null,
    ): Flow<PagingData<Post>> {
        return networkRepository.dataSource
            .getPosts(instance ?: networkRepository.defaultInstance,
                feed = feed,
                period = period ?: Period.All,
                order = sort ?: Sorting.New(false),
            )
    }

    fun getPost(instance: String?,
                id: PostId,
                commentId: CommentId? = null,
    ): Flow<Post> {
        //return networkRepository.dataSource.getPost(instance, id, commentId)
        return networkRepository.dataSource
            .getPost(instance ?: networkRepository.defaultInstance, id.id)
    }

    fun likePost(instance: String?,
                 id: PostId,
                 score: SingleVote,
    ) {
        /*
        return networkRepository.dataSource
            .likePost(instance ?: networkRepository.defaultInstance, id,
                score = score
            )
         */
    }
}
