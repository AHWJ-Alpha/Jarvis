package com.jarvis.ui.themes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.app.R
import com.jarvis.app.runtime.ChatMessage
import com.jarvis.app.runtime.ChatMessageRole
import com.jarvis.app.runtime.ChatSession
import com.jarvis.app.runtime.RuntimeState
import com.jarvis.app.runtime.WorkspaceProfile
import com.jarvis.ui.components.*
import com.jarvis.ui.theme.*
import com.jarvis.ui.tokens.*
import kotlinx.coroutines.flow.first

/**
 * 樱夜校园的两套手写色板（22 个必填字段全给，另补 primaryContainer/onPrimaryContainer）。
 *
 * 本次自算的数值（脚本见交接报告）：
 *  - 暗色：borderSubtle/surface 1.73 · ΔL*(surface,background) 8.70 · onSurface/surface 13.89 ·
 *    onSurfaceVariant/surface 8.64 · onPrimary/primary 8.49。
 *  - 亮色：borderSubtle/surface 1.60 · ΔL*(surface,background) 5.42 · onSurface/surface 13.76 ·
 *    onSurfaceVariant/surface 6.62 · onPrimary/primary 5.81。
 */

/** 樱夜：身份底色，夜校灯影下的樱粉。也是 palette 的兼容回落目标。 */
private val SakuraDark = ThemePalette(
    primary = Color(0xFFF2A2B8),
    onPrimary = Color(0xFF3A0D1B),
    secondary = Color(0xFFE7BCD0),
    onSecondary = Color(0xFF2C1018),
    background = Color(0xFF0D0A10),
    onBackground = Color(0xFFF3E9EE),
    surface = Color(0xFF241C2A),
    onSurface = Color(0xFFF3E9EE),
    surfaceContainerLow = Color(0xFF1A1520),
    surfaceContainer = Color(0xFF2C2333),
    surfaceContainerHigh = Color(0xFF3E3344),
    onSurfaceVariant = Color(0xFFCBB6C2),
    outline = Color(0xFF5E4F65),
    borderSubtle = Color(0xFF4E4155),
    divider = Color(0xFF2F2536),
    error = Color(0xFFFF8F9C),
    onError = Color(0xFF3B060D),
    errorContainer = Color(0xFF4C1420),
    onErrorContainer = Color(0xFFFFD9DE),
    scrim = Color(0xFF000000),
    accentSoft = Color(0xFF3A2331),
    glow = Color(0xFFF7C3D2),
    primaryContainer = Color(0xFF3D2233),
    onPrimaryContainer = Color(0xFFF9CBD9)
)

/** 白天校园：日光把樱粉冲淡成奶白纸面，深玫粉承担行动色。 */
private val SakuraLight = ThemePalette(
    primary = Color(0xFFB03A5F),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF8C4A63),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8FA),
    onBackground = Color(0xFF2A1620),
    surface = Color(0xFFF6E6EE),
    onSurface = Color(0xFF331521),
    surfaceContainerLow = Color(0xFFFDF2F6),
    surfaceContainer = Color(0xFFF0DCE6),
    surfaceContainerHigh = Color(0xFFE4C7D6),
    onSurfaceVariant = Color(0xFF6B4657),
    outline = Color(0xFF9E7286),
    borderSubtle = Color(0xFFD2B2C2),
    divider = Color(0xFFE2C9D6),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBD9DC),
    onErrorContainer = Color(0xFF5C1108),
    scrim = Color(0xFF000000),
    accentSoft = Color(0xFFFBE4EC),
    glow = Color(0xFFC8668A),
    primaryContainer = Color(0xFFFCE3ED),
    onPrimaryContainer = Color(0xFF6B1636)
)

/** 设置页的明暗三档，顺序与 [ThemeModeOverride] 的语义一一对应。 */
private val sakuraModes = listOf(
    ThemeModeOverride.FOLLOW_SYSTEM, ThemeModeOverride.LIGHT, ThemeModeOverride.DARK
)
/** 三档的图标与无障碍说明：档位名不再占横向文字位（原「跟随系统/浅色/深色」chip 行整行删除）。 */
private val sakuraModeGlyphs: List<Pair<ImageVector, String>> = listOf(
    Icons.Default.Brightness6 to "明暗跟随系统",
    Icons.Default.LightMode to "明暗固定浅色",
    Icons.Default.DarkMode to "明暗固定深色"
)

/**
 * 樱夜校园：极简顶栏 + 卡片堆叠与大留白，行间用三枚花瓣圆点代替分割线，
 * 底部四图标导航 52dp（JarvisSize.bottomBarHeight），选中项上浮 2dp 并落一枚指示点。
 *
 * 层次一律由底色明度差承担：卡片 surface（不画描边）、内嵌块 surfaceContainer、
 * 输入框 surfaceContainerHigh；只有分隔线用 JarvisStroke.hairline。
 */
internal class SakuraNightTheme : JarvisThemeModule {
    override val id = "sakura_night"
    override val label = "樱夜校园"
    override val story = "暮色来信"
    override val description = "樱粉 · 夜校灯影"
    override val mode = ThemeMode.DARK
    override val artwork = ThemeArtwork(
        banner = R.drawable.theme_sakura_night_banner,
        avatar = R.drawable.theme_sakura_night_avatar,
        texture = R.drawable.theme_sakura_night_texture
    )
    override val chrome = ChromeStyle.BOTTOM_BAR

    override val palette: ThemePalette = SakuraDark

    override val darkPalette: ThemePalette = SakuraDark

    override val lightPalette: ThemePalette = SakuraLight

    override fun floatingPalette() = FloatingPaletteSpec(
        panel = palette.surface, title = palette.onSurface, body = palette.onSurfaceVariant,
        accent = palette.primary, muted = palette.outline, bubbleRing = palette.borderSubtle
    )

    private val defaultEfforts = listOf("off", "minimal", "low", "medium", "high", "max")

    @Composable
    override fun ChatScreen(ctx: ChatUiContext) {
        val state = ctx.state
        val cb = ctx.callbacks
        val efforts = state.reasoningEfforts.ifEmpty { defaultEfforts }
        val listState = rememberLazyListState()
        LaunchedEffect(state.conversationId, state.messages.size) {
            if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
        }
        Column(Modifier.fillMaxSize()) {
            SakuraHeader("对话", subtitle = "在消息下方编辑、撤销或创建分支",
                actions = {
                    val index = efforts.indexOf(state.reasoningEffort).coerceAtLeast(0)
                    JarvisIconAction(Icons.Default.Psychology,
                        "切换思考深度，当前 " + state.reasoningEffort,
                        onClick = { cb.onReasoningEffortChange(efforts[(index + 1) % efforts.size]) })
                    JarvisIconAction(Icons.Default.Build, "打开工具箱", cb.onOpenTools)
                    JarvisIconAction(Icons.Default.Add, "新建对话", cb.onNewConversation)
                })
            if (state.messages.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().weight(1f)
                        .padding(horizontal = JarvisSpacing.pageHorizontal),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 空态插画卡保留：整块 banner 不透明铺满，不叠任何 alpha 层。
                    Image(
                        painter = painterResource(artwork.banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.CenterEnd,
                        modifier = Modifier.fillMaxWidth().height(172.dp).clip(JarvisShapes.xl)
                    )
                    Spacer(Modifier.height(JarvisSpacing.space4))
                    Text("让想法，\n开始行动。", style = JarvisType.titleScreen,
                        color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(JarvisSpacing.space2))
                    Text("交给 Jarvis，逐步完成。", style = JarvisType.bodyMeta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = JarvisSpacing.pageHorizontal),
                    verticalArrangement = Arrangement.spacedBy(JarvisSpacing.cardGap),
                    contentPadding = PaddingValues(vertical = JarvisSpacing.space3)) {
                    itemsIndexed(state.messages, key = { _, message -> message.id }) { index, message ->
                        SakuraMessageCard(message, state, cb)
                        if (index != state.messages.lastIndex) SakuraPetalDivider()
                    }
                }
            }
            Column(Modifier.jarvisScreenPadding(),
                verticalArrangement = Arrangement.spacedBy(JarvisSpacing.space3)) {
                if (state.editingLatest) {
                    NoticeLine("正在修改上一条；发送后才会替换原对话轮次。", tone = NoticeTone.SUCCESS)
                }
                if (state.speechNotice.isNotBlank()) NoticeLine(state.speechNotice)
                if (state.voiceListening) NoticeLine("正在听…")
                SakuraComposer(state.draft, state.editingLatest, state.sending, cb)
                if (state.sending) JarvisLoading(if (state.streaming) "正在接收回复…" else "正在请求模型…")
            }
            SakuraBottomBar(ctx.shell)
        }
    }

    @Composable
    override fun TasksScreen(ctx: TasksUiContext) {
        val state = ctx.state
        val cb = ctx.callbacks
        val editing = state.rules.firstOrNull { it.id == state.editingRuleId }
        val kinds = state.ruleDraftKindOptions
        val isPixiv = state.ruleDraftKindKey.contains("PIXIV")
        Column(Modifier.fillMaxSize()) {
            SakuraTextureBand(artwork.texture, 48.dp)
            SakuraHeader("自动化",
                subtitle = "通过对话创建，收到通知后按规则执行一次。",
                actions = {
                    JarvisIconAction(Icons.Default.Refresh, "刷新自动化", cb.onRefresh)
                    JarvisIconAction(Icons.Default.Add, "新建规则", cb.onCreateRuleStart)
                })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = JarvisSpacing.pageHorizontal),
                verticalArrangement = Arrangement.spacedBy(JarvisSpacing.sectionGap)) {
                val access = if (state.notificationAccess) "已授权" else "未授权"
                val a11y = if (state.accessibilityGranted) "已连接" else "未连接"
                SakuraCard("运行前检查", "通知访问：" + access + " · 无障碍：" + a11y) {
                    NoticeLine("规则默认暂停；启用后只匹配目标应用通知，并在完成后核实结果。")
                    if (!state.notificationAccess || !state.accessibilityGranted) {
                        JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                            if (!state.notificationAccess) {
                                JarvisIconAction(Icons.Default.Notifications, "授权通知访问",
                                    onClick = { cb.onRequestNotificationAccess() })
                            }
                            if (!state.accessibilityGranted) {
                                JarvisIconAction(Icons.Default.Accessibility, "开启无障碍服务",
                                    onClick = { cb.onRequestAccessibility() })
                            }
                        }
                    }
                    if (!state.runtimeReady) {
                        NoticeLine("运行时尚未就绪，启用后规则无法执行。", tone = NoticeTone.ERROR)
                    }
                    if (state.notice.isNotBlank()) NoticeLine(state.notice, tone = NoticeTone.ERROR)
                }
                if (state.rules.isEmpty() && !state.creatingRule) {
                    EmptyStatePanel("还没有自动化", Modifier.fillMaxWidth(), icon = Icons.Default.Bolt,
                        subtitle = "在对话里说“当哔哩哔哩上有人回复我就立刻回复”，或点下面的按钮直接新建一条。",
                        action = {
                            JarvisIconAction(Icons.Default.Add, "新建规则",
                                onClick = cb.onCreateRuleStart, tone = JarvisActionTone.ACCENT)
                        })
                }
                // 新建草案表单：画师字段只在 Pixiv 类型时出现，校验失败回填 ruleDraftError。
                if (state.creatingRule) {
                    SakuraCard("新建规则", "先选类型，再写触发要求；创建后默认暂停，回到列表里启用。") {
                        SakuraField(SakuraFieldSpec("规则标题", state.ruleDraftTitle,
                            cb.onCreateRuleTitleChange, placeholder = "例如：B 站回复我"))
                        if (kinds.isNotEmpty()) {
                            Text("规则类型", style = JarvisType.titleItem,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            SakuraChipRow(kinds.map { it.label },
                                kinds.indexOfFirst { it.key == state.ruleDraftKindKey },
                                { index -> cb.onCreateRuleKindChange(kinds[index.coerceIn(0, kinds.lastIndex)].key) })
                        }
                        SakuraField(SakuraFieldSpec(
                            "触发要求（例如：当有人在哔哩哔哩回复我，就立刻回复他）",
                            state.ruleDraftPrompt, cb.onCreateRulePromptChange, 3))
                        if (isPixiv) {
                            SakuraField(SakuraFieldSpec(
                                "Pixiv 画师名称或用户 ID（可留空）",
                                state.ruleDraftArtist, cb.onCreateRuleArtistChange))
                        }
                        if (state.ruleDraftError.isNotBlank()) {
                            NoticeLine(state.ruleDraftError, tone = NoticeTone.ERROR)
                        }
                        JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                            JarvisIconAction(Icons.Default.Check, "创建规则",
                                onClick = { cb.onCreateRuleSubmit() },
                                enabled = state.ruleDraftTitle.isNotBlank(),
                                tone = JarvisActionTone.ACCENT)
                            JarvisIconAction(Icons.Default.Close, "取消新建", cb.onCreateRuleCancel)
                        }
                    }
                }
                state.rules.forEach { rule ->
                    SakuraCard(rule.title, rule.kindLabel) {
                        SettingSwitchRow(
                            title = if (rule.enabled) "已启用 · 等待目标应用通知" else "已暂停 · 不会自动操作",
                            checked = rule.enabled,
                            onCheckedChange = { enabled -> cb.onToggleRule(rule.id, enabled) })
                        rule.readiness?.let { NoticeLine("启用前：" + it, tone = NoticeTone.ERROR) }
                        if (rule.lastResult.isNotBlank()) NoticeLine("最近结果：" + rule.lastResult)
                        SakuraPetalDivider()
                        JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                            JarvisIconAction(Icons.Default.Settings, "配置这条规则",
                                onClick = { cb.onConfigureRule(rule.id) })
                            JarvisIconAction(Icons.Default.Delete, "删除这条规则",
                                onClick = { cb.onDeleteRule(rule.id) }, tone = JarvisActionTone.DANGER)
                        }
                    }
                }
                if (editing != null) {
                    SakuraCard("配置自动化", editing.title) {
                        NoticeLine("原始要求：" + editing.sourcePrompt)
                        if (editing.kindKey == "PIXIV_NEW_WORK") {
                            SakuraField(SakuraFieldSpec(
                                "Pixiv 画师名称或用户 ID", state.artistDraft, cb.onArtistDraftChange))
                        }
                        if (editing.kindKey == "BILIBILI_REPLY") {
                            SakuraField(SakuraFieldSpec(
                                "回复风格要求", state.replyDraft, cb.onReplyDraftChange, 2))
                        }
                        NoticeLine("启用前还需要目标应用通知访问和 Jarvis 无障碍服务。")
                        JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                            JarvisIconAction(Icons.Default.Check, "保存配置", tone = JarvisActionTone.ACCENT,
                                onClick = { cb.onSaveRuleConfig(editing.id, state.artistDraft, state.replyDraft) })
                            JarvisIconAction(Icons.Default.Close, "取消配置", { cb.onCancelRuleConfig() })
                        }
                    }
                }
                if (state.recentEvents.isNotEmpty()) {
                    SectionHeader("最近执行")
                    state.recentEvents.take(6).forEach { event ->
                        NoticeLine(
                            text = (if (event.ok) "✓ " else "! ") + event.title + " · " + event.message,
                            tone = if (event.ok) NoticeTone.SUCCESS else NoticeTone.ERROR)
                    }
                }
                Spacer(Modifier.height(JarvisSpacing.space4))
            }
            SakuraBottomBar(ctx.shell)
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
        var openFile by remember { mutableStateOf<String?>(null) }
        val current = state.workspaces.firstOrNull { it.name == state.selectedWorkspace }
        val dirLabel = current?.dirLabel.orEmpty().ifBlank { "未绑定" }
        val hasDir = current?.hasDirectory == true
        val conversations = state.conversations.filter { it.workspace == state.selectedWorkspace }
        Column(Modifier.fillMaxSize()) {
            SakuraHeader("工作区",
                subtitle = "为每个工作区选择共同设定，或单独编辑人格与世界书。")
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = JarvisSpacing.pageHorizontal),
                verticalArrangement = Arrangement.spacedBy(JarvisSpacing.sectionGap)) {
                SakuraCard("归档",
                    "已归档工作区 " + state.archivedWorkspaces.size + " 个 · 已归档对话 " +
                        state.archivedConversationIds.size + " 条") {
                    SettingSwitchRow("显示已归档", state.showArchived, cb.onToggleShowArchived,
                        subtitle = if (state.showArchived) {
                            "列表里同时显示归档项，随时可以还原。"
                        } else {
                            "列表只显示未归档项；归档不删数据，随时可以还原。"
                        })
                    if (state.archivedWorkspaces.isNotEmpty()) {
                        SakuraPetalDivider()
                        state.archivedWorkspaces.forEach { profile ->
                            ListRow(title = profile.name,
                                subtitle = "已归档 · 档案与对话都还在",
                                trailing = ({
                                    JarvisIconAction(Icons.Default.Unarchive, "还原 " + profile.name,
                                        onClick = { cb.onUnarchive(profile.name) })
                                }))
                        }
                    }
                }
                state.workspaces.forEach { profile ->
                    val count = state.conversationCountByWorkspace[profile.name] ?: 0
                    val persona = if (profile.useGlobalPersona) "共同人格与世界书" else "自定义人格与世界书"
                    SakuraCard(profile.name, persona + " · " + count + " 个会话") {
                        if (profile.name == state.selectedWorkspace || profile.archived) {
                            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                                if (profile.name == state.selectedWorkspace) {
                                    JarvisGlyphLabel(Icons.Default.CheckCircle, "当前",
                                        tone = JarvisActionTone.ACCENT)
                                }
                                if (profile.archived) {
                                    JarvisGlyphLabel(Icons.Default.Archive, "已归档")
                                }
                            }
                        }
                        JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight)) {
                            JarvisIconAction(Icons.Default.Login, "进入 " + profile.name,
                                onClick = { cb.onSelect(profile.name) }, tone = JarvisActionTone.ACCENT)
                            JarvisIconAction(Icons.Default.Edit, "编辑 " + profile.name,
                                onClick = { editing = profile })
                            if (profile.archived) {
                                JarvisIconAction(Icons.Default.Unarchive, "还原 " + profile.name,
                                    onClick = { cb.onUnarchive(profile.name) })
                            } else if (profile.name != WorkspaceProfile.DEFAULT_NAME) {
                                JarvisIconAction(Icons.Default.Archive, "归档 " + profile.name,
                                    onClick = { cb.onArchive(profile.name) })
                            }
                            JarvisIconAction(Icons.Default.Delete, "删除 " + profile.name,
                                onClick = { cb.onDelete(profile.name) }, tone = JarvisActionTone.DANGER)
                        }
                    }
                }
                // 目录绑定 + 只读文件清单：排在当前工作区下面，避免每个工作区各挂一份长清单。
                SakuraCard("目录文件", "当前目录：" + dirLabel) {
                    JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight)) {
                        JarvisIconAction(
                            icon = if (hasDir) Icons.Default.FolderOpen else Icons.Default.CreateNewFolder,
                            contentDescription = if (hasDir) "更换目录" else "绑定目录",
                            onClick = { cb.onPickDirectory(state.selectedWorkspace) })
                        if (hasDir) {
                            JarvisIconAction(Icons.Default.LinkOff, "解绑目录",
                                onClick = { cb.onClearDirectory(state.selectedWorkspace) })
                        }
                    }
                    if (hasDir && !state.dirReady) {
                        NoticeLine("目录读取权限已失效，请重新绑定目录。", tone = NoticeTone.ERROR)
                    }
                    if (state.filesLoading) JarvisLoading("正在读取目录")
                    if (state.filesNotice.isNotBlank()) NoticeLine(state.filesNotice)
                    if (state.files.isEmpty() && !state.filesLoading) {
                        NoticeLine("这个目录还没有可显示的条目；清单只读，最多 200 条。")
                    }
                    state.files.forEach { file ->
                        val expanded = openFile == file.name
                        SakuraFileRow(file, expanded) {
                            openFile = if (expanded) null else file.name
                        }
                    }
                }
                SakuraCard("对话", "「" + state.selectedWorkspace + "」下 " + conversations.size +
                    " 条；改名、移动、归档、删除都在这里。") {
                    if (conversations.isEmpty()) NoticeLine("这个工作区还没有对话。")
                    conversations.take(20).forEach { session ->
                        val archived = session.isArchived
                        ListRow(title = session.title,
                            subtitle = session.messages.size.toString() + " 条消息" +
                                (if (archived) " · 已归档" else ""),
                            trailing = if (archived) ({
                                JarvisGlyphLabel(Icons.Default.Archive, "已归档")
                            }) else null)
                        JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight)) {
                            JarvisIconAction(Icons.Default.DriveFileRenameOutline, "给这条对话改名",
                                onClick = {
                                    renamingId = if (renamingId == session.id) null else session.id
                                    renameDraft = session.title
                                })
                            JarvisIconAction(Icons.Default.DriveFileMove, "把这条对话移动到别的工作区",
                                onClick = { movingId = if (movingId == session.id) null else session.id })
                            if (archived) {
                                JarvisIconAction(Icons.Default.Unarchive, "还原这条对话",
                                    onClick = { cb.onUnarchiveConversation(session.id) })
                            } else {
                                JarvisIconAction(Icons.Default.Archive, "归档这条对话",
                                    onClick = { cb.onArchiveConversation(session.id) })
                            }
                            JarvisIconAction(Icons.Default.Delete, "删除这条对话",
                                onClick = { cb.onDeleteConversation(session.id) },
                                tone = JarvisActionTone.DANGER)
                        }
                        if (renamingId == session.id) {
                            SakuraField(SakuraFieldSpec("新标题", renameDraft, { renameDraft = it }))
                            JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight)) {
                                JarvisIconAction(Icons.Default.Check, "保存标题",
                                    onClick = {
                                        cb.onRenameConversation(session.id, renameDraft.trim())
                                        renamingId = null
                                    },
                                    enabled = renameDraft.isNotBlank(),
                                    tone = JarvisActionTone.ACCENT)
                                JarvisIconAction(Icons.Default.Close, "取消改名",
                                    onClick = { renamingId = null })
                            }
                        }
                        if (movingId == session.id) {
                            val targets = state.workspaces.map { it.name }
                                .filter { it != session.workspace }
                            if (targets.isEmpty()) {
                                NoticeLine("没有别的工作区可以移动。")
                            } else {
                                Text("移动到", style = JarvisType.titleItem,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                SakuraChipRow(targets, -1, { index ->
                                    cb.onMoveConversation(session.id, targets[index])
                                    movingId = null
                                })
                            }
                        }
                        JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    }
                }
                SakuraWorkspaceForm(artwork, editing, state, cb) { editing = null }
                Spacer(Modifier.height(JarvisSpacing.space4))
            }
            SakuraBottomBar(ctx.shell)
        }
    }

    @Composable
    override fun SettingsScreen(ctx: SettingsUiContext) {
        val state = ctx.state
        val cb = ctx.callbacks
        var pluginDraft by remember { mutableStateOf("") }
        val efforts = state.reasoningEfforts.ifEmpty { defaultEfforts }
        Column(Modifier.fillMaxSize()) {
            SakuraHeader("设置",
                subtitle = state.versionName.ifBlank { "模型、权限与设备连接" })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = JarvisSpacing.pageHorizontal),
                verticalArrangement = Arrangement.spacedBy(JarvisSpacing.sectionGap)) {
                SectionHeader("显示")
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    val current = state.themeOptions.firstOrNull { it.id == state.themeId }
                    ListRow(title = "主题",
                        subtitle = current?.description ?: "图片与控件同步切换，每套主题使用统一色系",
                        trailing = { StatusBadge(current?.label ?: state.theme.label, BadgeTone.ACTIVE) })
                    SakuraChipRow(state.themeOptions.map { it.label },
                        state.themeOptions.indexOfFirst { it.id == state.themeId },
                        { cb.onThemeChangeById(state.themeOptions[it].id) })
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    // 明暗偏好：三档直接给三枚图标（跟随/浅/深），当前档用主色，不再占一行文字 chip。
                    Row(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight),
                        verticalAlignment = Alignment.CenterVertically) {
                        JarvisGlyphLabel(Icons.Default.Brightness6, "明暗", modifier = Modifier.weight(1f))
                        JarvisIconRow(horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
                            sakuraModeGlyphs.forEachIndexed { index, (icon, description) ->
                                JarvisIconAction(icon, description,
                                    onClick = { cb.onModeOverrideChange(sakuraModes[index]) },
                                    tone = if (sakuraModes[index] == state.modeOverride) {
                                        JarvisActionTone.ACCENT
                                    } else {
                                        JarvisActionTone.NEUTRAL
                                    })
                            }
                        }
                    }
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    ListRow(title = "版本",
                        subtitle = state.versionName.ifBlank { state.bootstrapReport.runtimeVersion },
                        trailing = {
                            JarvisGlyphLabel(runtimeIcon(state.bootstrapReport.state),
                                runtimeLabel(state.bootstrapReport.state),
                                tone = runtimeActionTone(state.bootstrapReport.state))
                        })
                }
                SectionHeader("行为")
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    SettingSwitchRow("限制模式", state.restrictionMode, cb.onRestrictionChange,
                        subtitle = "默认开启，控制高风险内容和动作")
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    SettingSwitchRow("启用命令通道", state.commandChannelEnabled, cb.onCommandChange,
                        subtitle = "指定会话可发送 /claw 命令，其余消息走普通人格")
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    SettingSwitchRow("深度思考", state.deepThinkingEnabled, cb.onDeepThinkingChange,
                        subtitle = "开启后由下方档位控制")
                    SakuraChipRow(efforts, efforts.indexOf(state.reasoningEffort).coerceAtLeast(0),
                        { cb.onReasoningEffortChange(efforts[it]) })
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    SakuraField(SakuraFieldSpec("命令前缀", state.commandPrefix, cb.onPrefixChange))
                    SakuraField(SakuraFieldSpec("模型 Base URL", state.modelBaseUrl, cb.onModelBaseUrlChange))
                    SakuraField(SakuraFieldSpec("模型名", state.modelName, cb.onModelNameChange))
                    SakuraField(SakuraFieldSpec("模型 API Key", state.modelApiKey, cb.onModelApiKeyChange, secret = true))
                    SakuraField(SakuraFieldSpec("模型回退链（每行一个，可选）", state.modelChain,
                        cb.onModelChainChange, 3))
                    SakuraField(SakuraFieldSpec("深度思考模型（可选）", state.deepThinkingModel,
                        cb.onDeepThinkingModelChange))
                    SakuraField(SakuraFieldSpec("识图 Base URL", state.visionBaseUrl, cb.onVisionBaseUrlChange))
                    SakuraField(SakuraFieldSpec("识图模型名", state.visionModel, cb.onVisionModelChange))
                    SakuraField(SakuraFieldSpec("识图 API Key", state.visionApiKey, cb.onVisionApiKeyChange, secret = true))
                    SakuraField(SakuraFieldSpec("全局提示词", state.mentorPrompt, cb.onMentorPromptChange, 3))
                    SakuraField(SakuraFieldSpec("全局世界书", state.worldBook, cb.onWorldBookChange, 3))
                    if (state.importNotice.isNotBlank()) NoticeLine(state.importNotice, tone = NoticeTone.SUCCESS)
                    if (state.exportNotice.isNotBlank()) NoticeLine(state.exportNotice)
                    JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                        JarvisIconAction(Icons.Default.FileDownload, "导入 AI Classroom 数据",
                            onClick = { cb.onImportAiClassroom() })
                        JarvisIconAction(Icons.Default.FileUpload, "导出 AI Classroom 数据",
                            onClick = { cb.onExportAiClassroom() })
                    }
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    if (state.applyNotice.isNotBlank()) {
                        NoticeLine(state.applyNotice,
                            tone = if (state.applyFailed) NoticeTone.ERROR else NoticeTone.SUCCESS)
                    }
                    JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight)) {
                        if (state.saving) JarvisLoading("应用中…")
                        JarvisIconAction(Icons.Default.Save, "保存并生效",
                            onClick = { cb.onApply() }, tone = JarvisActionTone.ACCENT)
                        JarvisIconAction(Icons.Default.CheckCircle, "保存并验证主模型",
                            onClick = { cb.onApplyAndVerify() })
                    }
                }
                SectionHeader("安全")
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    state.rootModes.forEach { mode ->
                        val current = mode.key == state.rootModeKey
                        ListRow(title = mode.label,
                            subtitle = if (current) "当前：" + mode.label else "完全模式会在启用前显示三秒警告",
                            trailing = if (current) null else ({
                                JarvisGlyphLabel(Icons.Default.ChevronRight, "切换")
                            }),
                            onClick = { cb.onRootModeRequest(mode) })
                        JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    }
                    val report = state.bootstrapReport
                    ListRow(title = "root / Bridge Token",
                        subtitle = (if (report.rootAvailable) "已检测到" else "未检测到") + " · " +
                            (if (report.bridgeTokenReady) "已生成" else "缺失"))
                    ListRow(title = "插件目录", subtitle = report.pluginDirectory)
                    if (state.runtimeMessage.isNotBlank()) NoticeLine(state.runtimeMessage)
                    JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight)) {
                        JarvisIconAction(Icons.Default.PlayArrow, "启动 Gateway",
                            onClick = { cb.onStartRuntime() }, tone = JarvisActionTone.ACCENT)
                        JarvisIconAction(Icons.Default.Stop, "停止 Gateway",
                            onClick = { cb.onStopRuntime() })
                        JarvisIconAction(Icons.Default.Refresh, "重新检查运行时",
                            onClick = { cb.onRefreshRuntime() })
                    }
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    SettingSwitchRow("启用悬浮球", state.floatingEnabled, cb.onFloatingEnabledChange,
                        subtitle = "语音任务识别后会直接交给 Jarvis 执行")
                    val overlay = if (state.overlayGranted) "已授权" else "未授权"
                    val a11y = if (state.accessibilityGranted) "已启用" else "未启用"
                    NoticeLine("悬浮窗：" + overlay + " · 无障碍：" + a11y)
                    JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight)) {
                        JarvisIconAction(Icons.Default.PictureInPicture, "悬浮窗权限",
                            onClick = { cb.onOverlayPermissionRequest() })
                        JarvisIconAction(Icons.Default.Accessibility, "无障碍设置",
                            onClick = { cb.onAccessibilitySettingsOpen() })
                    }
                    SettingSwitchRow("打开 Jarvis 时自动连接", state.accessibilityAutoConnect,
                        cb.onAccessibilityAutoConnectChange, subtitle = "需要 root 完全模式；关闭不会撤销已有授权")
                }
                SectionHeader("浮窗与手势")
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    ListRow(title = "头像边长", subtitle = state.avatarSizeDp.toString() + " dp",
                        trailing = { JarvisGlyphLabel(Icons.Default.CheckCircle, "当前",
                            tone = JarvisActionTone.ACCENT) })
                    SakuraChipRow(state.avatarSizeChoices.map { it.toString() + "dp" },
                        state.avatarSizeChoices.indexOf(state.avatarSizeDp),
                        { cb.onAvatarSizeChange(state.avatarSizeChoices[it]) })
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    ListRow(title = "悬浮球边长", subtitle = state.floatingSizeDp.toString() + " dp")
                    SakuraChipRow(state.avatarSizeChoices.map { it.toString() + "dp" },
                        state.avatarSizeChoices.indexOf(state.floatingSizeDp),
                        { cb.onFloatingSizeChange(state.avatarSizeChoices[it]) })
                    if (state.floatingShapes.isNotEmpty()) {
                        JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                        ListRow(title = "悬浮球形状",
                            subtitle = state.floatingShapes.firstOrNull { it.key == state.floatingShapeKey }?.label
                                ?: state.floatingShapeKey)
                        SakuraChipRow(state.floatingShapes.map { it.label },
                            state.floatingShapes.indexOfFirst { it.key == state.floatingShapeKey },
                            { cb.onFloatingShapeChange(state.floatingShapes[it].key) })
                    }
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    ListRow(title = "双击/三击暂时隐藏", subtitle = state.avatarHideSeconds.toString() + " 秒")
                    SakuraChipRow(state.hideSecondChoices.map { it.toString() + "秒" },
                        state.hideSecondChoices.indexOf(state.avatarHideSeconds),
                        { cb.onAvatarHideSecondsChange(state.hideSecondChoices[it]) })
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    if (state.gestureOptions.isNotEmpty()) {
                        ListRow(title = "双击操作",
                            subtitle = state.gestureOptions.firstOrNull { it.key == state.doubleGestureKey }?.label
                                ?: state.doubleGestureKey)
                        SakuraChipRow(state.gestureOptions.map { it.label },
                            state.gestureOptions.indexOfFirst { it.key == state.doubleGestureKey },
                            { cb.onDoubleGestureChange(state.gestureOptions[it].key) })
                    }
                    SakuraField(SakuraFieldSpec("双击时执行的任务", state.doubleTask, cb.onDoubleTaskChange))
                    state.savedAutomations.take(3).forEach { rule ->
                        ListRow(title = rule.title, subtitle = "双击选用 · " + rule.kindLabel) {
                            cb.onSelectAutomationForGesture(state.doubleGestureKey, rule.id)
                        }
                    }
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    if (state.gestureOptions.isNotEmpty()) {
                        ListRow(title = "三击操作",
                            subtitle = state.gestureOptions.firstOrNull { it.key == state.tripleGestureKey }?.label
                                ?: state.tripleGestureKey)
                        SakuraChipRow(state.gestureOptions.map { it.label },
                            state.gestureOptions.indexOfFirst { it.key == state.tripleGestureKey },
                            { cb.onTripleGestureChange(state.gestureOptions[it].key) })
                    }
                    SakuraField(SakuraFieldSpec("三击时执行的任务", state.tripleTask, cb.onTripleTaskChange))
                    state.savedAutomations.take(3).forEach { rule ->
                        ListRow(title = rule.title, subtitle = "三击选用 · " + rule.kindLabel) {
                            cb.onSelectAutomationForGesture(state.tripleGestureKey, rule.id)
                        }
                    }
                }
                SectionHeader("连接")
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    SettingSwitchRow("启用远程网页控制台", state.serverRunning,
                        onCheckedChange = { enabled -> if (enabled) cb.onStartServer() else cb.onStopServer() },
                        subtitle = "默认关闭；只允许已配对的浏览器提交任务")
                    if (state.pairingCode.isNotBlank()) {
                        NoticeLine("一次性配对码：" + state.pairingCode, tone = NoticeTone.SUCCESS)
                        JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight)) {
                            JarvisIconAction(Icons.Default.ContentCopy, "复制配对码",
                                onClick = { cb.onCopyPairingCode() })
                            JarvisIconAction(Icons.Default.LinkOff, "撤销配对",
                                onClick = { cb.onRevokePairing() }, tone = JarvisActionTone.DANGER)
                        }
                    }
                    JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight)) {
                        JarvisIconAction(Icons.Default.QrCode2, "生成配对码",
                            onClick = { cb.onNewPairingCode() }, tone = JarvisActionTone.ACCENT)
                        JarvisIconAction(Icons.Default.Refresh, "刷新地址",
                            onClick = { cb.onRefreshRemoteUrls() })
                    }
                    state.remoteUrls.forEach { NoticeLine(it) }
                    SettingSwitchRow("允许远程设备调用本机能力", state.remoteDeviceControl,
                        cb.onRemoteDeviceControlChange)
                }
                SectionHeader("维护")
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    if (state.checkingTools) {
                        JarvisLoading("正在检查工具路径")
                    } else {
                        JarvisIconAction(Icons.Default.Refresh, "重新检查工具路径",
                            onClick = { cb.onRefreshTools() })
                    }
                    NoticeLine("CMake：" + (if (state.cmakeReady) "可用" else "缺失") + " · FFmpeg：" +
                        (if (state.ffmpegReady) "可用" else "缺失"))
                    state.runtimeTools.forEach { tool ->
                        val detail = state.tools.firstOrNull { it.id == tool.name || it.label == tool.name }
                        val source = detail?.sourceLabel.orEmpty()
                        val subtitle = if (tool.installed) tool.version.ifBlank { "已安装" } else "未安装"
                        ListRow(
                            title = tool.name,
                            subtitle = if (source.isBlank() || subtitle.contains(source)) {
                                subtitle
                            } else {
                                subtitle + " · " + source
                            },
                            trailing = if (tool.installed) null else ({
                                JarvisIconAction(Icons.Default.Download, "安装 " + tool.name,
                                    onClick = { cb.onInstallTool(detail?.id ?: tool.name) })
                            })
                        )
                    }
                    if (state.toolNotice.isNotBlank()) NoticeLine(state.toolNotice)
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    ListRow(title = "社区插件", subtitle = state.plugins.size.toString() + " 个已安装",
                        trailing = {
                            if (state.pluginBusy) {
                                JarvisLoading("处理中")
                            } else {
                                JarvisIconAction(Icons.Default.Refresh, "刷新插件列表",
                                    onClick = { cb.onRefreshPlugins() })
                            }
                        })
                    state.plugins.forEach { plugin ->
                        SettingSwitchRow(title = plugin.name, checked = plugin.enabled,
                            subtitle = plugin.version + " · " + plugin.tools.size.toString() + " 个工具",
                            onCheckedChange = { enabled -> cb.onTogglePlugin(plugin.id, enabled) })
                        JarvisIconAction(Icons.Default.Delete, "卸载 " + plugin.name,
                            onClick = { cb.onUninstallPlugin(plugin.id) }, tone = JarvisActionTone.DANGER)
                    }
                    if (state.pluginNotice.isNotBlank()) NoticeLine(state.pluginNotice)
                    SakuraField(SakuraFieldSpec("粘贴插件草稿（JSON 或清单文本）", pluginDraft,
                        { pluginDraft = it }, 3))
                    JarvisIconAction(Icons.Default.Extension, "安装插件",
                        onClick = {
                            cb.onInstallPlugin(pluginDraft)
                            pluginDraft = ""
                        },
                        enabled = pluginDraft.isNotBlank(),
                        tone = JarvisActionTone.ACCENT)
                    JarvisDivider(Modifier.padding(vertical = JarvisSpacing.space2))
                    ListRow(title = "操作记忆",
                        subtitle = state.memoryStatus.ifBlank { "首次成功后学习，后续优先复用" },
                        trailing = {
                            JarvisIconAction(Icons.Default.Refresh, "刷新操作记忆",
                                onClick = { cb.onRefreshMemory() })
                        })
                    state.memoryEntries.forEach { entry ->
                        ListRow(title = entry.title,
                            subtitle = (if (entry.ready) "可复用" else "已失效") + " · 成功 " +
                                entry.successes.toString() + " / 失败 " + entry.failures.toString(),
                            trailing = {
                                JarvisIconAction(Icons.Default.Delete, "删除这条操作记忆",
                                    onClick = { cb.onDeleteMemoryEntry(entry.id) },
                                    tone = JarvisActionTone.DANGER)
                            })
                    }
                }
                Spacer(Modifier.height(JarvisSpacing.space4))
            }
            SakuraBottomBar(ctx.shell)
        }
    }
}

/** 页头：标题行只放操作，副标题单独占一行，不与图标按钮挤在同一行。 */
@Composable
private fun SakuraHeader(
    title: String,
    subtitle: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = JarvisSpacing.pageHorizontal, vertical = JarvisSpacing.space3)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = JarvisType.titleScreen, color = scheme.onSurface,
                modifier = Modifier.weight(1f))
            if (actions != null) {
                Spacer(Modifier.width(JarvisSpacing.space2))
                JarvisIconRow(actions = actions)
            }
        }
        if (subtitle != null) {
            Spacer(Modifier.height(JarvisSpacing.space2))
            Text(subtitle, style = JarvisType.bodyMeta, color = scheme.onSurfaceVariant)
        }
    }
}

/** chip 行：视觉高 40dp（JarvisSize.chipHeight），M3 交互尺寸层保证触达仍 ≥ 44dp；选中只靠实心底色，无描边。 */
@Composable
private fun SakuraChipRow(items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
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
        Modifier.fillMaxWidth().padding(vertical = JarvisSpacing.space2).horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2),
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

private data class SakuraFieldSpec(
    val label: String,
    val value: String,
    val onChange: (String) -> Unit,
    val minLines: Int = 1,
    val placeholder: String = "",
    val secret: Boolean = false
)

/** 花瓣分隔：三枚圆点，代替列表分割线。 */
@Composable
private fun SakuraPetalDivider() {
    Row(Modifier.fillMaxWidth().padding(vertical = JarvisSpacing.space2),
        horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            Box(Modifier.size(if (index == 1) 6.dp else 4.dp).clip(JarvisShapes.pill)
                .background(MaterialTheme.colorScheme.outlineVariant))
        }
    }
}

/** 卡片：surface 底、不画描边（边界由 surface 与 background 的 ΔL* 8.70 / 5.42 承担）。 */
@Composable
private fun SakuraCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
        Text(title, style = JarvisType.titleBlock, color = scheme.onSurface)
        Spacer(Modifier.height(JarvisSpacing.space2))
        Text(subtitle, style = JarvisType.bodyMeta, color = scheme.onSurfaceVariant)
        JarvisCardHeaderGap()
        content()
    }
}

@Composable
private fun SakuraMessageCard(
    message: ChatMessage,
    state: ChatUiContext.ChatState,
    cb: ChatUiContext.ChatCallbacks
) {
    val scheme = MaterialTheme.colorScheme
    val isUser = message.role == ChatMessageRole.USER
    val lastUserId = state.messages.lastOrNull { it.role == ChatMessageRole.USER }?.id
    JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(JarvisShapes.pill)
                .background(if (isUser) scheme.secondary else scheme.primary))
            Spacer(Modifier.width(JarvisSpacing.space3))
            Text(if (isUser) "你" else "Jarvis", style = JarvisType.titleItem,
                color = if (isUser) scheme.secondary else scheme.primary)
        }
        JarvisCardHeaderGap()
        Text(message.content, style = JarvisType.bodyBase, color = scheme.onSurface)
        JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight)) {
            JarvisIconAction(Icons.Default.CallSplit, "从这条消息创建分支",
                onClick = { cb.onBranch(message.id) }, enabled = !state.sending)
            if (isUser && message.id == lastUserId) {
                JarvisIconAction(
                    icon = if (state.editingLatest) Icons.Default.Close else Icons.Default.Edit,
                    contentDescription = if (state.editingLatest) "取消编辑" else "编辑这条消息",
                    onClick = { if (state.editingLatest) cb.onCancelEdit() else cb.onEditLatest() },
                    enabled = !state.sending && (state.editingLatest || state.canEditLatest))
            }
            if (message.id == state.messages.lastOrNull()?.id) {
                JarvisIconAction(Icons.Default.Undo, "撤销上一条",
                    onClick = cb.onUndoLatest, enabled = state.canUndoLatest)
            }
        }
    }
}

/** 输入区：输入框用 surfaceContainerHigh 实底（不画边框），发送键 44dp，语音键 44dp 触达。 */
@Composable
private fun SakuraComposer(
    draft: String,
    editing: Boolean,
    sending: Boolean,
    cb: ChatUiContext.ChatCallbacks
) {
    val scheme = MaterialTheme.colorScheme
    val sendEnabled = !sending && draft.isNotBlank()
    JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.insetCardPadding) {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                Modifier.weight(1f)
                    .heightIn(min = JarvisSize.rowMinHeight)
                    .clip(JarvisShapes.sm)
                    .background(scheme.surfaceContainerHigh)
                    .padding(horizontal = JarvisSpacing.space4, vertical = JarvisSpacing.space3)
            ) {
                BasicTextField(
                    value = draft, onValueChange = cb.onDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = JarvisType.bodyBase.copy(color = scheme.onSurface),
                    cursorBrush = SolidColor(scheme.primary), maxLines = 4,
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) {
                                Text(
                                    text = if (editing) "修改上一条消息后重新发送…" else "给 Jarvis 发消息，或描述一个任务…",
                                    style = JarvisType.bodyBase, color = scheme.onSurfaceVariant)
                            }
                            inner()
                        }
                    }
                )
            }
            Spacer(Modifier.width(JarvisSpacing.space2))
            JarvisIconAction(Icons.Default.Mic, "语音转文字", cb.onVoiceInput)
            Box(
                Modifier.size(JarvisSize.sendButton).clip(JarvisShapes.pill)
                    .background(if (sendEnabled) scheme.primary else scheme.surfaceContainerHigh)
                    .clickable(enabled = sendEnabled, role = Role.Button) { cb.onSend(draft.trim()) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, "发送", modifier = Modifier.size(JarvisSize.toolbarIcon),
                    tint = if (sendEnabled) scheme.onPrimary else scheme.onSurfaceVariant)
            }
        }
    }
}

/** 自绘底栏：高 JarvisSize.bottomBarHeight(52dp)，选中项上浮 2dp 并落一枚指示点。 */
@Composable
private fun SakuraBottomBar(shell: ShellUiContext) {
    val scheme = MaterialTheme.colorScheme
    val tabs = if (shell.tabs.isEmpty()) ShellUiContext.DefaultTabs else shell.tabs
    Column(Modifier.fillMaxWidth().background(scheme.surface)) {
        JarvisDivider()
        Row(Modifier.fillMaxWidth().height(JarvisSize.bottomBarHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, tab ->
                val active = !tab.opensFloating && index == shell.tabIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = JarvisSize.minTouch)
                        .offset(y = if (active) -2.dp else 0.dp)
                        .clip(JarvisShapes.md)
                        .background(if (active) scheme.surfaceContainer else scheme.surface)
                        .clickable(role = Role.Tab) {
                            if (tab.opensFloating) shell.onOpenFloating() else shell.onNavigate(index)
                        }
                        .semantics {
                            role = Role.Tab
                            selected = active
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(tab.icon ?: sakuraTabIcon(tab.key), tab.label,
                        modifier = Modifier.size(JarvisSize.toolbarIcon),
                        tint = if (active) scheme.primary else scheme.onSurfaceVariant)
                    Spacer(Modifier.height(JarvisSpacing.space2))
                    Box(Modifier.size(4.dp).clip(JarvisShapes.pill)
                        .background(if (active) scheme.primary else Color.Transparent))
                }
            }
        }
    }
}

private fun sakuraTabIcon(key: String): ImageVector = when (key) {
    "chat" -> Icons.Default.Chat
    "tasks" -> Icons.Default.Bolt
    "workspace" -> Icons.Default.Folder
    "settings" -> Icons.Default.Settings
    else -> Icons.Default.OpenInNew
}

/**
 * 输入框：无边框，靠 surfaceContainerHigh 与 surface 的明度差成块；标签用 titleItem(SemiBold)，
 * 正文 bodyBase 单字号，删掉粗边框后靠字重与底色区分，不靠线条。
 */
@Composable
private fun SakuraField(spec: SakuraFieldSpec) {
    val scheme = MaterialTheme.colorScheme
    if (spec.secret) {
        JarvisSecretField(
            label = spec.label, value = spec.value, onValueChange = spec.onChange,
            labelStyle = JarvisType.titleItem, valueStyle = JarvisType.bodyBase,
            labelColor = scheme.onSurfaceVariant, valueColor = scheme.onSurface,
            containerColor = scheme.surfaceContainerHigh, accentColor = scheme.primary)
        return
    }
    Column(Modifier.fillMaxWidth().padding(vertical = JarvisSpacing.space2),
        verticalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
        Text(spec.label, style = JarvisType.titleItem, color = scheme.onSurfaceVariant)
        BasicTextField(
            value = spec.value,
            onValueChange = spec.onChange,
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = JarvisSize.rowMinHeight)
                .clip(JarvisShapes.sm)
                .background(scheme.surfaceContainerHigh)
                .padding(horizontal = JarvisSpacing.space4, vertical = JarvisSpacing.space3),
            textStyle = JarvisType.bodyBase.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.primary),
            minLines = spec.minLines,
            decorationBox = { inner ->
                Box {
                    if (spec.value.isEmpty() && spec.placeholder.isNotBlank()) {
                        Text(spec.placeholder, style = JarvisType.bodyBase,
                            color = scheme.onSurfaceVariant)
                    }
                    inner()
                }
            }
        )
    }
}

/** 目录清单的一行：只读；previewable 时整行可点展开预览文本。 */
@Composable
private fun SakuraFileRow(file: WorkspaceFileView, expanded: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        val kind = if (file.isDirectory) "目录" else sakuraBytes(file.sizeBytes)
        ListRow(
            title = file.name,
            subtitle = kind + if (file.previewable) " · 可预览" else "",
            trailing = if (file.previewable) ({
                JarvisIconAction(
                    icon = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起预览" else "预览文件内容",
                    onClick = onToggle, tone = JarvisActionTone.ACCENT)
            }) else null,
            onClick = if (file.previewable) onToggle else null
        )
        if (expanded && file.previewText.isNotBlank()) {
            JarvisInsetCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = JarvisSpacing.space2),
                contentPadding = JarvisSpacing.insetCardPadding
            ) {
                Text(file.previewText.take(1200), style = JarvisType.codeInline,
                    color = scheme.onSurfaceVariant)
            }
        }
        JarvisDivider()
    }
}

@Composable
private fun SakuraWorkspaceForm(
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
            Image(painterResource(artwork.avatar), null,
                Modifier.size(40.dp).clip(JarvisShapes.pill), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(JarvisSpacing.space3))
            Text(if (initial == null) "新建工作区" else "编辑工作区",
                style = JarvisType.titleBlock, color = MaterialTheme.colorScheme.onSurface)
        }
        JarvisCardHeaderGap()
        SakuraField(SakuraFieldSpec("工作区名称", name, { name = it.take(60) }))
        SettingSwitchRow(
            title = "使用共同设定",
            subtitle = if (useGlobal) "此工作区会跟随设置中的全局人格与世界书。" else "使用下方自定义人格与世界书。",
            checked = useGlobal, onCheckedChange = { useGlobal = it })
        if (!useGlobal) {
            SakuraField(SakuraFieldSpec("自定义人格 / 提示词", persona.ifBlank { state.globalPersona },
                { persona = it }, 3))
            SakuraField(SakuraFieldSpec("自定义世界书", worldBook.ifBlank { state.globalWorldBook },
                { worldBook = it }, 3))
            state.personaSources.take(3).forEach { source ->
                ListRow(title = source.title, subtitle = "从其他对话粘贴 · " + source.workspace) {
                    persona = source.mentorPrompt
                    worldBook = source.worldBook
                }
            }
        }
        Spacer(Modifier.height(JarvisSpacing.rowGap))
        JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
            JarvisIconAction(Icons.Default.Check,
                contentDescription = if (initial == null) "创建并进入" else "保存工作区",
                enabled = clean.isNotEmpty(),
                tone = JarvisActionTone.ACCENT,
                onClick = {
                    cb.onSave(
                        WorkspaceProfile(
                            name = clean, useGlobalPersona = useGlobal,
                            mentorPrompt = if (useGlobal) "" else persona.trim(),
                            worldBook = if (useGlobal) "" else worldBook.trim()),
                        initial?.name)
                })
            if (initial != null) {
                JarvisIconAction(Icons.Default.Close, "取消编辑工作区", onClick = onDismiss)
            }
        }
    }
}

private fun runtimeLabel(state: RuntimeState): String = when (state) {
    RuntimeState.READY -> "就绪"
    RuntimeState.RUNNING -> "运行中"
    RuntimeState.DEGRADED -> "兼容"
    RuntimeState.NOT_CONFIGURED -> "未配置"
    RuntimeState.ERROR -> "出错"
}

/** 运行时状态一改用「图标 + 2~4 字」表达，颜色由 [runtimeActionTone] 承担，不再占用整枚文字徽标。 */
private fun runtimeIcon(state: RuntimeState): ImageVector = when (state) {
    RuntimeState.READY, RuntimeState.RUNNING -> Icons.Default.CheckCircle
    RuntimeState.DEGRADED -> Icons.Default.Warning
    RuntimeState.ERROR -> Icons.Default.Error
    RuntimeState.NOT_CONFIGURED -> Icons.Default.Info
}

private fun runtimeActionTone(state: RuntimeState): JarvisActionTone = when (state) {
    RuntimeState.READY, RuntimeState.RUNNING -> JarvisActionTone.ACCENT
    RuntimeState.DEGRADED, RuntimeState.ERROR -> JarvisActionTone.DANGER
    RuntimeState.NOT_CONFIGURED -> JarvisActionTone.NEUTRAL
}

/** 字节数：-1 表示提供方没给（与 0 字节区分开）。 */
private fun sakuraBytes(bytes: Long): String = when {
    bytes < 0L -> "大小未知"
    bytes < 1024L -> bytes.toString() + " B"
    bytes < 1024L * 1024L -> (bytes / 1024.0).let { String.format("%.1f KB", it) }
    else -> (bytes / (1024.0 * 1024.0)).let { String.format("%.1f MB", it) }
}

/** 页首窄带：把 theme_*_texture 当成一条不透明视觉带（严禁 alpha 叠层），高度固定、Crop 铺满。 */
@Composable
private fun SakuraTextureBand(texture: Int?, height: Dp) {
    if (texture == null) return
    Image(
        painter = painterResource(texture),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().height(height).clip(JarvisShapes.sm)
    )
    Spacer(Modifier.height(JarvisSpacing.space3))
}
