# Jarvis

Jarvis 是面向已 root Android 手机的本地 AI 代理。当前仓库处于第一步骨架阶段，使用 Kotlin、Jetpack Compose 和 Android Gradle Plugin。

OpenClaw upstream is pinned to `2026.9.7` at commit `c074824a27c96d3983043f9eeb33823cd1772d8c` under the MIT license. Its Gateway requires Node `>=24.16.0 <25` or `>=26.1.0`, pnpm, and native npm dependencies. Jarvis will prefer an embedded arm64 runtime and fall back to a Termux Gateway when the device cannot run the embedded runtime.

## 打开与构建

用 Android Studio 打开 `D:\Jarvis`，等待 Gradle 同步后运行 `app` 配置。命令行构建：

```powershell
$env:JAVA_HOME = "D:\JDK"
$env:ANDROID_SDK_ROOT = "C:\Users\lenovo\AppData\Local\Android\Sdk"
.\gradlew.bat :app:assembleDebug --no-daemon
```

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`

## 当前首步范围

- Jarvis 主界面和四个入口：对话、任务、工作区、设置。
- 本地配置保存：限制模式、root 模式、QQ 命令通道开关和命令前缀。
- root 完全模式的三秒警告弹窗。
- 模型 Base URL 和模型名配置占位。
- OpenClaw version pin, private runtime directories, Bridge Token, root probe, and plugin directory bootstrap.
- Plugin manifest parsing, HTTPS download, and SHA-256 verification.
- 设计基线见 `docs/openclaw-root-agent-design.md`。

## 后续实现顺序

1. Android Keystore、模型网关和流式对话。
2. 全局/工作区人格、世界书、示例和记忆。
3. Tool Broker、审计日志和权限确认状态机。
4. NotificationListener、Accessibility、OCR/CV 和通用应用自动化。
5. QQ 命令通道与普通消息分流。
6. 受限 `su` helper、标准/完全 root 模式和后台任务。
