package com.jarvis.ui.theme

import com.jarvis.ui.themes.JarvisThemes

/**
 * 主题注册表。阶段 2 把 5 套主题实现留在 com.jarvis.ui.themes 里，阶段 3A 由 [JarvisThemes]
 * 统一装配，本注册表只做 id 查询门面，不再维护第二份可变清单（重复清单会让 byId 与 default
 * 各说各话）。
 */
internal object JarvisThemeRegistry {
    const val DeepNight = "deep_night"
    const val PaperMinimal = "paper_minimal"
    const val MorningBento = "morning_bento"
    const val InkMoon = "ink_moon"
    const val SakuraNight = "sakura_night"

    /** 旧 JarvisTheme 枚举 ordinal 0..4 对应的主题 id，顺序是持久化契约，不允许调整。 */
    val legacyOrder: List<String> = LegacyThemeMigration.legacyThemeOrder

    /** 全部已装配主题，顺序见 [JarvisThemes.all]。 */
    val all: List<JarvisThemeModule> get() = JarvisThemes.all

    fun byId(id: String): JarvisThemeModule =
        JarvisThemes.byId(id)
            ?: error("未注册的主题 id：$id；已注册=" + JarvisThemes.all.joinToString { it.id })

    fun default(): JarvisThemeModule = JarvisThemes.default()
}
