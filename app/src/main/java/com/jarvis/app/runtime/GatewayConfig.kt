package com.jarvis.app.runtime

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit

/** Keeps the private Gateway configuration in sync with Jarvis model settings. */
class GatewayConfig(private val context: Context) {
    private val root = File(context.filesDir, "jarvis")
    private val runtime = File(root, "runtime")
    private val configFile = File(runtime, "openclaw-home/openclaw.json")
    private val tokenFile = File(root, "config/gateway.token")

    /** 最近一次同步的回退告警（如"未配置主模型"）；启动结果会把它带给用户，而不是静默降级。 */
    @Volatile
    var lastWarning: String? = null
        private set

    /** 本次同步如何注册识图模型：NONE 未配置、SHARED 与主模型同源、DEDICATED 独立 provider、UNAVAILABLE 回退失败。 */
    @Volatile
    var visionRegistration: VisionRegistration = VisionRegistration.NONE
        private set

    val visionRegistered: Boolean
        get() = visionRegistration == VisionRegistration.SHARED ||
            visionRegistration == VisionRegistration.DEDICATED

    fun token(): String {
        tokenFile.parentFile?.mkdirs()
        if (tokenFile.isFile && tokenFile.readText().trim().length >= 32) return tokenFile.readText().trim()
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).also { tokenFile.writeText(it) }
    }

    fun synchronize(): Boolean {
        val prefs = context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE)
        if (prefs.getString("modelBaseUrl", "").isNullOrBlank()) importAiClassroom(prefs)
        val base = prefs.getString("modelBaseUrl", "").orEmpty().trim().trimEnd('/')
        val key = prefs.getString("modelApiKey", "").orEmpty().trim()
        val model = prefs.getString("modelName", "").orEmpty().trim()
        val config = if (configFile.isFile) JSONObject(configFile.readText()) else JSONObject()
        val gateway = config.optJSONObject("gateway") ?: JSONObject().also { config.put("gateway", it) }
        gateway.put("mode", "local").put("bind", "loopback").put("port", 18789)
        gateway.put("auth", JSONObject().put("mode", "token").put("token", token()))
        gateway.put("http", JSONObject().put("endpoints", JSONObject().put("chatCompletions", JSONObject().put("enabled", true))))
        // The scheduler is a control-plane tool, so OpenClaw denies it on the
        // HTTP invoke surface unless the Gateway owner explicitly allows it.
        // Keep the exception narrow; tool visibility for model turns is handled
        // separately by tools.alsoAllow below.
        val gatewayTools = gateway.optJSONObject("tools") ?: JSONObject().also { gateway.put("tools", it) }
        val gatewayToolAllow = gatewayTools.optJSONArray("allow") ?: JSONArray().also { gatewayTools.put("allow", it) }
        if ((0 until gatewayToolAllow.length()).none { gatewayToolAllow.optString(it) == "automations" }) {
            gatewayToolAllow.put("automations")
        }
        gateway.put("controlUi", JSONObject().put("enabled", false))
        val plugins = config.optJSONObject("plugins") ?: JSONObject().also { config.put("plugins", it) }
        val enabledPlugins = CommunityPluginManager(context).list().filter { it.enabled }
        val pluginIds = JSONArray().put("jarvis-device-bridge")
        val pluginPaths = JSONArray().put(File(runtime, "jarvis-plugin").absolutePath)
        val pluginEntries = JSONObject().put("jarvis-device-bridge", JSONObject().put("enabled", true))
        enabledPlugins.forEach { plugin ->
            pluginIds.put(plugin.id)
            pluginPaths.put(plugin.directory.absolutePath)
            pluginEntries.put(plugin.id, JSONObject().put("enabled", true))
        }
        plugins.put("allow", pluginIds)
        plugins.put("load", JSONObject().put("paths", pluginPaths))
        plugins.put("entries", pluginEntries)
        config.put("tools", JSONObject().put("profile", "minimal").put("alsoAllow", JSONArray().apply {
            put("jarvis-device-bridge")
            enabledPlugins.forEach { plugin ->
                put(plugin.id)
                plugin.tools.forEach { put(it) }
            }
            put("automations")
        }))
        config.put("memory", JSONObject().put("search", JSONObject().put("enabled", false)))
        val agents = config.optJSONObject("agents") ?: JSONObject().also { config.put("agents", it) }
        val defaults = agents.optJSONObject("defaults") ?: JSONObject().also { agents.put("defaults", it) }
        // Keep Jarvis-owned sessions separate from older CLI/probe workspaces.
        // Android exposes app data through both /data/user/0 and /data/data;
        // OpenClaw pins workspace identity and rejects a previously seen alias.
        defaults.put("workspace", File(runtime, "openclaw-home/jarvis-workspace").absolutePath)
        val mainTarget = ModelTarget(model, base, key)
        val mainChoice = ModelFallback.main(model, base, key)
        val reasoningEnabled = prefs.getBoolean("deepThinkingEnabled", false)
        val reasonChoice = ModelFallback.reason(mainTarget, prefs.getString("deepThinkingModel", "").orEmpty())
        val visionChoice = ModelFallback.vision(mainTarget, ModelTarget(
            prefs.getString("visionModel", "").orEmpty(),
            prefs.getString("visionBaseUrl", "").orEmpty(),
            prefs.getString("visionApiKey", "").orEmpty()
        ))
        lastWarning = listOf(mainChoice, reasonChoice, visionChoice)
            .mapNotNull { (it as? ModelChoice.Unavailable)?.error }
            .distinct()
            .joinToString("；")
            .ifBlank { null }
        if (mainChoice is ModelChoice.Ready) {
            val target = mainChoice.target
            // 思考模型只覆盖模型名；留空即回到主模型，绝不拼出空 model 名。
            val selected = (reasonChoice as? ModelChoice.Ready)?.target?.model ?: target.model
            val plan = ModelProviders.build(
                target,
                selected,
                reasoningEnabled,
                (visionChoice as? ModelChoice.Ready)?.target
            )
            // 识图模型回退失败的只留告警不注册；否则同源追加、异源并列一个 provider。
            visionRegistration = if (visionChoice is ModelChoice.Unavailable) {
                VisionRegistration.UNAVAILABLE
            } else {
                plan.vision
            }
            config.put("models", JSONObject().put("mode", "merge").put("providers", plan.providers))
            defaults.put("model", JSONObject().put("primary", ModelProviders.MAIN_PROVIDER + "/" + selected))
        } else {
            visionRegistration = VisionRegistration.UNAVAILABLE
        }
        configFile.parentFile?.mkdirs()
        val content = config.toString(2)
        val changed = !configFile.isFile || configFile.readText() != content
        if (changed) configFile.writeText(content)
        return changed
    }

    private fun importAiClassroom(prefs: android.content.SharedPreferences) {
        val source = "/data/user/0/com.aiclassroom.app/files/ai_classroom_14_classes.json"
        val process = runCatching { ProcessBuilder("su", "-c", "cat '$source'").start() }.getOrNull() ?: return
        if (!process.waitFor(4, TimeUnit.SECONDS) || process.exitValue() != 0) {
            process.destroyForcibly()
            return
        }
        val raw = process.inputStream.bufferedReader().readText().take(1_000_000)
        runCatching {
            val classes = JSONArray(raw)
            val candidate = classes.optJSONObject(0)?.optJSONObject("config") ?: return
            val model = candidate.optString("selectedModel").ifBlank { candidate.optString("customModel") }
            prefs.edit()
                .putString("modelBaseUrl", candidate.optString("baseUrl"))
                .putString("modelApiKey", candidate.optString("apiKey"))
                .putString("modelName", model)
                .putString("mentorPrompt", candidate.optString("mentorPrompt"))
                .putString("worldBook", candidate.optString("worldBook"))
                .apply()
        }
    }
}
