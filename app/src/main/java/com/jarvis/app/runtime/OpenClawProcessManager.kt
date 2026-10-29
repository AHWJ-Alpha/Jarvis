package com.jarvis.app.runtime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class RuntimeLaunchResult(
    val started: Boolean,
    val mode: RuntimeMode,
    val message: String,
    val pid: Long? = null
)

class OpenClawProcessManager private constructor(private val context: Context) {
    private val root = File(context.filesDir, "jarvis")
    private val runtime = File(root, "runtime")
    private val logs = File(root, "logs")
    private val config = GatewayConfig(context)
    private val mutex = Mutex()
    @Volatile private var process: Process? = null

    suspend fun start(): RuntimeLaunchResult = mutex.withLock { withContext(Dispatchers.IO) {
        BootstrapController(context).bootstrap()
        if (!File(runtime, "node").isFile || !File(runtime, "openclaw.mjs").isFile) {
            return@withContext RuntimeLaunchResult(false, RuntimeMode.EMBEDDED, "内置运行时文件不完整；请使用运行时修复")
        }
        val changed = runCatching { config.synchronize() }.getOrElse {
            return@withContext RuntimeLaunchResult(false, RuntimeMode.EMBEDDED, "Gateway 配置失败：${it.message}")
        }
        // 模型回退告警（如主模型未配置）与识图注册结果随启动结果一起交给用户，避免静默降级。
        val warning = config.lastWarning?.takeIf { it.isNotBlank() }
        val visionNote = if (config.visionRegistered) {
            "识图模型已注册（" + config.visionRegistration.name.lowercase() + "）"
        } else {
            null
        }
        fun notice(base: String): String = listOfNotNull(base, warning, visionNote).joinToString("；")
        if (process?.isAlive == true && !changed && healthy()) {
            return@withContext RuntimeLaunchResult(true, RuntimeMode.EMBEDDED, notice("OpenClaw Gateway 已就绪"))
        }
        stopProcess()
        logs.mkdirs()
        val log = File(logs, "openclaw-${System.currentTimeMillis()}.log")
        val builder = ProcessBuilder(
            "/system/bin/linker64", File(runtime, "node").absolutePath,
            File(runtime, "openclaw.mjs").absolutePath,
            "gateway", "--allow-unconfigured", "--bind", "loopback", "--port", "18789"
        ).directory(runtime).redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(log))
        val env = builder.environment()
        env["LD_LIBRARY_PATH"] = File(runtime, "lib").absolutePath
        env["PREFIX"] = runtime.absolutePath
        env["TERMUX_PREFIX"] = runtime.absolutePath
        env["PATH"] = "${File(runtime, "bin").absolutePath}:${env["PATH"].orEmpty()}"
        env["HOME"] = File(runtime, "home").apply { mkdirs() }.absolutePath
        env["TMPDIR"] = File(runtime, "tmp").apply { mkdirs() }.absolutePath
        env["NODE_PATH"] = File(runtime, "node_modules").absolutePath
        env["OPENCLAW_HOME"] = File(runtime, "openclaw-home").apply { mkdirs() }.absolutePath
        env["OPENCLAW_STATE_DIR"] = env["OPENCLAW_HOME"]!!
        env["OPENCLAW_CONFIG_PATH"] = File(runtime, "openclaw-home/openclaw.json").absolutePath
        env["OPENCLAW_NO_RESPAWN"] = "1"
        env["OPENCLAW_NO_AUTO_UPDATE"] = "1"
        env["JARVIS_NODE_PATH"] = File(runtime, "node").absolutePath
        env["JARVIS_ANDROID_COMPAT"] = File(runtime, "android-compat.mjs").absolutePath
        env["NODE_OPTIONS"] = "--import=${env["JARVIS_ANDROID_COMPAT"]}"
        env["OPENSSL_CONF"] = File(runtime, "etc/tls/openssl.cnf").absolutePath
        env["SSL_CERT_FILE"] = File(runtime, "etc/tls/cert.pem").absolutePath
        env["JARVIS_BRIDGE_URL"] = "http://127.0.0.1:18791"
        env["JARVIS_BRIDGE_TOKEN"] = BootstrapController(context).bridgeToken()
        val launched = runCatching { builder.start() }.getOrElse {
            return@withContext RuntimeLaunchResult(false, RuntimeMode.EMBEDDED, "内置 Gateway 启动失败：${it.message}")
        }
        process = launched
        repeat(90) {
            if (healthy()) return@withContext RuntimeLaunchResult(true, RuntimeMode.EMBEDDED, notice("OpenClaw Gateway 已就绪"))
            if (!launched.isAlive) {
                process = null
                return@withContext RuntimeLaunchResult(false, RuntimeMode.EMBEDDED, notice("Gateway 已退出，请查看 ${log.name}"))
            }
            Thread.sleep(1000)
        }
        RuntimeLaunchResult(false, RuntimeMode.EMBEDDED, notice("Gateway 启动超时，请查看 ${log.name}"))
    } }

    suspend fun stop(): Boolean = mutex.withLock { withContext(Dispatchers.IO) {
        stopProcess()
        true
    } }

    fun isRunning(): Boolean = process?.isAlive == true && healthy()
    fun token(): String = config.token()

    private fun healthy(): Boolean = runCatching {
        val connection = URL("http://127.0.0.1:18789/v1/models").openConnection() as HttpURLConnection
        connection.connectTimeout = 1000
        connection.readTimeout = 1000
        connection.setRequestProperty("Authorization", "Bearer ${config.token()}")
        val ok = connection.responseCode == 200
        connection.disconnect()
        ok
    }.getOrDefault(false)

    private fun stopProcess() {
        val current = process ?: return
        current.destroy()
        if (!current.waitFor(1500, TimeUnit.MILLISECONDS)) current.destroyForcibly()
        process = null
    }

    companion object {
        @Volatile private var instance: OpenClawProcessManager? = null
        fun get(context: Context): OpenClawProcessManager = instance ?: synchronized(this) {
            instance ?: OpenClawProcessManager(context.applicationContext).also { instance = it }
        }
    }
}
