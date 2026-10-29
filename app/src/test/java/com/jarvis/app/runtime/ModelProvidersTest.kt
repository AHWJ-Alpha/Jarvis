package com.jarvis.app.runtime

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelProvidersTest {
    private val main = ModelTarget("deepseek-chat", "https://api.deepseek.com/v1", "sk-main-key")
    private val mainProvider = JSONObject()
        .put("baseUrl", main.baseUrl).put("apiKey", main.apiKey).put("api", "openai-completions")

    private fun ids(plan: ProviderPlan, provider: String): List<String> {
        val models = plan.providers.optJSONObject(provider)?.optJSONArray("models") ?: return emptyList()
        return (0 until models.length()).map { models.optJSONObject(it).optString("id") }
    }

    @Test
    fun blankVisionRegistersNothing() {
        val plan = ModelProviders.build(main, "deepseek-chat", reasoning = false, vision = null)
        assertEquals(VisionRegistration.NONE, plan.vision)
        assertEquals(listOf("deepseek-chat"), ids(plan, ModelProviders.MAIN_PROVIDER))
        assertNull(plan.providers.optJSONObject(ModelProviders.VISION_PROVIDER))
        assertEquals(mainProvider.getString("baseUrl"), plan.providers.optJSONObject("jarvis").getString("baseUrl"))
    }

    @Test
    fun sameOriginVisionIsAppendedToTheMainProvider() {
        val vision = ModelTarget("gpt-4o-mini", main.baseUrl, main.apiKey)
        val plan = ModelProviders.build(main, "deepseek-chat", reasoning = false, vision = vision)
        assertEquals(VisionRegistration.SHARED, plan.vision)
        assertEquals(listOf("deepseek-chat", "gpt-4o-mini"), ids(plan, ModelProviders.MAIN_PROVIDER))
        assertNull(plan.providers.optJSONObject(ModelProviders.VISION_PROVIDER))
        val entry = plan.providers.optJSONObject("jarvis").optJSONArray("models").optJSONObject(1)
        assertEquals("gpt-4o-mini", entry.getString("name"))
        assertFalse(entry.getBoolean("reasoning"))
        assertEquals("image", entry.getJSONArray("input").getString(1))
    }

    @Test
    fun sameOriginIsDeduplicatedById() {
        val vision = ModelTarget("deepseek-chat", main.baseUrl, main.apiKey)
        val plan = ModelProviders.build(main, "deepseek-chat", reasoning = true, vision = vision)
        assertEquals(VisionRegistration.SHARED, plan.vision)
        assertEquals(listOf("deepseek-chat"), ids(plan, ModelProviders.MAIN_PROVIDER))
    }

    @Test
    fun differentOriginAddsASecondProvider() {
        val vision = ModelTarget("qwen-vl-max", "https://dashscope.example.com/v1", "sk-vision-key")
        val plan = ModelProviders.build(main, "deepseek-reasoner", reasoning = true, vision = vision)
        assertEquals(VisionRegistration.DEDICATED, plan.vision)
        assertEquals(listOf("deepseek-reasoner"), ids(plan, ModelProviders.MAIN_PROVIDER))
        assertEquals(listOf("qwen-vl-max"), ids(plan, ModelProviders.VISION_PROVIDER))
        val visionProvider = plan.providers.getJSONObject(ModelProviders.VISION_PROVIDER)
        assertEquals("https://dashscope.example.com/v1", visionProvider.getString("baseUrl"))
        assertEquals("sk-vision-key", visionProvider.getString("apiKey"))
        assertEquals("openai-completions", visionProvider.getString("api"))
        assertNotNull(plan.providers.getJSONObject(ModelProviders.MAIN_PROVIDER))
    }
}
