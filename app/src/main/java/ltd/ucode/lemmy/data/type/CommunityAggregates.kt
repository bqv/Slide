package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunityAggregates (
    val comments: Int,
    @SerialName("community_id") val communityId: Int,
    val id: Int,
    val posts: Int,
    val subscribers: Int,
    @SerialName("users_active_day") val usersActiveDay: Int,
    @SerialName("users_active_week") val usersActiveWeek: Int,
    @SerialName("users_active_month") val usersActiveMonth: Int,
    @SerialName("users_active_half_year") val usersActiveHalfYear: Int,
)
