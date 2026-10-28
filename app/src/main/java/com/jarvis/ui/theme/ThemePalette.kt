package com.jarvis.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

enum class ThemeMode { LIGHT, DARK }

/**
 * 用户的明暗偏好。每套主题都有明暗两套色板，这里是「此刻用哪一套」的唯一入口。
 *
 * 持久化契约：字符串 follow / light / dark 写在 SharedPreferences 的 mode_override 键上，
 * 见 [key] / [ThemeModeOverride.fromKey]。老用户只有 theme_id、没有 mode_override，
 * 读取失败一律回落 [FOLLOW_SYSTEM]，不改动他们的主题选择。
 */
enum class ThemeModeOverride {
    /** 跟随系统明暗（默认）。 */
    FOLLOW_SYSTEM,
    /** 强制浅色。 */
    LIGHT,
    /** 强制深色。 */
    DARK;

    /** 持久化用的字符串键：follow / light / dark。 */
    val key: String
        get() = when (this) {
            FOLLOW_SYSTEM -> "follow"
            LIGHT -> "light"
            DARK -> "dark"
        }

    companion object {
        /** 反解持久化字符串；null/空串/无法识别的值一律回落 [FOLLOW_SYSTEM]。 */
        fun fromKey(raw: String?): ThemeModeOverride = when (raw?.trim()?.lowercase()) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> FOLLOW_SYSTEM
        }
    }
}

/** 用户偏好 + 系统明暗 -> 实际生效的明暗。FOLLOW_SYSTEM 时才看 [systemDark]。 */
fun resolveMode(override: ThemeModeOverride, systemDark: Boolean): ThemeMode = when (override) {
    ThemeModeOverride.FOLLOW_SYSTEM -> if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT
    ThemeModeOverride.LIGHT -> ThemeMode.LIGHT
    ThemeModeOverride.DARK -> ThemeMode.DARK
}

/**
 * 一套主题的完整色板，逐主题显式给定，不做明暗推导。
 *
 * 纪律：
 * - [onPrimary] / [onSecondary] / [onError] 必须由每个主题显式给定，禁止 `if (dark)` 推导。旧实现
 *   JarvisAppearance.kt:75 用 background 充当 onPrimary，暗色主题下白字只有 1.70:1。
 * - 层次由明度差承担，不再由描边承担：JarvisCard / JarvisInsetCard 默认不画描边（border = null），
 *   所以每个主题必须保证 surface 与 background 的 ΔL* >= 2.5（深色主题）/ >= 3.0（浅色主题），
 *   去掉描边后卡片边界仍可分辨；surfaceContainer 三阶再在 surface 之上继续分层。
 * - [borderSubtle] 降级为「可选描边的柔化色」，只有显式 JarvisCard(border = ...) 才用到，目标与 [surface]
 *   的对比度收窄到深色 1.6~2.2:1、浅色 1.5~2.0:1。描边只做辅助，不许再抢视觉重心；旧值 3:1 正是「边框太显眼」
 *   的同源问题，而更不能回到旧实现 outlineVariant 当描边的 1.27~1.50:1（那会让描边重新变成噪声或干脆消失）。
 * - [divider] 只用于分隔线，不用于卡片外框。
 * - 可空槽位缺省时回落到本主题自身的色阶（见 [colorScheme]），永不回落到 M3 baseline。
 */
data class ThemePalette(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val borderSubtle: Color,
    val divider: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val scrim: Color,
    val accentSoft: Color,
    val glow: Color,
    val primaryContainer: Color? = null,
    val onPrimaryContainer: Color? = null,
    val inversePrimary: Color? = null,
    val secondaryContainer: Color? = null,
    val onSecondaryContainer: Color? = null,
    val tertiary: Color? = null,
    val onTertiary: Color? = null,
    val tertiaryContainer: Color? = null,
    val onTertiaryContainer: Color? = null,
    val surfaceVariant: Color? = null,
    val surfaceTint: Color? = null,
    val surfaceBright: Color? = null,
    val surfaceDim: Color? = null,
    val surfaceContainerHighest: Color? = null,
    val surfaceContainerLowest: Color? = null,
    val outlineVariant: Color? = null,
    val inverseSurface: Color? = null,
    val inverseOnSurface: Color? = null
)

/**
 * 补齐 material3 1.3.0 的 ColorScheme 全部 36 个颜色键。
 *
 * 旧实现（JarvisAppearance.kt:72-92）靠 `base.copy(...)` 只覆盖了部分键，error/onError/
 * errorContainer/onErrorContainer/scrim 全部留在 M3 baseline 上，于是错误态和遮罩是紫色。
 * 这里直接用 36 参数构造器逐键赋值，不存在「没写就等于基线紫」的缝隙。
 */
fun ThemePalette.colorScheme(): ColorScheme = ColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer ?: accentSoft,
    onPrimaryContainer = onPrimaryContainer ?: onPrimary,
    inversePrimary = inversePrimary ?: primary,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer ?: surfaceContainerHigh,
    onSecondaryContainer = onSecondaryContainer ?: onSecondary,
    tertiary = tertiary ?: secondary,
    onTertiary = onTertiary ?: onSecondary,
    tertiaryContainer = tertiaryContainer ?: surfaceContainerHigh,
    onTertiaryContainer = onTertiaryContainer ?: onSurface,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant ?: surfaceContainer,
    onSurfaceVariant = onSurfaceVariant,
    surfaceTint = surfaceTint ?: primary,
    inverseSurface = inverseSurface ?: onSurface,
    inverseOnSurface = inverseOnSurface ?: surface,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    outline = outline,
    outlineVariant = outlineVariant ?: borderSubtle,
    scrim = scrim,
    surfaceBright = surfaceBright ?: surface,
    surfaceDim = surfaceDim ?: surfaceContainerLow,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest ?: surfaceContainerHigh,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainerLowest = surfaceContainerLowest ?: background
)

/*
 * M3 *Fixed 家族。
 *
 * material3 1.3.0 的 ColorScheme 只有 36 个颜色键，没有 primaryFixed/onPrimaryFixed 这类槽位
 * （javap 实测：ColorScheme 的 36 参数构造器与 36 个 getter 均无 Fixed 名）。补齐 Fixed 需要升级
 * material3，本阶段禁止改动依赖，因此 Fixed 家族以派生属性形式给出，供阶段 1/2 的组件直接使用；
 * 依赖升级后把它们一并映射进 colorScheme() 即可，调用点不用改。
 */
val ThemePalette.primaryFixed: Color get() = primaryContainer ?: accentSoft
val ThemePalette.primaryFixedDim: Color get() = primary
val ThemePalette.onPrimaryFixed: Color get() = onPrimaryContainer ?: onPrimary
val ThemePalette.onPrimaryFixedVariant: Color get() = onPrimary

val ThemePalette.secondaryFixed: Color get() = secondaryContainer ?: surfaceContainerHigh
val ThemePalette.secondaryFixedDim: Color get() = secondary
val ThemePalette.onSecondaryFixed: Color get() = onSecondaryContainer ?: onSecondary
val ThemePalette.onSecondaryFixedVariant: Color get() = onSecondary

val ThemePalette.tertiaryFixed: Color get() = tertiaryContainer ?: surfaceContainerHigh
val ThemePalette.tertiaryFixedDim: Color get() = tertiary ?: secondary
val ThemePalette.onTertiaryFixed: Color get() = onTertiaryContainer ?: onSurface
val ThemePalette.onTertiaryFixedVariant: Color get() = onTertiary ?: onSecondary
