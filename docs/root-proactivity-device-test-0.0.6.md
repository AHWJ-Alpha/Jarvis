# Jarvis 0.0.6：root 与主动执行实机验证

测试时间：2026-10-26，约 14:31—14:42（北京时间）。

## 结论

当前 Android 用户中的 Jarvis 已获得可用的 root 授权，应用内“完全模式”已开启（rootMode=2）。通过内置 OpenClaw Gateway 的 `jarvis_root_exec` 调用，实际返回 `uid=0(root)`。主动执行测试失败：定时任务到期未运行，返回前台仍未执行；手动触发同一任务也遇到 Android 兼容错误。因此不能将当前版本描述为已经具备可靠的后台主动助理能力。

本轮只做诊断并保存证据，没有修改生产代码、重编 APK 或发布新版本。

## 测试设备和权限链

- Redmi M2007J3SC / apollo，Android 17（SDK 37），ADB serial `1f5244c1`。
- 当前 Android user 为 11，Jarvis UID 为 1110305，包名 `com.jarvis.app`。
- 手机使用 SukiSU；测试开始时授权已经存在，无需重复授予。测试结束时完全模式仍开启。
- 使用已安装的 Jarvis 0.0.6、内置 OpenClaw 2026.9.7、Android arm64 Node 24.18.0。
- 操作链为：测试客户端 → 内置 Gateway → Jarvis 插件 → 应用原生桥 → 应用发起 `su -c`。ADB 用于连接、读取测试所需凭据和独立核对结果；root 成功结论来自应用自己的工具调用。
- root 响应：`uid=0(root) gid=0(root) groups=0(root) context=u:r:ksu:s0`。

## root 测试结果

| 测试 | 实际核验 | 结果 |
| --- | --- | --- |
| root 身份 | Gateway 调用 `id`，返回 UID 0 | 通过 |
| 受保护文件读写 | 创建唯一临时目录 0700，写入 root 所有的 0600 文件，核对 UID、权限、长度及随机标记 | 通过 |
| 系统设置修改 | 在 user 11 写入唯一 secure 测试键，读回后立即删除，独立读取为 null | 通过 |
| root 截屏 | `screencap -p` 生成文件，校验 PNG 八字节签名 | 通过 |
| 跨应用启动 | root 启动 user 11 的系统设置，无障碍检查确认前台包名 | 通过 |
| 返回 Jarvis | root 启动 Jarvis，确认前台包名 | 通过 |

这些测试证明当前工具链可执行上述 root 操作。摄像头、麦克风、其他应用全部业务功能以及所有硬件接口并未逐项测试，不能由 UID 0 推断其全部可用。Node/Gateway 本身仍以应用 UID 运行，需要权限的动作通过 root 桥执行。

## 主动执行测试

1. Gateway `cron.status` 显示 enabled=true、triggersEnabled=true，原有三项系统任务。
2. 添加唯一一次性测试任务，预定北京时间 14:36:20 执行。任务仅允许模型调用 `jarvis_root_exec`，将 `id` 和时间写入唯一临时文件；不操作第三方账号。
3. 将 Jarvis 切到后台，确认前台为 MIUI 桌面。手机保持唤醒，Gateway 可以持续响应状态查询。
4. 到期后持续检查：执行历史为空，任务未进入 running，标记文件未创建。
5. 14:40 左右恢复 Jarvis 前台；14:41:36 再次核对，历史仍为空。
6. 为区分定时唤醒和执行链问题，单独手动调用 `cron.run`。这不是主动执行成功，实际返回：

```text
UNAVAILABLE
Error: cron run cannot acquire a durable fence without process start identity
```

已定位一个确定的兼容阻塞点：内置 `dist/pid-alive-BK4A222T.mjs` 的 `getProcessStartTime` 对 `process.platform !== "linux"` 直接返回 null，而内置 Node 上报 `android`。`prepareCronRunReceiptClaim` 在进程启动身份为 null 时拒绝获取执行锁。需要为 Android 使用真实 `/proc/<pid>/stat` 的启动身份，并保留进程复用校验。定时器未运行是否还有其他原因，需要修复后再验证，不能仅凭此错误断言已经定位全部原因。

## 尚未接通的主动能力

- `GatewayConfig.kt` 当前将工具策略写为 minimal，仅额外开放 `jarvis-device-bridge`。实测 HTTP 调用 `automations` 返回 404 / `Tool not available: automations`，聊天模型不能据此自行安排任务。
- Jarvis 未开启当前用户的通知访问。现有通知监听代码也只是缓冲通知，没有接入“事件 → 模型 → 操作”的处理链，不能据此宣称支持新消息自动处理。
- 任务页的部分按钮仍为空操作。
- 前台服务使用 START_STICKY，但尚无开机恢复、运行时看护、持久任务恢复等完整路径；没有独立验证息屏、重启或进程被杀后的执行。

接下来应先修复进程身份兼容、复测真正到点执行，再开放自动化工具并接通通知/任务界面，最后测试息屏和重启恢复。root 授权已经不是当前主要障碍。

## 清理和证据

已移除本次一次性任务和所有测试临时文件/设置键，保留原有三项系统任务，确认临时标记不存在。测试结束时 Jarvis 在前台；未发送任何 QQ 或其他第三方消息。

- 原始结果：`artwork/root-proactivity-test.json`（不含 API key 或桥接 token）。
- 复测脚本：`tools/test-root-proactivity-device.py`。
- 测试任务 ID：`5694be5b-72d5-4ca9-aab4-fc45c45a3dd2`（已删除）。

脚本的 `root`、`status` 可用于复核；`schedule` 会建立临时任务，必须随后执行 `result` 和 `cleanup`。已有证据文件保留历史 jobId，重新安排测试请使用新的 `--state` 文件。
