package ltd.ucode.lemmy.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import ltd.ucode.Util.SnakeCaseSerializer
import ltd.ucode.Util.filterNotNullValues
import ltd.ucode.lemmy.api.request.GetSiteRequest
import ltd.ucode.lemmy.api.response.GetSiteResponse
import ltd.ucode.lemmy.data.type.NodeInfoResult
import ltd.ucode.lemmy.data.type.webfinger.Resource
import ltd.ucode.lemmy.data.type.webfinger.Uri
import ltd.ucode.slide.BuildConfig
import me.ccrama.redditslide.util.LogUtil
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit

const val MAX_RETRIES: Long = 3
const val RETRY_DELAY: Long = 3
const val SECONDS: Long = 1000

open class InstanceDataSource(
    val instance: String,
    protected val headers: Map<String, String> = mapOf(),
) {
    val nodeInfo: Flow<NodeInfoResult> =
        flow<Resource> { api.nodeInfo().unwrap() }
            .let {
                it.combine(
                    it
                        .map { resource -> resource.links.first().href }
                        .map { url: Uri -> api.nodeInfo20(url).unwrap() },
                ::NodeInfoResult)
            }
            .retry(MAX_RETRIES, ::retryOnApiException)

    open fun site(request: GetSiteRequest): Flow<GetSiteResponse> =
        flow<GetSiteResponse> { api.getSite(request.toForm()).unwrap() }
            .retry(MAX_RETRIES, ::retryOnApiException)


    private suspend fun retryOnApiException(cause: Throwable): Boolean {
        return if (cause is ApiException) {
            cause.printStackTrace()
            delay(RETRY_DELAY * SECONDS)
            true
        } else {
            false
        }
    }


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

    private fun createApi(): ILemmyHttpApi {
        if (BuildConfig.DEBUG) LogUtil.v("Creating API Object")

        val retrofit = Retrofit.Builder()
            .baseUrl("https://${instance}/api/v3/")
            .client(client)
            .addConverterFactory(SnakeCaseSerializer.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(ILemmyHttpApi::class.java)
    }


    protected inline fun <reified T> T.toForm(): Map<String, String> {
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

    protected fun <T> Response<T>.unwrap(): T {
        if (BuildConfig.DEBUG) LogUtil.v("Response received: ${this.code()}")

        if (this.isSuccessful)
            return this.body()!!
        else {
            val path = this.raw().request.url.encodedPath
            throw ApiException(path, this.code(), this.errorBody()?.string().orEmpty())
        }
    }
}
