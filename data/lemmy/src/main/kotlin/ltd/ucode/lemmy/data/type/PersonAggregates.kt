package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.id.PersonId

@Serializable
data class PersonAggregates (
    val id: Int,
    @SerialName("comment_count") val commentCount: Int,
    @SerialName("comment_score") val commentScore: Int,
    @SerialName("person_id") val personId: PersonId,
    @SerialName("post_count") val postCount: Int,
    @SerialName("post_score") val postScore: Int,
)
