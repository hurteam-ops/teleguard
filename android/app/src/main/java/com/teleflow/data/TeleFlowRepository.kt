package com.teleflow.data

import com.teleflow.data.api.ApiClient
import com.teleflow.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TeleFlowRepository(private val secureStorage: SecureStorage) {

    suspend fun authenticate(request: AuthRequest): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.api.authenticate(request)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                secureStorage.authToken = body.token
                secureStorage.tokenExpiresAt = body.expiresAt
                secureStorage.userId = body.user.id
                secureStorage.username = body.user.username
                secureStorage.isPremium = body.user.isPremium
                secureStorage.premiumExpiresAt = body.user.subscriptionExpiresAt
                Result.success(body)
            } else {
                Result.failure(Exception("Auth failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkPremiumStatus(): Result<PremiumStatus> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.api.checkPremium("Bearer ${secureStorage.authToken}")
            if (response.isSuccessful && response.body() != null) {
                val status = response.body()!!
                secureStorage.isPremium = status.isPremium
                secureStorage.premiumExpiresAt = status.expiresAt
                Result.success(status)
            } else {
                Result.failure(Exception("Premium check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConfig(): Result<ServerConfig> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.api.getConfig("Bearer ${secureStorage.authToken}")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Config fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProxies(): Result<List<ProxyServer>> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.api.getProxies("Bearer ${secureStorage.authToken}")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Proxy list fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun initAuth(): Result<InitAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.api.initAuth()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Auth init failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkPendingAuth(code: String): Result<PendingAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.api.checkPendingAuth(code)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Pending check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAuthToken(): String = secureStorage.authToken
    fun isAuthenticated(): Boolean = secureStorage.isAuthenticated
    fun isPremium(): Boolean = secureStorage.isPremium
    fun getUsername(): String = secureStorage.username
    fun getSelectedProxyId(): String = secureStorage.selectedProxyId
    fun setSelectedProxyId(id: String) { secureStorage.selectedProxyId = id }

    fun isKillSwitchEnabled(): Boolean = secureStorage.killSwitchEnabled
    fun setKillSwitchEnabled(enabled: Boolean) { secureStorage.killSwitchEnabled = enabled }

    fun isAutoConnectEnabled(): Boolean = secureStorage.autoConnectEnabled
    fun setAutoConnectEnabled(enabled: Boolean) { secureStorage.autoConnectEnabled = enabled }

    fun getDnsServer(): String = secureStorage.dnsServer
    fun setDnsServer(dns: String) { secureStorage.dnsServer = dns }

    fun getThemePreference(): String = secureStorage.themePreference
    fun setThemePreference(theme: String) { secureStorage.themePreference = theme }

    fun getProtocol(): String = secureStorage.protocol
    fun setProtocol(p: String) { secureStorage.protocol = p }

    fun isIpv6ProtectionEnabled(): Boolean = secureStorage.ipv6LeakProtection
    fun setIpv6ProtectionEnabled(enabled: Boolean) { secureStorage.ipv6LeakProtection = enabled }

    fun isConnectionAlertsEnabled(): Boolean = secureStorage.connectionAlerts
    fun setConnectionAlertsEnabled(enabled: Boolean) { secureStorage.connectionAlerts = enabled }

    fun getTotalSessionTime(): Long = secureStorage.totalSessionTime
    fun addSessionTime(ms: Long) { secureStorage.totalSessionTime = secureStorage.totalSessionTime + ms }

    fun getTotalDataDown(): Long = secureStorage.totalDataDown
    fun addDataDown(bytes: Long) { secureStorage.totalDataDown = secureStorage.totalDataDown + bytes }

    fun getTotalDataUp(): Long = secureStorage.totalDataUp
    fun addDataUp(bytes: Long) { secureStorage.totalDataUp = secureStorage.totalDataUp + bytes }

    fun logout() { secureStorage.clear() }
}
