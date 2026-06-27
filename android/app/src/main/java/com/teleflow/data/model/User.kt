package com.teleflow.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val photoUrl: String = "",
    val isPremium: Boolean = false,
    val subscriptionExpiresAt: Long = 0L,
    val authToken: String = "",
    val tokenExpiresAt: Long = 0L
)

@Serializable
data class AuthRequest(
    val id: Long,
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val authDate: Long = 0,
    val hash: String = ""
)

@Serializable
data class AuthResponse(
    val token: String,
    val expiresAt: Long,
    val user: User
)

@Serializable
data class PremiumStatus(
    val isPremium: Boolean,
    val expiresAt: Long = 0L
)

@Serializable
data class InitAuthResponse(
    val code: String
)

@Serializable
data class PendingAuthResponse(
    val status: String,
    val token: String? = null,
    val user: User? = null,
    val error: String? = null
)

@Serializable
data class ServerConfig(
    val proxies: List<ProxyServer>,
    val defaultProxy: String,
    val token: String,
    val expiresAt: Long
)
