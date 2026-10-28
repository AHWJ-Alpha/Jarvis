package com.jarvis.ui.theme

/**
 * 旧 JarvisTheme 枚举（JarvisAppearance.kt:45-70）的顺序是持久化契约：ordinal 已经写进
 * SharedPreferences（JarvisAppearance.kt:44 有注释「Keep the order stable」）。一旦改动枚举顺序，
 * 老用户的选择会串档，所以迁移只按 ordinal 查表，不依赖枚举本身。
 */
internal object LegacyThemeMigration {
    /** 与 JarvisTheme.entries 一一对应：0 Deep, 1 Graphite, 2 Forest, 3 Ink, 4 Anime。 */
    val legacyThemeOrder: List<String> = listOf(
        "deep_night",     // 0 Deep
        "paper_minimal",  // 1 Graphite
        "morning_bento",  // 2 Forest
        "ink_moon",       // 3 Ink
        "sakura_night"    // 4 Anime
    )

    /** null 与越界 ordinal 一律回落 deep_night（默认主题），绝不抛异常。 */
    fun legacyOrdinalToThemeId(ordinal: Int?): String =
        ordinal?.let { legacyThemeOrder.getOrNull(it) } ?: legacyThemeOrder[0]
}
