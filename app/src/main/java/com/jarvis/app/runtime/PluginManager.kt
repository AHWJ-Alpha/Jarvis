package com.jarvis.app.runtime

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class PluginManager(context: Context) {
    private val bootstrap = BootstrapController(context)
    private val pluginRoot = bootstrap.pluginDirectory()

    fun listInstalled(): List<PluginRecord> = pluginRoot.listFiles()?.flatMap { plugin ->
        plugin.listFiles()?.mapNotNull { version ->
            val manifestFile = File(version, "manifest.json")
            if (!manifestFile.isFile) return@mapNotNull null
            runCatching {
                PluginRecord(PluginManifest.fromJson(manifestFile.readText()), PluginState.INSTALLED, version.absolutePath)
            }.getOrNull()
        } ?: emptyList()
    } ?: emptyList()

    fun install(manifest: PluginManifest, onProgress: (Long, Long) -> Unit = { _, _ -> }): PluginRecord {
        require(manifest.id.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{2,}"))) { "插件 id 无效" }
        require(manifest.artifactUrl.startsWith("https://")) { "插件必须使用 HTTPS 来源" }
        require(manifest.sha256.matches(Regex("[0-9a-f]{64}"))) { "插件必须提供 SHA-256" }
        val target = File(File(pluginRoot, manifest.id), manifest.version)
        val staging = File(pluginRoot, ".staging-${manifest.id}-${manifest.version}")
        if (staging.exists()) staging.deleteRecursively()
        staging.mkdirs()
        return try {
            val artifact = File(staging, "artifact")
            download(manifest, artifact, onProgress)
            check(sha256(artifact) == manifest.sha256) { "插件 SHA-256 校验失败" }
            File(staging, "manifest.json").writeText(manifestJson(manifest))
            if (target.exists()) target.deleteRecursively()
            target.parentFile?.mkdirs()
            check(staging.renameTo(target)) { "插件目录激活失败" }
            PluginRecord(manifest, PluginState.INSTALLED, target.absolutePath, manifest.sha256)
        } catch (error: Throwable) {
            staging.deleteRecursively()
            PluginRecord(manifest, PluginState.FAILED, target.absolutePath, lastError = error.message.orEmpty())
        }
    }

    private fun download(manifest: PluginManifest, destination: File, onProgress: (Long, Long) -> Unit) {
        val connection = (URL(manifest.artifactUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            requestMethod = "GET"
        }
        connection.connect()
        check(connection.responseCode in 200..299) { "插件下载失败：HTTP ${connection.responseCode}" }
        val length = connection.contentLengthLong.takeIf { it > 0 } ?: manifest.sizeBytes
        var total = 0L
        connection.inputStream.use { input ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    check(total <= 100L * 1024L * 1024L) { "插件超过 100MB 限制" }
                    output.write(buffer, 0, count)
                    onProgress(total, length)
                }
            }
        }
        connection.disconnect()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun manifestJson(manifest: PluginManifest): String = """
        {
          "id":"${manifest.id}",
          "name":"${manifest.name.replace("\"", "\\\"")}",
          "version":"${manifest.version}",
          "type":"${manifest.type}",
          "entrypoint":"${manifest.entrypoint}",
          "artifact":{"url":"${manifest.artifactUrl}","sha256":"${manifest.sha256}","size":${manifest.sizeBytes}},
          "capabilities":[${manifest.capabilities.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }}],
          "source":"${manifest.source.replace("\"", "\\\"")}",
          "license":"${manifest.license.replace("\"", "\\\"")}"
        }
    """.trimIndent()
}
