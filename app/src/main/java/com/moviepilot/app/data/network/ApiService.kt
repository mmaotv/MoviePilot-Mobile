package com.moviepilot.app.data.network

import com.google.gson.JsonObject
import com.moviepilot.app.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 只保留 PWA 外壳需要的三个接口。
 *
 * 其余业务全部由网页版自己请求，App 不再维护一份 API 映射——
 * 这样服务器前端升级时，客户端不需要跟着改。
 */
interface ApiService {

    @POST("api/v1/login/access-token")
    @FormUrlEncoded
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("otp_password") otpPassword: String? = null
    ): LoginResponse

    /**
     * 当前登录用户。既用来校验本地 token 是否还有效，也用来还原网页版的
     * `user` store（superUser / permissions）——没有它，网页版根路由会把
     * 超管也送去「应用中心」，看不到主页。
     *
     * v2 / v3 对无效 token 都返回 401/403，有效则 200。
     */
    @GET("api/v1/user/current")
    suspend fun currentUser(): Response<JsonObject>

    /** 备用校验点，仅用于接口老版本没有 /user/current 的情况 */
    @GET("api/v1/system/global/user")
    suspend fun checkToken(): Response<JsonObject>
}
