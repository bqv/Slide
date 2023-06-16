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
import ltd.ucode.lemmy.api.request.GetSiteRequest
import ltd.ucode.lemmy.api.request.LoginRequest
import ltd.ucode.lemmy.data.GetSiteResult
import ltd.ucode.lemmy.data.LoginResult
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

    var retryLimit: Int = -1 // TODO: use

    private fun createApi(): LemmyHttpApi {
        if (BuildConfig.DEBUG) LogUtil.v("Creating API Object")
        val client = OkHttpClient.Builder()
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

        val retrofit = Retrofit.Builder()
            .baseUrl("https://${instance}/api/v3/")
            .client(client)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(LemmyHttpApi::class.java)
    }

    suspend fun login(user: String, password: String): LoginResult {
        val response = api.login(LoginRequest(
            usernameOrEmail = user,
            password = password
        )).unwrap()

        return response.toResult()
    }

    suspend fun getSite(auth: String? = null): GetSiteResult {
        val response = api.getSite(GetSiteRequest(
            auth = auth
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
