package com.jarvis.app.runtime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.io.File
import java.util.concurrent.TimeUnit

enum class ToolSource(val label: String) {
    EMBEDDED("内置运行时"),
    SYSTEM("系统"),
    ROOT("root"),
    TERMUX("Termux"),
    MISSING("未安装")
}

data class RuntimeToolStatus(
    val id: String,
    val label: String,
    val description: String,
    val source: ToolSource,
    val path: String = "",
    val installPackage: String
)

data class ToolInstallResult(val success: Boolean, val message: String)

/** Detects optional command line tools without assuming that Android ships them. */
class RuntimeToolManager(private val context: Context) {
    private val runtime = File(context.filesDir, "jarvis/runtime")
    private val termuxPrefix = "/data/data/com.termux/files/usr"

    private data class Definition(
        val id: String,
        val label: String,
        val description: String,
        val packageName: String
    )

    private val definitions = listOf(
        Definition("zip", "zip", "压缩与解压", "zip"),
        Definition("rust", "Rust", "Rust 编译工具链（rustc/cargo）", "rust"),
        Definition("python", "Python", "Python 脚本环境", "python"),
        Definition("git", "Git", "版本控制工具", "git")
    )

    fun inspect(): List<RuntimeToolStatus> = definitions.map { definition ->
        val embedded = listOf(
            File(runtime, "bin/${definition.id}"),
            File(runtime, definition.id)
        ).firstOrNull { it.isFile }
        if (embedded != null) {
            return@map RuntimeToolStatus(definition.id, definition.label, definition.description, ToolSource.EMBEDDED, embedded.absolutePath, definition.packageName)
        }

        val system = listOf(
            File("/system/bin/${definition.id}"),
            File("/system/xbin/${definition.id}")
        ).firstOrNull { it.isFile }
        if (system != null) {
            return@map RuntimeToolStatus(definition.id, definition.label, definition.description, ToolSource.SYSTEM, system.absolutePath, definition.packageName)
        }

        val rootPath = commandPath(definition.id, asRoot = true)
        if (!rootPath.isNullOrBlank()) {
            return@map RuntimeToolStatus(definition.id, definition.label, definition.description, ToolSource.ROOT, rootPath, definition.packageName)
        }

        val termux = File(termuxPrefix, "bin/${definition.id}")
        if (termux.isFile) {
            return@map RuntimeToolStatus(definition.id, definition.label, definition.description, ToolSource.TERMUX, termux.absolutePath, definition.packageName)
        }

        RuntimeToolStatus(definition.id, definition.label, definition.description, ToolSource.MISSING, installPackage = definition.packageName)
    }

    fun install(status: RuntimeToolStatus): ToolInstallResult {
        if (status.source != ToolSource.MISSING) {
            return ToolInstallResult(true, "${status.label} 已可用：${status.source.label}")
        }
        val recovery = RuntimeRecoveryManager(context)
        if (!recovery.termuxPackageInstalled()) {
            return ToolInstallResult(false, "未检测到 Termux。请先安装 Termux，或在 root 环境中手动提供 ${status.label}。")
        }
        val command = "pkg update -y && pkg install -y ${status.installPackage}"
        return runCatching {
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                component = ComponentName("com.termux", "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", command))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            }
            context.startService(intent)
            ToolInstallResult(true, "已请求 Termux 安装 ${status.label}，完成后点击刷新状态。")
        }.getOrElse {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Jarvis 工具安装命令", command))
            context.packageManager.getLaunchIntentForPackage("com.termux")?.let { launch ->
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
            }
            ToolInstallResult(false, "系统拒绝自动执行，已复制命令并打开 Termux：$command")
        }
    }

    private fun commandPath(command: String, asRoot: Boolean): String? = runCatching {
        val builder = if (asRoot) {
            ProcessBuilder("/system/bin/su", "-c", "command -v $command")
        } else {
            ProcessBuilder("/system/bin/sh", "-c", "command -v $command")
        }
        val process = builder.redirectErrorStream(true).start()
        if (!process.waitFor(1200, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            return@runCatching null
        }
        if (process.exitValue() != 0) return@runCatching null
        process.inputStream.bufferedReader().use { it.readText().trim().lineSequence().firstOrNull() }
    }.getOrNull()
}
