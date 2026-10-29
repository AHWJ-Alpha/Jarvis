package com.jarvis.ui.themes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.jarvis.app.R
import com.jarvis.app.runtime.ChatMessage
import com.jarvis.app.runtime.ChatMessageRole
import com.jarvis.app.runtime.RuntimeState
import com.jarvis.app.runtime.WorkspaceProfile
import com.jarvis.ui.components.*
import com.jarvis.ui.theme.*
import com.jarvis.ui.tokens.*
import kotlinx.coroutines.flow.first

/**
 * 对话空态插画池：三张竖构图轮换，同一会话固定一张（按 conversationId 取模），不叠在内容之上。
 * 新增插画只往这里加，资源 id 不散落到各处。
 */
private val deepEmptyStateArt = listOf(
    R.drawable.theme_deep_night_portrait_room,
    R.drawable.theme_deep_night_scene_a,
    R.drawable.theme_deep_night_scene_b
)

/**
 * 深海夜航：全宽 hero 沉浸头部（surface 渐隐蒙版）+ 自 hero 下沿上浮的大卡片承载对话与清单，输入区为胶囊加圆形发送键。
 */
internal class DeepNightTheme : JarvisThemeModule {
    override val id = "deep_night"
    override val label = "深海夜航"
    override val story = "潮汐来信"
    override val description = "海蓝 · 流动插画"
    override val mode = ThemeMode.DARK
    override val artwork = ThemeArtwork(
        banner = R.drawable.theme_deep_night_banner,
        avatar = R.drawable.theme_deep_night_avatar,
        texture = R.drawable.theme_deep_night_texture
    )
    override val chrome = ChromeStyle.DRAWER

    /** 身份色板（与 [darkPalette] 同值）：深海夜航的原生暗色。 */
    override val palette = ThemePalette(
        primary = Color(0xFF56C4E0), onPrimary = Color(0xFF00232E),
        secondary = Color(0xFF8FD6E8), onSecondary = Color(0xFF04222C),
        background = Color(0xFF050A10), onBackground = Color(0xFFE4EFF6),
        surface = Color(0xFF16232F), onSurface = Color(0xFFE4EFF6),
        surfaceContainerLow = Color(0xFF0C1620), surfaceContainer = Color(0xFF1B2A38),
        surfaceContainerHigh = Color(0xFF243543), onSurfaceVariant = Color(0xFFA3B7C6),
        outline = Color(0xFF3A5262), borderSubtle = Color(0xFF354B59), divider = Color(0xFF1A2630),
        error = Color(0xFFFF8A8A), onError = Color(0xFF3A0507),
        errorContainer = Color(0xFF4A1219), onErrorContainer = Color(0xFFFFD9DA),
        scrim = Color(0xFF000000), accentSoft = Color(0xFF12333F), glow = Color(0xFF7AD9F5),
        primaryContainer = Color(0xFF12333F), onPrimaryContainer = Color(0xFF7AD9F5)
    )

    /**
     * 暗色：深海夜航本体。层次全由明度差承担（卡片不画描边）：
     * borderSubtle/surface 1.75:1、surface 比 background 高 10.5 L*、onSurface 13.66:1、
     * onSurfaceVariant 7.71:1、onPrimary 8.10:1。surfaceContainer 三阶继续向上分层。
     */
    override val darkPalette = ThemePalette(
        primary = Color(0xFF56C4E0), onPrimary = Color(0xFF00232E),
        secondary = Color(0xFF8FD6E8), onSecondary = Color(0xFF04222C),
        background = Color(0xFF050A10), onBackground = Color(0xFFE4EFF6),
        surface = Color(0xFF16232F), onSurface = Color(0xFFE4EFF6),
        surfaceContainerLow = Color(0xFF0C1620), surfaceContainer = Color(0xFF1B2A38),
        surfaceContainerHigh = Color(0xFF243543), onSurfaceVariant = Color(0xFFA3B7C6),
        outline = Color(0xFF3A5262), borderSubtle = Color(0xFF354B59), divider = Color(0xFF1A2630),
        error = Color(0xFFFF8A8A), onError = Color(0xFF3A0507),
        errorContainer = Color(0xFF4A1219), onErrorContainer = Color(0xFFFFD9DA),
        scrim = Color(0xFF000000), accentSoft = Color(0xFF12333F), glow = Color(0xFF7AD9F5),
        primaryContainer = Color(0xFF12333F), onPrimaryContainer = Color(0xFF7AD9F5)
    )

    /**
     * 亮色：白日海面。不是把暗色调亮，而是换成「正午浅海」的独立一套：
     * 背景是带天光的海雾（#E3EFF6, L* 93.8），卡片是浪尖白（#FBFDFE, L* 99.2），
     * 文字是深海底色（#0B1E27），主色是收敛后的海水蓝（#0F6B85，配白字 6.07:1）。
     * 指标：borderSubtle/surface 1.80:1、ΔL* 5.43、onSurface 16.74:1、
     * onSurfaceVariant 6.98:1、onPrimary 6.07:1。
     */
    override val lightPalette = ThemePalette(
        primary = Color(0xFF0F6B85), onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF1E7C93), onSecondary = Color(0xFFFFFFFF),
        background = Color(0xFFE3EFF6), onBackground = Color(0xFF0B1E27),
        surface = Color(0xFFFBFDFE), onSurface = Color(0xFF0B1E27),
        surfaceContainerLow = Color(0xFFF3F9FC), surfaceContainer = Color(0xFFE9F3F8),
        surfaceContainerHigh = Color(0xFFDCEBF3), onSurfaceVariant = Color(0xFF3C5C6E),
        outline = Color(0xFF6E8FA2), borderSubtle = Color(0xFFA9C3D2), divider = Color(0xFFCFE0EA),
        error = Color(0xFFA8322C), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFE0DC), onErrorContainer = Color(0xFF440B08),
        scrim = Color(0xFF000000), accentSoft = Color(0xFFCDE9F4), glow = Color(0xFF7FD6EE),
        primaryContainer = Color(0xFFCDE9F4), onPrimaryContainer = Color(0xFF08414F)
    )

    override fun floatingPalette() = FloatingPaletteSpec(
        panel = palette.surface, title = palette.onSurface, body = palette.onSurfaceVariant,
        accent = palette.primary, muted = palette.outline, bubbleRing = palette.borderSubtle
    )

    private val defaultEfforts = listOf("off", "minimal", "low", "medium", "high", "max")

    @Composable
    private fun heroHeight(fraction: Float) = (LocalConfiguration.current.screenHeightDp * fraction).dp

    @Composable
    override fun ChatScreen(ctx: ChatUiContext) {
        val state = ctx.state
        val cb = ctx.callbacks
        val efforts = state.reasoningEfforts.ifEmpty { defaultEfforts }
        val listState = rememberLazyListState()
        val heroHeight = heroHeight(0.38f)
        LaunchedEffect(state.conversationId, state.messages.size) {
            if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
        }
        Box(Modifier.fillMaxSize()) {
            DeepHero(artwork, label, story, "对话", "在消息下方编辑、撤销或创建分支", heroHeight)
            Column(Modifier.fillMaxSize().padding(top = heroHeight - JarvisSpacing.space7)) {
                JarvisCard(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = JarvisSpacing.pageHorizontal),
                    contentPadding = JarvisSpacing.cardPadding) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val index = efforts.indexOf(state.reasoningEffort).coerceAtLeast(0)
                        JarvisIconAction(
                            icon = Icons.Default.Tune,
                            contentDescription = "思考深度：${state.reasoningEffort}，点击切换",
                            onClick = { cb.onReasoningEffortChange(efforts[(index + 1) % efforts.size]) })
                        Spacer(Modifier.weight(1f))
                        JarvisIconRow {
                            JarvisIconAction(Icons.Default.Build, "打开工具箱", onClick = { cb.onOpenTools() })
                            JarvisIconAction(Icons.Default.Add, "新建对话", onClick = { cb.onNewConversation() }, tone = JarvisActionTone.ACCENT)
                            JarvisIconAction(Icons.Default.CallSplit, "从当前对话创建分支", onClick = { cb.onBranch(null) })
                        }
                    }
                    JarvisCardHeaderGap()
                    if (state.messages.isEmpty()) {
                        DeepEmptyState(state.conversationId, Modifier.fillMaxWidth().weight(1f))
                    } else {
                        LazyColumn(state = listState, modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(JarvisSpacing.cardGap)) {
                            items(state.messages, key = { it.id }) { message -> DeepBubble(message, state, cb) }
                        }
                    }
                }
                Column(
                    Modifier.padding(horizontal = JarvisSpacing.pageHorizontal, vertical = JarvisSpacing.rowGap),
                    verticalArrangement = Arrangement.spacedBy(JarvisSpacing.rowGap)
                ) {
                    if (state.editingLatest) NoticeLine("正在修改上一条；发送后才会替换原对话轮次。", tone = NoticeTone.SUCCESS)
                    if (state.speechNotice.isNotBlank()) NoticeLine(state.speechNotice)
                    if (state.voiceListening) NoticeLine("正在听…")
                    DeepComposer(state.draft, state.editingLatest, state.sending, cb)
                    if (state.sending) JarvisLoading(if (state.streaming) "正在接收回复…" else "正在请求模型…")
                }
            }
        }
    }

    @Composable
    override fun TasksScreen(ctx: TasksUiContext) {
        val state = ctx.state
        val cb = ctx.callbacks
        val heroHeight = heroHeight(0.26f)
        Box(Modifier.fillMaxSize()) {
            DeepHero(artwork, label, story, "自动化", "通过对话创建，收到通知后按规则执行一次。", heroHeight)
            Column(
                Modifier.fillMaxSize().padding(top = heroHeight - JarvisSpacing.space7)
                    .verticalScroll(rememberScrollState()).padding(horizontal = JarvisSpacing.pageHorizontal),
                verticalArrangement = Arrangement.spacedBy(JarvisSpacing.cardGap)
            ) {
                val access = if (state.notificationAccess) "已授权" else "未授权"
                val a11y = if (state.accessibilityGranted) "已连接" else "未连接"
                DeepCard("运行前检查", "通知访问：${access} · 无障碍：${a11y}", icon = Icons.Outlined.Rule) {
                    NoticeLine("规则默认暂停；启用后只匹配目标应用通知，并在完成后核实结果。")
                    if (!state.runtimeReady) NoticeLine("运行时尚未就绪，启用后规则无法执行。", tone = NoticeTone.ERROR)
                    if (state.notice.isNotBlank()) NoticeLine(state.notice, tone = NoticeTone.ERROR)
                    JarvisIconRow {
                        if (!state.notificationAccess) {
                            JarvisIconAction(Icons.Default.Notifications, "授权通知访问", onClick = { cb.onRequestNotificationAccess() })
                        }
                        if (!state.accessibilityGranted) {
                            JarvisIconAction(Icons.Outlined.AccessibilityNew, "开启无障碍服务", onClick = { cb.onRequestAccessibility() })
                        }
                        JarvisIconAction(Icons.Default.Refresh, "重新检查运行状态", onClick = { cb.onRefresh() })
                    }
                }
                if (state.rules.isEmpty() && !state.creatingRule) {
                    EmptyStatePanel(
                        title = "还没有自动化", modifier = Modifier.fillMaxWidth(), icon = Icons.Default.Bolt,
                        subtitle = "在对话中输入“当哔哩哔哩上有人回复我就立刻回复”或“当 Pixiv 画师发布新作品就立刻下载”。",
                        action = { JarvisIconAction(Icons.Default.Add, "新建规则", onClick = cb.onCreateRuleStart, tone = JarvisActionTone.ACCENT) })
                } else if (!state.creatingRule) {
                    SectionHeader("自动化规则") { JarvisIconAction(Icons.Default.Add, "新建规则", onClick = { cb.onCreateRuleStart() }) }
                }
                if (state.creatingRule) {
                    DeepCard("新建自动化", "标题与触发要求必填；类型决定后续配置项。", icon = Icons.Outlined.Add) {
                        DeepFields(DeepFieldSpec("规则标题", state.ruleDraftTitle, cb.onCreateRuleTitleChange))
                        NoticeLine("类型")
                        val kinds = state.ruleDraftKindOptions
                        if (kinds.isEmpty()) {
                            NoticeLine("暂无可用类型，请先刷新。", tone = NoticeTone.ERROR)
                        } else {
                            val kindIndex = kinds.indexOfFirst { it.key == state.ruleDraftKindKey }.coerceAtLeast(0)
                            DeepChipRow(kinds.map { it.label }, kindIndex) { cb.onCreateRuleKindChange(kinds[it].key) }
                        }
                        DeepFields(
                            DeepFieldSpec(
                                "触发要求（例如：当哔哩哔哩上有人回复我就立刻回复）",
                                state.ruleDraftPrompt, cb.onCreateRulePromptChange, 2))
                        if (state.ruleDraftKindKey == "PIXIV_NEW_WORK") {
                            DeepFields(DeepFieldSpec("Pixiv 画师名称或用户 ID", state.ruleDraftArtist, cb.onCreateRuleArtistChange))
                        }
                        if (state.ruleDraftError.isNotBlank()) {
                            NoticeLine(state.ruleDraftError, tone = NoticeTone.ERROR)
                        }
                        NoticeLine("新建的规则默认暂停；启用前仍需通知访问与无障碍服务。")
                        JarvisIconRow {
                            JarvisIconAction(Icons.Default.Check, "创建规则", onClick = { cb.onCreateRuleSubmit() }, tone = JarvisActionTone.ACCENT)
                            JarvisIconAction(Icons.Default.Close, "取消新建规则", onClick = { cb.onCreateRuleCancel() })
                        }
                    }
                }
                state.rules.forEach { rule ->
                    JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                        SettingSwitchRow(
                            title = rule.title, checked = rule.enabled, subtitle = rule.kindLabel,
                            onCheckedChange = { enabled -> cb.onToggleRule(rule.id, enabled) })
                         NoticeLine(text = if (rule.enabled) "已启用 · 等待目标应用通知" else "已暂停 · 不会自动操作", tone = if (rule.enabled) NoticeTone.SUCCESS else NoticeTone.INFO)
                        rule.readiness?.let { NoticeLine("启用前：${it}", tone = NoticeTone.ERROR) }
                        if (rule.lastResult.isNotBlank()) NoticeLine("最近结果：${rule.lastResult}")
                        JarvisIconRow {
                            JarvisIconAction(Icons.Default.Settings, "配置规则", onClick = { cb.onConfigureRule(rule.id) })
                            JarvisIconAction(Icons.Default.Delete, "删除规则", onClick = { cb.onDeleteRule(rule.id) }, tone = JarvisActionTone.DANGER)
                        }
                    }
                }
                val editing = state.rules.firstOrNull { it.id == state.editingRuleId }
                if (editing != null) {
                    DeepCard("配置自动化", editing.title, icon = Icons.Outlined.Tune) {
                        NoticeLine("原始要求：${editing.sourcePrompt}")
                        if (editing.kindKey == "PIXIV_NEW_WORK") {
                            DeepFields(DeepFieldSpec("Pixiv 画师名称或用户 ID", state.artistDraft, cb.onArtistDraftChange))
                        }
                        if (editing.kindKey == "BILIBILI_REPLY") {
                            DeepFields(DeepFieldSpec("回复风格要求", state.replyDraft, cb.onReplyDraftChange, 2))
                        }
                        NoticeLine("启用前还需要目标应用通知访问和 Jarvis 无障碍服务。")
                        JarvisIconRow {
                            JarvisIconAction(Icons.Default.Check, "保存规则配置", onClick = {
                                cb.onSaveRuleConfig(editing.id, state.artistDraft, state.replyDraft)
                            }, tone = JarvisActionTone.ACCENT)
                            JarvisIconAction(Icons.Default.Close, "取消配置", onClick = { cb.onCancelRuleConfig() })
                        }
                    }
                }
                if (state.recentEvents.isNotEmpty()) SectionHeader("最近执行")
                state.recentEvents.take(6).forEach { event ->
                    NoticeLine(
                        text = "${if (event.ok) "✓" else "!"} ${event.title} · ${event.message}",
                        tone = if (event.ok) NoticeTone.SUCCESS else NoticeTone.ERROR)
                }
                Spacer(Modifier.height(JarvisSpacing.cardGap))
            }
        }
    }

    @Composable
    override fun WorkspaceScreen(ctx: WorkspaceUiContext) {
        val state = ctx.state
        val cb = ctx.callbacks
        var editing by remember { mutableStateOf<WorkspaceProfile?>(null) }
        var renamingId by remember { mutableStateOf<String?>(null) }
        var renameDraft by remember { mutableStateOf("") }
        var movingId by remember { mutableStateOf<String?>(null) }
        val heroHeight = heroHeight(0.26f)
        Box(Modifier.fillMaxSize()) {
            DeepHero(artwork, label, story, "工作区", "为每个工作区选择共同设定，并绑定一个只读目录。", heroHeight)
            Column(
                Modifier.fillMaxSize().padding(top = heroHeight - JarvisSpacing.space7)
                    .verticalScroll(rememberScrollState()).padding(horizontal = JarvisSpacing.pageHorizontal),
                verticalArrangement = Arrangement.spacedBy(JarvisSpacing.cardGap)
            ) {
                SettingSwitchRow(
                    title = "显示已归档",
                    checked = state.showArchived,
                    onCheckedChange = cb.onToggleShowArchived,
                    subtitle = "已归档 ${state.archivedWorkspaces.size} 个工作区 · ${state.archivedConversationIds.size} 个对话；归档不删任何数据")
                state.workspaces.forEach { profile ->
                    val count = state.conversationCountByWorkspace[profile.name] ?: 0
                    JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding, onClick = { cb.onSelect(profile.name) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.theme_deep_night_thumb), null,
                                Modifier.size(44.dp).clip(JarvisShapes.md), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(JarvisSpacing.space3))
                            Text(profile.name, style = JarvisType.titleBlock,
                                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            if (profile.name == state.selectedWorkspace) {
                                JarvisGlyphLabel(Icons.Default.CheckCircle, "当前", tone = JarvisActionTone.ACCENT)
                            }
                            if (profile.archived) {
                                Spacer(Modifier.width(JarvisSpacing.space2))
                                JarvisGlyphLabel(Icons.Default.Archive, "已归档")
                            }
                        }
                        val persona = if (profile.useGlobalPersona) "共同人格与世界书" else "自定义人格与世界书"
                        NoticeLine("${persona} · ${count} 个会话")
                        NoticeLine(when {
                            profile.dirLabel.isNotBlank() -> "目录：${profile.dirLabel}"
                            profile.hasDirectory -> "目录：已绑定（无显示名）"
                            else -> "目录：未绑定"
                        })
                        JarvisIconRow {
                            JarvisIconAction(Icons.Default.ArrowForward, "进入该工作区",
                                onClick = { cb.onSelect(profile.name) }, tone = JarvisActionTone.ACCENT)
                            JarvisIconAction(Icons.Default.Edit, "编辑工作区", onClick = { editing = profile })
                            if (profile.archived) {
                                JarvisIconAction(Icons.Default.Unarchive, "还原工作区", onClick = { cb.onUnarchive(profile.name) })
                            } else {
                                JarvisIconAction(Icons.Default.Archive, "归档工作区", onClick = { cb.onArchive(profile.name) })
                            }
                            JarvisIconAction(Icons.Default.FolderOpen,
                                if (profile.hasDirectory) "更换绑定目录" else "绑定只读目录",
                                onClick = { cb.onPickDirectory(profile.name) })
                            if (profile.hasDirectory) {
                                JarvisIconAction(Icons.Default.LinkOff, "解绑目录", onClick = { cb.onClearDirectory(profile.name) })
                            }
                            JarvisIconAction(Icons.Default.Delete, "删除工作区",
                                onClick = { cb.onDelete(profile.name) }, tone = JarvisActionTone.DANGER)
                        }
                    }
                }
                DeepCard(
                    "目录文件",
                    if (state.dirReady) "只读清单，最多 200 条；文本可展开预览"
                    else "当前工作区没有可用目录，先在上面绑定一个",
                    icon = Icons.Outlined.FolderOpen) {
                    if (!state.dirReady) {
                        NoticeLine("未绑定目录或读权限已失效：用工作区卡上的「绑定目录 / 换目录」重新选择。", tone = NoticeTone.ERROR)
                    }
                    if (state.filesLoading) JarvisLoading("正在读取目录…")
                    if (state.filesNotice.isNotBlank()) NoticeLine(state.filesNotice)
                    if (!state.filesLoading && state.files.isEmpty()) NoticeLine("目录为空或尚未读取。")
                    state.files.take(60).forEach { file -> DeepFileRow(file) }
                    if (state.files.size > 60) NoticeLine("还有 ${state.files.size - 60} 项未列出。")
                }
                DeepCard("会话", "每条对话可改名、移动、归档或删除；归档后仍可还原", avatar = R.drawable.theme_deep_night_thumb) {
                    if (state.conversations.isEmpty()) NoticeLine("当前没有会话。")
                    val shown = state.conversations.take(30)
                    shown.forEachIndexed { index, session ->
                        ListRow(
                            modifier = Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight),
                            title = session.title.ifBlank { "未命名对话" },
                            subtitle = session.workspace + if (session.isArchived) " · 已归档" else "")
                        JarvisIconRow {
                            JarvisIconAction(Icons.Default.Edit, "重命名会话", onClick = {
                                renamingId = if (renamingId == session.id) null else session.id
                                renameDraft = session.title
                            })
                            JarvisIconAction(Icons.Default.SwapHoriz, "移动到其他工作区", onClick = {
                                movingId = if (movingId == session.id) null else session.id
                            })
                            if (session.isArchived) {
                                JarvisIconAction(Icons.Default.Unarchive, "还原会话", onClick = { cb.onUnarchiveConversation(session.id) })
                            } else {
                                JarvisIconAction(Icons.Default.Archive, "归档会话", onClick = { cb.onArchiveConversation(session.id) })
                            }
                            JarvisIconAction(Icons.Default.Delete, "删除会话",
                                onClick = { cb.onDeleteConversation(session.id) }, tone = JarvisActionTone.DANGER)
                        }
                        if (renamingId == session.id) {
                            DeepFields(DeepFieldSpec("会话标题", renameDraft, { renameDraft = it.take(60) }))
                            JarvisIconRow {
                                JarvisIconAction(Icons.Default.Check, "保存新标题",
                                    enabled = renameDraft.isNotBlank(),
                                    onClick = {
                                        cb.onRenameConversation(session.id, renameDraft.trim())
                                        renamingId = null
                                    },
                                    tone = JarvisActionTone.ACCENT)
                                JarvisIconAction(Icons.Default.Close, "取消改名", onClick = { renamingId = null })
                            }
                        }
                        if (movingId == session.id) {
                            val targets = state.workspaces.map { it.name }.filter { it != session.workspace }
                            if (targets.isEmpty()) {
                                NoticeLine("没有其他工作区可以移动。")
                            } else {
                                NoticeLine("移动到：")
                                DeepChipRow(targets, -1) { picked ->
                                    cb.onMoveConversation(session.id, targets[picked])
                                    movingId = null
                                }
                            }
                        }
                        if (index != shown.lastIndex) JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    }
                    if (state.conversations.size > shown.size) {
                        NoticeLine("还有 ${state.conversations.size - shown.size} 个会话未列出。")
                    }
                }
                DeepWorkspaceForm(artwork, editing, state, cb) { editing = null }
                Spacer(Modifier.height(JarvisSpacing.cardGap))
            }
        }
    }

    @Composable
    override fun SettingsScreen(ctx: SettingsUiContext) {
        val state = ctx.state
        val cb = ctx.callbacks
        val heroHeight = heroHeight(0.24f)
        var pluginDraft by remember { mutableStateOf("") }
        Box(Modifier.fillMaxSize()) {
            DeepHero(artwork, label, story, "设置", "模型、权限与设备连接", heroHeight)
            Column(
                Modifier.fillMaxSize().padding(top = heroHeight - JarvisSpacing.space7)
                    .verticalScroll(rememberScrollState()).padding(horizontal = JarvisSpacing.pageHorizontal),
                verticalArrangement = Arrangement.spacedBy(JarvisSpacing.cardGap)
            ) {
                Image(
                    painterResource(R.drawable.theme_deep_night_portrait_header), null,
                    Modifier.fillMaxWidth().height(120.dp).clip(JarvisShapes.lg),
                    contentScale = ContentScale.Crop)
                DeepCard("风格与外观", "图片与控件同步切换，每套主题使用统一色系", avatar = artwork.avatar) {
                    val current = state.themeOptions.firstOrNull { it.id == state.themeId }
                    NoticeLine("当前：${current?.label ?: state.theme.label}", tone = NoticeTone.SUCCESS)
                    if (current != null) NoticeLine("${current.story} · ${current.description}")
                    DeepChipRow(state.themeOptions.map { it.label },
                        state.themeOptions.indexOfFirst { it.id == state.themeId },
                        { cb.onThemeChangeById(state.themeOptions[it].id) })
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    val modes = listOf(ThemeModeOverride.FOLLOW_SYSTEM, ThemeModeOverride.LIGHT, ThemeModeOverride.DARK)
                    val modeLabels = listOf("跟随系统", "浅色", "深色")
                    NoticeLine("明暗：${modeLabels[modes.indexOf(state.modeOverride).coerceAtLeast(0)]} · 当前渲染 ${if (state.effectiveMode == ThemeMode.LIGHT) "浅色" else "深色"}")
                    DeepChipRow(modeLabels, modes.indexOf(state.modeOverride),
                        { cb.onModeOverrideChange(modes[it]) })
                }
                DeepCard("OpenClaw 运行时", "单 APK 运行目录和插件目录状态", icon = Icons.Outlined.Cloud) {
                    val report = state.bootstrapReport
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JarvisGlyphLabel(runtimeIcon(report.state), runtimeLabel(report.state), tone = runtimeTone(report.state))
                        Spacer(Modifier.width(JarvisSpacing.space3))
                        NoticeLine("版本：${state.versionName.ifBlank { report.runtimeVersion }}")
                    }
                    val root = if (report.rootAvailable) "已检测到" else "未检测到"
                    val token = if (report.bridgeTokenReady) "已生成" else "缺失"
                    NoticeLine("root：${root} · Bridge Token：${token}")
                    NoticeLine("插件目录：${report.pluginDirectory}")
                    if (state.runtimeMessage.isNotBlank()) NoticeLine(state.runtimeMessage)
                    JarvisIconRow {
                        JarvisIconAction(Icons.Default.PlayArrow, "启动 Gateway", onClick = { cb.onStartRuntime() }, tone = JarvisActionTone.ACCENT)
                        JarvisIconAction(Icons.Default.Stop, "停止 Gateway", onClick = { cb.onStopRuntime() })
                        JarvisIconAction(Icons.Default.Refresh, "重新检查运行时", onClick = { cb.onRefreshRuntime() })
                    }
                }
                DeepCard("模型与连接", "兼容 OpenAI Chat Completions 的服务；关闭思考时不发送 reasoning_effort", icon = Icons.Outlined.Router) {
                    SettingSwitchRow("限制模式", state.restrictionMode, cb.onRestrictionChange,
                        subtitle = "默认开启，控制高风险内容和动作；关闭后会持续显示警告状态")
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    SettingSwitchRow("启用命令通道", state.commandChannelEnabled, cb.onCommandChange,
                        subtitle = "指定会话可发送 /claw 命令，其余消息走普通人格")
                    SettingSwitchRow("深度思考", state.deepThinkingEnabled, cb.onDeepThinkingChange)
                    val efforts = state.reasoningEfforts.ifEmpty { defaultEfforts }
                    DeepChipRow(efforts, efforts.indexOf(state.reasoningEffort).coerceAtLeast(0),
                        { cb.onReasoningEffortChange(efforts[it]) })
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    DeepFields(
                        DeepFieldSpec("命令前缀", state.commandPrefix, cb.onPrefixChange),
                        DeepFieldSpec("模型 Base URL", state.modelBaseUrl, cb.onModelBaseUrlChange),
                        DeepFieldSpec("模型名", state.modelName, cb.onModelNameChange),
                        DeepFieldSpec("模型 API Key", state.modelApiKey, cb.onModelApiKeyChange, secret = true),
                        DeepFieldSpec("模型回退链（每行一个，可选）", state.modelChain, cb.onModelChainChange, 3),
                        DeepFieldSpec("深度思考模型（可选）", state.deepThinkingModel, cb.onDeepThinkingModelChange),
                        DeepFieldSpec("识图 Base URL", state.visionBaseUrl, cb.onVisionBaseUrlChange),
                        DeepFieldSpec("识图模型名", state.visionModel, cb.onVisionModelChange),
                        DeepFieldSpec("识图 API Key", state.visionApiKey, cb.onVisionApiKeyChange, secret = true)
                    )
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    JarvisIconRow {
                        JarvisIconAction(Icons.Default.Check, "保存并生效", onClick = { cb.onApply() }, tone = JarvisActionTone.ACCENT)
                        JarvisIconAction(Icons.Default.CheckCircle, "保存并验证主模型", onClick = { cb.onApplyAndVerify() })
                        if (state.saving) JarvisLoading("应用中…")
                    }
                    if (state.applyNotice.isNotBlank()) {
                        NoticeLine(state.applyNotice, tone = if (state.applyFailed) NoticeTone.ERROR else NoticeTone.SUCCESS)
                    }
                }
                DeepCard("root 模式", "完全模式会在启用前显示三秒警告；所有动作写入审计日志", icon = Icons.Outlined.AdminPanelSettings) {
                    state.rootModes.forEach { mode ->
                        val current = mode.key == state.rootModeKey
                        ListRow(
                            title = mode.label,
                            subtitle = if (current) "当前：${mode.label}" else null,
                            trailing = if (current) null else ({ JarvisGlyphLabel(Icons.Default.SwapHoriz, "切换") }),
                            onClick = { cb.onRootModeRequest(mode) })
                    }
                }
                DeepCard("全局人格与 AI Classroom", "供选择“使用共同设定”的工作区使用", icon = Icons.Outlined.MenuBook) {
                     DeepFields(DeepFieldSpec("全局提示词", state.mentorPrompt, cb.onMentorPromptChange, 3), DeepFieldSpec("全局世界书", state.worldBook, cb.onWorldBookChange, 3))
                    if (state.importNotice.isNotBlank()) NoticeLine(state.importNotice, tone = NoticeTone.SUCCESS)
                    if (state.exportNotice.isNotBlank()) NoticeLine(state.exportNotice)
                    JarvisIconRow {
                        JarvisIconAction(Icons.Default.Download, "导入 AI Classroom 数据", onClick = { cb.onImportAiClassroom() })
                        JarvisIconAction(Icons.Default.Upload, "导出 AI Classroom 数据", onClick = { cb.onExportAiClassroom() })
                    }
                }
                DeepCard("远程访问", "通过局域网或 Tailscale 访问网页控制台", icon = Icons.Outlined.Lan) {
                    SettingSwitchRow("启用远程网页控制台", state.serverRunning,
                        onCheckedChange = { enabled -> if (enabled) cb.onStartServer() else cb.onStopServer() },
                        subtitle = if (state.serverRunning) "已开启；配对令牌 5 分钟内有效" else "默认关闭；只允许已配对的浏览器提交任务")
                    if (state.pairingCode.isNotBlank()) {
                        NoticeLine("一次性配对码：${state.pairingCode}", tone = NoticeTone.SUCCESS)
                        JarvisIconRow {
                            JarvisIconAction(Icons.Default.ContentCopy, "复制配对码", onClick = { cb.onCopyPairingCode() })
                            JarvisIconAction(Icons.Default.LinkOff, "撤销配对", onClick = { cb.onRevokePairing() })
                        }
                    }
                    JarvisIconRow {
                        JarvisIconAction(Icons.Default.VpnKey, "生成配对码", onClick = { cb.onNewPairingCode() }, tone = JarvisActionTone.ACCENT)
                        JarvisIconAction(Icons.Default.Refresh, "刷新远程地址", onClick = { cb.onRefreshRemoteUrls() })
                    }
                    state.remoteUrls.forEach { NoticeLine("${it}") }
                    SettingSwitchRow("允许远程设备调用本机能力", state.remoteDeviceControl, cb.onRemoteDeviceControlChange)
                }
                DeepCard("运行时工具与插件", "查看 CMake、FFmpeg、Python、Rust 等命令是否可用", icon = Icons.Outlined.Extension) {
                    if (state.checkingTools) JarvisLoading("正在检查工具路径")
                    else JarvisIconAction(Icons.Default.Refresh, "重新检查工具路径", onClick = { cb.onRefreshTools() })
                    val cmake = if (state.cmakeReady) "可用" else "缺失"
                    val ffmpeg = if (state.ffmpegReady) "可用" else "缺失"
                    NoticeLine("CMake：${cmake} · FFmpeg：${ffmpeg}")
                    state.runtimeTools.forEach { tool ->
                        val detail = state.tools.firstOrNull { it.id == tool.name || it.label == tool.name }
                        val source = detail?.sourceLabel.orEmpty()
                        val subtitle = if (tool.installed) tool.version.ifBlank { "已安装" } else "未安装"
                        ListRow(
                            title = tool.name,
                            subtitle = if (source.isBlank() || subtitle.contains(source)) subtitle else "${subtitle} · ${source}",
                            trailing = if (tool.installed) null else ({
                                JarvisIconAction(Icons.Default.Download, "安装 ${tool.name}",
                                    onClick = { cb.onInstallTool(detail?.id ?: tool.name) })
                            })
                        )
                    }
                    if (state.toolNotice.isNotBlank()) NoticeLine(state.toolNotice)
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NoticeLine("社区插件 ${state.plugins.size} 个", tone = NoticeTone.SUCCESS)
                        Spacer(Modifier.weight(1f))
                        if (state.pluginBusy) JarvisLoading("处理中")
                        else JarvisIconAction(Icons.Default.Refresh, "刷新插件列表", onClick = { cb.onRefreshPlugins() })
                    }
                    state.plugins.forEach { plugin ->
                        SettingSwitchRow(
                            title = plugin.name, checked = plugin.enabled,
                            subtitle = "${plugin.version} · ${plugin.tools.size} 个工具",
                            onCheckedChange = { enabled -> cb.onTogglePlugin(plugin.id, enabled) })
                        JarvisIconAction(Icons.Default.Delete, "卸载 ${plugin.name}",
                            onClick = { cb.onUninstallPlugin(plugin.id) }, tone = JarvisActionTone.DANGER)
                    }
                    if (state.pluginNotice.isNotBlank()) NoticeLine(state.pluginNotice)
                    DeepFields(
                        DeepFieldSpec("粘贴插件草稿（JSON 或清单文本）", pluginDraft, { pluginDraft = it }, 3)
                    )
                    JarvisIconAction(Icons.Default.Add, "安装插件草稿", enabled = pluginDraft.isNotBlank(), onClick = {
                        cb.onInstallPlugin(pluginDraft)
                        pluginDraft = ""
                    }, tone = JarvisActionTone.ACCENT)
                }
                DeepCard("操作记忆", "首次成功后学习，后续优先复用；失败时重新探索", icon = Icons.Outlined.History) {
                    if (state.memoryStatus.isNotBlank()) NoticeLine(state.memoryStatus)
                    state.memoryEntries.forEach { entry ->
                        ListRow(
                            title = entry.title,
                            subtitle = if (entry.ready) "可复用 · 成功 ${entry.successes} / 失败 ${entry.failures}"
                            else "已失效，待重新学习 · 成功 ${entry.successes} / 失败 ${entry.failures}",
                            trailing = { JarvisIconAction(Icons.Default.Delete, "删除该操作记忆",
                                onClick = { cb.onDeleteMemoryEntry(entry.id) }, tone = JarvisActionTone.DANGER) })
                    }
                    JarvisIconAction(Icons.Default.Refresh, "刷新操作路径", onClick = { cb.onRefreshMemory() })
                }
                DeepCard("悬浮助手与无障碍", "语音识别后直接执行；首次需要悬浮窗与无障碍授权", icon = Icons.Outlined.AccessibilityNew) {
                    SettingSwitchRow("启用悬浮球", state.floatingEnabled, cb.onFloatingEnabledChange,
                        subtitle = "语音任务识别后会直接交给 Jarvis 执行")
                    val overlay = if (state.overlayGranted) "已授权" else "未授权"
                    val a11y = if (state.accessibilityGranted) "已启用" else "未启用"
                    NoticeLine("悬浮窗：${overlay} · 无障碍：${a11y}")
                    if (state.accessibilityStatus.isNotBlank()) NoticeLine(state.accessibilityStatus)
                    JarvisIconRow {
                        JarvisIconAction(Icons.Default.OpenInNew, "悬浮窗权限", onClick = { cb.onOverlayPermissionRequest() })
                        JarvisIconAction(Icons.Default.Settings, "无障碍设置", onClick = { cb.onAccessibilitySettingsOpen() })
                    }
                    SettingSwitchRow("打开 Jarvis 时自动连接", state.accessibilityAutoConnect,
                        cb.onAccessibilityAutoConnectChange, subtitle = "需要 root 完全模式；关闭不会撤销已有授权")
                }
                DeepCard("浮窗外观", "边长、形状与暂时隐藏秒数即时生效", icon = Icons.Outlined.Palette) {
                    NoticeLine("头像边长：${state.avatarSizeDp} dp")
                    DeepChipRow(state.avatarSizeChoices.map { "${it}dp" },
                        state.avatarSizeChoices.indexOf(state.avatarSizeDp),
                        { cb.onAvatarSizeChange(state.avatarSizeChoices[it]) })
                    NoticeLine("悬浮球边长：${state.floatingSizeDp} dp")
                    DeepChipRow(state.avatarSizeChoices.map { "${it}dp" },
                        state.avatarSizeChoices.indexOf(state.floatingSizeDp),
                        { cb.onFloatingSizeChange(state.avatarSizeChoices[it]) })
                    if (state.floatingShapes.isNotEmpty()) {
                        NoticeLine("悬浮球形状：${state.floatingShapes.firstOrNull { it.key == state.floatingShapeKey }?.label ?: state.floatingShapeKey}")
                        DeepChipRow(state.floatingShapes.map { it.label },
                            state.floatingShapes.indexOfFirst { it.key == state.floatingShapeKey },
                            { cb.onFloatingShapeChange(state.floatingShapes[it].key) })
                    }
                    NoticeLine("暂时隐藏：${state.avatarHideSeconds} 秒")
                    DeepChipRow(state.hideSecondChoices.map { "${it}秒" },
                        state.hideSecondChoices.indexOf(state.avatarHideSeconds),
                        { cb.onAvatarHideSecondsChange(state.hideSecondChoices[it]) })
                }
                DeepCard("手势任务", "双击/三击悬浮球时执行；可绑定已有自动化", icon = Icons.Outlined.Gesture) {
                    if (state.gestureOptions.isNotEmpty()) {
                        NoticeLine("双击操作：${state.gestureOptions.firstOrNull { it.key == state.doubleGestureKey }?.label ?: state.doubleGestureKey}")
                        DeepChipRow(state.gestureOptions.map { it.label },
                            state.gestureOptions.indexOfFirst { it.key == state.doubleGestureKey },
                            { cb.onDoubleGestureChange(state.gestureOptions[it].key) })
                    }
                    DeepFields(DeepFieldSpec("双击时执行的任务", state.doubleTask, cb.onDoubleTaskChange))
                    state.savedAutomations.take(3).forEach { rule ->
                        ListRow(title = rule.title, subtitle = "双击选用 · ${rule.kindLabel}") {
                            cb.onSelectAutomationForGesture(state.doubleGestureKey, rule.id)
                        }
                    }
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    if (state.gestureOptions.isNotEmpty()) {
                        NoticeLine("三击操作：${state.gestureOptions.firstOrNull { it.key == state.tripleGestureKey }?.label ?: state.tripleGestureKey}")
                        DeepChipRow(state.gestureOptions.map { it.label },
                            state.gestureOptions.indexOfFirst { it.key == state.tripleGestureKey },
                            { cb.onTripleGestureChange(state.gestureOptions[it].key) })
                    }
                    DeepFields(DeepFieldSpec("三击时执行的任务", state.tripleTask, cb.onTripleTaskChange))
                    state.savedAutomations.take(3).forEach { rule ->
                        ListRow(title = rule.title, subtitle = "三击选用 · ${rule.kindLabel}") {
                            cb.onSelectAutomationForGesture(state.tripleGestureKey, rule.id)
                        }
                    }
                }
                Spacer(Modifier.height(JarvisSpacing.cardGap))
            }
        }
    }
}

/** chip 行：行上下与 chip 之间各留 space3；选中态只用实心底色与文字色区分，不画任何描边。 */
@Composable
private fun DeepChipRow(items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val positions = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }
    val edgeMargin = with(LocalDensity.current) { JarvisSpacing.space3.roundToPx() }
    LaunchedEffect(selectedIndex, items.size) {
        if (items.isEmpty()) return@LaunchedEffect
        val target = selectedIndex.coerceIn(0, items.lastIndex)
        val (start, width) = snapshotFlow { positions[target] }.first { it != null }!!
        val visibleStart = scrollState.value
        val visibleEnd = visibleStart + scrollState.viewportSize
        if (start < visibleStart || start + width > visibleEnd) {
            scrollState.animateScrollTo((start - edgeMargin).coerceIn(0, scrollState.maxValue))
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = JarvisSpacing.space3).horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, label ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                border = null,
                label = { Text(label, style = JarvisType.titleItem, maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = scheme.surfaceContainer,
                    labelColor = scheme.onSurfaceVariant,
                    selectedContainerColor = scheme.primaryContainer,
                    selectedLabelColor = scheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .height(JarvisSize.chipHeight)
                    .onGloballyPositioned { coordinates ->
                        val next = coordinates.positionInParent().x.toInt() to coordinates.size.width
                        if (positions[index] != next) positions[index] = next
                    }
            )
        }
    }
}

private data class DeepFieldSpec(val label: String, val value: String, val onChange: (String) -> Unit, val minLines: Int = 1, val secret: Boolean = false)

/** 对话空态：轮换插画（≤200dp，在内容流里）+ 两行文案，不叠内容、不做满屏背景。 */
@Composable
private fun DeepEmptyState(conversationId: String, modifier: Modifier = Modifier) {
    val art = remember(conversationId) {
        val raw = conversationId.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
        deepEmptyStateArt[raw % deepEmptyStateArt.size]
    }
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(vertical = JarvisSpacing.space4),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painterResource(art), null,
            Modifier.fillMaxWidth().height(180.dp).clip(JarvisShapes.md),
            contentScale = ContentScale.Crop)
        Spacer(Modifier.height(JarvisSpacing.space5))
        Text("让想法，开始行动。", style = JarvisType.titleBlock, color = scheme.onSurface)
        Spacer(Modifier.height(JarvisSpacing.space2))
        Text("交给 Jarvis，逐步完成。", style = JarvisType.bodyMeta, color = scheme.onSurfaceVariant)
    }
}

/** 目录里的一行：默认只读展示；可预览的文本点开后原地展开预览，不弹窗、不跳页。 */
@Composable
private fun DeepFileRow(file: WorkspaceFileView) {
    var expanded by remember(file.name, file.previewText) { mutableStateOf(false) }
    ListRow(
        modifier = Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight),
        title = file.name.ifBlank { "（未命名）" },
        subtitle = buildString {
            append(if (file.isDirectory) "目录" else deepFileSize(file.sizeBytes))
            if (file.previewable) append(" · 可预览")
        },
        trailing = if (file.previewable) ({
            JarvisIconAction(
                icon = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起预览" else "预览文本",
                onClick = { expanded = !expanded })
        }) else null,
        onClick = if (file.previewable) ({ expanded = !expanded }) else null)
    if (expanded) {
        JarvisInsetCard(
            modifier = Modifier.fillMaxWidth().padding(top = JarvisSpacing.space2),
            contentPadding = JarvisSpacing.insetCardPadding
        ) {
            Text(file.previewText, style = JarvisType.codeInline,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun deepFileSize(bytes: Long): String = when {
    bytes < 0L -> "大小未知"
    bytes < 1024L -> "${bytes} B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
}

@Composable
private fun DeepHero(artwork: ThemeArtwork, label: String, story: String, title: String, subtitle: String, height: Dp) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().height(height)) {
        Image(painterResource(artwork.banner), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, scheme.surface))))
        Column(
            Modifier.align(Alignment.BottomStart).padding(
                start = JarvisSpacing.pageHorizontal, end = JarvisSpacing.pageHorizontal,
                top = JarvisSpacing.space6, bottom = JarvisSpacing.space8 + JarvisSpacing.space2)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.primary)
            Spacer(Modifier.height(JarvisSpacing.space2))
            Text(story, style = MaterialTheme.typography.titleMedium, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(JarvisSpacing.space3))
            Text(title, style = MaterialTheme.typography.displayLarge, color = scheme.onSurface)
            Spacer(Modifier.height(JarvisSpacing.space3))
            Text(subtitle, style = JarvisType.bodyMeta, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeepBubble(message: ChatMessage, state: ChatUiContext.ChatState, cb: ChatUiContext.ChatCallbacks) {
    val scheme = MaterialTheme.colorScheme
    val isUser = message.role == ChatMessageRole.USER
    val lastUserId = state.messages.lastOrNull { it.role == ChatMessageRole.USER }?.id
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(
            text = if (isUser) "你" else "Jarvis", style = MaterialTheme.typography.labelSmall, color = scheme.primary,
            modifier = Modifier.padding(start = JarvisSpacing.space2, bottom = JarvisSpacing.space2))
        Box(Modifier.fillMaxWidth(0.82f), contentAlignment = Alignment.TopStart) {
            Column(
                Modifier.clip(if (isUser) JarvisShapes.md else JarvisShapes.lg)
                    .background(if (isUser) scheme.primaryContainer else scheme.surfaceContainer)
                    .padding(horizontal = JarvisSpacing.space5, vertical = JarvisSpacing.space3)) {
                Text(message.content, style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) scheme.onPrimaryContainer else scheme.onSurface)
            }
        }
        JarvisIconRow {
            JarvisIconAction(Icons.Default.CallSplit, "从这条消息创建分支",
                enabled = !state.sending, onClick = { cb.onBranch(message.id) })
            if (isUser && message.id == lastUserId) {
                val editable = !state.sending && (state.editingLatest || state.canEditLatest)
                if (state.editingLatest) {
                    JarvisIconAction(Icons.Default.Close, "取消编辑", enabled = editable, onClick = { cb.onCancelEdit() })
                } else {
                    JarvisIconAction(Icons.Default.Edit, "编辑上一条消息", enabled = editable, onClick = { cb.onEditLatest() })
                }
            }
            if (message.id == state.messages.lastOrNull()?.id) {
                JarvisIconAction(Icons.Default.Undo, "撤销最后一条", enabled = state.canUndoLatest, onClick = cb.onUndoLatest)
            }
        }
    }
}

@Composable
private fun DeepComposer(draft: String, editing: Boolean, sending: Boolean, cb: ChatUiContext.ChatCallbacks) {
    val scheme = MaterialTheme.colorScheme
    val sendEnabled = !sending && draft.isNotBlank()
    Surface(
        shape = JarvisShapes.pill, color = scheme.surfaceContainer,
        // outlineVariant 由 ThemePalette.colorScheme() 映射到 borderSubtle，只有这一条路能取到它。
        border = JarvisContrast.hairlineBorder(scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
             modifier = Modifier.padding(start = JarvisSpacing.space5, end = JarvisSpacing.space2, top = JarvisSpacing.space2, bottom = JarvisSpacing.space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = draft, onValueChange = cb.onDraftChange, modifier = Modifier.weight(1f),
                textStyle = JarvisType.bodyBase.copy(fontWeight = FontWeight.Medium, color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary), maxLines = 4,
                decorationBox = { inner ->
                    Box {
                        if (draft.isEmpty()) {
                             Text(text = if (editing) "修改上一条消息后重新发送…" else "给 Jarvis 发消息，或描述一个任务…", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                        }
                        inner()
                    }
                }
            )
            JarvisIconAction(Icons.Default.Mic, "语音转文字", onClick = { cb.onVoiceInput() })
            IconButton(
                onClick = { cb.onSend(draft.trim()) }, enabled = sendEnabled,
                modifier = Modifier.size(JarvisSize.sendButton).clip(JarvisShapes.pill)
                    .background(if (sendEnabled) scheme.primary else scheme.surfaceContainerHigh)
            ) {
                val tint = if (sendEnabled) scheme.onPrimary else scheme.onSurfaceVariant
                Icon(Icons.Default.Send, "发送", modifier = Modifier.size(20.dp), tint = tint)
            }
        }
    }
}

@Composable
private fun DeepCard(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    avatar: Int? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (avatar != null) {
                Image(painterResource(avatar), null, Modifier.size(44.dp).clip(JarvisShapes.sm),
                    contentScale = ContentScale.Crop)
            } else if (icon != null) {
                Box(
                    Modifier.size(40.dp).clip(JarvisShapes.sm).background(scheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, modifier = Modifier.size(20.dp), tint = scheme.primary)
                }
            }
            Spacer(Modifier.width(JarvisSpacing.space3))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                Spacer(Modifier.height(JarvisSpacing.space2))
                Text(subtitle, style = JarvisType.bodyMeta, color = scheme.onSurfaceVariant)
            }
        }
        JarvisCardHeaderGap()
        content()
    }
}

@Composable
private fun DeepFields(vararg specs: DeepFieldSpec) {
    Column(verticalArrangement = Arrangement.spacedBy(JarvisSpacing.rowGap)) {
        specs.forEach { spec ->
            if (spec.secret) {
                JarvisSecretField(
                    label = spec.label, value = spec.value, onValueChange = spec.onChange,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    accentColor = MaterialTheme.colorScheme.primary)
                return@forEach
            }
            TextField(
                value = spec.value, onValueChange = spec.onChange, modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(spec.label, style = JarvisType.bodyMeta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                textStyle = JarvisType.bodyBase.copy(
                    fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface),
                shape = JarvisShapes.sm, minLines = spec.minLines,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
private fun DeepWorkspaceForm(
    artwork: ThemeArtwork,
    initial: WorkspaceProfile?,
    state: WorkspaceUiContext.WorkspaceState,
    cb: WorkspaceUiContext.WorkspaceCallbacks,
    onDismiss: () -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var useGlobal by remember(initial) { mutableStateOf(initial?.useGlobalPersona ?: true) }
    var persona by remember(initial) { mutableStateOf(initial?.mentorPrompt ?: "") }
    var worldBook by remember(initial) { mutableStateOf(initial?.worldBook ?: "") }
    val clean = name.trim()
    JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(JarvisShapes.sm).background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Folder, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(JarvisSpacing.space4))
            Text(if (initial == null) "新建工作区" else "编辑工作区",
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        JarvisCardHeaderGap()
        DeepFields(DeepFieldSpec("工作区名称", name, { name = it.take(60) }))
        SettingSwitchRow(
            title = "使用共同设定",
            subtitle = if (useGlobal) "此工作区会跟随设置中的全局人格与世界书。" else "使用下方自定义人格与世界书。",
            checked = useGlobal, onCheckedChange = { useGlobal = it })
        if (!useGlobal) {
            DeepFields(
                DeepFieldSpec("自定义人格 / 提示词", persona.ifBlank { state.globalPersona }, { persona = it }, 3),
                DeepFieldSpec("自定义世界书", worldBook.ifBlank { state.globalWorldBook }, { worldBook = it }, 3))
            state.personaSources.take(3).forEach { source ->
                ListRow(title = source.title, subtitle = "从其他对话粘贴 · ${source.workspace}") {
                    persona = source.mentorPrompt
                    worldBook = source.worldBook
                }
            }
        }
        JarvisCardHeaderGap()
        JarvisIconRow {
            JarvisIconAction(Icons.Default.Check,
                if (initial == null) "创建并进入工作区" else "保存工作区",
                enabled = clean.isNotEmpty(),
                onClick = {
                    cb.onSave(
                        WorkspaceProfile(
                            name = clean, useGlobalPersona = useGlobal,
                            mentorPrompt = if (useGlobal) "" else persona.trim(),
                            worldBook = if (useGlobal) "" else worldBook.trim()),
                        initial?.name)
                },
                tone = JarvisActionTone.ACCENT)
            if (initial != null) JarvisIconAction(Icons.Default.Close, "取消编辑工作区", onClick = onDismiss)
        }
    }
}

private fun runtimeLabel(state: RuntimeState): String = when (state) {
    RuntimeState.READY -> "就绪"
    RuntimeState.RUNNING -> "运行中"
    RuntimeState.DEGRADED -> "兼容模式"
    RuntimeState.NOT_CONFIGURED -> "未配置"
    RuntimeState.ERROR -> "配置错误"
}

/** 运行时状态 -> 图标与语气：状态本身靠图标形状 + 颜色表达，文字只留 2~4 个汉字。 */
private fun runtimeIcon(state: RuntimeState): ImageVector = when (state) {
    RuntimeState.READY, RuntimeState.RUNNING -> Icons.Default.CheckCircle
    RuntimeState.ERROR -> Icons.Default.Warning
    RuntimeState.DEGRADED, RuntimeState.NOT_CONFIGURED -> Icons.Default.Info
}

private fun runtimeTone(state: RuntimeState): JarvisActionTone = when (state) {
    RuntimeState.READY, RuntimeState.RUNNING -> JarvisActionTone.ACCENT
    RuntimeState.ERROR -> JarvisActionTone.DANGER
    RuntimeState.DEGRADED, RuntimeState.NOT_CONFIGURED -> JarvisActionTone.NEUTRAL
}
