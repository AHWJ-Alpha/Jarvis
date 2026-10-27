package com.jarvis.app.runtime

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

data class ManagedOpenClawPlugin(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val directory: File,
    val tools: List<String>,
    val enabled: Boolean,
    val source: String
)

data class GeneratedOpenClawPlugin(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val tools: List<String>,
    val source: String
)

/** Installs user-reviewed OpenClaw extensions into Jarvis's private plugin directory. */
class CommunityPluginManager(context: Context) {
    private val root = File(BootstrapController(context.applicationContext).pluginDirectory(), "community")

    fun list(): List<ManagedOpenClawPlugin> = root.listFiles()?.asSequence()
        ?.filter { it.isDirectory && !it.name.startsWith(".") }
        ?.mapNotNull { directory -> runCatching { readPlugin(directory) }.getOrNull() }
        ?.sortedBy { it.name.lowercase() }
        ?.toList().orEmpty()

    fun installArchive(input: InputStream): ManagedOpenClawPlugin {
        root.mkdirs()
        val staging = File(root, ".staging-${UUID.randomUUID()}")
        staging.mkdirs()
        try {
            extractArchive(input, staging)
            normalizeArchiveRoot(staging)
            val manifest = validateDirectory(staging)
            val id = manifest.getString("id")
            val target = File(root, id)
            check(!target.exists()) { "相同 ID 的插件已存在；为避免覆盖，请先卸载旧插件。" }
            if (!staging.renameTo(target)) error("插件目录安装失败")
            writeEnabled(target, false)
            return readPlugin(target)
        } catch (error: Throwable) {
            staging.deleteRecursively()
            throw error
        }
    }

    fun installGenerated(plugin: GeneratedOpenClawPlugin): ManagedOpenClawPlugin {
        validateId(plugin.id)
        validateVersion(plugin.version)
        require(plugin.name.isNotBlank() && plugin.name.length <= 100) { "插件名称不能为空或过长" }
        require(plugin.description.length <= 1000) { "插件说明过长" }
        require(plugin.source.isNotBlank() && plugin.source.length <= MAX_SOURCE_BYTES) { "插件源码为空或超过 256 KB" }
        require(plugin.source.contains("export default") && plugin.source.contains("register")) { "源码不是标准 OpenClaw 插件入口" }
        require(Regex("\\bid\\s*:\\s*['\"]${Regex.escape(plugin.id)}['\"]").containsMatchIn(plugin.source)) {
            "插件源码中的 id 必须与插件清单一致"
        }
        require(plugin.tools.isNotEmpty() && plugin.tools.size <= MAX_TOOLS && plugin.tools.distinct().size == plugin.tools.size) {
            "插件需包含 1–20 个不重复工具名"
        }
        require(!UNSAFE_SOURCE.containsMatchIn(plugin.source)) {
            "生成的源码包含直接系统访问或动态执行语句；请改成调用公开 API 的插件，或手动导入并审查可信插件。"
        }
        val staging = File(root, ".staging-${UUID.randomUUID()}")
        staging.mkdirs()
        try {
            val descriptor = JSONObject()
                .put("id", plugin.id)
                .put("name", plugin.name)
                .put("description", plugin.description)
                .put("activation", JSONObject().put("onStartup", true))
                .put("enabledByDefault", false)
                .put("contracts", JSONObject().put("tools", JSONArray(plugin.tools)))
            val packageJson = JSONObject()
                .put("name", plugin.id)
                .put("version", plugin.version)
                .put("type", "module")
                .put("main", "index.js")
            File(staging, "openclaw.plugin.json").writeText(descriptor.toString(2))
            File(staging, "package.json").writeText(packageJson.toString(2))
            File(staging, "index.js").writeText(plugin.source)
            File(staging, "jarvis-plugin.json").writeText(JSONObject()
                .put("id", plugin.id).put("name", plugin.name).put("version", plugin.version)
                .put("description", plugin.description).put("source", "generated").put("enabled", false)
                .toString(2))
            validateDirectory(staging)
            val target = File(root, plugin.id)
            check(!target.exists()) { "相同 ID 的插件已存在；为避免覆盖，请先卸载旧插件或重新生成不同 ID。" }
            if (!staging.renameTo(target)) error("插件目录安装失败")
            return readPlugin(target)
        } catch (error: Throwable) {
            staging.deleteRecursively()
            throw error
        }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val plugin = pluginDirectory(id)
        check(plugin.isDirectory) { "找不到插件" }
        readPlugin(plugin)
        writeEnabled(plugin, enabled)
    }

    fun remove(id: String) {
        validateId(id)
        val directory = pluginDirectory(id)
        if (directory.exists()) check(directory.deleteRecursively()) { "插件目录删除失败" }
    }

    private fun extractArchive(input: InputStream, destination: File) {
        val rootPath = destination.canonicalFile
        var entries = 0
        var expandedBytes = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries += 1
                check(entries <= MAX_ENTRIES) { "插件文件数量超过限制" }
                val name = entry.name.replace('\\', '/')
                check(name.isNotBlank() && !name.startsWith('/') && !Regex("^[A-Za-z]:").containsMatchIn(name)) { "插件压缩包包含无效路径" }
                check(name.split('/').none { it == ".." || it == "." }) { "插件压缩包包含越界路径" }
                val target = File(destination, name).canonicalFile
                check(target.path.startsWith(rootPath.path + File.separator)) { "插件压缩包写入路径越界" }
                if (entry.isDirectory) target.mkdirs() else {
                    target.parentFile?.mkdirs()
                    target.outputStream().buffered().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            expandedBytes += count
                            check(expandedBytes <= MAX_EXPANDED_BYTES) { "插件解压后超过 40 MB" }
                            output.write(buffer, 0, count)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun normalizeArchiveRoot(destination: File) {
        if (File(destination, "openclaw.plugin.json").isFile) return
        val children = destination.listFiles().orEmpty()
        if (children.size != 1 || !children[0].isDirectory || !File(children[0], "openclaw.plugin.json").isFile) return
        val wrappedRoot = children[0]
        wrappedRoot.listFiles().orEmpty().forEach { child ->
            check(child.renameTo(File(destination, child.name))) { "插件文件解包整理失败" }
        }
        check(wrappedRoot.delete()) { "插件临时目录清理失败" }
    }

    private fun validateDirectory(directory: File): JSONObject {
        val openClawFile = File(directory, "openclaw.plugin.json")
        val packageFile = File(directory, "package.json")
        check(openClawFile.isFile && packageFile.isFile) { "压缩包根目录需要 openclaw.plugin.json 和 package.json" }
        val descriptor = JSONObject(openClawFile.readText())
        val packageJson = JSONObject(packageFile.readText())
        val id = descriptor.optString("id")
        validateId(id)
        check(packageJson.optString("name") == id) { "package.json name 必须与插件 id 一致" }
        val version = packageJson.optString("version")
        validateVersion(version)
        check(packageJson.optString("type") == "module") { "插件 package.json 必须声明 type=module" }
        check((packageJson.optJSONObject("dependencies")?.length() ?: 0) == 0) {
            "当前仅支持无需额外 npm 依赖的自包含插件包"
        }
        val entrypoint = packageJson.optString("main", "index.js")
        check(entrypoint.endsWith(".js") || entrypoint.endsWith(".mjs")) { "仅支持 JavaScript 模块入口" }
        val entryFile = File(directory, entrypoint).canonicalFile
        check(entryFile.path.startsWith(directory.canonicalPath + File.separator) && entryFile.isFile) { "找不到插件入口文件" }
        check(entryFile.length() <= MAX_SOURCE_BYTES) { "插件入口源码超过 256 KB" }
        check(descriptor.optString("name").isNotBlank()) { "插件描述文件缺少 name" }
        val declaredTools = descriptor.optJSONObject("contracts")?.optJSONArray("tools") ?: JSONArray()
        check(declaredTools.length() in 1..MAX_TOOLS) { "插件需声明 1–20 个工具" }
        val names = HashSet<String>()
        for (index in 0 until declaredTools.length()) {
            val toolName = declaredTools.optString(index)
            check(toolName.matches(Regex("[a-z][a-z0-9_]{2,63}"))) { "插件工具名无效：$toolName" }
            check(names.add(toolName)) { "插件工具名重复：$toolName" }
        }
        return descriptor
    }

    private fun readPlugin(directory: File): ManagedOpenClawPlugin {
        val descriptor = validateDirectory(directory)
        val packageJson = JSONObject(File(directory, "package.json").readText())
        val metadataFile = File(directory, "jarvis-plugin.json")
        val metadata = if (metadataFile.isFile) JSONObject(metadataFile.readText()) else JSONObject()
        val toolsArray = descriptor.optJSONObject("contracts")?.optJSONArray("tools") ?: JSONArray()
        val tools = (0 until toolsArray.length()).map { toolsArray.optString(it) }.filter { it.isNotBlank() }
        return ManagedOpenClawPlugin(
            id = descriptor.getString("id"),
            name = descriptor.optString("name", descriptor.getString("id")),
            version = packageJson.optString("version"),
            description = descriptor.optString("description"),
            directory = directory,
            tools = tools,
            enabled = metadata.optBoolean("enabled", false),
            source = metadata.optString("source", "archive")
        )
    }

    private fun writeEnabled(directory: File, enabled: Boolean) {
        val descriptor = JSONObject(File(directory, "openclaw.plugin.json").readText())
        val packageJson = JSONObject(File(directory, "package.json").readText())
        val oldMetadata = File(directory, "jarvis-plugin.json").takeIf { it.isFile }
            ?.let { runCatching { JSONObject(it.readText()) }.getOrNull() } ?: JSONObject()
        File(directory, "jarvis-plugin.json").writeText(JSONObject()
            .put("id", descriptor.optString("id"))
            .put("name", descriptor.optString("name"))
            .put("version", packageJson.optString("version"))
            .put("description", descriptor.optString("description"))
            .put("source", oldMetadata.optString("source", "archive"))
            .put("enabled", enabled)
            .toString(2))
    }

    private fun pluginDirectory(id: String): File {
        validateId(id)
        val directory = File(root, id).canonicalFile
        check(directory.path.startsWith(root.canonicalPath + File.separator)) { "插件路径无效" }
        return directory
    }

    private fun validateId(id: String) {
        require(id.matches(Regex("[a-z][a-z0-9._-]{2,63}"))) { "插件 id 需使用小写字母、数字、点、短横线，长度 3–64" }
        require(id != "jarvis-device-bridge") { "不能替换 Jarvis 内置设备桥接插件" }
    }

    private fun validateVersion(version: String) {
        require(version.matches(Regex("[0-9A-Za-z][0-9A-Za-z.+_-]{0,39}"))) { "插件版本号无效" }
    }

    companion object {
        private const val MAX_ENTRIES = 2_000
        private const val MAX_TOOLS = 20
        private const val MAX_EXPANDED_BYTES = 40L * 1024 * 1024
        private const val MAX_SOURCE_BYTES = 256 * 1024
        private val UNSAFE_SOURCE = Regex(
            "\\beval\\s*\\(|\\bnew\\s+Function\\b|\\brequire\\s*\\(|\\bprocess\\b|\\bglobalThis\\b|\\bDeno\\b|\\bfetch\\s*\\(|WebSocket|XMLHttpRequest|child_process|node:(fs|net|http|https|os)|from\\s+['\"]fs['\"]",
            RegexOption.IGNORE_CASE
        )
    }
}
