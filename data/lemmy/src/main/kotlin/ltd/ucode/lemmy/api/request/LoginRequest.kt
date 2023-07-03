package ltd.ucode.lemmy.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("username_or_email") val usernameOrEmail: String,
    val password: String,
    @SerialName("totp_2fa_token") val totp2faToken: String? = null,
)
