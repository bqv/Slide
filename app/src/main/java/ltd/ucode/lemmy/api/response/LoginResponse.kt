package ltd.ucode.lemmy.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val jwt: String?,
    @SerialName("registration_created") val registrationCreated: Boolean,
    @SerialName("verify_email_sent") val verifyEmailSent: Boolean
)
