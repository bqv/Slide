package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SiteAggregates (
    val comments: Int,
    val communities: Int,
    val id: Int,
    val posts: Int,
    @SerialName("site_id") val siteId: SiteId,
    val users: Int,
    @SerialName("users_active_day") val usersActiveDay: Int,
    @SerialName("users_active_week") val usersActiveWeek: Int,
    @SerialName("users_active_month") val usersActiveMonth: Int,
    @SerialName("users_active_half_year") val usersActiveHalfYear: Int,
)
