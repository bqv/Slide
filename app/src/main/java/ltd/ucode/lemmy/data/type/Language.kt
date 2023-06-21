package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable

@Serializable
data class Language (
    val id: LanguageId,
    val code: LanguageCode,
    val name: String,
)
