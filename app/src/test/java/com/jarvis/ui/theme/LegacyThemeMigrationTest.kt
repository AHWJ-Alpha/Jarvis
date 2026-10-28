package com.jarvis.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyThemeMigrationTest {

    @Test
    fun legacyOrdinalsMapToThemeIds() {
        assertEquals("deep_night", LegacyThemeMigration.legacyOrdinalToThemeId(0))
        assertEquals("paper_minimal", LegacyThemeMigration.legacyOrdinalToThemeId(1))
        assertEquals("morning_bento", LegacyThemeMigration.legacyOrdinalToThemeId(2))
        assertEquals("ink_moon", LegacyThemeMigration.legacyOrdinalToThemeId(3))
        assertEquals("sakura_night", LegacyThemeMigration.legacyOrdinalToThemeId(4))
    }

    @Test
    fun nullFallsBackToDeepNight() {
        assertEquals("deep_night", LegacyThemeMigration.legacyOrdinalToThemeId(null))
    }

    @Test
    fun outOfRangeOrdinalsFallBackToDeepNight() {
        assertEquals("deep_night", LegacyThemeMigration.legacyOrdinalToThemeId(-1))
        assertEquals("deep_night", LegacyThemeMigration.legacyOrdinalToThemeId(5))
        assertEquals("deep_night", LegacyThemeMigration.legacyOrdinalToThemeId(99))
    }
}
