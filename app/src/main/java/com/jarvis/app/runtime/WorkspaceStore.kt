package com.jarvis.app.runtime

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class WorkspaceProfile(
    val name: String,
    val useGlobalPersona: Boolean = true,
    val mentorPrompt: String = "",
    val worldBook: String = ""
) {
    fun resolvedMentorPrompt(global: String): String =
        if (useGlobalPersona) global else mentorPrompt.ifBlank { global }

    fun resolvedWorldBook(global: String): String =
        if (useGlobalPersona) global else worldBook.ifBlank { global }

    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("useGlobalPersona", useGlobalPersona)
        .put("mentorPrompt", mentorPrompt)
        .put("worldBook", worldBook)

    companion object {
        const val DEFAULT_NAME = "默认工作区"

        fun fromJson(json: JSONObject): WorkspaceProfile? {
            val name = json.optString("name").trim().takeIf { it.isNotEmpty() } ?: return null
            return WorkspaceProfile(
                name = name,
                useGlobalPersona = json.optBoolean("useGlobalPersona", true),
                mentorPrompt = json.optString("mentorPrompt"),
                worldBook = json.optString("worldBook")
            )
        }
    }
}

/** Stores workspace settings separately from chat history so an empty workspace persists. */
class WorkspaceStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, "jarvis/workspaces.json")

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
        temporary.writeText(JSONObject().put("version", 1)
            .put("workspaces", JSONArray().also { array -> normalized.forEach { array.put(it.toJson()) } })
            .toString(2))
        if (!temporary.renameTo(file)) {
            file.writeText(temporary.readText())
            temporary.delete()
        }
    }

    companion object { private val lock = Any() }
}
