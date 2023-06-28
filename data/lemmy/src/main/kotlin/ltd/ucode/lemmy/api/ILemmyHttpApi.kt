package ltd.ucode.lemmy.api

import ltd.ucode.lemmy.api.request.*
import ltd.ucode.lemmy.api.response.*
import ltd.ucode.lemmy.data.type.webfinger.NodeInfo
import ltd.ucode.lemmy.data.type.webfinger.Resource
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface ILemmyHttpApi {

    @GET("/.well-known/nodeinfo")
    suspend fun nodeInfo(): Response<Resource>

    @GET
    suspend fun nodeInfo20(@Url url: String): Response<NodeInfo>

    @POST("user/login")
    suspend fun login(@Body req: LoginRequest): Response<LoginResponse>

    // Generated

    /* Add an admin to your site. */
    //@POST("admin/add")
    //suspend fun addAdmin(@QueryMap form: Map<String, String>): Response<AddAdminResponse> // AddAdminRequest

    /* Add a moderator to your community. */
    //@POST("community/mod")
    //suspend fun addModToCommunity(@QueryMap form: Map<String, String>): Response<AddModToCommunityResponse> // AddModToCommunityRequest

    /* Approve a registration application */
    //@PUT("admin/registration_application/approve")
    //suspend fun approveRegistrationApplication(@QueryMap form: Map<String, String>): Response<RegistrationApplicationResponse> // ApproveRegistrationApplicationRequest

    /* Ban a user from a community. */
    //@POST("community/ban_user")
    //suspend fun banFromCommunity(@QueryMap form: Map<String, String>): Response<BanFromCommunityResponse> // BanFromCommunityRequest

    /* Ban a person from your site. */
    //@POST("user/ban")
    //suspend fun banPerson(@QueryMap form: Map<String, String>): Response<BanPersonResponse> // BanPersonRequest

    /* Block a community. */
    //@POST("community/block")
    //suspend fun blockCommunity(@QueryMap form: Map<String, String>): Response<BlockCommunityResponse> // BlockCommunityRequest

    /* Block a person. */
    //@POST("user/block")
    //suspend fun blockPerson(@QueryMap form: Map<String, String>): Response<BlockPersonResponse> // BlockPersonRequest

    /* Change your user password. */
    @PUT("user/change_password")
    suspend fun changePassword(@QueryMap form: Map<String, String>): Response<LoginResponse> // ChangePasswordRequest

    /* Create a comment. */
    //@POST("comment")
    //suspend fun createComment(@QueryMap form: Map<String, String>): Response<CommentResponse> // CreateCommentRequest

    /* Report a comment. */
    //@POST("comment/report")
    //suspend fun createCommentReport(@QueryMap form: Map<String, String>): Response<CommentReportResponse> // CreateCommentReportRequest

    /* Create a new community. */
    //@POST("community")
    //suspend fun createCommunity(@QueryMap form: Map<String, String>): Response<CommunityResponse> // CreateCommunityRequest

    /* Create a new custom emoji */
    //@POST("custom_emoji")
    //suspend fun createCustomEmoji(@QueryMap form: Map<String, String>): Response<CustomEmojiResponse> // CreateCustomEmojiRequest

    /* Create a post. */
    //@POST("post")
    //suspend fun createPost(@QueryMap form: Map<String, String>): Response<PostResponse> // CreatePostRequest

    /* Report a post. */
    //@POST("post/report")
    //suspend fun createPostReport(@QueryMap form: Map<String, String>): Response<PostReportResponse> // CreatePostReportRequest

    /* Create a private message. */
    //@POST("private_message")
    //suspend fun createPrivateMessage(@QueryMap form: Map<String, String>): Response<PrivateMessageResponse> // CreatePrivateMessageRequest

    /* Create a report for a private message. */
    //@POST("private_message/report")
    //suspend fun createPrivateMessageReport(@QueryMap form: Map<String, String>): Response<PrivateMessageReportResponse> // CreatePrivateMessageReportRequest

    /* Create your site. */
    //@POST("site")
    //suspend fun createSite(@QueryMap form: Map<String, String>): Response<SiteResponse> // CreateSiteRequest

    /* Delete your account. */
    @POST("user/delete_account")
    suspend fun deleteAccount(@QueryMap form: Map<String, String>): Response<Unit> // DeleteAccountRequest

    /* Delete a comment. */
    //@POST("comment/delete")
    //suspend fun deleteComment(@QueryMap form: Map<String, String>): Response<CommentResponse> // DeleteCommentRequest

    /* Delete a community. */
    //@POST("community/delete")
    //suspend fun deleteCommunity(@QueryMap form: Map<String, String>): Response<CommunityResponse> // DeleteCommunityRequest

    /* Delete a custom emoji */
    //@POST("custom_emoji/delete")
    //suspend fun deleteCustomEmoji(@QueryMap form: Map<String, String>): Response<DeleteCustomEmojiResponse> // DeleteCustomEmojiRequest

    /* Delete a post. */
    //@POST("post/delete")
    //suspend fun deletePost(@QueryMap form: Map<String, String>): Response<PostResponse> // DeletePostRequest

    /* Delete a private message. */
    //@POST("private_message/delete")
    //suspend fun deletePrivateMessage(@QueryMap form: Map<String, String>): Response<PrivateMessageResponse> // DeletePrivateMessageRequest

    /* Distinguishes a comment (speak as moderator) */
    //@POST("comment/distinguish")
    //suspend fun distinguishComment(@QueryMap form: Map<String, String>): Response<CommentResponse> // DistinguishCommentRequest

    /* Edit a comment. */
    //@PUT("comment")
    //suspend fun editComment(@QueryMap form: Map<String, String>): Response<CommentResponse> // EditCommentRequest

    /* Edit a community. */
    //@PUT("community")
    //suspend fun editCommunity(@QueryMap form: Map<String, String>): Response<CommunityResponse> // EditCommunityRequest

    /* Edit an existing custom emoji */
    //@PUT("custom_emoji")
    //suspend fun editCustomEmoji(@QueryMap form: Map<String, String>): Response<CustomEmojiResponse> // EditCustomEmojiRequest

    /* Edit a post. */
    //@PUT("post")
    //suspend fun editPost(@QueryMap form: Map<String, String>): Response<PostResponse> // EditPostRequest

    /* Edit a private message. */
    //@PUT("private_message")
    //suspend fun editPrivateMessage(@QueryMap form: Map<String, String>): Response<PrivateMessageResponse> // EditPrivateMessageRequest

    /* Edit your site. */
    //@PUT("site")
    //suspend fun editSite(@QueryMap form: Map<String, String>): Response<SiteResponse> // EditSiteRequest

    /* A moderator can feature a community post ( IE stick it to the top of a community ). */
    //@POST("post/feature")
    //suspend fun featurePost(@QueryMap form: Map<String, String>): Response<PostResponse> // FeaturePostRequest

    /* Follow / subscribe to a community. */
    //@POST("community/follow")
    //suspend fun followCommunity(@QueryMap form: Map<String, String>): Response<CommunityResponse> // FollowCommunityRequest

    /* Get a list of banned users */
    //@GET("user/banned")
    //suspend fun getBannedPersons(@QueryMap form: Map<String, String>): Response<BannedPersonsResponse> // GetBannedPersonsRequest

    /* Fetch a Captcha. */
    //@GET("user/get_captcha")
    //suspend fun getCaptcha(@QueryMap form: Map<String, String>): Response<GetCaptchaResponse> // GetCaptchaRequest

    /* Get / fetch comment. */
    //@GET("comment")
    //suspend fun getComment(@QueryMap form: Map<String, String>): Response<CommentResponse> // GetCommentRequest

    /* Get / fetch comments. */
    @GET("comment/list")
    suspend fun getComments(@QueryMap form: Map<String, String>): Response<GetCommentsResponse> // GetCommentsRequest

    /* Get / fetch a community. */
    @GET("community")
    suspend fun getCommunity(@QueryMap form: Map<String, String>): Response<GetCommunityResponse> // GetCommunityRequest

    /* Fetch federated instances. */
    @GET("federated_instances")
    suspend fun getFederatedInstances(@QueryMap form: Map<String, String>): Response<GetFederatedInstancesResponse> // GetFederatedInstancesRequest

    /* Get the modlog. */
    //@GET("modlog")
    //suspend fun getModlog(@QueryMap form: Map<String, String>): Response<GetModlogResponse> // GetModlogRequest

    /* Get the details for a person. */
    @GET("user")
    suspend fun getPersonDetails(@QueryMap form: Map<String, String>): Response<GetPersonDetailsResponse> // GetPersonDetailsRequest

    /* Get mentions for your user. */
    //@GET("user/mention")
    //suspend fun getPersonMentions(@QueryMap form: Map<String, String>): Response<GetPersonMentionsResponse> // GetPersonMentionsRequest

    /* Get / fetch a post. */
    @GET("post")
    suspend fun getPost(@QueryMap form: Map<String, String>): Response<GetPostResponse> // GetPostRequest

    /* Get / fetch posts, with various filters. */
    @GET("post/list")
    suspend fun getPosts(@QueryMap form: Map<String, String>): Response<GetPostsResponse> // GetPostsRequest

    /* Get / fetch private messages. */
    //@GET("private_message/list")
    //suspend fun getPrivateMessages(@QueryMap form: Map<String, String>): Response<PrivateMessagesResponse> // GetPrivateMessagesRequest

    /* Get comment replies. */
    //@GET("user/replies")
    //suspend fun getReplies(@QueryMap form: Map<String, String>): Response<GetRepliesResponse> // GetRepliesRequest

    /* Get counts for your reports */
    //@GET("user/report_count")
    //suspend fun getReportCount(@QueryMap form: Map<String, String>): Response<GetReportCountResponse> // GetReportCountRequest

    /* Gets the site, and your user data. */
    @GET("site")
    suspend fun getSite(@QueryMap form: Map<String, String>): Response<GetSiteResponse> // GetSiteRequest

    /* Fetch metadata for any given site. */
    //@GET("post/site_metadata")
    //suspend fun getSiteMetadata(@QueryMap form: Map<String, String>): Response<GetSiteMetadataResponse> // GetSiteMetadataRequest

    /* Get your unread counts */
    @GET("user/unread_count")
    suspend fun getUnreadCount(@QueryMap form: Map<String, String>): Response<GetUnreadCountResponse> // GetUnreadCountRequest

    /* Get the unread registration applications count. */
    //@GET("admin/registration_application/count")
    //suspend fun getUnreadRegistrationApplicationCount(@QueryMap form: Map<String, String>): Response<GetUnreadRegistrationApplicationCountResponse> // GetUnreadRegistrationApplicationCountRequest

    /* Leave the Site admins. */
    @POST("user/leave_admin")
    suspend fun leaveAdmin(@QueryMap form: Map<String, String>): Response<GetSiteResponse> // LeaveAdminRequest

    /* Like / vote on a comment. */
    //@POST("comment/like")
    //suspend fun likeComment(@QueryMap form: Map<String, String>): Response<CommentResponse> // CreateCommentLikeRequest

    /* Like / vote on a post. */
    //@POST("post/like")
    //suspend fun likePost(@QueryMap form: Map<String, String>): Response<PostResponse> // CreatePostLikeRequest

    /* List comment reports. */
    //@GET("comment/report/list")
    //suspend fun listCommentReports(@QueryMap form: Map<String, String>): Response<ListCommentReportsResponse> // ListCommentReportsRequest

    /* List communities, with various filters. */
    @GET("community/list")
    suspend fun listCommunities(@QueryMap form: Map<String, String>): Response<ListCommunitiesResponse> // ListCommunitiesRequest

    /* List post reports. */
    //@GET("post/report/list")
    //suspend fun listPostReports(@QueryMap form: Map<String, String>): Response<ListPostReportsResponse> // ListPostReportsRequest

    /* List private message reports. */
    //@GET("private_message/report/list")
    //suspend fun listPrivateMessageReports(@QueryMap form: Map<String, String>): Response<ListPrivateMessageReportsResponse> // ListPrivateMessageReportsRequest

    /* List the registration applications. */
    //@GET("admin/registration_application/list")
    //suspend fun listRegistrationApplications(@QueryMap form: Map<String, String>): Response<ListRegistrationApplicationsResponse> // ListRegistrationApplicationsRequest

    /* A moderator can lock a post ( IE disable new comments ). */
    //@POST("post/lock")
    //suspend fun lockPost(@QueryMap form: Map<String, String>): Response<PostResponse> // LockPostRequest

    /* Log into lemmy. */
    @POST("user/login")
    suspend fun login(@QueryMap form: Map<String, String>): Response<LoginResponse> // LoginRequest

    /* Mark all replies as read. */
    //@POST("user/mark_all_as_read")
    //suspend fun markAllAsRead(@QueryMap form: Map<String, String>): Response<GetRepliesResponse> // MarkAllAsReadRequest

    /* Mark a comment as read. */
    //@POST("comment/mark_as_read")
    //suspend fun markCommentReplyAsRead(@QueryMap form: Map<String, String>): Response<CommentReplyResponse> // MarkCommentReplyAsReadRequest

    /* Mark a person mention as read. */
    //@POST("user/mention/mark_as_read")
    //suspend fun markPersonMentionAsRead(@QueryMap form: Map<String, String>): Response<PersonMentionResponse> // MarkPersonMentionAsReadRequest

    /* Mark a post as read. */
    //@POST("post/mark_as_read")
    //suspend fun markPostAsRead(@QueryMap form: Map<String, String>): Response<PostResponse> // MarkPostAsReadRequest

    /* Mark a private message as read. */
    //@POST("private_message/mark_as_read")
    //suspend fun markPrivateMessageAsRead(@QueryMap form: Map<String, String>): Response<PrivateMessageResponse> // MarkPrivateMessageAsReadRequest

    /* Change your password from an email / token based reset. */
    @POST("user/password_change")
    suspend fun passwordChangeAfterReset(@QueryMap form: Map<String, String>): Response<LoginResponse> // PasswordChangeAfterResetRequest

    /* Reset your password. */
    @POST("user/password_reset")
    suspend fun passwordReset(@QueryMap form: Map<String, String>): Response<Unit> // PasswordResetRequest

    /* Purge / Delete a comment from the database. */
    //@POST("admin/purge/comment")
    //suspend fun purgeComment(@QueryMap form: Map<String, String>): Response<PurgeItemResponse> // PurgeCommentRequest

    /* Purge / Delete a community from the database. */
    //@POST("admin/purge/community")
    //suspend fun purgeCommunity(@QueryMap form: Map<String, String>): Response<PurgeItemResponse> // PurgeCommunityRequest

    /* Purge / Delete a person from the database. */
    //@POST("admin/purge/person")
    //suspend fun purgePerson(@QueryMap form: Map<String, String>): Response<PurgeItemResponse> // PurgePersonRequest

    /* Purge / Delete a post from the database. */
    //@POST("admin/purge/post")
    //suspend fun purgePost(@QueryMap form: Map<String, String>): Response<PurgeItemResponse> // PurgePostRequest

    /* Register a new user. */
    @POST("user/register")
    suspend fun register(@QueryMap form: Map<String, String>): Response<LoginResponse> // RegisterRequest

    /* A moderator remove for a comment. */
    //@POST("comment/remove")
    //suspend fun removeComment(@QueryMap form: Map<String, String>): Response<CommentResponse> // RemoveCommentRequest

    /* A moderator remove for a community. */
    //@POST("community/remove")
    //suspend fun removeCommunity(@QueryMap form: Map<String, String>): Response<CommunityResponse> // RemoveCommunityRequest

    /* A moderator remove for a post. */
    //@POST("post/remove")
    //suspend fun removePost(@QueryMap form: Map<String, String>): Response<PostResponse> // RemovePostRequest

    /* Resolve a comment report. Only a mod can do this. */
    //@PUT("comment/report/resolve")
    //suspend fun resolveCommentReport(@QueryMap form: Map<String, String>): Response<CommentReportResponse> // ResolveCommentReportRequest

    /* Fetch a non-local / federated object. */
    //@GET("resolve_object")
    //suspend fun resolveObject(@QueryMap form: Map<String, String>): Response<ResolveObjectResponse> // ResolveObjectRequest

    /* Resolve a post report. Only a mod can do this. */
    //@PUT("post/report/resolve")
    //suspend fun resolvePostReport(@QueryMap form: Map<String, String>): Response<PostReportResponse> // ResolvePostReportRequest

    /* Resolve a report for a private message. */
    //@PUT("private_message/report/resolve")
    //suspend fun resolvePrivateMessageReport(@QueryMap form: Map<String, String>): Response<PrivateMessageReportResponse> // ResolvePrivateMessageReportRequest

    /* Save a comment. */
    //@PUT("comment/save")
    //suspend fun saveComment(@QueryMap form: Map<String, String>): Response<CommentResponse> // SaveCommentRequest

    /* Save a post. */
    //@PUT("post/save")
    //suspend fun savePost(@QueryMap form: Map<String, String>): Response<PostResponse> // SavePostRequest

    /* Save your user settings. */
    @PUT("user/save_user_settings")
    suspend fun saveUserSettings(@QueryMap form: Map<String, String>): Response<LoginResponse> // SaveUserSettingsRequest

    /* Search lemmy. */
    //@GET("search")
    //suspend fun search(@QueryMap form: Map<String, String>): Response<SearchResponse> // SearchRequest

    /* Transfer your community to an existing moderator. */
    @POST("community/transfer")
    suspend fun transferCommunity(@QueryMap form: Map<String, String>): Response<GetCommunityResponse> // TransferCommunityRequest

    /* Upload an image to the server. */
    @Multipart @POST
    suspend fun uploadImage(@Url url: String, @Header("Cookie") cookie: String, @Part image: MultipartBody.Part): Response<UploadImageResponse> // UploadImageRequest

    /* Verify your email */
    @POST("user/verify_email")
    suspend fun verifyEmail(@QueryMap form: Map<String, String>): Response<Unit> // VerifyEmailRequest
}
