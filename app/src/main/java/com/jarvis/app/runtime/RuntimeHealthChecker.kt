package com.jarvis.app.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RuntimeHealth(
    val bootstrap: BootstrapReport,
    val processRunning: Boolean,
    val bridge: BridgeHealth,
    val diagnosis: String
)

class RuntimeHealthChecker(
    private val bootstrap: BootstrapController,
    private val processManager: OpenClawProcessManager
) {
    suspend fun check(baseUrl: String): RuntimeHealth = withContext(Dispatchers.IO) {
        val report = bootstrap.bootstrap()
        val bridge = OpenClawBridge(baseUrl, bootstrap.bridgeToken()).health()
        RuntimeHealth(
            bootstrap = report,
            processRunning = processManager.isRunning(),
            bridge = bridge,
            diagnosis = when {
                bridge.reachable -> "Gateway 可连接"
                processManager.isRunning() -> "Gateway 进程已启动，但健康接口尚未响应"
                report.runtimeMode == RuntimeMode.EXTERNAL_TERMUX -> "等待 Termux Gateway 或内置 arm64 Node runtime"
                else -> "等待 OpenClaw Gateway 启动"
            }
        )
    }
}
