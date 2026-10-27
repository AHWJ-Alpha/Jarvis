package com.jarvis.app.runtime

import android.content.Context
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
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("workspace", workspace)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
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
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }
}

/** Persists conversation history independently from transient Compose navigation state. */
class ConversationStore(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "jarvis/conversations.json")
    private val prefs = appContext.getSharedPreferences("jarvis_conversations", Context.MODE_PRIVATE)

    fun loadOrCreate(): List<ChatSession> = synchronized(lock) {
        val loaded = if (file.isFile) runCatching {
            val array = JSONObject(file.readText()).optJSONArray("sessions") ?: JSONArray()
            (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(ChatSession::fromJson) }
        }.getOrElse { emptyList() } else emptyList()
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

    private fun write(sessions: List<ChatSession>) {
        runCatching {
            file.parentFile?.mkdirs()
            val temporary = File(file.parentFile, "${file.name}.tmp")
            temporary.writeText(JSONObject().put("version", 1)
                .put("sessions", JSONArray().also { array -> sessions.forEach { array.put(it.toJson()) } })
                .toString(2))
            if (!temporary.renameTo(file)) {
                file.writeText(temporary.readText())
                temporary.delete()
            }
        }.onFailure { android.util.Log.e("JarvisConversations", "Could not persist conversation history", it) }
    }

    companion object {
        private const val KEY_ACTIVE_SESSION = "activeSessionId"
        private const val MAX_SESSIONS = 100
        private val lock = Any()
    }
}
