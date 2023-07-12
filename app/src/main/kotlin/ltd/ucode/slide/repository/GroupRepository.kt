package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import ltd.ucode.network.lemmy.api.ApiResult
import ltd.ucode.network.lemmy.api.request.GetCommunityRequest
import ltd.ucode.network.lemmy.api.request.ListCommunitiesRequest
import ltd.ucode.network.lemmy.data.id.CommunityId
import ltd.ucode.network.lemmy.data.type.CommunityView
import ltd.ucode.network.lemmy.data.type.PostListingType
import ltd.ucode.network.lemmy.data.type.PostSortType
import ltd.ucode.slide.data.ContentDatabase
import ltd.ucode.slide.data.entity.Group
import ltd.ucode.slide.data.entity.Site
import javax.inject.Inject
import kotlin.concurrent.thread

class GroupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDatabase: ContentDatabase,
    private val networkRepository: NetworkRepository,
) {
    suspend fun listCommunities(instance: String?,
                                limit: Int? = null, // <= 50
                                page: Int? = null,
                                sort: PostSortType? = null,
                                type: PostListingType? = null
    ): ApiResult<List<CommunityView>> {
        return networkRepository[instance].listCommunities(
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

    fun getGroupBySiteId(communityId: CommunityId, site: Site): Flow<Group> {
        thread {
            runBlocking(Dispatchers.IO) {
                val response = networkRepository[site.name]
                    .getCommunity(GetCommunityRequest())
                response.onSuccess {
                    val group = when (val group = contentDatabase.groups.get(communityId.id, site.rowId).singleOrNull()) {
                        null -> {
                            Group.from(it.communityView, site).also { contentDatabase.groups.add(it) }
                        }
                        else -> {
                            group.copy(it.communityView).also { contentDatabase.groups.update(it) }
                        }
                    }

                    group
                }.success
            }
        }

        return contentDatabase.groups.flow(communityId.id, site.rowId)
    }
}
