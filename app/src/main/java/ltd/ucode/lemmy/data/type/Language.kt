package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class Language (
    val code: String,
    val id: Int,
    val name: String,
)
