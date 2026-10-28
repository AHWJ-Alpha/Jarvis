package com.jarvis.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 主题插画引用，均为 R.drawable。 */
data class ThemeArtwork(val banner: Int, val avatar: Int, val texture: Int? = null)

/** 外层导航形态。 */
enum class ChromeStyle { DRAWER, BOTTOM_BAR }

/**
 * 浮窗配色。FloatingAssistantService.kt 是纯 View 层，用不了 Compose 的 ColorScheme，现在写死
 * 0xff172438 / 0xffeff6ff / 0xffb8c9dd / 0xff8de3cf；这里给出一份可以直接传给它的取值规格。
 */
data class FloatingPaletteSpec(
    val panel: Color,
    val title: Color,
    val body: Color,
    val accent: Color,
    val muted: Color,
    val bubbleRing: Color
)

/**
 * 一套主题的完整定义。阶段 2 的每套主题实现本接口，阶段 3 由外壳统一装配。
 *
 * 明暗双模式（本阶段新增）：
 * - 主题作者只做一件事：为「另一侧」补 [darkPalette] / [lightPalette]。两套色板都由作者手写，
 *   不做任何明暗自动推导。未填（null）的一侧自动回落到 [palette]，所以只写一套色板的老主题
 *   一行不改也能编译、也能跟随系统切换（跟随系统时只是换到回落的同一套色板）。
 * - [mode] 保留，但它现在只是该主题的「身份底色」（主题卡片上的徽标、既有调用点的默认值），
 *   不再表示实际渲染的明暗。实际明暗 = 用户偏好（跟随系统/浅色/深色）经 resolveMode 解析所得，
 *   见 ThemeModeOverride 与 resolveMode。
 */
internal interface JarvisThemeModule {
    val id: String
    val label: String
    val story: String
    val description: String

    /** 身份底色：该主题「原生」的明暗，用于徽标与旧调用点，不是运行期实际明暗。 */
    val mode: ThemeMode

    /** 主色板：主题作者原本写的那一套，也是 [darkPalette]/[lightPalette] 缺省时的回落目标。 */
    val palette: ThemePalette

    /** 暗色专属色板；null 表示沿用 [palette]。主题作者手写，不许明暗推导。 */
    val darkPalette: ThemePalette? get() = null

    /** 浅色专属色板；null 表示沿用 [palette]。主题作者手写，不许明暗推导。 */
    val lightPalette: ThemePalette? get() = null

    /**
     * 按实际生效的明暗取色板：填了对应一侧就用它，没填就回落到 [palette]。
     *
     * 与同名的 [palette] 属性并存：属性是「作者写的那一套」，本函数是「此刻该用的那一套」。
     */
    fun palette(mode: ThemeMode): ThemePalette = when (mode) {
        ThemeMode.DARK -> darkPalette ?: this.palette
        ThemeMode.LIGHT -> lightPalette ?: this.palette
    }

    /** 该主题的默认明暗（= 身份底色 [mode]）；外壳应改用运行期解析后的实际明暗。 */
    val defaultMode: ThemeMode get() = mode

    val artwork: ThemeArtwork
    val chrome: ChromeStyle

    fun floatingPalette(): FloatingPaletteSpec

    @Composable fun ChatScreen(ctx: ChatUiContext)
    @Composable fun TasksScreen(ctx: TasksUiContext)
    @Composable fun WorkspaceScreen(ctx: WorkspaceUiContext)
    @Composable fun SettingsScreen(ctx: SettingsUiContext)
}

/**
 * 把「运行期实际明暗」套到主题上：返回一个只覆盖 [JarvisThemeModule.mode] 与
 * [JarvisThemeModule.palette] 的包装模块，其余成员原样转发。
 *
 * 外壳（ui/shell）仍然只读 module.palette / module.mode，因此装配层只要把包装后的模块传下去，
 * 状态栏图标颜色与整套配色就会一起跟随系统切换，外壳代码不需要任何改动。
 */
internal fun JarvisThemeModule.forMode(resolved: ThemeMode): JarvisThemeModule {
    val base = this
    return object : JarvisThemeModule by base {
        override val mode: ThemeMode get() = resolved
        override val palette: ThemePalette get() = base.palette(resolved)
        override val darkPalette: ThemePalette? get() = base.darkPalette
        override val lightPalette: ThemePalette? get() = base.lightPalette
        override val defaultMode: ThemeMode get() = resolved
    }
}
