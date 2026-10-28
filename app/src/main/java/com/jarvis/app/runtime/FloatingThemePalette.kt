package com.jarvis.app.runtime

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.toArgb
import com.jarvis.ui.theme.FloatingPaletteSpec
import com.jarvis.ui.theme.JarvisThemeModule
import com.jarvis.ui.theme.LegacyThemeMigration
import com.jarvis.ui.theme.ThemeMode
import com.jarvis.ui.theme.ThemeModeOverride
import com.jarvis.ui.theme.resolveMode
import com.jarvis.ui.themes.JarvisThemes

/**
 * 浮窗是纯 View 层，拿不到 Compose 的 ColorScheme。[FloatingThemeColors] 把主题模块给出的
 * FloatingPaletteSpec 换成 ARGB Int，并补上 spec 里没有的两槽：error（来自 ThemePalette.error）
 * 与 avatar（来自 ThemeArtwork.avatar）。
 *
 * 持久化契约（与装配代理一致）：
 * 1. jarvis_config/theme_id（字符串，只由装配代理写入）命中且 byId 查得到 -> 用它；
 * 2. 否则回退旧键 jarvis_config/theme（Int = 旧 JarvisTheme ordinal）经 legacyOrdinalToThemeId 映射；
 * 3. 都失败 -> deep_night（默认主题），任何一步都不抛异常。
 *
 * 明暗（本阶段）：实际明暗 = jarvis_config/mode_override（follow/light/dark，缺失或非法回落 follow）
 * 经 [resolveMode] 与系统 uiMode 解析所得。浮窗不能用 module.floatingPalette()（每套主题只写了一套，
 * 暗色主题下就恒为浅色浮窗），改为按 [JarvisThemeModule.palette] 的当前明暗现构 spec。
 */
internal data class FloatingThemeColors(
    val themeId: String,
    /** 本套配色对应的实际明暗；syncTheme 用它判断"只切了明暗但主题没换"的情况。 */
    val mode: ThemeMode,
    val avatarRes: Int,
    val panel: Int,
    val title: Int,
    val body: Int,
    val accent: Int,
    val muted: Int,
    val bubbleRing: Int,
    val error: Int,
    val onError: Int
) {
    /** 标题栏带：面板色向标题色提亮 10%，浅色主题下即"更白"，深色主题下即"更亮"。 */
    val header: Int get() = blend(panel, title, 0.10f)
    /** 输入框底色：比面板再淡一档。 */
    val field: Int get() = blend(panel, title, 0.06f)
    /** 提示条/返回确认条底色：面板掺 accent，保留主题色相。 */
    val notice: Int get() = blend(panel, accent, 0.12f)
    /** 按钮底色。 */
    val buttonFill: Int get() = blend(panel, accent, 0.20f)
    /** 按钮涟漪色：accent 压到 24% 不透明度。 */
    val ripple: Int get() = withAlpha(accent, 0x3D)
}

internal object FloatingThemePalette {
    private const val PREFS = "jarvis_config"
    private const val KEY_THEME_ID = "theme_id"
    private const val KEY_LEGACY_THEME = "theme"
    private const val KEY_MODE_OVERRIDE = "mode_override"

    fun read(context: Context): FloatingThemeColors {
        val module = resolveModule(context)
        val mode = resolveMode(readModeOverride(context), isSystemDark(context))
        // 取当前实际明暗那一套色板，再按浮窗的六个角色槽映射；两套色板都由主题作者手写，不做明暗推导。
        val palette = module.palette(mode)
        val spec = FloatingPaletteSpec(
            panel = palette.surface,
            title = palette.onSurface,
            body = palette.onSurfaceVariant,
            accent = palette.primary,
            muted = palette.outline,
            bubbleRing = palette.borderSubtle
        )
        return FloatingThemeColors(
            themeId = module.id,
            mode = mode,
            avatarRes = module.artwork.avatar,
            panel = spec.panel.toArgb(),
            title = spec.title.toArgb(),
            body = spec.body.toArgb(),
            accent = spec.accent.toArgb(),
            muted = spec.muted.toArgb(),
            bubbleRing = spec.bubbleRing.toArgb(),
            error = palette.error.toArgb(),
            onError = palette.onError.toArgb()
        )
    }

    /** mode_override：缺失 / 非法值一律回落 follow（老用户只有 theme_id，不改动其选择）。 */
    fun readModeOverride(context: Context): ThemeModeOverride {
        val raw = runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MODE_OVERRIDE, null)
        }.getOrNull()
        return ThemeModeOverride.fromKey(raw)
    }

    /** 系统当前是否深色；配置变化（含旋转、系统切深浅色）后必须重算，所以每次调用都现读。 */
    fun isSystemDark(context: Context): Boolean {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return night == Configuration.UI_MODE_NIGHT_YES
    }

    private fun resolveModule(context: Context): JarvisThemeModule {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // getString 在旧装上升级后可能读到 Int 键，getInt 也可能读到 String 键：一律吞掉类型错。
        val storedId = runCatching { prefs.getString(KEY_THEME_ID, null) }.getOrNull()
        JarvisThemes.byId(storedId.orEmpty())?.let { return it }
        val ordinal = runCatching { prefs.getInt(KEY_LEGACY_THEME, 0) }.getOrNull()
        val legacyId = LegacyThemeMigration.legacyOrdinalToThemeId(ordinal)
        return JarvisThemes.byId(legacyId) ?: JarvisThemes.default()
    }
}

/** 线性插值混色，返回不透明 ARGB（保留 base 的 alpha 通道）。 */
internal fun blend(base: Int, over: Int, ratio: Float): Int {
    val r = ratio.coerceIn(0f, 1f)
    fun channel(baseChannel: Int, overChannel: Int): Int =
        (baseChannel + (overChannel - baseChannel) * r).toInt().coerceIn(0, 255)
    return ((base ushr 24) and 0xFF shl 24) or
        (channel((base shr 16) and 0xFF, (over shr 16) and 0xFF) shl 16) or
        (channel((base shr 8) and 0xFF, (over shr 8) and 0xFF) shl 8) or
        channel(base and 0xFF, over and 0xFF)
}

/** 只替换 alpha 通道。 */
internal fun withAlpha(color: Int, alpha: Int): Int =
    (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
