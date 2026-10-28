package com.jarvis.app.runtime

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class ChatMessageRole(val wireValue: String) { USER("user"), ASSISTANT("assistant") }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatMessageRole,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson() = JSONObject()
        .put("id", id)
        .put("role", role.wireValue)
        .put("content", content)
        .put("createdAt", createdAt)

    companion object {
        fun fromJson(json: JSONObject): ChatMessage? {
            val role = ChatMessageRole.entries.firstOrNull { it.wireValue == json.optString("role") } ?: return null
            val content = json.optString("content")
            if (content.isBlank()) return null
            return ChatMessage(
                id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                role = role,
                content = content,
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val workspace: String = "默认工作区",
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /**
     * 归档时间戳（毫秒）；null 或 <=0 表示未归档。
     * 追加在参数末尾且带默认值，既有位置参数调用不受影响；
     * 旧 conversations.json 没有该键时由 fromJson 视为未归档，自动兼容。
     */
    val archivedAt: Long? = null
) {
    /** 是否已归档（隐藏但完整保留，不删消息）。 */
    val isArchived: Boolean get() = (archivedAt ?: 0L) > 0L

    fun toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("workspace", workspace)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("archivedAt", archivedAt?.takeIf { it > 0L } ?: JSONObject.NULL)
        put("messages", JSONArray().also { array -> messages.forEach { array.put(it.toJson()) } })
    }

    companion object {
        fun fromJson(json: JSONObject): ChatSession? {
            val id = json.optString("id").ifBlank { return null }
            val items = json.optJSONArray("messages") ?: JSONArray()
            val messages = (0 until items.length()).mapNotNull { items.optJSONObject(it)?.let(ChatMessage::fromJson) }
            return ChatSession(
                id = id,
                title = json.optString("title").ifBlank { "新对话" },
                workspace = json.optString("workspace").ifBlank { "默认工作区" },
                messages = messages,
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                archivedAt = json.optLong("archivedAt", 0L).takeIf { it > 0L }
            )
        }
    }
}

/** 旧版存放归档对话 id 的来源（早期版本写在偏好里，现已迁移进 conversations.json）。 */
internal interface ArchivedIdSource {
    fun read(): Set<String>
    fun clear()
}

internal data class ArchivedMigrationResult(
    /** 迁移后的会话清单；未发生落盘时原样返回。 */
    val sessions: List<ChatSession>,
    /** 本次新标记为归档的条数；0 表示无需迁移。 */
    val changed: Int,
    /** 旧偏好是否已被清空；false 表示下次启动会重试。 */
    val prefsCleared: Boolean
)

/**
 * 迁移计划（纯函数）：把 [legacyIds] 里仍然存在的会话补上 archivedAt=[now]，已归档的不重复打标。
 * 幂等：对第一次的返回结果再跑一次，结果列表完全相同（changed 必为 0）。
 */
internal fun planArchivedMigration(
    sessions: List<ChatSession>,
    legacyIds: Set<String>,
    now: Long
): List<ChatSession> {
    if (legacyIds.isEmpty()) return sessions
    return sessions.map { session ->
        if (session.id in legacyIds && !session.isArchived) session.copy(archivedAt = now) else session
    }
}

/**
 * 迁移执行（可单测）：先落盘，成功后才清旧偏好。
 * 因此写盘失败或进程崩溃时旧偏好保留（下次启动重试）；成功清空后再调用即 changed=0，绝不重复迁移。
 */
internal fun migrateArchivedConversations(
    source: ArchivedIdSource,
    sessions: List<ChatSession>,
    now: Long,
    persist: (List<ChatSession>) -> Boolean
): ArchivedMigrationResult {
    val legacy = source.read()
    if (legacy.isEmpty()) return ArchivedMigrationResult(sessions, 0, false)
    val planned = planArchivedMigration(sessions, legacy, now)
    val changed = planned.count { it.isArchived } - sessions.count { it.isArchived }
    if (changed <= 0) {
        // 偏好里只剩找不到的孤儿 id：清键即可，不动会话文件。
        source.clear()
        return ArchivedMigrationResult(sessions, 0, true)
    }
    if (!persist(planned)) return ArchivedMigrationResult(sessions, 0, false)
    source.clear()
    return ArchivedMigrationResult(planned, changed, true)
}

/** Persists conversation history independently from transient Compose navigation state. */
class ConversationStore(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "jarvis/conversations.json")
    private val prefs = appContext.getSharedPreferences("jarvis_conversations", Context.MODE_PRIVATE)

    fun loadOrCreate(): List<ChatSession> = synchronized(lock) {
        val loaded = readSessions()
        val sessions = loaded.ifEmpty { listOf(ChatSession()) }
            .sortedByDescending { it.updatedAt }
            .take(MAX_SESSIONS)
        write(sessions)
        sessions
    }

    fun activeSessionId(sessions: List<ChatSession>): String {
        val selected = prefs.getString(KEY_ACTIVE_SESSION, null)
        return sessions.firstOrNull { it.id == selected }?.id ?: sessions.first().id
    }

    fun save(sessions: List<ChatSession>, activeId: String) = synchronized(lock) {
        val normalized = sessions.distinctBy { it.id }.sortedByDescending { it.updatedAt }.take(MAX_SESSIONS)
        write(normalized)
        prefs.edit().putString(KEY_ACTIVE_SESSION, activeId).commit()
    }

    /**
     * 在给定清单上设置/取消归档并落盘，返回新清单。
     *
     * @param archived true 写入当前时间戳（已有时间戳则保留），false 清空。
     */
    fun setArchived(sessions: List<ChatSession>, id: String, archived: Boolean): List<ChatSession> =
        synchronized(lock) {
            val updated = sessions.map { session ->
                when {
                    session.id != id -> session
                    archived -> session.copy(
                        archivedAt = session.archivedAt?.takeIf { it > 0L } ?: System.currentTimeMillis()
                    )
                    else -> session.copy(archivedAt = null)
                }
            }
            if (updated != sessions) write(updated)
            updated
        }

    /** 按 id 直接操作已落盘的清单（界面没有内存态时使用）。 */
    fun setArchived(id: String, archived: Boolean): List<ChatSession> = synchronized(lock) {
        setArchived(readSessions(), id, archived)
    }

    /**
     * 一次性迁移：把旧版存在 SharedPreferences 里的 archivedConversationIds 写进会话的 archivedAt，
     * 落盘成功后清掉该键。返回本次迁移条数；失败只记日志不抛。
     */
    fun migrateArchivedFromPreferences(legacyPrefs: SharedPreferences): Int = synchronized(lock) {
        val source = object : ArchivedIdSource {
            override fun read(): Set<String> = runCatching {
                legacyPrefs.getStringSet(KEY_LEGACY_ARCHIVED, null)
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.toSet()
                    .orEmpty()
            }.getOrDefault(emptySet())

            override fun clear() {
                runCatching { legacyPrefs.edit().remove(KEY_LEGACY_ARCHIVED).commit() }
                    .onFailure { Log.w(TAG, "清空旧归档偏好失败，下次启动重试", it) }
            }
        }
        val result = runCatching {
            migrateArchivedConversations(source, readSessions(), System.currentTimeMillis(), ::write)
        }.getOrElse { error ->
            Log.w(TAG, "归档迁移失败，保留旧偏好下次重试", error)
            ArchivedMigrationResult(readSessions(), 0, false)
        }
        if (result.changed > 0) Log.i(TAG, "已把 $result.changed 条对话的归档标记迁移进 conversations.json")
        result.changed
    }

    private fun readSessions(): List<ChatSession> = if (file.isFile) runCatching {
        val array = JSONObject(file.readText()).optJSONArray("sessions") ?: JSONArray()
        (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(ChatSession::fromJson) }
    }.getOrElse { emptyList() } else emptyList()

    private fun write(sessions: List<ChatSession>): Boolean = runCatching {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(JSONObject().put("version", 2)
            .put("sessions", JSONArray().also { array -> sessions.forEach { array.put(it.toJson()) } })
            .toString(2))
        if (!temporary.renameTo(file)) {
            file.writeText(temporary.readText())
            temporary.delete()
        }
    }.onFailure {
        Log.e(TAG, "Could not persist conversation history", it)
    }.isSuccess

    companion object {
        private const val KEY_ACTIVE_SESSION = "activeSessionId"
        /** 旧版存放归档对话 id 的偏好键；迁移成功后即删除。 */
        internal const val KEY_LEGACY_ARCHIVED = "archivedConversationIds"
        private const val MAX_SESSIONS = 100
        private const val TAG = "JarvisConversations"
        private val lock = Any()

        /** 归档过滤查询：includeArchived=false 时隐藏归档对话（列表默认行为）。 */
        fun visibleSessions(sessions: List<ChatSession>, includeArchived: Boolean = false): List<ChatSession> =
            if (includeArchived) sessions else sessions.filterNot { it.isArchived }

        /** 已归档对话 id，由 archivedAt 派生（不再读偏好）。 */
        fun archivedIds(sessions: List<ChatSession>): List<String> =
            sessions.filter { it.isArchived }.map { it.id }
    }
}
