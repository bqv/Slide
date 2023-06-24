package ltd.ucode.lemmy.api

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import ltd.ucode.Util
import ltd.ucode.Util.SnakeCaseSerializer
import ltd.ucode.Util.filterNotNullValues
import ltd.ucode.lemmy.api.request.GetCommentsRequest
import ltd.ucode.lemmy.api.request.GetPostRequest
import ltd.ucode.lemmy.api.request.GetPostsRequest
import ltd.ucode.lemmy.api.request.GetSiteRequest
import ltd.ucode.lemmy.api.request.GetUnreadCountRequest
import ltd.ucode.lemmy.api.request.ListCommunitiesRequest
import ltd.ucode.lemmy.api.request.LoginRequest
import ltd.ucode.lemmy.api.request.UploadImageRequest
import ltd.ucode.lemmy.api.response.GetCommentsResponse
import ltd.ucode.lemmy.api.response.GetPostResponse
import ltd.ucode.lemmy.api.response.GetPostsResponse
import ltd.ucode.lemmy.api.response.GetSiteResponse
import ltd.ucode.lemmy.api.response.GetUnreadCountResponse
import ltd.ucode.lemmy.api.response.ListCommunitiesResponse
import ltd.ucode.lemmy.api.response.LoginResponse
import ltd.ucode.lemmy.api.response.UploadImageResponse
import ltd.ucode.lemmy.data.type.NodeInfoResult
import ltd.ucode.lemmy.data.type.webfinger.NodeInfo
import ltd.ucode.lemmy.data.type.webfinger.Resource
import ltd.ucode.lemmy.data.type.webfinger.Uri
import ltd.ucode.slide.BuildConfig
import me.ccrama.redditslide.util.LogUtil
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import java.net.HttpCookie

const val MAX_RETRIES: Int = 3
const val SECONDS: Long = 1000
const val RETRY_DELAY: Long = 3 * SECONDS
const val RETRY_BACKOFF: Float = 2f
const val DELAY_MAX: Long = 60 * SECONDS

open class InstanceDataSource(
    val context: Context,
    val instance: String,
    protected open val headers: Map<String, String> = mapOf(
        Pair("User-Agent", Util.userAgent)
    ),
) {
    protected val api: ILemmyHttpApi by lazy { createApi() }
    private val pictrs: String = "https://$instance/pictrs/image"

    suspend fun nodeInfo(): Resource = retryOnError { api.nodeInfo() }
    suspend fun nodeInfo20(url: Uri): NodeInfo = retryOnError { api.nodeInfo20(url) }

    suspend fun getNodeInfo(): NodeInfoResult {
        return nodeInfo().let {
            NodeInfoResult(
                it,
                it.links.first().href
                    .let { url -> nodeInfo20(url) } // lets hope
            )
        }
    }

    //open suspend fun addAdmin(request: AddAdminRequest): AddAdminResponse =
    //    retryOnError { api.addAdmin(request) }

    //open suspend fun addModToCommunity(request: AddModToCommunityRequest): AddModToCommunityResponse =
    //    retryOnError { api.addModToCommunity(request) }

    //open suspend fun approveRegistrationApplication(request: ApproveRegistrationApplicationRequest): RegistrationApplicationResponse =
    //    retryOnError { api.approveRegistrationApplication(request) }

    //open suspend fun banFromCommunity(request: BanFromCommunityRequest): BanFromCommunityResponse =
    //    retryOnError { api.banFromCommunity(request) }

    //open suspend fun banPerson(request: BanPersonRequest): BanPersonResponse =
    //    retryOnError { api.banPerson(request) }

    //open suspend fun blockCommunity(request: BlockCommunityRequest): BlockCommunityResponse =
    //    retryOnError { api.blockCommunity(request) }

    //open suspend fun blockPerson(request: BlockPersonRequest): BlockPersonResponse =
    //    retryOnError { api.blockPerson(request) }

    //open suspend fun changePassword(request: ChangePasswordRequest): LoginResponse =
    //    retryOnError { api.changePassword(request) }

    //open suspend fun createComment(request: CreateCommentRequest): CommentResponse =
    //    retryOnError { api.createComment(request) }

    //open suspend fun createCommentReport(request: CreateCommentReportRequest): CommentReportResponse =
    //    retryOnError { api.createCommentReport(request) }

    //open suspend fun createCommunity(request: CreateCommunityRequest): CommunityResponse =
    //    retryOnError { api.createCommunity(request) }

    //open suspend fun createCustomEmoji(request: CreateCustomEmojiRequest): CustomEmojiResponse =
    //    retryOnError { api.createCustomEmoji(request) }

    //open suspend fun createPost(request: CreatePostRequest): PostResponse =
    //    retryOnError { api.createPost(request) }

    //open suspend fun createPostReport(request: CreatePostReportRequest): PostReportResponse =
    //    retryOnError { api.createPostReport(request) }

    //open suspend fun createPrivateMessage(request: CreatePrivateMessageRequest): PrivateMessageResponse =
    //    retryOnError { api.createPrivateMessage(request) }

    //open suspend fun createPrivateMessageReport(request: CreatePrivateMessageReportRequest): PrivateMessageReportResponse =
    //    retryOnError { api.createPrivateMessageReport(request) }

    //open suspend fun createSite(request: CreateSiteRequest): SiteResponse =
    //    retryOnError { api.createSite(request) }

    //open suspend fun deleteAccount(request: DeleteAccountRequest): Unit =
    //    retryOnError { api.deleteAccount(request) }

    //open suspend fun deleteComment(request: DeleteCommentRequest): CommentResponse =
    //    retryOnError { api.deleteComment(request) }

    //open suspend fun deleteCommunity(request: DeleteCommunityRequest): CommunityResponse =
    //    retryOnError { api.deleteCommunity(request) }

    //open suspend fun deleteCustomEmoji(request: DeleteCustomEmojiRequest): DeleteCustomEmojiResponse =
    //    retryOnError { api.deleteCustomEmoji(request) }

    //open suspend fun deletePost(request: DeletePostRequest): PostResponse =
    //    retryOnError { api.deletePost(request) }

    //open suspend fun deletePrivateMessage(request: DeletePrivateMessageRequest): PrivateMessageResponse =
    //    retryOnError { api.deletePrivateMessage(request) }

    //open suspend fun distinguishComment(request: DistinguishCommentRequest): CommentResponse =
    //    retryOnError { api.distinguishComment(request) }

    //open suspend fun editComment(request: EditCommentRequest): CommentResponse =
    //    retryOnError { api.editComment(request) }

    //open suspend fun editCommunity(request: EditCommunityRequest): CommunityResponse =
    //    retryOnError { api.editCommunity(request) }

    //open suspend fun editCustomEmoji(request: EditCustomEmojiRequest): CustomEmojiResponse =
    //    retryOnError { api.editCustomEmoji(request) }

    //open suspend fun editPost(request: EditPostRequest): PostResponse =
    //    retryOnError { api.editPost(request) }

    //open suspend fun editPrivateMessage(request: EditPrivateMessageRequest): PrivateMessageResponse =
    //    retryOnError { api.editPrivateMessage(request) }

    //open suspend fun editSite(request: EditSiteRequest): SiteResponse =
    //    retryOnError { api.editSite(request) }

    //open suspend fun featurePost(request: FeaturePostRequest): PostResponse =
    //    retryOnError { api.featurePost(request) }

    //open suspend fun followCommunity(request: FollowCommunityRequest): CommunityResponse =
    //    retryOnError { api.followCommunity(request) }

    //open suspend fun getBannedPersons(request: GetBannedPersonsRequest): BannedPersonsResponse =
    //    retryOnError { api.getBannedPersons(request.toForm()) }

    //open suspend fun getCaptcha(request: GetCaptchaRequest): GetCaptchaResponse =
    //    retryOnError { api.getCaptcha(request.toForm()) }

    //open suspend fun getComment(request: GetCommentRequest): CommentResponse =
    //    retryOnError { api.getComment(request.toForm()) }

    open suspend fun getComments(request: GetCommentsRequest): GetCommentsResponse =
        retryOnError { api.getComments(request.toForm()) }

    //open suspend fun getCommunity(request: GetCommunityRequest): GetCommunityResponse =
    //    retryOnError { api.getCommunity(request.toForm()) }

    //open suspend fun getFederatedInstances(request: GetFederatedInstancesRequest): GetFederatedInstancesResponse =
    //    retryOnError { api.getFederatedInstances(request.toForm()) }

    //open suspend fun getModlog(request: GetModlogRequest): GetModlogResponse =
    //    retryOnError { api.getModlog(request.toForm()) }

    //open suspend fun getPersonDetails(request: GetPersonDetailsRequest): GetPersonDetailsResponse =
    //    retryOnError { api.getPersonDetails(request.toForm()) }

    //open suspend fun getPersonMentions(request: GetPersonMentionsRequest): GetPersonMentionsResponse =
    //    retryOnError { api.getPersonMentions(request.toForm()) }

    open suspend fun getPost(request: GetPostRequest): GetPostResponse =
        retryOnError { api.getPost(request.toForm()) }

    open suspend fun getPosts(request: GetPostsRequest): GetPostsResponse =
        retryOnError { api.getPosts(request.toForm()) }

    //open suspend fun getPrivateMessages(request: GetPrivateMessagesRequest): PrivateMessagesResponse =
    //    retryOnError { api.getPrivateMessages(request.toForm()) }

    //open suspend fun getReplies(request: GetRepliesRequest): GetRepliesResponse =
    //    retryOnError { api.getReplies(request.toForm()) }

    //open suspend fun getReportCount(request: GetReportCountRequest): GetReportCountResponse =
    //    retryOnError { api.getReportCount(request.toForm()) }

    open suspend fun getSite(request: GetSiteRequest = GetSiteRequest()): GetSiteResponse =
        retryOnError { api.getSite(request.toForm()) }

    //open suspend fun getSiteMetadata(request: GetSiteMetadataRequest): GetSiteMetadataResponse =
    //    retryOnError { api.getSiteMetadata(request.toForm()) }

    open suspend fun getUnreadCount(request: GetUnreadCountRequest = GetUnreadCountRequest()): GetUnreadCountResponse =
        retryOnError { api.getUnreadCount(request.toForm()) }

    //open suspend fun getUnreadRegistrationApplicationCount(request: GetUnreadRegistrationApplicationCountRequest): GetUnreadRegistrationApplicationCountResponse =
    //    retryOnError { api.getUnreadRegistrationApplicationCount(request.toForm()) }

    //open suspend fun leaveAdmin(request: LeaveAdminRequest): GetSiteResponse =
    //    retryOnError { api.leaveAdmin(request) }

    //open suspend fun likeComment(request: CreateCommentLikeRequest): CommentResponse =
    //    retryOnError { api.likeComment(request) }

    //open suspend fun likePost(request: CreatePostLikeRequest): PostResponse =
    //    retryOnError { api.likePost(request) }

    //open suspend fun listCommentReports(request: ListCommentReportsRequest): ListCommentReportsResponse =
    //    retryOnError { api.listCommentReports(request.toForm()) }

    open suspend fun listCommunities(request: ListCommunitiesRequest): ListCommunitiesResponse =
        retryOnError { api.listCommunities(request.toForm()) }

    //open suspend fun listPostReports(request: ListPostReportsRequest): ListPostReportsResponse =
    //    retryOnError { api.listPostReports(request.toForm()) }

    //open suspend fun listPrivateMessageReports(request: ListPrivateMessageReportsRequest): ListPrivateMessageReportsResponse =
    //    retryOnError { api.listPrivateMessageReports(request.toForm()) }

    //open suspend fun listRegistrationApplications(request: ListRegistrationApplicationsRequest): ListRegistrationApplicationsResponse =
    //    retryOnError { api.listRegistrationApplications(request.toForm()) }

    //open suspend fun lockPost(request: LockPostRequest): PostResponse =
    //    retryOnError { api.lockPost(request) }

    open suspend fun login(request: LoginRequest): LoginResponse =
        retryOnError { api.login(request) }

    //open suspend fun markAllAsRead(request: MarkAllAsReadRequest): GetRepliesResponse =
    //    retryOnError { api.markAllAsRead(request) }

    //open suspend fun markCommentReplyAsRead(request: MarkCommentReplyAsReadRequest): CommentReplyResponse =
    //    retryOnError { api.markCommentReplyAsRead(request) }

    //open suspend fun markPersonMentionAsRead(request: MarkPersonMentionAsReadRequest): PersonMentionResponse =
    //    retryOnError { api.markPersonMentionAsRead(request) }

    //open suspend fun markPostAsRead(request: MarkPostAsReadRequest): PostResponse =
    //    retryOnError { api.markPostAsRead(request) }

    //open suspend fun markPrivateMessageAsRead(request: MarkPrivateMessageAsReadRequest): PrivateMessageResponse =
    //    retryOnError { api.markPrivateMessageAsRead(request) }

    //open suspend fun passwordChangeAfterReset(request: PasswordChangeAfterResetRequest): LoginResponse =
    //    retryOnError { api.passwordChangeAfterReset(request) }

    //open suspend fun passwordReset(request: PasswordResetRequest): Unit =
    //    retryOnError { api.passwordReset(request) }

    //open suspend fun purgeComment(request: PurgeCommentRequest): PurgeItemResponse =
    //    retryOnError { api.purgeComment(request) }

    //open suspend fun purgeCommunity(request: PurgeCommunityRequest): PurgeItemResponse =
    //    retryOnError { api.purgeCommunity(request) }

    //open suspend fun purgePerson(request: PurgePersonRequest): PurgeItemResponse =
    //    retryOnError { api.purgePerson(request) }

    //open suspend fun purgePost(request: PurgePostRequest): PurgeItemResponse =
    //    retryOnError { api.purgePost(request) }

    //open suspend fun register(request: RegisterRequest): LoginResponse =
    //    retryOnError { api.register(request) }

    //open suspend fun removeComment(request: RemoveCommentRequest): CommentResponse =
    //    retryOnError { api.removeComment(request) }

    //open suspend fun removeCommunity(request: RemoveCommunityRequest): CommunityResponse =
    //    retryOnError { api.removeCommunity(request) }

    //open suspend fun removePost(request: RemovePostRequest): PostResponse =
    //    retryOnError { api.removePost(request) }

    //open suspend fun resolveCommentReport(request: ResolveCommentReportRequest): CommentReportResponse =
    //    retryOnError { api.resolveCommentReport(request) }

    //open suspend fun resolveObject(request: ResolveObjectRequest): ResolveObjectResponse =
    //    retryOnError { api.resolveObject(request.toForm()) }

    //open suspend fun resolvePostReport(request: ResolvePostReportRequest): PostReportResponse =
    //    retryOnError { api.resolvePostReport(request) }

    //open suspend fun resolvePrivateMessageReport(request: ResolvePrivateMessageReportRequest): PrivateMessageReportResponse =
    //    retryOnError { api.resolvePrivateMessageReport(request) }

    //open suspend fun saveComment(request: SaveCommentRequest): CommentResponse =
    //    retryOnError { api.saveComment(request) }

    //open suspend fun savePost(request: SavePostRequest): PostResponse =
    //    retryOnError { api.savePost(request) }

    //open suspend fun saveUserSettings(request: SaveUserSettingsRequest): LoginResponse =
    //    retryOnError { api.saveUserSettings(request) }

    //open suspend fun search(request: SearchRequest): SearchResponse =
    //    retryOnError { api.search(request.toForm()) }

    //open suspend fun transferCommunity(request: TransferCommunityRequest): GetCommunityResponse =
    //    retryOnError { api.transferCommunity(request) }

    open suspend fun uploadImage(request: UploadImageRequest): UploadImageResponse =
        retryOnError { api.uploadImage(pictrs,
            HttpCookie("jwt", request.auth!!).toString(),
            request.image.let {
                MultipartBody.Part.createFormData(
                    "images[]", request.filename, request.image.toRequestBody()
                )
            }) }

    //open suspend fun verifyEmail(request: VerifyEmailRequest): Unit =
    //    retryOnError { api.verifyEmail(request) }


    protected open suspend fun <T> retryOnError(block: suspend () -> Response<T>): T {
        var currentDelay = RETRY_DELAY
        var currentRetry = MAX_RETRIES

        val call: suspend () -> T = {
            val response = block()
            if (BuildConfig.DEBUG) LogUtil.v("Response received: ${response.code()}")

            val path = response.raw().request.url.encodedPath

            if (!response.isSuccessful)
                throw ApiException(path, response.code(),
                    response.errorBody()?.string().orEmpty())

            response.body()!!
        }

        while (currentRetry > 0) {
            var exception = try {
                return call()
            } catch (e: Exception) {
                e.printStackTrace()
                e
            }

            when (exception) {
                is ApiException -> {
                    currentRetry -= 1
                }
                is ApiException.ServerException -> {
                    // lenient retry attempts
                }
                else -> {
                    throw exception
                }
            }

            delay(currentDelay)
            currentDelay = (currentDelay * RETRY_BACKOFF).toLong()
                .coerceAtMost(DELAY_MAX)
        }

        return call()
    }

    private var client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            if (BuildConfig.DEBUG) LogUtil.v("OkHttp: ${request.method} ${request.url}")
            val reqBuilder = request.newBuilder()
                .headers(headers.toHeaders())
                .header(
                    "User-Agent",
                    "android:ltd.ucode.slide:v" + BuildConfig.VERSION_NAME
                )
            val response = chain.proceed(reqBuilder.build())
            if (BuildConfig.DEBUG) LogUtil.v("OkHttp: ${request.method} ${request.url} returned ${response.code}")
            response
        }
        .build()

    private fun createApi(): ILemmyHttpApi {
        if (BuildConfig.DEBUG) LogUtil.v("Creating API Object")

        val retrofit = Retrofit.Builder()
            .baseUrl("https://${instance}/api/v3/")
            .client(client)
            .addConverterFactory(SnakeCaseSerializer.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(ILemmyHttpApi::class.java)
    }


    private inline fun <reified T> T.toForm(): Map<String, String> {
        return Json { encodeDefaults = true }
            .encodeToJsonElement(this)
            .jsonObject
            .toMap()
            .mapValues { (_, element) -> when (element) {
                is JsonNull -> null
                is JsonPrimitive -> if (element.isString) element.content else TODO()
                is JsonArray -> TODO()
                is JsonObject -> TODO()
            } }
            .filterNotNullValues()
    }
}
