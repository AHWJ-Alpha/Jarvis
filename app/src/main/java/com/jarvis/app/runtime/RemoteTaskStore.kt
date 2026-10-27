package com.jarvis.app.runtime

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class RemoteTaskState(val wireName: String) {
    QUEUED("queued"), RUNNING("running"), COMPLETED("completed"), FAILED("failed")
}

data class RemoteTask(
    val id: String,
    val sessionId: String,
    val prompt: String,
    val state: RemoteTaskState,
    val result: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toJson() = JSONObject()
        .put("id", id)
        .put("sessionId", sessionId)
        .put("prompt", prompt)
        .put("state", state.wireName)
        .put("result", result)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
}

/** Small durable queue for tasks submitted by paired remote browsers. */
class RemoteTaskStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("jarvis_remote_tasks", Context.MODE_PRIVATE)

    fun enqueue(sessionId: String, prompt: String): RemoteTask = synchronized(processLock) {
        require(prompt.isNotBlank()) { "任务内容不能为空" }
        require(prompt.length <= MAX_PROMPT_CHARS) { "任务内容不能超过 $MAX_PROMPT_CHARS 个字符" }
        require(sessionId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) { "会话标识无效" }
        val items = readTasks()
        require(items.count { it.state == RemoteTaskState.QUEUED || it.state == RemoteTaskState.RUNNING } < MAX_ACTIVE_TASKS) {
            "远程任务队列已满，请等待现有任务完成"
        }
        val now = System.currentTimeMillis()
        val task = RemoteTask(UUID.randomUUID().toString(), sessionId, prompt.trim(), RemoteTaskState.QUEUED, "", now, now)
        writeTasks((items + task).takeLast(MAX_RETAINED_TASKS))
        task
    }

    fun queued(): List<RemoteTask> = synchronized(processLock) { readTasks().filter { it.state == RemoteTaskState.QUEUED } }

    fun list(sessionId: String? = null): List<RemoteTask> = synchronized(processLock) {
        readTasks().asReversed().filter { sessionId.isNullOrBlank() || it.sessionId == sessionId }.take(30)
    }

    fun find(id: String): RemoteTask? = synchronized(processLock) { readTasks().firstOrNull { it.id == id } }

    fun claim(id: String): RemoteTask? = synchronized(processLock) {
        val items = readTasks()
        val task = items.firstOrNull { it.id == id && it.state == RemoteTaskState.QUEUED } ?: return null
        val claimed = task.copy(state = RemoteTaskState.RUNNING, updatedAt = System.currentTimeMillis())
        writeTasks(items.map { if (it.id == id) claimed else it })
        claimed
    }

    fun complete(id: String, result: String, state: RemoteTaskState = RemoteTaskState.COMPLETED) = synchronized(processLock) {
        val now = System.currentTimeMillis()
        writeTasks(readTasks().map {
            if (it.id == id) it.copy(state = state, result = result.take(MAX_RESULT_CHARS), updatedAt = now) else it
        })
    }

    /** A process death cannot safely replay an interaction that may already have happened. */
    fun recoverInterrupted() = synchronized(processLock) {
        val now = System.currentTimeMillis()
        writeTasks(readTasks().map {
            if (it.state == RemoteTaskState.RUNNING) it.copy(
                state = RemoteTaskState.FAILED,
                result = "应用进程重启；为避免重复操作，未自动重试此任务。请检查设备状态后重新提交。",
                updatedAt = now
            ) else it
        })
    }

    private fun readTasks(): List<RemoteTask> {
        val array = runCatching { JSONArray(prefs.getString(KEY_TASKS, "[]")) }.getOrDefault(JSONArray())
        return (0 until array.length()).mapNotNull { index ->
            val json = array.optJSONObject(index) ?: return@mapNotNull null
            val state = RemoteTaskState.entries.firstOrNull { it.wireName == json.optString("state") } ?: return@mapNotNull null
            RemoteTask(
                id = json.optString("id"),
                sessionId = json.optString("sessionId"),
                prompt = json.optString("prompt"),
                state = state,
                result = json.optString("result"),
                createdAt = json.optLong("createdAt"),
                updatedAt = json.optLong("updatedAt")
            ).takeIf { it.id.isNotBlank() }
        }
    }

    private fun writeTasks(tasks: List<RemoteTask>) {
        prefs.edit().putString(KEY_TASKS, JSONArray().also { array -> tasks.forEach { array.put(it.toJson()) } }.toString()).commit()
    }

    companion object {
        const val MAX_PROMPT_CHARS = 4_000
        const val MAX_RESULT_CHARS = 4_000
        private const val MAX_ACTIVE_TASKS = 12
        private const val MAX_RETAINED_TASKS = 50
        private const val KEY_TASKS = "tasks"
        private val processLock = Any()
    }
}
