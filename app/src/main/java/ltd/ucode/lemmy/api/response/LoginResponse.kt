package ltd.ucode.lemmy.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.LoginResult

@Serializable
data class LoginResponse(
    val jwt: String? = null,
    @SerialName("registration_created") val registrationCreated: Boolean,
    @SerialName("verify_email_sent") val verifyEmailSent: Boolean
) {
    fun toResult(): LoginResult {
        if (verifyEmailSent) {
            return LoginResult.EmailNotVerified
        } else if (registrationCreated) {
            return LoginResult.WaitApproval
        } else if (jwt.isNullOrBlank()) {
            return LoginResult.Failure
        }

        return LoginResult.Success(jwt)
    }
}
