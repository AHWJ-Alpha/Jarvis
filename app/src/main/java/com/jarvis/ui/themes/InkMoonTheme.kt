package com.jarvis.ui.themes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jarvis.app.R
import com.jarvis.app.runtime.BootstrapReport
import com.jarvis.app.runtime.ChatMessage
import com.jarvis.app.runtime.ChatMessageRole
import com.jarvis.app.runtime.ChatSession
import com.jarvis.app.runtime.RuntimeState
import com.jarvis.app.runtime.WorkspaceProfile
import com.jarvis.ui.components.EmptyStatePanel
import com.jarvis.ui.components.JarvisActionTone
import com.jarvis.ui.components.JarvisCardHeaderGap
import com.jarvis.ui.components.JarvisChipRow
import com.jarvis.ui.components.JarvisDivider
import com.jarvis.ui.components.JarvisGlyphLabel
import com.jarvis.ui.components.JarvisIconAction
import com.jarvis.ui.components.JarvisIconRow
import com.jarvis.ui.components.JarvisLoading
import com.jarvis.ui.components.JarvisSecretField
import com.jarvis.ui.components.ListRow
import com.jarvis.ui.components.NoticeLine
import com.jarvis.ui.components.NoticeTone
import com.jarvis.ui.components.SettingSwitchRow
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
import com.jarvis.ui.tokens.JarvisContrast
import com.jarvis.ui.tokens.JarvisShapes
import com.jarvis.ui.tokens.JarvisSize
import com.jarvis.ui.tokens.JarvisSpacing
import com.jarvis.ui.tokens.JarvisType

/** 卷轴横幅高度与空态最小高度，都是图形尺寸而非间距。 */
private val BannerHeight = 220.dp
private val EmptyStateHeight = 200.dp

/** 目录清单一次最多铺多少行；其余只报数量，避免 200 条把画廊拉成流水表。 */
private const val MaxVisibleFiles = 30

/** Pixiv 类型 key 的标记（AutomationKind.PIXIV_NEW_WORK）：画师字段只在这种类型下出现。 */
private const val PixivKindMarker = "PIXIV"

/** 明暗偏好的展示顺序与文案（持久化值由 ThemeModeOverride 决定，见 ThemePalette.kt）。 */
private val ModeChoices = listOf(ThemeModeOverride.FOLLOW_SYSTEM, ThemeModeOverride.LIGHT, ThemeModeOverride.DARK)
private val ModeLabels = listOf("跟随系统", "浅色", "深色")

/**
 * 亮色：宣纸画廊。22 个必填槽位逐项给定，不做任何明暗推导；
 * 额外补 primaryContainer / onPrimaryContainer 两个可选槽位，选中 chip 才有可读的底与字。
 */
private val XuanPaperPalette = ThemePalette(
    primary = Color(0xFF33363A), onPrimary = Color(0xFFF7F3E9),
    secondary = Color(0xFF5C5A54), onSecondary = Color(0xFFF7F3E9),
    background = Color(0xFFEFEADB), onBackground = Color(0xFF23221F),
    surface = Color(0xFFFBF8F1), onSurface = Color(0xFF23221F),
    surfaceContainerLow = Color(0xFFEDE8DC), surfaceContainer = Color(0xFFE7E1D3),
    surfaceContainerHigh = Color(0xFFDED7C7), onSurfaceVariant = Color(0xFF625D53),
    outline = Color(0xFFB0A692), borderSubtle = Color(0xFFC5BCA8),
    divider = Color(0xFFDCD5C6), error = Color(0xFF96382C),
    onError = Color(0xFFFDF2EE), errorContainer = Color(0xFFF0DCD5),
    onErrorContainer = Color(0xFF5B1F16), scrim = Color(0xFF1E1C18),
    accentSoft = Color(0xFFE3DAC7), glow = Color(0xFFC9BFA8),
    primaryContainer = Color(0xFFDCD3BE), onPrimaryContainer = Color(0xFF2A2C30)
)

/** 暗色：夜色山月。宣纸变墨纸，墨线换月银；层次仍靠底色明度差与发丝线，不加第二套卡片。 */
private val InkPaperPalette = ThemePalette(
    primary = Color(0xFFD8D2C2), onPrimary = Color(0xFF1A1B1F),
    secondary = Color(0xFF9A968B), onSecondary = Color(0xFF14151A),
    background = Color(0xFF0F1013), onBackground = Color(0xFFE8E4D9),
    surface = Color(0xFF191A1F), onSurface = Color(0xFFEDE9DE),
    surfaceContainerLow = Color(0xFF202126), surfaceContainer = Color(0xFF26272D),
    surfaceContainerHigh = Color(0xFF2E2F36), onSurfaceVariant = Color(0xFFA9A69B),
    outline = Color(0xFF5A5A52), borderSubtle = Color(0xFF3B3C43),
    divider = Color(0xFF2B2C32), error = Color(0xFFC87A6B),
    onError = Color(0xFF1C0F0B), errorContainer = Color(0xFF3A211B),
    onErrorContainer = Color(0xFFF0CFC6), scrim = Color(0xFF000000),
    accentSoft = Color(0xFF2B2C31), glow = Color(0xFF6E6A5E),
    primaryContainer = Color(0xFF3A3B33), onPrimaryContainer = Color(0xFFE8E4D9)
)

/**
 * 水墨山月：宣纸底 + 满幅平铺墨纹 + 居中画廊留白；全屏无卡片，层次只靠字号、字重与极细墨线。
 * 明暗两套色板都由本文件手写：亮色是宣纸画廊，暗色是同一套排版的「夜色山月」。
 */
internal object InkMoonTheme : JarvisThemeModule {
    override val id = "ink_moon"
    override val label = "水墨山月"
    override val story = "山月无声"
    override val description = "宣纸 · 水墨留白"
    override val mode = ThemeMode.LIGHT

    /** 身份色板：亮色宣纸画廊，也是未填一侧的回落目标。 */
    override val palette: ThemePalette = XuanPaperPalette
    override val lightPalette: ThemePalette = XuanPaperPalette
    override val darkPalette: ThemePalette = InkPaperPalette

    /**
     * 此刻生效的色板。明暗取自外壳提供的 [LocalJarvisMode]（已解析 mode_override + 系统值），
     * 不再从 MaterialTheme.colorScheme.background 逐个比色反查——反查把「配色来源」藏进颜色值里，
     * 而且每次重组都要重算。remember 让同一个 mode 复用同一个实例。
     */
    @Composable
    private fun inkPalette(): ThemePalette {
        val mode = LocalJarvisMode.current
        return remember(mode) { if (mode == ThemeMode.DARK) InkPaperPalette else XuanPaperPalette }
    }

    /** 当前是否渲染暗色（墨纸）。 */
    @Composable
    private fun inkIsDark(): Boolean = LocalJarvisMode.current == ThemeMode.DARK

    override val artwork = ThemeArtwork(
        banner = R.drawable.theme_ink_moon_banner,
        avatar = R.drawable.theme_ink_moon_avatar,
        texture = R.drawable.theme_ink_moon_texture
    )
    override val chrome = ChromeStyle.DRAWER
    override fun floatingPalette() = FloatingPaletteSpec(
        panel = palette.surfaceContainerHigh, title = palette.onSurface,
        body = palette.onSurfaceVariant, accent = palette.primary,
        muted = palette.onSurfaceVariant, bubbleRing = palette.borderSubtle
    )

    /* ---------------- 画廊基元 ---------------- */
    @Composable
    private fun Line(text: String, style: TextStyle, color: Color, align: TextAlign = TextAlign.Center) =
        Text(text, Modifier.fillMaxWidth(), color = color, style = style, textAlign = align)
    @Composable
    private fun Body(text: String, color: Color = Color.Unspecified) =
        Line(text, JarvisType.bodyBase, if (color == Color.Unspecified) inkPalette().onBackground else color)
    @Composable
    private fun Meta(text: String, color: Color = Color.Unspecified) =
        Line(text, JarvisType.bodyMeta, if (color == Color.Unspecified) inkPalette().onSurfaceVariant else color)
    @Composable
    private fun GalleryPage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
        GalleryRoot(modifier) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).jarvisScreenPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { content() }
        }
    }

    /**
     * 画廊根：纸底 + 满幅平铺的墨纹（ImageShader 真平铺，不拉伸、不叠 alpha）。
     *
     * 墨纹本身是宣纸图像（亮底），铺到暗色墨纸上会把整页拖回亮色——所以纹样只在亮色出现，
     * 暗色留给纯墨纸，层次改由 surfaceContainerHigh 与 surface 的明度差承担。
     */
    @Composable
    private fun GalleryRoot(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
        val palette = inkPalette()
        val dark = inkIsDark()
        Box(modifier.fillMaxSize().background(palette.background)) {
            if (!dark) {
                artwork.texture?.let { texture ->
                    val bitmap = ImageBitmap.imageResource(texture)
                    val brush = remember(bitmap) { ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated)) }
                    Box(Modifier.matchParentSize().background(brush))
                }
            }
            content()
        }
    }

    /** 居中页题：displaySection + 一行说明 + 细线；层间只走 rowGap / cardGap，不再留 20/24dp 大字面空隙。 */
    @Composable
    private fun GalleryTitle(title: String, subtitle: String) {
        val palette = inkPalette()
        Line(title, JarvisType.displaySection, palette.onBackground)
        Spacer(Modifier.height(JarvisSpacing.rowGap))
        Body(subtitle, palette.onSurfaceVariant)
        Spacer(Modifier.height(JarvisSpacing.cardGap))
        JarvisDivider()
    }

    /** 区块题：区块之间 sectionGap，题与内容之间统一 JarvisCardHeaderGap()（12 -> 8dp）。 */
    @Composable
    private fun GallerySection(title: String) {
        val palette = inkPalette()
        Spacer(Modifier.height(JarvisSpacing.sectionGap))
        Line(title, JarvisType.titleBlock, palette.primary)
        JarvisCardHeaderGap()
    }

    /**
     * 状态型标签只给语气：就绪取主色、缺失取危险色。
     * 文字型「已连接 / 未启用」一律删掉，语义交给图标 + 颜色（JarvisGlyphLabel 只留 2~4 字）。
     */
    private fun inkTone(ready: Boolean): JarvisActionTone =
        if (ready) JarvisActionTone.ACCENT else JarvisActionTone.DANGER

    /** 输入框两类手段之一（路线①）：发丝线勾边 + surfaceContainerHigh 底，不加粗描边。 */
    private fun Modifier.fieldBox(corner: androidx.compose.ui.graphics.Shape, fill: Color, border: Color): Modifier =
        clip(corner).background(fill).border(JarvisContrast.hairlineBorder(border), corner)

    /** 「左标题右值 + 下细线」清单行，右值就地可编辑。 */
    @Composable
    private fun ValueRow(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
        hint: String = "",
        secret: Boolean = false
    ) {
        val palette = inkPalette()
        if (secret) {
            JarvisSecretField(
                label = label, value = value, onValueChange = onValueChange, hint = hint,
                labelStyle = JarvisType.bodyBase, valueStyle = JarvisType.bodyBase,
                labelColor = palette.onSurfaceVariant, valueColor = palette.onSurface,
                containerColor = palette.surfaceContainerHigh, accentColor = palette.primary,
                showDivider = true)
            return
        }
        Column(Modifier.fillMaxWidth().padding(vertical = JarvisSpacing.space2)) {
            Row(Modifier.fillMaxWidth().heightIn(min = JarvisSize.rowMinHeight), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), color = palette.onSurfaceVariant, style = JarvisType.bodyBase)
                Spacer(Modifier.width(JarvisSpacing.space4))
                BasicTextField(
                    value = value, onValueChange = onValueChange,
                    modifier = Modifier.weight(1.5f)
                        .fieldBox(JarvisShapes.sm, palette.surfaceContainerHigh, palette.borderSubtle)
                        .padding(horizontal = JarvisSpacing.space4, vertical = JarvisSpacing.space3),
                    singleLine = true,
                    textStyle = JarvisType.bodyBase.copy(color = palette.onSurface, textAlign = TextAlign.End),
                    cursorBrush = SolidColor(palette.primary),
                    visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth()) {
                            if (value.isEmpty() && hint.isNotEmpty()) Line(hint, JarvisType.bodyMeta, palette.onSurfaceVariant, TextAlign.End)
                            inner()
                        }
                    }
                )
            }
            Spacer(Modifier.height(JarvisSpacing.space2))
            JarvisDivider()
        }
    }

    /** 多行输入块：小字重标签在上、方框正文在下，细线收口（去掉粗边框后靠底色与字号拉层次）。 */
    @Composable
    private fun BlockField(label: String, value: String, onValueChange: (String) -> Unit, minLines: Int = 3) {
        val palette = inkPalette()
        Column(Modifier.fillMaxWidth().padding(vertical = JarvisSpacing.space2)) {
            Text(label, style = JarvisType.labelBadge, color = palette.onSurfaceVariant)
            Spacer(Modifier.height(JarvisSpacing.space2))
            BasicTextField(
                value, onValueChange,
                modifier = Modifier.fillMaxWidth()
                    .fieldBox(JarvisShapes.sm, palette.surfaceContainerHigh, palette.borderSubtle)
                    .padding(horizontal = JarvisSpacing.space4, vertical = JarvisSpacing.space3)
                    .heightIn(min = JarvisSize.minTouch),
                minLines = minLines,
                textStyle = JarvisType.bodyBase.copy(color = palette.onSurface),
                cursorBrush = SolidColor(palette.primary)
            )
            Spacer(Modifier.height(JarvisSpacing.space2))
            JarvisDivider()
        }
    }

    /** 布尔项：直接复用 SettingSwitchRow，再补一条下细线，保持清单收口。 */
    @Composable
    private fun ValueSwitchRow(
        label: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        subtitle: String = ""
    ) {
        SettingSwitchRow(label, checked, onCheckedChange, subtitle = subtitle.ifEmpty { null })
        JarvisDivider()
    }

    /** 只读信息行：复用 ListRow（标题/说明/右侧动作），下细线由本主题补。 */
    @Composable
    private fun InfoRow(title: String, detail: String, action: @Composable (() -> Unit)? = null) {
        ListRow(title = title, subtitle = detail.ifEmpty { null }, trailing = action)
        JarvisDivider()
    }
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

    /* ---------------- 对话 ---------------- */
    @Composable
    override fun ChatScreen(ctx: ChatUiContext) {
        val palette = inkPalette()
        val state = ctx.state
        val cb = ctx.callbacks
        val efforts = state.reasoningEfforts.ifEmpty { listOf("off", "minimal", "low", "medium", "high", "max") }
        val listState = rememberLazyListState()
        LaunchedEffect(state.conversationId, state.messages.size) {
            if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex + 1)
        }
        GalleryRoot(Modifier.padding(state.contentPadding)) {
            Column(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(JarvisSpacing.pageHorizontal, JarvisSpacing.space4),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Image(
                            painterResource(id = artwork.banner), contentDescription = null,
                            contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd,
                            modifier = Modifier.fillMaxWidth().heightIn(min = BannerHeight)
                        )
                        Spacer(Modifier.height(JarvisSpacing.sectionGap))
                        GalleryTitle("对话", "在消息下方编辑、撤销或创建分支")
                    }
                    if (state.messages.isEmpty()) {
                        item {
                            Spacer(Modifier.height(JarvisSpacing.sectionGap))
                            EmptyStatePanel("从一个小任务开始", subtitle = "交给 Jarvis，逐步完成。", icon = Icons.Default.Edit, modifier = Modifier.fillMaxWidth().heightIn(min = EmptyStateHeight))
                        }
                    } else {
                        itemsIndexed(state.messages, key = { _, message -> message.id }) { index, message ->
                            MessageBlock(message, index == state.messages.lastIndex, state, cb)
                        }
                    }
                    if (state.editingLatest) item { NoticeLine("正在修改上一条；发送后才会替换原对话轮次。") }
                    if (state.sending) item { JarvisLoading("正在请求模型…") }
                    if (state.speechNotice.isNotEmpty()) item { NoticeLine(state.speechNotice) }
                }
                Composer(state, cb, efforts)
            }
        }
    }
    @Composable
    private fun MessageBlock(
        message: ChatMessage,
        isLatest: Boolean,
        state: ChatUiContext.ChatState,
        cb: ChatUiContext.ChatCallbacks
    ) {
        val palette = inkPalette()
        Column(
            Modifier.fillMaxWidth().padding(vertical = JarvisSpacing.space6),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (message.role == ChatMessageRole.USER) "你" else "Jarvis", color = palette.primary, style = JarvisType.labelBadge)
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            Line(message.content, JarvisType.bodyBase, palette.onBackground)
            if (isLatest) {
                Spacer(Modifier.height(JarvisSpacing.rowGap))
                JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                    if (state.canEditLatest) JarvisIconAction(Icons.Default.Edit, "编辑这条消息", cb.onEditLatest)
                    if (state.canUndoLatest) JarvisIconAction(Icons.Default.Undo, "撤销这一轮", cb.onUndoLatest)
                    JarvisIconAction(Icons.Default.CallSplit, "从这里创建分支", { cb.onBranch(message.id) })
                }
            }
            Spacer(Modifier.height(JarvisSpacing.cardGap))
            JarvisDivider()
        }
    }
    /** 输入区：方框输入 + 44dp 圆形发送键；墨线只用发丝线，不加第二层卡片。 */
    @Composable
    private fun Composer(state: ChatUiContext.ChatState, cb: ChatUiContext.ChatCallbacks, efforts: List<String>) {
        val palette = inkPalette()
        val canSend = state.draft.isNotBlank() && !state.sending
        Column(
            Modifier.fillMaxWidth().padding(horizontal = JarvisSpacing.pageHorizontal)
                .padding(bottom = JarvisSpacing.space4)
        ) {
            JarvisDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                BasicTextField(
                    value = state.draft,
                    onValueChange = cb.onDraftChange,
                    modifier = Modifier.weight(1f)
                        .fieldBox(JarvisShapes.sm, palette.surfaceContainerHigh, palette.borderSubtle)
                        .padding(horizontal = JarvisSpacing.space4, vertical = JarvisSpacing.space3)
                        .heightIn(min = JarvisSize.minTouch),
                    maxLines = 5,
                    textStyle = JarvisType.bodyBase.copy(color = palette.onSurface),
                    cursorBrush = SolidColor(palette.primary),
                    decorationBox = { inner ->
                        Box {
                            if (state.draft.isEmpty()) Text("给 Jarvis 发消息，或描述一个任务…", color = palette.onSurfaceVariant, style = JarvisType.bodyBase)
                            inner()
                        }
                    }
                )
                Spacer(Modifier.width(JarvisSpacing.space2))
                IconButton(onClick = cb.onVoiceInput, enabled = !state.voiceListening, modifier = Modifier.size(JarvisSize.minTouch)) {
                    Icon(Icons.Default.Mic, tint = palette.onSurfaceVariant, contentDescription = if (state.voiceListening) "正在识别语音" else "语音转文字")
                }
                IconButton(onClick = cb.onOpenTools, modifier = Modifier.size(JarvisSize.minTouch)) {
                    Icon(Icons.Default.Build, contentDescription = "打开工具箱", tint = palette.onSurfaceVariant)
                }
                Spacer(Modifier.width(JarvisSpacing.space2))
                Box(
                    Modifier
                        .size(JarvisSize.sendButton)
                        .clip(JarvisShapes.pill)
                        .background(if (canSend) palette.primary else palette.surfaceContainerHigh)
                        .clickable(enabled = canSend) { cb.onSend(state.draft) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送消息",
                        tint = if (canSend) palette.onPrimary else palette.onSurfaceVariant,
                        modifier = Modifier.size(JarvisSize.toolbarIcon)
                    )
                }
            }
            JarvisDivider()
            Chips(efforts, efforts.indexOf(state.reasoningEffort)) { cb.onReasoningEffortChange(efforts[it]) }
            JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight), Arrangement.End) {
                JarvisIconAction(Icons.Default.AddComment, "新建对话", cb.onNewConversation)
                JarvisIconAction(Icons.Default.CallSplit, "创建分支", { cb.onBranch(null) })
            }
        }
    }

    /* ---------------- 任务 ---------------- */
    @Composable
    override fun TasksScreen(ctx: TasksUiContext) {
        val palette = inkPalette()
        val state = ctx.state
        val cb = ctx.callbacks
        GalleryPage(Modifier.padding(state.contentPadding)) {
            GalleryTitle("自动化", "通过对话创建，收到通知后按规则执行一次。")
            Spacer(Modifier.height(JarvisSpacing.cardGap))
            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                JarvisGlyphLabel(Icons.Default.Notifications, "通知", tone = inkTone(state.notificationAccess))
                JarvisGlyphLabel(Icons.Default.Accessibility, "无障碍", tone = inkTone(state.accessibilityGranted))
                JarvisGlyphLabel(Icons.Default.Build, "运行时", tone = inkTone(state.runtimeReady))
            }
            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                JarvisIconAction(Icons.Default.Add, "新建规则", cb.onCreateRuleStart, tone = JarvisActionTone.ACCENT)
                JarvisIconAction(Icons.Default.Notifications, "授权通知访问", cb.onRequestNotificationAccess)
                JarvisIconAction(Icons.Default.Accessibility, "开启无障碍服务", cb.onRequestAccessibility)
                JarvisIconAction(Icons.Default.Refresh, "刷新自动化规则", cb.onRefresh)
            }
            if (state.notice.isNotEmpty()) NoticeLine(state.notice)
            if (state.creatingRule) RuleDraftForm(state, cb)
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            JarvisDivider()
            // 建单态只画草案卡：空态 CTA 与之互斥，否则同屏出现两个「新建规则」入口。
            if (state.rules.isEmpty() && !state.creatingRule) {
                Spacer(Modifier.height(JarvisSpacing.sectionGap))
                EmptyStatePanel(
                    "还没有自动化",
                    icon = Icons.Default.Notifications,
                    modifier = Modifier.fillMaxWidth().heightIn(min = EmptyStateHeight),
                    subtitle = "在对话中输入“当哔哩哔哩上有人回复我就立刻回复”或“当 Pixiv 画师发布新作品就立刻下载”。",
                    action = { JarvisIconAction(Icons.Default.Add, "新建规则", cb.onCreateRuleStart, tone = JarvisActionTone.ACCENT) }
                )
            } else {
                state.rules.forEach { rule -> RuleBlock(rule, cb) }
            }
            if (state.recentEvents.isNotEmpty()) {
                GallerySection("最近执行")
                state.recentEvents.take(5).forEach { event ->
                    Line(event.title, JarvisType.titleItem, if (event.ok) palette.primary else palette.error)
                    Spacer(Modifier.height(JarvisSpacing.space2))
                    Meta(event.message)
                    Spacer(Modifier.height(JarvisSpacing.rowGap))
                    JarvisDivider()
                }
            }
            Spacer(Modifier.height(JarvisSpacing.sectionGap))
        }
        val editing = state.rules.firstOrNull { it.id == state.editingRuleId }
        if (editing != null) RuleConfigDialog(editing, state, cb)
    }

    /** 任务页「新建自动化」表单：标题 / 类型 / 触发要求 / 画师（仅 Pixiv）/ 错误行。 */
    @Composable
    private fun RuleDraftForm(state: TasksUiContext.TasksState, cb: TasksUiContext.TasksCallbacks) {
        val palette = inkPalette()
        val pixiv = state.ruleDraftKindKey.contains(PixivKindMarker, ignoreCase = true)
        Column(Modifier.fillMaxWidth()) {
            GallerySection("新建规则")
            BlockField("规则标题", state.ruleDraftTitle, cb.onCreateRuleTitleChange, minLines = 1)
            if (state.ruleDraftKindOptions.isNotEmpty()) {
                Meta("规则类型")
                Spacer(Modifier.height(JarvisSpacing.space2))
                ChoiceChips(state.ruleDraftKindOptions, state.ruleDraftKindKey, cb.onCreateRuleKindChange)
            }
            BlockField("触发要求（看到什么通知时执行）", state.ruleDraftPrompt, cb.onCreateRulePromptChange)
            if (pixiv) {
                BlockField("Pixiv 画师名称或用户 ID", state.ruleDraftArtist, cb.onCreateRuleArtistChange, minLines = 1)
                Meta("画师留空时规则不能启用；启用前还需目标应用通知访问与无障碍服务。")
                Spacer(Modifier.height(JarvisSpacing.space2))
            }
            if (state.ruleDraftError.isNotEmpty()) {
                NoticeLine(state.ruleDraftError, tone = NoticeTone.ERROR)
                Spacer(Modifier.height(JarvisSpacing.space2))
            }
            JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight), Arrangement.Center) {
                JarvisIconAction(Icons.Default.Check, "保存规则", cb.onCreateRuleSubmit, tone = JarvisActionTone.ACCENT)
                JarvisIconAction(Icons.Default.Close, "取消新建", cb.onCreateRuleCancel)
            }
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            JarvisDivider()
        }
    }
    @Composable
    private fun RuleBlock(rule: AutomationRuleView, cb: TasksUiContext.TasksCallbacks) {
        val palette = inkPalette()
        Spacer(Modifier.height(JarvisSpacing.sectionGap))
        Line(rule.title, JarvisType.titleScreen, if (rule.enabled) palette.primary else palette.onBackground)
        JarvisCardHeaderGap()
        Row(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight), verticalAlignment = Alignment.CenterVertically) {
            JarvisGlyphLabel(
                if (rule.enabled) Icons.Default.PlayArrow else Icons.Default.Pause,
                if (rule.enabled) "启用中" else "已暂停",
                tone = if (rule.enabled) JarvisActionTone.ACCENT else JarvisActionTone.NEUTRAL
            )
            Spacer(Modifier.width(JarvisSpacing.space3))
            Text(rule.kindLabel, color = palette.onSurfaceVariant, style = JarvisType.bodyMeta)
        }
        if (rule.lastResult.isNotEmpty()) {
            Spacer(Modifier.height(JarvisSpacing.space2))
            Meta("最近结果：" + rule.lastResult)
        }
        rule.readiness?.let {
            Spacer(Modifier.height(JarvisSpacing.space2))
            Line("启用前：" + it, JarvisType.bodyMeta, palette.error)
        }
        Spacer(Modifier.height(JarvisSpacing.rowGap))
        JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
            JarvisIconAction(
                if (rule.enabled) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (rule.enabled) "暂停规则" else "启用规则",
                { cb.onToggleRule(rule.id, !rule.enabled) },
                tone = JarvisActionTone.ACCENT
            )
            JarvisIconAction(Icons.Default.Settings, "配置规则", { cb.onConfigureRule(rule.id) })
            JarvisIconAction(Icons.Default.Delete, "删除规则", { cb.onDeleteRule(rule.id) }, tone = JarvisActionTone.DANGER)
        }
        Spacer(Modifier.height(JarvisSpacing.rowGap))
        JarvisDivider()
    }
    @Composable
    private fun RuleConfigDialog(
        rule: AutomationRuleView,
        state: TasksUiContext.TasksState,
        cb: TasksUiContext.TasksCallbacks
    ) {
        val palette = inkPalette()
        AlertDialog(
            onDismissRequest = cb.onCancelRuleConfig, containerColor = palette.surface,
            titleContentColor = palette.onSurface, textContentColor = palette.onSurfaceVariant,
            title = { Text("配置自动化", style = JarvisType.titleBlock) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (rule.sourcePrompt.isNotEmpty()) Meta("原始要求：" + rule.sourcePrompt)
                    if (state.artistDraft.isEmpty() && rule.artist.isNotEmpty()) Meta("当前画师：" + rule.artist)
                    BlockField("Pixiv 画师名称或用户 ID", state.artistDraft, cb.onArtistDraftChange, minLines = 1)
                    BlockField("回复风格要求", state.replyDraft, cb.onReplyDraftChange)
                    if (state.replyDraft.isEmpty() && rule.replyInstruction.isNotEmpty()) Meta("当前要求：" + rule.replyInstruction)
                    Meta("启用前还需要目标应用通知访问和 Jarvis 无障碍服务。")
                }
            },
            confirmButton = {
                TextButton(onClick = { cb.onSaveRuleConfig(rule.id, state.artistDraft, state.replyDraft) }, modifier = Modifier.heightIn(min = JarvisSize.minTouch)) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = cb.onCancelRuleConfig, modifier = Modifier.heightIn(min = JarvisSize.minTouch)) { Text("取消") }
            }
        )
    }

    /* ---------------- 工作区 ---------------- */
    @Composable
    override fun WorkspaceScreen(ctx: WorkspaceUiContext) {
        val palette = inkPalette()
        val state = ctx.state
        val cb = ctx.callbacks
        var editing by remember { mutableStateOf<WorkspaceProfile?>(null) }
        var creating by remember { mutableStateOf(false) }
        var renaming by remember { mutableStateOf<ChatSession?>(null) }
        var renameDraft by remember { mutableStateOf("") }
        var moving by remember { mutableStateOf<ChatSession?>(null) }
        val expandedFiles = remember { mutableStateMapOf<String, Boolean>() }
        GalleryPage(Modifier.padding(state.contentPadding)) {
            GalleryTitle("工作区", "为每个工作区选择共同设定，或单独编辑人格与世界书。")
            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                JarvisIconAction(
                    Icons.Default.Add, "新建工作区",
                    { creating = true; editing = WorkspaceProfile(name = "") },
                    tone = JarvisActionTone.ACCENT
                )
            }
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            ValueSwitchRow(
                "显示已归档",
                state.showArchived,
                cb.onToggleShowArchived,
                "已归档工作区 " + state.archivedWorkspaces.size + " 个 · 归档不删档案与会话，随时可还原"
            )
            if (!state.showArchived && state.archivedWorkspaces.isNotEmpty()) {
                Meta("还有 " + state.archivedWorkspaces.size + " 个已归档工作区被隐藏，打开上面的开关查看。")
                Spacer(Modifier.height(JarvisSpacing.space2))
            }
            if (state.selectedWorkspace.isNotEmpty()) {
                Spacer(Modifier.height(JarvisSpacing.cardGap))
                Meta("当前：" + state.selectedWorkspace, palette.primary)
            }
            state.workspaces.forEach { profile ->
                val selected = profile.name == state.selectedWorkspace
                val sessions = state.conversations.filter { it.workspace == profile.name }
                Spacer(Modifier.height(JarvisSpacing.sectionGap))
                Row(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile.name,
                        modifier = Modifier.weight(1f),
                        color = if (selected) palette.primary else palette.onBackground,
                        style = JarvisType.titleScreen
                    )
                    if (profile.archived) JarvisGlyphLabel(Icons.Default.Archive, "已归档")
                }
                JarvisCardHeaderGap()
                Meta(
                    (if (profile.useGlobalPersona) "共同设定" else "自定义设定") +
                        " · " + (state.conversationCountByWorkspace[profile.name] ?: sessions.size) + " 个会话"
                )
                JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                    JarvisIconAction(
                        Icons.Default.Login,
                        if (selected) "当前工作区" else "进入该工作区",
                        { cb.onSelect(profile.name) },
                        enabled = !selected,
                        tone = JarvisActionTone.ACCENT
                    )
                    JarvisIconAction(Icons.Default.Edit, "编辑工作区", { creating = false; editing = profile })
                    if (profile.archived) {
                        JarvisIconAction(Icons.Default.Restore, "还原工作区", { cb.onUnarchive(profile.name) }, tone = JarvisActionTone.ACCENT)
                    } else if (profile.name != WorkspaceProfile.DEFAULT_NAME) {
                        JarvisIconAction(Icons.Default.Archive, "归档工作区", { cb.onArchive(profile.name) })
                    }
                    if (profile.name != WorkspaceProfile.DEFAULT_NAME) {
                        JarvisIconAction(Icons.Default.Delete, "删除工作区", { cb.onDelete(profile.name) }, tone = JarvisActionTone.DANGER)
                    }
                }
                if (selected) {
                    Spacer(Modifier.height(JarvisSpacing.rowGap))
                    Meta(
                        "目录：" + profile.dirLabel.ifBlank { "未绑定" } +
                            (when {
                                !profile.hasDirectory -> ""
                                state.dirReady -> " · 读权限有效"
                                else -> " · 读权限失效，请重新绑定"
                            })
                    )
                    JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                        JarvisIconAction(
                            if (profile.hasDirectory) Icons.Default.FolderOpen else Icons.Default.CreateNewFolder,
                            if (profile.hasDirectory) "更换目录" else "绑定目录",
                            { cb.onPickDirectory(profile.name) },
                            tone = if (profile.hasDirectory) JarvisActionTone.NEUTRAL else JarvisActionTone.ACCENT
                        )
                        if (profile.hasDirectory) {
                            JarvisIconAction(Icons.Default.FolderOff, "解绑目录", { cb.onClearDirectory(profile.name) })
                        }
                    }
                    if (state.filesLoading) JarvisLoading("正在读取目录…")
                    if (state.filesNotice.isNotEmpty()) NoticeLine(state.filesNotice)
                    if (profile.hasDirectory) {
                        Spacer(Modifier.height(JarvisSpacing.space2))
                        Meta("目录内容（只读）· " + state.files.size + " 项")
                        Spacer(Modifier.height(JarvisSpacing.space2))
                        if (state.files.isEmpty() && !state.filesLoading && state.filesNotice.isEmpty()) {
                            Meta("目录为空，或还没有读到内容。")
                        }
                        state.files.take(MaxVisibleFiles).forEach { file ->
                            val key = profile.name + "/" + file.name
                            FileRow(
                                file = file,
                                open = expandedFiles[key] == true,
                                onToggle = { expandedFiles[key] = expandedFiles[key] != true }
                            )
                        }
                        if (state.files.size > MaxVisibleFiles) {
                            Meta("另有 " + (state.files.size - MaxVisibleFiles) + " 项未在此展开显示。")
                        }
                    }
                    Spacer(Modifier.height(JarvisSpacing.sectionGap))
                    Line("本工作区对话", JarvisType.titleBlock, palette.primary)
                    Spacer(Modifier.height(JarvisSpacing.rowGap))
                    if (sessions.isEmpty()) {
                        Meta(if (state.showArchived) "还没有对话。" else "没有未归档的对话；已归档的在开关打开后出现。")
                    }
                    sessions.forEach { session ->
                        val archived = session.id in state.archivedConversationIds
                        ConversationRow(
                            session = session,
                            archived = archived,
                            onRename = { renameDraft = session.title; renaming = session },
                            onMove = { moving = session },
                            onDelete = { cb.onDeleteConversation(session.id) },
                            onArchiveToggle = {
                                if (archived) cb.onUnarchiveConversation(session.id) else cb.onArchiveConversation(session.id)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(JarvisSpacing.cardGap))
                JarvisDivider()
            }
            GallerySection("全局人格与世界书")
            Body(state.globalPersona.ifBlank { "—" })
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            Meta(state.globalWorldBook.ifBlank { "—" })
            Spacer(Modifier.height(JarvisSpacing.cardGap))
            JarvisDivider()
        }
        editing?.let { profile ->
            WorkspaceEditor(
                profile = profile,
                isNew = creating,
                state = state,
                onDismiss = { editing = null },
                onSave = { updated ->
                    cb.onSave(updated, if (creating) null else profile.name)
                    editing = null
                }
            )
        }
        renaming?.let { session ->
            AlertDialog(
                onDismissRequest = { renaming = null }, containerColor = palette.surface,
                titleContentColor = palette.onSurface, textContentColor = palette.onSurfaceVariant,
                title = { Text("重命名对话", style = JarvisType.titleBlock) },
                text = {
                    Column {
                        BlockField("对话标题", renameDraft, { renameDraft = it }, minLines = 1)
                        Meta("当前：" + session.title)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { cb.onRenameConversation(session.id, renameDraft); renaming = null },
                        modifier = Modifier.heightIn(min = JarvisSize.minTouch)
                    ) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { renaming = null }, modifier = Modifier.heightIn(min = JarvisSize.minTouch)) { Text("取消") }
                }
            )
        }
        moving?.let { session ->
            val targets = state.workspaces.map { it.name }.filter { it != session.workspace }
            AlertDialog(
                onDismissRequest = { moving = null }, containerColor = palette.surface,
                titleContentColor = palette.onSurface, textContentColor = palette.onSurfaceVariant,
                title = { Text("移动对话", style = JarvisType.titleBlock) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Meta("对话：" + session.title + " · 当前在 " + session.workspace)
                        if (targets.isEmpty()) {
                            Meta("还没有其他工作区，先新建一个再移动。")
                        }
                        targets.forEach { target ->
                            InfoRow(session.title, "移到 " + target) {
                                JarvisIconAction(
                                    Icons.Default.DriveFileMove,
                                    "移动到 " + target,
                                    { cb.onMoveConversation(session.id, target); moving = null },
                                    tone = JarvisActionTone.ACCENT
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { moving = null }, modifier = Modifier.heightIn(min = JarvisSize.minTouch)) { Text("取消") }
                }
            )
        }
    }

    /** 目录里的一行（只读）；previewable 的整行可点展开预览文本，再次点击收起。 */
    @Composable
    private fun FileRow(file: WorkspaceFileView, open: Boolean, onToggle: () -> Unit) {
        val palette = inkPalette()
        Column(Modifier.fillMaxWidth()) {
            ListRow(
                title = file.name + (if (file.isDirectory) "/" else ""),
                subtitle = if (file.isDirectory) {
                    "文件夹"
                } else {
                    readableSize(file.sizeBytes) + (if (file.previewable) " · 可预览" else "")
                },
                trailing = if (file.previewable) {
                    {
                        JarvisIconAction(
                            if (open) Icons.Default.ExpandLess else Icons.Default.Visibility,
                            if (open) "收起预览" else "预览文件内容",
                            onToggle
                        )
                    }
                } else {
                    null
                },
                onClick = if (file.previewable) onToggle else null
            )
            if (open) {
                Text(
                    file.previewText.ifBlank { "没有可显示的内容。" },
                    style = JarvisType.codeInline,
                    color = palette.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                        .clip(JarvisShapes.sm)
                        .background(palette.surfaceContainerHigh)
                        .padding(JarvisSpacing.space4)
                )
            }
            JarvisDivider()
        }
    }

    /** 对话行：标题在上，改名/移动/归档（或还原）/删除四个入口在下，行高由 ListRow 兜住 52dp。 */
    @Composable
    private fun ConversationRow(
        session: ChatSession,
        archived: Boolean,
        onRename: () -> Unit,
        onMove: () -> Unit,
        onDelete: () -> Unit,
        onArchiveToggle: () -> Unit
    ) {
        Column(Modifier.fillMaxWidth()) {
            ListRow(
                title = session.title,
                subtitle = (if (archived) "已归档 · " else "") + session.workspace
            )
            JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight), Arrangement.End) {
                JarvisIconAction(Icons.Default.DriveFileRenameOutline, "重命名对话", onRename)
                JarvisIconAction(Icons.Default.DriveFileMove, "移动到其他工作区", onMove)
                JarvisIconAction(
                    if (archived) Icons.Default.Restore else Icons.Default.Archive,
                    if (archived) "还原对话" else "归档对话",
                    onArchiveToggle
                )
                JarvisIconAction(Icons.Default.Delete, "删除对话", onDelete, tone = JarvisActionTone.DANGER)
            }
            JarvisDivider()
        }
    }

    /** 文件大小文案；-1 表示提供方没给（不是 0 字节）。 */
    private fun readableSize(bytes: Long): String = when {
        bytes < 0L -> "大小未知"
        bytes < 1024L -> bytes.toString() + " B"
        bytes < 1024L * 1024L -> (bytes / 1024L).toString() + " KB"
        else -> (bytes / (1024L * 1024L)).toString() + " MB"
    }

    @Composable
    private fun WorkspaceEditor(
        profile: WorkspaceProfile,
        isNew: Boolean,
        state: WorkspaceUiContext.WorkspaceState,
        onDismiss: () -> Unit,
        onSave: (WorkspaceProfile) -> Unit
    ) {
        val palette = inkPalette()
        var name by remember(profile) { mutableStateOf(profile.name) }
        var shared by remember(profile) { mutableStateOf(profile.useGlobalPersona) }
        var persona by remember(profile) { mutableStateOf(profile.mentorPrompt) }
        var worldBook by remember(profile) { mutableStateOf(profile.worldBook) }
        AlertDialog(
            onDismissRequest = onDismiss, containerColor = palette.surface,
            titleContentColor = palette.onSurface, textContentColor = palette.onSurfaceVariant,
            title = { Text(if (isNew) "新建工作区" else "编辑工作区", style = JarvisType.titleBlock) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    BlockField("工作区名称", name, { name = it }, minLines = 1)
                    ValueSwitchRow("使用共同设定", shared, { shared = it }, "此工作区会跟随设置中的全局人格与世界书。")
                    BlockField("自定义人格 / 提示词", persona, { persona = it })
                    BlockField("自定义世界书", worldBook, { worldBook = it })
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
                TextButton(
                    onClick = {
                        onSave(
                            profile.copy(
                                name = name.trim(), useGlobalPersona = shared,
                                mentorPrompt = persona, worldBook = worldBook
                            )
                        )
                    },
                    modifier = Modifier.heightIn(min = JarvisSize.minTouch)
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = JarvisSize.minTouch)) { Text("取消") }
            }
        )
    }

    /* ---------------- 设置 ---------------- */
    @Composable
    override fun SettingsScreen(ctx: SettingsUiContext) {
        val palette = inkPalette()
        val state = ctx.state
        val cb = ctx.callbacks
        val efforts = state.reasoningEfforts.ifEmpty { listOf("off", "minimal", "low", "medium", "high", "max") }
        val themes = state.themeOptions
        val currentTheme = themes.firstOrNull { it.id == state.themeId } ?: themes.firstOrNull()
        var pluginDraft by remember { mutableStateOf("") }
        GalleryPage(Modifier.padding(state.contentPadding)) {
            GalleryTitle("设置", "Jarvis " + state.versionName + " · 模型、权限与设备连接")
            GallerySection("风格与外观")
            Meta((currentTheme?.story ?: state.theme.story) + " · " + (currentTheme?.description ?: state.theme.description))
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            Chips(themes.map { it.label }, themes.indexOfFirst { it.id == state.themeId }) { cb.onThemeChangeById(themes[it].id) }
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            Meta("明暗 · 此刻渲染" + if (state.effectiveMode == ThemeMode.DARK) "墨纸（夜色山月）" else "宣纸（画廊）")
            Spacer(Modifier.height(JarvisSpacing.space2))
            Chips(ModeLabels, ModeChoices.indexOf(state.modeOverride)) { cb.onModeOverrideChange(ModeChoices[it]) }
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            JarvisDivider()
            GallerySection("权限与命令")
            ValueSwitchRow("限制模式", state.restrictionMode, cb.onRestrictionChange, "默认开启，控制高风险内容和动作")
            ValueSwitchRow("启用命令通道", state.commandChannelEnabled, cb.onCommandChange, "首版默认只做预览，不直接执行")
            ValueRow("命令前缀", state.commandPrefix, cb.onPrefixChange)
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            Meta("root 模式")
            Spacer(Modifier.height(JarvisSpacing.space2))
            Chips(state.rootModes.map { it.label }, state.rootModes.indexOfFirst { it.key == state.rootModeKey }) {
                cb.onRootModeRequest(state.rootModes[it])
            }
            Spacer(Modifier.height(JarvisSpacing.space2))
            Meta("当前：" + state.rootModes.firstOrNull { it.key == state.rootModeKey }?.label.orEmpty(), palette.primary)
            GallerySection("模型连接")
            ValueRow("Base URL", state.modelBaseUrl, cb.onModelBaseUrlChange)
            ValueRow("模型名", state.modelName, cb.onModelNameChange)
            ValueRow("API Key", state.modelApiKey, cb.onModelApiKeyChange, secret = true)
            BlockField("模型回退链（每行一个，可选）", state.modelChain, cb.onModelChainChange)
            ValueSwitchRow("深度思考", state.deepThinkingEnabled, cb.onDeepThinkingChange, "关闭时不发送 reasoning_effort；开启后由下方档位控制")
            Spacer(Modifier.height(JarvisSpacing.space2))
            Chips(efforts, efforts.indexOf(state.reasoningEffort)) { cb.onReasoningEffortChange(efforts[it]) }
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            ValueRow("深度思考模型（可选）", state.deepThinkingModel, cb.onDeepThinkingModelChange, "留空则使用主模型")
            GallerySection("识图模型")
            Meta("识图请求会使用单独的多模态配置；未填写时回退到主模型。")
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            ValueRow("识图 Base URL", state.visionBaseUrl, cb.onVisionBaseUrlChange, "留空继承主模型 Base URL")
            ValueRow("识图模型名", state.visionModel, cb.onVisionModelChange)
            ValueRow("识图 API Key", state.visionApiKey, cb.onVisionApiKeyChange, "留空继承主模型 API Key", true)
            GallerySection("全局人格与世界书")
            BlockField("全局提示词", state.mentorPrompt, cb.onMentorPromptChange, minLines = 2)
            BlockField("全局世界书", state.worldBook, cb.onWorldBookChange, minLines = 2)
            GallerySection("悬浮助手")
            ValueSwitchRow("启用悬浮球", state.floatingEnabled, cb.onFloatingEnabledChange, "语音任务识别后直接交给 Jarvis 执行")
            if (state.floatingShapes.isNotEmpty()) {
                Spacer(Modifier.height(JarvisSpacing.rowGap))
                Meta("悬浮球形状")
                Spacer(Modifier.height(JarvisSpacing.space2))
                ChoiceChips(state.floatingShapes, state.floatingShapeKey, cb.onFloatingShapeChange)
            }
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            Meta("头像大小")
            Spacer(Modifier.height(JarvisSpacing.space2))
            IntChips(state.avatarSizeChoices, state.avatarSizeDp, " dp") { cb.onAvatarSizeChange(it); cb.onFloatingSizeChange(it) }
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            Meta("暂时隐藏时长")
            Spacer(Modifier.height(JarvisSpacing.space2))
            IntChips(state.hideSecondChoices, state.avatarHideSeconds, " 秒", cb.onAvatarHideSecondsChange)
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                JarvisGlyphLabel(Icons.Default.OpenInNew, "悬浮窗", tone = inkTone(state.overlayGranted))
                JarvisGlyphLabel(Icons.Default.Accessibility, "无障碍", tone = inkTone(state.accessibilityGranted))
            }
            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                JarvisIconAction(Icons.Default.OpenInNew, "打开悬浮窗权限设置", cb.onOverlayPermissionRequest)
                JarvisIconAction(Icons.Default.Accessibility, "打开无障碍设置", cb.onAccessibilitySettingsOpen)
            }
            GallerySection("手势任务")
            if (state.gestureOptions.isNotEmpty()) {
                Meta("双击手势")
                Spacer(Modifier.height(JarvisSpacing.space2))
                ChoiceChips(state.gestureOptions, state.doubleGestureKey, cb.onDoubleGestureChange)
                Spacer(Modifier.height(JarvisSpacing.rowGap))
                Meta("三击手势")
                Spacer(Modifier.height(JarvisSpacing.space2))
                ChoiceChips(state.gestureOptions, state.tripleGestureKey, cb.onTripleGestureChange)
            }
            BlockField("双击执行的任务", state.doubleTask, cb.onDoubleTaskChange, minLines = 1)
            BlockField("三击执行的任务", state.tripleTask, cb.onTripleTaskChange, minLines = 1)
            if (state.savedAutomations.isNotEmpty()) {
                Meta("选用已有自动化（绑定到双击手势）")
                Spacer(Modifier.height(JarvisSpacing.space2))
                Chips(state.savedAutomations.map { it.title }, -1) {
                    cb.onSelectAutomationForGesture(state.doubleGestureKey, state.savedAutomations[it].id)
                }
            }
            GallerySection("远程访问")
            ValueSwitchRow(
                "启用远程网页控制台", state.serverRunning,
                { if (it) cb.onStartServer() else cb.onStopServer() },
                "通过局域网或 Tailscale 访问网页控制台"
            )
            if (state.pairingCode.isNotEmpty()) {
                Line("一次性配对码：" + state.pairingCode, JarvisType.codeInline, palette.primary)
            } else {
                Meta("还没有配对码，点击下方生成。")
            }
            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                JarvisIconAction(Icons.Default.VpnKey, "生成新配对码", cb.onNewPairingCode, tone = JarvisActionTone.ACCENT)
                JarvisIconAction(Icons.Default.ContentCopy, "复制配对码", cb.onCopyPairingCode)
                JarvisIconAction(Icons.Default.LinkOff, "撤销配对", cb.onRevokePairing, tone = JarvisActionTone.DANGER)
            }
            ValueSwitchRow("允许远程设备操作", state.remoteDeviceControl, cb.onRemoteDeviceControlChange, "关闭时远程端只能对话和整理计划")
            if (state.remoteUrls.isNotEmpty()) {
                state.remoteUrls.forEach { Meta(it) }
            } else {
                Meta("暂未发现局域网或 Tailscale 地址；确认手机已连接网络后刷新。")
            }
            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                JarvisIconAction(Icons.Default.Refresh, "刷新远程地址", cb.onRefreshRemoteUrls)
            }
            GallerySection("运行时工具")
            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                JarvisGlyphLabel(Icons.Default.Terminal, "CMake", tone = inkTone(state.cmakeReady))
                JarvisGlyphLabel(Icons.Default.Terminal, "FFmpeg", tone = inkTone(state.ffmpegReady))
            }
            if (state.checkingTools) {
                JarvisLoading("正在检查工具路径")
            } else {
                state.runtimeTools.forEach { tool ->
                    InfoRow(tool.name, if (tool.installed) "已安装 · " + tool.version else "未安装") {
                        if (!tool.installed) {
                            JarvisIconAction(
                                Icons.Default.FileDownload, "安装 " + tool.name,
                                { cb.onInstallTool(tool.name) },
                                tone = JarvisActionTone.ACCENT
                            )
                        }
                    }
                }
            }
            if (state.toolNotice.isNotEmpty()) NoticeLine(state.toolNotice)
            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                JarvisIconAction(Icons.Default.ManageSearch, "检查工具路径", cb.onRefreshTools)
                JarvisIconAction(Icons.Default.Refresh, "重新检查运行时", cb.onCheckTools)
            }
            GallerySection("社区插件")
            if (state.pluginBusy) JarvisLoading("处理中…")
            state.plugins.forEach { plugin ->
                ValueSwitchRow(
                    plugin.name + " " + plugin.version, plugin.enabled, { cb.onTogglePlugin(plugin.id, it) },
                    "工具：" + plugin.tools.joinToString().ifEmpty { "无" } + " · " + plugin.source
                )
                JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight), Arrangement.End) {
                    JarvisIconAction(Icons.Default.Delete, "卸载插件", { cb.onUninstallPlugin(plugin.id) }, tone = JarvisActionTone.DANGER)
                }
            }
            BlockField("粘贴插件草稿（JSON）", pluginDraft, { pluginDraft = it })
            if (state.pluginNotice.isNotEmpty()) NoticeLine(state.pluginNotice)
            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                JarvisIconAction(Icons.Default.FileDownload, "安装草稿插件", { cb.onInstallPlugin(pluginDraft) }, tone = JarvisActionTone.ACCENT)
                JarvisIconAction(Icons.Default.Refresh, "刷新插件列表", cb.onRefreshPlugins)
            }
            GallerySection("操作记忆")
            Meta(state.memoryStatus.ifEmpty { "首次成功后学习，后续优先复用；失败时重新探索" })
            Spacer(Modifier.height(JarvisSpacing.rowGap))
            state.memoryEntries.forEach { entry ->
                InfoRow(
                    entry.title,
                    (if (entry.ready) "可复用" else "已失效，待重新学习") + " / 成功 " + entry.successes + " 失败 " + entry.failures,
                    { JarvisIconAction(Icons.Default.Delete, "删除这条操作记忆", { cb.onDeleteMemoryEntry(entry.id) }, tone = JarvisActionTone.DANGER) }
                )
            }
            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                JarvisIconAction(Icons.Default.Refresh, "刷新操作路径", cb.onRefreshMemory)
            }
            GallerySection("无障碍")
            ValueSwitchRow("打开 Jarvis 时自动连接", state.accessibilityAutoConnect, cb.onAccessibilityAutoConnectChange, "需要 root 完全模式；关闭此项不会撤销已有授权")
            if (state.accessibilityStatus.isNotEmpty()) Meta(state.accessibilityStatus)
            GallerySection("AI Classroom 配置同步")
            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                JarvisIconAction(Icons.Default.FileDownload, "从 AI Classroom 导入", cb.onImportAiClassroom, tone = JarvisActionTone.ACCENT)
                JarvisIconAction(Icons.Default.FileUpload, "导出到 AI Classroom", cb.onExportAiClassroom)
            }
            if (state.importNotice.isNotEmpty()) NoticeLine(state.importNotice)
            if (state.exportNotice.isNotEmpty()) NoticeLine(state.exportNotice)
            Meta("API Key 只在本机配置中保存。")
            GallerySection("OpenClaw 运行时")
            RuntimeBlock(state.bootstrapReport, state.runtimeMessage, state.configNotice, cb)
            GallerySection("保存并应用")
            if (state.saving) JarvisLoading("应用中…")
            if (state.applyNotice.isNotEmpty()) {
                NoticeLine(state.applyNotice, tone = if (state.applyFailed) NoticeTone.ERROR else NoticeTone.SUCCESS)
            }
            JarvisIconRow(Modifier.heightIn(min = JarvisSize.denseRowHeight)) {
                JarvisIconAction(Icons.Default.Save, "保存并应用", cb.onApply, tone = JarvisActionTone.ACCENT)
                JarvisIconAction(Icons.Default.Verified, "保存并验证", cb.onApplyAndVerify)
            }
            Meta("编辑内容自动保存在本机；保存并应用可更新智能体。验证会发起一次简短的主模型请求。")
            Spacer(Modifier.height(JarvisSpacing.sectionGap))
        }
    }
    @Composable
    private fun RuntimeBlock(
        report: BootstrapReport,
        message: String,
        configNotice: String,
        cb: SettingsUiContext.SettingsCallbacks
    ) {
        val palette = inkPalette()
        Body("状态：" + runtimeStateLabel(report.state))
        Meta("版本：" + report.runtimeVersion)
        Line("插件目录：" + report.pluginDirectory, JarvisType.codeInline, palette.onSurfaceVariant)
        Meta((if (report.filesReady) "文件已检测到" else "文件缺失") + " · " + (if (report.rootAvailable) "root 已检测到" else "root 缺失") + " · " + (if (report.bridgeTokenReady) "令牌已生成" else "令牌缺失"))
        Spacer(Modifier.height(JarvisSpacing.rowGap))
        JarvisIconRow(Modifier.fillMaxWidth().heightIn(min = JarvisSize.denseRowHeight), Arrangement.Center) {
            JarvisIconAction(Icons.Default.Refresh, "重新检查运行时", cb.onRefreshRuntime)
            JarvisIconAction(Icons.Default.PlayArrow, "启动 Gateway", cb.onStartRuntime, tone = JarvisActionTone.ACCENT)
            JarvisIconAction(Icons.Default.Stop, "停止 Gateway", cb.onStopRuntime)
            JarvisIconAction(Icons.Default.Build, "运行时修复 / Termux 备用方案", cb.onRecovery)
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
