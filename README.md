# Jarvis

Jarvis 是面向已 root Android 手机的本地 AI 代理。当前仓库处于第一步骨架阶段，使用 Kotlin、Jetpack Compose 和 Android Gradle Plugin。

OpenClaw upstream is pinned to `2026.9.7` at commit `c074824a27c96d3983043f9eeb33823cd1772d8c` under the MIT license. Its Gateway requires Node `>=24.16.0 <25` or `>=26.1.0`, pnpm, and native npm dependencies. Jarvis will prefer an embedded arm64 runtime and fall back to a Termux Gateway when the device cannot run the embedded runtime.

## 内置运行时构建契约

要生成真正的单 APK 运行版本，需要将经过 Android arm64 验证的归档放到：

```text
app/src/main/assets/openclaw/runtime.bundle.zip
```

归档必须包含以下文件，并且 `node` 是 Android bionic 可执行文件：

```text
node                  # Android arm64 Node.js >=24.16
openclaw.mjs          # OpenClaw 2026.9.7 入口
node_modules/         # 在 Android arm64 上安装/编译后的依赖
```

Jarvis 首次启动会安全解压该归档到 `files/jarvis/runtime`，校验路径不允许越界，并启动：

```text
node openclaw.mjs gateway --bind loopback
```

当前 APK 已携带一个约 223 MB 的 Android arm64 候选归档，包含官方 Termux Node.js 24.18.0 bionic 二进制、运行库、OpenClaw 2026.9.7 和依赖。启动器会自动解压并优先启动它；如果实机上的原生依赖或动态链接失败，设置页仍提供 Termux 备用部署。该候选包需要在至少一台 Android arm64 设备上完成 Gateway 健康检查后，才能作为正式发布包。

重新获取并组装候选归档：

```powershell
.\tools\fetch-android-runtime.ps1
```

该脚本固定下载 URL 和 SHA-256，使用官方 Termux aarch64 Node 包；OpenClaw 依赖在 Windows 上只用于组装，最终的 `node-pty`、`koffi` 等原生模块仍必须在 Android arm64 真机验证。归档约 223 MB，建议通过 Git LFS 或 Release artifact 分发。

OpenClaw 访问 Android 软硬件通过 `DeviceCapabilityBridge` 的能力协议完成。摄像头、无障碍、OCR、通知和 root helper 必须分别授予 Android 权限，并经过 Jarvis 的风险确认和审计日志；root 权限本身不会绕过 Android 沙盒、SELinux 或硬件专属权限。

内置归档还包含 `jarvis-device-bridge` OpenClaw 插件。Gateway 启动时通过 `openclaw-home/openclaw.json` 加载它，模型工具列表会出现 `jarvis_device_info`、`jarvis_camera_capture`、`jarvis_accessibility_inspect`、`jarvis_accessibility_click`、`jarvis_ocr_read`、`jarvis_notification_read` 和 `jarvis_root_exec`，每个调用都经过本地 Bridge。

当前已接通的能力：

- `device.info`：设备桥接健康检查。
- `camera.capture`：请求相机权限并调起系统相机，图片保存到 Jarvis 私有 captures 目录。
- `accessibility.inspect`：读取当前无障碍节点文字。
- `accessibility.click`：通过无障碍手势提交坐标点击。
- `ocr.read`：使用无障碍节点文字作为无图像 OCR 的前置结果。
- `notification.read`：读取 Jarvis 通知监听服务缓存的最近通知。
- `root.exec`：仅在明确批准后执行，并写入审计日志。

纯图像 OCR、QQ 专用适配器和更细粒度的 root 工具仍需按设备和应用逐项验证，不能仅靠一个 root 开关保证所有软件兼容。

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
- OpenAI-compatible 对话模型请求，支持模型回退链、API Key、全局提示词和世界书。
- 独立识图模型配置，Base URL、API Key 和模型名为空时继承对话模型。
- 深度思考开关和 `off`、`minimal`、`low`、`medium`、`high`、`max` 六档 `reasoning_effort`。
- AI Classroom 兼容 JSON 导入/导出；可直接导入其 classes JSON 的第一个课堂配置。
- OpenClaw version pin, private runtime directories, Bridge Token, root probe, and plugin directory bootstrap.
- Plugin manifest parsing, HTTPS download, and SHA-256 verification.
- 设计基线见 `docs/openclaw-root-agent-design.md`。

## 后续实现顺序

1. Android Keystore、模型网关流式响应和视觉请求在设备能力桥中的进一步编排。
2. 全局/工作区人格、世界书、示例和记忆。
3. Tool Broker、审计日志和权限确认状态机。
4. NotificationListener、Accessibility、OCR/CV 和通用应用自动化。
5. QQ 命令通道与普通消息分流。
6. 受限 `su` helper、标准/完全 root 模式和后台任务。
