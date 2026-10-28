# Jarvis 对话操作 Pixiv 下载实测

日期：2026-10-26。设备：Redmi M2007J3SC，Android 第二空间 User 11，Jarvis 完全模式，已启用无障碍。

## 结论

本次单次流程通过：从 Jarvis 对话框发送任务，内置 OpenClaw 调用设备桥接工具打开 Pixiv、浏览作品，并使用全屏图片菜单的保存功能下载一张图片。随后返回 Jarvis，界面实际显示了完成回复。

测试人员通过 ADB 输入并发送 Jarvis 对话任务；重试任务运行期间只读取日志、界面和文件，不代替智能体点击 Pixiv 的保存按钮。测试结束后独立拉取图片验证。

## 下载结果与独立校验

- 文件：`/data/media/11/Pictures/pixiv/illust_151298651_20261026_131234.png`。
- 手机第二空间中对应 `Pictures/pixiv` 目录，可在相册或文件管理器查看。
- 大小：2,985,800 字节，约 2.85 MiB。
- 格式与实际尺寸：PNG，1672 × 941，RGB；Pillow `verify()` 成功，并已查看图片内容。
- SHA256：`e871ee8f02b92e9ec16cb1a0e89d6d396a3db6093f736ee3f657352e97c33602`。
- 重试前后对比：Pixiv 目录从 3 个文件变为 4 个，仅新增上述一个图片文件。智能体还删除了上一轮留下的 `Download/claw_screen.png` 临时截图。
- 电脑副本：`artwork/pixiv-downloaded-151298651.png`。

## 本轮修复及构建

第一轮能搜索作品，但 HTTP 连接在三分钟后超时，Gateway 日志明确记录 `HTTP client disconnected`，该轮未完成下载。

为继续测试，修改并覆盖安装 Jarvis 0.0.4 / code 4，保留应用数据：

- 对话读取超时从三分钟延长到十五分钟，允许多轮设备操作。
- 无障碍结果增加控件资源 ID、屏幕范围，过滤不可见节点，并启用资源 ID 报告。
- 设备信息增加 Android 用户 ID 和外部存储目录，减少把主空间存储误当第二空间的问题。
- 完全模式下启动应用显式指定当前 Android 用户，处理后台启动限制。
- 工具说明补充正确参数、根据当前节点点击、第二空间文件核实方法；内置覆盖文件同步更新。

`gradlew.bat :app:assembleDebug --no-daemon` 构建成功，存在原有图标 API 和 Gradle 弃用警告；ADB 覆盖安装成功。

APK：`app/build/outputs/apk/debug/app-debug.apk`。

APK SHA256：`F0A2C7C0210B89455C4F7EA720B4929A21B3ED72ECC993D73F33474CF275E371`。版本号本轮未递增，使用此哈希区分构建。

第二空间模型连接原先为空。本轮仅填充空白的 Base URL、API Key、模型字段，复用主空间已验证的连接；人格与其他设置保留。原配置备份存放在设备应用私有目录，报告不包含密钥。

## 第二轮证据

任务从 Jarvis 对话界面发送，工具操作约在 13:10:24 至 13:12:52 之间；Gateway 在 13:12:57 记录本轮正常结束。

此区间审计记录共 83 次设备调用：`device.info` 1 次、`app.launch` 1 次、`accessibility.inspect` 33 次、`accessibility.click` 29 次、`accessibility.input` 2 次、`root.exec` 17 次。测试人员准备阶段的输入和结束后的读取不计入这些次数。

- 任务文本：`artwork/pixiv-chat-retry-prompt.txt`。
- 下载前后目录：`artwork/pixiv-download-files-before-retry.txt`、`artwork/pixiv-download-files-after-retry.txt`。
- 机器可读校验：`artwork/pixiv-chat-test-result.json`。
- 审计：`artwork/pixiv-capability-audit.jsonl`。
- Jarvis 实际回复：`artwork/jarvis-pixiv-chat-result.json`、`artwork/jarvis-pixiv-chat-result.png`。
- Pixiv 图片界面截图：`artwork/jarvis-pixiv-observe.png`。

测试结束已恢复 User 11 原来的自动旋转设置；主空间 Jarvis 仍停止，以避免两个空间占用相同 Gateway 端口。

## 尚存的问题

下载成功不代表回复中的所有描述都准确。智能体报告图片尺寸为 616 × 939，但独立读取真实文件得到 1672 × 941；报告中作品名「91」及作者 kaku 也未独立核实，不作为已确认信息。

操作过程存在绕路和误触。智能体报告曾打开退出确认框并取消；保存文件已经独立确认，但尚不能据此声称它能稳定完成所有 Pixiv 操作。

Gateway 日志出现上下文预算告警，智能体尝试写记忆时调用了不存在的 `write` 工具。此次下载仍正常结束，长任务的上下文管理、结构化结果校验及记忆工具接通仍需完善。

本轮不包含登录、购买、点赞、关注或消息发送测试，也未推送 GitHub。

清理临时调试数据库的命令被自动审批拒绝，原因仅返回 `blocked by policy`。电脑 `%TEMP%/jarvis-pixiv-test.sqlite` 与手机 `/data/local/tmp/jarvis-pixiv-test.sqlite`、对应 WAL 临时副本尚未清理；这些不属于应用正常运行所需文件。下载图片本身保留在上述 Pixiv 目录。
