package com.jarvis.app.runtime

import android.content.Context
import java.io.File
import java.security.SecureRandom
import java.util.Base64

class BootstrapController(private val context: Context) {
    private val rootDirectory = File(context.filesDir, "jarvis")
    private val runtimeDirectory = File(rootDirectory, "runtime")
    private val pluginDirectory = File(rootDirectory, "plugins")
    private val configDirectory = File(rootDirectory, "config")
    private val logsDirectory = File(rootDirectory, "logs")
    private val bridgeTokenFile = File(configDirectory, "bridge.token")

    fun bootstrap(): BootstrapReport {
        val filesReady = listOf(rootDirectory, runtimeDirectory, pluginDirectory, configDirectory, logsDirectory)
            .all { it.mkdirs() || it.isDirectory }
        val tokenReady = ensureBridgeToken()
        val runtimeMarker = readRuntimeMarker()
        val rootAvailable = probeRoot()
        val state = when {
            !filesReady || !tokenReady -> RuntimeState.ERROR
            runtimeMarker == null -> RuntimeState.DEGRADED
            else -> RuntimeState.READY
        }
        return BootstrapReport(
            state = state,
            runtimeMode = if (runtimeMarker == null) RuntimeMode.EXTERNAL_TERMUX else RuntimeMode.EMBEDDED,
            runtimeVersion = runtimeMarker ?: "未检测到兼容运行时",
            filesReady = filesReady,
            rootAvailable = rootAvailable,
            bridgeTokenReady = tokenReady,
            pluginDirectory = pluginDirectory.absolutePath,
            message = when (state) {
                RuntimeState.READY -> "内置运行时目录已准备"
                RuntimeState.DEGRADED -> "已固定 OpenClaw 2026.9.7；当前设备需配置内置 Node 或 Termux 后端"
                RuntimeState.ERROR -> "自动配置未完成，请查看日志"
                else -> "自动配置已完成"
            }
        )
    }

    fun bridgeToken(): String = bridgeTokenFile.takeIf { it.isFile }?.readText()?.trim().orEmpty()

    fun pluginDirectory(): File = pluginDirectory

    private fun ensureBridgeToken(): Boolean {
        if (bridgeTokenFile.isFile && bridgeTokenFile.readText().trim().length >= 32) return true
        return runCatching {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            bridgeTokenFile.writeText(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
            true
        }.getOrDefault(false)
    }

    private fun readRuntimeMarker(): String? = runCatching {
        context.assets.open("openclaw/runtime.json").bufferedReader().use { reader ->
            val text = reader.readText()
            Regex("\\\"version\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(text)?.groupValues?.get(1)
        }
    }.getOrNull()

    private fun probeRoot(): Boolean = runCatching {
        val process = ProcessBuilder("su", "-c", "id")
            .redirectErrorStream(true)
            .start()
        process.waitFor(1500, java.util.concurrent.TimeUnit.MILLISECONDS) && process.exitValue() == 0
    }.getOrDefault(false)
}
