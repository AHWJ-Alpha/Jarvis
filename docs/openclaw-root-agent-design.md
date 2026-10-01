# OpenClaw Root 手机代理设计基线

状态：设计稿 v0.4（2026-10-01）

## 1. 产品目标

构建一个只运行在已 root Android 手机上的本地 AI 代理。代理通过统一的工具协议调用应用、文件、网络和硬件，并能维护长期人格、世界书、示例、记忆和任务历史。用户可以用自然语言要求它处理 QQ 消息、采集摄像头样本、检索资料、安装或编写工具、生成图片和语音，以及在授权范围内创建 Android 程序。

“root”只代表系统能力更强，不代表所有动作默认放行。每个工具都必须声明权限范围、风险级别、输入输出和审计策略；高风险工具采用预览、确认、执行三步流程。

## 2. 总体架构

```text
┌──────────────────────────────────────────────────────────┐
│ Android UI：聊天 / 任务 / 工具 / 记忆 / 审计 / 设置       │
└───────────────────────┬──────────────────────────────────┘
                        │ IPC / local API
┌───────────────────────▼──────────────────────────────────┐
│ Agent Runtime（前台服务 + JobScheduler）                  │
│ 计划器 · 状态机 · 预算 · 超时 · 重试 · 人机确认            │
└───────┬───────────────┬───────────────┬──────────────────┘
        │               │               │
┌───────▼──────┐ ┌──────▼───────┐ ┌─────▼──────────────┐
│ Context      │ │ Tool Broker  │ │ Model Gateway       │
│ 人格/世界书  │ │ 权限/沙箱/审计│ │ OpenAI兼容/本地模型 │
│ 记忆/RAG     │ │ 插件生命周期  │ │ fallback/多模态/TTS │
└───────┬──────┘ └──────┬───────┘ └─────┬──────────────┘
        │               │               │
  Room DB + 文件库   root helper      云端或本地模型
                    / 插件进程
        │
┌───────▼──────────────────────────────────────────────────┐
│ Android / root adapters：Accessibility、MediaProjection、 │
│ CameraX、麦克风、通知监听、Intent、Shizuku/root shell、ADB │
└──────────────────────────────────────────────────────────┘
```

推荐 Android 原生 Kotlin + Jetpack Compose。运行时拆成 `app`、`agent-core`、`tool-sdk`、`tool-builtin`、`root-helper` 五个模块。首版只支持 Android，不设计 Windows runner 或 EXE 生成链路；代码生成优先服务 Android/Termux 工具。

## 3. 参考 AI Classroom 的可复用设计

来自 `D:\AI Classroom` 的有效模式：

- 每个“课堂”独立保存模型、人格、世界书、知识库和对话，适合改造成多个代理工作区。
- 系统提示词中把用户人格放在最高优先级，并在末尾重复优先级提醒，降低长上下文导致的人格漂移。
- 最近对话优先，长期记忆、章节摘要和资料检索按预算召回，避免把整个资料库塞进请求。
- 世界书卡片带 `visibility`、`knownBy`、`unknownBy`、`evidence`，可用于区分代理内部事实、用户私密事实和外部可公开事实。
- 普通模型、Vision、TTS 分开配置，并支持模型 fallback。
- 本地 JSON 持久化简单可靠，但本产品应升级为 Room/SQLCipher，并把密钥放进 Android Keystore。

不应直接复用的部分：单体 `MainActivity.kt`、明文 API Key、本地 JSON 作为唯一数据库、把安全约束只写在提示词中，以及通过 Accessibility 无确认地发送消息。

本产品默认支持 NSFW 内容，不提供 AI Classroom 中的“高效模式/NSFW 过滤”开关。设置中单独提供“限制模式”，默认开启，集中控制法律风险、未成年人、真实伤害、平台规则和其他高风险行为。关闭限制模式前必须经过明确确认；关闭后仍保留操作审计、三秒警告和系统权限边界，不能把用户未授予的 Android 权限凭空变出来。

### 设置页的两个独立开关

安全与权限设置放在同一页，但“限制模式”和“root 模式”互不替代：

- **限制模式**：默认开启。开启时，高风险内容和动作进入阻断、人工确认或降级流程；关闭时放宽这些策略，但状态栏持续显示“限制模式已关闭”，每次策略变更写入审计。该开关不等于取得 root，也不等于绕过登录、验证码、加密或第三方平台权限。
- **root 模式**：提供“关闭 / 标准 / 完全”三档。完全模式的启用需要全屏警告、三秒倒计时、勾选风险确认和再次点击；应用重启、后台恢复或授权过期后不会自动续期。

限制模式关闭时仍保留一个不可编辑的“系统能力说明”区域，展示当前实际拥有的 Android 权限、root 状态、无障碍状态、录屏状态和可访问的应用范围，避免用户把策略开关误解成系统权限开关。

## 4. 核心领域模型

```kotlin
data class Workspace(
    val id: String,
    val name: String,
    val personality: String,
    val worldBookId: String?,
    val modelProfileId: String,
    val policyProfileId: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class MemoryCard(
    val id: String,
    val text: String,
    val visibility: Visibility,       // public, user_private, agent_private, app_private
    val knownBy: List<String>,
    val unknownBy: List<String>,
    val evidence: String,
    val keywords: List<String>,
    val sourceEventId: String?
)

data class ToolManifest(
    val id: String,
    val version: String,
    val capabilities: Set<Capability>,
    val risk: RiskLevel,
    val inputSchema: JsonObject,
    val outputSchema: JsonObject,
    val requiresConfirmation: Boolean,
    val networkAccess: NetworkPolicy,
    val filesystemScope: List<PathRule>
)

data class ApprovalRequest(
    val id: String,
    val toolId: String,
    val humanSummary: String,
    val diffOrPreview: String?,
    val expiresAt: Instant,
    val requiredFactors: Set<ApprovalFactor>
)

enum class RootMode { OFF, STANDARD, FULL }

enum class CommandChannelMode { DISABLED, PREVIEW, CONFIRM, TRUSTED }

data class CommandChannel(
    val id: String,
    val appPackage: String,
    val accountId: String,
    val peerId: String,
    val conversationId: String,
    val workspaceId: String,
    val mode: CommandChannelMode = CommandChannelMode.PREVIEW,
    val commandPrefix: String = "/claw",
    val allowedToolIds: Set<String> = emptySet(),
    val expiresAt: Instant? = null,
    val dailyBudget: Int = 100
)

data class PolicyProfile(
    val restrictionModeEnabled: Boolean = true,
    val rootMode: RootMode = RootMode.STANDARD,
    val requireThreeSecondWarningForFullMode: Boolean = true,
    val allowBackgroundAutomation: Boolean = false,
    val allowedPackages: Set<String> = emptySet()
)
```

关键事件统一写入 `audit_events`：请求、计划、工具调用、用户确认、工具结果、错误、撤销。每个事件包含时间、工作区、模型、工具版本、参数摘要、结果摘要和敏感字段哈希；默认不保存原始密码、完整聊天内容和摄像头原片。

## 5. 工具协议与插件系统

工具采用 JSON Schema 输入输出，运行时只接收已签名 manifest。建议接口：

```json
{
  "tool": "app.automation.execute",
  "version": "1.0.0",
  "risk": "high",
  "capabilities": ["notification.read", "ui.interact"],
  "input": {
    "taskId": "string",
    "targetPackage": "string",
    "mode": "draft|execute",
    "steps": "array"
  },
  "mode": "preview|execute",
  "requiresConfirmation": true
}
```

内置工具分组：

| 分组 | MVP 工具 | 实现方式 | 默认策略 |
|---|---|---|---|
| 手机应用 | `app.list`、`app.open`、`app.intent` | PackageManager、Intent | 低风险；安装/卸载需确认 |
| 应用自动化 | `app.observe`、`app.draft`、`app.action`、`app.automation.execute` | Accessibility / OCR / root input | 默认只读或草稿；写入、点击、提交需确认或任务授权 |
| 通知与消息 | `notification.list`、`notification.read`、`message.draft`、`message.send` | NotificationListener + 应用适配器 | QQ 只是示例；按包名、会话和动作单独授权 |
| 屏幕 | `screen.capture`、`screen.find_text`、`screen.tap` | MediaProjection + Accessibility | 截图可授权；点击按应用白名单 |
| 摄像头 | `camera.preview`、`camera.sample` | CameraX 前台服务 | 每次采集显示状态；后台持续采集需二次确认 |
| 文件 | `fs.read`、`fs.write`、`fs.patch` | 沙箱路径 + root helper | 默认仅应用目录；系统路径需确认 |
| 网络 | `web.search`、`web.fetch`、`download.file` | 代理 HTTP 客户端 | 域名白名单、大小/类型限制 |
| 代码 | `code.generate`、`code.test`、`app.build` | 容器/Termux 沙箱 | 禁止直接执行未审查二进制 |
| 媒体 | `image.generate`、`tts.speak`、`vision.analyze` | 供应商适配器 | API Key 在 Keystore；可切换服务商 |

插件生命周期：安装 manifest -> 签名校验 -> 静态权限检查 -> 用户启用 -> 健康检查 -> 版本锁定。下载到的插件先进入隔离目录，必须通过 hash、签名和 manifest 校验后才允许执行。

## 6. root 模式与方案选择

你已经决定授予应用 root 权限。仍建议保留 Android 权限和工具级权限，因为 root 只解决“能否访问”，不解决“是否应该执行”。设置中提供三种 root 模式：

| 模式 | 能力 | 默认 | 适用场景 |
|---|---|---|---|
| 关闭 | 不调用 root，只使用普通 Android API | 否 | 调试兼容性和安全边界 |
| 标准模式 | 使用白名单 root 命令、受限路径和工具级权限 | 是 | 日常使用、消息处理、挂机任务 |
| 完全模式 | 暴露所有已实现的查看、修改、进程、文件和输入能力 | 否 | 用户明确需要系统级操作时 |

启用完全模式时强制显示三秒倒计时。界面必须列出：将开放的能力、可访问路径/应用、可能不可逆的动作、当前限制模式状态和日志位置；倒计时期间只能取消，不能确认。倒计时结束后用户再次点击确认，完全模式才生效。完全模式的启用、关闭和每次高风险工具调用都写入审计日志。

按能力划分四层：

1. Android 普通权限：网络、相机、麦克风、通知监听、悬浮窗、媒体投影。
2. 特殊权限：无障碍服务、忽略电池优化、VPN/代理、安装未知来源。
3. root helper：受限 `su` 命令集合、只读系统信息、受控文件操作、iptables/进程管理。
4. 完全模式能力：在用户明确启用后，可查看或修改所有已实现的系统文件、应用私有数据、进程和输入事件能力；仍保留路径规范化、命令结构化、超时、结果审计和不可绕过的三秒启用警告。这里的“所有”指工具协议已经实现且 root helper 明确暴露的能力，不把任意自然语言转换成任意 shell。

三种常见方案的区别：

| 方案 | 机制 | 优点 | 局限 | 本项目建议 |
|---|---|---|---|---|
| Shizuku | 借助用户启动的特权服务，以 Binder 暴露部分系统 API | 权限范围较容易收敛，兼容性好，出问题容易撤销 | 不等于完整 root；部分底层操作仍不可用 | 可作为兼容层，不作为唯一方案 |
| Magisk 模块 | 在系统启动和模块目录中注入脚本/二进制 | 启动稳定，可提供独立 helper 和策略文件 | 安装、升级、卸载复杂；模块错误可能影响系统启动 | 适合发布版 root helper |
| 独立 `su` helper | 应用按需执行签名的 helper 命令 | 开发最快，直接利用你已授予的 root | 如果把 shell 暴露给模型，风险最高 | MVP 采用，但必须结构化命令、白名单、审计 |

推荐路线：MVP 使用独立 `su` helper；将 helper 设计成稳定的 JSON-RPC 命令协议，后续可打包成 Magisk 模块。Shizuku 作为没有 root 或部分厂商限制时的降级适配，不改变核心工具协议。标准模式和完全模式共用协议，只由 Policy Engine 决定命令白名单、路径范围和确认级别。

root helper 不接受自然语言或任意 shell 字符串，只接受枚举命令和结构化参数，例如 `read_file(path)`、`kill_process(pid)`、`set_prop(key,value)`。每个命令都进行路径规范化、UID 校验、超时和审计。危险命令默认编译时关闭，可通过开发者模式显式开启。

## 7. QQ 命令通道与普通消息分流

可以把某个 QQ 用户的聊天绑定为命令通道，同时让其他 QQ 消息继续使用工作区人格自动回复。这个功能不把 QQ 当成特殊后门，而是把它作为 `CommandChannel` 的一个消息入口；以后可以用同一模型接入 Telegram、短信或其他通知来源。

### 绑定方式

在本地设置页创建命令通道，选择 QQ 账号、对方的稳定账号标识（例如 UIN）、会话标识、目标工作区、命令前缀和允许的工具集合。只使用显示名称不足以完成绑定，因为名称可修改。推荐采用一次性配对码：应用本地生成短期配对码，指定联系人发送后，由用户在手机上确认账号、会话和权限清单。首次绑定、换绑账号或扩大工具集合必须重新本地确认。默认只允许一对一私聊；群聊必须同时绑定群 ID 和成员 ID。

### 消息分流

1. NotificationListener 收到消息后，先校验 QQ 包名、账号标识、会话标识和已绑定的 `peerId`。
2. 来自命令通道且以 `/claw`（可自定义）开头的消息进入命令解析器，例如 `/claw 打开相机采样 3 秒`、`/claw 执行每日签到任务`。命令解析器接收自然语言任务，但只输出结构化计划。
3. 命令解析器只能生成结构化计划，不能直接执行 shell；Tool Broker 重新检查限制模式、root 模式、允许的工具、目标包名、预算和审批状态。
4. 不符合命令前缀的消息进入普通对话流程，使用当前工作区的全局/工作区人格、世界书、示例和记忆自动生成回复。
5. 命令结果以简短状态回复到命令会话；详细参数、截图哈希、工具结果和错误进入本地任务页与审计页。

### 命令通道模式

- `DISABLED`：消息只按普通聊天处理。
- `PREVIEW`：识别命令并生成计划，不执行；适合初次调试。
- `CONFIRM`：执行前把计划和风险发回命令会话，同时要求手机本地确认。
- `TRUSTED`：对已授权的低/中风险工具自动执行；高风险动作、完全 root、购买/支付、删除、安装、账号设置和超出预算的任务仍暂停确认。

每个命令通道还可选择消息解释方式：`PREFIX`（默认，只有 `/claw` 前缀是命令）、`COMMAND_ONLY`（该会话的所有用户原始消息都视为命令）或 `CHAT_ONLY`（只按普通人格聊天）。`COMMAND_ONLY` 必须经过本地确认、自动过期和每日预算限制；需要普通聊天时使用 `/chat`，避免自然语言闲聊误触工具。

“完全自动”只对明确绑定的会话生效，不会让其他 QQ 消息自动获得命令权限。命令通道必须保存已处理消息 ID 或等价去重键、时间戳和任务状态，防止 QQ 通知重复投递导致重复执行。命令确认码绑定 `requestId` 和有效期，旧确认码不能重放。会话被转发、账号重新登录、QQ 账号标识变化或超过授权有效期时，通道自动降级为 `PREVIEW`。

### 需要防护的情况

- 命令消息仍是外部输入，不能覆盖系统策略、限制模式、root helper 白名单或工具 manifest。
- 命令通道的 QQ 账号一旦被盗，攻击者可能尝试控制手机；因此默认不开放完全 root 和高风险工具，且支持一键撤销、有效期、每日预算和本地暂停口令。
- 发送者身份必须绑定稳定账号/会话标识，不使用头像、昵称或消息内容作为唯一认证因素。
- 如果通知或无障碍层无法可靠取得稳定账号标识，只能把消息当普通聊天或要求手机本地确认，不能直接进入自动执行。
- 附件、网页、引用文本、群聊 @ 内容和模型生成文本不产生命令权限；只解析绑定会话中用户的原始文本。
- 命令执行前记录计划摘要和审批来源；恢复任务使用幂等键，避免重复点击、重复发送或重复安装。

命令通道状态机为：`UNBOUND -> PAIRING -> BOUND_DISABLED -> ARMED -> AWAITING_LOCAL_APPROVAL -> EXECUTING -> VERIFYING -> SUCCEEDED/FAILED/PAUSED/EXPIRED -> REVOKED`。设备重启、权限撤销或长时间闲置后回到 `BOUND_DISABLED`。命令通道只回传短结果、`requestId` 和确认预览；密钥、Cookie、完整文件和摄像头原图只在本机展示，不通过 QQ 回传。

## 8. 通用应用自动化方案

QQ 只是一个示例，挂机任务、短视频操作、游戏日常、表单填写、客服回复和其他 App 流程都统一建模为“观察 -> 计划 -> 动作 -> 验证 -> 循环”。不要依赖某个应用的私有协议、注入或绕过风控。流程：

1. 观察器获取目标 App 的通知、当前窗口、可访问节点、截图或 OCR 结果，按用户授权的包名过滤。
2. 代理使用当前工作区人格、世界书和最近上下文生成草稿。
3. UI 显示目标 App、动作序列、输入参数、风险和预计持续时间；用户确认后才执行。后台挂机任务还要设置开始/结束时间、循环次数、失败策略和停止口令。
4. 执行器完成动作后必须重新观察界面，确认状态变化符合预期；找不到稳定控件或验证失败时停止并请求人工接管。
5. 记录动作前后界面摘要、目标包名、动作结果和错误，不保存完整敏感内容，除非用户开启。

### 方案 A：Accessibility 语义控件方案

通过无障碍服务读取窗口树、文本、resource-id、contentDescription、可点击状态和滚动关系，再调用点击、输入、返回和滚动动作。它适合消息、表单、设置页和大多数原生 Android 页面。

- 优点：使用语义节点，坐标变化影响小；能读取当前控件状态；动作可验证；实现成本中等。
- 局限：游戏、Canvas、WebView 或自绘界面可能没有有效节点；不同版本 App 的 resource-id 和层级会变化；部分厂商会限制后台无障碍。
- 适用：QQ 消息、表单填写、签到、重复点击、日常挂机流程。

### 方案 B：OCR + 模拟点击方案

通过 MediaProjection 截图，使用本地 OCR/Vision 找到文字、按钮和区域，再用 root `input tap/swipe` 或 InputManager 注入触控。无法识别文字时可使用模板匹配、颜色/图标特征和固定坐标兜底。

- 优点：能处理游戏、Canvas、自绘控件和无障碍树为空的页面；不依赖 resource-id。
- 局限：受分辨率、主题、动画、遮挡和 OCR 错误影响；坐标点击误触风险更高；必须频繁截图验证。
- 适用：游戏挂机、图形化任务、视频/图片内容、无障碍不可用的页面。

### 方案 C：混合观察与动作方案（推荐）

默认优先使用 Accessibility；节点缺失或验证失败时，自动切换到 OCR/模板匹配；仍失败才使用 root 坐标输入。每个动作携带 `observeBefore`、`action`、`verifyAfter` 三段数据，并保存候选控件、截图哈希和置信度。模型只负责生成任务计划和解释，不直接输出任意坐标或 shell。

更稳定的长期方案是为常用 App 编写轻量适配器：适配器提供页面状态识别、控件语义、可执行动作和验证规则；通用 OCR 作为未知 App 的 fallback。这样既支持“帮我完成挂机任务”，也不会把所有自动化逻辑绑死在 QQ 上。

自动回复采用渐进式信任：

1. `关闭`：只读取通知，不生成或发送。
2. `草稿`：自动生成回复，用户逐条确认。
3. `确认后自动`：用户确认一次后，在指定会话和时间窗口内自动发送；遇到敏感词、附件、金额、群聊@全体或模型置信度不足时退回确认。
4. `完全自动`：对指定会话自动回复每条消息。开启前必须选择联系人/群、最大消息长度、每日上限、冷却时间和暂停口令；任何一项策略命中即停止自动发送。

自动回复不是一次性永久授权。每个会话保存独立状态、最近一次人工纠正、纠正次数、自动回复成功率和可撤销的授权过期时间。人工纠正会进入该工作区的示例和记忆候选区，经整理后影响后续风格。

消息发送只是 `message.send` 的一种动作。对任意 App 的写入、提交、购买、删除、安装、转账或账号设置修改，都复用相同的白名单、时间窗口、频率上限、敏感动作阻断和撤销策略。自动执行必须保持关闭，直到用户为指定任务或应用单独开启。

挂机任务使用声明式 DSL 和有限状态机保存进度，不保存无法验证的裸坐标脚本：

```yaml
task: daily-check-in
package: com.example.app
steps:
  - observe: {type: accessibility, selector: {text: "签到"}}
    precondition: {appForeground: true}
    action: {type: nodeClick}
    postcondition: {ocr: "已签到", confidence: 0.90}
    timeoutMs: 5000
    retry: 2
    checkpoint: signed-in
stopWhen: [captcha, login-expired, unexpected-screen]
```

执行器按 `observe -> precondition -> action -> postcondition -> checkpoint` 循环；动作有幂等键，避免恢复任务重复点击或重复发送。节点置信度低于 0.95、OCR 置信度低于 0.90 或验证失败时暂停并请求确认；低于 0.70 直接停止。每个任务还保存版本、分辨率、开始/结束时间、循环次数、失败策略和停止口令。后台任务运行时使用 Foreground Service 和常驻通知，通知中提供暂停按钮；检测到锁屏、验证码、OTP、支付/金融页面、输入法弹窗或目标 App 版本变化时 fail-closed。

## 9. 摄像头与硬件采样

使用 CameraX ImageAnalysis，输出低分辨率帧或结构化特征，默认不落盘原图。采样任务包含：设备、镜头、分辨率、帧率、持续时间、目标分析器、保存策略。前台持续采集必须显示通知和状态指示；后台采集必须有可见的常驻通知、开始/停止按钮和自动过期时间。

建议把硬件能力拆成“采集”和“分析”：采集模块只产生受控数据流，Vision 模块按需取样；不要让模型直接获得无限制的摄像头句柄。

## 10. 代码生成、下载与构建

代理可以创建 Android 项目、修改文件、运行测试和生成 APK，但采用以下流水线：

```text
需求 -> 计划 -> 文件变更预览 -> 用户确认 -> 沙箱构建 -> 测试/静态扫描
     -> 产物签名信息 -> 安装/导出确认 -> 审计记录
```

Android 构建在 Termux 或独立容器中执行。下载器限制重定向次数、大小、MIME、哈希和解压路径，拒绝路径穿越与自启动脚本。每个产物生成 SBOM、SHA-256 和来源 URL。

## 11. 全局与工作区上下文

配置采用“全局默认 -> 工作区覆盖 -> 当前任务临时值”的继承规则。每个字段都支持三态：未设置（继承）、明确为空（关闭继承）、有值（覆盖）。因此全局提示词、全局世界书和全局示例可供所有工作区使用，而某工作区可以单独覆盖其中任意一项。

建议数据结构：

```text
GlobalProfile
  personalityPrompt
  worldBook
  examples[]
  modelProfiles[]
  policyProfile

WorkspaceProfile
  personalityPromptOverride?
  worldBookOverride?
  examplesOverride?
  modelProfileOverride?
  qqPolicyOverride?
```

提示词组装顺序建议：

1. 安全与工具使用规则（不可被用户资料覆盖）。
2. 用户保存的人格与行为风格（最高用户自定义层）。
3. 生效人格：工作区人格，否则全局人格。
4. 生效世界书：工作区世界书与全局世界书按字段合并；同 key 由工作区覆盖。
5. 生效示例：工作区示例优先，未设置时使用全局示例；可限制注入数量和 token 预算。
6. 工作区目标、工具目录、当前权限、预算和审批状态。
7. 按关键词召回的记忆卡与知识片段。
8. 最近连续对话和当前任务。
9. 输出格式与本轮工具调用约束。

世界书继续采用 `visibility/knownBy/unknownBy/evidence`，但增加 `sourceTrust`（user/model/tool）和 `expiresAt`。模型生成的事实先进入候选区，不能直接成为高可信长期记忆；需要用户确认或工具证据才能升级。

## 12. 安全、隐私和故障策略

- API Key、Cookie、OAuth token 放 Android Keystore；日志做字段级脱敏。
- 工具调用默认最小权限、最短时效和最小数据；每个工作区可独立撤销权限。
- 网络请求经统一代理，支持域名白名单、离线模式、上传前预览和总流量预算。
- 所有长任务支持暂停、取消、断点恢复；工具超时后进入未知状态，禁止自动重试有副作用的操作。
- 模型提示词不承担唯一安全责任；权限在 Tool Broker 和 root helper 层强制执行。
- 崩溃或被杀后，恢复器只重放无副作用步骤；发送消息、安装 APK、删除文件等动作必须重新确认。

## 13. MVP 路线

### M0：可运行骨架（1-2 周）

- Android Compose 聊天界面、Room 数据库、Keystore 配置。
- OpenAI 兼容模型、Vision、TTS 和 fallback。
- 工作区、人格、世界书、记忆卡、工具 manifest 和审计表。
- Tool Broker 的预览/确认/执行状态机；先实现 `app.open`、`fs.read`、`web.fetch`。

### M1：手机控制（2-4 周）

- NotificationListener、Accessibility、MediaProjection、CameraX。
- 通用应用自动化的录制/预览/确认、挂机任务 DSL、Accessibility + OCR fallback、屏幕截图与摄像头一次性采样。
- root helper 只读信息和受限文件操作；权限中心与白名单。

### M2：自主工作流（3-5 周）

- 计划器、后台任务、下载器、资料检索、代码生成/测试沙箱。
- APK 构建、产物哈希、安装前确认；任务时间线和失败恢复。

### M3：扩展生态

- 插件签名与版本仓库、远程 runner、更多媒体服务商、本地模型、跨设备控制。

## 14. 首批验收标准

- 没有用户确认或明确任务授权时，任何应用自动化工具都无法执行写入、提交、发送、购买、删除或安装动作。
- root 完全模式启用前必须显示三秒倒计时和权限清单；倒计时期间无法确认，退出或过期后不会自动续期。
- 限制模式默认开启；关闭或重新开启时有明确状态提示，并写入审计日志。
- 权限撤销后，已运行任务在下一个工具边界停止。
- 任意工具调用都能在审计页定位到工作区、参数摘要、结果和确认记录。
- 摄像头采样有可见状态，停止后句柄释放，默认不保存原图。
- 恢复任务不会重复发送、重复安装或重复删除。
- 人格、世界书和最近对话冲突时，按文档顺序处理，并能在调试面板看到实际注入的上下文摘要。

## 15. 当前仍待确认的产品决策

1. 首批默认接入哪些模型供应商或本地模型（OpenAI 兼容、Ollama、通义、DeepSeek 等）？
2. 完全自动应用任务是否默认设置每日运行时长上限？建议默认 8 小时，可在任务设置中调整。
3. 全局世界书与工作区世界书发生同名条目冲突时，当前按工作区覆盖全局；如果你希望按追加合并，需要在数据模型中改为显式优先级。
