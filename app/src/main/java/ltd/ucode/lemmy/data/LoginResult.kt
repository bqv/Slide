package ltd.ucode.lemmy.data

sealed class LoginResult {
    class Success(val jwt: String): LoginResult()
    object Failure : LoginResult()
    object WaitApproval : LoginResult()
    object EmailNotVerified : LoginResult()
}
