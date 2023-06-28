package ltd.ucode.lemmy.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ltd.ucode.lemmy.data.type.jwt.Token

@Serializable
data class LoginResponse(
    val jwt: String? = null,
    @SerialName("registration_created") val registrationCreated: Boolean,
    @SerialName("verify_email_sent") val verifyEmailSent: Boolean
) {
    val result: Result
        get() = if (verifyEmailSent) {
            Result.EmailNotVerified
        } else if (registrationCreated) {
            Result.WaitApproval
        } else if (jwt.isNullOrBlank()) {
            Result.Failure
        } else {
            Result.Success(Token(jwt))
        }

    val emailNotVerified: Result.EmailNotVerified
        get() = result as Result.EmailNotVerified
    val waitApproval: Result.WaitApproval
        get() = result as Result.WaitApproval
    val failure: Result.Failure
        get() = result as Result.Failure
    val success: Result.Success
        get() = result as Result.Success

    sealed class Result {
        class Success(val jwt: Token): Result()
        object Failure : Result()
        object WaitApproval : Result()
        object EmailNotVerified : Result()
    }
}
