package com.jarvis.app.runtime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.Manifest
import android.os.Build
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
 * Narrow bridge surface exposed to OpenClaw. Implementations for camera,
 * accessibility and OCR are added behind this contract so the model never
 * receives unrestricted Android or root process access.
 */
class DeviceCapabilityBridge(private val context: Context) {
    private val auditLog = File(context.filesDir, "jarvis/logs/capability-audit.jsonl")

    fun execute(request: CapabilityRequest, approved: Boolean = false): CapabilityResult {
        if (request.requiresApproval && !approved) return CapabilityResult(false, "需要用户确认：${request.capability}")
        auditLog.parentFile?.mkdirs()
        auditLog.appendText(JSONObject().apply {
            put("capability", request.capability)
            put("risk", request.risk.name)
            put("approved", approved)
            put("timestamp", System.currentTimeMillis())
        }.toString() + "\n")
        return when (request.capability) {
            "device.info" -> CapabilityResult(true, "设备信息可用", JSONObject().put("package", context.packageName))
            "root.exec" -> rootExec(request.arguments.optString("command"), approved)
            "camera.capture" -> cameraCapture()
            "accessibility.inspect" -> accessibilityInspect()
            "accessibility.click" -> accessibilityClick(request.arguments)
            "ocr.read" -> accessibilityOcr()
            "notification.read" -> notificationRead()
            else -> CapabilityResult(false, "未知设备能力：${request.capability}")
        }
    }

    companion object {
        fun capabilityNames(): List<String> = listOf(
            "device.info", "camera.capture", "accessibility.inspect", "accessibility.click",
            "ocr.read", "notification.read", "root.exec"
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

    private fun rootExec(command: String, approved: Boolean): CapabilityResult {
        if (!approved || command.isBlank()) return CapabilityResult(false, "root 命令需要明确确认")
        return runCatching {
            val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val finished = process.waitFor(10, TimeUnit.SECONDS)
            val output = process.inputStream.bufferedReader().readText().take(12000)
            if (!finished) process.destroyForcibly()
            CapabilityResult(finished && process.exitValue() == 0, output.ifBlank { "root 命令已执行" })
        }.getOrElse { CapabilityResult(false, "root 执行失败：${it.message}") }
    }
}
