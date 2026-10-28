package com.jarvis.ui.theme

import androidx.compose.foundation.layout.PaddingValues as ContentPadding
import androidx.compose.ui.graphics.vector.ImageVector
import com.jarvis.app.JarvisTheme
import com.jarvis.app.runtime.BootstrapReport
import com.jarvis.app.runtime.ChatMessage
import com.jarvis.app.runtime.ChatSession
import com.jarvis.app.runtime.WorkspaceProfile

/*
 * 这些上下文对象把 MainActivity.kt 里四个屏幕函数（ChatScreen 1253 / TasksScreen 1466 /
 * WorkspaceScreen 1607 / SettingsScreen 2384，含其内嵌卡片 1789-2260）的实参逐参数搬出来，
 * 让主题实现只依赖本文件，不再碰 MainActivity 的内部变量。
 *
 * 阶段 3A 的扩面原则：
 *  - 新增字段一律带默认值，既有字段的名称/类型/顺序不动，5 套主题因此无需改动即可继续编译。
 *  - MainActivity 里 file-private 的领域类型（AutomationRule / FloatingAssistantPreferences /
 *    RuntimeToolStatus / ManagedOpenClawPlugin 等）不搬到主题层，改由本文件的只读视图 DTO
 *    （*View）按界面渲染所需裁剪复述，全部带默认值。
 *  - 回调只描述「界面能发起什么」，不含任何 Android 上下文依赖。
 *
 * 字段来源：
 *   ChatUiContext      <- ChatScreen(MainActivity.kt:1253-1273) + RuntimeToolsDialog(1404-1463)
 *   TasksUiContext     <- TasksScreen(MainActivity.kt:1466-1575) + AutomationRuleCard(1578-1604)
 *   WorkspaceUiContext <- WorkspaceScreen(MainActivity.kt:1607-1616)；编辑弹窗已删除，改由主题内联表单 +
 *     WorkspaceCallbacks.onSave(profile, newName) 覆盖新建(newName=null)与重命名两条路径
 *   SettingsUiContext  <- SettingsScreen(MainActivity.kt:2384-2412) + 其内嵌卡片（见各字段 KDoc 行号）
 *   ShellUiContext     <- 外壳导航（JarvisTab + SessionRail 1088 / 底部导航条），供阶段 3B 的 Shell 使用
 */

/** 根权限档位。旧类型 RootMode 是 MainActivity.kt 的 file-private 枚举，主题层只能用 key/label 复述。 */
data class RootModeOption(val key: String, val label: String)

/**
 * 底部导航/侧栏的一个页签，Shell 与主题共用同一份描述。
 *
 * @property key 稳定标识，对应旧 JarvisTab 名称（chat/tasks/workspace/settings）。
 * @property label 显示名。
 * @property icon 可选图标；为空时由主题自行占位。
 * @property opensFloating true 表示这一项不是页面跳转，而是「打开浮窗」动作（第 5 项「浮窗」用，
 *   霓虹/便当主题的 5 段底栏依赖它）；此时主题应调用 [ShellUiContext.onOpenFloating] 而不是 onNavigate。
 */
data class TabOption(
    val key: String = "",
    val label: String = "",
    val icon: ImageVector? = null,
    val opensFloating: Boolean = false
)

/**
 * 外壳导航上下文：底部导航条只需要「当前页签 + 页签清单 + 切换动作」。
 *
 * 旧界面用 JarvisTab 枚举与 innerPadding 驱动（MainActivity.kt:1042-1084），
 * 主题层只能用 key/label 复述；当前 key 与 [TabOption.key] 比较即可。
 */
data class ShellUiContext(
    /** 当前页签下标，越界时主题应回退到第 0 项。 */
    val tabIndex: Int = 0,
    /** 页签清单，顺序即显示顺序。 */
    val tabs: List<TabOption> = emptyList(),
    /** 切换到第 index 个页签。 */
    val onNavigate: (Int) -> Unit = {},
    /** 「打开浮窗」动作：点击 [TabOption.opensFloating] 为 true 的那一项时调用，不切换页面。 */
    val onOpenFloating: () -> Unit = {}
) {
    companion object {
        /**
         * 5 个一级路由：对话 / 任务 / 工作区 / 设置 是 4 个页面，第 5 项「浮窗」只触发
         * [onOpenFloating]，不参与页签下标语义（tabIndex 只取 0..3）。
         */
        val DefaultTabs: List<TabOption> = listOf(
            TabOption("chat", "对话"),
            TabOption("tasks", "任务"),
            TabOption("workspace", "工作区"),
            TabOption("settings", "设置"),
            TabOption("floating", "浮窗", opensFloating = true)
        )
    }
}

/** 通用二选一枚举视图（悬浮球形状、手势动作等），避免主题层引用 MainActivity 的私有枚举。 */
data class ChoiceOption(
    /** 稳定 key，与当前选中 key 比较。 */
    val key: String = "",
    /** 显示名。 */
    val label: String = ""
)

/**
 * 自动化规则只读视图。
 *
 * 旧类型 AutomationRule 在 com.jarvis.app.runtime 里可引用，但对界面只需要渲染字段，
 * 这里裁剪掉 JSON 序列化与 lastRunAt 语义细节之外的部分，并补上「启用前检查」结论。
 */
data class AutomationRuleView(
    /** 规则 id。 */
    val id: String = "",
    /** 标题。 */
    val title: String = "",
    /** 类型 key（BILIBILI_REPLY / PIXIV_NEW_WORK）。 */
    val kindKey: String = "",
    /** 类型显示名，对应 AutomationKind.label。 */
    val kindLabel: String = "",
    /** 创建规则时的原始要求。 */
    val sourcePrompt: String = "",
    /** Pixiv 画师名或用户 ID。 */
    val artist: String = "",
    /** 回复风格要求。 */
    val replyInstruction: String = "",
    /** 是否已启用。 */
    val enabled: Boolean = false,
    /** 最近一次执行结果文案。 */
    val lastResult: String = "",
    /** 最近执行时间戳（毫秒），0 表示从未执行。 */
    val lastRunAt: Long = 0L,
    /** 未启用时的「启用前检查」提示，null 表示无需提示（对应 MainActivity.kt:1525 readiness）。 */
    val readiness: String? = null
)

/** 最近执行事件视图，对应 MainActivity.kt:1538-1541 的三元组渲染。 */
data class RecentEventView(
    /** 事件 id。 */
    val id: String = "",
    /** 事件标题。 */
    val title: String = "",
    /** 事件说明。 */
    val message: String = "",
    /** 是否成功，决定 ✓ / ! 与配色。 */
    val ok: Boolean = false,
    /** 发生时间戳（毫秒）。 */
    val occurredAt: Long = 0L
)

/** 工作区编辑弹窗里「从其他对话快捷粘贴」的可选来源（MainActivity.kt:1710-1714）。 */
data class PersonaSourceView(
    /** 来源会话 id。 */
    val sessionId: String = "",
    /** 来源会话所属工作区名。 */
    val workspace: String = "",
    /** 来源会话标题。 */
    val title: String = "",
    /** 该工作区解析后的人格文本。 */
    val mentorPrompt: String = "",
    /** 该工作区解析后的世界书文本。 */
    val worldBook: String = ""
)

/** 运行时工具检查项（CMake / FFmpeg / Python…），对应 RuntimeToolStatus 的界面子集。 */
data class ToolStatusView(
    /** 工具 id，也是安装时的键。 */
    val id: String = "",
    /** 显示名。 */
    val label: String = "",
    /** 说明。 */
    val description: String = "",
    /** 来源显示名（内置运行时/系统/root/Termux/未安装）。 */
    val sourceLabel: String = "",
    /** 探测到的可执行路径，空串表示未找到。 */
    val path: String = "",
    /** 可请求安装的包名。 */
    val installPackage: String = ""
)

/**
 * 「运行时工具」卡片的一行（MainActivity.kt:2626-2648）：只要名称/是否已安装/版本。
 * 与 [ToolStatusView] 同源，后者是带路径与安装包的详细视图。
 */
data class RuntimeToolView(
    /** 工具名，也是请求安装时的键。 */
    val name: String = "",
    /** 是否已可用（找到可执行文件）。 */
    val installed: Boolean = false,
    /** 版本或来源后缀，空串表示未探测到。 */
    val version: String = ""
)

/**
 * 悬浮助手设置视图（MainActivity.kt:1940-2078 的整张卡片）。
 * 与 SettingsState 上的扁平字段（floatingEnabled / floatingSizeDp / floatingShapeKey /
 * floatingOpacity / avatarHideSeconds）同源，
 * 本类型是权威分组视图，扁平字段只是等价快捷方式。
 */
data class FloatingAssistantView(
    /** 悬浮球开关（MainActivity.kt:1944、1991）。 */
    val enabled: Boolean = false,
    /** 悬浮球边长 dp（MainActivity.kt:2017-2023）。 */
    val sizeDp: Int = 56,
    /** 悬浮球形状 key：circle / rounded / square（MainActivity.kt:2012-2016）。 */
    val shape: String = "circle",
    /** 不透明度 0..1。 */
    val opacity: Float = 1f,
    /** 双击/三击「暂时隐藏」的秒数（MainActivity.kt:2066-2073）。 */
    val hideSeconds: Int = 15
)

/** 社区插件视图，对应 ManagedOpenClawPlugin 的界面子集。 */
data class PluginView(
    /** 插件 id。 */
    val id: String = "",
    /** 显示名。 */
    val name: String = "",
    /** 版本号。 */
    val version: String = "",
    /** 说明。 */
    val description: String = "",
    /** 暴露的工具名列表。 */
    val tools: List<String> = emptyList(),
    /** 是否启用。 */
    val enabled: Boolean = false,
    /** 来源（community / generated）。 */
    val source: String = ""
)

/** 操作记忆中的一条路径（MainActivity.kt:1884-1891）。 */
data class MemoryEntryView(
    /** 记录 id，删除时回指。 */
    val id: String = "",
    /** 路径标题。 */
    val title: String = "",
    /** 是否仍可复用（status == ready）。 */
    val ready: Boolean = false,
    /** 成功次数。 */
    val successes: Int = 0,
    /** 失败次数。 */
    val failures: Int = 0
)

/** 主题选择项。旧界面 ThemePicker 直接遍历主题枚举，主题层改为读这份清单。 */
data class ThemeOptionView(
    /** 主题 id，与 JarvisThemes.byId 的键一致。 */
    val id: String = "",
    /** 显示名。 */
    val label: String = "",
    /** 副标题/故事名。 */
    val story: String = "",
    /** 一句话描述。 */
    val description: String = "",
    /** 明暗模式 key：light / dark。 */
    val modeKey: String = "dark"
)

/** 对话页上下文。 */
data class ChatUiContext(
    val state: ChatState,
    val callbacks: ChatCallbacks,
    /** 底栏/侧栏导航上下文：主题屏据此画自己的底栏并切页，阶段 3C-1 回填时使用。 */
    val shell: ShellUiContext = ShellUiContext()
) {
    data class ChatState(
        val contentPadding: ContentPadding,
        val conversationId: String,
        val messages: List<ChatMessage>,
        val draft: String,
        val sending: Boolean,
        val speechNotice: String,
        val editingLatest: Boolean,
        val canUndoLatest: Boolean,
        val canEditLatest: Boolean,
        val reasoningEffort: String,
        /** 是否正在流式接收回复；[sending] 覆盖整个请求周期，此位只表示已有增量文本。 */
        val streaming: Boolean = false,
        /** 语音识别是否进行中（决定麦克风按钮的忙碌态），MainActivity.kt:1382-1384。 */
        val voiceListening: Boolean = false,
        /** 思考档位候选列表，顺序即展示顺序；空表示由主题内置默认（off/minimal/low/medium/high/max）。 */
        val reasoningEfforts: List<String> = emptyList(),
        /** 工具箱（RuntimeToolsDialog，MainActivity.kt:1404）是否展开。 */
        val toolboxVisible: Boolean = false
    )

    data class ChatCallbacks(
        val onDraftChange: (String) -> Unit,
        val onSend: (String) -> Unit,
        val onVoiceInput: () -> Unit,
        val onNewConversation: () -> Unit,
        val onBranch: (String?) -> Unit,
        val onUndoLatest: () -> Unit,
        val onEditLatest: () -> Unit,
        val onCancelEdit: () -> Unit,
        val onReasoningEffortChange: (String) -> Unit,
        /** 打开工具箱（旧界面输入区的 Build 图标，MainActivity.kt:1385-1387）。 */
        val onOpenTools: () -> Unit = {},
        /** 关闭工具箱（旧界面 AlertDialog 的「完成」/返回，MainActivity.kt:1461）。 */
        val onCloseTools: () -> Unit = {}
    )
}

/**
 * 任务页上下文。
 *
 * 旧 TasksScreen(MainActivity.kt:1466) 唯一的参数是 padding：规则列表、通知事件、授权状态
 * 都由屏幕自己 new AutomationManager 读取。阶段 3A 把这些状态与动作提到上下文里，
 * 主题实现因此不必持有 Android 上下文，装配由外壳（阶段 3B）完成。
 */
data class TasksUiContext(
    val state: TasksState,
    val callbacks: TasksCallbacks,
    /** 底栏/侧栏导航上下文，同 [ChatUiContext.shell]。 */
    val shell: ShellUiContext = ShellUiContext()
) {
    data class TasksState(
        val contentPadding: ContentPadding,
        /** 自动化规则清单（MainActivity.kt:1519-1534）。 */
        val rules: List<AutomationRuleView> = emptyList(),
        /** 最近执行事件，界面只取前若干条（MainActivity.kt:1535-1542）。 */
        val recentEvents: List<RecentEventView> = emptyList(),
        /** 通知访问是否已授权（MainActivity.kt:1475、1504-1507）。 */
        val notificationAccess: Boolean = false,
        /** Jarvis 无障碍服务是否已连接（MainActivity.kt:1476、1508）。 */
        val accessibilityGranted: Boolean = false,
        /** 运行时是否就绪；未就绪时规则无法真正执行。 */
        val runtimeReady: Boolean = false,
        /** 操作结果/错误提示，空串表示不显示（MainActivity.kt:1512-1514）。 */
        val notice: String = "",
        /** 正在配置的规则 id，null 表示配置弹窗关闭（MainActivity.kt:1548）。 */
        val editingRuleId: String? = null,
        /** 配置弹窗里 Pixiv 画师草稿（MainActivity.kt:1557）。 */
        val artistDraft: String = "",
        /** 配置弹窗里回复风格草稿（MainActivity.kt:1560）。 */
        val replyDraft: String = "",

        /** 任务页「直接新建草案」表单是否展开；与聊天里自动创建并存，两条路径互不影响。 */
        val creatingRule: Boolean = false,
        /** 新建草案的标题草稿。 */
        val ruleDraftTitle: String = "",
        /** 新建草案的类型 key，取值见 [ruleDraftKindOptions]，默认第一条 BILIBILI_REPLY。 */
        val ruleDraftKindKey: String = "BILIBILI_REPLY",
        /** 类型候选（由 AutomationKind.entries 生成，key=name、label=label）。 */
        val ruleDraftKindOptions: List<ChoiceOption> = emptyList(),
        /** 新建草案的触发要求草稿（对应 sourcePrompt）。 */
        val ruleDraftPrompt: String = "",
        /** 新建草案的画师名/ID 草稿，仅 Pixiv 类型需要，可留空。 */
        val ruleDraftArtist: String = "",
        /** 提交失败原因，空串表示无错误；校验失败不静默，必须回填这里。 */
        val ruleDraftError: String = ""
    )

    data class TasksCallbacks(
        /** 启用/暂停规则：(规则 id, 是否启用)，MainActivity.kt:1525-1528。 */
        val onToggleRule: (String, Boolean) -> Unit = { _, _ -> },
        /** 删除规则（MainActivity.kt:1529-1533）。 */
        val onDeleteRule: (String) -> Unit = {},
        /** 打开规则配置弹窗（MainActivity.kt:1521-1524）。 */
        val onConfigureRule: (String) -> Unit = {},
        /** 保存配置：(规则 id, 画师草稿, 回复风格草稿)，MainActivity.kt:1566-1570。 */
        val onSaveRuleConfig: (String, String, String) -> Unit = { _, _, _ -> },
        /** 配置弹窗里画师草稿的输入回调（MainActivity.kt:1557）。 */
        val onArtistDraftChange: (String) -> Unit = {},
        /** 配置弹窗里回复风格草稿的输入回调（MainActivity.kt:1560）。 */
        val onReplyDraftChange: (String) -> Unit = {},
        /** 取消配置弹窗（MainActivity.kt:1572）。 */
        val onCancelRuleConfig: () -> Unit = {},
        /** 跳系统通知访问授权页（MainActivity.kt:1507）。 */
        val onRequestNotificationAccess: () -> Unit = {},
        /** 跳系统无障碍设置页（MainActivity.kt:1508）。 */
        val onRequestAccessibility: () -> Unit = {},
        /** 重新读取规则/事件/授权状态（MainActivity.kt:1498、1478-1483）。 */
        val onRefresh: () -> Unit = {},

        /** 展开「新建草案」表单并清空上一次的错误。 */
        val onCreateRuleStart: () -> Unit = {},
        /** 新建草案标题草稿变更。 */
        val onCreateRuleTitleChange: (String) -> Unit = {},
        /** 新建草案类型切换（传 [TasksState.ruleDraftKindOptions] 的 key）。 */
        val onCreateRuleKindChange: (String) -> Unit = {},
        /** 新建草案触发要求变更。 */
        val onCreateRulePromptChange: (String) -> Unit = {},
        /** 新建草案画师名/ID 变更（Pixiv 类型用，可空）。 */
        val onCreateRuleArtistChange: (String) -> Unit = {},
        /** 提交新建草案；标题或触发要求为空时由外壳写 [TasksState.ruleDraftError] 而不是静默失败。 */
        val onCreateRuleSubmit: () -> Unit = {},
        /** 收起表单并清空草稿。 */
        val onCreateRuleCancel: () -> Unit = {}
    )
}

/**
 * 工作区绑定的 SAF 目录里的一项（只读视图）。MainActivity 用 ContentResolver 列一层，
 * 最多 200 条；本阶段不做任何写操作（重命名/删除文件一律没有回调）。
 *
 * @property name 文件名（DISPLAY_NAME）。
 * @property sizeBytes 字节数，-1 表示提供方未给出（并非 0 字节）。
 * @property isDirectory 是否为目录。
 * @property lastModified 最后修改时间戳（毫秒），0 表示未知。
 * @property previewable 是否可预览（文本 MIME 或常见文本扩展名，大小 -1/0 或 < 256KB，且确实读到非空文本；
 *   空文本文件为 false）。
 * @property previewText 预览文本，最多 4000 字符；previewable 为 false 时为空串。
 */
data class WorkspaceFileView(
    val name: String = "",
    val sizeBytes: Long = 0L,
    val isDirectory: Boolean = false,
    val lastModified: Long = 0L,
    val previewable: Boolean = false,
    val previewText: String = ""
)

/**
 * 工作区页上下文。
 *
 * 归档语义（本阶段新增）：
 *  - [WorkspaceState.workspaces] 只含「当前应显示」的档案：未归档，或 [WorkspaceState.showArchived] 为 true 时的全部。
 *  - [WorkspaceState.archivedWorkspaces] 始终是完整归档清单，供主题画「显示已归档」开关的数量徽标。
 *  - 归档不删数据：档案与对话全部保留，[WorkspaceCallbacks.onUnarchive] 可还原。
 */
data class WorkspaceUiContext(
    val state: WorkspaceState,
    val callbacks: WorkspaceCallbacks,
    /** 底栏/侧栏导航上下文，同 [ChatUiContext.shell]。 */
    val shell: ShellUiContext = ShellUiContext()
) {
    data class WorkspaceState(
        val contentPadding: ContentPadding,
        val workspaces: List<WorkspaceProfile>,
        val conversations: List<ChatSession>,
        val selectedWorkspace: String,
        val globalPersona: String,
        val globalWorldBook: String,
        /** 每个工作区的会话数，键为工作区名（旧界面 WorkspaceRow 的 count 参数）。 */
        val conversationCountByWorkspace: Map<String, Int> = emptyMap(),
        /** 编辑弹窗「从其他对话快捷粘贴」的可选来源。 */
        val personaSources: List<PersonaSourceView> = emptyList(),
        /** 当前工作区 SAF 目录的第一层内容（最多 200 条），未绑定目录时为空。 */
        val files: List<WorkspaceFileView> = emptyList(),
        /** 是否正在读取目录。 */
        val filesLoading: Boolean = false,
        /** 目录读取提示（未绑定/权限失效/截断/出错），空串表示不显示。 */
        val filesNotice: String = "",
        /** 当前工作区的目录是否仍持有持久化读权限；false 时主题应引导重新选择目录。 */
        val dirReady: Boolean = false,
        /** 「显示已归档」开关状态；false 时 [workspaces] 与 [conversations] 都已滤掉归档项。 */
        val showArchived: Boolean = false,
        /** 完整归档清单（不论开关），用于计数与还原入口。 */
        val archivedWorkspaces: List<WorkspaceProfile> = emptyList(),
        /** 已归档对话 id（由 ChatSession.archivedAt 派生）；[conversations] 已按开关过滤。 */
        val archivedConversationIds: List<String> = emptyList()
    )

    data class WorkspaceCallbacks(
        /** 切换当前工作区。 */
        val onSelect: (String) -> Unit = {},
        /**
         * 保存/新建工作区。
         *
         * @param profile 编辑后的档案。
         * @param newName 旧名称：null 表示这是一次新建而不需要重命名，非 null 表示把该旧名的工作区改名。
         */
        val onSave: (profile: WorkspaceProfile, newName: String?) -> Unit = { _, _ -> },
        /** 删除工作区（按名称），旧界面无入口，新增动作。 */
        val onDelete: (String) -> Unit = {},
        /** 归档工作区：只置 archived=true，不删档案与对话；默认工作区不可归档。 */
        val onArchive: (String) -> Unit = {},
        /** 取消归档（还原到普通列表）。 */
        val onUnarchive: (String) -> Unit = {},
        /** 打开 SAF 目录选择器，选中后写回该工作区的 dirUri/dirLabel 并持久化读权限。 */
        val onPickDirectory: (String) -> Unit = {},
        /** 解除目录绑定（不清任何文件）。 */
        val onClearDirectory: (String) -> Unit = {},
        /** 切换「显示已归档」开关。 */
        val onToggleShowArchived: (Boolean) -> Unit = {},
        /** 把对话移动到另一工作区：(conversationId, targetWorkspace)。 */
        val onMoveConversation: (String, String) -> Unit = { _, _ -> },
        /** 删除对话（从对话存储移除）。 */
        val onDeleteConversation: (String) -> Unit = {},
        /** 归档对话：隐藏但保留可还原，不删消息。 */
        val onArchiveConversation: (String) -> Unit = {},
        /** 取消归档对话。 */
        val onUnarchiveConversation: (String) -> Unit = {},
        /** 重命名对话标题；空白标题会被忽略。 */
        val onRenameConversation: (String, String) -> Unit = { _, _ -> }
    )
}

/**
 * 设置页上下文。
 *
 * 旧签名（MainActivity.kt:2384-2412）收的是 file-private 的 JarvisConfig 与 RootMode，主题层无法引用，
 * 因此把 config 逐字段展开成 [SettingsState]，把 RootMode 复述成 [RootModeOption] 列表加当前 key；
 * 装配时由外壳做一行适配（Stage 3B），屏幕行为语义不变。
 *
 * 阶段 3A 按老界面的分组补齐：风格与外观、头像外观/悬浮球、手势任务、远程访问、工具检查、
 * 插件管理、操作记忆、无障碍、AI Classroom 导入导出、保存并应用。
 */
internal data class SettingsUiContext(
    val state: SettingsState,
    val callbacks: SettingsCallbacks,
    /** 底栏/侧栏导航上下文，同 [ChatUiContext.shell]。 */
    val shell: ShellUiContext = ShellUiContext()
) {
    internal data class SettingsState(
        val contentPadding: ContentPadding,
        val restrictionMode: Boolean,
        val commandChannelEnabled: Boolean,
        val commandPrefix: String,
        val modelBaseUrl: String,
        val modelName: String,
        val modelApiKey: String,
        val modelChain: String,
        val deepThinkingEnabled: Boolean,
        val reasoningEffort: String,
        val deepThinkingModel: String,
        val visionBaseUrl: String,
        val visionApiKey: String,
        val visionModel: String,
        val mentorPrompt: String,
        val worldBook: String,
        val theme: JarvisTheme,
        /** 当前生效的主题 id（5 套，见 [ShellUiContext] 同级 registry），旧 [theme] 枚举同样覆盖 5 套。 */
        val themeId: String = "",
        val rootModes: List<RootModeOption>,
        val rootModeKey: String,
        val bootstrapReport: BootstrapReport,
        val runtimeMessage: String,
        val configNotice: String,
        /** 可选主题清单（旧 ThemePicker，MainActivity.kt:2443）。 */
        val themeOptions: List<ThemeOptionView> = emptyList(),
        /** 明暗偏好：跟随系统 / 强制浅色 / 强制深色，持久化键 mode_override（follow/light/dark）。 */
        val modeOverride: ThemeModeOverride = ThemeModeOverride.FOLLOW_SYSTEM,
        /** 当前实际生效的明暗（已把「跟随系统」解析成具体一档），供设置页展示，不是用户偏好。 */
        val effectiveMode: ThemeMode = ThemeMode.DARK,
        /** 思考档位候选，顺序即展示顺序；合法值见 MainActivity.kt:206 的
         *  off/minimal/low/medium/high/max，主题不要自己写死字面量。 */
        val reasoningEfforts: List<String> = emptyList(),
        /** 安装包版本名，对应旧设置页副标题里的 BuildConfig.VERSION_NAME（MainActivity.kt:2434）。 */
        val versionName: String = "",

        /** 悬浮球头像边长 dp（MainActivity.kt:2017-2023）。 */
        val avatarSizeDp: Int = 56,
        /** 双击/三击「暂时隐藏」的秒数（MainActivity.kt:2066-2073）。 */
        val avatarHideSeconds: Int = 15,
        /** 头像边长候选值（MainActivity.kt:2019）。 */
        val avatarSizeChoices: List<Int> = listOf(44, 56, 68, 80, 88),
        /** 隐藏秒数候选值（MainActivity.kt:2069）。 */
        val hideSecondChoices: List<Int> = listOf(5, 15, 30, 60, 300),

        /** 悬浮助手设置分组（MainActivity.kt:1940-2078），扁平字段的权威来源。 */
        val floatingAssistant: FloatingAssistantView = FloatingAssistantView(),
        /** 悬浮球开关（MainActivity.kt:1944、1991-1998），等价 floatingAssistant.enabled。 */
        val floatingEnabled: Boolean = false,
        /** 悬浮球尺寸 dp（MainActivity.kt:1945 appearance.sizeDp）。 */
        val floatingSizeDp: Int = 56,
        /** 悬浮球形状 key：circle / rounded / square（MainActivity.kt:2012-2016）。 */
        val floatingShapeKey: String = "circle",
        /** 悬浮球不透明度 0..1；旧界面固定不透明白底，新增可调项。 */
        val floatingOpacity: Float = 1f,
        /** 形状候选清单（MainActivity.kt:2012 FloatingShape.entries）。 */
        val floatingShapes: List<ChoiceOption> = emptyList(),
        /** 悬浮窗权限是否已授予（MainActivity.kt:1962、2000）。 */
        val overlayGranted: Boolean = false,
        /** 无障碍是否已启用（MainActivity.kt:1963、2000）。 */
        val accessibilityGranted: Boolean = false,

        /** 双击手势 key（MainActivity.kt:2026-2031）。 */
        val doubleGestureKey: String = "rotate",
        /** 三击手势 key（MainActivity.kt:2047-2052）。 */
        val tripleGestureKey: String = "hide",
        /** 双击自定义任务文本（MainActivity.kt:2032-2044）。 */
        val doubleTask: String = "",
        /** 三击自定义任务文本（MainActivity.kt:2053-2065）。 */
        val tripleTask: String = "",
        /** 手势候选清单（MainActivity.kt:2027 FloatingGestureAction.entries）。 */
        val gestureOptions: List<ChoiceOption> = emptyList(),
        /** 「选用已有自动化」的可选规则（MainActivity.kt:1946、2039-2041）。 */
        val savedAutomations: List<AutomationRuleView> = emptyList(),

        /** 远程网页控制台是否运行（MainActivity.kt:2085、2098-2113）。 */
        val serverRunning: Boolean = false,
        /** 一次性配对码，空串表示当前没有有效配对码（MainActivity.kt:2116-2122）。 */
        val pairingCode: String = "",
        /** 配对码过期时间戳（毫秒），0 表示无（MainActivity.kt:2119）。 */
        val pairingExpiresAt: Long = 0L,
        /** 是否允许远程设备调用本机能力（MainActivity.kt:2128-2132）。 */
        val remoteDeviceControl: Boolean = false,
        /** 已发现的局域网/Tailscale 地址（MainActivity.kt:2133-2138）。 */
        val remoteUrls: List<String> = emptyList(),

        /** 「运行时工具」卡片清单（名称/已安装/版本，MainActivity.kt:2626-2648）。 */
        val runtimeTools: List<RuntimeToolView> = emptyList(),
        /** 运行时工具详细探测结果（含路径与安装包，MainActivity.kt:2634-2641），runtimeTools 的精简来源。 */
        val tools: List<ToolStatusView> = emptyList(),
        /** 是否正在检查工具（MainActivity.kt:2418、2631）。 */
        val checkingTools: Boolean = false,
        /** CMake 是否可用（导出自 tools，给主题做一行摘要）。 */
        val cmakeReady: Boolean = false,
        /** FFmpeg 是否可用（导出自 tools，给主题做一行摘要）。 */
        val ffmpegReady: Boolean = false,
        /** 工具检查/安装提示（MainActivity.kt:2642-2645）。 */
        val toolNotice: String = "",

        /** 已安装社区插件（MainActivity.kt:2179、2188）。 */
        val plugins: List<PluginView> = emptyList(),
        /** 插件操作提示（MainActivity.kt:2182）。 */
        val pluginNotice: String = "",
        /** 插件正在加载/卸载（MainActivity.kt:2183）。 */
        val pluginBusy: Boolean = false,

        /** 操作记忆路径清单（MainActivity.kt:1884-1891）。 */
        val memoryEntries: List<MemoryEntryView> = emptyList(),
        /** 操作记忆概览文案（MainActivity.kt:1855、1881）。 */
        val memoryStatus: String = "",

        /** 打开 Jarvis 时是否自动连接无障碍（MainActivity.kt:1901、1917-1919）。 */
        val accessibilityAutoConnect: Boolean = false,
        /** 无障碍连接状态文案（MainActivity.kt:1906-1910、1915）。 */
        val accessibilityStatus: String = "",

        /** AI Classroom 导入结果提示（MainActivity.kt:2595-2598）。 */
        val importNotice: String = "",
        /** AI Classroom 导出结果提示。 */
        val exportNotice: String = "",

        /** 正在保存并应用配置（MainActivity.kt:1794、1839-1840）。 */
        val saving: Boolean = false,
        /** 保存/应用结果提示（MainActivity.kt:1808-1830、1842-1847）。 */
        val applyNotice: String = "",
        /** 上一次保存/应用是否失败，决定提示配色（MainActivity.kt:1844）。 */
        val applyFailed: Boolean = false
    )

    internal data class SettingsCallbacks(
        val onRefreshRuntime: () -> Unit,
        val onRecovery: () -> Unit,
        val onStartRuntime: () -> Unit,
        val onStopRuntime: () -> Unit,
        val onRestrictionChange: (Boolean) -> Unit,
        val onCommandChange: (Boolean) -> Unit,
        val onPrefixChange: (String) -> Unit,
        val onModelBaseUrlChange: (String) -> Unit,
        val onModelNameChange: (String) -> Unit,
        val onModelApiKeyChange: (String) -> Unit,
        val onModelChainChange: (String) -> Unit,
        val onDeepThinkingChange: (Boolean) -> Unit,
        val onReasoningEffortChange: (String) -> Unit,
        val onDeepThinkingModelChange: (String) -> Unit,
        val onVisionBaseUrlChange: (String) -> Unit,
        val onVisionApiKeyChange: (String) -> Unit,
        val onVisionModelChange: (String) -> Unit,
        val onMentorPromptChange: (String) -> Unit,
        val onWorldBookChange: (String) -> Unit,
        /** 旧枚举通道：只能覆盖 legacy 5 套，保留给尚未回填的主题。 */
        val onThemeChange: (JarvisTheme) -> Unit,
        /** 权威通道：按 [ThemeOptionView.id] 切换，能选到 registry 里的全部 5 套。 */
        val onThemeChangeById: (String) -> Unit = {},
        /** 切换明暗偏好（跟随系统 / 浅色 / 深色），写 mode_override 并立即重解析实际色板。 */
        val onModeOverrideChange: (ThemeModeOverride) -> Unit = {},
        val onImportAiClassroom: () -> Unit,
        val onExportAiClassroom: () -> Unit,
        val onRootModeRequest: (RootModeOption) -> Unit,

        /** 头像边长换成 sizeDp（MainActivity.kt:2020）。 */
        val onAvatarSizeChange: (Int) -> Unit = {},
        /** 隐藏秒数改成 seconds（MainActivity.kt:2071）。 */
        val onAvatarHideSecondsChange: (Int) -> Unit = {},

        /** 启用/关闭悬浮球（MainActivity.kt:1991-1998）。 */
        val onFloatingEnabledChange: (Boolean) -> Unit = {},
        /** 悬浮球尺寸换成 dp（MainActivity.kt:2020）。 */
        val onFloatingSizeChange: (Int) -> Unit = {},
        /** 悬浮球形状换成 key（MainActivity.kt:2013）。 */
        val onFloatingShapeChange: (String) -> Unit = {},
        /** 申请系统悬浮窗权限（MainActivity.kt:1965-1969、2005）。 */
        val onOverlayPermissionRequest: () -> Unit = {},
        /** 打开系统无障碍设置页（MainActivity.kt:2006）。 */
        val onAccessibilitySettingsOpen: () -> Unit = {},

        /** 双击手势改成 key（MainActivity.kt:2028）。 */
        val onDoubleGestureChange: (String) -> Unit = {},
        /** 三击手势改成 key（MainActivity.kt:2049）。 */
        val onTripleGestureChange: (String) -> Unit = {},
        /** 双击任务文本（MainActivity.kt:2034）。 */
        val onDoubleTaskChange: (String) -> Unit = {},
        /** 三击任务文本（MainActivity.kt:2055）。 */
        val onTripleTaskChange: (String) -> Unit = {},
        /** 把手势指向一条已有自动化：(gestureKey, ruleId)，MainActivity.kt:2039-2041。 */
        val onSelectAutomationForGesture: (String, String) -> Unit = { _, _ -> },

        /** 开启远程网页控制台（MainActivity.kt:2098-2112）。 */
        val onStartServer: () -> Unit = {},
        /** 关闭远程网页控制台并失效配对令牌（MainActivity.kt:2101-2106）。 */
        val onStopServer: () -> Unit = {},
        /** 生成新的 5 分钟配对码（MainActivity.kt:2123-2127）。 */
        val onNewPairingCode: () -> Unit = {},
        /** 复制当前配对码到剪贴板，旧界面无按钮，新增动作。 */
        val onCopyPairingCode: () -> Unit = {},
        /** 撤销所有远程配对（MainActivity.kt:2141-2145）。 */
        val onRevokePairing: () -> Unit = {},
        /** 允许/禁止远程设备调用本机能力（MainActivity.kt:2128-2132）。 */
        val onRemoteDeviceControlChange: (Boolean) -> Unit = {},
        /** 刷新局域网地址（MainActivity.kt:2140）。 */
        val onRefreshRemoteUrls: () -> Unit = {},

        /** 「运行时工具」卡片的刷新（MainActivity.kt:2629），与 onCheckTools 等价。 */
        val onRefreshTools: () -> Unit = {},
        /** 重新检查运行时工具（MainActivity.kt:2629）。 */
        val onCheckTools: () -> Unit = {},
        /** 请求安装指定工具（MainActivity.kt:2636-2639）。 */
        val onInstallTool: (String) -> Unit = {},

        /** 刷新插件列表（MainActivity.kt:2187-2189）。 */
        val onRefreshPlugins: () -> Unit = {},
        /** 从文本/草稿安装插件（MainActivity.kt:2154-2172 的解析入口）。 */
        val onInstallPlugin: (String) -> Unit = {},
        /** 卸载插件（MainActivity.kt:2185 pendingRemove）。 */
        val onUninstallPlugin: (String) -> Unit = {},
        /** 启用/停用插件：(id, enabled)，MainActivity.kt:2191-2202。 */
        val onTogglePlugin: (String, Boolean) -> Unit = { _, _ -> },

        /** 刷新操作记忆（MainActivity.kt:1883）。 */
        val onRefreshMemory: () -> Unit = {},
        /** 删除一条操作记忆（MainActivity.kt:1890）。 */
        val onDeleteMemoryEntry: (String) -> Unit = {},

        /** 保存「打开 Jarvis 时自动连接」开关（MainActivity.kt:1917-1930）。 */
        val onAccessibilityAutoConnectChange: (Boolean) -> Unit = {},

        /** 保存并应用配置（MainActivity.kt:1839）。 */
        val onApply: () -> Unit = {},
        /** 保存、应用并发起一次主模型连通性验证（MainActivity.kt:1840）。 */
        val onApplyAndVerify: () -> Unit = {}
    )
}
