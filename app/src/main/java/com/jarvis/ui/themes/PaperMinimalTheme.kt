package com.jarvis.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jarvis.app.R
import com.jarvis.app.runtime.BootstrapReport
import com.jarvis.app.runtime.ChatMessage
import com.jarvis.app.runtime.ChatMessageRole
import com.jarvis.app.runtime.RuntimeState
import com.jarvis.app.runtime.WorkspaceProfile
import com.jarvis.ui.components.BadgeTone
import com.jarvis.ui.components.DangerActionLabel
import com.jarvis.ui.components.EmptyStatePanel
import com.jarvis.ui.components.JarvisChipRow
import com.jarvis.ui.components.JarvisDivider
import com.jarvis.ui.components.JarvisInsetCard
import com.jarvis.ui.components.JarvisLoading
import com.jarvis.ui.components.NoticeLine
import com.jarvis.ui.components.NoticeTone
import com.jarvis.ui.components.SectionHeader
import com.jarvis.ui.components.SettingSwitchRow
import com.jarvis.ui.components.StatusBadge
import com.jarvis.ui.components.jarvisScreenPadding
import com.jarvis.ui.theme.AutomationRuleView
import com.jarvis.ui.theme.ChatUiContext
import com.jarvis.ui.theme.ChoiceOption
import com.jarvis.ui.theme.ChromeStyle
import com.jarvis.ui.theme.FloatingPaletteSpec
import com.jarvis.ui.theme.JarvisThemeModule
import com.jarvis.ui.theme.LocalJarvisMode
import com.jarvis.ui.theme.SettingsUiContext
import com.jarvis.ui.theme.TasksUiContext
import com.jarvis.ui.theme.ThemeArtwork
import com.jarvis.ui.theme.ThemeMode
import com.jarvis.ui.theme.ThemeModeOverride
import com.jarvis.ui.theme.ThemePalette
import com.jarvis.ui.theme.WorkspaceFileView
import com.jarvis.ui.theme.WorkspaceUiContext
import com.jarvis.ui.tokens.JarvisShapes
import com.jarvis.ui.tokens.JarvisSize
import com.jarvis.ui.tokens.JarvisSpacing
import com.jarvis.ui.tokens.JarvisType

/** 清单行高：收敛到 JarvisSize.rowMinHeight（52dp），不再是主题私有的 64dp。 */
private val RowHeight = JarvisSize.rowMinHeight

/** 选中竖条的图形宽度（不是间距）。 */
private val AccentStrip = 2.dp

/** 目录预览文本折叠到这么多字符，避免单条把整页撑开。 */
private const val PreviewCharLimit = 1200

/**
 * 浅色「纸面清单」：白纸 surface + 灰白 background，分割线与留白分层，靠明度差而不是描边分块。
 *
 * 硬指标（自算，见报告）：borderSubtle/surface 1.71:1、ΔL*(surface,background) 4.88、
 * onSurface/surface 16.87:1、onSurfaceVariant/surface 5.91:1、onPrimary/primary 9.70:1。
 */
private val PaperLightPalette = ThemePalette(
    primary = Color(0xFF2A4A45), onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF5A5C60), onSecondary = Color(0xFFFBFBFA),
    background = Color(0xFFF2F1ED), onBackground = Color(0xFF1C1D1F),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1C1D1F),
    surfaceContainerLow = Color(0xFFF0EFEC), surfaceContainer = Color(0xFFEBEAE6),
    surfaceContainerHigh = Color(0xFFE4E3DE), onSurfaceVariant = Color(0xFF62646A),
    outline = Color(0xFFBCBEC1), borderSubtle = Color(0xFFC6C6C1),
    divider = Color(0xFFE8E6E1), error = Color(0xFF9B2C21),
    onError = Color(0xFFFFF7F5), errorContainer = Color(0xFFF3DEDA),
    onErrorContainer = Color(0xFF5F1D14), scrim = Color(0xFF141516),
    accentSoft = Color(0xFFE6EDEA), glow = Color(0xFFDDDCD6),
    // 选中态 chip / ACTIVE 徽标的底与字：不给就会回落到 onPrimary（白字压浅底 1.1:1，等于看不见）。
    primaryContainer = Color(0xFFE6EDEA), onPrimaryContainer = Color(0xFF2A4A45)
)

/**
 * 暗色「暗夜纸面」：石墨纸底 + 微亮纸面，纸纹降到 10% 不透明度当作暗面的压纹。
 *
 * 硬指标（自算，见报告）：borderSubtle/surface 1.59:1、ΔL*(surface,background) 5.96、
 * onSurface/surface 13.10:1、onSurfaceVariant/surface 6.71:1、onPrimary/primary 9.41:1。
 */
private val PaperDarkPalette = ThemePalette(
    primary = Color(0xFF95C3B7), onPrimary = Color(0xFF0F1613),
    secondary = Color(0xFFB9BBC0), onSecondary = Color(0xFF191B1F),
    background = Color(0xFF15171B), onBackground = Color(0xFFEDEAE3),
    surface = Color(0xFF202329), onSurface = Color(0xFFEDEAE3),
    surfaceContainerLow = Color(0xFF1B1E22), surfaceContainer = Color(0xFF2A2E35),
    surfaceContainerHigh = Color(0xFF34383F), onSurfaceVariant = Color(0xFFA8A9AE),
    outline = Color(0xFF4A4F58), borderSubtle = Color(0xFF3F434B),
    divider = Color(0xFF2B3038), error = Color(0xFFE7A29B),
    onError = Color(0xFF2A0F0C), errorContainer = Color(0xFF4A211C),
    onErrorContainer = Color(0xFFF7D9D4), scrim = Color(0xFF000000),
    accentSoft = Color(0xFF23302C), glow = Color(0xFF2A2E35),
    primaryContainer = Color(0xFF23302C), onPrimaryContainer = Color(0xFF95C3B7)
)

/** 新建规则时「画师字段只属于 Pixiv 类型」的唯一判定口径。 */
private fun isPixivKind(kindKey: String, kindLabel: String): Boolean =
    kindKey.contains("pixiv", ignoreCase = true) || kindLabel.contains("pixiv", ignoreCase = true)

/** 目录条目的一行摘要：大小/类型 + 是否可预览。 */
private fun fileSummary(file: WorkspaceFileView): String {
    val size = when {
        file.isDirectory -> "目录"
        file.sizeBytes < 0L -> "大小未知"
        file.sizeBytes < 1024L -> file.sizeBytes.toString() + " B"
        file.sizeBytes < 1024L * 1024L -> (file.sizeBytes / 1024L).toString() + " KB"
        else -> (file.sizeBytes / (1024L * 1024L)).toString() + " MB"
    }
    return size + if (file.previewable) " · 可预览" else " · 只读"
}

/**
 * 纸感极简：亮色纸面清单 + 满幅点纹，暗色是「暗夜纸面」（同一套纸纹压到 10% 不透明度）。
 *
 * 结构纪律：不做卡片盒子，分块靠 surface / surfaceContainerHigh 的明度差（JarvisContrast 路②），
 * 输入槽用 surfaceContainerLow 实底承载，行与行之间只留 JarvisDivider 的 0.5dp 发丝线，
 * 因此不存在「粗边框 + 大字号」那种糊成一片的观感：标签降到 labelBadge 11sp Medium，
 * 值保持 bodyBase 14sp，靠字号与字重而不是描边区分层级。
 */
internal object PaperMinimalTheme : JarvisThemeModule {
    override val id = "paper_minimal"
    override val label = "纸感极简"
    override val story = "白纸留白"
    override val description = "灰白 · 清单分割"
    override val mode = ThemeMode.LIGHT

    /** 身份色板 = 浅色纸面；[lightPalette] 与它同值，两套都由作者手写，不做推导。 */
    override val palette = PaperLightPalette
    override val lightPalette: ThemePalette = PaperLightPalette
    override val darkPalette: ThemePalette = PaperDarkPalette

    override val artwork = ThemeArtwork(
        banner = R.drawable.theme_paper_minimal_banner,
        avatar = R.drawable.theme_paper_minimal_avatar,
        texture = R.drawable.theme_paper_minimal_texture
    )
    override val chrome = ChromeStyle.DRAWER

    /** 浮窗是 View 层，拿不到 CompositionLocal，按卡面墨色给一份可直接用的取值（纸面浅色为基准）。 */
    override fun floatingPalette() = FloatingPaletteSpec(
        panel = palette.surface, title = palette.onSurface,
        body = palette.onSurfaceVariant, accent = palette.primary,
        muted = palette.onSurfaceVariant, bubbleRing = palette.divider
    )

    /**
     * 此刻该用哪一套手写色板。
     *
     * 取的是外壳（JarvisShell）提供的 [LocalJarvisMode]——它已经是「mode_override + 系统明暗」
     * 解析后的实际模式，所以既不用猜系统值，也不用从 background 亮度反查（反查每次重组都要重算，
     * 且把配色来源藏进颜色值里）。remember 保证同一个 mode 下返回的是同一个实例。
     */
    @Composable
    private fun pal(): ThemePalette {
        val mode = LocalJarvisMode.current
        return remember(mode) { if (mode == ThemeMode.DARK) darkPalette else lightPalette }
    }

    /* ---------------- 清单基元 ---------------- */
    @Composable
    private fun Meta(text: String, align: TextAlign = TextAlign.Start) {
        val pal = pal()
        Text(text, Modifier.fillMaxWidth(), color = pal.onSurfaceVariant, style = JarvisType.bodyMeta, textAlign = align)
    }

    /** 纸面根容器：background 打底 + 满幅平铺纸纹；暗色把纸纹压到 10%，避免亮纹理糊掉暗面。 */
    @Composable
    private fun PaperRoot(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
        val pal = pal()
        val dark = pal === darkPalette
        Box(modifier.fillMaxSize().background(pal.background)) {
            artwork.texture?.let { texture ->
                val bitmap = ImageBitmap.imageResource(texture)
                val brush = remember(bitmap) {
                    ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
                }
                Box(Modifier.matchParentSize().alpha(if (dark) 0.10f else 0.55f).background(brush))
            }
            content()
        }
    }

    @Composable
    private fun PaperPage(modifier: Modifier = Modifier, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
        PaperRoot(modifier) {
            Column(
                modifier = if (scroll) {
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).jarvisScreenPadding()
                } else {
                    Modifier.fillMaxSize().jarvisScreenPadding()
                }
            ) { content() }
        }
    }

    /** 四个页面统一用 displaySection 页题，左对齐，下面只跟一行灰字与一条发丝分割线。 */
    @Composable
    private fun PageTitle(title: String, subtitle: String) {
        val pal = pal()
        Text(title, color = pal.onBackground, style = JarvisType.displaySection)
        Spacer(Modifier.height(JarvisSpacing.space2))
        Meta(subtitle)
        Spacer(Modifier.height(JarvisSpacing.cardGap))
        JarvisDivider()
    }

    @Composable
    private fun GroupTitle(title: String) {
        Spacer(Modifier.height(JarvisSpacing.sectionGap))
        SectionHeader(title)
        JarvisDivider()
    }

    /** 清单行：52dp 行高、左标题右 chevron；选中行靠左侧 2dp 强调竖条，不靠底色。 */
    @Composable
    private fun NavRow(
        title: String,
        subtitle: String = "",
        selected: Boolean = false,
        onClick: (() -> Unit)? = null
    ) {
        val pal = pal()
        val base = Modifier.fillMaxWidth().heightIn(min = RowHeight)
        Row(
            modifier = if (onClick == null) base else base.clip(JarvisShapes.xs).clickable(role = Role.Button, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.width(AccentStrip).heightIn(min = RowHeight)
                    .then(if (selected) Modifier.background(pal.primary) else Modifier)
            )
            Spacer(Modifier.width(JarvisSpacing.space4))
            Column(Modifier.weight(1f)) {
                Text(title, color = pal.onSurface, style = JarvisType.titleItem)
                if (subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(JarvisSpacing.space2))
                    Meta(subtitle)
                }
            }
            if (onClick != null) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = pal.onSurfaceVariant)
            }
        }
        JarvisDivider()
    }

    /** 信息行：不可跳转但有右侧动作（安装/删除/状态），同样 52dp 起。 */
    @Composable
    private fun InfoRow(title: String, detail: String, action: @Composable (() -> Unit)? = null) {
        val pal = pal()
        Row(Modifier.fillMaxWidth().heightIn(min = RowHeight), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(JarvisSpacing.space4))
            Column(Modifier.weight(1f)) {
                Text(title, color = pal.onSurface, style = JarvisType.titleItem)
                if (detail.isNotEmpty()) {
                    Spacer(Modifier.height(JarvisSpacing.space2))
                    Meta(detail)
                }
            }
            if (action != null) action()
        }
        JarvisDivider()
    }

    /**
     * 表单行：11sp Medium 小标签在上，值放在 surfaceContainerLow 实底槽位里，不画外框。
     *
     * 这是 JarvisContrast 路②（底色成块）：槽位底色与页面 paper 面拉开层次，因此不需要任何描边；
     * 去掉粗边框后同步把标签压到 labelBadge/Medium、值保持 bodyBase，靠字号字重分主次。
     */
    @Composable
    private fun FieldLine(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
        hint: String = "",
        secret: Boolean = false,
        singleLine: Boolean = true,
        minLines: Int = 1
    ) {
        val pal = pal()
        Column(Modifier.fillMaxWidth().padding(vertical = JarvisSpacing.space3)) {
            Text(label, color = pal.onSurfaceVariant, style = JarvisType.labelBadge)
            Spacer(Modifier.height(JarvisSpacing.space2))
            Box(
                Modifier.fillMaxWidth()
                    .clip(JarvisShapes.xs)
                    .background(pal.surfaceContainerLow)
                    .padding(horizontal = JarvisSpacing.space4, vertical = JarvisSpacing.space3)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = JarvisSize.minTouch - JarvisSpacing.space6),
                    singleLine = singleLine,
                    minLines = minLines,
                    textStyle = JarvisType.bodyBase.copy(color = pal.onSurface),
                    cursorBrush = SolidColor(pal.primary),
                    visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty() && hint.isNotEmpty()) {
                                Text(hint, color = pal.onSurfaceVariant, style = JarvisType.bodyMeta)
                            }
                            inner()
                        }
                    }
                )
            }
        }
    }

    /** 芯片行：视觉高度由 JarvisChipRow 取 JarvisSize.chipHeight（40dp），这里只负责上下留白。 */
    @Composable
    private fun Chips(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
        if (labels.isEmpty()) return
        Column(Modifier.fillMaxWidth().padding(vertical = JarvisSpacing.space2)) {
            JarvisChipRow(labels, selectedIndex.coerceAtLeast(0), onSelect)
        }
    }

    @Composable
    private fun ChoiceChips(options: List<ChoiceOption>, selectedKey: String, onSelect: (String) -> Unit) =
        Chips(options.map { it.label }, options.indexOfFirst { it.key == selectedKey }) { onSelect(options[it].key) }

    @Composable
    private fun IntChips(choices: List<Int>, current: Int, suffix: String, onPick: (Int) -> Unit) =
        Chips(choices.map { it.toString() + suffix }, choices.indexOf(current)) { onPick(choices[it]) }

    /** 文字动作：不用 Material 按钮底，触达高度钉在 JarvisSize.minTouch（44dp）。 */
    @Composable
    private fun TextAction(
        text: String,
        onClick: () -> Unit,
        color: Color? = null,
        enabled: Boolean = true
    ) {
        val pal = pal()
        Box(
            Modifier.heightIn(min = JarvisSize.minTouch)
                .clip(JarvisShapes.xs)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = JarvisSpacing.space3),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                color = when {
                    !enabled -> pal.onSurfaceVariant
                    color != null -> color
                    else -> pal.onSurface
                },
                style = JarvisType.titleItem
            )
        }
    }

    /* ---------------- 对话 ---------------- */
    @Composable
    override fun ChatScreen(ctx: ChatUiContext) {
        val state = ctx.state
        val cb = ctx.callbacks
        val efforts = state.reasoningEfforts.ifEmpty { listOf("off", "minimal", "low", "medium", "high", "max") }
        val listState = rememberLazyListState()
        LaunchedEffect(state.conversationId, state.messages.size) {
            if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
        }
        PaperRoot(Modifier.padding(state.contentPadding)) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.jarvisScreenPadding()) {
                    PageTitle("对话", "在消息下方编辑、撤销或创建分支")
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextAction("新建对话", cb.onNewConversation)
                        TextAction("创建分支", { cb.onBranch(null) })
                        Spacer(Modifier.weight(1f))
                        TextAction("工具箱", cb.onOpenTools)
                    }
                    Chips(efforts, efforts.indexOf(state.reasoningEffort)) { cb.onReasoningEffortChange(efforts[it]) }
                }
                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    if (state.messages.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                "从一个小任务开始",
                                subtitle = "交给 Jarvis，逐步完成。",
                                icon = Icons.Default.Add,
                                modifier = Modifier.fillMaxWidth().heightIn(min = RowHeight * 4)
                            )
                        }
                    }
                    items(state.messages, key = { it.id }) { message ->
                        MessageRow(message, message.id == state.messages.lastOrNull()?.id, state, cb)
                    }
                    if (state.sending) item { JarvisLoading("正在请求模型…") }
                    if (state.speechNotice.isNotEmpty()) item { NoticeLine(state.speechNotice) }
                }
                InputBar(state, cb)
            }
        }
    }

    @Composable
    private fun MessageRow(
        message: ChatMessage,
        isLatest: Boolean,
        state: ChatUiContext.ChatState,
        cb: ChatUiContext.ChatCallbacks
    ) {
        val pal = pal()
        Row(Modifier.fillMaxWidth().padding(vertical = JarvisSpacing.space3).jarvisScreenPadding()) {
            Box(
                Modifier.width(AccentStrip).heightIn(min = JarvisSize.minTouch)
                    .then(if (message.role == ChatMessageRole.USER) Modifier.background(pal.primary) else Modifier)
            )
            Spacer(Modifier.width(JarvisSpacing.space4))
            Column(Modifier.weight(1f)) {
                Meta(if (message.role == ChatMessageRole.USER) "你" else "Jarvis")
                Spacer(Modifier.height(JarvisSpacing.space2))
                Text(message.content, color = pal.onSurface, style = JarvisType.bodyBase)
                if (isLatest) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.canEditLatest) TextAction("编辑", cb.onEditLatest)
                        if (state.canUndoLatest) TextAction("撤销", cb.onUndoLatest)
                        TextAction("分支", { cb.onBranch(message.id) })
                    }
                }
            }
        }
        JarvisDivider()
    }

    /** 发送键：直径 JarvisSize.sendButton（44dp）圆形主色块，禁用态落到 surfaceContainerHigh。 */
    @Composable
    private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
        val pal = pal()
        Box(
            Modifier.size(JarvisSize.sendButton)
                .clip(JarvisShapes.pill)
                .background(if (enabled) pal.primary else pal.surfaceContainerHigh)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "发送",
                color = if (enabled) pal.onPrimary else pal.onSurfaceVariant,
                style = JarvisType.labelBadge
            )
        }
    }

    /** 输入区：surface 底 + 底部发丝线；输入槽用 surfaceContainerLow 实底，不再有任何外框。 */
    @Composable
    private fun InputBar(state: ChatUiContext.ChatState, cb: ChatUiContext.ChatCallbacks) {
        val pal = pal()
        Surface(color = pal.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = JarvisSpacing.pageHorizontal, vertical = JarvisSpacing.space3)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.weight(1f)
                            .clip(JarvisShapes.xs)
                            .background(pal.surfaceContainerLow)
                            .padding(horizontal = JarvisSpacing.space4, vertical = JarvisSpacing.space3)
                    ) {
                        BasicTextField(
                            value = state.draft,
                            onValueChange = cb.onDraftChange,
                            modifier = Modifier.fillMaxWidth().heightIn(min = JarvisSpacing.space7),
                            maxLines = 5,
                            textStyle = JarvisType.bodyBase.copy(color = pal.onSurface),
                            cursorBrush = SolidColor(pal.primary),
                            decorationBox = { inner ->
                                Box {
                                    if (state.draft.isEmpty()) {
                                        Text("给 Jarvis 发消息，或描述一个任务…", color = pal.onSurfaceVariant, style = JarvisType.bodyBase)
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                    Spacer(Modifier.width(JarvisSpacing.space3))
                    IconButton(onClick = cb.onVoiceInput, enabled = !state.voiceListening, modifier = Modifier.size(JarvisSize.minTouch)) {
                        Icon(
                            Icons.Default.Mic, tint = pal.onSurfaceVariant,
                            contentDescription = if (state.voiceListening) "正在识别语音" else "语音转文字"
                        )
                    }
                    SendButton(enabled = state.draft.isNotBlank() && !state.sending) { cb.onSend(state.draft) }
                }
                Spacer(Modifier.height(JarvisSpacing.space2))
                JarvisDivider()
            }
        }
    }

    /* ---------------- 任务 ---------------- */
    @Composable
    override fun TasksScreen(ctx: TasksUiContext) {
        val state = ctx.state
        val cb = ctx.callbacks
        PaperPage(Modifier.padding(state.contentPadding)) {
            PageTitle("任务", "通过对话创建，收到通知后按规则执行一次。")
            Row(
                Modifier.fillMaxWidth().padding(top = JarvisSpacing.space3),
                horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space3)
            ) {
                StatusBadge(if (state.notificationAccess) "通知已连接" else "通知未授权", if (state.notificationAccess) BadgeTone.ACTIVE else BadgeTone.WARNING)
                StatusBadge(if (state.accessibilityGranted) "无障碍已连接" else "无障碍未启用", if (state.accessibilityGranted) BadgeTone.ACTIVE else BadgeTone.WARNING)
                StatusBadge(if (state.runtimeReady) "运行时就绪" else "运行时未就绪", if (state.runtimeReady) BadgeTone.ACTIVE else BadgeTone.WARNING)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextAction("授权通知", cb.onRequestNotificationAccess)
                TextAction("开启无障碍", cb.onRequestAccessibility)
                TextAction("刷新", cb.onRefresh)
            }
            if (state.notice.isNotEmpty()) NoticeLine(state.notice)
            JarvisDivider()
            if (state.creatingRule) {
                NewRuleForm(state, cb)
            } else {
                TextAction("新建规则", cb.onCreateRuleStart)
            }
            if (state.rules.isEmpty() && !state.creatingRule) {
                EmptyStatePanel(
                    "还没有自动化",
                    icon = Icons.Default.Add,
                    modifier = Modifier.fillMaxWidth().heightIn(min = RowHeight * 4),
                    subtitle = "在对话中输入“当哔哩哔哩上有人回复我就立刻回复”或“当 Pixiv 画师发布新作品就立刻下载”，也可以点「新建规则」直接填表。",
                    action = { TextAction("新建规则", cb.onCreateRuleStart) }
                )
            } else {
                state.rules.forEach { rule -> RuleRow(rule, cb) }
            }
            if (state.recentEvents.isNotEmpty()) {
                GroupTitle("最近执行")
                state.recentEvents.take(5).forEach { event ->
                    InfoRow(event.title, event.message) {
                        StatusBadge(if (event.ok) "成功" else "失败", if (event.ok) BadgeTone.ACTIVE else BadgeTone.ERROR)
                    }
                }
            }
            Spacer(Modifier.height(JarvisSpacing.space7))
        }
        val editing = state.rules.firstOrNull { it.id == state.editingRuleId }
        if (editing != null) RuleDialog(editing, state, cb)
    }

    /**
     * 新建自动化的内联表单（TasksState.creatingRule）。
     *
     * 整块是 surfaceContainerHigh 实底（JarvisContrast 路②），没有外框；画师字段只在 Pixiv 类型出现；
     * ruleDraftError 非空时用 error 语气原样显示，不静默吞掉校验失败。
     */
    @Composable
    private fun NewRuleForm(state: TasksUiContext.TasksState, cb: TasksUiContext.TasksCallbacks) {
        val pal = pal()
        val kindLabel = state.ruleDraftKindOptions.firstOrNull { it.key == state.ruleDraftKindKey }?.label.orEmpty()
        JarvisInsetCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = JarvisSpacing.space3),
            contentPadding = JarvisSpacing.space5,
            shape = JarvisShapes.sm
        ) {
            Text("新建自动化", color = pal.onSurface, style = JarvisType.titleBlock)
            Spacer(Modifier.height(JarvisSpacing.space2))
            Meta("标题与触发要求都填写后才能提交；画师只在 Pixiv 类型出现。")
            FieldLine("规则名称", state.ruleDraftTitle, cb.onCreateRuleTitleChange, hint = "例如：B 站有人回复就回一句")
            if (state.ruleDraftKindOptions.isNotEmpty()) {
                Text("规则类型", color = pal.onSurfaceVariant, style = JarvisType.labelBadge)
                Spacer(Modifier.height(JarvisSpacing.space2))
                ChoiceChips(state.ruleDraftKindOptions, state.ruleDraftKindKey, cb.onCreateRuleKindChange)
            }
            FieldLine(
                "触发要求", state.ruleDraftPrompt, cb.onCreateRulePromptChange,
                hint = "当…就…", singleLine = false, minLines = 3
            )
            if (isPixivKind(state.ruleDraftKindKey, kindLabel)) {
                FieldLine(
                    "Pixiv 画师名称或用户 ID", state.ruleDraftArtist, cb.onCreateRuleArtistChange,
                    hint = "留空则按收藏画师监听"
                )
            }
            if (state.ruleDraftError.isNotEmpty()) {
                NoticeLine(state.ruleDraftError, tone = NoticeTone.ERROR)
                Spacer(Modifier.height(JarvisSpacing.space2))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextAction("提交", cb.onCreateRuleSubmit, color = pal.primary)
                Spacer(Modifier.weight(1f))
                TextAction("取消", cb.onCreateRuleCancel)
            }
        }
        JarvisDivider()
    }

    @Composable
    private fun RuleRow(rule: AutomationRuleView, cb: TasksUiContext.TasksCallbacks) {
        val pal = pal()
        SettingSwitchRow(
            rule.title, rule.enabled, { cb.onToggleRule(rule.id, it) },
            subtitle = rule.kindLabel + " · " + if (rule.enabled) "已启用 · 等待目标应用通知" else "已暂停 · 不会自动操作"
        )
        if (rule.lastResult.isNotEmpty()) Meta("最近结果：" + rule.lastResult)
        rule.readiness?.let { Text("启用前：" + it, color = pal.error, style = JarvisType.bodyMeta) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextAction("配置", { cb.onConfigureRule(rule.id) })
            Spacer(Modifier.weight(1f))
            DangerActionLabel("删除", { cb.onDeleteRule(rule.id) })
        }
        JarvisDivider()
    }

    @Composable
    private fun RuleDialog(
        rule: AutomationRuleView,
        state: TasksUiContext.TasksState,
        cb: TasksUiContext.TasksCallbacks
    ) {
        val pal = pal()
        AlertDialog(
            onDismissRequest = cb.onCancelRuleConfig, containerColor = pal.surface,
            titleContentColor = pal.onSurface, textContentColor = pal.onSurfaceVariant,
            title = { Text("配置自动化", style = JarvisType.titleBlock) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (rule.sourcePrompt.isNotEmpty()) Meta("原始要求：" + rule.sourcePrompt)
                    if (state.artistDraft.isEmpty() && rule.artist.isNotEmpty()) Meta("当前画师：" + rule.artist)
                    if (isPixivKind(rule.kindKey, rule.kindLabel)) {
                        FieldLine("Pixiv 画师名称或用户 ID", state.artistDraft, cb.onArtistDraftChange)
                    }
                    FieldLine("回复风格要求", state.replyDraft, cb.onReplyDraftChange, singleLine = false, minLines = 3)
                    if (state.replyDraft.isEmpty() && rule.replyInstruction.isNotEmpty()) Meta("当前要求：" + rule.replyInstruction)
                    Meta("启用前还需要目标应用通知访问和 Jarvis 无障碍服务。")
                }
            },
            confirmButton = { TextButton(onClick = { cb.onSaveRuleConfig(rule.id, state.artistDraft, state.replyDraft) }) { Text("保存") } },
            dismissButton = { TextButton(onClick = cb.onCancelRuleConfig) { Text("取消") } }
        )
    }

    /* ---------------- 工作区 ---------------- */
    @Composable
    override fun WorkspaceScreen(ctx: WorkspaceUiContext) {
        val pal = pal()
        val state = ctx.state
        val cb = ctx.callbacks
        var editing by remember { mutableStateOf<WorkspaceProfile?>(null) }
        var creating by remember { mutableStateOf(false) }
        var renameId by remember { mutableStateOf<String?>(null) }
        var renameDraft by remember { mutableStateOf("") }
        var movingId by remember { mutableStateOf<String?>(null) }
        var previewKey by remember { mutableStateOf<String?>(null) }
        val selected = state.workspaces.firstOrNull { it.name == state.selectedWorkspace }
        PaperPage(Modifier.padding(state.contentPadding)) {
            PageTitle("工作区", "为每个工作区选择共同设定，或单独编辑人格与世界书。")

            GroupTitle("归档")
            SettingSwitchRow(
                "显示已归档", state.showArchived, cb.onToggleShowArchived,
                subtitle = "已归档工作区 " + state.archivedWorkspaces.size + " 个 · 已归档对话 " +
                    state.archivedConversationIds.size + " 条；归档只隐藏，不删数据"
            )
            JarvisDivider()

            GroupTitle("工作区清单")
            NavRow("新建工作区", "为每个工作区选择共同设定，或单独编辑人格与世界书", false) {
                creating = true
                editing = WorkspaceProfile(name = "")
            }
            state.workspaces.forEach { profile ->
                val sessions = state.conversationCountByWorkspace[profile.name]
                    ?: state.conversations.count { it.workspace == profile.name }
                val dirText = profile.dirLabel.ifBlank {
                    if (profile.hasDirectory) "已绑定（提供方未给名称）" else "未绑定"
                }
                NavRow(
                    title = profile.name + if (profile.archived) " · 已归档" else "",
                    subtitle = (if (profile.useGlobalPersona) "共同人格与世界书" else "自定义人格与世界书") +
                        " · " + sessions + " 个会话 · 目录：" + dirText,
                    selected = profile.name == state.selectedWorkspace,
                    onClick = { cb.onSelect(profile.name) }
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextAction("编辑", { creating = false; editing = profile })
                    TextAction(if (profile.hasDirectory) "解绑目录" else "绑定目录", {
                        if (profile.hasDirectory) cb.onClearDirectory(profile.name) else cb.onPickDirectory(profile.name)
                    })
                    if (profile.archived) {
                        TextAction("还原", { cb.onUnarchive(profile.name) }, color = pal.primary)
                    } else if (profile.name != WorkspaceProfile.DEFAULT_NAME) {
                        TextAction("归档", { cb.onArchive(profile.name) })
                    }
                    if (profile.name != WorkspaceProfile.DEFAULT_NAME) {
                        DangerActionLabel("删除", { cb.onDelete(profile.name) })
                    }
                }
                JarvisDivider()
            }

            GroupTitle("当前工作区目录（只读）")
            if (selected == null) {
                Meta("先在上方选择一个工作区，目录与文件清单才会显示。")
            } else {
                Meta(
                    "目录：" + selected.dirLabel.ifBlank {
                        if (selected.hasDirectory) "已绑定（提供方未给名称）" else "未绑定"
                    }
                )
                Spacer(Modifier.height(JarvisSpacing.space2))
                Meta(if (state.dirReady) "读取权限有效 · 清单只读，不提供改文件入口" else "读取权限失效或未绑定，请重新选择目录")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    TextAction("选择目录", { cb.onPickDirectory(selected.name) })
                    if (selected.hasDirectory) TextAction("解除绑定", { cb.onClearDirectory(selected.name) }, color = pal.error)
                }
                JarvisDivider()
                if (state.filesLoading) JarvisLoading("正在读取目录…")
                if (state.filesNotice.isNotEmpty()) NoticeLine(state.filesNotice)
                if (state.files.isEmpty() && !state.filesLoading) {
                    Meta("目录为空，或尚未绑定目录。")
                }
                state.files.forEach { file ->
                    val key = file.name + "@" + file.lastModified
                    FileRow(file, expanded = previewKey == key) { previewKey = if (previewKey == key) null else key }
                }
            }

            GroupTitle("对话")
            if (state.conversations.isEmpty()) {
                Meta(if (state.showArchived) "还没有会话。" else "还没有会话；已归档的会话需要打开「显示已归档」。")
            }
            state.conversations.forEach { session ->
                val archived = session.isArchived || state.archivedConversationIds.contains(session.id)
                Row(Modifier.fillMaxWidth().heightIn(min = RowHeight), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(JarvisSpacing.space4))
                    Column(Modifier.weight(1f)) {
                        Text(
                            session.title.ifBlank { "未命名对话" },
                            color = pal.onSurface, style = JarvisType.titleItem
                        )
                        Spacer(Modifier.height(JarvisSpacing.space2))
                        Meta(
                            session.workspace + " · " + session.messages.size + " 条消息" +
                                if (archived) " · 已归档" else ""
                        )
                    }
                }
                if (renameId == session.id) {
                    FieldLine("重命名对话", renameDraft, { renameDraft = it }, hint = "空白标题会被忽略")
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextAction("保存名称", {
                            cb.onRenameConversation(session.id, renameDraft)
                            renameId = null
                        }, color = pal.primary)
                        Spacer(Modifier.weight(1f))
                        TextAction("取消", { renameId = null })
                    }
                }
                if (movingId == session.id) {
                    val targets = state.workspaces.filter { it.name != session.workspace }
                    Meta(if (targets.isEmpty()) "没有别的工作区可移动" else "移动到工作区")
                    Spacer(Modifier.height(JarvisSpacing.space2))
                    if (targets.isNotEmpty()) {
                        Chips(targets.map { it.name }, -1) { index ->
                            cb.onMoveConversation(session.id, targets[index].name)
                            movingId = null
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextAction("改名", { renameId = session.id; renameDraft = session.title })
                    TextAction(if (movingId == session.id) "收起移动" else "移动", { movingId = if (movingId == session.id) null else session.id })
                    if (archived) {
                        TextAction("还原", { cb.onUnarchiveConversation(session.id) }, color = pal.primary)
                    } else {
                        TextAction("归档", { cb.onArchiveConversation(session.id) })
                    }
                    DangerActionLabel("删除", { cb.onDeleteConversation(session.id) })
                }
                JarvisDivider()
            }

            GroupTitle("全局人格与世界书")
            Meta(state.globalPersona.ifBlank { "—" })
            Spacer(Modifier.height(JarvisSpacing.space2))
            Meta(state.globalWorldBook.ifBlank { "—" })
            Spacer(Modifier.height(JarvisSpacing.space7))
        }
        editing?.let { profile ->
            WorkspaceEditor(
                profile = profile, isNew = creating, state = state,
                onDismiss = { editing = null },
                onSave = { updated ->
                    cb.onSave(updated, if (creating) null else profile.name)
                    editing = null
                }
            )
        }
    }

    /** 目录条目：52dp 行；可预览项整行可点展开预览文本（只读，折叠到 1200 字）。 */
    @Composable
    private fun FileRow(file: WorkspaceFileView, expanded: Boolean, onToggle: () -> Unit) {
        val pal = pal()
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = RowHeight)
                    .then(if (file.previewable) Modifier.clickable(role = Role.Button, onClick = onToggle) else Modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(JarvisSpacing.space4))
                Column(Modifier.weight(1f)) {
                    Text(file.name, color = pal.onSurface, style = JarvisType.titleItem)
                    Spacer(Modifier.height(JarvisSpacing.space2))
                    Meta(fileSummary(file))
                }
                if (file.previewable) {
                    TextAction(if (expanded) "收起" else "预览", onToggle, color = pal.primary)
                }
            }
            if (expanded && file.previewable) {
                Box(
                    Modifier.fillMaxWidth()
                        .padding(bottom = JarvisSpacing.space3)
                        .clip(JarvisShapes.xs)
                        .background(pal.surfaceContainerLow)
                        .padding(JarvisSpacing.space4)
                ) {
                    Text(
                        file.previewText.take(PreviewCharLimit),
                        color = pal.onSurface, style = JarvisType.codeInline
                    )
                }
            }
            JarvisDivider()
        }
    }

    @Composable
    private fun WorkspaceEditor(
        profile: WorkspaceProfile,
        isNew: Boolean,
        state: WorkspaceUiContext.WorkspaceState,
        onDismiss: () -> Unit,
        onSave: (WorkspaceProfile) -> Unit
    ) {
        val pal = pal()
        var name by remember(profile) { mutableStateOf(profile.name) }
        var shared by remember(profile) { mutableStateOf(profile.useGlobalPersona) }
        var persona by remember(profile) { mutableStateOf(profile.mentorPrompt) }
        var worldBook by remember(profile) { mutableStateOf(profile.worldBook) }
        AlertDialog(
            onDismissRequest = onDismiss, containerColor = pal.surface,
            titleContentColor = pal.onSurface, textContentColor = pal.onSurfaceVariant,
            title = { Text(if (isNew) "新建工作区" else "编辑工作区", style = JarvisType.titleBlock) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    FieldLine("工作区名称", name, { name = it })
                    SettingSwitchRow("使用共同设定", shared, { shared = it }, subtitle = "此工作区会跟随设置中的全局人格与世界书。")
                    JarvisDivider()
                    FieldLine("自定义人格 / 提示词", persona, { persona = it }, singleLine = false, minLines = 3)
                    FieldLine("自定义世界书", worldBook, { worldBook = it }, singleLine = false, minLines = 3)
                    if (state.personaSources.isNotEmpty()) {
                        Meta("从其他对话快捷粘贴")
                        Spacer(Modifier.height(JarvisSpacing.space2))
                        Chips(state.personaSources.map { it.title }, -1) { index ->
                            persona = state.personaSources[index].mentorPrompt
                            worldBook = state.personaSources[index].worldBook
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(
                        profile.copy(
                            name = name.trim(), useGlobalPersona = shared,
                            mentorPrompt = persona, worldBook = worldBook
                        )
                    )
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
        )
    }

    /* ---------------- 设置 ---------------- */
    @Composable
    override fun SettingsScreen(ctx: SettingsUiContext) {
        val pal = pal()
        val state = ctx.state
        val cb = ctx.callbacks
        val efforts = state.reasoningEfforts.ifEmpty { listOf("off", "minimal", "low", "medium", "high", "max") }
        val themes = state.themeOptions
        val currentTheme = themes.firstOrNull { it.id == state.themeId } ?: themes.firstOrNull()
        val modeChoices = listOf(ThemeModeOverride.FOLLOW_SYSTEM, ThemeModeOverride.LIGHT, ThemeModeOverride.DARK)
        var pluginDraft by remember { mutableStateOf("") }
        PaperPage(Modifier.padding(state.contentPadding)) {
            PageTitle("设置", "Jarvis " + state.versionName + " · 模型、权限与设备连接")
            GroupTitle("风格与外观")
            Meta((currentTheme?.story ?: state.theme.story) + " · " + (currentTheme?.description ?: state.theme.description))
            Spacer(Modifier.height(JarvisSpacing.space3))
            Chips(themes.map { it.label }, themes.indexOfFirst { it.id == state.themeId }) {
                cb.onThemeChangeById(themes[it].id)
            }
            Spacer(Modifier.height(JarvisSpacing.space3))
            Text("明暗", color = pal.onSurfaceVariant, style = JarvisType.labelBadge)
            Spacer(Modifier.height(JarvisSpacing.space2))
            Chips(listOf("跟随系统", "浅色", "深色"), modeChoices.indexOf(state.modeOverride)) {
                cb.onModeOverrideChange(modeChoices[it])
            }
            Meta("此刻生效：" + if (state.effectiveMode == ThemeMode.DARK) "暗夜纸面" else "纸面清单")
            GroupTitle("权限与命令")
            SettingSwitchRow("限制模式", state.restrictionMode, cb.onRestrictionChange, subtitle = "默认开启，控制高风险内容和动作")
            JarvisDivider()
            SettingSwitchRow("启用命令通道", state.commandChannelEnabled, cb.onCommandChange, subtitle = "首版默认只做预览，不直接执行")
            JarvisDivider()
            FieldLine("命令前缀", state.commandPrefix, cb.onPrefixChange)
            Text("root 模式", color = pal.onSurfaceVariant, style = JarvisType.labelBadge)
            Spacer(Modifier.height(JarvisSpacing.space2))
            Chips(state.rootModes.map { it.label }, state.rootModes.indexOfFirst { it.key == state.rootModeKey }) {
                cb.onRootModeRequest(state.rootModes[it])
            }
            Meta("当前：" + state.rootModes.firstOrNull { it.key == state.rootModeKey }?.label.orEmpty())
            GroupTitle("模型连接")
            FieldLine("Base URL", state.modelBaseUrl, cb.onModelBaseUrlChange)
            FieldLine("模型名", state.modelName, cb.onModelNameChange)
            FieldLine("API Key", state.modelApiKey, cb.onModelApiKeyChange, secret = true)
            FieldLine("模型回退链（每行一个，可选）", state.modelChain, cb.onModelChainChange, singleLine = false, minLines = 3, hint = "主模型失败时按顺序尝试")
            SettingSwitchRow("深度思考", state.deepThinkingEnabled, cb.onDeepThinkingChange, subtitle = "关闭时不发送 reasoning_effort；开启后由下方档位控制")
            JarvisDivider()
            Meta("思考深度：" + state.reasoningEffort)
            Spacer(Modifier.height(JarvisSpacing.space2))
            Chips(efforts, efforts.indexOf(state.reasoningEffort)) { cb.onReasoningEffortChange(efforts[it]) }
            FieldLine("深度思考模型（可选）", state.deepThinkingModel, cb.onDeepThinkingModelChange, hint = "留空则使用主模型")
            GroupTitle("识图模型")
            Meta("识图请求会使用单独的多模态配置；未填写时回退到主模型。")
            Spacer(Modifier.height(JarvisSpacing.space3))
            FieldLine("识图 Base URL", state.visionBaseUrl, cb.onVisionBaseUrlChange, hint = "留空继承主模型 Base URL")
            FieldLine("识图模型名", state.visionModel, cb.onVisionModelChange)
            FieldLine("识图 API Key", state.visionApiKey, cb.onVisionApiKeyChange, hint = "留空继承主模型 API Key", secret = true)
            GroupTitle("全局人格与世界书")
            FieldLine("全局提示词", state.mentorPrompt, cb.onMentorPromptChange, singleLine = false, minLines = 3)
            FieldLine("全局世界书", state.worldBook, cb.onWorldBookChange, singleLine = false, minLines = 3)
            GroupTitle("悬浮助手")
            SettingSwitchRow("启用悬浮球", state.floatingEnabled, cb.onFloatingEnabledChange, subtitle = "语音任务识别后直接交给 Jarvis 执行")
            JarvisDivider()
            if (state.floatingShapes.isNotEmpty()) {
                Spacer(Modifier.height(JarvisSpacing.space2))
                Text("悬浮球形状", color = pal.onSurfaceVariant, style = JarvisType.labelBadge)
                Spacer(Modifier.height(JarvisSpacing.space2))
                ChoiceChips(state.floatingShapes, state.floatingShapeKey, cb.onFloatingShapeChange)
            }
            Text("头像大小", color = pal.onSurfaceVariant, style = JarvisType.labelBadge)
            Spacer(Modifier.height(JarvisSpacing.space2))
            IntChips(state.avatarSizeChoices, state.avatarSizeDp, " dp") { cb.onAvatarSizeChange(it); cb.onFloatingSizeChange(it) }
            Spacer(Modifier.height(JarvisSpacing.space2))
            Text("暂时隐藏时长", color = pal.onSurfaceVariant, style = JarvisType.labelBadge)
            Spacer(Modifier.height(JarvisSpacing.space2))
            IntChips(state.hideSecondChoices, state.avatarHideSeconds, " 秒", cb.onAvatarHideSecondsChange)
            Spacer(Modifier.height(JarvisSpacing.space3))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space3)) {
                StatusBadge(if (state.overlayGranted) "悬浮窗已授权" else "悬浮窗未授权", if (state.overlayGranted) BadgeTone.ACTIVE else BadgeTone.WARNING)
                StatusBadge(if (state.accessibilityGranted) "无障碍已连接" else "无障碍未启用", if (state.accessibilityGranted) BadgeTone.ACTIVE else BadgeTone.WARNING)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextAction("悬浮窗权限", cb.onOverlayPermissionRequest)
                TextAction("无障碍设置", cb.onAccessibilitySettingsOpen)
            }
            GroupTitle("手势任务")
            if (state.gestureOptions.isNotEmpty()) {
                Text("双击手势", color = pal.onSurfaceVariant, style = JarvisType.labelBadge)
                Spacer(Modifier.height(JarvisSpacing.space2))
                ChoiceChips(state.gestureOptions, state.doubleGestureKey, cb.onDoubleGestureChange)
                Spacer(Modifier.height(JarvisSpacing.space2))
                Text("三击手势", color = pal.onSurfaceVariant, style = JarvisType.labelBadge)
                Spacer(Modifier.height(JarvisSpacing.space2))
                ChoiceChips(state.gestureOptions, state.tripleGestureKey, cb.onTripleGestureChange)
            }
            FieldLine("双击执行的任务", state.doubleTask, cb.onDoubleTaskChange)
            FieldLine("三击执行的任务", state.tripleTask, cb.onTripleTaskChange)
            if (state.savedAutomations.isNotEmpty()) {
                Text("选用已有自动化（绑定到双击手势）", color = pal.onSurfaceVariant, style = JarvisType.labelBadge)
                Spacer(Modifier.height(JarvisSpacing.space2))
                Chips(state.savedAutomations.map { it.title }, -1) {
                    cb.onSelectAutomationForGesture(state.doubleGestureKey, state.savedAutomations[it].id)
                }
            }
            GroupTitle("远程访问")
            SettingSwitchRow(
                "启用远程网页控制台", state.serverRunning,
                { on -> if (on) cb.onStartServer() else cb.onStopServer() },
                subtitle = "通过局域网或 Tailscale 访问网页控制台"
            )
            JarvisDivider()
            Text(
                if (state.pairingCode.isNotEmpty()) "一次性配对码：" + state.pairingCode else "还没有配对码，点击下方生成。",
                color = if (state.pairingCode.isNotEmpty()) pal.primary else pal.onSurfaceVariant,
                style = if (state.pairingCode.isNotEmpty()) JarvisType.codeInline else JarvisType.bodyMeta
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextAction("生成新配对码", cb.onNewPairingCode)
                TextAction("复制配对码", cb.onCopyPairingCode)
                TextAction("撤销配对", cb.onRevokePairing)
            }
            SettingSwitchRow("允许远程设备操作", state.remoteDeviceControl, cb.onRemoteDeviceControlChange, subtitle = "关闭时远程端只能对话和整理计划")
            JarvisDivider()
            if (state.remoteUrls.isNotEmpty()) {
                state.remoteUrls.forEach { Meta(it) }
            } else {
                Meta("暂未发现局域网或 Tailscale 地址；确认手机已连接网络后刷新。")
            }
            TextAction("刷新地址", cb.onRefreshRemoteUrls)
            GroupTitle("运行时工具")
            Meta("CMake " + (if (state.cmakeReady) "可用" else "缺失") + " · FFmpeg " + (if (state.ffmpegReady) "可用" else "缺失"))
            Spacer(Modifier.height(JarvisSpacing.space3))
            if (state.checkingTools) {
                JarvisLoading("正在检查工具路径")
            } else {
                state.runtimeTools.forEach { tool ->
                    InfoRow(tool.name, if (tool.installed) "已安装 · " + tool.version else "未安装") {
                        if (!tool.installed) TextAction("安装", { cb.onInstallTool(tool.name) })
                    }
                }
            }
            if (state.toolNotice.isNotEmpty()) NoticeLine(state.toolNotice)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextAction("检查工具", cb.onRefreshTools)
                TextAction("重新检查运行时", cb.onCheckTools)
            }
            GroupTitle("社区插件")
            if (state.pluginBusy) JarvisLoading("处理中…")
            state.plugins.forEach { plugin ->
                SettingSwitchRow(
                    plugin.name + " " + plugin.version, plugin.enabled, { cb.onTogglePlugin(plugin.id, it) },
                    subtitle = "工具：" + plugin.tools.joinToString().ifEmpty { "无" } + " · " + plugin.source
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    DangerActionLabel("卸载", { cb.onUninstallPlugin(plugin.id) })
                }
                JarvisDivider()
            }
            FieldLine("粘贴插件草稿（JSON）", pluginDraft, { pluginDraft = it }, singleLine = false, minLines = 4)
            if (state.pluginNotice.isNotEmpty()) NoticeLine(state.pluginNotice)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextAction("安装草稿", { cb.onInstallPlugin(pluginDraft) })
                TextAction("刷新插件", cb.onRefreshPlugins)
            }
            GroupTitle("操作记忆")
            Meta(state.memoryStatus.ifEmpty { "首次成功后学习，后续优先复用；失败时重新探索" })
            Spacer(Modifier.height(JarvisSpacing.space3))
            state.memoryEntries.forEach { entry ->
                InfoRow(
                    entry.title,
                    (if (entry.ready) "可复用" else "已失效，待重新学习") + " / 成功 " + entry.successes + " 失败 " + entry.failures
                ) { DangerActionLabel("删除", { cb.onDeleteMemoryEntry(entry.id) }) }
            }
            TextAction("刷新操作路径", cb.onRefreshMemory)
            GroupTitle("无障碍")
            SettingSwitchRow("打开 Jarvis 时自动连接", state.accessibilityAutoConnect, cb.onAccessibilityAutoConnectChange, subtitle = "需要 root 完全模式；关闭此项不会撤销已有授权")
            JarvisDivider()
            if (state.accessibilityStatus.isNotEmpty()) Meta(state.accessibilityStatus)
            GroupTitle("AI Classroom 配置同步")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextAction("导入", cb.onImportAiClassroom)
                TextAction("导出", cb.onExportAiClassroom)
            }
            if (state.importNotice.isNotEmpty()) NoticeLine(state.importNotice)
            if (state.exportNotice.isNotEmpty()) NoticeLine(state.exportNotice)
            Meta("API Key 只在本机配置中保存。")
            GroupTitle("OpenClaw 运行时")
            RuntimeBlock(state.bootstrapReport, state.runtimeMessage, state.configNotice, cb)
            GroupTitle("保存并应用")
            if (state.saving) JarvisLoading("应用中…")
            if (state.applyNotice.isNotEmpty()) {
                NoticeLine(state.applyNotice, tone = if (state.applyFailed) NoticeTone.ERROR else NoticeTone.SUCCESS)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextAction("保存并应用", cb.onApply)
                TextAction("保存并验证", cb.onApplyAndVerify)
            }
            Meta("编辑内容自动保存在本机；保存并应用可更新智能体。验证会发起一次简短的主模型请求。")
            Spacer(Modifier.height(JarvisSpacing.space7))
        }
    }

    @Composable
    private fun RuntimeBlock(
        report: BootstrapReport,
        message: String,
        configNotice: String,
        cb: SettingsUiContext.SettingsCallbacks
    ) {
        val pal = pal()
        Text("状态：" + runtimeStateLabel(report.state), color = pal.onSurface, style = JarvisType.titleItem)
        Spacer(Modifier.height(JarvisSpacing.space2))
        Meta("版本：" + report.runtimeVersion)
        Meta("插件目录：" + report.pluginDirectory)
        Meta((if (report.filesReady) "文件已检测到" else "文件缺失") + " · " + (if (report.rootAvailable) "root 已检测到" else "root 缺失") + " · " + (if (report.bridgeTokenReady) "令牌已生成" else "令牌缺失"))
        Spacer(Modifier.height(JarvisSpacing.space2))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextAction("重新检查", cb.onRefreshRuntime)
            TextAction("启动 Gateway", cb.onStartRuntime)
            TextAction("停止", cb.onStopRuntime)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextAction("运行时修复 / Termux 备用方案", cb.onRecovery)
        }
        if (configNotice.isNotEmpty()) NoticeLine(configNotice)
        if (message.isNotEmpty()) NoticeLine(message)
        if (report.message.isNotEmpty()) NoticeLine(report.message)
    }

    private fun runtimeStateLabel(state: RuntimeState): String = when (state) {
        RuntimeState.READY -> "就绪"
        RuntimeState.RUNNING -> "运行中"
        RuntimeState.DEGRADED -> "兼容模式"
        RuntimeState.NOT_CONFIGURED -> "未配置"
        RuntimeState.ERROR -> "配置错误"
    }
}
