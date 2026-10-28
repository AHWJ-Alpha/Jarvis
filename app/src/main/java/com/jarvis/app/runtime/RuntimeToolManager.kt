package com.jarvis.app.runtime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import org.json.JSONArray
import org.json.JSONObject
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
    private val androidUserId = Process.myUid() / 100000
    private val termuxPrefix = "/data/user/$androidUserId/com.termux/files/usr"

    private data class Definition(
        val id: String,
        val label: String,
        val description: String,
        val packageName: String,
        val executable: String = id
    )

    private val definitions = listOf(
        Definition("cmake", "CMake", "配置和生成 C/C++ 项目构建文件", "cmake"),
        Definition("ninja", "Ninja", "快速增量构建工具", "ninja"),
        Definition("clang", "Clang", "C/C++ 编译器；Android 目标仍需匹配的 NDK 工具链", "clang"),
        Definition("make", "GNU Make", "运行 Makefile 构建任务", "make"),
        Definition("ffmpeg", "FFmpeg", "转换、裁剪、压缩音视频", "ffmpeg"),
        Definition("ffprobe", "FFprobe", "读取媒体文件编码、时长和分辨率", "ffmpeg"),
        Definition("python", "Python", "运行 Python 脚本", "python"),
        Definition("git", "Git", "版本控制与代码下载", "git"),
        Definition("curl", "cURL", "HTTP 请求与文件下载", "curl"),
        Definition("wget", "Wget", "批量下载文件", "wget"),
        Definition("jq", "jq", "处理 JSON 命令行数据", "jq"),
        Definition("openssl", "OpenSSL", "哈希、证书和加密工具", "openssl"),
        Definition("sqlite3", "SQLite", "查询本地 SQLite 数据库", "sqlite"),
        Definition("tar", "tar", "打包与解包归档", "tar"),
        Definition("unzip", "unzip", "解压 ZIP 归档", "unzip"),
        Definition("zip", "zip", "压缩与解压", "zip"),
        Definition("node", "Node.js", "运行 JavaScript 工具和 OpenClaw", "nodejs-lts"),
        Definition("rustc", "Rust", "Rust 编译器", "rust", "rustc"),
        Definition("cargo", "Cargo", "Rust 包管理和构建工具", "rust")
    )

    fun inspect(): List<RuntimeToolStatus> {
        val unresolved = definitions.filter { definition ->
            embeddedPath(definition) == null && systemPath(definition) == null && !File(termuxPrefix, "bin/${definition.executable}").isFile
        }
        // Probe all remaining commands in two short shell calls instead of starting a
        // separate root prompt/process for every optional tool shown in the UI.
        val ordinaryPaths = commandPaths(unresolved.map { it.executable }, asRoot = false)
            .filterValues { path -> !path.contains("/com.termux/") || path.startsWith("$termuxPrefix/") }
        val fullRootEnabled = context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE).getInt("rootMode", 1) == 2
        val rootPaths = if (fullRootEnabled) {
            commandPaths(unresolved.filter { it.executable !in ordinaryPaths }.map { it.executable }, asRoot = true)
                .filterValues { path -> !path.contains("/com.termux/") || path.startsWith("$termuxPrefix/") }
        } else emptyMap()

        return definitions.map { definition ->
            val embedded = embeddedPath(definition)
            val system = systemPath(definition)
            val root = rootPaths[definition.executable] ?: ordinaryPaths[definition.executable]
            val termux = File(termuxPrefix, "bin/${definition.executable}")
            when {
                embedded != null -> status(definition, ToolSource.EMBEDDED, embedded)
                system != null -> status(definition, ToolSource.SYSTEM, system)
                root != null -> status(definition, ToolSource.ROOT, root)
                termux.isFile -> status(definition, ToolSource.TERMUX, termux.absolutePath)
                else -> RuntimeToolStatus(definition.id, definition.label, definition.description, ToolSource.MISSING, installPackage = definition.packageName)
            }
        }
    }

    /** A structured entry point for OpenClaw; only catalogued tools can run. */
    fun capability(arguments: JSONObject): CapabilityResult = when (arguments.optString("action", "list")) {
        "list" -> CapabilityResult(true, "已检查运行时工具", JSONObject().put("tools", JSONArray().apply {
            inspect().forEach { status ->
                put(JSONObject().put("id", status.id).put("label", status.label)
                    .put("source", status.source.name).put("path", status.path)
                    .put("installed", status.source != ToolSource.MISSING))
            }
        }))
        "run" -> runTool(arguments)
        else -> CapabilityResult(false, "只支持 list 或 run")
    }

    private fun runTool(request: JSONObject): CapabilityResult {
        val fullRoot = context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE).getInt("rootMode", 1) == 2
        if (!fullRoot) return CapabilityResult(false, "运行命令前请在 Jarvis 设置中启用完全 root 模式")
        val id = request.optString("tool")
        if (definitions.none { it.id == id }) return CapabilityResult(false, "工具不在 Jarvis 工具目录中：$id")
        val status = inspect().firstOrNull { it.id == id }
            ?: return CapabilityResult(false, "工具状态不可用：$id")
        if (status.source == ToolSource.MISSING || status.path.isBlank()) {
            return CapabilityResult(false, "${status.label} 尚未安装；请先在工具箱中安装并刷新")
        }
        val rawArgs = request.optJSONArray("args") ?: JSONArray()
        if (rawArgs.length() > 64) return CapabilityResult(false, "参数数量超过 64 个")
        val args = (0 until rawArgs.length()).map { index ->
            val value = rawArgs.opt(index)
            if (value !is String || value.length > 4096 || value.contains('\u0000')) return CapabilityResult(false, "参数必须是长度不超过 4096 的字符串")
            value
        }
        val workDir = request.optString("workingDirectory").ifBlank { context.filesDir.absolutePath }
        if (!workDir.startsWith("/") || workDir.contains('\u0000') || workDir.contains('\n')) {
            return CapabilityResult(false, "工作目录必须是绝对路径")
        }
        val timeout = request.optInt("timeoutSeconds", 30).coerceIn(1, 120)
        val command = buildString {
            if (status.source == ToolSource.TERMUX || status.path.startsWith("$termuxPrefix/")) {
                append("export PREFIX=${quote(termuxPrefix)} HOME=${quote("/data/user/$androidUserId/com.termux/files/home")} TMPDIR=${quote("$termuxPrefix/tmp")}; ")
                append("export LD_LIBRARY_PATH=\"\$PREFIX/lib\" PATH=\"\$PREFIX/bin:\$PATH\"; ")
            }
            val bundledNode = id == "node" && status.source == ToolSource.EMBEDDED
            if (bundledNode) {
                append("export PREFIX=${quote(runtime.absolutePath)} HOME=${quote(File(runtime, "home").absolutePath)} TMPDIR=${quote(File(runtime, "tmp").absolutePath)}; ")
                append("export LD_LIBRARY_PATH=${quote(File(runtime, "lib").absolutePath)} PATH=${quote(File(runtime, "bin").absolutePath)}:\$PATH; ")
            }
            append("cd ${quote(workDir)} || exit 125; exec ")
            if (bundledNode) append("/system/bin/linker64 ${quote(status.path)}") else append(quote(status.path))
            args.forEach { append(" ${quote(it)}") }
        }
        return runCatching {
            val child = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val output = StringBuilder()
            val reader = Thread {
                child.inputStream.bufferedReader().use { stream ->
                    val buffer = CharArray(1024)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        synchronized(output) {
                            if (output.length < 12000) output.append(buffer, 0, minOf(count, 12000 - output.length))
                        }
                    }
                }
            }.apply { start() }
            val finished = child.waitFor(timeout.toLong(), TimeUnit.SECONDS)
            if (!finished) child.destroyForcibly()
            reader.join(1000)
            val exitCode = if (finished) child.exitValue() else -1
            CapabilityResult(finished && exitCode == 0,
                if (finished) "${status.label} 已结束，退出码 $exitCode" else "${status.label} 超时，已停止",
                JSONObject().put("tool", id).put("exitCode", exitCode).put("timedOut", !finished)
                    .put("output", synchronized(output) { output.toString() }))
        }.getOrElse { CapabilityResult(false, "${status.label} 执行失败：${it.message}") }
    }

    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    fun install(status: RuntimeToolStatus): ToolInstallResult {
        if (status.source != ToolSource.MISSING) {
            return ToolInstallResult(true, "${status.label} 已可用：${status.source.label}")
        }
        val recovery = RuntimeRecoveryManager(context)
        if (!recovery.termuxPackageInstalled()) {
            return ToolInstallResult(false, "未检测到 Termux。请先安装 Termux，或在 root 环境中手动提供 ${status.label}。")
        }
        val command = "pkg install -y ${status.installPackage}"
        if (!File(termuxPrefix, "bin/bash").isFile) {
            return handoffToTermux(command, "当前 Android 用户的 Termux 尚未初始化，请先打开它完成首次安装")
        }
        return runCatching {
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                component = ComponentName("com.termux", "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", "$termuxPrefix/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", command))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/user/$androidUserId/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            }
            context.startService(intent)
            ToolInstallResult(true, "已请求 Termux 安装 ${status.label}，完成后点击刷新状态。")
        }.getOrElse {
            handoffToTermux(command, "系统拒绝自动执行")
        }
    }

    private fun handoffToTermux(command: String, reason: String): ToolInstallResult {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
            ?.setPrimaryClip(ClipData.newPlainText("Jarvis 工具安装命令", command))
        context.packageManager.getLaunchIntentForPackage("com.termux")?.let { launch ->
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
        }
        return ToolInstallResult(false, "$reason；已复制命令并打开 Termux。完成后回到 Jarvis 刷新状态：$command")
    }

    private fun status(definition: Definition, source: ToolSource, path: String) = RuntimeToolStatus(
        definition.id, definition.label, definition.description, source, path, definition.packageName
    )

    private fun embeddedPath(definition: Definition): String? = listOf(
        File(runtime, "bin/${definition.executable}"), File(runtime, definition.executable)
    ).firstOrNull { it.isFile }?.absolutePath

    private fun systemPath(definition: Definition): String? = listOf(
        File("/system/bin/${definition.executable}"), File("/system/xbin/${definition.executable}")
    ).firstOrNull { it.isFile }?.absolutePath

    private fun commandPaths(commands: List<String>, asRoot: Boolean): Map<String, String> {
        if (commands.isEmpty()) return emptyMap()
        require(commands.all { it.matches(Regex("[A-Za-z0-9_.+-]{1,40}")) })
        val script = commands.joinToString("\n") { command ->
            "path=\$(command -v $command 2>/dev/null || true); if [ -n \"\$path\" ]; then printf '%s=%s\\n' '$command' \"\$path\"; fi"
        }
        return runCatching {
            val process = if (asRoot) ProcessBuilder("/system/bin/su", "-c", script)
            else ProcessBuilder("/system/bin/sh", "-c", script)
            process.redirectErrorStream(true)
            val child = process.start()
            val output = StringBuilder()
            val reader = Thread {
                child.inputStream.bufferedReader().useLines { lines -> lines.forEach { line -> output.appendLine(line) } }
            }.apply { start() }
            if (!child.waitFor(2200, TimeUnit.MILLISECONDS)) {
                child.destroyForcibly()
                return@runCatching emptyMap()
            }
            reader.join(500)
            if (child.exitValue() != 0) return@runCatching emptyMap()
            output.lineSequence().mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1).trim()
            }.filter { (name, path) -> name in commands && path.startsWith("/") }.toMap()
        }.getOrDefault(emptyMap())
    }
}
