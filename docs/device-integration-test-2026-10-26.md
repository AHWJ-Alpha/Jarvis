# Jarvis 0.0.3 实机接入记录

测试设备：Redmi M2007J3SC，Android 17 / API 37，arm64。使用 `adb install -r` 覆盖安装，应用数据保留。

## 已验证

- APK 内置 OpenClaw 2026.9.7 与 Node 24.18.0；Gateway 以 Jarvis 应用 UID 启动，`/v1/models` 返回 HTTP 200。
- Gateway 加载 Jarvis 设备插件；`jarvis_device_info` 返回机型、品牌、Android 版本和 SDK。
- `jarvis_app_launch` 打开系统设置；启用 Jarvis 无障碍后，`jarvis_accessibility_inspect` 读取 40 个界面节点，`jarvis_accessibility_global` 执行返回动作，`jarvis_accessibility_input` 向 Jarvis 聊天输入框写入文字。
- 模型通过 Gateway 主动调用 `jarvis_device_info`，返回 `M2007J3SC` 与 Android 17；同样的结果在 Jarvis 聊天 UI 显示。
- 已从手机 AI Classroom 现有配置填入 Jarvis 模型设置；测试结束后 Gateway 模型地址恢复为 `https://api.deepseek.com/v1`。

## 当前限制

- 手机 VPN `com.github.kr328.clash` 处于 suspended 状态，手机 DNS 无法解析模型域名。模型链路验证使用临时 ADB 端口转发到电脑的模型中继；测试后已停止中继并移除转发。脱离 USB 后需先恢复手机网络，模型对话才能继续。
- 手机的 `adb shell su` 可用，但 Jarvis 进程检测不到 root。本次 AI Classroom 私有配置通过 ADB 辅助写入；应用内自动导入与 `jarvis_root_exec` 仍需授予 Jarvis 应用 root 后验证。
- QQ 收发、相机采样、OCR 识别、插件下载和 16 KB 页大小设备未在本轮验证。

构建命令：`./gradlew.bat :app:assembleDebug --no-daemon`。产物：`app/build/outputs/apk/debug/app-debug.apk`。
