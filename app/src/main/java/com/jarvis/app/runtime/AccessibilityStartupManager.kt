package com.jarvis.app.runtime

import android.content.ComponentName
import android.content.Context
import android.os.Process
import android.provider.Settings
import java.util.concurrent.TimeUnit

class AccessibilityStartupManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE)
    private val component = ComponentName(appContext, JarvisAccessibilityService::class.java)

    var autoConnect: Boolean
        get() = prefs.getBoolean("autoConnectAccessibility", false)
        set(value) { prefs.edit().putBoolean("autoConnectAccessibility", value).apply() }

    fun isEnabled(): Boolean {
        val services = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val listed = services.orEmpty().split(':').any { ComponentName.unflattenFromString(it) == component }
        return listed && Settings.Secure.getInt(appContext.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
    }

    fun connectWithRoot(): String {
        if (isEnabled()) return "Jarvis 无障碍已启用"
        if (prefs.getInt("rootMode", 1) != 2) return "请先在 root 模式中选择完全模式，或手动开启无障碍"

        val userId = Process.myUid() / 100000
        val current = shell("settings --user $userId get secure enabled_accessibility_services")
            ?: return "无法读取当前用户的无障碍设置；请检查 root 授权"
        val services = current.trim().takeUnless { it == "null" || it.isEmpty() }
            ?.split(':')?.filter { it.isNotBlank() }.orEmpty()
        val updated = (services.filterNot { ComponentName.unflattenFromString(it) == component } + component.flattenToString()).joinToString(":")
        val escaped = updated.replace("'", "'\\''")
        if (shell("settings --user $userId put secure enabled_accessibility_services '$escaped'") == null ||
            shell("settings --user $userId put secure accessibility_enabled 1") == null) {
            return "root 自动连接失败；请手动开启 Jarvis 无障碍服务"
        }
        return if (isEnabled()) "已启用 Jarvis 无障碍；服务正在连接" else "系统未确认无障碍已启用；请手动检查"
    }

    private fun shell(command: String): String? = runCatching {
        val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        if (!process.waitFor(8, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return null
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (process.exitValue() == 0) output else null
    }.getOrNull()
}
