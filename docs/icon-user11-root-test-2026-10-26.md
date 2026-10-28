# 本地图标、第二空间安装与 root 实测

测试日期：2026-10-26。设备：Redmi M2007J3SC（apollo），ADB 序列号 `1f5244c1`。

## 图标与构建

- 使用电脑上的 A1111 WebUI 与 Kohaku XL epsilon rev1（SDXL）本地生成 768 × 768 的头像，文件为 `artwork/jarvis-sdxl-local.png`。
- 修复原先仅有 2 字节的损坏图标，将生成图片写入 `app/src/main/res/drawable/jarvis_user_icon.png`；补全旧版 Android 的普通及圆形图标资源。Android 8 以上继续使用引用此图片的自适应图标。
- `gradlew.bat :app:assembleDebug --no-daemon` 构建成功。APK：`app/build/outputs/apk/debug/app-debug.apk`，版本为 0.0.3（versionCode 3）。
- 覆盖安装保留应用数据，并使用 `cmd package install-existing --user 11 com.jarvis.app` 注册到手机第二空间。
- SukiSU 应用列表显示新图标。第二空间桌面最初仍使用旧的默认图标，重启该用户的 `com.miui.home` 后显示正确；未清除桌面数据。截图：`artwork/jarvis-user11-home-page2.png`。
- 从桌面点击新图标，确认恢复的活动是 `u11 com.jarvis.app/.MainActivity`，界面有正常内容。截图：`artwork/jarvis-user11-ready.png`。

## root 与 OpenClaw

- 在 SukiSU Ultra 的 Jarvis USER 11 配置页开启“超级用户”，界面确认开关已选中。
- 在 Jarvis 设置中选择 root“完全”模式，等待三秒警告结束后确认；界面显示“Jarvis · 完全模式”。
- Jarvis 自身的 `root.exec` 接口执行只读命令 `id`，返回 `ok: true` 和 `uid=0(root) gid=0(root) groups=0(root) context=u:r:ksu:s0`。
- 第二空间的应用审计日志记录 `capability=root.exec`、`fullRoot=true`。
- 第二空间 Gateway 进程的 UID 为 `1110305`，对应 `/data/user/11/com.jarvis.app`，不是 ADB shell 的进程。
- 使用第二空间私有 Gateway Token 请求 `/v1/models`，返回 HTTP 200；请求 `/tools/invoke` 调用 `jarvis_root_exec` 并执行 `id`，同样返回 `uid=0`。
- OpenClaw 工具调用结果保存在 `artwork/user11-openclaw-root-test.json`，不含鉴权 Token。
- 临时 ADB 测试端口转发已移除；本地生图服务在生成完成后关闭。

## 当前限制

- 两个用户空间共用同一手机的网络端口，目前 Bridge 与 Gateway 固定使用 18791、18789。两个空间同时运行会争用端口。本轮已停止主空间 Jarvis 后台实例，让第二空间单独运行；没有删除主空间应用或数据。当前版本应只在一个空间保持 Gateway 运行。
- 本轮验证范围为图标、第二空间安装与启动、Gateway 健康检查和 root 工具链；没有重新验证 QQ 收发、自动回复、模型联网对话和全部软硬件能力。root 授权成功不能代替各项能力的逐项测试。
