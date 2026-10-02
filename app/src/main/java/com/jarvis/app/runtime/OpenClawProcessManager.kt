package com.jarvis.app.runtime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RuntimeLaunchResult(
    val started: Boolean,
    val mode: RuntimeMode,
    val message: String,
    val pid: Long? = null
)

class OpenClawProcessManager(private val context: Context) {
    private val bootstrap = BootstrapController(context)
    private val root = File(context.filesDir, "jarvis")
    private val runtime = File(root, "runtime")
    private val logs = File(root, "logs")
    private val bridge = DeviceCapabilityBridge(context)
    private var process: Process? = null

    suspend fun start(): RuntimeLaunchResult = withContext(Dispatchers.IO) {
        if (process?.isAlive == true) {
            return@withContext RuntimeLaunchResult(true, RuntimeMode.EMBEDDED, "OpenClaw Gateway 已在运行")
        }
        logs.mkdirs()
        val embeddedNode = File(runtime, "node")
        val embeddedEntry = File(runtime, "openclaw.mjs")
        val runtimeProbe = "runtime=${runtime.absolutePath} exists=${runtime.exists()} node=${embeddedNode.absolutePath} nodeExists=${embeddedNode.exists()} nodeFile=${embeddedNode.isFile} nodeExec=${embeddedNode.canExecute()} entryExists=${embeddedEntry.exists()}"
        // Android can report false for File.canExecute() on app-private files
        // mounted with restrictive labels even when the process can execute
        // them. Let ProcessBuilder make the definitive check and surface its
        // error in the runtime log.
        if (embeddedNode.isFile && embeddedEntry.isFile) {
            prepareOpenClawConfig()
            val embeddedResult = launch(
                mode = RuntimeMode.EMBEDDED,
                command = listOf(embeddedNode.absolutePath, embeddedEntry.absolutePath, "gateway", "--bind", "loopback"),
                workingDirectory = runtime
            )
            if (embeddedResult.started) return@withContext embeddedResult
            if (embeddedResult.message.contains("Permission denied") || embeddedResult.message.contains("error=13")) {
                val rootResult = launchAsRoot(embeddedNode, embeddedEntry)
                if (rootResult.started) return@withContext rootResult
                return@withContext RuntimeLaunchResult(
                    false,
                    RuntimeMode.EMBEDDED,
                    "普通应用沙箱禁止执行内置 Node（error=13），root 启动也失败：${rootResult.message}。请在 KernelSU 中授予 Jarvis root 权限后重试。"
                )
            }
            // Preserve the embedded failure details when no external fallback
            // is installed; the generic "not found" message hides the real
            // linker/configuration error.
            if (!RuntimeRecoveryManager(context).termuxPackageInstalled()) {
                return@withContext embeddedResult
            }
        }
        val selectedMode = File(root, "config/runtime-mode").takeIf { it.isFile }?.readText()?.trim()
        if (selectedMode == RuntimeMode.EMBEDDED.name) {
            return@withContext RuntimeLaunchResult(false, RuntimeMode.EMBEDDED, "已选择内置运行时，但未找到可执行 Node/OpenClaw 文件；请使用运行时修复或切换 Termux")
        }
        if (RuntimeRecoveryManager(context).termuxPackageInstalled()) {
            val result = RuntimeRecoveryManager(context).startTermuxGateway()
            return@withContext RuntimeLaunchResult(result.success, RuntimeMode.EXTERNAL_TERMUX, result.message)
        }
        RuntimeLaunchResult(false, RuntimeMode.REMOTE, "未找到内置 Node/OpenClaw 或 Termux Gateway；请安装兼容运行时（$runtimeProbe）")
    }

    suspend fun stop(): Boolean = withContext(Dispatchers.IO) {
        val current = process ?: return@withContext true
        current.destroy()
        if (!current.waitFor(1500, TimeUnit.MILLISECONDS)) current.destroyForcibly()
        process = null
        true
    }

    fun isRunning(): Boolean = process?.isAlive == true

    fun capabilityBridge(): DeviceCapabilityBridge = bridge

    private fun launch(mode: RuntimeMode, command: List<String>, workingDirectory: File): RuntimeLaunchResult {
        return runCatching {
            val logFile = File(logs, "openclaw-${System.currentTimeMillis()}.log")
            val builder = ProcessBuilder(command)
                .directory(workingDirectory)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
            if (mode == RuntimeMode.EMBEDDED) {
                val environment = builder.environment()
                val runtimeLib = File(runtime, "lib").absolutePath
                environment["LD_LIBRARY_PATH"] = runtimeLib
                environment["PREFIX"] = runtime.absolutePath
                environment["TERMUX_PREFIX"] = runtime.absolutePath
                environment["PATH"] = "${File(runtime, "bin").absolutePath}:${environment["PATH"].orEmpty()}"
                environment["HOME"] = File(runtime, "home").absolutePath
                environment["TMPDIR"] = File(runtime, "tmp").apply { mkdirs() }.absolutePath
                environment["NODE_PATH"] = File(runtime, "node_modules").absolutePath
                environment["OPENCLAW_HOME"] = File(runtime, "openclaw-home").apply { mkdirs() }.absolutePath
                environment["OPENCLAW_CONFIG_PATH"] = File(runtime, "openclaw-home/openclaw.json").absolutePath
                environment["JARVIS_BRIDGE_URL"] = "http://127.0.0.1:18791"
                environment["JARVIS_BRIDGE_TOKEN"] = bootstrap.bridgeToken()
            }
            val started = builder.start()
            process = started
            Thread.sleep(800)
            if (!started.isAlive) {
                process = null
                return RuntimeLaunchResult(false, mode, "Gateway 进程立即退出，通常表示 Android 原生依赖或动态链接不兼容；日志：${logFile.name}")
            }
            RuntimeLaunchResult(true, mode, "OpenClaw Gateway 启动中，日志：${logFile.name}", null)
        }.getOrElse { error ->
            RuntimeLaunchResult(false, mode, "Gateway 启动失败：${error.message}")
        }
    }

    private fun prepareOpenClawConfig() {
        val home = File(runtime, "openclaw-home").apply { mkdirs() }
        val config = File(home, "openclaw.json")
        runCatching {
            val json = if (config.isFile) JSONObject(config.readText()) else JSONObject()
            val plugins = json.optJSONObject("plugins") ?: JSONObject().also { json.put("plugins", it) }
            val load = plugins.optJSONObject("load") ?: JSONObject().also { plugins.put("load", it) }
            load.put("paths", org.json.JSONArray().put(File(runtime, "jarvis-plugin").absolutePath))
            config.writeText(json.toString(2))
        }.onFailure { error ->
            File(logs, "openclaw-config-error.log").writeText(error.stackTraceToString())
        }
    }

    private fun launchAsRoot(node: File, entry: File): RuntimeLaunchResult {
        val command = listOf(
            "export LD_LIBRARY_PATH=${shellQuote(File(runtime, "lib").absolutePath)}",
            "export PREFIX=${shellQuote(runtime.absolutePath)}",
            "export TERMUX_PREFIX=${shellQuote(runtime.absolutePath)}",
            "export PATH=${shellQuote(File(runtime, "bin").absolutePath)}:\$PATH",
            "export HOME=${shellQuote(File(runtime, "home").absolutePath)}",
            "export TMPDIR=${shellQuote(File(runtime, "tmp").apply { mkdirs() }.absolutePath)}",
            "export NODE_PATH=${shellQuote(File(runtime, "node_modules").absolutePath)}",
            "export OPENCLAW_HOME=${shellQuote(File(runtime, "openclaw-home").absolutePath)}",
            "export OPENCLAW_CONFIG_PATH=${shellQuote(File(runtime, "openclaw-home/openclaw.json").absolutePath)}",
            "export JARVIS_BRIDGE_URL=http://127.0.0.1:18791",
            "export JARVIS_BRIDGE_TOKEN=${shellQuote(bootstrap.bridgeToken())}",
            "exec ${shellQuote(node.absolutePath)} ${shellQuote(entry.absolutePath)} gateway --bind loopback"
        ).joinToString("; ")
        return launch(
            mode = RuntimeMode.EMBEDDED,
            command = listOf("/system/bin/su", "-c", command),
            workingDirectory = runtime
        )
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\\"'\\\"'")}'"

    private fun findExecutable(path: String): String? = path.takeIf { File(it).canExecute() }
}
