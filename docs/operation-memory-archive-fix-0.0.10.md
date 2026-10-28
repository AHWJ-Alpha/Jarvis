# Jarvis 0.0.10：操作记忆与归档兼容修复

测试设备：`1f5244c1`，Android user 11。时间：2026-10-26。

## 问题与根因

1. 0.0.8 的 `OperationMemoryTest` 共 14 项、7 项失败。操作记忆写入时用 `File.renameTo()` 覆盖现存 JSON 文件；该方法不保证跨平台覆盖，Windows 测试环境覆盖失败。保存、更新失效状态和运行结果因此连带失败。现在改用同目录临时文件、先同步内容，再用 NIO 原子替换；仅在文件系统不支持原子移动时回退到普通替换，失败时清理临时文件并保留原文件。
2. OpenClaw 2026.9.7 将会话归档从 SQLite 发布到文件时调用 `fs.linkSync`。此设备的 Android 应用私有目录对硬链接返回 `EACCES`，因此任务虽已删除，归档仍滞留在 SQLite。Android 兼容覆盖层现在仅在 `EACCES/EPERM` 时用排他复制、同步及上游现有的 SHA-256 校验发布归档，其他错误照常上报。补丁严格匹配当前固定版本的模块内容，版本变化会明确失败。
3. 诊断清理脚本现在即使 OpenClaw 在删除任务后的归档步骤报错，也会清除唯一临时标记，并再次查询任务列表，分别报告任务删除与归档错误。

## 验证

- `:app:testDebugUnitTest` 全部 14 项通过；`:app:assembleDebug` 成功。
- 0.0.10 已安装并启动于 user 11。
- 实机使用唯一临时路径完成首次保存、覆盖保存、读取 revision=2、删除；均成功，未改动已有路径。
- 安装修复前，两次诊断任务都成功执行 root 命令，但删除时归档分别报告 1、2 项待导出。日志显示确切底层错误为 `EACCES: permission denied, link ...`。
- 安装修复后，前两条挂起的 `.jsonl.deleted.*.zst` 归档已出现在会话目录；新的定时诊断任务到点执行成功，删除返回 `cleanedUp=true`、`testJobRemoved=true`、`cleanupJobError=null`。

此验证只覆盖本设备、当前 OpenClaw 固定版本及上述执行路径，不代表所有硬件接口或第三方应用流程均已测试。诊断结果未保存 API key 或 Gateway token。
