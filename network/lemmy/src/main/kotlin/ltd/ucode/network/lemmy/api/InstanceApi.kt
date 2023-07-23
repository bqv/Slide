package ltd.ucode.network.lemmy.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.reactivecircus.cache4k.Cache
import io.github.reactivecircus.cache4k.CacheEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import ltd.ucode.network.Serializers
import ltd.ucode.network.lemmy.api.ApiCache.cached
import ltd.ucode.network.lemmy.api.ApiCache.cachedUnit
import ltd.ucode.network.lemmy.api.ApiCache.cachedWithDefault
import ltd.ucode.network.lemmy.api.KeepJson.ConverterFactoryWrapper.Companion.keepJson
import ltd.ucode.network.lemmy.api.request.Authenticated
import ltd.ucode.network.lemmy.api.request.CreateCommentLikeRequest
import ltd.ucode.network.lemmy.api.request.CreatePostLikeRequest
import ltd.ucode.network.lemmy.api.request.GetCommentsRequest
import ltd.ucode.network.lemmy.api.request.GetCommunityRequest
import ltd.ucode.network.lemmy.api.request.GetFederatedInstancesRequest
import ltd.ucode.network.lemmy.api.request.GetPersonDetailsRequest
import ltd.ucode.network.lemmy.api.request.GetPostRequest
import ltd.ucode.network.lemmy.api.request.GetPostsRequest
import ltd.ucode.network.lemmy.api.request.GetSiteRequest
import ltd.ucode.network.lemmy.api.request.GetUnreadCountRequest
import ltd.ucode.network.lemmy.api.request.ListCommunitiesRequest
import ltd.ucode.network.lemmy.api.request.LoginRequest
import ltd.ucode.network.lemmy.api.request.UploadImageRequest
import ltd.ucode.network.lemmy.api.response.IResponse
import ltd.ucode.network.lemmy.data.type.NodeInfoResult
import ltd.ucode.network.lemmy.data.type.jwt.Token
import ltd.ucode.network.lemmy.data.type.webfinger.Resource
import ltd.ucode.network.lemmy.data.type.webfinger.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import java.net.HttpCookie
import kotlin.reflect.KFunction1
import kotlin.time.Duration.Companion.ZERO

open class InstanceApi (
    val instance: String,
    private val okHttpClient: OkHttpClient,
) {
    private val logger: KLogger by lazy {
        val domain = instance
            .split(".")
            .joinToString("") { it.replaceFirstChar(Char::titlecase) }
        if (username == null)
            KotlinLogging.logger("lemmy:$domain")
        else
            KotlinLogging.logger("lemmy:$username@$domain")
    }

    init {
        logger.info { "Creating Lemmy API: ${username?.let { "$it@" }.orEmpty()}$instance"}
    }

    protected val api: ILemmyHttpApi by lazy { createApi() }
    private val pictrs: String by lazy { "https://$instance/pictrs/image" }

    val isAuthenticated: Boolean get() = username != null
    var username: String? = null
        private set
    protected var password: String? = null
    private var totp: String? = null
    private var jwt: Token? = null
    val token: Token
        get() = jwt!!

    constructor(username: String,
                password: String,
                totp: String?,
                instance: String,
                okHttpClient: OkHttpClient,
    ) : this(instance, okHttpClient) {
        this.username = username
        this.password = password
        this.totp = totp
    }

    constructor(username: String,
                jwt: String,
                instance: String,
                okHttpClient: OkHttpClient
    ) : this(instance, okHttpClient) {
        this.username = username
        this.jwt = Token(jwt)
    }

    companion object {
        private const val SECONDS: Long = 1000
        const val MAX_RETRIES: Int = 3
        const val RETRY_DELAY: Long = 3 * SECONDS
        const val RETRY_BACKOFF: Float = 2f
        const val DELAY_MAX: Long = 60 * SECONDS
    }

    val nodeInfo by cachedUnit(timeToLive = null) {
        retryOnError { api.nodeInfo() } }
    val nodeInfo20 by cached(timeToLive = null) { url: Uri ->
        retryOnError { api.nodeInfo20(url) } }

    suspend fun getNodeInfo(): ApiResult<NodeInfoResult> {
        return nodeInfo(Unit).bindSuccess {
            data.let { resource: Resource ->
                val url = resource.links.first().href
                nodeInfo20(url) // lets hope
                    .mapSuccess {
                        NodeInfoResult(resource, data)
                    }
            }
        }
    }

    //val addAdmin by cached() { request: AddAdminRequest ->
    //    retryOnError { api.addAdmin(authenticated(request)) } }

    //val addModToCommunity by cached() { request: AddModToCommunityRequest ->
    //    retryOnError { api.addModToCommunity(authenticated(request)) } }

    //val approveRegistrationApplication by cached() { request: ApproveRegistrationApplicationRequest ->
    //    retryOnError { api.approveRegistrationApplication(authenticated(request)) } }

    //val banFromCommunity by cached() { request: BanFromCommunityRequest ->
    //    retryOnError { api.banFromCommunity(authenticated(request)) } }

    //val banPerson by cached() { request: BanPersonRequest ->
    //    retryOnError { api.banPerson(authenticated(request)) } }

    //val blockCommunity by cached() { request: BlockCommunityRequest ->
    //    retryOnError { api.blockCommunity(authenticated(request)) } }

    //val blockPerson by cached() { request: BlockPersonRequest ->
    //    retryOnError { api.blockPerson(authenticated(request)) } }

    //val changePassword by cached() { request: ChangePasswordRequest ->
    //    retryOnError { api.changePassword(authenticated(request)) } }

    //val createComment by cached() { request: CreateCommentRequest ->
    //    retryOnError { api.createComment(authenticated(request)) } }

    //val createCommentReport by cached() { request: CreateCommentReportRequest ->
    //    retryOnError { api.createCommentReport(authenticated(request)) } }

    //val createCommunity by cached() { request: CreateCommunityRequest ->
    //    retryOnError { api.createCommunity(authenticated(request)) } }

    //val createCustomEmoji by cached() { request: CreateCustomEmojiRequest ->
    //    retryOnError { api.createCustomEmoji(authenticated(request)) } }

    //val createPost by cached() { request: CreatePostRequest ->
    //    retryOnError { api.createPost(authenticated(request)) } }

    //val createPostReport by cached() { request: CreatePostReportRequest ->
    //    retryOnError { api.createPostReport(authenticated(request)) } }

    //val createPrivateMessage by cached() { request: CreatePrivateMessageRequest ->
    //    retryOnError { api.createPrivateMessage(authenticated(request)) } }

    //val createPrivateMessageReport by cached() { request: CreatePrivateMessageReportRequest ->
    //    retryOnError { api.createPrivateMessageReport(authenticated(request)) } }

    //val createSite by cached() { request: CreateSiteRequest ->
    //    retryOnError { api.createSite(authenticated(request)) } }

    //val deleteAccount by cached() { request: DeleteAccountRequest ->
    //    retryOnError { api.deleteAccount(authenticated(request)) } }

    //val deleteComment by cached() { request: DeleteCommentRequest ->
    //    retryOnError { api.deleteComment(authenticated(request)) } }

    //val deleteCommunity by cached() { request: DeleteCommunityRequest ->
    //    retryOnError { api.deleteCommunity(authenticated(request)) } }

    //val deleteCustomEmoji by cached() { request: DeleteCustomEmojiRequest ->
    //    retryOnError { api.deleteCustomEmoji(authenticated(request)) } }

    //val deletePost by cached() { request: DeletePostRequest ->
    //    retryOnError { api.deletePost(authenticated(request)) } }

    //val deletePrivateMessage by cached() { request: DeletePrivateMessageRequest ->
    //    retryOnError { api.deletePrivateMessage(authenticated(request)) } }

    //val distinguishComment by cached() { request: DistinguishCommentRequest ->
    //    retryOnError { api.distinguishComment(authenticated(request)) } }

    //val editComment by cached() { request: EditCommentRequest ->
    //    retryOnError { api.editComment(authenticated(request)) } }

    //val editCommunity by cached() { request: EditCommunityRequest ->
    //    retryOnError { api.editCommunity(authenticated(request)) } }

    //val editCustomEmoji by cached() { request: EditCustomEmojiRequest ->
    //    retryOnError { api.editCustomEmoji(authenticated(request)) } }

    //val editPost by cached() { request: EditPostRequest ->
    //    retryOnError { api.editPost(authenticated(request)) } }

    //val editPrivateMessage by cached() { request: EditPrivateMessageRequest ->
    //    retryOnError { api.editPrivateMessage(authenticated(request)) } }

    //val editSite by cached() { request: EditSiteRequest ->
    //    retryOnError { api.editSite(authenticated(request)) } }

    //val featurePost by cached() { request: FeaturePostRequest ->
    //    retryOnError { api.featurePost(authenticated(request)) } }

    //val followCommunity by cached() { request: FollowCommunityRequest ->
    //    retryOnError { api.followCommunity(authenticated(request)) } }

    //val getBannedPersons by cached() { request: GetBannedPersonsRequest ->
    //    retryOnError { api.getBannedPersons(authenticated(request).toForm()) } }

    //val getCaptcha by cached() { request: GetCaptchaRequest ->
    //    retryOnError { api.getCaptcha(authenticated(request).toForm()) } }

    //val getComment by cached() { request: GetCommentRequest ->
    //    retryOnError { api.getComment(authenticated(request).toForm()) } }

    val getComments by cached() { request: GetCommentsRequest ->
        retryOnError { api.getComments(authenticated(request).toForm()) } }

    val getCommunity by cached() { request: GetCommunityRequest ->
        retryOnError { api.getCommunity(authenticated(request).toForm()) } }

    val getFederatedInstances by cached() { request: GetFederatedInstancesRequest ->
        retryOnError { api.getFederatedInstances(authenticated(request).toForm()) } }

    //val getModlog by cached() { request: GetModlogRequest ->
    //    retryOnError { api.getModlog(authenticated(request).toForm()) } }

    val getPersonDetails by cached() { request: GetPersonDetailsRequest ->
        retryOnError { api.getPersonDetails(authenticated(request).toForm()) } }

    //val getPersonMentions by cached() { request: GetPersonMentionsRequest ->
    //    retryOnError { api.getPersonMentions(authenticated(request).toForm()) } }

    val getPost by cached() { request: GetPostRequest ->
        retryOnError { api.getPost(authenticated(request).toForm()) } }

    val getPosts by cached() { request: GetPostsRequest ->
        retryOnError { api.getPosts(authenticated(request).toForm()) } }

    //val getPrivateMessages by cached() { request: GetPrivateMessagesRequest ->
    //    retryOnError { api.getPrivateMessages(authenticated(request).toForm()) } }

    //val getReplies by cached() { request: GetRepliesRequest ->
    //    retryOnError { api.getReplies(authenticated(request).toForm()) } }

    //val getReportCount by cached() { request: GetReportCountRequest ->
    //    retryOnError { api.getReportCount(authenticated(request).toForm()) } }

    val getSite by cachedWithDefault(GetSiteRequest()) { request ->
        retryOnError { api.getSite(authenticated(request).toForm()) } }

    //val getSiteMetadata by cached() { request: GetSiteMetadataRequest ->
    //    retryOnError { api.getSiteMetadata(authenticated(request).toForm()) } }

    val getUnreadCount by cachedWithDefault(GetUnreadCountRequest()) { request ->
        retryOnError { api.getUnreadCount(authenticated(request).toForm()) } }

    //val getUnreadRegistrationApplicationCount by cached() { request: GetUnreadRegistrationApplicationCountRequest ->
    //    retryOnError { api.getUnreadRegistrationApplicationCount(authenticated(request).toForm()) } }

    //val leaveAdmin by cached() { request: LeaveAdminRequest ->
    //    retryOnError { api.leaveAdmin(authenticated(request)) } }

    val likeComment by cached() { request: CreateCommentLikeRequest ->
        retryOnError { api.likeComment(authenticated(request)) } }

    val likePost by cached() { request: CreatePostLikeRequest ->
        retryOnError { api.likePost(authenticated(request)) } }

    //val listCommentReports by cached() { request: ListCommentReportsRequest ->
    //    retryOnError { api.listCommentReports(authenticated(request).toForm()) } }

    val listCommunities by cached() { request: ListCommunitiesRequest ->
        retryOnError { api.listCommunities(authenticated(request).toForm()) } }

    //val listPostReports by cached() { request: ListPostReportsRequest ->
    //    retryOnError { api.listPostReports(authenticated(request).toForm()) } }

    //val listPrivateMessageReports by cached() { request: ListPrivateMessageReportsRequest ->
    //    retryOnError { api.listPrivateMessageReports(authenticated(request).toForm()) } }

    //val listRegistrationApplications by cached() { request: ListRegistrationApplicationsRequest ->
    //    retryOnError { api.listRegistrationApplications(authenticated(request).toForm()) } }

    //val lockPost by cached() { request: LockPostRequest ->
    //    retryOnError { api.lockPost(authenticated(request)) } }

    val login by cached(timeToLive = ZERO) { request: LoginRequest ->
        retryOnError { api.login(request) } }

    //val markAllAsRead by cached() { request: MarkAllAsReadRequest ->
    //    retryOnError { api.markAllAsRead(authenticated(request)) } }

    //val markCommentReplyAsRead by cached() { request: MarkCommentReplyAsReadRequest ->
    //    retryOnError { api.markCommentReplyAsRead(authenticated(request)) } }

    //val markPersonMentionAsRead by cached() { request: MarkPersonMentionAsReadRequest ->
    //    retryOnError { api.markPersonMentionAsRead(authenticated(request)) } }

    //val markPostAsRead by cached() { request: MarkPostAsReadRequest ->
    //    retryOnError { api.markPostAsRead(authenticated(request)) } }

    //val markPrivateMessageAsRead by cached() { request: MarkPrivateMessageAsReadRequest ->
    //    retryOnError { api.markPrivateMessageAsRead(authenticated(request)) } }

    //val passwordChangeAfterReset by cached() { request: PasswordChangeAfterResetRequest ->
    //    retryOnError { api.passwordChangeAfterReset(authenticated(request)) } }

    //val passwordReset by cached() { request: PasswordResetRequest ->
    //    retryOnError { api.passwordReset(authenticated(request)) } }

    //val purgeComment by cached() { request: PurgeCommentRequest ->
    //    retryOnError { api.purgeComment(authenticated(request)) } }

    //val purgeCommunity by cached() { request: PurgeCommunityRequest ->
    //    retryOnError { api.purgeCommunity(authenticated(request)) } }

    //val purgePerson by cached() { request: PurgePersonRequest ->
    //    retryOnError { api.purgePerson(authenticated(request)) } }

    //val purgePost by cached() { request: PurgePostRequest ->
    //    retryOnError { api.purgePost(authenticated(request)) } }

    //val register by cached() { request: RegisterRequest ->
    //    retryOnError { api.register(authenticated(request)) } }

    //val removeComment by cached() { request: RemoveCommentRequest ->
    //    retryOnError { api.removeComment(authenticated(request)) } }

    //val removeCommunity by cached() { request: RemoveCommunityRequest ->
    //    retryOnError { api.removeCommunity(authenticated(request)) } }

    //val removePost by cached() { request: RemovePostRequest ->
    //    retryOnError { api.removePost(authenticated(request)) } }

    //val resolveCommentReport by cached() { request: ResolveCommentReportRequest ->
    //    retryOnError { api.resolveCommentReport(authenticated(request)) } }

    //val resolveObject by cached() { request: ResolveObjectRequest ->
    //    retryOnError { api.resolveObject(authenticated(request).toForm()) } }

    //val resolvePostReport by cached() { request: ResolvePostReportRequest ->
    //    retryOnError { api.resolvePostReport(authenticated(request)) } }

    //val resolvePrivateMessageReport by cached() { request: ResolvePrivateMessageReportRequest ->
    //    retryOnError { api.resolvePrivateMessageReport(authenticated(request)) } }

    //val saveComment by cached() { request: SaveCommentRequest ->
    //    retryOnError { api.saveComment(authenticated(request)) } }

    //val savePost by cached() { request: SavePostRequest ->
    //    retryOnError { api.savePost(authenticated(request)) } }

    //val saveUserSettings by cached() { request: SaveUserSettingsRequest ->
    //    retryOnError { api.saveUserSettings(authenticated(request)) } }

    //val search by cached() { request: SearchRequest ->
    //    retryOnError { api.search(authenticated(request).toForm()) } }

    //val transferCommunity by cached() { request: TransferCommunityRequest ->
    //    retryOnError { api.transferCommunity(authenticated(request)) } }

    val uploadImage by cached() { request: UploadImageRequest ->
        retryOnError {
            api.uploadImage(pictrs,
                HttpCookie("jwt", authenticated(request).auth!!).toString(),
                MultipartBody.Part.createFormData(
                    "images[]", request.filename, request.image.toRequestBody()
                )
            )
        }
    }

    //open suspend fun verifyEmail(request: VerifyEmailRequest): Unit =
    //    retryOnError { api.verifyEmail(authenticated(request)) } }


    private suspend fun <T : IResponse> retryOnError(block: suspend () -> Response<T>): ApiResult<T> {
        val withCallerScope: KFunction1<Throwable.() -> Unit, Unit> = let {
            val t = Throwable("ApiCall")
            t::run
        }

        var currentDelay = RETRY_DELAY
        var currentRetry = MAX_RETRIES

        val call: suspend () -> T = {
            val response = block()

            //noinspection KotlinCompilerError
            val path = response.raw().request.url.encodedPath

            when {
                response.code() >= 500 -> {
                    throw ServersideException(path, response.code(),
                        response.errorBody()?.string())
                }
                !response.isSuccessful -> {
                    throw ApiException(path, response.code(),
                        response.errorBody()?.string().orEmpty())
                }
                else -> {
                    response.body()!!
                }
            }
        }

        while (currentRetry > 0) {
            val exception = try {
                return ApiResult.Success(instance, call())
            } catch (e: Exception) {
                //e.printStackTrace()
                e
            }

            when (exception) {
                is ApiException -> {
                    if (exception.reason == ApiException.Reason.Unauthenticated && jwt != null) {
                        logger.debug { "Unauthenticated" }
                        refresh()
                    }
                    currentRetry -= 1
                }

                is ServersideException -> {
                    // lenient retry attempts
                }

                else -> {
                    throw exception
                }
            }

            logger.debug { "Retry in $currentDelay" }
            delay(currentDelay)
            currentDelay = (currentDelay * RETRY_BACKOFF).toLong()
                .coerceAtMost(DELAY_MAX)
        }

        return try {
            ApiResult.Success(instance, call())
        } catch (e: ServersideException) {
            withCallerScope(e::addContext)
            ApiResult.Failed(instance, e)
        } catch (e: ApiException) {
            withCallerScope(e::addContext)
            ApiResult.Failed(instance, e)
        } catch (e: Exception) {
            withCallerScope(e::addContext)
            throw e
        }
    }

    private fun createApi(): ILemmyHttpApi {
        logger.info("Creating API Object")

        val retrofit = Retrofit.Builder()
            .baseUrl("https://${instance}/api/v3/")
            .client(okHttpClient)
            .addConverterFactory(
                Serializers.snakeCase.asConverterFactory(
                    "application/json".toMediaType()
                ).keepJson()
            )
            .build()

        return retrofit.create(ILemmyHttpApi::class.java)
    }

    private val json: Json by lazy {
        Json { encodeDefaults = true }
    }

    private inline fun <reified T> T.toForm(): Map<String, String> {
        return json.encodeToJsonElement(this)
            .jsonObject
            .toMap()
            .mapValues { (_, element) -> when (element) {
                is JsonNull -> null
                is JsonPrimitive -> if (element.isString) element.content
                else if (element.intOrNull != null) element.int.toString()
                else TODO()
                is JsonArray -> TODO()
                is JsonObject -> TODO()
            } }
            .filterNotNullValues()
    }

    private suspend fun refresh() {
        logger.debug { "Refreshing Token" }
        jwt = login(LoginRequest(username!!, password!!, totp))
            .mapSuccess { data.success.jwt }
            .success
    }

    protected suspend fun <T : Authenticated> authenticated(request: T): T {
        if (jwt == null) refresh()
        return request.apply { auth = jwt!!.token }
            .also {
                logger.trace { "Authenticated Request: ${request.auth}" }
            }
    }
}

private fun Throwable.addContext(t: Throwable) {
    if (cause == t) return
    cause?.addContext(t) ?: initCause(t)
}

private fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> {
    return mapNotNull { it.value?.let { value -> it.key to value } }
        .toMap()
}
