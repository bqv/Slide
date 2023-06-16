package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class PersonAggregates (
    val comment_count: Int,
    val comment_score: Int,
    val id: Int,
    val person_id: Int,
    val post_count: Int,
    val post_score: Int,
)
