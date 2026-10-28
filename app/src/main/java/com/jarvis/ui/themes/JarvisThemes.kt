package com.jarvis.ui.themes

import com.jarvis.ui.theme.JarvisThemeModule

/**
 * 5 套主题的唯一装配点（原第 6 套赛博主题已按用户要求删除，留档见 artwork/themes-v2/README.md）。
 *
 * 顺序是持久化契约：5 项必须与 JarvisThemeRegistry.legacyOrder（= LegacyThemeMigration.
 * legacyThemeOrder）逐位一致。新增主题只能追加在最后，不能插队。
 *
 * 注意 DeepNightTheme / SakuraNightTheme 是 internal class（需要实例化），其余 3 套是
 * internal object（单例）。阶段 3A 不改主题文件，因此这里按各自形态引用。
 */
internal object JarvisThemes {
    val all: List<JarvisThemeModule> = listOf<JarvisThemeModule>(
        DeepNightTheme(),      // 0 deep_night
        PaperMinimalTheme,     // 1 paper_minimal
        MorningBentoTheme,     // 2 morning_bento
        InkMoonTheme,          // 3 ink_moon
        SakuraNightTheme()     // 4 sakura_night
    )

    /** 未知 id（含已删除赛博主题的历史持久化值）返回 null，调用方必须回落到 [default]。 */
    fun byId(id: String): JarvisThemeModule? = all.firstOrNull { it.id == id }

    /** 默认主题：deep_night（旧 JarvisTheme 的 ordinal 0，也是迁移越界时的回落目标）。 */
    fun default(): JarvisThemeModule = byId("deep_night") ?: all.first()
}
