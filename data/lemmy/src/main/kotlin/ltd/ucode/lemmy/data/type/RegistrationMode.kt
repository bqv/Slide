package ltd.ucode.lemmy.data.type

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
enum class RegistrationMode {
    // became CamelCase from 0.18.0 onwards
    @JsonNames("closed") Closed,
    @JsonNames("open") Open,
    @JsonNames("requireapplication") RequireApplication,
}
