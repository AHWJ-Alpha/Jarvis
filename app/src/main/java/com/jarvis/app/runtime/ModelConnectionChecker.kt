package com.jarvis.app.runtime

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

data class ModelConnectionResult(val ok: Boolean, val message: String)

/** Verifies an actual inference request without giving the test any device tools. */
object ModelConnectionChecker {
    fun test(baseUrl: String, apiKey: String, model: String): ModelConnectionResult {
        if (baseUrl.isBlank()) return ModelConnectionResult(false, "请填写 Base URL 后重试。")
        if (model.isBlank()) return ModelConnectionResult(false, "请填写模型名后重试。")
        val endpoint = runCatching {
            val base = URL(baseUrl.trim().trimEnd('/'))
            require(base.protocol in listOf("http", "https") && base.host.isNotBlank() && base.userInfo == null && base.query == null && base.ref == null)
            URL(base.toString() + "/chat/completions")
        }.getOrElse { return ModelConnectionResult(false, "Base URL 格式无效，请填写 HTTP(S) API 根地址，例如 https://api.deepseek.com/v1。") }

        return runCatching {
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 45_000
                instanceFollowRedirects = false
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            }
            try {
                val body = JSONObject().put("model", model.trim()).put("stream", false)
                    .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "Connection test. Reply with only OK.")))
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
                val status = connection.responseCode
                if (status !in 200..299) return@runCatching ModelConnectionResult(false, when (status) {
                    401 -> "验证失败（HTTP 401）：API Key 无效或已失效。"
                    402 -> "验证失败（HTTP 402）：请检查服务余额。"
                    403 -> "验证失败（HTTP 403）：当前账号没有该接口或模型的权限。"
                    404 -> "验证失败（HTTP 404）：请检查 Base URL 和模型名。"
                    429 -> "验证失败（HTTP 429）：服务限流或配额不足，请稍后重试。"
                    in 300..399 -> "验证失败：接口发生重定向，请填写最终 API 地址。"
                    in 500..599 -> "验证失败（HTTP $status）：模型服务暂时不可用。"
                    else -> "验证失败（HTTP $status）：请检查模型名及服务的 Chat Completions 配置。"
                })
                val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val message = JSONObject(response).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                val content = message?.opt("content")
                val hasContent = (content is String && content.isNotBlank()) || (content is JSONArray && content.length() > 0)
                ModelConnectionResult(hasContent, if (hasContent) "主模型验证成功：已收到实际测试回复。" else "接口返回 HTTP $status，但没有返回可显示的模型回复，请检查模型和接口类型。")
            } finally {
                connection.disconnect()
            }
        }.getOrElse { error -> ModelConnectionResult(false, when (error) {
            is UnknownHostException -> "验证失败：手机无法解析模型域名，请检查网络和 DNS。"
            is SocketTimeoutException -> "验证超时：请检查手机网络及模型服务。"
            is SSLException -> "验证失败：TLS 连接异常，请检查证书、手机时间及代理。"
            else -> "验证失败：无法完成模型请求，请检查地址、网络和接口格式。"
        }) }
    }
}
