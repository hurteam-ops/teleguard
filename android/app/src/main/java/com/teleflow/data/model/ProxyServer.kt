package com.teleflow.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProxyServer(
    val ip: String,
    val port: Int,
    val label: String = "",
    val country: String = "",
    val countryCode: String = "",
    val city: String = "",
    val latency: Int = 0,
    val load: Int = 0,
    val protocol: String = "socks5",
    val isOnline: Boolean = true
) {
    val displayName: String
        get() = if (label.isNotBlank()) label else "$country - $ip"
}
