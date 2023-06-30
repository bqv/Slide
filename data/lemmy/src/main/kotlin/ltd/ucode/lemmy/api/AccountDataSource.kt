package ltd.ucode.lemmy.api

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import ltd.ucode.lemmy.api.request.Authenticated
import ltd.ucode.lemmy.api.request.CreateCommentLikeRequest
import ltd.ucode.lemmy.api.request.CreatePostLikeRequest
import ltd.ucode.lemmy.api.request.GetPostsRequest
import ltd.ucode.lemmy.api.request.GetSiteRequest
import ltd.ucode.lemmy.api.request.GetUnreadCountRequest
import ltd.ucode.lemmy.api.request.LoginRequest
import ltd.ucode.lemmy.api.request.UploadImageRequest
import ltd.ucode.lemmy.api.response.CommentResponse
import ltd.ucode.lemmy.api.response.GetPostsResponse
import ltd.ucode.lemmy.api.response.GetSiteResponse
import ltd.ucode.lemmy.api.response.GetUnreadCountResponse
import ltd.ucode.lemmy.api.response.PostResponse
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
    private val logger: KLogger = KotlinLogging.logger("ADS:${
        instance
            .split(".")
            .joinToString("") { it.replaceFirstChar(Char::titlecase) }
    }")

    init {
        logger.info { "Creating ${AccountDataSource::class.simpleName}: $instance"}
    }

    private lateinit var jwt: Token

    override suspend fun getPosts(request: GetPostsRequest): ApiResult<GetPostsResponse> =
        super.getPosts(authenticated(request))

    override suspend fun getSite(request: GetSiteRequest): ApiResult<GetSiteResponse> =
        super.getSite(authenticated(request))

    override suspend fun getUnreadCount(request: GetUnreadCountRequest): ApiResult<GetUnreadCountResponse> =
        super.getUnreadCount(authenticated(request))

    override suspend fun likeComment(request: CreateCommentLikeRequest): ApiResult<CommentResponse> =
        super.likeComment(authenticated(request))

    override suspend fun likePost(request: CreatePostLikeRequest): ApiResult<PostResponse> =
        super.likePost(authenticated(request))

    override suspend fun uploadImage(request: UploadImageRequest): ApiResult<UploadImageResponse> =
        super.uploadImage(authenticated(request))

    private suspend fun refresh() {
        logger.debug { "Refreshing Token" }
        jwt = login(LoginRequest(username, password))
            .mapSuccess { data.success.jwt }
            .success
    }

    override suspend fun <T> retryOnError(block: suspend () -> Response<T>): ApiResult<T> {
        try {
            return super.retryOnError(block)
        } catch (e: ApiException) {
            if (e.reason != ApiException.Reason.Unauthenticated)
                throw e
            logger.debug { "Unauthenticated" }

            refresh()
        }
        return super.retryOnError(block)
    }

    private suspend fun <T : Authenticated> authenticated(request: T): T {
        if (!::jwt.isInitialized) refresh()
        return request.apply { auth = jwt.token }
            .also {
                logger.debug { "Authenticated Request: ${request.auth}" }
            }
    }
}
