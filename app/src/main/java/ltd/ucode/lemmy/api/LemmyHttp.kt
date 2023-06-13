package ltd.ucode.lemmy.api

import ltd.ucode.lemmy.data.LoginResult
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import ltd.ucode.lemmy.api.request.LoginRequest
import ltd.ucode.slide.BuildConfig
import okhttp3.Interceptor
import retrofit2.Retrofit
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Response

class LemmyHttp(val apiUrl: String, val headers: Map<String, String> = mapOf()) {
    private val api: LemmyHttpApi by lazy {
        createApi()
    }

    private fun createApi(): LemmyHttpApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(object: Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val request = chain.request()
                    val reqBuilder = request.newBuilder()
                        .header("User-Agent",
                            "android:ltd.ucode.slide:v" + BuildConfig.VERSION_NAME)
                    return chain.proceed(reqBuilder.build())
                }
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(apiUrl)
            .client(client)
            .addConverterFactory(Json.asConverterFactory(MediaType.get("application/json")))
            .build()

        return retrofit.create(LemmyHttpApi::class.java)
    }

    suspend fun login(user: String, password: String): LoginResult {
        val response = api.login(LoginRequest(user, password))

        if (response.verifyEmailSent) {
            return LoginResult.EmailNotVerified
        } else if (response.registrationCreated) {
            return LoginResult.WaitApproval
        } else if (response.jwt.isNullOrBlank()) {
            return LoginResult.Failure
        }

        return LoginResult.Success(response.jwt)
    }
}
