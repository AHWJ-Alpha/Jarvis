package com.jarvis.app.runtime

import org.json.JSONObject

enum class RuntimeMode { EMBEDDED, EXTERNAL_TERMUX, REMOTE }

enum class RuntimeState { NOT_CONFIGURED, READY, RUNNING, DEGRADED, ERROR }

data class BootstrapReport(
    val state: RuntimeState,
    val runtimeMode: RuntimeMode,
    val runtimeVersion: String,
    val filesReady: Boolean,
    val rootAvailable: Boolean,
    val bridgeTokenReady: Boolean,
    val pluginDirectory: String,
    val message: String
)

enum class PluginState { STAGED, INSTALLED, ENABLED, DISABLED, FAILED, REMOVED }

data class PluginTool(
    val name: String,
    val risk: String,
    val requiresApproval: Boolean
)

data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val type: String,
    val entrypoint: String,
    val artifactUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val capabilities: List<String>,
    val tools: List<PluginTool>,
    val source: String,
    val license: String
) {
    companion object {
        fun fromJson(raw: String): PluginManifest {
            val json = JSONObject(raw)
            val artifact = json.optJSONObject("artifact") ?: JSONObject()
            val tools = buildList {
                val array = json.optJSONArray("tools") ?: return@buildList
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        PluginTool(
                            name = item.optString("name"),
                            risk = item.optString("risk", "medium"),
                            requiresApproval = item.optBoolean("requiresApproval", true)
                        )
                    )
                }
            }
            return PluginManifest(
                id = json.getString("id"),
                name = json.optString("name", json.getString("id")),
                version = json.getString("version"),
                type = json.optString("type", "http_tool"),
                entrypoint = json.optString("entrypoint"),
                artifactUrl = artifact.optString("url"),
                sha256 = artifact.optString("sha256").lowercase(),
                sizeBytes = artifact.optLong("size", 0L),
                capabilities = json.optJSONArray("capabilities")?.let { array ->
                    buildList { for (index in 0 until array.length()) add(array.optString(index)) }
                } ?: emptyList(),
                tools = tools,
                source = json.optString("source", artifact.optString("url")),
                license = json.optString("license", "unknown")
            )
        }
    }
}

data class PluginRecord(
    val manifest: PluginManifest,
    val state: PluginState,
    val directory: String,
    val installedSha256: String = "",
    val lastError: String = ""
)
