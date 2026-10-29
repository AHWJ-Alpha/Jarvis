package com.jarvis.app.runtime

/** 一次模型请求真正使用的地址、密钥与模型名（已按回退规则合并，字段已 trim）。 */
data class ModelTarget(
    val model: String = "",
    val baseUrl: String = "",
    val apiKey: String = ""
)

/** 合并结果：要么可用，要么一条可直接展示给用户的错误；不存在"空 model 名继续请求"的状态。 */
sealed interface ModelChoice {
    data class Ready(val target: ModelTarget) : ModelChoice
    data class Unavailable(val error: String) : ModelChoice
}

/**
 * 识图模型 / 深度思考模型 与主模型的统一回退规则，全仓只在这里定义一次。
 * 槽位（识图 / 思考）字段为空则逐项继承主模型；主模型也为空时返回 [ModelChoice.Unavailable]，
 * 调用方不得再发出 model 名为空的请求。
 */
object ModelFallback {
    const val ERROR_NO_MAIN_MODEL =
        "未配置主模型：请先在设置中填写主模型名，识图模型与深度思考模型留空时会继承它。"
    const val ERROR_NO_BASE_URL =
        "未配置接口地址：该模型的 Base URL 与主模型 Base URL 都为空。"

    /** 主模型自身：没有任何可继承对象。 */
    fun main(model: String, baseUrl: String, apiKey: String): ModelChoice =
        merge(ModelTarget(), ModelTarget(model, baseUrl, apiKey))

    /** 识图模型：模型名、Base URL、API Key 逐项为空则继承主模型同名配置。 */
    fun vision(main: ModelTarget, vision: ModelTarget): ModelChoice = merge(vision, main)

    /** 深度思考模型：只覆盖模型名，Base URL 与 API Key 始终继承主模型。 */
    fun reason(main: ModelTarget, deepThinkingModel: String): ModelChoice =
        merge(ModelTarget(model = deepThinkingModel), main)

    fun merge(slot: ModelTarget, main: ModelTarget): ModelChoice {
        val model = slot.model.trim().ifBlank { main.model.trim() }
        if (model.isEmpty()) return ModelChoice.Unavailable(ERROR_NO_MAIN_MODEL)
        val baseUrl = slot.baseUrl.trim().ifBlank { main.baseUrl.trim() }
        if (baseUrl.isEmpty()) return ModelChoice.Unavailable(ERROR_NO_BASE_URL)
        val apiKey = slot.apiKey.trim().ifBlank { main.apiKey.trim() }
        return ModelChoice.Ready(ModelTarget(model, baseUrl, apiKey))
    }

    /** 供界面提示与日志使用的一行描述；未就绪时给出原因本身。 */
    fun describe(choice: ModelChoice): String = when (choice) {
        is ModelChoice.Ready -> choice.target.model + " @ " + choice.target.baseUrl
        is ModelChoice.Unavailable -> choice.error
    }
}
