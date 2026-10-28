package com.jarvis.app.runtime

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 一个风味工作区的档案。
 *
 * 新增字段一律带默认值，[fromJson] 用 optXxx 读取，因此旧版 workspaces.json
 * （只有 name/useGlobalPersona/mentorPrompt/worldBook）读进来会自动补齐为新值。
 *
 * @property archived true 表示已归档：设置/工作区列表默认不显示，数据（档案 + 对话）全部保留。
 * @property dirUri SAF 目录的持久化 URI（TakePersistableUriPermission 之后可长期读），空串表示未绑定。
 * @property dirLabel 目录显示名，仅用于界面；空串时由界面回退到「未绑定」文案。
 */
data class WorkspaceProfile(
    val name: String,
    val useGlobalPersona: Boolean = true,
    val mentorPrompt: String = "",
    val worldBook: String = "",
    val archived: Boolean = false,
    val dirUri: String = "",
    val dirLabel: String = ""
) {
    fun resolvedMentorPrompt(global: String): String =
        if (useGlobalPersona) global else mentorPrompt.ifBlank { global }

    fun resolvedWorldBook(global: String): String =
        if (useGlobalPersona) global else worldBook.ifBlank { global }

    /** 是否绑定了一个 SAF 目录（不代表权限仍然有效，权限由 MainActivity 检查 ContentResolver）。 */
    val hasDirectory: Boolean get() = dirUri.isNotBlank()

    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("useGlobalPersona", useGlobalPersona)
        .put("mentorPrompt", mentorPrompt)
        .put("worldBook", worldBook)
        .put("archived", archived)
        .put("dirUri", dirUri)
        .put("dirLabel", dirLabel)

    companion object {
        const val DEFAULT_NAME = "默认工作区"

        fun fromJson(json: JSONObject): WorkspaceProfile? {
            val name = json.optString("name").trim().takeIf { it.isNotEmpty() } ?: return null
            return WorkspaceProfile(
                name = name,
                useGlobalPersona = json.optBoolean("useGlobalPersona", true),
                mentorPrompt = json.optString("mentorPrompt"),
                worldBook = json.optString("worldBook"),
                archived = json.optBoolean("archived", false),
                dirUri = json.optString("dirUri").trim(),
                dirLabel = json.optString("dirLabel").trim()
            )
        }
    }
}

/** Stores workspace settings separately from chat history so an empty workspace persists. */
class WorkspaceStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, "jarvis/workspaces.json")

    /** 全部工作区，含已归档；调用方自行按 [WorkspaceProfile.archived] 过滤显示。 */
    fun load(conversationWorkspaceNames: List<String> = emptyList()): List<WorkspaceProfile> = synchronized(lock) {
        val saved = runCatching {
            val array = JSONObject(file.readText()).optJSONArray("workspaces") ?: JSONArray()
            (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(WorkspaceProfile::fromJson) }
        }.getOrDefault(emptyList())
        val names = listOf(WorkspaceProfile.DEFAULT_NAME) + conversationWorkspaceNames
        val legacy = names.mapNotNull { name ->
            name.trim().takeIf { it.isNotEmpty() }?.let { WorkspaceProfile(it) }
        }
        val result = (saved + legacy).distinctBy { it.name.lowercase() }
        if (!file.isFile || result.size != saved.size) save(result)
        result
    }

    fun save(profiles: List<WorkspaceProfile>) = synchronized(lock) {
        val normalized = profiles.distinctBy { it.name.lowercase() }
        require(normalized.any { it.name == WorkspaceProfile.DEFAULT_NAME })
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(JSONObject().put("version", 2)
            .put("workspaces", JSONArray().also { array -> normalized.forEach { array.put(it.toJson()) } })
            .toString(2))
        if (!temporary.renameTo(file)) {
            file.writeText(temporary.readText())
            temporary.delete()
        }
    }

    companion object { private val lock = Any() }
}
