package ltd.ucode.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.type.CommunityId
import ltd.ucode.lemmy.data.type.PersonId
import ltd.ucode.lemmy.data.type.SortType

@Serializable
data class GetPersonDetailsRequest (
    @SerialName("person_id") val personId: PersonId? = null,
    val username: String? = null,
    val sort: SortType? = null,
    val page: Int? = null,
    val limit: Int? = null,
    @SerialName("community_id") val communityId: CommunityId? = null,
    @SerialName("saved_only") val savedOnly: Boolean? = null,
    val auth: String? = null,
)
