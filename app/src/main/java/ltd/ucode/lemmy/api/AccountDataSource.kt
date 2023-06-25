package ltd.ucode.lemmy.api

import android.content.Context
import ltd.ucode.lemmy.api.request.Authenticated
import ltd.ucode.lemmy.api.request.GetSiteRequest
import ltd.ucode.lemmy.api.request.GetUnreadCountRequest
import ltd.ucode.lemmy.api.request.LoginRequest
import ltd.ucode.lemmy.api.request.UploadImageRequest
import ltd.ucode.lemmy.api.response.GetSiteResponse
import ltd.ucode.lemmy.api.response.GetUnreadCountResponse
import ltd.ucode.lemmy.api.response.UploadImageResponse
import ltd.ucode.lemmy.data.type.jwt.Token
import ltd.ucode.slide.repository.AccountRepository
import okhttp3.OkHttpClient
import retrofit2.Response
import javax.inject.Inject

class AccountDataSource @Inject constructor(
    context: Context,
    private val accountRepository: AccountRepository = AccountRepository(context),
    val username: String,
    instance: String,
    okHttpClient: OkHttpClient,
) : InstanceDataSource(context, instance, okHttpClient) {
    private lateinit var jwt: Token

    private val password: String
        get() = accountRepository.getPassword(username, instance)!!

    override suspend fun getSite(request: GetSiteRequest): GetSiteResponse =
        super.getSite(authenticated(request))

    override suspend fun getUnreadCount(request: GetUnreadCountRequest): GetUnreadCountResponse =
        super.getUnreadCount(authenticated(request))

    override suspend fun uploadImage(request: UploadImageRequest): UploadImageResponse =
        super.uploadImage(authenticated(request))

    private suspend fun refresh() {
        jwt = login(LoginRequest(username, password))
            .let { it.success.jwt }
    }

    override suspend fun <T> retryOnError(block: suspend () -> Response<T>): T {
        try {
            return super.retryOnError(block)
        } catch (e: ApiException.AuthenticationException) {
            refresh()
        }
        return super.retryOnError(block)
    }

    private suspend fun <T : Authenticated> authenticated(request: T): T {
        if (!::jwt.isInitialized) refresh()
        return request.apply { auth = jwt.token }
    }
}
