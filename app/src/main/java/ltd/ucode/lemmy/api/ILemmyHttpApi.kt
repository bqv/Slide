package ltd.ucode.lemmy.api

import ltd.ucode.lemmy.api.request.*
import ltd.ucode.lemmy.api.response.*
import ltd.ucode.lemmy.data.type.webfinger.NodeInfo
import ltd.ucode.lemmy.data.type.webfinger.Resource
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface ILemmyHttpApi {

    @GET("/.well-known/nodeinfo")
    suspend fun nodeInfo(): Response<Resource>

    @GET
    suspend fun nodeInfo20(@Url url: String): Response<NodeInfo>

    @POST("user/login")
    suspend fun login(@Body req: LoginRequest): Response<LoginResponse>

    @GET("site")
    suspend fun getSite(@QueryMap form: Map<String, String>): Response<GetSiteResponse>

    @GET("post/list")
    suspend fun getPosts(@QueryMap form: Map<String, String>): Response<GetPostsResponse>

    @GET("post")
    suspend fun getPost(@QueryMap form: Map<String, String>): Response<GetPostResponse>

    @GET("comment/list")
    suspend fun getComments(@QueryMap form: Map<String, String>): Response<GetCommentsResponse>

    @GET("community/list")
    suspend fun listCommunities(@QueryMap form: Map<String, String>): Response<ListCommunitiesResponse>

    @GET("user")
    suspend fun getPersonDetails(@QueryMap form: Map<String, String>): Response<GetPersonDetailsResponse>

}
