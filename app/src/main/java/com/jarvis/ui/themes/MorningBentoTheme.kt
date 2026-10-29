package com.jarvis.ui.themes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.app.runtime.*
import com.jarvis.ui.components.*
import com.jarvis.ui.theme.*
import com.jarvis.ui.tokens.*

/** 浅色色板「晨光案头」：22 个必填字段逐字段显式给出，不做任何明暗推导。 */
private val BentoLightPalette = ThemePalette(
    primary = Color(0xFFB5551F), onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF3F7358), onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFEFE1CB), onBackground = Color(0xFF2A2118),
    surface = Color(0xFFFFFDFA), onSurface = Color(0xFF241C14),
    surfaceContainerLow = Color(0xFFFAF3E9), surfaceContainer = Color(0xFFF5EBDD),
    surfaceContainerHigh = Color(0xFFEFE2CF), onSurfaceVariant = Color(0xFF6E6053),
    outline = Color(0xFFC4AE90), borderSubtle = Color(0xFFD2BFA6),
    divider = Color(0xFFE4D5C0), error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
    scrim = Color(0xFF1A1207), accentSoft = Color(0xFFFBE2CE), glow = Color(0xFFC87A32),
    primaryContainer = Color(0xFFFBE2CE), onPrimaryContainer = Color(0xFF5A2A0C),
    inversePrimary = Color(0xFFC87A32), secondaryContainer = Color(0xFFDFEDE2),
    onSecondaryContainer = Color(0xFF1E3A2C), tertiary = Color(0xFF8A6A3E),
    onTertiary = Color(0xFFFFFFFF), tertiaryContainer = Color(0xFFF3E3C8),
    onTertiaryContainer = Color(0xFF3B2A12), surfaceVariant = Color(0xFFF5EBDD),
    surfaceTint = Color(0xFFB5551F), surfaceBright = Color(0xFFFFFDFA),
    surfaceDim = Color(0xFFEDE0CB), surfaceContainerHighest = Color(0xFFE7D7C0),
    surfaceContainerLowest = Color(0xFFFFFDFA), outlineVariant = Color(0xFFD2BFA6),
    inverseSurface = Color(0xFF2A2118), inverseOnSurface = Color(0xFFFFF9F2)
)

/** 暗色色板「夜晚厨房」：同一副便当格栅，灯关了灶火还亮着。 */
private val BentoDarkPalette = ThemePalette(
    primary = Color(0xFFEDA06B), onPrimary = Color(0xFF2A1403),
    secondary = Color(0xFF9CCFB0), onSecondary = Color(0xFF10210F),
    background = Color(0xFF14100C), onBackground = Color(0xFFF2E6D6),
    surface = Color(0xFF1E1813), onSurface = Color(0xFFF6EEE3),
    surfaceContainerLow = Color(0xFF241D17), surfaceContainer = Color(0xFF2A221B),
    surfaceContainerHigh = Color(0xFF332A21), onSurfaceVariant = Color(0xFFC9B7A2),
    outline = Color(0xFF6A5B4B), borderSubtle = Color(0xFF4A3E32),
    divider = Color(0xFF3A3129), error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xFF000000), accentSoft = Color(0xFF4A2E17), glow = Color(0xFFFFA24A),
    primaryContainer = Color(0xFF5A3418), onPrimaryContainer = Color(0xFFFFDCC2),
    inversePrimary = Color(0xFFC87A32), secondaryContainer = Color(0xFF2C4436),
    onSecondaryContainer = Color(0xFFC6E9D2), tertiary = Color(0xFFD9BE97),
    onTertiary = Color(0xFF3B2A12), tertiaryContainer = Color(0xFF4A3A22),
    onTertiaryContainer = Color(0xFFF3E3C8), surfaceVariant = Color(0xFF332A21),
    surfaceTint = Color(0xFFEDA06B), surfaceBright = Color(0xFF2E2620),
    surfaceDim = Color(0xFF14100C), surfaceContainerHighest = Color(0xFF3B3128),
    surfaceContainerLowest = Color(0xFF100D0A), outlineVariant = Color(0xFF4A3E32),
    inverseSurface = Color(0xFFF2E6D6), inverseOnSurface = Color(0xFF33291F)
)

/**
 * 晨光便当（morning_bento）：亮色便当格栅——0.62/0.38 混列等高格、不对称圆角气泡、大留白分隔、底栏 5 个可点便当胶囊。
 */
internal object MorningBentoTheme : JarvisThemeModule {
    override val id = "morning_bento"
    override val label = "晨光便当"
    override val story = "清晨的案头"
    override val description = "暖杏晨光 · 便当格栅"
    override val mode = ThemeMode.LIGHT
    override val chrome = ChromeStyle.BOTTOM_BAR
    override val artwork = ThemeArtwork(
        banner = com.jarvis.app.R.drawable.theme_morning_bento_banner,
        avatar = com.jarvis.app.R.drawable.theme_morning_bento_avatar,
        texture = com.jarvis.app.R.drawable.theme_morning_bento_texture
    )

    /**
     * 明暗双模式：`palette` 仍是「作者写的那一套」作为身份底色，实际渲染取 `palette(mode)`。
     * 两套色板都是完整 22 字段（见文件顶部 BentoLightPalette / BentoDarkPalette），没有任何字段回落。
     *
     * 对比度实测（WCAG 相对亮度 + CIE L*，本轮脚本算过，不是估值）：
     *
     * | 指标                       | 亮色  | 暗色  | 门槛            |
     * |----------------------------|-------|-------|-----------------|
     * | borderSubtle vs surface    | 1.76  | 1.70  | 1.5~2.2:1       |
     * | ΔL*(surface, background)   | 9.29  | 3.86  | 亮≥3.0 / 暗≥2.5 |
     * | onSurface vs surface       | 16.53 | 15.27 | ≥7:1            |
     * | onSurfaceVariant vs surface| 5.98  | 9.02  | ≥4.5:1          |
     * | onPrimary vs primary       | 4.90  | 8.20  | ≥4.5:1          |
     *
     * surfaceContainer 阶梯 ΔL*（低→最高）：亮 3.26 / 5.91 / 8.95 / 12.68，暗 2.59 / 5.12 / 8.99 / 12.34；
     * chip 与输入框落在 surfaceContainerHigh，靠这层明度差立住，因此两者都不画外框。
     */
    override val palette: ThemePalette = BentoLightPalette

    override val lightPalette: ThemePalette = BentoLightPalette

    override val darkPalette: ThemePalette = BentoDarkPalette


    override fun floatingPalette() = FloatingPaletteSpec(
        panel = palette.surface, title = palette.onSurface, body = palette.onBackground,
        accent = palette.primary, muted = palette.onSurfaceVariant, bubbleRing = palette.borderSubtle
    )

    @Composable
    override fun ChatScreen(ctx: ChatUiContext) = BentoChat(ctx)

    @Composable
    override fun TasksScreen(ctx: TasksUiContext) = BentoTasks(ctx)

    @Composable
    override fun WorkspaceScreen(ctx: WorkspaceUiContext) = BentoWorkspace(ctx)

    @Composable
    override fun SettingsScreen(ctx: SettingsUiContext) = BentoSettings(ctx)
}

private val BentoEfforts = listOf("off", "minimal", "low", "medium", "high", "max")
private val BentoPrompts = listOf("手机状态", "打开应用", "操作记忆")
private val BentoPromptText = listOf("查询本机手机型号、系统版本和当前设备状态。", "帮我打开一个应用，请先问我要打开哪个。", "查看已保存的操作路径，告诉我有哪些可以复用。")
private val BentoWide = 0.62f

/** 便当格栅的堆叠阈值（可用宽度 dp）：低于它 0.38 窄格承载不了文字，改纵向堆叠。 */
private val BentoStackBelowDp = 420.dp
private val bentoArt = MorningBentoTheme.artwork
private val bentoTexture = MorningBentoTheme.artwork.texture ?: 0
private val BubbleBot = JarvisShapes.md.copy(topStart = CornerSize(JarvisSpacing.space2))
private val BubbleUser = JarvisShapes.md.copy(topEnd = CornerSize(JarvisSpacing.space2))

/** 平铺笔刷：texture 先缩到 22%（512 -> 约 113px/tile），再 ImageShader 满幅重复；真平铺、不拉伸。 */
@Composable
private fun BentoTileBrush(): Brush? {
    if (bentoTexture == 0) return null
    val source = ImageBitmap.imageResource(bentoTexture)
    return remember(source) {
        val original = source.asAndroidBitmap()
        val w = (original.width * 0.22f).toInt().coerceAtLeast(1)
        val h = (original.height * 0.22f).toInt().coerceAtLeast(1)
        val tile = android.graphics.Bitmap.createScaledBitmap(original, w, h, true)
        ShaderBrush(ImageShader(tile.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
}

/**
 * 页面底：background 打底 + theme_morning_bento_texture 满幅平铺，卡片不透明、不叠 alpha。
 *
 * texture 是**亮米色**图案，只属于浅色「晨光案头」。暗色「夜晚厨房」下铺上去会把 ΔL* 3.86 的暗底
 * 整片提亮成浅米色，onSurface 的浅字压在上面几乎看不见（实测设置页标题读不出来），
 * 因此暗色只保留纯 background，纹理只在亮色页面出现。
 */
@Composable
private fun Modifier.bentoPage(): Modifier {
    val base = background(MaterialTheme.colorScheme.background)
    // 明暗只认外壳提供的 LocalJarvisMode：不再按 background 亮度反查（每次重组重算一遍）。
    if (LocalJarvisMode.current == ThemeMode.DARK) return base
    val brush = BentoTileBrush() ?: return base
    return base.background(brush)
}

private fun bentoClock(epochMs: Long): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(epochMs))

/**
 * 便当格栅：宽屏（可用宽 >= [BentoStackBelowDp]）保持 0.62 / 0.38 两列、Row + IntrinsicSize.Min 强制等高；
 * 窄屏（手机竖屏，可用宽 < [BentoStackBelowDp]）改为纵向堆叠，两格各自 fillMaxWidth、各自按内容取高。
 *
 * 堆叠是必须的：0.38 格在 360~410dp 的手机上只剩约 150dp，长副标题会被压成单字竖排、
 * chip 条会把标签截断，这正是「很多项目挤在一起」最刺眼的一处。堆叠分支不再用 IntrinsicSize。
 *
 * Row 分支的等高靠 height(IntrinsicSize.Min) 求两格最大 intrinsic 高度，因此**格槽内禁止放任何
 * SubcomposeLayout**（LazyRow / LazyVerticalGrid / TabRow 等）：向 SubcomposeLayout 求 intrinsic
 * 会抛 IllegalStateException("Asking for intrinsic measurements of SubcomposeLayout layouts is
 * not supported")，整屏崩溃。chip 行与滚动列表要放进整宽 JarvisCard，不要塞进 BentoPair 的格里。
 *
 * 已实测（foundation-layout 1.7.4）：[BentoChipRow] 的 FlowRow 不是 SubcomposeLayout
 * （FlowLayout.kt 用的是普通 Layout + MultiContentMeasurePolicy，intrinsic 有默认实现），
 * 因此 chip 行进格子后，宽屏分支（>= 420dp，含横屏/平板/折叠屏展开）不崩：把密度改成 300
 * （1080px -> 576dp）打开设置页，BentoPair 走 Row+IntrinsicSize.Min 分支，crash buffer 与
 * "Asking for intrinsic" 均 0 命中。往格子里放 Lazy* 仍然禁止。
 */
@Composable
private fun BentoPair(a: Float = BentoWide, first: @Composable ColumnScope.() -> Unit, second: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < BentoStackBelowDp) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(JarvisSpacing.cardGap)
            ) {
                JarvisCard(Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding, shape = JarvisShapes.sm) { first() }
                JarvisCard(Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding, shape = JarvisShapes.sm) { second() }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.cardGap)
            ) {
                JarvisCard(Modifier.weight(a).fillMaxHeight(), contentPadding = JarvisSpacing.cardPadding, shape = JarvisShapes.sm) { first() }
                JarvisCard(Modifier.weight(1f - a).fillMaxHeight(), contentPadding = JarvisSpacing.cardPadding, shape = JarvisShapes.sm) { second() }
            }
        }
    }
}

/**
 * 单卡便当格：**同一个对象**的信息区与操作区要留在同一张卡里。
 *
 * [BentoPair] 天生拆成两张卡（窄屏上下两张、宽屏左右两张），用在别处没问题；但工作区这一行
 * 拆开后就是「名称/会话数/进入」一张、「当前徽标/编辑/归档/删除」另一张，看上去像两个不同的
 * 东西。这里外框只有一个，宽屏在卡内按 0.62/0.38 分两栏，窄屏在卡内上下堆叠——格栅还在，割裂没了。
 *
 * 与 [BentoPair] 同样的限制：宽屏分支靠 IntrinsicSize.Min 求高，格槽内不放任何 SubcomposeLayout。
 */
@Composable
private fun BentoPairCard(
    a: Float = BentoWide,
    info: @Composable ColumnScope.() -> Unit,
    ops: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < BentoStackBelowDp) {
            JarvisCard(Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding, shape = JarvisShapes.sm) {
                Column(Modifier.fillMaxWidth()) { info() }
                Spacer(Modifier.height(JarvisSpacing.rowGap))
                Column(Modifier.fillMaxWidth()) { ops() }
            }
        } else {
            JarvisCard(Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding, shape = JarvisShapes.sm) {
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.cardGap)
                ) {
                    Column(Modifier.weight(a).fillMaxHeight()) { info() }
                    Column(Modifier.weight(1f - a).fillMaxHeight()) { ops() }
                }
            }
        }
    }
}

@Composable
private fun BentoGap(height: Dp = JarvisSpacing.sectionGap) = Spacer(Modifier.height(height))

/**
 * 屏头：标题、副标题各占一行（副标题不再和「新建 / 扳手 / 待命」挤同一行），两行之间留 space2；
 * 右侧操作区另起一行靠右，不参与标题列的宽度竞争，长副标题因此永远整行可读。
 */
@Composable
private fun BentoHeader(
    title: String,
    subtitle: String,
    leading: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(JarvisSpacing.space4))
            }
            Text(title, Modifier.weight(1f), style = JarvisType.titleScreen, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(JarvisSpacing.space2))
        Text(subtitle, Modifier.fillMaxWidth(), style = JarvisType.bodyMeta,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actions != null) {
            Spacer(Modifier.height(JarvisSpacing.space3))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

@Composable
private fun BentoHead(title: String, badge: String, tone: BadgeTone) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = JarvisType.titleItem, color = MaterialTheme.colorScheme.onSurface)
        StatusBadge(badge, tone)
    }
}

/**
 * 无标题开关行：卡头（[BentoHead] / [SectionHeader]）已经承载分组标题时用它，
 * 避免「限制模式」出现两遍（卡头一遍、开关行又一遍）。开关名由卡头承担，
 * 这里只放副标题 + 右侧 Switch，语义由 Switch 自身的 Role.Switch 承担。
 */
@Composable
private fun BentoSwitchLine(subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = JarvisSize.rowMinHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(subtitle, Modifier.weight(1f), style = JarvisType.bodyMeta,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(JarvisSpacing.space4))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun BentoMeta(text: String) = Text(text, style = JarvisType.codeInline, color = MaterialTheme.colorScheme.onSurfaceVariant)

/**
 * 图标动作规格：「动作型」按钮一律只画图标，语义全部落在 contentDescription 上（无障碍不失）；
 * 危险动作（删除/卸载/解绑）走 [JarvisActionTone.DANGER]，主操作走 ACCENT。
 */
private data class BentoAct(
    val icon: ImageVector,
    val label: String,
    val tone: JarvisActionTone = JarvisActionTone.NEUTRAL,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/** 图标动作横排：间距 4dp，触达由 [JarvisIconAction] 内嵌的 44dp 方框承担。 */
@Composable
private fun BentoIconActions(vararg actions: BentoAct) {
    JarvisIconRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
        actions.forEach { act ->
            JarvisIconAction(
                icon = act.icon, contentDescription = act.label, onClick = act.onClick,
                enabled = act.enabled, tone = act.tone
            )
        }
    }
}

/** 图标 + 极短文字的可点动作：只在「点的是哪一个对象」必须靠文字识别时用（来源标题、规则名、工具名、目标工作区）。 */
@Composable
private fun BentoGlyphAction(icon: ImageVector, text: String, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = JarvisSize.minTouch).clip(JarvisShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = JarvisSpacing.space3),
        contentAlignment = Alignment.Center
    ) { JarvisGlyphLabel(icon, text) }
}

/** 纯状态标记（不可点）：图标 + 颜色即可表达的状态不再写汉字。 */
@Composable
private fun BentoStateMark(icon: ImageVector, description: String, active: Boolean) {
    Icon(
        imageVector = icon, contentDescription = description,
        tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(JarvisSize.toolbarIcon)
    )
}

/**
 * chip 行：**不画描边**。M3 FilterChip 默认外框走 colorScheme.outline（本主题 2.1:1），
 * 是「图标/按钮边框太显眼」的主要来源之一；这里统一改成实心胶囊，选中/未选中由
 * primaryContainer 与 surfaceContainerLow 的底色明度差表达，chip 之间 space3。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BentoChipRow(items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    if (items.isEmpty()) return
    // FlowRow 换行而不是横向滚动：滚动会在卡片右边缘切掉半个 chip，看上去就是「被截断」。
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space3),
        verticalArrangement = Arrangement.spacedBy(JarvisSpacing.space3)
    ) {
        items.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(index) },
                label = { Text(label, style = JarvisType.titleItem, maxLines = 1) },
                modifier = Modifier
                    .heightIn(min = JarvisSize.chipHeight)
                    .semantics {
                        selected = isSelected
                        role = Role.RadioButton
                    },
                shape = JarvisShapes.pill,
                // 不画外框：未选中落 surfaceContainerHigh（与卡片 surface ΔL* 亮 8.95 / 暗 8.99），
                // 选中落 primaryContainer，两级底色差替代原来的 1dp 圆角外框。
                border = BorderStroke(0.dp, Color.Transparent),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

/** 标签 + 单选 chip 行。 */
@Composable
private fun BentoChips(label: String, items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    if (items.isEmpty()) return
    BentoMeta(label)
    Spacer(Modifier.height(JarvisSpacing.space3))
    BentoChipRow(items, selectedIndex, onSelect)
    Spacer(Modifier.height(JarvisSpacing.space3))
}

@Composable
private fun BentoAvatarMark() {
    Image(
        painter = painterResource(bentoArt.avatar), contentDescription = "主题头像", contentScale = ContentScale.Crop,
        modifier = Modifier.size(56.dp).clip(JarvisShapes.pill)
    )
}

private data class BentoF(val label: String, val value: String, val onChange: (String) -> Unit, val hint: String? = null, val mono: Boolean = true, val lines: Int = 1, val secret: Boolean = false)

/**
 * 输入框配色：填充底 + 底部一条细线，四边不画外框。OutlinedTextField 的 1dp 圆角外框
 * 每格 8 个，是「边框太显眼」最扎眼的一类；填充底用 surfaceContainerLow，与卡片 surface 有明度差。
 */
@Composable
private fun bentoFieldColors() = TextFieldDefaults.colors(
    // 底色 surfaceContainerHigh + 底部一条细线：四边不画外框，靠与卡片 surface 的明度差成块（亮 8.95 / 暗 8.99 ΔL*）。
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
    disabledIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
private fun BentoFields(vararg specs: BentoF) {
    Column(verticalArrangement = Arrangement.spacedBy(JarvisSpacing.rowGap)) {
        specs.forEach { spec ->
            if (spec.secret) {
                JarvisSecretField(
                    label = spec.label, value = spec.value, onValueChange = spec.onChange,
                    hint = spec.hint.orEmpty(),
                    labelStyle = JarvisType.labelBadge, valueStyle = JarvisType.codeInline,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    accentColor = MaterialTheme.colorScheme.primary, showDivider = true)
                return@forEach
            }
            TextField(
                value = spec.value, onValueChange = spec.onChange, modifier = Modifier.fillMaxWidth(),
                label = { Text(spec.label, style = JarvisType.labelBadge) },
                placeholder = if (spec.hint == null) null else { { Text(spec.hint, style = JarvisType.bodyMeta) } },
                textStyle = if (spec.mono) JarvisType.codeInline else JarvisType.bodyBase,
                singleLine = spec.lines == 1, minLines = spec.lines, shape = JarvisShapes.sm,
                colors = bentoFieldColors()
            )
        }
    }
}

/**
 * 自绘底栏：5 个便当胶囊，页面项调 shell.onNavigate(i)，浮窗项调 shell.onOpenFloating()，选中态读 shell.tabIndex。
 *
 * 栏高收成 [JarvisSize.bottomBarHeight]（52dp），与外壳顶栏同档；52 - 上下各 space2（4dp）= 44dp，
 * 正好等于 [JarvisSize.minTouch]，胶囊的触达下限由栏高本身兜住（不再额外堆 heightIn）。
 * 栏面 surface、页面 background，两者 ΔL* 亮 9.29 / 暗 3.86，不用描边也能分出栏沿；
 * 栏沿再压一条 0.5dp 发丝线（[JarvisStroke.hairline] + outlineVariant），只勾一道，不抢色块。
 * 选中项是 primaryContainer 实心胶囊（字号升到 titleItem），未选中项**不画底色**，只留文字。
 */
@Composable
private fun BentoBar(shell: ShellUiContext, fallbackIndex: Int) {
    val colors = MaterialTheme.colorScheme
    val tabs = if (shell.tabs.isEmpty()) ShellUiContext.DefaultTabs else shell.tabs
    val selected = if (shell.tabs.isEmpty()) fallbackIndex else shell.tabIndex
    var page = 0
    Column(Modifier.fillMaxWidth().background(colors.surface)) {
        Box(Modifier.fillMaxWidth().height(JarvisStroke.hairline).background(colors.outlineVariant))
        Row(
            Modifier.fillMaxWidth().height(JarvisSize.bottomBarHeight)
                .padding(horizontal = JarvisSpacing.space4, vertical = JarvisSpacing.space2),
            horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)
        ) {
            tabs.forEach { tab ->
                val target = if (tab.opensFloating) -1 else page
                if (!tab.opensFloating) page += 1
                val active = !tab.opensFloating && target == selected
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().clip(JarvisShapes.sm)
                        .background(if (active) colors.primaryContainer else Color.Transparent)
                        .clickable(role = Role.Tab) { if (tab.opensFloating) shell.onOpenFloating() else shell.onNavigate(target) }
                        .semantics { this.selected = active },
                    contentAlignment = Alignment.Center
                ) {
                    Text(tab.label, style = if (active) JarvisType.titleItem else JarvisType.labelBadge, maxLines = 1,
                        color = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BentoChat(ctx: ChatUiContext) {
    val s = ctx.state
    val cb = ctx.callbacks
    val efforts = s.reasoningEfforts.ifEmpty { BentoEfforts }
    val list = rememberLazyListState()
    LaunchedEffect(s.conversationId, s.messages.size) {
        if (s.messages.isNotEmpty()) list.animateScrollToItem(s.messages.lastIndex)
    }
    Column(Modifier.fillMaxSize().bentoPage()) {
        Column(Modifier.fillMaxSize().weight(1f).jarvisScreenPadding()) {
            BentoHeader("对话", subtitle = "在消息下方编辑、撤销或创建分支", leading = { BentoAvatarMark() },
                actions = {
                    JarvisIconRow(horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
                        JarvisIconAction(Icons.Filled.Add, "新建对话", cb.onNewConversation, tone = JarvisActionTone.ACCENT)
                        JarvisIconAction(Icons.Filled.Build, "工具箱", cb.onOpenTools, tone = JarvisActionTone.ACCENT)
                    }
                    Spacer(Modifier.width(JarvisSpacing.space3))
                    StatusBadge(if (s.streaming) "流式" else if (s.sending) "请求" else "待命",
                        if (s.sending) BadgeTone.WARNING else BadgeTone.ACTIVE)
                })
            BentoGap()
            if (s.messages.isEmpty()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    BentoPair(
                        first = {
                            BentoHead("开始", "晨", BadgeTone.ACTIVE)
                            Text("让想法，\n开始行动。", style = JarvisType.titleScreen, color = MaterialTheme.colorScheme.onSurface)
                            Text("交给 Jarvis，逐步完成。", style = JarvisType.bodyMeta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        second = {
                            BentoHead("会话", s.reasoningEffort, BadgeTone.NEUTRAL)
                            BentoMeta(s.conversationId.take(8))
                        }
                    )
                    BentoGap()
                    JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                        SectionHeader("从一个小任务开始", action = { StatusBadge("3", BadgeTone.NEUTRAL) })
                        BentoPrompts.forEachIndexed { index, label ->
                            ListRow(title = label, subtitle = BentoPromptText[index]) { cb.onDraftChange(BentoPromptText[index]) }
                        }
                    }
                }
            } else {
                LazyColumn(state = list, modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(JarvisSpacing.cardGap)) {
                    items(s.messages, key = { it.id }) { message -> BentoBubble(message) }
                    item {
                        JarvisIconRow(horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
                            JarvisIconAction(Icons.Filled.AccountTree, "从这条消息创建分支",
                                { cb.onBranch(s.messages.lastOrNull()?.id) })
                            JarvisIconAction(Icons.Filled.Restore, "撤销最后一条", cb.onUndoLatest,
                                enabled = s.canUndoLatest)
                            JarvisIconAction(
                                if (s.editingLatest) Icons.Filled.Close else Icons.Filled.Edit,
                                if (s.editingLatest) "取消编辑" else "编辑上一条",
                                if (s.editingLatest) cb.onCancelEdit else cb.onEditLatest,
                                enabled = s.editingLatest || s.canEditLatest)
                        }
                    }
                }
            }
            BentoGap(JarvisSpacing.cardGap)
            JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                TextField(
                    value = s.draft, onValueChange = cb.onDraftChange, modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(if (s.editingLatest) "修改上一条消息后重新发送…" else "给 Jarvis 发消息，或描述一个任务…",
                            style = JarvisType.bodyMeta)
                    },
                    maxLines = 4, textStyle = JarvisType.bodyBase, shape = JarvisShapes.sm,
                    colors = bentoFieldColors()
                )
                Spacer(Modifier.height(JarvisSpacing.space3))
                BentoChipRow(efforts, efforts.indexOf(s.reasoningEffort.lowercase()), { cb.onReasoningEffortChange(efforts[it]) })
                Spacer(Modifier.height(JarvisSpacing.space3))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BentoMeta(s.reasoningEffort)
                    Spacer(Modifier.weight(1f))
                    JarvisIconAction(Icons.Filled.Mic, "语音转文字", cb.onVoiceInput,
                        enabled = !s.sending && !s.voiceListening, tone = JarvisActionTone.ACCENT)
                    JarvisIconAction(Icons.Filled.Build, "打开工具箱", cb.onOpenTools, tone = JarvisActionTone.ACCENT)
                    // 发送键 44dp 实心圆（primaryContainer 底 + onPrimaryContainer 图标），不再是一枚悬空图标。
                    IconButton(onClick = { cb.onSend(s.draft.trim()) }, enabled = !s.sending && s.draft.isNotBlank(),
                        modifier = Modifier.size(JarvisSize.sendButton)
                            .clip(JarvisShapes.pill)
                            .background(if (s.draft.isNotBlank() && !s.sending) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送",
                            tint = if (s.draft.isNotBlank() && !s.sending) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(JarvisSize.toolbarIcon))
                    }
                }
                if (s.voiceListening) JarvisLoading("正在识别语音…")
                if (s.editingLatest) NoticeLine("正在修改上一条；发送后才会替换原对话轮次。")
                if (s.speechNotice.isNotBlank()) NoticeLine(s.speechNotice)
                if (s.sending) JarvisLoading("正在请求模型…")
            }
        }
        BentoBar(ctx.shell, 0)
    }
}

/** 气泡：primaryContainer 底（用户侧 secondaryContainer 区分），紧角只在时间戳对侧。 */
@Composable
private fun BentoBubble(message: ChatMessage) {
    val user = message.role == ChatMessageRole.USER
    val colors = MaterialTheme.colorScheme
    val shape = if (user) BubbleUser else BubbleBot
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
        Column(
            Modifier.fillMaxWidth(0.88f).clip(shape)
                .background(if (user) colors.secondaryContainer else colors.primaryContainer)
                .padding(horizontal = JarvisSpacing.cardPadding, vertical = JarvisSpacing.insetCardPadding)
        ) {
            Text(message.content, style = JarvisType.bodyBase,
                color = if (user) colors.onSecondaryContainer else colors.onPrimaryContainer)
            BentoGap(JarvisSpacing.space3)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.Start else Arrangement.End) {
                Text(bentoClock(message.createdAt), style = JarvisType.codeInline, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BentoTasks(ctx: TasksUiContext) {
    val s = ctx.state
    val cb = ctx.callbacks
    val kindOptions = s.ruleDraftKindOptions
    val pixivDraft = s.ruleDraftKindKey.contains("PIXIV", ignoreCase = true)
    val creating = s.creatingRule
    Column(Modifier.fillMaxSize().bentoPage()) {
        Column(Modifier.fillMaxSize().weight(1f).jarvisScreenPadding()) {
            BentoHeader("自动化", subtitle = "对话创建与表单直填两条路；收到通知后按规则执行一次。", leading = { BentoAvatarMark() },
                actions = {
                    // 建单态不再挂页头「新建规则」入口：草案卡自带创建/取消，两个入口并存只会互相抢点击。
                    JarvisIconRow(horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
                        if (!creating) {
                            JarvisIconAction(Icons.Filled.Add, "新建规则", cb.onCreateRuleStart, tone = JarvisActionTone.ACCENT)
                        }
                        JarvisIconAction(Icons.Filled.Refresh, "刷新规则", cb.onRefresh)
                    }
                    Spacer(Modifier.width(JarvisSpacing.space3))
                    StatusBadge(if (s.runtimeReady) "就绪" else "未就绪",
                        if (s.runtimeReady) BadgeTone.ACTIVE else BadgeTone.WARNING)
                })
            BentoGap()
            // 建单态把「通知访问 / 无障碍 / 规则摘要」整段让路：这三块是浏览态的背景信息，
            // 留着会把草案卡压进约 160dp 的视口（实测「规则类型」chips 正落在 y=2033 折叠线上）。
            if (!creating) {
                BentoPair(
                    first = {
                        BentoHead("通知访问", if (s.notificationAccess) "已授权" else "待授权",
                            if (s.notificationAccess) BadgeTone.ACTIVE else BadgeTone.WARNING)
                        if (!s.notificationAccess) {
                            BentoIconActions(BentoAct(Icons.Filled.Notifications, "去授权通知访问", JarvisActionTone.ACCENT) {
                                cb.onRequestNotificationAccess()
                            })
                        }
                    },
                    second = {
                        BentoHead("无障碍", if (s.accessibilityGranted) "已连接" else "待连接",
                            if (s.accessibilityGranted) BadgeTone.ACTIVE else BadgeTone.WARNING)
                        if (!s.accessibilityGranted) {
                            BentoIconActions(BentoAct(Icons.Filled.Accessibility, "开启无障碍服务", JarvisActionTone.ACCENT) {
                                cb.onRequestAccessibility()
                            })
                        }
                    }
                )
                // 没有规则时「规则 0 · 最近执行 0」这张摘要卡只剩一句说明，占掉空态 CTA 的位置；
                // 此时只保留可能存在的操作提示，把视口让给空态。
                if (s.rules.isNotEmpty()) {
                    BentoGap(JarvisSpacing.cardGap)
                    JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("规则 " + s.rules.size + " · 最近执行 " + s.recentEvents.size, Modifier.weight(1f),
                                style = JarvisType.titleItem, color = MaterialTheme.colorScheme.onSurface)
                            StatusBadge(if (s.runtimeReady) "可执行" else "待授权",
                                if (s.runtimeReady) BadgeTone.ACTIVE else BadgeTone.WARNING)
                        }
                        NoticeLine("规则默认暂停；启用后只匹配目标应用通知，并在完成后核实结果。")
                        if (s.notice.isNotBlank()) NoticeLine(s.notice, tone = NoticeTone.ERROR)
                    }
                } else if (s.notice.isNotBlank()) {
                    BentoGap(JarvisSpacing.cardGap)
                    NoticeLine(s.notice, tone = NoticeTone.ERROR)
                }
                BentoGap(JarvisSpacing.cardGap)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(JarvisSpacing.cardGap)) {
                // 草案卡与空态**互斥渲染**：同一时刻只画一个。此前 creatingRule=true 时两者同列，
                // 空态 CTA 与草案卡争同一个视口，CTA 恰好压在 y≈2033 的折叠线上。
                if (creating) {
                    BentoRuleDraft(s, cb, kindOptions, pixivDraft)
                } else if (s.rules.isEmpty()) {
                    EmptyStatePanel(
                        title = "还没有自动化",
                        subtitle = "说一句“当哔哩哔哩上有人回复我就立刻回复”，或在下面直接建一张草案。",
                        icon = Icons.Filled.CheckCircle,
                        contentPadding = JarvisSpacing.insetCardPadding,
                        modifier = Modifier.fillMaxWidth(),
                        action = {
                            Button(
                                onClick = cb.onCreateRuleStart,
                                modifier = Modifier.heightIn(min = JarvisSize.denseRowHeight),
                                contentPadding = PaddingValues(horizontal = JarvisSpacing.cardPadding)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null,
                                    modifier = Modifier.size(JarvisSize.toolbarIcon))
                                Spacer(Modifier.width(JarvisSpacing.space3))
                                Text("新建", style = JarvisType.titleItem)
                            }
                        }
                    )
            } else {
                BentoGap(JarvisSpacing.cardGap)
                    s.rules.forEachIndexed { index, rule ->
                        BentoPair(
                            a = if (index % 2 == 0) BentoWide else 1f - BentoWide,
                            first = {
                                Text(rule.title, style = JarvisType.titleItem, color = MaterialTheme.colorScheme.onSurface)
                                BentoMeta(rule.kindLabel)
                                if (rule.artist.isNotBlank()) BentoMeta("画师 " + rule.artist)
                                if (rule.lastRunAt > 0) BentoMeta(bentoClock(rule.lastRunAt) + " · " + rule.lastResult)
                                if (rule.readiness != null) NoticeLine(rule.readiness, tone = NoticeTone.ERROR)
                            },
                            second = {
                                SettingSwitchRow("启用", rule.enabled, { cb.onToggleRule(rule.id, it) })
                                BentoIconActions(
                                    BentoAct(Icons.Filled.Settings, "配置规则") { cb.onConfigureRule(rule.id) },
                                    BentoAct(Icons.Filled.Delete, "删除规则", JarvisActionTone.DANGER) { cb.onDeleteRule(rule.id) }
                                )
                            }
                        )
                    }
                    if (s.recentEvents.isNotEmpty()) {
                        JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                            SectionHeader("最近执行", action = { StatusBadge(s.recentEvents.size.toString(), BadgeTone.NEUTRAL) })
                            s.recentEvents.take(8).forEach { event ->
                                Text((if (event.ok) "✓ " else "! ") + event.title + " · " + event.message,
                                    style = JarvisType.bodyMeta,
                                    color = if (event.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    val configRule = s.rules.firstOrNull { it.id == s.editingRuleId }
                    if (configRule != null) {
                        JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                            SectionHeader("配置 " + configRule.title)
                            JarvisCardHeaderGap()
                            NoticeLine("原始要求：" + configRule.sourcePrompt)
                            BentoFields(
                                BentoF("Pixiv 画师名称或用户 ID", s.artistDraft, cb.onArtistDraftChange),
                                BentoF("回复风格要求", s.replyDraft, cb.onReplyDraftChange, lines = 3, mono = false)
                            )
                            JarvisCardHeaderGap()
                            BentoIconActions(
                                BentoAct(Icons.Filled.Save, "保存配置", JarvisActionTone.ACCENT) {
                                    cb.onSaveRuleConfig(configRule.id, s.artistDraft, s.replyDraft)
                                },
                                BentoAct(Icons.Filled.Close, "取消配置") { cb.onCancelRuleConfig() }
                            )
                        }
                    }
            }
                // 滚动内容末尾留 1 个 cardGap 的 slack：主 CTA 与最后一张卡不再恰好贴在折叠线上。
                Spacer(Modifier.height(JarvisSpacing.cardGap))
            }
        }
        BentoBar(ctx.shell, 1)
    }
}

/**
 * 新建规则草案卡（TasksState.creatingRule 分支）：标题 / 类型 / 触发要求 / 画师 / 错误。
 *
 * - 类型取 [TasksUiContext.TasksState.ruleDraftKindOptions]，chip 行由 key 反查下标，不做双向绑定；
 * - 画师字段**只在 Pixiv 类型出现**（key 含 PIXIV），其它类型连输入框都不画，避免无意义填项；
 * - [TasksUiContext.TasksState.ruleDraftError] 非空必须显示：校验失败由外壳回填错误串，这里不能静默。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BentoRuleDraft(
    s: TasksUiContext.TasksState,
    cb: TasksUiContext.TasksCallbacks,
    kindOptions: List<ChoiceOption>,
    pixiv: Boolean
) {
    JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
        SectionHeader("新建自动化", action = {
            StatusBadge(kindOptions.firstOrNull { it.key == s.ruleDraftKindKey }?.label ?: s.ruleDraftKindKey, BadgeTone.ACTIVE)
        })
        JarvisCardHeaderGap()
        BentoFields(BentoF("规则标题", s.ruleDraftTitle, cb.onCreateRuleTitleChange, "例：B 站新回复自动答", mono = false))
        if (kindOptions.isNotEmpty()) {
            BentoChips("规则类型", kindOptions.map { it.label },
                kindOptions.indexOfFirst { it.key == s.ruleDraftKindKey },
                { cb.onCreateRuleKindChange(kindOptions[it].key) })
        }
        BentoFields(
            BentoF("触发要求（sourcePrompt）", s.ruleDraftPrompt, cb.onCreateRulePromptChange,
                "例：当哔哩哔哩上有人回复我时，按设定语气回一句", mono = false, lines = 3)
        )
        if (pixiv) {
            BentoFields(BentoF("Pixiv 画师名 / 用户 ID", s.ruleDraftArtist, cb.onCreateRuleArtistChange, "可留空＝匹配任意画师"))
        }
        if (s.ruleDraftError.isNotBlank()) NoticeLine(s.ruleDraftError, tone = NoticeTone.ERROR)
        JarvisCardHeaderGap()
        BentoIconActions(
            BentoAct(Icons.Filled.Check, "创建规则", JarvisActionTone.ACCENT) { cb.onCreateRuleSubmit() },
            BentoAct(Icons.Filled.Close, "取消新建") { cb.onCreateRuleCancel() }
        )
    }
}


@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BentoWorkspace(ctx: WorkspaceUiContext) {
    val s = ctx.state
    val cb = ctx.callbacks
    var editing by remember { mutableStateOf<WorkspaceProfile?>(null) }
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var mentor by remember { mutableStateOf("") }
    var world by remember { mutableStateOf("") }
    var shared by remember { mutableStateOf(true) }
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var movingId by remember { mutableStateOf<String?>(null) }
    var previewFile by remember { mutableStateOf<String?>(null) }
    val current = s.workspaces.firstOrNull { it.name == s.selectedWorkspace }
        ?: s.archivedWorkspaces.firstOrNull { it.name == s.selectedWorkspace }
    Column(Modifier.fillMaxSize().bentoPage()) {
        Column(Modifier.fillMaxSize().weight(1f).jarvisScreenPadding()) {
            BentoHeader("工作区", subtitle = "归档、目录与对话都在这一页；归档只隐藏，不删任何数据。",
                leading = { BentoAvatarMark() },
                actions = {
                    JarvisIconRow(horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
                        JarvisIconAction(Icons.Filled.Add, "新建工作区",
                            { editing = null; creating = true; name = ""; mentor = ""; world = ""; shared = true },
                            tone = JarvisActionTone.ACCENT)
                    }
                    Spacer(Modifier.width(JarvisSpacing.space3))
                    StatusBadge(s.workspaces.size.toString() + " 格", BadgeTone.NEUTRAL)
                })
            BentoGap()
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(JarvisSpacing.cardGap)) {
                // 归档开关 + 目录绑定：两格只放静态行，不放 Lazy*（宽屏分支走 IntrinsicSize.Min 求高）。
                BentoPair(
                    first = {
                        BentoHead("归档", if (s.showArchived) "显示中" else "已隐藏", BadgeTone.NEUTRAL)
                        BentoSwitchLine("显示已归档（" + s.archivedWorkspaces.size + " 个）",
                            s.showArchived, cb.onToggleShowArchived)
                        JarvisIconRow(horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
                            s.archivedWorkspaces.take(3).forEach { archived ->
                                JarvisIconAction(Icons.Filled.Unarchive, "还原 " + archived.name,
                                    { cb.onUnarchive(archived.name) })
                            }
                        }
                    },
                    second = {
                        val bound = current?.hasDirectory == true
                        BentoHead(
                            "绑定目录",
                            when {
                                !bound -> "未绑定"
                                s.dirReady -> "可用"
                                else -> "需重选"
                            },
                            if (bound && s.dirReady) BadgeTone.ACTIVE else BadgeTone.WARNING
                        )
                        BentoMeta(current?.dirLabel?.ifBlank { "未绑定目录" } ?: "未绑定目录")
                        JarvisIconRow(horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
                            JarvisIconAction(Icons.Filled.FolderOpen, if (bound) "换绑定目录" else "选绑定目录",
                                { current?.let { cb.onPickDirectory(it.name) } }, enabled = current != null)
                            JarvisIconAction(Icons.Filled.LinkOff, "解绑目录",
                                { current?.let { cb.onClearDirectory(it.name) } }, enabled = bound)
                        }
                    }
                )
                s.workspaces.forEachIndexed { index, profile ->
                    val active = profile.name == s.selectedWorkspace
                    val count = s.conversationCountByWorkspace[profile.name]
                        ?: s.conversations.count { it.workspace == profile.name }
                    // 一个工作区只占**一张卡**：信息（名称/会话数/目录/进入）与操作（当前/编辑/归档/删除）
                    // 同框，宽屏卡内 0.62/0.38 分栏、窄屏卡内上下堆叠，不再拆成两张。
                    BentoPairCard(
                        a = if (index % 2 == 0) BentoWide else 1f - BentoWide,
                        info = {
                            Text(profile.name, style = JarvisType.titleItem, color = MaterialTheme.colorScheme.onSurface)
                            BentoMeta(count.toString() + " 个会话 · " +
                                if (profile.useGlobalPersona) "共同设定" else "自定义设定")
                            BentoMeta(if (profile.hasDirectory) "目录 " + profile.dirLabel.ifBlank { "已绑定" } else "未绑定目录")
                            if (profile.archived) BentoMeta("已归档 · 档案与对话都保留")
                            JarvisIconRow(horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
                                JarvisIconAction(Icons.AutoMirrored.Filled.Login, "进入 " + profile.name,
                                    { cb.onSelect(profile.name) }, tone = JarvisActionTone.ACCENT)
                            }
                        },
                        // 操作区**一行收口**：状态用图标+颜色，动作全部图标化，行高即 48dp。
                        ops = {
                            JarvisIconRow(Modifier.fillMaxWidth(), Arrangement.spacedBy(JarvisSpacing.space2)) {
                                BentoStateMark(
                                    when {
                                        profile.archived -> Icons.Filled.Archive
                                        active -> Icons.Filled.CheckCircle
                                        else -> Icons.Filled.Circle
                                    },
                                    when {
                                        profile.archived -> "已归档"
                                        active -> "当前工作区"
                                        else -> "未选中"
                                    },
                                    active
                                )
                                Spacer(Modifier.weight(1f))
                                JarvisIconAction(Icons.Filled.Edit, "编辑 " + profile.name, {
                                    editing = profile
                                    creating = false
                                    name = profile.name
                                    mentor = profile.mentorPrompt
                                    world = profile.worldBook
                                    shared = profile.useGlobalPersona
                                })
                                // 默认工作区不可归档（外壳拒绝），因此只在非默认工作区给归档入口。
                                if (profile.name != WorkspaceProfile.DEFAULT_NAME) {
                                    JarvisIconAction(
                                        if (profile.archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                        if (profile.archived) "还原 " + profile.name else "归档 " + profile.name,
                                        {
                                            if (profile.archived) cb.onUnarchive(profile.name)
                                            else cb.onArchive(profile.name)
                                        })
                                }
                                JarvisIconAction(Icons.Filled.Delete, "删除 " + profile.name,
                                    { cb.onDelete(profile.name) }, tone = JarvisActionTone.DANGER)
                            }
                        }
                    )
                }
                if (editing != null || creating) {
                    JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                        SectionHeader(if (creating) "新建工作区" else "编辑 " + (editing?.name ?: ""))
                        JarvisCardHeaderGap()
                        BentoFields(
                            BentoF("工作区名称", name,
                                { if (creating || editing?.name != WorkspaceProfile.DEFAULT_NAME) name = it.take(60) }, mono = false),
                            BentoF("人格 / 提示词", mentor, { mentor = it }, s.globalPersona, mono = false, lines = 3),
                            BentoF("世界书", world, { world = it }, s.globalWorldBook, mono = false, lines = 3)
                        )
                        if (s.personaSources.isNotEmpty()) {
                            BentoMeta("从其他对话粘贴")
                            JarvisIconRow(horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
                                s.personaSources.take(3).forEach { source ->
                                    BentoGlyphAction(Icons.Filled.ContentPaste, source.title, {
                                        mentor = source.mentorPrompt
                                        world = source.worldBook
                                    })
                                }
                            }
                        }
                        SettingSwitchRow("使用共同设定", shared, { shared = it },
                            subtitle = if (shared) "跟随设置中的全局人格与世界书" else "单独编辑人格与世界书")
                        JarvisCardHeaderGap()
                        // 保存/取消就在这张编辑卡里：不在编辑或新建态时整张卡都不渲染，
                        // 页面上因此不会出现「只有保存键」的空卡。
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BentoMeta(if (name.isBlank()) "填了名称才能保存" else "保存后立即生效")
                            Spacer(Modifier.weight(1f))
                            JarvisIconAction(Icons.Filled.Save, if (creating) "创建工作区" else "保存工作区", {
                                val payload = WorkspaceProfile(name.trim(), shared, mentor.trim(), world.trim())
                                cb.onSave(payload, if (creating) null else editing?.name)
                                editing = null
                                creating = false
                            }, enabled = name.isNotBlank(), tone = JarvisActionTone.ACCENT)
                            JarvisIconAction(Icons.Filled.Close, "取消编辑",
                                { editing = null; creating = false })
                        }
                    }
                }
                // 目录文件：只读视图。previewable 的行整行可点，展开显示外壳已读好的 previewText。
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("目录文件", Modifier.weight(1f), style = JarvisType.titleItem,
                            color = MaterialTheme.colorScheme.onSurface)
                        StatusBadge(s.files.size.toString() + " 项", BadgeTone.NEUTRAL)
                    }
                    BentoMeta(
                        if (s.dirReady) "只读清单，本轮不写文件"
                        else "未绑定或权限已失效，先用上面的目录图标重新绑定"
                    )
                    if (s.filesLoading) JarvisLoading("正在读取目录…")
                    if (s.filesNotice.isNotBlank()) NoticeLine(s.filesNotice)
                    if (s.files.isEmpty() && !s.filesLoading) {
                        BentoGap(JarvisSpacing.space3)
                        BentoMeta("目录为空，或还没有绑定目录。")
                    }
                    s.files.take(20).forEach { file ->
                        val expanded = previewFile == file.name
                        ListRow(
                            title = file.name,
                            subtitle = bentoFileMeta(file) + when {
                                !file.previewable -> ""
                                expanded -> " · 收起预览"
                                else -> " · 点击预览"
                            },
                            onClick = if (file.previewable) {
                                { previewFile = if (expanded) null else file.name }
                            } else null
                        )
                        if (expanded && file.previewText.isNotBlank()) {
                            Text(
                                file.previewText.take(2000),
                                style = JarvisType.codeInline,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().clip(JarvisShapes.sm)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(JarvisSpacing.space4)
                            )
                        }
                    }
                    if (s.files.size > 20) {
                        BentoMeta("只列前 20 项，共 " + s.files.size + " 项（外壳最多读 200 项）")
                    }
                }
                // 对话：每条都带改名 / 移动 / 归档（或还原）/ 删除入口，改名与移动在行内展开。
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("对话 " + s.conversations.size, Modifier.weight(1f), style = JarvisType.titleItem,
                            color = MaterialTheme.colorScheme.onSurface)
                        StatusBadge("归档 " + s.archivedConversationIds.size, BadgeTone.NEUTRAL)
                    }
                    BentoMeta("改名、移动、归档在每条对话下面；归档只隐藏，消息完整保留。")
                    if (s.conversations.isEmpty()) {
                        BentoGap(JarvisSpacing.space3)
                        BentoMeta("当前工作区没有对话。")
                    }
                    s.conversations.take(30).forEach { conversation ->
                        val archived = conversation.isArchived || conversation.id in s.archivedConversationIds
                        BentoGap(JarvisSpacing.space3)
                        Column(
                            Modifier.fillMaxWidth().heightIn(min = JarvisSize.rowMinHeight),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                conversation.title.ifBlank { "未命名对话" },
                                style = JarvisType.titleItem,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            BentoMeta(conversation.workspace + if (archived) " · 已归档" else "")
                        }
                        JarvisIconRow(horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
                            JarvisIconAction(
                                if (renamingId == conversation.id) Icons.Filled.Close else Icons.Filled.Edit,
                                if (renamingId == conversation.id) "收起改名" else "改名", {
                                    renamingId = if (renamingId == conversation.id) null else conversation.id
                                    renameText = conversation.title
                                })
                            JarvisIconAction(
                                if (movingId == conversation.id) Icons.Filled.Close else Icons.AutoMirrored.Filled.DriveFileMove,
                                if (movingId == conversation.id) "收起移动" else "移动", {
                                    movingId = if (movingId == conversation.id) null else conversation.id
                                })
                            JarvisIconAction(
                                if (archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                if (archived) "还原对话" else "归档对话", {
                                    if (archived) cb.onUnarchiveConversation(conversation.id)
                                    else cb.onArchiveConversation(conversation.id)
                                })
                            JarvisIconAction(Icons.Filled.Delete, "删除对话",
                                { cb.onDeleteConversation(conversation.id) }, tone = JarvisActionTone.DANGER)
                        }
                        if (renamingId == conversation.id) {
                            TextField(
                                value = renameText, onValueChange = { renameText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("新标题", style = JarvisType.labelBadge) },
                                singleLine = true, textStyle = JarvisType.bodyBase,
                                shape = JarvisShapes.sm, colors = bentoFieldColors()
                            )
                            JarvisIconAction(Icons.Filled.Check, "保存标题", {
                                if (renameText.isNotBlank()) {
                                    cb.onRenameConversation(conversation.id, renameText.trim())
                                    renamingId = null
                                }
                            }, enabled = renameText.isNotBlank(), tone = JarvisActionTone.ACCENT)
                        }
                        if (movingId == conversation.id) {
                            BentoMeta("移动到工作区")
                            JarvisIconRow(horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
                                s.workspaces.filter { it.name != conversation.workspace }.take(4).forEach { target ->
                                    BentoGlyphAction(Icons.AutoMirrored.Filled.DriveFileMove, target.name) {
                                        cb.onMoveConversation(conversation.id, target.name)
                                        movingId = null
                                    }
                                }
                            }
                        }
                    }
                    if (s.conversations.size > 30) {
                        BentoMeta("只列前 30 条，共 " + s.conversations.size + " 条")
                    }
                }
            }
        }
        BentoBar(ctx.shell, 2)
    }
}

/** 目录文件行副标题：大小（-1 表示提供方未给出）+ 最后修改时间；目录单独标出。 */
private fun bentoFileMeta(file: WorkspaceFileView): String {
    val size = when {
        file.isDirectory -> "目录"
        file.sizeBytes < 0L -> "大小未知"
        file.sizeBytes < 1024L -> file.sizeBytes.toString() + " B"
        else -> (file.sizeBytes / 1024L).toString() + " KB"
    }
    val time = if (file.lastModified > 0L) " · " + bentoClock(file.lastModified) else ""
    return size + time
}

private fun bentoRuntime(state: RuntimeState): Pair<String, BadgeTone> = when (state) {
    RuntimeState.NOT_CONFIGURED -> "未配置" to BadgeTone.NEUTRAL
    RuntimeState.READY -> "就绪" to BadgeTone.ACTIVE
    RuntimeState.RUNNING -> "运行中" to BadgeTone.ACTIVE
    RuntimeState.DEGRADED -> "降级" to BadgeTone.WARNING
    RuntimeState.ERROR -> "异常" to BadgeTone.ERROR
}

@Composable
private fun BentoSettings(ctx: SettingsUiContext) {
    val s = ctx.state
    val cb = ctx.callbacks
    val rt = bentoRuntime(s.bootstrapReport.state)
    val efforts = s.reasoningEfforts.ifEmpty { BentoEfforts }
    var pluginDraft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().bentoPage()) {
        Column(Modifier.fillMaxSize().weight(1f).jarvisScreenPadding()) {
            BentoHeader("设置", subtitle = "Jarvis " + s.versionName + " · 模型、权限与设备连接",
                leading = { BentoAvatarMark() },
                actions = {
                    JarvisIconRow(horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
                        JarvisIconAction(Icons.Filled.Refresh, "刷新运行时", cb.onRefreshRuntime)
                    }
                    Spacer(Modifier.width(JarvisSpacing.space3))
                    StatusBadge(rt.first, rt.second)
                })
            BentoGap()
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(JarvisSpacing.cardGap)) {
                BentoPair(
                    first = {
                        BentoHead("风格与外观", s.themeId.ifBlank { s.theme.label },
                            if (s.themeId.isBlank()) BadgeTone.NEUTRAL else BadgeTone.ACTIVE)
                        if (s.themeOptions.isEmpty()) {
                            BentoMeta("主题清单待外壳注入")
                        } else {
                            Spacer(Modifier.height(JarvisSpacing.space3))
                            BentoChipRow(s.themeOptions.map { it.label },
                                s.themeOptions.indexOfFirst { it.id == s.themeId },
                                { cb.onThemeChangeById(s.themeOptions[it].id) })
                            Spacer(Modifier.height(JarvisSpacing.space3))
                            s.themeOptions.firstOrNull { it.id == s.themeId }?.let { current ->
                                BentoGap(JarvisSpacing.space2)
                                BentoMeta(current.story + " · " + current.modeKey)
                            }
                        }
                    },
                    second = {
                        BentoHead("限制模式", if (s.restrictionMode) "开" else "关",
                            if (s.restrictionMode) BadgeTone.ACTIVE else BadgeTone.WARNING)
                        BentoSwitchLine("关闭后会持续显示警告状态", s.restrictionMode, cb.onRestrictionChange)
                    }
                )
                BentoPair(
                    a = 1f - BentoWide,
                    first = {
                        BentoHead("root 模式", s.rootModeKey, BadgeTone.WARNING)
                        Spacer(Modifier.height(JarvisSpacing.space3))
                        BentoChipRow(s.rootModes.map { it.label },
                            s.rootModes.indexOfFirst { it.key == s.rootModeKey },
                            { cb.onRootModeRequest(s.rootModes[it]) })
                        Spacer(Modifier.height(JarvisSpacing.space3))
                    },
                    second = {
                        BentoHead("命令通道", s.commandPrefix,
                            if (s.commandChannelEnabled) BadgeTone.ACTIVE else BadgeTone.NEUTRAL)
                        BentoSwitchLine("首版默认只做预览，不直接执行", s.commandChannelEnabled, cb.onCommandChange)
                        BentoFields(BentoF("命令前缀", s.commandPrefix, cb.onPrefixChange))
                    }
                )
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    SectionHeader("模型与识图", action = {
                        StatusBadge(if (s.modelName.isBlank()) "未配置" else s.modelName,
                            if (s.modelName.isBlank()) BadgeTone.WARNING else BadgeTone.ACTIVE)
                    })
                    JarvisCardHeaderGap()
                    BentoFields(
                        BentoF("Base URL", s.modelBaseUrl, cb.onModelBaseUrlChange, "https://api.openai.com/v1"),
                        BentoF("模型名", s.modelName, cb.onModelNameChange, "gpt-4o-mini"),
                        BentoF("API Key", s.modelApiKey, cb.onModelApiKeyChange, secret = true),
                        BentoF("模型回退链（每行一个，可选）", s.modelChain, cb.onModelChainChange,
                            "主模型失败时按顺序尝试", lines = 2),
                        BentoF("深度思考模型（可选）", s.deepThinkingModel, cb.onDeepThinkingModelChange, "留空则使用主模型"),
                        BentoF("识图 Base URL", s.visionBaseUrl, cb.onVisionBaseUrlChange, "留空继承主模型"),
                        BentoF("识图模型名", s.visionModel, cb.onVisionModelChange, "gpt-4o-mini"),
                        BentoF("识图 API Key", s.visionApiKey, cb.onVisionApiKeyChange, "留空继承主模型", secret = true)
                    )
                    SettingSwitchRow("深度思考", s.deepThinkingEnabled, cb.onDeepThinkingChange,
                        subtitle = "关闭时不发送 reasoning_effort；开启后由下方档位控制")
                    BentoChips("思考深度 " + s.reasoningEffort, efforts,
                        efforts.indexOf(s.reasoningEffort.lowercase()), { cb.onReasoningEffortChange(efforts[it]) })
                }
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    SectionHeader("远程访问", action = {
                        StatusBadge(if (s.serverRunning) "运行中" else "已停止",
                            if (s.serverRunning) BadgeTone.ACTIVE else BadgeTone.NEUTRAL)
                    })
                    JarvisCardHeaderGap()
                    if (s.pairingCode.isNotBlank()) {
                        Text(s.pairingCode, style = JarvisType.codeInline, color = MaterialTheme.colorScheme.primary)
                        if (s.pairingExpiresAt > 0) BentoMeta("配对码到期 " + bentoClock(s.pairingExpiresAt))
                    } else {
                        BentoMeta("当前没有有效配对码")
                    }
                    s.remoteUrls.forEach { BentoMeta(it) }
                    SettingSwitchRow("允许远程设备调用本机能力", s.remoteDeviceControl, cb.onRemoteDeviceControlChange)
                    BentoIconActions(
                        BentoAct(Icons.Filled.PlayArrow, "启动远程访问", JarvisActionTone.ACCENT) { cb.onStartServer() },
                        BentoAct(Icons.Filled.Stop, "停止远程访问") { cb.onStopServer() },
                        BentoAct(Icons.Filled.Key, "生成新配对码") { cb.onNewPairingCode() },
                        BentoAct(Icons.Filled.ContentCopy, "复制配对码") { cb.onCopyPairingCode() },
                        BentoAct(Icons.Filled.LinkOff, "撤销配对") { cb.onRevokePairing() },
                        BentoAct(Icons.Filled.Refresh, "刷新地址") { cb.onRefreshRemoteUrls() }
                    )
                }
                BentoPair(
                    a = 1f - BentoWide,
                    first = {
                        BentoHead("运行时工具", if (s.checkingTools) "检查中" else s.runtimeTools.size.toString() + " 项",
                            if (s.checkingTools) BadgeTone.WARNING else BadgeTone.NEUTRAL)
                        if (s.checkingTools) JarvisLoading("正在检查…")
                        s.runtimeTools.forEach { tool ->
                            BentoMeta(tool.name + " · " + tool.version.ifBlank { if (tool.installed) "已可用" else "未找到" })
                        }
                        s.tools.filter { it.installPackage.isNotBlank() }.take(2).forEach { tool ->
                            BentoGlyphAction(Icons.Filled.Download, tool.label) { cb.onInstallTool(tool.id) }
                        }
                        BentoIconActions(
                            BentoAct(Icons.Filled.Refresh, "刷新工具列表") { cb.onRefreshTools() },
                            BentoAct(Icons.Filled.Search, "重新检查工具") { cb.onCheckTools() }
                        )
                        if (s.toolNotice.isNotBlank()) NoticeLine(s.toolNotice)
                    },
                    second = {
                        BentoHead("操作记忆", s.memoryEntries.size.toString() + " 条", BadgeTone.NEUTRAL)
                        if (s.memoryStatus.isNotBlank()) NoticeLine(s.memoryStatus)
                        s.memoryEntries.forEach { entry ->
                            Text(entry.title, style = JarvisType.titleItem, color = MaterialTheme.colorScheme.onSurface)
                            BentoMeta("成功 " + entry.successes + " · 失败 " + entry.failures +
                                if (entry.ready) " · 可复用" else " · 需重试")
                            JarvisIconAction(Icons.Filled.Delete, "删除这条记忆",
                                { cb.onDeleteMemoryEntry(entry.id) }, tone = JarvisActionTone.DANGER)
                        }
                        BentoIconActions(BentoAct(Icons.Filled.Refresh, "刷新操作记忆") { cb.onRefreshMemory() })
                    }
                )
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    SectionHeader("社区插件", action = {
                        StatusBadge(s.plugins.size.toString() + " 个",
                            if (s.pluginBusy) BadgeTone.WARNING else BadgeTone.NEUTRAL)
                    })
                    JarvisCardHeaderGap()
                    if (s.pluginBusy) JarvisLoading("正在处理插件…")
                    s.plugins.forEach { plugin ->
                        SettingSwitchRow(
                            title = plugin.name,
                            checked = plugin.enabled,
                            onCheckedChange = { cb.onTogglePlugin(plugin.id, it) },
                            subtitle = plugin.version + " · " + plugin.source +
                                if (plugin.tools.isEmpty()) "" else " · " + plugin.tools.joinToString(" / ")
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BentoMeta(plugin.id)
                            Spacer(Modifier.weight(1f))
                            JarvisIconAction(Icons.Filled.DeleteForever, "卸载插件 " + plugin.name,
                                { cb.onUninstallPlugin(plugin.id) }, tone = JarvisActionTone.DANGER)
                        }
                    }
                    BentoGap(JarvisSpacing.cardGap)
                    BentoFields(BentoF("粘贴插件草稿", pluginDraft, { pluginDraft = it }, "把插件清单/草稿文本粘贴到这里再安装", lines = 4))
                    BentoIconActions(
                        BentoAct(Icons.Filled.Upload, "安装插件", JarvisActionTone.ACCENT, pluginDraft.isNotBlank()) {
                            cb.onInstallPlugin(pluginDraft)
                        },
                        BentoAct(Icons.Filled.Refresh, "刷新插件列表") { cb.onRefreshPlugins() }
                    )
                    if (s.pluginNotice.isNotBlank()) NoticeLine(s.pluginNotice)
                }
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    SectionHeader("悬浮球与手势", action = {
                        StatusBadge(if (s.floatingEnabled) "开" else "关",
                            if (s.floatingEnabled) BadgeTone.ACTIVE else BadgeTone.NEUTRAL)
                    })
                    JarvisCardHeaderGap()
                    BentoSwitchLine("尺寸 " + s.floatingSizeDp + " dp · 隐藏 " + s.avatarHideSeconds + " 秒",
                        s.floatingEnabled, cb.onFloatingEnabledChange)
                    if (!s.overlayGranted) {
                        BentoIconActions(BentoAct(Icons.Filled.Layers, "请求悬浮窗权限", JarvisActionTone.ACCENT) {
                            cb.onOverlayPermissionRequest()
                        })
                    }
                    BentoChips("头像边长 " + s.avatarSizeDp + " dp", s.avatarSizeChoices.map { it.toString() },
                        s.avatarSizeChoices.indexOf(s.avatarSizeDp),
                        { cb.onAvatarSizeChange(s.avatarSizeChoices[it]); cb.onFloatingSizeChange(s.avatarSizeChoices[it]) })
                    BentoChips("形状 " + s.floatingShapeKey, s.floatingShapes.map { it.label },
                        s.floatingShapes.indexOfFirst { it.key == s.floatingShapeKey },
                        { cb.onFloatingShapeChange(s.floatingShapes[it].key) })
                    BentoChips("隐藏秒数 " + s.avatarHideSeconds, s.hideSecondChoices.map { it.toString() },
                        s.hideSecondChoices.indexOf(s.avatarHideSeconds),
                        { cb.onAvatarHideSecondsChange(s.hideSecondChoices[it]) })
                    JarvisCardHeaderGap()
                    BentoChips("双击 " + s.doubleGestureKey, s.gestureOptions.map { it.label },
                        s.gestureOptions.indexOfFirst { it.key == s.doubleGestureKey },
                        { cb.onDoubleGestureChange(s.gestureOptions[it].key) })
                    BentoFields(BentoF("双击任务文本", s.doubleTask, cb.onDoubleTaskChange, mono = false, lines = 2))
                    BentoChips("三击 " + s.tripleGestureKey, s.gestureOptions.map { it.label },
                        s.gestureOptions.indexOfFirst { it.key == s.tripleGestureKey },
                        { cb.onTripleGestureChange(s.gestureOptions[it].key) })
                    BentoFields(BentoF("三击任务文本", s.tripleTask, cb.onTripleTaskChange, mono = false, lines = 2))
                    if (s.savedAutomations.isNotEmpty()) {
                        JarvisIconRow(horizontalArrangement = Arrangement.spacedBy(JarvisSpacing.space2)) {
                            s.savedAutomations.take(2).forEach { rule ->
                                BentoGlyphAction(Icons.Filled.PlayArrow, rule.title) {
                                    cb.onSelectAutomationForGesture("double", rule.id)
                                }
                            }
                        }
                    }
                }
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    SectionHeader("无障碍与人格", action = {
                        StatusBadge(if (s.accessibilityGranted) "已连接" else "未连接",
                            if (s.accessibilityGranted) BadgeTone.ACTIVE else BadgeTone.WARNING)
                    })
                    JarvisCardHeaderGap()
                    if (s.accessibilityStatus.isNotBlank()) BentoMeta(s.accessibilityStatus)
                    SettingSwitchRow("打开 Jarvis 时自动连接", s.accessibilityAutoConnect, cb.onAccessibilityAutoConnectChange)
                    BentoIconActions(BentoAct(Icons.Filled.Accessibility, "打开无障碍设置") { cb.onAccessibilitySettingsOpen() })
                    JarvisCardHeaderGap()
                    BentoFields(
                        BentoF("全局提示词", s.mentorPrompt, cb.onMentorPromptChange, mono = false, lines = 3),
                        BentoF("全局世界书", s.worldBook, cb.onWorldBookChange, mono = false, lines = 3)
                    )
                }
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    SectionHeader("AI Classroom 与运行时", action = { StatusBadge(rt.first, rt.second) })
                    JarvisCardHeaderGap()
                    BentoIconActions(
                        BentoAct(Icons.Filled.Upload, "导入 AI Classroom") { cb.onImportAiClassroom() },
                        BentoAct(Icons.Filled.Download, "导出 AI Classroom") { cb.onExportAiClassroom() }
                    )
                    if (s.importNotice.isNotBlank()) NoticeLine(s.importNotice, tone = NoticeTone.SUCCESS)
                    if (s.exportNotice.isNotBlank()) NoticeLine(s.exportNotice, tone = NoticeTone.SUCCESS)
                    BentoMeta(s.bootstrapReport.runtimeMode.name + " · " + s.bootstrapReport.runtimeVersion)
                    BentoMeta(s.bootstrapReport.pluginDirectory)
                    NoticeLine(s.bootstrapReport.message)
                    if (s.runtimeMessage.isNotBlank()) NoticeLine(s.runtimeMessage)
                    if (s.configNotice.isNotBlank()) NoticeLine(s.configNotice)
                    BentoIconActions(
                        BentoAct(Icons.Filled.Refresh, "刷新运行时") { cb.onRefreshRuntime() },
                        BentoAct(Icons.Filled.PlayArrow, "启动运行时", JarvisActionTone.ACCENT) { cb.onStartRuntime() },
                        BentoAct(Icons.Filled.Stop, "停止运行时") { cb.onStopRuntime() },
                        BentoAct(Icons.Filled.Restore, "恢复运行时") { cb.onRecovery() }
                    )
                }
                JarvisCard(modifier = Modifier.fillMaxWidth(), contentPadding = JarvisSpacing.cardPadding) {
                    SectionHeader("保存并应用", action = {
                        StatusBadge(if (s.saving) "保存中" else if (s.applyFailed) "失败" else "就绪",
                            if (s.saving) BadgeTone.WARNING else if (s.applyFailed) BadgeTone.ERROR else BadgeTone.ACTIVE)
                    })
                    if (s.saving) JarvisLoading("正在保存并应用…")
                    if (s.applyNotice.isNotBlank()) NoticeLine(s.applyNotice,
                        tone = if (s.applyFailed) NoticeTone.ERROR else NoticeTone.SUCCESS)
                    JarvisCardHeaderGap()
                    BentoIconActions(
                        BentoAct(Icons.Filled.Save, "保存并应用", JarvisActionTone.ACCENT) { cb.onApply() },
                        BentoAct(Icons.Filled.Check, "保存并验证") { cb.onApplyAndVerify() }
                    )
                }
            }
        }
        BentoBar(ctx.shell, 3)
    }
}
