package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class PersonBlockView (
    val person: Person,
    val target: Person,
)
