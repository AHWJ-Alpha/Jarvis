package com.jarvis.app.runtime

import android.content.Context
import android.util.Log
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.ZipInputStream

class BootstrapController(private val context: Context) {
    companion object { private val bootstrapLock = Any() }
    private val rootDirectory = File(context.filesDir, "jarvis")
    private val runtimeDirectory = File(rootDirectory, "runtime")
    private val pluginDirectory = File(rootDirectory, "plugins")
    private val configDirectory = File(rootDirectory, "config")
    private val logsDirectory = File(rootDirectory, "logs")
    private val bridgeTokenFile = File(configDirectory, "bridge.token")
    private val runtimeModeFile = File(configDirectory, "runtime-mode")

    fun prepareTokens() = synchronized(bootstrapLock) {
        configDirectory.mkdirs()
        ensureBridgeToken()
    }

    fun bootstrap(): BootstrapReport = synchronized(bootstrapLock) {
        val filesReady = listOf(rootDirectory, runtimeDirectory, pluginDirectory, configDirectory, logsDirectory)
            .all { it.mkdirs() || it.isDirectory }
        ensureEmbeddedBundle()
        val compatibilityReady = installAndroidCompatibility()
        val tokenReady = ensureBridgeToken()
        val runtimeMarker = readInstalledRuntimeVersion()
        val rootAvailable = probeRoot()
        val selectedMode = runtimeModeFile.takeIf { it.isFile }?.readText()?.trim()
            ?.let { runCatching { RuntimeMode.valueOf(it) }.getOrNull() }
        val state = when {
            !filesReady || !tokenReady || !compatibilityReady -> RuntimeState.ERROR
            runtimeMarker == null -> RuntimeState.DEGRADED
            else -> RuntimeState.READY
        }
        return BootstrapReport(
            state = state,
            runtimeMode = when {
                runtimeMarker != null -> RuntimeMode.EMBEDDED
                selectedMode == RuntimeMode.EXTERNAL_TERMUX -> RuntimeMode.EXTERNAL_TERMUX
                else -> RuntimeMode.REMOTE
            },
            runtimeVersion = runtimeMarker ?: "未检测到兼容运行时",
            filesReady = filesReady,
            rootAvailable = rootAvailable,
            bridgeTokenReady = tokenReady,
            pluginDirectory = pluginDirectory.absolutePath,
            message = when (state) {
                RuntimeState.READY -> "内置 Android arm64 运行时已解压；启动后将通过本机健康检查确认兼容性"
                RuntimeState.DEGRADED -> if (selectedMode == RuntimeMode.EXTERNAL_TERMUX) "已选择 Termux 备用方案；请在 Termux 中完成安装并返回检查" else "已固定 OpenClaw 2026.9.7；当前设备尚未安装可执行运行时，可选择修复或 Termux 备用方案"
                RuntimeState.ERROR -> "自动配置未完成，请查看日志"
                else -> "自动配置已完成"
            }
        )
    }

    fun bridgeToken(): String = bridgeTokenFile.takeIf { it.isFile }?.readText()?.trim().orEmpty()

    fun pluginDirectory(): File = pluginDirectory

    private fun installAndroidCompatibility(): Boolean {
        val files = listOf(
            "android-compat.mjs",
            "jarvis-plugin/index.js",
            "jarvis-plugin/openclaw.plugin.json",
            "node_modules/@openclaw/fs-safe/dist/publish-copy-stage.js",
            "node_modules/@openclaw/fs-safe/dist/pinned-write-staged.js"
        )
        return files.map { relative -> runCatching {
            val target = File(runtimeDirectory, relative)
            target.parentFile?.mkdirs()
            context.assets.open("openclaw/overrides/$relative").use { input ->
                val bytes = input.readBytes()
                if (!target.isFile || !target.readBytes().contentEquals(bytes)) target.writeBytes(bytes)
            }
        }.onFailure { Log.e("JarvisBootstrap", "Compatibility overlay failed: $relative", it) }.isSuccess }.all { it }
    }

    private fun ensureEmbeddedBundle(): Boolean {
        val marker = File(runtimeDirectory, ".installed")
        if (marker.isFile && File(runtimeDirectory, "node").canExecute() && File(runtimeDirectory, "openclaw.mjs").isFile) return true
        return runCatching {
            context.assets.open("openclaw/runtime.bundle.zip").use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val target = File(runtimeDirectory, entry.name).canonicalFile
                        val runtimePath = runtimeDirectory.canonicalPath
                        require(target.path == runtimePath || target.path.startsWith(runtimePath + File.separator)) {
                            "invalid runtime archive path"
                        }
                        if (entry.isDirectory) target.mkdirs() else {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { output -> zip.copyTo(output) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            val node = File(runtimeDirectory, "node")
            val entry = File(runtimeDirectory, "openclaw.mjs")
            if (!node.isFile || !entry.isFile) {
                marker.delete()
                false
            } else {
                node.setExecutable(true)
                marker.writeText("openclaw-2026.9.7")
                true
            }
        }.onFailure { error ->
            val detail = error.stackTraceToString()
            Log.e("JarvisBootstrap", "Embedded OpenClaw bundle extraction failed", error)
            runCatching {
                File(logsDirectory, "bootstrap-error.log").writeText(detail)
                File(runtimeDirectory, ".extraction-error").writeText(error.message ?: error.javaClass.name)
            }
        }.getOrDefault(false)
    }

    private fun ensureBridgeToken(): Boolean {
        if (bridgeTokenFile.isFile && bridgeTokenFile.readText().trim().length >= 32) return true
        return runCatching {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            bridgeTokenFile.writeText(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
            true
        }.getOrDefault(false)
    }

    private fun readInstalledRuntimeVersion(): String? {
        val node = File(runtimeDirectory, "node")
        val entry = File(runtimeDirectory, "openclaw.mjs")
        if (!node.canExecute() || !entry.isFile) return null
        return runCatching {
            context.assets.open("openclaw/runtime.json").bufferedReader().use { reader ->
                Regex("\\\"version\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(reader.readText())?.groupValues?.get(1)
            }
        }.getOrNull() ?: "已安装"
    }

    private fun probeRoot(): Boolean = runCatching {
        val process = ProcessBuilder("/system/bin/su", "-c", "id")
            .redirectErrorStream(true)
            .start()
        process.waitFor(1500, java.util.concurrent.TimeUnit.MILLISECONDS) && process.exitValue() == 0
    }.getOrDefault(false)
}
