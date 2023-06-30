package ltd.ucode.lemmy.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Marker
import io.github.oshai.kotlinlogging.coroutines.withLoggingContextAsync
import io.github.oshai.kotlinlogging.withLoggingContext
import kotlin.reflect.KFunction1
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
import ltd.ucode.SnakeCaseSerializer
import ltd.ucode.lemmy.api.request.CreateCommentLikeRequest
import ltd.ucode.lemmy.api.request.CreatePostLikeRequest
import ltd.ucode.lemmy.api.request.GetCommentsRequest
import ltd.ucode.lemmy.api.request.GetFederatedInstancesRequest
import ltd.ucode.lemmy.api.request.GetPersonDetailsRequest
import ltd.ucode.lemmy.api.request.GetPostRequest
import ltd.ucode.lemmy.api.request.GetPostsRequest
import ltd.ucode.lemmy.api.request.GetSiteRequest
import ltd.ucode.lemmy.api.request.GetUnreadCountRequest
import ltd.ucode.lemmy.api.request.ListCommunitiesRequest
import ltd.ucode.lemmy.api.request.LoginRequest
import ltd.ucode.lemmy.api.request.UploadImageRequest
import ltd.ucode.lemmy.api.response.CommentResponse
import ltd.ucode.lemmy.api.response.GetCommentsResponse
import ltd.ucode.lemmy.api.response.GetFederatedInstancesResponse
import ltd.ucode.lemmy.api.response.GetPersonDetailsResponse
import ltd.ucode.lemmy.api.response.GetPostResponse
import ltd.ucode.lemmy.api.response.GetPostsResponse
import ltd.ucode.lemmy.api.response.GetSiteResponse
import ltd.ucode.lemmy.api.response.GetUnreadCountResponse
import ltd.ucode.lemmy.api.response.ListCommunitiesResponse
import ltd.ucode.lemmy.api.response.LoginResponse
import ltd.ucode.lemmy.api.response.PostResponse
import ltd.ucode.lemmy.api.response.UploadImageResponse
import ltd.ucode.lemmy.data.type.NodeInfoResult
import ltd.ucode.lemmy.data.type.webfinger.NodeInfo
import ltd.ucode.lemmy.data.type.webfinger.Resource
import ltd.ucode.lemmy.data.type.webfinger.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import java.net.HttpCookie

open class InstanceDataSource (
    val instance: String,
    private val okHttpClient: OkHttpClient,
) {
    private val logger: KLogger = KotlinLogging.logger("IDS:${
        instance
            .split(".")
            .joinToString("") { it.replaceFirstChar(Char::titlecase) }
    }")

    init {
        logger.info { "Creating ${InstanceDataSource::class.simpleName}: $instance"}
    }

    protected val api: ILemmyHttpApi by lazy { createApi() }
    private val pictrs: String = "https://$instance/pictrs/image"

    companion object {
        const val MAX_RETRIES: Int = 3
        const val SECONDS: Long = 1000
        const val RETRY_DELAY: Long = 3 * SECONDS
        const val RETRY_BACKOFF: Float = 2f
        const val DELAY_MAX: Long = 60 * SECONDS
    }

    suspend fun nodeInfo(): ApiResult<Resource> =
        retryOnError { api.nodeInfo() }
    suspend fun nodeInfo20(url: Uri): ApiResult<NodeInfo> =
        retryOnError { api.nodeInfo20(url) }

    suspend fun getNodeInfo(): ApiResult<NodeInfoResult> {
        return nodeInfo().bindSuccess {
            data.let { resource: Resource ->
                val url = resource.links.first().href
                nodeInfo20(url) // lets hope
                    .mapSuccess {
                        NodeInfoResult(resource, data)
                    }
            }
        }
    }

    //open suspend fun addAdmin(request: AddAdminRequest): ApiResult<AddAdminResponse> =
    //    retryOnError { api.addAdmin(request) }

    //open suspend fun addModToCommunity(request: AddModToCommunityRequest): ApiResult<AddModToCommunityResponse> =
    //    retryOnError { api.addModToCommunity(request) }

    //open suspend fun approveRegistrationApplication(request: ApproveRegistrationApplicationRequest): ApiResult<RegistrationApplicationResponse> =
    //    retryOnError { api.approveRegistrationApplication(request) }

    //open suspend fun banFromCommunity(request: BanFromCommunityRequest): ApiResult<BanFromCommunityResponse> =
    //    retryOnError { api.banFromCommunity(request) }

    //open suspend fun banPerson(request: BanPersonRequest): ApiResult<BanPersonResponse> =
    //    retryOnError { api.banPerson(request) }

    //open suspend fun blockCommunity(request: BlockCommunityRequest): ApiResult<BlockCommunityResponse> =
    //    retryOnError { api.blockCommunity(request) }

    //open suspend fun blockPerson(request: BlockPersonRequest): ApiResult<BlockPersonResponse> =
    //    retryOnError { api.blockPerson(request) }

    //open suspend fun changePassword(request: ChangePasswordRequest): ApiResult<LoginResponse> =
    //    retryOnError { api.changePassword(request) }

    //open suspend fun createComment(request: CreateCommentRequest): ApiResult<CommentResponse> =
    //    retryOnError { api.createComment(request) }

    //open suspend fun createCommentReport(request: CreateCommentReportRequest): ApiResult<CommentReportResponse> =
    //    retryOnError { api.createCommentReport(request) }

    //open suspend fun createCommunity(request: CreateCommunityRequest): ApiResult<CommunityResponse> =
    //    retryOnError { api.createCommunity(request) }

    //open suspend fun createCustomEmoji(request: CreateCustomEmojiRequest): ApiResult<CustomEmojiResponse> =
    //    retryOnError { api.createCustomEmoji(request) }

    //open suspend fun createPost(request: CreatePostRequest): ApiResult<PostResponse> =
    //    retryOnError { api.createPost(request) }

    //open suspend fun createPostReport(request: CreatePostReportRequest): ApiResult<PostReportResponse> =
    //    retryOnError { api.createPostReport(request) }

    //open suspend fun createPrivateMessage(request: CreatePrivateMessageRequest): ApiResult<PrivateMessageResponse> =
    //    retryOnError { api.createPrivateMessage(request) }

    //open suspend fun createPrivateMessageReport(request: CreatePrivateMessageReportRequest): ApiResult<PrivateMessageReportResponse> =
    //    retryOnError { api.createPrivateMessageReport(request) }

    //open suspend fun createSite(request: CreateSiteRequest): ApiResult<SiteResponse> =
    //    retryOnError { api.createSite(request) }

    //open suspend fun deleteAccount(request: DeleteAccountRequest): ApiResult<Unit> =
    //    retryOnError { api.deleteAccount(request) }

    //open suspend fun deleteComment(request: DeleteCommentRequest): ApiResult<CommentResponse> =
    //    retryOnError { api.deleteComment(request) }

    //open suspend fun deleteCommunity(request: DeleteCommunityRequest): ApiResult<CommunityResponse> =
    //    retryOnError { api.deleteCommunity(request) }

    //open suspend fun deleteCustomEmoji(request: DeleteCustomEmojiRequest): ApiResult<DeleteCustomEmojiResponse> =
    //    retryOnError { api.deleteCustomEmoji(request) }

    //open suspend fun deletePost(request: DeletePostRequest): ApiResult<PostResponse> =
    //    retryOnError { api.deletePost(request) }

    //open suspend fun deletePrivateMessage(request: DeletePrivateMessageRequest): ApiResult<PrivateMessageResponse> =
    //    retryOnError { api.deletePrivateMessage(request) }

    //open suspend fun distinguishComment(request: DistinguishCommentRequest): ApiResult<CommentResponse> =
    //    retryOnError { api.distinguishComment(request) }

    //open suspend fun editComment(request: EditCommentRequest): ApiResult<CommentResponse> =
    //    retryOnError { api.editComment(request) }

    //open suspend fun editCommunity(request: EditCommunityRequest): ApiResult<CommunityResponse> =
    //    retryOnError { api.editCommunity(request) }

    //open suspend fun editCustomEmoji(request: EditCustomEmojiRequest): ApiResult<CustomEmojiResponse> =
    //    retryOnError { api.editCustomEmoji(request) }

    //open suspend fun editPost(request: EditPostRequest): ApiResult<PostResponse> =
    //    retryOnError { api.editPost(request) }

    //open suspend fun editPrivateMessage(request: EditPrivateMessageRequest): ApiResult<PrivateMessageResponse> =
    //    retryOnError { api.editPrivateMessage(request) }

    //open suspend fun editSite(request: EditSiteRequest): ApiResult<SiteResponse> =
    //    retryOnError { api.editSite(request) }

    //open suspend fun featurePost(request: FeaturePostRequest): ApiResult<PostResponse> =
    //    retryOnError { api.featurePost(request) }

    //open suspend fun followCommunity(request: FollowCommunityRequest): ApiResult<CommunityResponse> =
    //    retryOnError { api.followCommunity(request) }

    //open suspend fun getBannedPersons(request: GetBannedPersonsRequest): ApiResult<BannedPersonsResponse> =
    //    retryOnError { api.getBannedPersons(request.toForm()) }

    //open suspend fun getCaptcha(request: GetCaptchaRequest): ApiResult<GetCaptchaResponse> =
    //    retryOnError { api.getCaptcha(request.toForm()) }

    //open suspend fun getComment(request: GetCommentRequest): ApiResult<CommentResponse> =
    //    retryOnError { api.getComment(request.toForm()) }

    open suspend fun getComments(request: GetCommentsRequest): ApiResult<GetCommentsResponse> =
        retryOnError { api.getComments(request.toForm()) }

    //open suspend fun getCommunity(request: GetCommunityRequest): ApiResult<GetCommunityResponse> =
    //    retryOnError { api.getCommunity(request.toForm()) }

    open suspend fun getFederatedInstances(request: GetFederatedInstancesRequest = GetFederatedInstancesRequest()): ApiResult<GetFederatedInstancesResponse> =
        retryOnError { api.getFederatedInstances(request.toForm()) }

    //open suspend fun getModlog(request: GetModlogRequest): ApiResult<GetModlogResponse> =
    //    retryOnError { api.getModlog(request.toForm()) }

    open suspend fun getPersonDetails(request: GetPersonDetailsRequest): ApiResult<GetPersonDetailsResponse> =
        retryOnError { api.getPersonDetails(request.toForm()) }

    //open suspend fun getPersonMentions(request: GetPersonMentionsRequest): ApiResult<GetPersonMentionsResponse> =
    //    retryOnError { api.getPersonMentions(request.toForm()) }

    open suspend fun getPost(request: GetPostRequest): ApiResult<GetPostResponse> =
        retryOnError { api.getPost(request.toForm()) }

    open suspend fun getPosts(request: GetPostsRequest): ApiResult<GetPostsResponse> =
        retryOnError { api.getPosts(request.toForm()) }

    //open suspend fun getPrivateMessages(request: GetPrivateMessagesRequest): ApiResult<PrivateMessagesResponse> =
    //    retryOnError { api.getPrivateMessages(request.toForm()) }

    //open suspend fun getReplies(request: GetRepliesRequest): ApiResult<GetRepliesResponse> =
    //    retryOnError { api.getReplies(request.toForm()) }

    //open suspend fun getReportCount(request: GetReportCountRequest): ApiResult<GetReportCountResponse> =
    //    retryOnError { api.getReportCount(request.toForm()) }

    open suspend fun getSite(request: GetSiteRequest = GetSiteRequest()): ApiResult<GetSiteResponse> =
        retryOnError { api.getSite(request.toForm()) }

    //open suspend fun getSiteMetadata(request: GetSiteMetadataRequest): ApiResult<GetSiteMetadataResponse> =
    //    retryOnError { api.getSiteMetadata(request.toForm()) }

    open suspend fun getUnreadCount(request: GetUnreadCountRequest = GetUnreadCountRequest()): ApiResult<GetUnreadCountResponse> =
        retryOnError { api.getUnreadCount(request.toForm()) }

    //open suspend fun getUnreadRegistrationApplicationCount(request: GetUnreadRegistrationApplicationCountRequest): ApiResult<GetUnreadRegistrationApplicationCountResponse> =
    //    retryOnError { api.getUnreadRegistrationApplicationCount(request.toForm()) }

    //open suspend fun leaveAdmin(request: LeaveAdminRequest): ApiResult<GetSiteResponse> =
    //    retryOnError { api.leaveAdmin(request) }

    open suspend fun likeComment(request: CreateCommentLikeRequest): ApiResult<CommentResponse> =
        retryOnError { api.likeComment(request) }

    open suspend fun likePost(request: CreatePostLikeRequest): ApiResult<PostResponse> =
        retryOnError { api.likePost(request) }

    //open suspend fun listCommentReports(request: ListCommentReportsRequest): ApiResult<ListCommentReportsResponse> =
    //    retryOnError { api.listCommentReports(request.toForm()) }

    open suspend fun listCommunities(request: ListCommunitiesRequest): ApiResult<ListCommunitiesResponse> =
        retryOnError { api.listCommunities(request.toForm()) }

    //open suspend fun listPostReports(request: ListPostReportsRequest): ApiResult<ListPostReportsResponse> =
    //    retryOnError { api.listPostReports(request.toForm()) }

    //open suspend fun listPrivateMessageReports(request: ListPrivateMessageReportsRequest): ApiResult<ListPrivateMessageReportsResponse> =
    //    retryOnError { api.listPrivateMessageReports(request.toForm()) }

    //open suspend fun listRegistrationApplications(request: ListRegistrationApplicationsRequest): ApiResult<ListRegistrationApplicationsResponse> =
    //    retryOnError { api.listRegistrationApplications(request.toForm()) }

    //open suspend fun lockPost(request: LockPostRequest): ApiResult<PostResponse> =
    //    retryOnError { api.lockPost(request) }

    open suspend fun login(request: LoginRequest): ApiResult<LoginResponse> =
        retryOnError { api.login(request) }

    //open suspend fun markAllAsRead(request: MarkAllAsReadRequest): ApiResult<GetRepliesResponse> =
    //    retryOnError { api.markAllAsRead(request) }

    //open suspend fun markCommentReplyAsRead(request: MarkCommentReplyAsReadRequest): ApiResult<CommentReplyResponse> =
    //    retryOnError { api.markCommentReplyAsRead(request) }

    //open suspend fun markPersonMentionAsRead(request: MarkPersonMentionAsReadRequest): ApiResult<PersonMentionResponse> =
    //    retryOnError { api.markPersonMentionAsRead(request) }

    //open suspend fun markPostAsRead(request: MarkPostAsReadRequest): ApiResult<PostResponse> =
    //    retryOnError { api.markPostAsRead(request) }

    //open suspend fun markPrivateMessageAsRead(request: MarkPrivateMessageAsReadRequest): ApiResult<PrivateMessageResponse> =
    //    retryOnError { api.markPrivateMessageAsRead(request) }

    //open suspend fun passwordChangeAfterReset(request: PasswordChangeAfterResetRequest): ApiResult<LoginResponse> =
    //    retryOnError { api.passwordChangeAfterReset(request) }

    //open suspend fun passwordReset(request: PasswordResetRequest): ApiResult<Unit> =
    //    retryOnError { api.passwordReset(request) }

    //open suspend fun purgeComment(request: PurgeCommentRequest): ApiResult<PurgeItemResponse> =
    //    retryOnError { api.purgeComment(request) }

    //open suspend fun purgeCommunity(request: PurgeCommunityRequest): ApiResult<PurgeItemResponse> =
    //    retryOnError { api.purgeCommunity(request) }

    //open suspend fun purgePerson(request: PurgePersonRequest): ApiResult<PurgeItemResponse> =
    //    retryOnError { api.purgePerson(request) }

    //open suspend fun purgePost(request: PurgePostRequest): ApiResult<PurgeItemResponse> =
    //    retryOnError { api.purgePost(request) }

    //open suspend fun register(request: RegisterRequest): ApiResult<LoginResponse> =
    //    retryOnError { api.register(request) }

    //open suspend fun removeComment(request: RemoveCommentRequest): ApiResult<CommentResponse> =
    //    retryOnError { api.removeComment(request) }

    //open suspend fun removeCommunity(request: RemoveCommunityRequest): ApiResult<CommunityResponse> =
    //    retryOnError { api.removeCommunity(request) }

    //open suspend fun removePost(request: RemovePostRequest): ApiResult<PostResponse> =
    //    retryOnError { api.removePost(request) }

    //open suspend fun resolveCommentReport(request: ResolveCommentReportRequest): ApiResult<CommentReportResponse> =
    //    retryOnError { api.resolveCommentReport(request) }

    //open suspend fun resolveObject(request: ResolveObjectRequest): ApiResult<ResolveObjectResponse> =
    //    retryOnError { api.resolveObject(request.toForm()) }

    //open suspend fun resolvePostReport(request: ResolvePostReportRequest): ApiResult<PostReportResponse> =
    //    retryOnError { api.resolvePostReport(request) }

    //open suspend fun resolvePrivateMessageReport(request: ResolvePrivateMessageReportRequest): ApiResult<PrivateMessageReportResponse> =
    //    retryOnError { api.resolvePrivateMessageReport(request) }

    //open suspend fun saveComment(request: SaveCommentRequest): ApiResult<CommentResponse> =
    //    retryOnError { api.saveComment(request) }

    //open suspend fun savePost(request: SavePostRequest): ApiResult<PostResponse> =
    //    retryOnError { api.savePost(request) }

    //open suspend fun saveUserSettings(request: SaveUserSettingsRequest): ApiResult<LoginResponse> =
    //    retryOnError { api.saveUserSettings(request) }

    //open suspend fun search(request: SearchRequest): ApiResult<SearchResponse> =
    //    retryOnError { api.search(request.toForm()) }

    //open suspend fun transferCommunity(request: TransferCommunityRequest): ApiResult<GetCommunityResponse> =
    //    retryOnError { api.transferCommunity(request) }

    open suspend fun uploadImage(request: UploadImageRequest): ApiResult<UploadImageResponse> =
        retryOnError { api.uploadImage(pictrs,
            HttpCookie("jwt", request.auth!!).toString(),
            request.image.let {
                MultipartBody.Part.createFormData(
                    "images[]", request.filename, request.image.toRequestBody()
                )
            }) }

    //open suspend fun verifyEmail(request: VerifyEmailRequest): Unit =
    //    retryOnError { api.verifyEmail(request) }


    protected open suspend fun <T> retryOnError(block: suspend () -> Response<T>): ApiResult<T> {
        val withCallerScope: KFunction1<Throwable.() -> Unit, Unit> = let {
            val t = Throwable("ApiCall")
            t::run
        }

        var currentDelay = RETRY_DELAY
        var currentRetry = MAX_RETRIES

        val call: suspend () -> T = {
            val response = block()

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
                SnakeCaseSerializer.asConverterFactory(
                    "application/json".toMediaType()
                )
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
}

private fun Throwable.addContext(t: Throwable) {
    if (cause == t) return
    cause?.addContext(t) ?: initCause(t)
}

private fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> {
    return mapNotNull { it.value?.let { value -> it.key to value } }
        .toMap()
}
