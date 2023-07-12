package ltd.ucode.network.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.id.CommunityId
import ltd.ucode.network.lemmy.data.id.PersonId
import ltd.ucode.network.lemmy.data.type.PostSortType

@Serializable
data class GetPersonDetailsRequest (
    override var auth: String? = null,
    @SerialName("person_id") val personId: PersonId? = null,
    val username: String? = null,
    val sort: PostSortType? = null,
    val page: Int? = null,
    val limit: Int? = null,
    @SerialName("community_id") val communityId: CommunityId? = null,
    @SerialName("saved_only") val savedOnly: Boolean? = null,
) : Authenticated
