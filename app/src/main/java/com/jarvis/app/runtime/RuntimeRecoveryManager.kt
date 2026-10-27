package com.jarvis.app.runtime

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.os.Build
import java.io.File

enum class RecoveryAction { REINSTALL_EMBEDDED, SETUP_TERMUX }

data class RecoveryResult(val success: Boolean, val message: String, val action: RecoveryAction)

class RuntimeRecoveryManager(private val context: Context) {
    private val root = File(context.filesDir, "jarvis")
    private val runtime = File(root, "runtime")
    private val config = File(root, "config")
    private val plugins = File(root, "plugins")
    private val workspaces = File(root, "workspaces")
    private val logs = File(root, "logs")

    fun reinstallEmbeddedPreservingData(): RecoveryResult {
        val action = RecoveryAction.REINSTALL_EMBEDDED
        return runCatching {
            val backup = File(root, "runtime.backup-${System.currentTimeMillis()}")
            if (runtime.exists() && !runtime.renameTo(backup)) error("无法备份旧运行时")
            runtime.mkdirs()
            try {
                val report = BootstrapController(context).bootstrap()
                if (report.state != RuntimeState.READY) error(report.message)
                for (name in listOf("openclaw-home", "home")) {
                    val old = File(backup, name)
                    if (old.exists()) {
                        val fresh = File(runtime, name)
                        fresh.deleteRecursively()
                        if (!old.renameTo(fresh)) error("无法恢复 $name")
                    }
                }
                backup.deleteRecursively()
                RecoveryResult(true, "内置 OpenClaw 已重装；配置、会话和工作区数据已保留。", action)
            } catch (error: Exception) {
                for (name in listOf("openclaw-home", "home")) {
                    val restored = File(runtime, name)
                    val old = File(backup, name)
                    if (backup.exists() && restored.exists() && !old.exists()) restored.renameTo(old)
                }
                runtime.deleteRecursively()
                if (backup.exists()) backup.renameTo(runtime)
                throw error
            }
        }.getOrElse { RecoveryResult(false, "修复内置运行时失败：${it.message}", action) }
    }

    fun termuxPackageInstalled(): Boolean = runCatching {
        context.packageManager.getPackageInfo("com.termux", 0)
        true
    }.getOrDefault(false)

    fun buildTermuxSetupCommand(): String {
        val target = "openclaw@2026.9.7"
        val token = BootstrapController(context).bridgeToken()
        return "pkg update -y && pkg install -y nodejs-lts curl git && npm install -g $target && mkdir -p ~/.openclaw && export JARVIS_BRIDGE_URL=http://127.0.0.1:18791 JARVIS_BRIDGE_TOKEN=$token && openclaw gateway --bind loopback"
    }

    fun startTermuxGateway(): RecoveryResult {
        val action = RecoveryAction.SETUP_TERMUX
        if (!termuxPackageInstalled()) return RecoveryResult(false, "未检测到 Termux。请先安装 Termux。", action)
        val command = "if ! command -v openclaw >/dev/null 2>&1; then pkg update -y && pkg install -y nodejs-lts curl git && npm install -g openclaw@2026.9.7; fi; export JARVIS_BRIDGE_URL=http://127.0.0.1:18791 JARVIS_BRIDGE_TOKEN=${BootstrapController(context).bridgeToken()}; openclaw gateway --bind loopback"
        return runCatching {
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                component = ComponentName("com.termux", "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", command))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            }
            context.startService(intent)
            RecoveryResult(true, "已请求 Termux 后台启动 Gateway。返回 Jarvis 后点击重新检查。", action)
        }.getOrElse {
            val commandForUser = buildTermuxSetupCommand()
            context.getSystemService(Context.CLIPBOARD_SERVICE)?.let { service ->
                (service as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Jarvis OpenClaw 安装命令", commandForUser))
            }
            context.packageManager.getLaunchIntentForPackage("com.termux")?.let { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            RecoveryResult(false, "Termux 自动执行被系统拒绝，已打开 Termux 并复制完整安装命令，请粘贴执行：\n$commandForUser", action)
        }
    }

    fun launchTermuxSetup(): RecoveryResult {
        val action = RecoveryAction.SETUP_TERMUX
        if (!termuxPackageInstalled()) {
            return RecoveryResult(false, "未检测到 Termux。请先从 F-Droid 安装 Termux，再回来重试。", action)
        }
        return runCatching {
            val command = buildTermuxSetupCommand()
            val intent = context.packageManager.getLaunchIntentForPackage("com.termux")
                ?: return RecoveryResult(false, "无法打开 Termux，请确认应用已正确安装。", action)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Jarvis OpenClaw 安装命令", command))
            File(config, "runtime-mode").apply { parentFile?.mkdirs(); writeText(RuntimeMode.EXTERNAL_TERMUX.name) }
            RecoveryResult(true, "已打开 Termux。请在 Termux 中粘贴并执行安装命令：\n$command\n安装完成后返回 Jarvis，点击重新检查。", action)
        }.getOrElse { RecoveryResult(false, "无法打开 Termux：${it.message}", action) }
    }

    fun dataDirectories(): List<File> = listOf(config, plugins, workspaces, logs)
}
