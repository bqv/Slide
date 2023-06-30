package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ltd.ucode.lemmy.api.ApiResult
import ltd.ucode.lemmy.api.request.GetPersonDetailsRequest
import ltd.ucode.lemmy.api.response.GetPersonDetailsResponse
import ltd.ucode.lemmy.data.id.CommunityId
import ltd.ucode.lemmy.data.id.PersonId
import ltd.ucode.lemmy.data.type.PostSortType
import javax.inject.Inject

class UserRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val instanceRepository: InstanceRepository,
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
        return instanceRepository[instance].getPersonDetails(
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
}
