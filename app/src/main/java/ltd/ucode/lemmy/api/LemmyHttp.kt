package ltd.ucode.lemmy.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import ltd.ucode.Util.filterNotNullValues
import ltd.ucode.lemmy.api.iter.PagedData
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
import ltd.ucode.lemmy.data.type.CommunityId
import ltd.ucode.lemmy.data.type.CommunityView
import ltd.ucode.lemmy.data.type.ListingType
import ltd.ucode.lemmy.data.type.NodeInfoResult
import ltd.ucode.lemmy.data.type.PersonId
import ltd.ucode.lemmy.data.type.PostId
import ltd.ucode.lemmy.data.type.PostView
import ltd.ucode.lemmy.data.type.SortType
import ltd.ucode.slide.BuildConfig
import me.ccrama.redditslide.util.LogUtil
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit

class LemmyHttp(val instance: String = "lemmy.ml",
                private val headers: Map<String, String> = mapOf()) {
    private val api: LemmyHttpApi by lazy { createApi() }

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

    private fun createApi(): LemmyHttpApi {
        if (BuildConfig.DEBUG) LogUtil.v("Creating API Object")

        val retrofit = Retrofit.Builder()
            .baseUrl("https://${instance}/api/v3/")
            .client(client)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(LemmyHttpApi::class.java)
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
                 sort: SortType? = null,
                 type: ListingType? = null
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

    suspend fun listCommunities(auth: String? = null,
                                limit: Int? = null, // <= 50
                                page: Int? = null,
                                sort: SortType? = null,
                                type: ListingType? = null
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
                                 sort: SortType? = null,
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

private inline fun <reified T> T.toForm(): Map<String, String> {
    return Json { encodeDefaults = true }
        .encodeToJsonElement(this)
        .jsonObject
        .toMap()
        .mapValues { (_, element) -> when (element) {
            is JsonNull -> null
            is JsonPrimitive -> element.content
            is JsonArray -> TODO()
            is JsonObject -> TODO()
        } }
        .filterNotNullValues()
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
