package com.jarvis.app.runtime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.Manifest
import android.os.Build
import android.os.Environment
import android.os.Process
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

enum class CapabilityRisk { LOW, MEDIUM, HIGH }

data class CapabilityRequest(
    val capability: String,
    val arguments: JSONObject,
    val risk: CapabilityRisk,
    val requiresApproval: Boolean
)

data class CapabilityResult(val ok: Boolean, val message: String, val data: JSONObject = JSONObject())

/**
 * Device bridge exposed to OpenClaw. Camera, accessibility and OCR use
 * dedicated capabilities; shell access requires the user's Full root mode.
 */
class DeviceCapabilityBridge(private val context: Context) {
    private val auditLog = File(context.filesDir, "jarvis/logs/capability-audit.jsonl")
    private val memory = OperationMemory.at(context.filesDir)
    private val operationHost = object : OperationHost {
        override fun call(capability: String, arguments: JSONObject): CapabilityResult =
            execute(CapabilityRequest(capability, arguments, CapabilityRisk.HIGH, false))
        override fun inspect(): JSONObject = AccessibilityController.inspect()
        override fun version(packageName: String): String? = runCatching {
            val info = context.packageManager.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= 28) info.longVersionCode.toString() else info.versionCode.toString()
        }.getOrNull()
        override fun fileState(path: String): JSONObject? {
            if (!path.startsWith("/") || path.contains('\n') || path.contains('\u0000')) return null
            val file = File(path)
            if (file.isFile) return JSONObject().put("path", path).put("size", file.length()).put("modified", file.lastModified())
            val fullRoot = context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE).getInt("rootMode", 1) == 2
            val quoted = "'" + path.replace("'", "'\\''") + "'"
            val result = rootExec("test -f $quoted && stat -c '%s %Y' $quoted", fullRoot)
            if (!result.ok) return null
            val parts = result.message.trim().split(Regex("\\s+"))
            val size = parts.getOrNull(0)?.toLongOrNull() ?: return null
            val modified = parts.getOrNull(1)?.toLongOrNull() ?: return null
            return JSONObject().put("path", path).put("size", size).put("modified", modified * 1000)
        }
    }

    fun runOperationMemory(arguments: JSONObject): CapabilityResult = memory.execute(arguments, operationHost)

    fun execute(request: CapabilityRequest, approved: Boolean = false): CapabilityResult {
        if (RemoteExecutionGuard.active && !RemoteExecutionGuard.deviceControlAllowed) {
            return CapabilityResult(false, "远程设备操作尚未在手机设置中授权；本次设备能力未执行。")
        }
        if (request.requiresApproval && !approved) return CapabilityResult(false, "需要用户确认：${request.capability}")
        val fullRoot = context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE).getInt("rootMode", 1) == 2
        auditLog.parentFile?.mkdirs()
        auditLog.appendText(JSONObject().apply {
            put("capability", request.capability)
            put("risk", request.risk.name)
            put("fullRoot", fullRoot)
            put("timestamp", System.currentTimeMillis())
        }.toString() + "\n")
        val before = if (request.capability.startsWith("accessibility.")) AccessibilityController.inspect() else JSONObject()
        FloatingAssistantController.operationStarted(request.capability)
        var operationSucceeded = false
        val result = try {
            when (request.capability) {
            "operation.memory" -> memory.execute(request.arguments, operationHost)
            "device.info" -> CapabilityResult(true, "设备信息可用", JSONObject()
                .put("package", context.packageName).put("manufacturer", Build.MANUFACTURER)
                .put("brand", Build.BRAND).put("model", Build.MODEL)
                .put("androidVersion", Build.VERSION.RELEASE).put("sdk", Build.VERSION.SDK_INT)
                .put("androidUserId", Process.myUid() / 100000)
                .put("externalStorage", Environment.getExternalStorageDirectory().absolutePath))
            "runtime.tools" -> RuntimeToolManager(context).capability(request.arguments)
            "root.exec" -> rootExec(request.arguments.optString("command"), fullRoot)
            "camera.capture" -> cameraCapture()
            "accessibility.inspect" -> accessibilityInspect()
            "accessibility.click" -> accessibilityClick(request.arguments)
            "accessibility.input" -> accessibilityInput(request.arguments)
            "accessibility.global" -> accessibilityGlobal(request.arguments)
            "accessibility.perform" -> AccessibilityController.perform(request.arguments)
            "accessibility.swipe" -> CapabilityResult(AccessibilityController.swipe(request.arguments), "滑动已执行，需核实界面")
            "app.launch" -> appLaunch(request.arguments)
            "ocr.read" -> accessibilityOcr()
            "notification.read" -> notificationRead()
            else -> CapabilityResult(false, "未知设备能力：${request.capability}")
            }.also { operationSucceeded = it.ok }
        } finally { FloatingAssistantController.operationFinished(operationSucceeded) }
        if (!request.capability.startsWith("operation.")) runCatching { memory.record(request.capability, request.arguments, before, result) }
            .onFailure { android.util.Log.w("JarvisOperationMemory", "Recording unavailable", it) }
        return result
    }

    companion object {
        fun capabilityNames(): List<String> = listOf(
            "device.info", "camera.capture", "accessibility.inspect", "accessibility.click",
            "accessibility.input", "accessibility.global", "app.launch",
            "ocr.read", "notification.read", "root.exec", "accessibility.perform", "accessibility.swipe", "operation.memory", "runtime.tools"
        )
    }

    private fun cameraCapture(): CapabilityResult {
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return CapabilityResult(false, "需要相机权限", JSONObject().put("settings", Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
        }
        return runCatching {
            context.startActivity(Intent(context, CameraCaptureActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            CapabilityResult(true, "已打开相机取样界面，完成后图片写入 Jarvis captures 目录")
        }.getOrElse { CapabilityResult(false, "无法打开相机：${it.message}") }
    }

    private fun accessibilityInspect(): CapabilityResult {
        if (AccessibilityController.service == null) return CapabilityResult(false, "需要启用 Jarvis 无障碍服务", JSONObject().put("settings", Settings.ACTION_ACCESSIBILITY_SETTINGS))
        return CapabilityResult(true, "已读取当前界面节点", AccessibilityController.inspect())
    }

    private fun accessibilityClick(arguments: JSONObject): CapabilityResult {
        if (AccessibilityController.service == null) return CapabilityResult(false, "需要启用 Jarvis 无障碍服务", JSONObject().put("settings", Settings.ACTION_ACCESSIBILITY_SETTINGS))
        val x = arguments.optDouble("x", Double.NaN)
        val y = arguments.optDouble("y", Double.NaN)
        if (x.isNaN() || y.isNaN()) return CapabilityResult(false, "需要 x 和 y 坐标")
        return CapabilityResult(AccessibilityController.click(x.toFloat(), y.toFloat()), "已提交坐标点击")
    }

    private fun accessibilityInput(arguments: JSONObject): CapabilityResult {
        if (AccessibilityController.service == null) return CapabilityResult(false, "需要启用 Jarvis 无障碍服务")
        val text = arguments.optString("text")
        if (text.isBlank() || text.length > 4000) return CapabilityResult(false, "输入文字长度无效")
        return CapabilityResult(AccessibilityController.input(text), "已请求向当前输入框写入文字")
    }

    private fun accessibilityGlobal(arguments: JSONObject): CapabilityResult {
        if (AccessibilityController.service == null) return CapabilityResult(false, "需要启用 Jarvis 无障碍服务")
        val action = arguments.optString("action")
        return CapabilityResult(AccessibilityController.global(action), "已请求执行 $action")
    }

    private fun appLaunch(arguments: JSONObject): CapabilityResult {
        val packageName = arguments.optString("package").trim()
        if (!packageName.matches(Regex("[A-Za-z0-9_.]{3,150}"))) return CapabilityResult(false, "应用包名无效")
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return CapabilityResult(false, "未找到该应用的启动入口")
        val fullRoot = context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE).getInt("rootMode", 1) == 2
        val component = intent.component?.flattenToString()
        if (fullRoot && component != null && component.matches(Regex("[A-Za-z0-9_.$/]+"))) {
            // MIUI can silently reject startActivity while Jarvis operates in the background.
            // Resolve through PackageManager and explicitly target this Android user.
            val launched = rootExec("am start --user ${Process.myUid() / 100000} -n '$component'", true)
            return if (launched.ok && !launched.message.contains("Error:")) {
                CapabilityResult(true, "已请求在当前 Android 用户启动 $packageName；请检查界面确认")
            } else CapabilityResult(false, "启动失败：${launched.message}")
        }
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            CapabilityResult(true, "已启动 $packageName")
        }.getOrElse { CapabilityResult(false, "启动失败：${it.message}") }
    }

    private fun accessibilityOcr(): CapabilityResult {
        if (AccessibilityController.service == null) return CapabilityResult(false, "需要启用 Jarvis 无障碍服务后才能读取界面文字", JSONObject().put("settings", Settings.ACTION_ACCESSIBILITY_SETTINGS))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val imageResult = AccessibilityController.screenshotOcr()
            if (!imageResult.has("error")) return CapabilityResult(true, "已完成当前屏幕中文 OCR", imageResult)
        }
        return CapabilityResult(true, "当前版本使用无障碍节点文字作为 OCR 前置结果", AccessibilityController.inspect().optJSONArray("nodes")?.let { JSONObject().put("nodes", it) } ?: JSONObject())
    }

    private fun notificationRead(): CapabilityResult {
        if (!NotificationController.active) return CapabilityResult(false, "需要启用 Jarvis 通知访问", JSONObject().put("settings", Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        return CapabilityResult(true, "已读取最近通知", JSONObject().put("notifications", NotificationController.list()))
    }

    private fun rootExec(command: String, fullRoot: Boolean): CapabilityResult {
        if (!fullRoot || command.isBlank()) return CapabilityResult(false, "root 命令需要先在设置中开启完全模式")
        return runCatching {
            val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val output = StringBuilder()
            val reader = Thread {
                process.inputStream.bufferedReader().use { stream ->
                    val buffer = CharArray(1024)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        if (output.length < 12000) output.append(buffer, 0, minOf(count, 12000 - output.length))
                    }
                }
            }.apply { start() }
            val finished = process.waitFor(10, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
            reader.join(1000)
            CapabilityResult(finished && process.exitValue() == 0, output.toString().ifBlank { "root 命令已执行" })
        }.getOrElse { CapabilityResult(false, "root 执行失败：${it.message}") }
    }
}
