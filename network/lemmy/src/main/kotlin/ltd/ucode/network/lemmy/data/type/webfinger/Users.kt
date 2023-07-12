package ltd.ucode.network.lemmy.data.type.webfinger

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Users (
    val total: Int? = null,
    @SerialName("activeHalfyear") val activeHalfYear: Int? = null,
    val activeMonth: Int? = null,
)
