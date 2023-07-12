package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import ltd.ucode.network.lemmy.api.ApiResult
import ltd.ucode.network.lemmy.api.request.GetPersonDetailsRequest
import ltd.ucode.network.lemmy.api.response.GetPersonDetailsResponse
import ltd.ucode.network.lemmy.data.id.CommunityId
import ltd.ucode.network.lemmy.data.id.PersonId
import ltd.ucode.network.lemmy.data.type.PostSortType
import ltd.ucode.slide.data.ContentDatabase
import ltd.ucode.slide.data.entity.Site
import ltd.ucode.slide.data.entity.User
import javax.inject.Inject
import kotlin.concurrent.thread

class UserRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDatabase: ContentDatabase,
    private val networkRepository: NetworkRepository,
) {
    suspend fun getPersonDetails(instance: String?,
                                 communityId: CommunityId? = null,
                                 limit: Int? = null,
                                 page: Int? = null,
                                 personId: PersonId? = null,
                                 savedOnly: Boolean? = null,
                                 sort: PostSortType? = null,
                                 username: String? = null
    ): ApiResult<GetPersonDetailsResponse> {
        return networkRepository[instance].getPersonDetails(
            GetPersonDetailsRequest(
                communityId = communityId,
                limit = limit,
                page = page,
                personId = personId,
                savedOnly = savedOnly,
                sort = sort,
                username = username
            )
        )
    }

    fun getUserBySiteId(personId: PersonId, site: Site): Flow<User> {
        thread {
            runBlocking(Dispatchers.IO) {
                val response = networkRepository[site.name]
                        .getPersonDetails(GetPersonDetailsRequest())
                response.onSuccess {
                    val user = when (val user = contentDatabase.users.get(personId.id, site.rowId).singleOrNull()) {
                        null -> {
                            User.from(it.personView, site).also { contentDatabase.users.add(it) }
                        }
                        else -> {
                            user.copy(it.personView).also { contentDatabase.users.update(it) }
                        }
                    }

                    user
                }.success
            }
        }

        return contentDatabase.users.flow(personId.id, site.rowId)
    }
}
