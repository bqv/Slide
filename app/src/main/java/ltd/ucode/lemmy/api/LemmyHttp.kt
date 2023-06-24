package ltd.ucode.lemmy.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import ltd.ucode.Util.SnakeCaseSerializer
import ltd.ucode.lemmy.api.iter.PagedData
import ltd.ucode.lemmy.api.request.GetCommentsRequest
import ltd.ucode.lemmy.api.request.GetPersonDetailsRequest
import ltd.ucode.lemmy.api.request.GetPostRequest
import ltd.ucode.lemmy.api.request.GetPostsRequest
import ltd.ucode.lemmy.api.request.GetSiteRequest
import ltd.ucode.lemmy.api.request.ListCommunitiesRequest
import ltd.ucode.lemmy.api.request.LoginRequest
import ltd.ucode.lemmy.api.response.GetPersonDetailsResponse
import ltd.ucode.lemmy.api.response.GetPostResponse
import ltd.ucode.lemmy.api.response.GetSiteResponse
import ltd.ucode.lemmy.data.LoginResult
import ltd.ucode.lemmy.data.type.CommentId
import ltd.ucode.lemmy.data.type.CommentListingType
import ltd.ucode.lemmy.data.type.CommentSortType
import ltd.ucode.lemmy.data.type.CommentView
import ltd.ucode.lemmy.data.type.CommunityId
import ltd.ucode.lemmy.data.type.CommunityView
import ltd.ucode.lemmy.data.type.NodeInfoResult
import ltd.ucode.lemmy.data.type.PersonId
import ltd.ucode.lemmy.data.type.PostId
import ltd.ucode.lemmy.data.type.PostListingType
import ltd.ucode.lemmy.data.type.PostSortType
import ltd.ucode.lemmy.data.type.PostView
import ltd.ucode.slide.BuildConfig
import me.ccrama.redditslide.util.LogUtil
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit

class LemmyHttp(val instance: String = "lemmy.ml",
                private val headers: Map<String, String> = mapOf()) {
    private val api: ILemmyHttpApi by lazy { createApi() }

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

    var retryLimit: Int = -1 // TODO: use

    private fun createApi(): ILemmyHttpApi {
        if (BuildConfig.DEBUG) LogUtil.v("Creating API Object")

        val retrofit = Retrofit.Builder()
            .baseUrl("https://${instance}/api/v3/")
            .client(client)
            .addConverterFactory(SnakeCaseSerializer.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(ILemmyHttpApi::class.java)
    }

    suspend fun nodeInfo(): NodeInfoResult? {
        val response = api.nodeInfo().unwrap()

        val url = response.links.firstOrNull()?.href

        return url?.let { api.nodeInfo20(it) }?.unwrap()
            ?.let { NodeInfoResult(response, it) }
    }

    suspend fun login(user: String, password: String): LoginResult {
        val response = api.login(LoginRequest(
            usernameOrEmail = user,
            password = password
        )).unwrap()

        return response.toResult()
    }

    suspend fun getSite(auth: String? = null): GetSiteResponse {
        val response = api.getSite(GetSiteRequest(
            auth = auth
        ).toForm()).unwrap()

        return response.toResult()
    }

    fun getPosts(auth: String? = null,
                 communityId: CommunityId? = null,
                 communityName: String? = null, // community, or community@instance.tld
                 limit: Int? = null,
                 fromPage: Int? = null,
                 savedOnly: Boolean? = null,
                 sort: PostSortType? = null,
                 type: PostListingType? = null
    ): PagedData<PostView> {
        return PagedData(fromPage ?: 1) { page: Int -> {
                val response = api.getPosts(
                    GetPostsRequest(
                        auth = auth,
                        communityId = communityId,
                        communityName = communityName,
                        limit = limit,
                        page = page,
                        savedOnly = savedOnly,
                        sort = sort,
                        type = type
                    ).toForm()
                ).unwrap()

                response.toResult()
            }
        }
    }

    suspend fun getPost(auth: String? = null,
                        id: PostId? = null,
                        commentId: CommentId? = null
    ): GetPostResponse {
        val response = api.getPost(GetPostRequest(
            auth = auth,
            id = id,
            commentId = commentId
        ).toForm()).unwrap()

        return response.toResult()
    }

    fun getComments(auth: String? = null,
                    communityId: CommunityId? = null,
                    communityName: String? = null, // community, or community@instance.tld
                    parentId: CommentId? = null,
                    postId: PostId? = null,
                    maxDepth: Int? = null,
                    limit: Int? = null,
                    fromPage: Int? = null,
                    savedOnly: Boolean? = null,
                    sort: CommentSortType? = null,
                    type: CommentListingType? = null
    ): PagedData<CommentView> {
        return PagedData(fromPage ?: 1) { page: Int -> {
                val response = api.getComments(
                    GetCommentsRequest(
                        auth = auth,
                        communityId = communityId,
                        communityName = communityName,
                        parentId = parentId,
                        postId = postId,
                        maxDepth = maxDepth,
                        limit = limit,
                        page = page,
                        savedOnly = savedOnly,
                        sort = sort,
                        type = type
                    ).toForm()
                ).unwrap()

                response.toResult()
            }
        }
    }

    suspend fun listCommunities(auth: String? = null,
                                limit: Int? = null, // <= 50
                                page: Int? = null,
                                sort: PostSortType? = null,
                                type: PostListingType? = null
    ): List<CommunityView> {
        val response = api.listCommunities(ListCommunitiesRequest(
            auth = auth,
            limit = limit,
            page = page,
            sort = sort,
            type = type
        ).toForm()).unwrap()

        return response.toResult()
    }

    suspend fun getPersonDetails(auth: String? = null,
                                 communityId: CommunityId? = null,
                                 limit: Int? = null,
                                 page: Int? = null,
                                 personId: PersonId? = null,
                                 savedOnly: Boolean? = null,
                                 sort: PostSortType? = null,
                                 username: String? = null
    ): GetPersonDetailsResponse {
        val response = api.getPersonDetails(GetPersonDetailsRequest(
            auth = auth,
            communityId = communityId,
            limit = limit,
            page = page,
            personId = personId,
            savedOnly = savedOnly,
            sort = sort,
            username = username
        ).toForm()).unwrap()

        return response.toResult()
    }
}

private fun <T> Response<T>.unwrap(): T {
    if (BuildConfig.DEBUG) LogUtil.v("Response received: ${this.code()}")

    if (this.isSuccessful)
        return this.body()!!
    else {
        val path = this.raw().request.url.encodedPath
        throw ApiException(path, this.code(), this.errorBody()?.string().orEmpty())
    }
}
