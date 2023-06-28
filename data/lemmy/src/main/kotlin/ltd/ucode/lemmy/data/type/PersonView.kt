package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class PersonView (
    val counts: PersonAggregates,
    val person: Person,
)
