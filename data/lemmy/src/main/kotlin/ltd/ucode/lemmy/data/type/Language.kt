package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.id.LanguageCode
import ltd.ucode.lemmy.data.id.LanguageId

@Serializable
data class Language (
    val id: LanguageId,
    val code: LanguageCode,
    val name: String,
)
