package com.jarvis.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 当前**实际生效**的明暗模式（已把 mode_override 与系统值解析完），由 JarvisShell 在注入
 * MaterialTheme 的同一层提供。
 *
 * 主题内部取「此刻该用哪一套色板」必须读它，不要再从 MaterialTheme.colorScheme.background
 * 的亮度/颜色值反查：反查是每次重组都要重算的判断，而且把「配色来源」藏进了颜色值里。
 * 用 static 版本是因为它一次会话内极少变化，变化时整棵树本来就要换配色。
 */
val LocalJarvisMode = staticCompositionLocalOf { ThemeMode.DARK }
