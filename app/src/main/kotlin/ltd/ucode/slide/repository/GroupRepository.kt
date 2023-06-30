package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.ucode.lemmy.api.ApiResult
import ltd.ucode.lemmy.api.request.ListCommunitiesRequest
import ltd.ucode.lemmy.data.type.CommunityView
import ltd.ucode.lemmy.data.type.PostListingType
import ltd.ucode.lemmy.data.type.PostSortType
import javax.inject.Inject

class GroupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val instanceRepository: InstanceRepository,
) {
    suspend fun listCommunities(instance: String?,
                                limit: Int? = null, // <= 50
                                page: Int? = null,
                                sort: PostSortType? = null,
                                type: PostListingType? = null
    ): ApiResult<List<CommunityView>> {
        return instanceRepository[instance].listCommunities(
            ListCommunitiesRequest(
                limit = limit,
                page = page,
                sort = sort,
                type = type
            )
        ).mapSuccess {
            data.communities
        }
    }
}
