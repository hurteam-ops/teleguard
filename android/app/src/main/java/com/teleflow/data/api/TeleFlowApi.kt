package com.teleflow.data.api

import com.teleflow.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface TeleFlowApi {

    @POST("api/v1/auth/tg")
    suspend fun authenticate(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/v1/auth/check-premium")
    suspend fun checkPremium(
        @Header("Authorization") token: String
    ): Response<PremiumStatus>

    @POST("api/v1/auth/init")
    suspend fun initAuth(): Response<InitAuthResponse>

    @GET("api/v1/auth/pending/{code}")
    suspend fun checkPendingAuth(
        @Path("code") code: String
    ): Response<PendingAuthResponse>

    @GET("api/v1/config")
    suspend fun getConfig(
        @Header("Authorization") token: String
    ): Response<ServerConfig>

    @GET("api/v1/proxies")
    suspend fun getProxies(
        @Header("Authorization") token: String
    ): Response<List<ProxyServer>>

    @GET("api/v1/health")
    suspend fun health(): Response<Unit>
}
