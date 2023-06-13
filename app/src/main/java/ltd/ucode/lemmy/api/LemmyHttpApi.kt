package ltd.ucode.lemmy.api

import ltd.ucode.lemmy.api.request.*
import ltd.ucode.lemmy.api.response.*
import retrofit2.http.POST

interface LemmyHttpApi {

    @POST("/user/login")
    suspend fun login(req: LoginRequest): LoginResponse

}
