package com.jarvis.app.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelFallbackTest {
    private fun ready(choice: ModelChoice): ModelTarget {
        assertTrue("应当可用，实际：" + choice, choice is ModelChoice.Ready)
        return (choice as ModelChoice.Ready).target
    }

    private fun error(choice: ModelChoice): String {
        assertTrue("应当报错，实际：" + choice, choice is ModelChoice.Unavailable)
        return (choice as ModelChoice.Unavailable).error
    }

    @Test
    fun visionBlankInheritsMainModelAndKey() {
        val main = ModelTarget("deepseek-chat", "https://api.deepseek.com/v1", "sk-main")
        val target = ready(ModelFallback.vision(main, ModelTarget()))
        assertEquals("deepseek-chat", target.model)
        assertEquals("https://api.deepseek.com/v1", target.baseUrl)
        assertEquals("sk-main", target.apiKey)
    }

    @Test
    fun visionKeepsItsOwnKeyAndOnlyFillsBlanks() {
        val main = ModelTarget("deepseek-chat", "https://api.deepseek.com/v1", "sk-main")
        val vision = ModelTarget("gpt-4o-mini", "", "sk-vision")
        val target = ready(ModelFallback.vision(main, vision))
        assertEquals("gpt-4o-mini", target.model)
        assertEquals("https://api.deepseek.com/v1", target.baseUrl)
        assertEquals("sk-vision", target.apiKey)
    }

    @Test
    fun bothBlankIsAnExplicitError() {
        val main = ModelTarget()
        val vision = ModelTarget("  ", " ", "")
        assertEquals(ModelFallback.ERROR_NO_MAIN_MODEL, error(ModelFallback.vision(main, vision)))
        assertEquals(ModelFallback.ERROR_NO_MAIN_MODEL, error(ModelFallback.main("", "", "")))
    }

    @Test
    fun mainModelWithoutBaseUrlIsAlsoAnError() {
        assertEquals(ModelFallback.ERROR_NO_BASE_URL, error(ModelFallback.main("gpt-4o", "", "sk")))
    }

    @Test
    fun reasonModelFallsBackToMainModelNameAndCredentials() {
        val main = ModelTarget("deepseek-chat", "https://api.deepseek.com/v1", "sk-main")
        val inherited = ready(ModelFallback.reason(main, "   "))
        assertEquals("deepseek-chat", inherited.model)
        assertEquals("sk-main", inherited.apiKey)

        val own = ready(ModelFallback.reason(main, "deepseek-reasoner"))
        assertEquals("deepseek-reasoner", own.model)
        assertEquals("https://api.deepseek.com/v1", own.baseUrl)
        assertEquals("sk-main", own.apiKey)
    }

    @Test
    fun mergeTrimsWhitespaceOnlyOverrides() {
        val main = ModelTarget(" main-model ", " https://host/v1 ", " sk-main ")
        val target = ready(ModelFallback.vision(main, ModelTarget("   ", "   ", "  ")))
        assertEquals("main-model", target.model)
        assertEquals("https://host/v1", target.baseUrl)
        assertEquals("sk-main", target.apiKey)
    }
}
