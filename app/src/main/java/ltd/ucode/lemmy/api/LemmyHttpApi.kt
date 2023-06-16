package ltd.ucode.lemmy.api

import ltd.ucode.lemmy.api.request.*
import ltd.ucode.lemmy.api.response.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.QueryMap

interface LemmyHttpApi {

    @GET("/.well-known/nodeinfo")
    suspend fun nodeinfo(): Response<String>

    @POST("user/login")
    suspend fun login(@Body req: LoginRequest): Response<LoginResponse>

    @GET("site")
    suspend fun getSite(@QueryMap form: Map<String, String>): Response<GetSiteResponse>

}
