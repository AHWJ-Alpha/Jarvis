package com.jarvis.app.runtime

import org.json.JSONArray
import org.json.JSONObject

/** 识图模型在网关配置里的注册方式：[NONE] 未配置/未就绪，[SHARED] 与主模型同源共用 provider，[DEDICATED] 独立 provider。 */
enum class VisionRegistration { NONE, SHARED, DEDICATED, UNAVAILABLE }

/** 一次同步产出的 provider 表与识图注册方式（纯数据，便于单测）。 */
data class ProviderPlan(val providers: JSONObject, val vision: VisionRegistration)

/**
 * 生成 openclaw.json 的 models.providers。
 * 结构固定为 {baseUrl, apiKey, api, models[]}，每个模型条目 {id, name, reasoning, input, contextWindow, maxTokens}；
 * 识图模型只追加 models 条目或并列一个 provider，不改既有 provider 结构。
 */
object ModelProviders {
    const val MAIN_PROVIDER = "jarvis"
    const val VISION_PROVIDER = "jarvis-vision"
    private const val CONTEXT_WINDOW = 128000
    private const val MAX_TOKENS = 8192

    /**
     * @param main 主模型（已回退）
     * @param selected 主 provider 首选的模型名（思考模型已回退后的值）
     * @param vision 识图模型（已回退）；null 表示识图未就绪，不注册
     */
    fun build(main: ModelTarget, selected: String, reasoning: Boolean, vision: ModelTarget?): ProviderPlan {
        val mainModels = JSONArray().apply { put(entry(selected, reasoning, imageInput = false)) }
        val providers = JSONObject()
        var registration = if (vision == null) VisionRegistration.NONE else VisionRegistration.SHARED
        if (vision != null) {
            if (vision.baseUrl == main.baseUrl && vision.apiKey == main.apiKey) {
                // 同源：追加到主 provider 的 models，同 id 去重。
                if (indexOfModel(mainModels, vision.model) < 0) {
                    mainModels.put(entry(vision.model, reasoning = false, imageInput = true))
                }
            } else {
                providers.put(VISION_PROVIDER, provider(
                    vision,
                    JSONArray().put(entry(vision.model, reasoning = false, imageInput = true))
                ))
                registration = VisionRegistration.DEDICATED
            }
        }
        providers.put(MAIN_PROVIDER, provider(main, mainModels))
        return ProviderPlan(providers, registration)
    }

    fun entry(id: String, reasoning: Boolean, imageInput: Boolean): JSONObject {
        val input = JSONArray().put("text")
        if (imageInput) input.put("image")
        return JSONObject()
            .put("id", id)
            .put("name", id)
            .put("reasoning", reasoning)
            .put("input", input)
            .put("contextWindow", CONTEXT_WINDOW)
            .put("maxTokens", MAX_TOKENS)
    }

    fun provider(target: ModelTarget, models: JSONArray): JSONObject = JSONObject()
        .put("baseUrl", target.baseUrl)
        .put("apiKey", target.apiKey)
        .put("api", "openai-completions")
        .put("models", models)

    private fun indexOfModel(models: JSONArray, id: String): Int =
        (0 until models.length()).firstOrNull { models.optJSONObject(it)?.optString("id") == id } ?: -1
}
