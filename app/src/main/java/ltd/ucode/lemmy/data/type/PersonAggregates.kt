package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonAggregates (
    @SerialName("comment_count") val commentCount: Int,
    @SerialName("comment_score") val commentScore: Int,
    val id: Int,
    @SerialName("person_id") val personId: PersonId,
    @SerialName("post_count") val postCount: Int,
    @SerialName("post_score") val postScore: Int,
)
