package ltd.ucode.network.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.id.CommunityId

@Serializable
data class CommunityAggregates (
    val comments: Int,
    @SerialName("community_id") val communityId: CommunityId,
    val id: Int,
    val posts: Int,
    val subscribers: Int,
    @SerialName("users_active_day") val usersActiveDay: Int,
    @SerialName("users_active_week") val usersActiveWeek: Int,
    @SerialName("users_active_month") val usersActiveMonth: Int,
    @SerialName("users_active_half_year") val usersActiveHalfYear: Int,
    @SerialName("hot_rank") val hotRank: Int = Int.MAX_VALUE, // ADDED in 0.17.4
)
