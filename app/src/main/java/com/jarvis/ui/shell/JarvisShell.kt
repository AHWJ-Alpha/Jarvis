package com.jarvis.ui.shell

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.jarvis.app.JarvisBrandMark
import com.jarvis.ui.components.LocalJarvisDividerColor
import com.jarvis.ui.theme.ChatUiContext
import com.jarvis.ui.theme.ChromeStyle
import com.jarvis.ui.theme.JarvisThemeModule
import com.jarvis.ui.theme.LocalJarvisMode
import com.jarvis.ui.theme.SettingsUiContext
import com.jarvis.ui.theme.ShellUiContext
import com.jarvis.ui.theme.TasksUiContext
import com.jarvis.ui.theme.WorkspaceUiContext
import com.jarvis.ui.theme.ThemeMode
import com.jarvis.ui.theme.colorScheme
import com.jarvis.ui.tokens.JarvisMotion
import com.jarvis.ui.tokens.JarvisShapes
import com.jarvis.ui.tokens.JarvisSize
import com.jarvis.ui.tokens.JarvisStroke
import com.jarvis.ui.tokens.JarvisTypography

/**
 * 外壳的顶栏/抽屉/横屏侧栏装配参数。
 *
 * 外壳自己不做任何业务判断：标题、限制模式状态、抽屉与侧栏的开关都由此处传入，
 * 这样 MainActivity 仍然是唯一的状态所有者。
 */
internal data class ShellChrome(
    /** 顶栏主标题（一般是当前会话标题，空时回落到工作区名）。 */
    val title: String = "",
    /** 顶栏副标题（品牌与权限档位）。 */
    val subtitle: String = "",
    /** 当前是否处于限制模式，决定顶栏右侧图标。 */
    val restrictionMode: Boolean = true,
    /** 点击顶栏限制模式图标；外壳不再有裸 Icon 死区。 */
    val onRestrictionChange: () -> Unit = {},
    /** 竖屏抽屉是否展开（仅 [ChromeStyle.DRAWER] 主题使用）。 */
    val drawerOpen: Boolean = false,
    val onDrawerOpenChange: (Boolean) -> Unit = {},
    /** 横屏左侧常驻栏是否展开（仅 [ChromeStyle.DRAWER] 主题有左栏）。 */
    val railVisible: Boolean = true,
    val onRailVisibleChange: (Boolean) -> Unit = {},
    /** 主题自绘底栏的高度；外壳据此把底部渐变贴在栏沿而不是压在栏上。 */
    val bottomBarHeight: Dp = JarvisSize.bottomBarHeight,
    /**
     * 主题是否在内容区内自绘底栏。
     *
     * [ChromeStyle.BOTTOM_BAR] 的 5 套主题一律自绘（外壳按 chrome 自动认，调用方不必传），
     * [ChromeStyle.DRAWER] 主题默认没有底栏，只有显式传 true 时才在底部渐变下方预留 [bottomBarHeight]。
     * 自绘底栏是内容里的**在流**元素（主题 Column 的最后一项），不是覆盖层：这个布尔只决定
     * 「底部渐变贴在哪条栏沿上」，外壳不据此给内容加 padding——加了会把底栏顶起来、栏下留一条空带。
     */
    val selfDrawnBottomBar: Boolean = false
)

/**
 * 唯一外壳。负责四件事：注入 MaterialTheme 与分隔线颜色、按 [JarvisThemeModule.chrome] 画外层
 * 导航、把四个 UiContext 分发给主题屏、承载与主题无关的对话框。
 *
 * 底栏二选一：[ChromeStyle.BOTTOM_BAR] 的 5 套主题自己画底栏（二次接线时用
 * [ShellUiContext.onNavigate] 接点击），外壳只给内容区，避免两层底栏叠在一起。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JarvisShell(
    module: JarvisThemeModule,
    shell: ShellUiContext,
    chat: ChatUiContext,
    tasks: TasksUiContext,
    workspace: WorkspaceUiContext,
    settings: SettingsUiContext,
    chrome: ShellChrome = ShellChrome(),
    drawerContent: @Composable () -> Unit = {},
    dialogs: @Composable () -> Unit = {}
) {
    // ColorScheme/Shapes 必须按色板缓存：colorScheme() 每次调用都新建一个 36 键 ColorScheme，
    // 而 ColorScheme 没有值相等语义，于是外壳每重组一次都会把所有读 MaterialTheme 的地方
    // （每个 Text/Icon/卡片的取色）判成「值变了」而整棵树失效——这才是切换与滚动时掉帧的放大器。
    val palette = module.palette
    val colorScheme = remember(palette) { palette.colorScheme() }
    val materialShapes = remember { JarvisShapes.materialShapes() }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = JarvisTypography,
        shapes = materialShapes
    ) {
        CompositionLocalProvider(
            LocalJarvisDividerColor provides palette.divider,
            // 主题内部的「此刻用哪套色板」只认这里，不再从 colorScheme 反查（见 LocalJarvisMode）。
            LocalJarvisMode provides module.mode
        ) {
            // 系统栏（状态栏/导航栏）外观的唯一写入点。styles.xml 里 windowLightStatusBar=true 只是
            // 启动瞬间的默认值，真正生效的明暗必须跟随当前主题模块，否则深色主题会沿用「浅底+深图标」。
            ApplySystemBarAppearance(module)
            // 渐变一律传不透明页面底色，0.92 → 0 的 alpha 由 JarvisScrim 负责。
            val pageBackground = MaterialTheme.colorScheme.background
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                val screen: @Composable () -> Unit = { ShellScreenSlot(module, shell, chat, tasks, workspace, settings) }
                // 底部有没有主题自绘的栏：BOTTOM_BAR 5 套自动为 true，DRAWER 看显式声明。
                // 它只控制「底部渐变贴边 + 预留栏高」，不参与内容高度——内容高度由主题 Column + weight 决定。
                val themeDrawsBottomBar =
                    chrome.selfDrawnBottomBar || module.chrome == ChromeStyle.BOTTOM_BAR
                if (module.chrome == ChromeStyle.DRAWER) {
                    // 顶栏自己吃状态栏 inset，内容区再按 safeDrawing 让开导航栏与刘海。
                    Scaffold(
                        // 容器交给外层 Surface 的 background 上色；这一处既不是渐变末端色，也不参与栏沿过渡。
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        topBar = { ShellTopBar(chrome, landscape, module.artwork.avatar) }
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            ShellContent(landscape, chrome, drawerContent, screen)
                            // 顶栏下沿淡出（内容滚到顶栏下时的过渡）。
                            Box(Modifier.align(Alignment.TopCenter)) { ShellTopFade(pageBackground) }
                            // 内容底沿淡入：DRAWER 主题没有自绘底栏也要接住内容，有底栏时才留出栏高。
                            Column(Modifier.align(Alignment.BottomCenter)) {
                                ShellBottomFade(pageBackground)
                                if (themeDrawsBottomBar) Spacer(Modifier.height(chrome.bottomBarHeight))
                            }
                        }
                    }
                    if (!landscape) ShellDrawer(chrome, drawerContent, pageBackground)
                } else {
                    // 底栏主题没有顶栏；横屏也不给左栏，否则和主题底栏叠成两套导航。
                    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                        ShellContent(landscape, chrome, drawerContent, screen, showRail = false)
                        // 状态栏下方内容淡出（BOTTOM_BAR 主题没有顶栏，这里就是栏沿收口）。
                        Box(Modifier.align(Alignment.TopCenter)) { ShellTopFade(pageBackground) }
                        // 底栏由主题自己画，栏沿上方铺向上渐隐，再接住栏高。
                        Column(Modifier.align(Alignment.BottomCenter)) {
                            ShellBottomFade(pageBackground)
                            if (themeDrawsBottomBar) Spacer(Modifier.height(chrome.bottomBarHeight))
                        }
                    }
                }
            }
            // 对话框 / 浮层槽同样两端收口，避免槽内整屏浮层贴边出现硬边。
            Box(Modifier.fillMaxSize()) {
                dialogs()
                Box(Modifier.align(Alignment.TopCenter)) { ShellTopFade(pageBackground) }
                Box(Modifier.align(Alignment.BottomCenter)) { ShellBottomFade(pageBackground) }
            }
        }
    }
}

/**
 * 按主题模块的明暗设置状态栏/导航栏外观与栏底色。
 *
 * 图标明暗只认 [JarvisThemeModule.mode]：DARK 用浅色图标，LIGHT 用深色图标。
 * 栏底色同步成主题背景，覆盖 styles.xml 的浅色 jarvis_mist，避免非全面屏设备上出现一条浅色栏。
 */
@Composable
private fun ApplySystemBarAppearance(module: JarvisThemeModule) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: view.context.findActivity()?.window
    val light = module.mode == ThemeMode.LIGHT
    SideEffect {
        val target = window ?: return@SideEffect
        WindowCompat.getInsetsController(target, view).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
        @Suppress("DEPRECATION")
        target.statusBarColor = module.palette.background.toArgb()
        @Suppress("DEPRECATION")
        target.navigationBarColor = module.palette.background.toArgb()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** 横屏「左侧常驻栏 + 右侧内容」，竖屏直接给内容；[showRail] 为 false 时横屏也只有内容。 */
@Composable
private fun ShellContent(
    landscape: Boolean,
    chrome: ShellChrome,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
    showRail: Boolean = true
) {
    if (!landscape || !showRail) {
        content()
        return
    }
    Row(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = chrome.railVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(JarvisMotion.Base)) + fadeIn(tween(JarvisMotion.Fast)),
            exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(JarvisMotion.Base)) + fadeOut(tween(JarvisMotion.Fast))
        ) {
            Row {
                Box(Modifier.width(280.dp).fillMaxHeight()) { drawerContent() }
                ShellVerticalDivider()
            }
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun ShellVerticalDivider() {
    // 线宽用 [JarvisStroke.hairline]（0.5dp），不再写字面量。
    val width = JarvisStroke.hairline
    Box(Modifier.width(width).fillMaxHeight().background(LocalJarvisDividerColor.current ?: MaterialTheme.colorScheme.outlineVariant))
}

/** 屏分发：tabIndex 0..3，过渡沿用 [JarvisMotion.Base]。 */
@Composable
private fun ShellScreenSlot(
    module: JarvisThemeModule,
    shell: ShellUiContext,
    chat: ChatUiContext,
    tasks: TasksUiContext,
    workspace: WorkspaceUiContext,
    settings: SettingsUiContext
) {
    Crossfade(targetState = shell.tabIndex, animationSpec = tween(JarvisMotion.Base), label = "jarvis-screen") { index ->
        when (index) {
            1 -> module.TasksScreen(tasks)
            2 -> module.WorkspaceScreen(workspace)
            3 -> module.SettingsScreen(settings)
            else -> module.ChatScreen(chat)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellTopBar(chrome: ShellChrome, landscape: Boolean, avatarRes: Int) {
    TopAppBar(
        // 栏高收敛到 [JarvisSize.topBarHeight]（52dp，不含状态栏 inset），不再用 M3 默认 64dp。
        expandedHeight = JarvisSize.topBarHeight,
        // 栏面半透明（[ShellBarAlpha]）：氛围色能透出来，栏底再有渐变接内容。
        colors = TopAppBarDefaults.topAppBarColors(containerColor = shellBarColor()),
        navigationIcon = {
            IconButton(onClick = {
                if (landscape) chrome.onRailVisibleChange(!chrome.railVisible) else chrome.onDrawerOpenChange(true)
            }) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = if (landscape) "收起或展开左侧菜单" else "打开导航",
                    modifier = Modifier.size(JarvisSize.toolbarIcon)
                )
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JarvisBrandMark(avatarRes = avatarRes, size = 34)
                Spacer(Modifier.width(12.dp))
                // weight(1f) 让标题列吃到「图标之后剩余宽度」，配合 Ellipsis 才是省略号而不是硬裁。
                Column(Modifier.weight(1f, fill = false)) {
                    Text(
                        text = chrome.title.ifBlank { "Jarvis" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (chrome.subtitle.isNotBlank()) {
                        Text(
                            text = chrome.subtitle,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = chrome.onRestrictionChange) {
                if (chrome.restrictionMode) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = "限制模式已开启，点击关闭",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(JarvisSize.toolbarIcon)
                    )
                } else {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "限制模式已关闭，点击开启",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(JarvisSize.toolbarIcon)
                    )
                }
            }
        }
    )
}

/** 竖屏抽屉：遮罩点击与返回键都能关，动效沿用 [JarvisMotion]；抽屉面板上下两端同样用 [JarvisScrim] 收口。 */
@Composable
private fun ShellDrawer(chrome: ShellChrome, drawerContent: @Composable () -> Unit, pageBackground: Color) {
    BackHandler(enabled = chrome.drawerOpen) { chrome.onDrawerOpenChange(false) }
    AnimatedVisibility(
        visible = chrome.drawerOpen,
        enter = fadeIn(tween(JarvisMotion.Fast)) + slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(JarvisMotion.Base)),
        exit = fadeOut(tween(JarvisMotion.Fast)) + slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(JarvisMotion.Base))
    ) {
        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))) {
            Box(Modifier.widthIn(max = 320.dp).fillMaxHeight()) {
                drawerContent()
                // 抽屉覆盖层自绘面板：顶/底两端各铺一条渐变，列表滚到面板边缘时不出现硬切线。
                Box(Modifier.align(Alignment.TopCenter)) { ShellTopFade(pageBackground) }
                Box(Modifier.align(Alignment.BottomCenter)) { ShellBottomFade(pageBackground) }
            }
            Spacer(
                Modifier.weight(1f).fillMaxHeight().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { chrome.onDrawerOpenChange(false) }
            )
        }
    }
}
