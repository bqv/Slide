package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class PersonViewSafe (
    val counts: PersonAggregates,
    val person: PersonSafe,
)
