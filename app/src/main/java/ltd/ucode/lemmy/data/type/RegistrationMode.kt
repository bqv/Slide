package ltd.ucode.lemmy.data.type

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RegistrationMode {
    @SerialName("closed") Closed,
    @SerialName("open") Open,
    @SerialName("requireapplication") RequireApplication,
}
