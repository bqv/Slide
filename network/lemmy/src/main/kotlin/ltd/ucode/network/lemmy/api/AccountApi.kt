package ltd.ucode.network.lemmy.api

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import ltd.ucode.network.lemmy.api.request.Authenticated
import ltd.ucode.network.lemmy.api.request.CreateCommentLikeRequest
import ltd.ucode.network.lemmy.api.request.CreatePostLikeRequest
import ltd.ucode.network.lemmy.api.request.GetCommunityRequest
import ltd.ucode.network.lemmy.api.request.GetPostsRequest
import ltd.ucode.network.lemmy.api.request.GetSiteRequest
import ltd.ucode.network.lemmy.api.request.GetUnreadCountRequest
import ltd.ucode.network.lemmy.api.request.LoginRequest
import ltd.ucode.network.lemmy.api.request.UploadImageRequest
import ltd.ucode.network.lemmy.api.response.CommentResponse
import ltd.ucode.network.lemmy.api.response.GetCommunityResponse
import ltd.ucode.network.lemmy.api.response.GetPostsResponse
import ltd.ucode.network.lemmy.api.response.GetSiteResponse
import ltd.ucode.network.lemmy.api.response.GetUnreadCountResponse
import ltd.ucode.network.lemmy.api.response.IResponse
import ltd.ucode.network.lemmy.api.response.PostResponse
import ltd.ucode.network.lemmy.api.response.UploadImageResponse
import ltd.ucode.network.lemmy.data.type.jwt.Token
import okhttp3.OkHttpClient
import retrofit2.Response

class AccountApi : InstanceApi {
    private val logger: KLogger = KotlinLogging.logger("ADS:${
        instance
            .split(".")
            .joinToString("") { it.replaceFirstChar(Char::titlecase) }
    }")

    val username: String
    private var password: String? = null
    private var totp: String? = null

    constructor(username: String,
                password: String,
                totp: String?,
                instance: String,
                okHttpClient: OkHttpClient,
    ) : super(instance, okHttpClient) {
        this.username = username
        this.password = password
        this.totp = totp
    }

    constructor(username: String,
                jwt: String,
                instance: String,
                okHttpClient: OkHttpClient
    ) : super(instance, okHttpClient) {
        this.username = username
        this.jwt = Token(jwt)
    }

    init {
        logger.info { "Creating ${AccountApi::class.simpleName}: $instance" }
    }

    private lateinit var jwt: Token

    override suspend fun getCommunity(request: GetCommunityRequest): ApiResult<GetCommunityResponse> =
        super.getCommunity(authenticated(request))

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
        jwt = login(LoginRequest(username, password!!, totp))
            .mapSuccess { data.success.jwt }
            .success
    }

    override suspend fun <T : IResponse> retryOnError(block: suspend () -> Response<T>): ApiResult<T> {
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

    fun token(): Token {
        return jwt
    }
}
