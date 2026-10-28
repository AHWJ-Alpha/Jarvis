# Jarvis 0.0.4 图标与配置保存实测

日期：2026-10-26。设备：Redmi M2007J3SC / apollo，序列号 `1f5244c1`。

## 修改与安装

- 在设置页顶部固定显示“保存并应用”和“保存并验证”，竖屏和横屏均可见。
- 原有输入自动保存继续保留。显式保存检查 SharedPreferences 持久化结果，再同步配置并启动或更新内置 OpenClaw；失败会分别显示保存、Gateway 或模型连接问题。
- 验证执行一次真实的 Chat Completions 请求，按当前主模型或已启用的思考模型选择测试目标。未返回模型文本不会被当成验证成功；测试请求不携带设备工具。
- 增加设置页版本号，便于核对手机运行的版本。
- `gradlew.bat :app:assembleDebug --no-daemon` 构建成功，只有已有弃用警告。版本为 0.0.4 / versionCode 4。
- 覆盖安装 `app/build/outputs/apk/debug/app-debug.apk` 成功，未卸载、未清除应用数据。

## 两个空间的桌面图标

此前第二空间已刷新，但主空间桌面仍保留默认安卓图标。本轮重启各空间的 MIUI 桌面进程，未清除桌面数据；两个空间均实际显示本地生成的动漫头像。

- 主空间截图：`artwork/jarvis-owner-home-004.png`；局部：`artwork/jarvis-owner-icon-004.png`。
- 第二空间截图：`artwork/jarvis-user11-page2-004.png`；局部：`artwork/jarvis-user11-icon-004.png`。

## 主空间 User 0

- 竖屏和横屏均实测按钮可见，截图为 `artwork/jarvis-settings-portrait-004.png` 和 `artwork/jarvis-settings-004.png`。
- 点击“保存并应用”：显示“配置已保存并应用，OpenClaw 已就绪。”
- 点击“保存并验证”：显示“配置已保存并应用。主模型验证成功：已收到实际测试回复。”截图：`artwork/jarvis-save-verified-004.png`。
- 读取持久配置后，仅比较结果：Base URL、API Key、当前模型均与生成的 Gateway 配置一致。没有把密钥写入测试报告。
- 强制停止并重开应用后，持久设置的 SHA256 一致，配置保留。
- 使用该空间私有鉴权 Token 请求 Gateway：`/v1/models` 返回 HTTP 200；`/v1/chat/completions` 实际返回 `OK`。临时 ADB 转发已移除。
- 机器可读结果：`artwork/settings-save-test-004.json`。

## 第二空间 User 11

- 正常启动 0.0.4，保存按钮可见，原“完全模式”设置保留。
- 此空间的 Jarvis 表单尚未填写 Base URL、API Key 和模型名；两个空间的应用设置分别保存。
- 点击“保存并验证”后，明确显示“配置已保存并应用。请填写 Base URL 后重试。”未声称模型验证成功。
- 截图：`artwork/jarvis-user11-save-result-004.png`；结果：`artwork/settings-save-user11-test-004.json`。

## 范围与当前限制

- 本轮没有移植 QQ 插件，也没有发送 QQ 消息或开启自动回复。QQ 方案见 `docs/qq-integration-direction.md`。
- Gateway / Bridge 端口仍固定为 18789 / 18791，两个空间暂不能同时运行 Gateway。测试第二空间前已停止主空间 Jarvis；本轮结束时手机停留在第二空间设置页。
- 主空间测试期间临时锁定竖屏，完成后已恢复原来的自动旋转设置。
