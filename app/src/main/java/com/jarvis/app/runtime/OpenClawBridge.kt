package com.jarvis.app.runtime

import java.net.HttpURLConnection
import java.net.URL

data class BridgeHealth(val reachable: Boolean, val message: String)

class OpenClawBridge(
    private val baseUrl: String,
    private val token: String
) {
    fun health(): BridgeHealth = runCatching {
        val connection = (URL(baseUrl.trimEnd('/') + "/health").openConnection() as HttpURLConnection).apply {
            connectTimeout = 1500
            readTimeout = 1500
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
        }
        val code = connection.responseCode
        connection.disconnect()
        if (code in 200..299) BridgeHealth(true, "OpenClaw Gateway 已连接")
        else BridgeHealth(false, "Gateway 返回 HTTP $code")
    }.getOrElse { BridgeHealth(false, "尚未连接 OpenClaw Gateway") }
}
