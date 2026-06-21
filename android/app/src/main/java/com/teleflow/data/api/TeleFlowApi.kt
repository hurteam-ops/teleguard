package com.teleflow.data.api

import com.teleflow.data.model.AuthRequest
import com.teleflow.data.model.AuthResponse
import com.teleflow.data.model.PremiumStatus
import com.teleflow.data.model.ServerConfig
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface TeleFlowApi {

    @POST("api/v1/auth/tg")
    suspend fun authenticate(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/v1/auth/check-premium")
    suspend fun checkPremium(
        @Header("Authorization") token: String
    ): Response<PremiumStatus>

    @GET("api/v1/config")
    suspend fun getConfig(
        @Header("Authorization") token: String
    ): Response<ServerConfig>

    @GET("api/v1/proxies")
    suspend fun getProxies(
        @Header("Authorization") token: String
    ): Response<List<com.teleflow.data.model.ProxyServer>>

    @GET("api/v1/health")
    suspend fun health(): Response<Unit>
}
