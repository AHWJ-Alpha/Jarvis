# Jarvis 0.0.8：Android 兼容与自动化工具实测

测试时间：2026-10-26，Android user 11，设备 `1f5244c1`（Redmi M2007J3SC / apollo）。

## 修复

- OpenClaw 2026.9.7 的 cron 进程身份检查默认只接受 Linux。内置 Android Node 返回 `android`，因此 cron 无法为任务创建持久执行凭据。Android 兼容覆盖层将严格匹配的上游平台判断扩展为 Linux 或 Android；不匹配预期源码时会停止启动，避免静默套用错误补丁。
- OpenClaw 的 HTTP `/tools/invoke` 有一份独立于模型 `tools.alsoAllow` 的高风险工具拒绝列表。仅给 `tools.alsoAllow` 加 `automations` 仍会返回 404。Gateway 配置现显式允许该 owner 控制面工具，同时把它加入默认智能体工具白名单。
- 诊断脚本的清理阶段现在会在 OpenClaw 任务/会话清理报错后继续删除专用标记文件。

## 验证结果

- `:app:assembleDebug` 成功；APK 安装至 user 11，系统报告版本 `0.0.8`。
- 设备上的 Android 兼容文件已包含 Android 平台分支；运行时当前 Gateway 正常。
- `/tools/invoke` 的 `automations` `status` 调用返回 HTTP 200、`ok=true`；`cron.status` 显示 `enabled=true`、`triggersEnabled=true`，共有 3 个任务。
- 通过 `/v1/chat/completions` 向默认智能体提出只读状态查询，返回 HTTP 200，并给出 `enabled=true`、`triggersEnabled=true`、`jobs=3`。这确认聊天入口能够使用自动化工具。
- 增加一条仅执行 `id` 和写入时间戳的一次性诊断任务。任务到点后 `lastRunStatus=ok`、运行记录 `completionStatus=succeeded`；它调用 Jarvis root 桥得到 `uid=0(root)`。未操作任何第三方应用或账户。
- 已删除诊断任务及唯一标记文件，原有 3 个任务仍在。OpenClaw 在删除响应中另报告该任务的 transcript archive export 有待处理；任务删除已提交，列表确认该任务不存在。此归档提示不影响本次任务执行验证。

## 范围

本次证实的是 Gateway 自动化状态查询、模型聊天工具可用，以及到点后台任务可通过 Jarvis 桥执行一个 root 命令。它不证明息屏节电策略、重启恢复、任意外部插件安装、QQ 通知自动答复、相机权限或所有设备接口已具备生产级可靠性。

未在报告或命令输出中保存 API key、Gateway token 或设备桥接 token。
