package ltd.ucode.slide.repository

import android.content.Context
import androidx.paging.PagingData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import ltd.ucode.network.SingleVote
import ltd.ucode.network.lemmy.api.response.CommentResponse
import ltd.ucode.network.lemmy.data.id.CommentId
import ltd.ucode.network.lemmy.data.id.CommunityId
import ltd.ucode.network.lemmy.data.id.PostId
import ltd.ucode.network.lemmy.data.type.CommentListingType
import ltd.ucode.network.lemmy.data.type.CommentSortType
import ltd.ucode.network.lemmy.data.type.CommentView
import ltd.ucode.slide.data.ContentDatabase
import javax.inject.Inject

class CommentRepository @Inject constructor(
        @ApplicationContext private val context: Context,
        private val contentDatabase: ContentDatabase,
        private val networkRepository: NetworkRepository,
) {
    fun getComments(instance: String?,
                    communityId: CommunityId? = null,
                    postId: PostId? = null,
                    sort: CommentSortType? = null,
                    type: CommentListingType? = null,
    ): Flow<PagingData<CommentView>> {
        /*
        return networkRepository.dataSource
            .getComments(instance ?: networkRepository.defaultInstance,
                communityId = communityId,
                communityName = communityName,
                postId = postId,
                sort = sort,
                type = type
            )
         */TODO()
    }

    fun likeComment(instance: String?,
                    id: CommentId,
                    score: SingleVote,
    ): Flow<CommentResponse> {
        /*
        return networkRepository.dataSource
            .likeComment(instance ?: networkRepository.defaultInstance,
                commentId = id,
                score = score
            )
         */TODO()
    }
}
