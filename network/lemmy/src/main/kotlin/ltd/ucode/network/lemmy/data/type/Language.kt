package ltd.ucode.network.lemmy.data.type

import kotlinx.serialization.Serializable
import ltd.ucode.network.lemmy.data.value.LanguageCode
import ltd.ucode.network.lemmy.data.id.LanguageId

@Serializable
data class Language (
    val id: LanguageId,
    val code: LanguageCode,
    val name: String,
)
