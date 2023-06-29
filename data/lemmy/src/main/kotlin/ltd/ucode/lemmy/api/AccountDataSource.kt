package ltd.ucode.lemmy.api

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import ltd.ucode.lemmy.api.request.Authenticated
import ltd.ucode.lemmy.api.request.GetPostsRequest
import ltd.ucode.lemmy.api.request.GetSiteRequest
import ltd.ucode.lemmy.api.request.GetUnreadCountRequest
import ltd.ucode.lemmy.api.request.LoginRequest
import ltd.ucode.lemmy.api.request.UploadImageRequest
import ltd.ucode.lemmy.api.response.GetPostsResponse
import ltd.ucode.lemmy.api.response.GetSiteResponse
import ltd.ucode.lemmy.api.response.GetUnreadCountResponse
import ltd.ucode.lemmy.api.response.UploadImageResponse
import ltd.ucode.lemmy.data.type.jwt.Token
import okhttp3.OkHttpClient
import retrofit2.Response

class AccountDataSource (
    val username: String,
    private val password: String,
    instance: String,
    okHttpClient: OkHttpClient,
) : InstanceDataSource(instance, okHttpClient) {
    private val log: KLogger = KotlinLogging.logger {}

    private lateinit var jwt: Token

    override suspend fun getPosts(request: GetPostsRequest): GetPostsResponse =
        super.getPosts(authenticated(request))

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
