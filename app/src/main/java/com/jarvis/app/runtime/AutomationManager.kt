package com.jarvis.app.runtime

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

enum class AutomationKind(val label: String) {
    BILIBILI_REPLY("哔哩哔哩评论回复"), PIXIV_NEW_WORK("Pixiv 画师新作品")
}

data class AutomationDraft(
    val kind: AutomationKind,
    val title: String,
    val sourcePrompt: String,
    val artist: String = ""
)

data class AutomationRule(
    val id: String,
    val kind: AutomationKind,
    val title: String,
    val sourcePrompt: String,
    val artist: String = "",
    val replyInstruction: String = "简短、友好并结合上下文回复，不要重复固定话术。",
    val enabled: Boolean = false,
    val lastResult: String = "",
    val lastRunAt: Long = 0L,
    /** 规则来自哪个会话；该会话被删除时规则失去上下文，需要暂停。 */
    val sourceConversationId: String = ""
) {
    fun toJson() = JSONObject().put("id", id).put("kind", kind.name).put("title", title)
        .put("sourcePrompt", sourcePrompt).put("artist", artist)
        .put("replyInstruction", replyInstruction).put("enabled", enabled)
        .put("lastResult", lastResult).put("lastRunAt", lastRunAt)
        .put("sourceConversationId", sourceConversationId)

    companion object {
        fun fromJson(json: JSONObject) = AutomationRule(
            id = json.optString("id"),
            kind = runCatching { AutomationKind.valueOf(json.optString("kind")) }.getOrDefault(AutomationKind.BILIBILI_REPLY),
            title = json.optString("title"), sourcePrompt = json.optString("sourcePrompt"), artist = json.optString("artist"),
            replyInstruction = json.optString("replyInstruction").ifBlank { "简短、友好并结合上下文回复，不要重复固定话术。" },
            enabled = json.optBoolean("enabled"), lastResult = json.optString("lastResult"), lastRunAt = json.optLong("lastRunAt"),
            sourceConversationId = json.optString("sourceConversationId")
        )
    }
}

data class AutomationEvent(
    val eventId: String,
    val rule: AutomationRule,
    val packageName: String,
    val title: String,
    val text: String,
    val notificationKey: String,
    val notificationOpened: Boolean = false
)

/** Locally stored, opt-in automations driven by official-app notifications. */
class AutomationManager(private val context: Context) {
    private val file = File(context.filesDir, "jarvis/automations.json")
    private val lock = Any()

    fun create(draft: AutomationDraft, sourceConversationId: String = ""): AutomationRule = synchronized(lock) {
        val rule = AutomationRule(
            UUID.randomUUID().toString(), draft.kind, draft.title, draft.sourcePrompt,
            artist = draft.artist, sourceConversationId = sourceConversationId
        )
        saveRules(readRules() + rule)
        rule
    }

    /**
     * 来源会话被删除时暂停依赖该会话上下文的规则：规则正文是用户在那次对话里的要求，
     * 会话删掉后无法再核对上下文，暂停比继续跑更安全，且暂停状态在任务页可见。
     * 返回本次被暂停的规则。
     */
    fun pauseByConversation(conversationId: String): List<AutomationRule> = synchronized(lock) {
        val matched = readRules().filter { it.enabled && dependsOnConversation(it, conversationId) }
        if (matched.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val paused = matched.map { it.copy(enabled = false, lastRunAt = now, lastResult = PAUSED_BY_CONVERSATION_DELETE) }
        saveRules(readRules().map { rule -> paused.firstOrNull { it.id == rule.id } ?: rule })
        val root = readRoot()
        val events = root.optJSONArray("events") ?: JSONArray()
        paused.forEach { rule ->
            events.put(JSONObject().put("ruleId", rule.id).put("title", rule.title)
                .put("ok", false).put("message", PAUSED_BY_CONVERSATION_DELETE).put("time", now))
        }
        while (events.length() > 100) events.remove(0)
        root.put("events", events)
        writeRoot(root)
        paused
    }

    fun list(): List<AutomationRule> = synchronized(lock) { readRules() }

    fun update(rule: AutomationRule): AutomationRule = synchronized(lock) {
        require(rule.title.isNotBlank()) { "自动化名称不能为空" }
        val old = readRules()
        require(old.any { it.id == rule.id }) { "自动化规则不存在" }
        saveRules(old.map { if (it.id == rule.id) rule else it })
        rule
    }

    fun delete(id: String) = synchronized(lock) { saveRules(readRules().filterNot { it.id == id }) }

    fun readiness(rule: AutomationRule): String? {
        if (rule.kind == AutomationKind.PIXIV_NEW_WORK && rule.artist.isBlank()) return "填写 Pixiv 画师名称或用户 ID"
        val preferences = context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE)
        if (preferences.getString("modelBaseUrl", "").isNullOrBlank() || preferences.getString("modelName", "").isNullOrBlank()) {
            return "先在设置里配置并验证 AI 模型"
        }
        if (!isInstalled(targetPackage(rule.kind))) return "请在 Jarvis 当前 Android 用户中安装目标应用"
        if (!hasNotificationAccess()) return "请授权 Jarvis 通知访问"
        if (AccessibilityController.service == null) return "请启用 Jarvis 无障碍服务"
        return null
    }

    fun setEnabled(id: String, enabled: Boolean): AutomationRule = synchronized(lock) {
        val rule = readRules().firstOrNull { it.id == id } ?: error("自动化规则不存在")
        val reason = if (enabled) readiness(rule) else null
        require(reason == null) { reason!! }
        if (enabled) require(readRules().none { it.id != id && it.kind == rule.kind && it.enabled }) {
            "同一目标目前只能启用一条自动化，避免重复回复或重复下载"
        }
        update(rule.copy(enabled = enabled, lastResult = if (enabled) "已启用，等待目标应用通知" else "已暂停"))
    }

    /** Deduplicates notification updates and creates one event per matching enabled rule. */
    fun acceptNotification(packageName: String, title: String, text: String, notificationKey: String): List<AutomationEvent> = synchronized(lock) {
        val body = "$title\n$text"
        val hash = sha256("$packageName\n$notificationKey\n$body")
        val root = readRoot()
        val seen = root.optJSONArray("seen") ?: JSONArray()
        if ((0 until seen.length()).any { seen.optString(it) == hash }) return emptyList()
        seen.put(hash)
        while (seen.length() > 300) seen.remove(0)
        root.put("seen", seen)
        writeRoot(root)

        readRules().filter { it.enabled && matchesNotification(it.kind, packageName, body, it.artist) }
            .map { AutomationEvent(hash, it, packageName, title.take(300), text.take(1200), notificationKey) }
    }

    fun findRule(id: String): AutomationRule? = synchronized(lock) { readRules().firstOrNull { it.id == id } }

    /** At-most-once execution guard: a process restart must not repeat a possibly-sent reply. */
    fun claim(eventId: String): Boolean = synchronized(lock) {
        val root = readRoot()
        val claims = root.optJSONArray("claims") ?: JSONArray()
        if ((0 until claims.length()).any { claims.optString(it) == eventId }) return false
        claims.put(eventId)
        while (claims.length() > 300) claims.remove(0)
        root.put("claims", claims)
        writeRoot(root)
        true
    }

    fun record(event: AutomationEvent, result: String) = synchronized(lock) {
        val now = System.currentTimeMillis()
        saveRules(readRules().map {
            if (it.id == event.rule.id) it.copy(lastRunAt = now, lastResult = result.take(400)) else it
        })
        val root = readRoot()
        val events = root.optJSONArray("events") ?: JSONArray()
        events.put(JSONObject().put("ruleId", event.rule.id).put("title", event.rule.title)
            .put("ok", result.startsWith("已核实完成：")).put("message", result.take(400)).put("time", now))
        while (events.length() > 100) events.remove(0)
        root.put("events", events)
        writeRoot(root)
    }

    fun markPending(event: AutomationEvent) = synchronized(lock) {
        val now = System.currentTimeMillis()
        saveRules(readRules().map {
            if (it.id == event.rule.id) it.copy(lastRunAt = now, lastResult = "已排队，等待核对通知内容") else it
        })
    }

    fun recentEvents(): List<JSONObject> = synchronized(lock) {
        val events = readRoot().optJSONArray("events") ?: JSONArray()
        (events.length() - 1 downTo 0).mapNotNull { events.optJSONObject(it)?.let { item -> JSONObject(item.toString()) } }
    }

    fun hasNotificationAccess(): Boolean {
        val raw = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        val expected = ComponentName(context, JarvisNotificationListenerService::class.java)
        return raw.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    fun isInstalled(packageName: String): Boolean = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
    }.isSuccess

    private fun readRules(): List<AutomationRule> {
        val rules = readRoot().optJSONArray("rules") ?: JSONArray()
        return (0 until rules.length()).mapNotNull { rules.optJSONObject(it)?.let(AutomationRule::fromJson) }
    }

    private fun saveRules(rules: List<AutomationRule>) {
        val root = readRoot().put("rules", JSONArray().also { array -> rules.forEach { array.put(it.toJson()) } })
        writeRoot(root)
    }

    private fun readRoot(): JSONObject = if (file.isFile) JSONObject(file.readText()) else JSONObject()

    private fun writeRoot(root: JSONObject) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(root.toString(2))
        if (!temp.renameTo(file)) {
            file.writeText(root.toString(2))
            temp.delete()
        }
    }

    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun targetPackage(kind: AutomationKind) = when (kind) {
        AutomationKind.BILIBILI_REPLY -> BILIBILI_PACKAGE
        AutomationKind.PIXIV_NEW_WORK -> PIXIV_PACKAGE
    }

    companion object {
        const val BILIBILI_PACKAGE = "tv.danmaku.bili"
        const val PIXIV_PACKAGE = "jp.pxv.android"
        const val PAUSED_BY_CONVERSATION_DELETE = "已暂停：来源会话已删除"

        /** 规则是否依赖某个会话作为上下文（该会话被删除时要暂停）。 */
        fun dependsOnConversation(rule: AutomationRule, conversationId: String): Boolean =
            conversationId.isNotBlank() && rule.sourceConversationId == conversationId

        fun matchesNotification(kind: AutomationKind, packageName: String, body: String, artist: String = ""): Boolean {
            val lowered = body.lowercase()
            return when (kind) {
                AutomationKind.BILIBILI_REPLY -> packageName == BILIBILI_PACKAGE && listOf(
                    "回复了你", "回复了你的评论", "回复你的评论", "评论回复了你", "回复你在"
                ).any(lowered::contains)
                AutomationKind.PIXIV_NEW_WORK -> packageName == PIXIV_PACKAGE && artist.isNotBlank() &&
                    (if (artist.all(Char::isDigit)) Regex("(?<!\\d)${Regex.escape(artist)}(?!\\d)").containsMatchIn(body)
                    else lowered.contains(artist.lowercase())) &&
                    listOf("新作", "新作品", "发布", "投稿", "作品を投稿", "新しい作品", "new work").any(lowered::contains)
            }
        }

        fun parse(prompt: String): AutomationDraft? {
            val text = prompt.trim()
            if (!text.contains("当") || !(text.contains("就") || text.contains("时"))) return null
            val lower = text.lowercase()
            return when {
                (text.contains("哔哩哔哩") || text.contains("哔哩") || lower.contains("bilibili") || text.contains("B站")) &&
                    text.contains("回复") && (text.contains("评论") || text.contains("有人") || text.contains("回复我")) ->
                    AutomationDraft(AutomationKind.BILIBILI_REPLY, "哔哩哔哩有人回复时自动回复", text)

                lower.contains("pixiv") && (text.contains("画师") || text.contains("作者")) &&
                    (text.contains("新作品") || text.contains("新作")) && (text.contains("下载") || text.contains("保存")) -> {
                    val urlId = Regex("pixiv\\.net/(?:[a-z]{2}/)?users/(\\d+)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)
                    val captured = Regex("(?:画师|作者)\\s*(?:ID|id)?\\s*(?:为|是|[:：])?\\s*([\\p{L}\\p{N}_-]+)")
                        .find(text)?.groupValues?.getOrNull(1).orEmpty()
                    val artist = urlId ?: captured.replace(Regex("(?:发布|更新|上传|新作品|新作|就|立刻|下载|保存).*$"), "").trim()
                    AutomationDraft(AutomationKind.PIXIV_NEW_WORK, "Pixiv 画师发布新作品时自动下载", text, artist)
                }
                else -> null
            }
        }
    }
}
