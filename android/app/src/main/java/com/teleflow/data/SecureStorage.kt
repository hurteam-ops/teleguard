package com.teleflow.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage private constructor(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    var authToken: String
        get() = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var tokenExpiresAt: Long
        get() = prefs.getLong(KEY_TOKEN_EXPIRES, 0L)
        set(value) = prefs.edit().putLong(KEY_TOKEN_EXPIRES, value).apply()

    val isAuthenticated: Boolean
        get() = authToken.isNotBlank() && tokenExpiresAt > System.currentTimeMillis()

    var userId: Long
        get() = prefs.getLong(KEY_USER_ID, 0L)
        set(value) = prefs.edit().putLong(KEY_USER_ID, value).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var isPremium: Boolean
        get() = prefs.getBoolean(KEY_IS_PREMIUM, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_PREMIUM, value).apply()

    var premiumExpiresAt: Long
        get() = prefs.getLong(KEY_PREMIUM_EXPIRES, 0L)
        set(value) = prefs.edit().putLong(KEY_PREMIUM_EXPIRES, value).apply()

    var selectedProxyId: String
        get() = prefs.getString(KEY_SELECTED_PROXY, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_SELECTED_PROXY, value).apply()

    var killSwitchEnabled: Boolean
        get() = prefs.getBoolean(KEY_KILL_SWITCH, true)
        set(value) = prefs.edit().putBoolean(KEY_KILL_SWITCH, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "teleflow_secure_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_TOKEN_EXPIRES = "token_expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_IS_PREMIUM = "is_premium"
        private const val KEY_PREMIUM_EXPIRES = "premium_expires_at"
        private const val KEY_SELECTED_PROXY = "selected_proxy"
        private const val KEY_KILL_SWITCH = "kill_switch_enabled"

        @Volatile
        private var instance: SecureStorage? = null

        fun getInstance(context: Context): SecureStorage {
            return instance ?: synchronized(this) {
                instance ?: SecureStorage(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
