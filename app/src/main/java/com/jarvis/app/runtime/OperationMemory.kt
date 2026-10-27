package com.jarvis.app.runtime

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock

/** App-private, versioned procedures. The host always uses the ordinary permission-checked bridge. */
interface OperationHost {
    fun call(capability: String, arguments: JSONObject): CapabilityResult
    fun inspect(): JSONObject
    fun version(packageName: String): String?
    fun fileState(path: String): JSONObject?
}

class OperationMemory(private val directory: File, private val now: () -> Long = System::currentTimeMillis) {
    private val file = File(directory, "procedures.json")
    private val traceFile = File(directory, "last-recording.json")
    private val lock = ReentrantLock()
    private var replaying = false

    @Synchronized fun begin(task: String): String {
        val id = UUID.randomUUID().toString()
        write(traceFile, JSONObject().put("id", id).put("task", task.take(500))
            .put("startedAt", now()).put("steps", JSONArray()))
        return id
    }

    @Synchronized fun record(capability: String, arguments: JSONObject, before: JSONObject, result: CapabilityResult) {
        if (replaying || capability !in recordable || !traceFile.exists()) return
        val trace = read(traceFile)
        val steps = trace.optJSONArray("steps") ?: JSONArray()
        if (steps.length() >= 100) return
        val guard = JSONObject().put("kind", "ui").put("package", before.optString("package"))
        var cap = capability
        var args = JSONObject(arguments.toString())
        if (capability == "accessibility.click") {
            val selector = selectorAt(before, arguments.optDouble("x"), arguments.optDouble("y"))
            if (selector != null) {
                cap = "accessibility.perform"
                args = JSONObject().put("action", "click").put("package", before.optString("package")).put("selector", selector)
                guard.put("selector", selector)
            } else guard.put("fingerprint", fingerprint(before))
        } else if (capability == "accessibility.perform") {
            arguments.optJSONObject("selector")?.let { guard.put("selector", it) }
        } else if (capability.startsWith("accessibility.")) {
            guard.put("fingerprint", fingerprint(before))
        }
        val step = JSONObject().put("capability", cap).put("arguments", args)
        if (cap.startsWith("accessibility.")) step.put("before", guard)
        steps.put(JSONObject().put("index", steps.length()).put("ok", result.ok)
            .put("time", now()).put("step", step))
        trace.put("steps", steps)
        write(traceFile, trace)
    }

    fun execute(args: JSONObject, host: OperationHost): CapabilityResult = try {
        when (args.optString("action")) {
            "find", "list" -> find(args.optString("query"), args.optString("package"))
            "get" -> get(args.getString("id"))
            "recording" -> synchronized(this) { CapabilityResult(true, "最近任务的操作记录；只提取成功且必要的步骤，并把动态内容参数化", read(traceFile)) }
            "save" -> save(args.getJSONObject("recipe"), host)
            "run" -> run(args.getString("id"), args.optJSONObject("parameters") ?: JSONObject(), host)
            "invalidate" -> invalidate(args.getString("id"), args.optString("reason", "用户或智能体报告路径失效"))
            "forget" -> forget(args.getString("id"))
            else -> CapabilityResult(false, "action 应为 find/list/get/recording/save/run/invalidate/forget")
        }
    } catch (error: Exception) { CapabilityResult(false, "操作记忆：${error.message}") }

    @Synchronized fun summary(): String {
        val recipes = recipes()
        val active = (0 until recipes.length()).count { recipes.getJSONObject(it).optString("status") == "ready" }
        return "已保存 ${recipes.length()} 条操作路径，$active 条可复用。失败路径会停用，重新探索成功后更新。"
    }

    @Synchronized private fun find(query: String, packageName: String): CapabilityResult {
        val terms = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        val all = recipes()
        val ranked = (0 until all.length()).map { all.getJSONObject(it) }
            .filter { packageName.isBlank() || it.optString("package") == packageName }
            .map { r ->
                val haystack = "${r.optString("title")} ${r.optString("intent")} ${r.optString("package")}".lowercase()
                var score = terms.sumOf { if (haystack.contains(it)) 10 else if (it.any { c -> c.code > 255 }) it.toSet().count { c -> haystack.contains(c) } else 0 }
                if (r.optString("status") == "ready") score += 1
                r to score
            }.sortedByDescending { it.second }.take(12)
        val output = JSONArray()
        for ((r, _) in ranked) output.put(JSONObject().put("id", r.getString("id"))
            .put("title", r.optString("title")).put("intent", r.optString("intent"))
            .put("package", r.optString("package")).put("status", r.optString("status"))
            .put("parameters", r.optJSONArray("parameters") ?: JSONArray())
            .put("steps", r.getJSONArray("steps").length()).put("successes", r.optInt("successes"))
            .put("failures", r.optInt("failures")).put("lastFailure", r.optString("lastFailure")))
        return CapabilityResult(true, "仅复用意图和目标一致的 ready 路径；stale 路径应重新探索并用同一 id 保存替代", JSONObject().put("recipes", output))
    }

    @Synchronized private fun get(id: String): CapabilityResult {
        val all = recipes()
        val r = (0 until all.length()).map { all.getJSONObject(it) }.firstOrNull { it.optString("id") == id }
        return if (r == null) CapabilityResult(false, "路径不存在")
            else CapabilityResult(true, "已读取路径；保留仍有效的步骤，修复失效部分后重新核实保存", JSONObject().put("recipe", r))
    }

    @Synchronized private fun save(input: JSONObject, host: OperationHost): CapabilityResult {
        val r = JSONObject(input.toString())
        val id = r.optString("id").ifBlank { UUID.randomUUID().toString() }
        require(id.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { "id 无效" }
        require(r.optString("title").isNotBlank() && r.optString("intent").isNotBlank()) { "需要 title 和 intent" }
        val steps = r.getJSONArray("steps")
        require(steps.length() in 1..40) { "每条路径需要 1–40 个步骤" }
        val params = r.optJSONArray("parameters") ?: JSONArray()
        val names = (0 until params.length()).map { params.getString(it) }.toSet()
        require(names.all { it.matches(Regex("[A-Za-z][A-Za-z0-9_]{0,39}")) }) { "参数名无效" }
        val placeholders = template.findAll(r.toString()).map { it.groupValues[1] }.toSet()
        require(names == placeholders) { "parameters 必须列出全部 {{name}} 参数，不得多列或漏列" }
        for (i in 0 until steps.length()) validateStep(steps.getJSONObject(i))
        val verification = r.getJSONObject("verify")
        validateCondition(verification)
        val sample = r.optJSONObject("sampleParameters") ?: JSONObject()
        val resolved = resolve(verification, sample) as JSONObject
        val evidence = check(resolved, host, 5000)
        require(evidence.ok) { "未能独立核实成功条件，未保存：${evidence.message}" }
        if (resolved.optString("kind") == "file") {
            val started = read(traceFile).optLong("startedAt", now())
            require(evidence.data.optLong("modified") >= started - 2000) { "文件早于本次任务，不能当作新操作成功证据" }
        }
        val existing = recipes()
        val old = (0 until existing.length()).map { existing.getJSONObject(it) }.firstOrNull { it.optString("id") == id }
        val packageName = r.optString("package")
        require(packageName.isBlank() || !packageName.contains("{{")) { "应用包名不能参数化" }
        val version = if (packageName.isNotBlank()) host.version(packageName) ?: error("目标应用未安装") else ""
        r.put("id", id).put("schema", 1).put("status", "ready").put("appVersion", version)
            .put("parameters", params).put("updatedAt", now()).put("createdAt", old?.optLong("createdAt") ?: now())
            .put("successes", (old?.optInt("successes") ?: 0) + 1).put("failures", old?.optInt("failures") ?: 0)
            .put("revision", (old?.optInt("revision") ?: 0) + 1).put("lastFailure", "")
        r.remove("sampleParameters") // Dynamic message/search values are not part of a reusable procedure.
        put(r)
        return CapabilityResult(true, "已核实并保存操作路径", JSONObject().put("id", id).put("revision", r.getInt("revision")).put("verification", evidence.data))
    }

    private fun run(id: String, parameters: JSONObject, host: OperationHost): CapabilityResult {
        if (!lock.tryLock()) return CapabilityResult(false, "另一条操作路径正在执行")
        val started = now()
        var completed = 0
        var executing = false
        try {
            val original = synchronized(this) {
                val all = recipes()
                (0 until all.length()).map { all.getJSONObject(it) }.firstOrNull { it.optString("id") == id }
            } ?: return CapabilityResult(false, "找不到操作路径")
            if (original.optString("status") != "ready") return CapabilityResult(false, "路径已停用，请重新探索后保存更新", JSONObject().put("replan", true))
            val names = original.getJSONArray("parameters")
            require(parameters.keys().asSequence().toSet() == (0 until names.length()).map { names.getString(it) }.toSet()) { "请准确提供路径要求的 parameters" }
            val r = resolve(original, parameters) as JSONObject
            val pkg = r.optString("package")
            if (pkg.isNotBlank() && host.version(pkg) != original.optString("appVersion")) return fail(id, -1, completed, started, "应用版本已变化", host)
            val verification = r.getJSONObject("verify")
            val baseline = if (verification.optString("kind") == "file") host.fileState(verification.getString("path"))?.toString() else null
            synchronized(this) { replaying = true }
            executing = true
            val steps = r.getJSONArray("steps")
            for (i in 0 until steps.length()) {
                if (now() - started > 120_000) return fail(id, i, completed, started, "路径执行超过两分钟", host)
                val step = steps.getJSONObject(i)
                step.optJSONObject("before")?.let {
                    val checked = check(it, host, step.optInt("timeoutMs", 5000).coerceIn(0, 8000))
                    if (!checked.ok) return fail(id, i, completed, started, "前置条件不匹配：${checked.message}", host)
                }
                val cap = step.getString("capability")
                val args = step.optJSONObject("arguments") ?: JSONObject()
                val result = host.call(cap, args)
                if (!result.ok) return fail(id, i, completed, started, result.message, host)
                completed++
                if (cap == "app.launch") {
                    val launched = check(JSONObject().put("kind", "ui").put("package", args.getString("package")), host, 8000)
                    if (!launched.ok) return fail(id, i, completed, started, "应用启动后未进入目标界面", host)
                }
                step.optJSONObject("after")?.let {
                    val checked = check(it, host, step.optInt("timeoutMs", 5000).coerceIn(0, 8000))
                    if (!checked.ok) return fail(id, i, completed, started, "后置条件不匹配：${checked.message}", host)
                }
            }
            val verified = check(verification, host, 8000)
            if (!verified.ok) return fail(id, steps.length(), completed, started, "结果未通过核实：${verified.message}", host)
            if (verification.optString("kind") == "file" && baseline == verified.data.toString()) return fail(id, steps.length(), completed, started, "文件未新增或更新，不能误报下载成功", host)
            synchronized(this) {
                original.put("successes", original.optInt("successes") + 1).put("lastUsedAt", now()).put("lastDurationMs", now() - started)
                put(original)
            }
            return CapabilityResult(true, "路径执行成功且结果已核实", JSONObject().put("id", id).put("completedSteps", completed)
                .put("durationMs", now() - started).put("verification", verified.data))
        } catch (error: Exception) {
            if (executing) return fail(id, completed, completed, started, error.message ?: "执行异常", host)
            // Invalid parameters do not imply that an otherwise valid route is obsolete.
            return CapabilityResult(false, "路径未完成：${error.message}", JSONObject().put("completedSteps", completed).put("replan", true))
        } finally {
            synchronized(this) { replaying = false }
            lock.unlock()
        }
    }

    private fun fail(id: String, index: Int, completed: Int, started: Long, reason: String, host: OperationHost): CapabilityResult {
        invalidate(id, reason)
        return CapabilityResult(false, "旧路径已停止并标为失效；根据当前界面重新探索，成功后用同一 id 保存更新。已执行步骤不会自动回滚。",
            JSONObject().put("id", id).put("failedStep", index).put("completedSteps", completed).put("reason", reason)
                .put("durationMs", now() - started).put("replan", true).put("screen", host.inspect())
                .put("recipe", get(id).data.optJSONObject("recipe") ?: JSONObject()))
    }

    @Synchronized private fun invalidate(id: String, reason: String): CapabilityResult {
        val all = recipes()
        val r = (0 until all.length()).map { all.getJSONObject(it) }.firstOrNull { it.optString("id") == id }
            ?: return CapabilityResult(false, "路径不存在")
        r.put("status", "stale").put("lastFailure", reason.take(300)).put("failures", r.optInt("failures") + 1)
        put(r)
        return CapabilityResult(true, "已停用失效路径")
    }

    @Synchronized private fun forget(id: String): CapabilityResult {
        val output = JSONArray()
        val all = recipes()
        for (i in 0 until all.length()) if (all.getJSONObject(i).optString("id") != id) output.put(all.getJSONObject(i))
        write(file, JSONObject().put("schema", 1).put("recipes", output))
        return CapabilityResult(true, "已删除操作路径")
    }

    private fun validateStep(step: JSONObject) {
        val cap = step.getString("capability")
        require(cap in recordable || cap in probes) { "不支持保存此能力：$cap" }
        val args = step.optJSONObject("arguments") ?: JSONObject()
        if (cap.startsWith("accessibility.") && cap != "accessibility.inspect") {
            val before = step.getJSONObject("before")
            require(before.optString("kind") == "ui" && before.optString("package").isNotBlank()) { "界面操作必须有应用前置条件" }
            if (cap == "accessibility.click" || cap == "accessibility.input" || cap == "accessibility.swipe") {
                require(before.optString("fingerprint").isNotBlank()) { "坐标、通用输入和滑动必须绑定界面指纹；优先使用 accessibility.perform 的唯一选择器" }
            }
            if (cap == "accessibility.perform") {
                require(args.optString("package").isNotBlank() && args.has("selector")) { "perform 必须指定 package 和 selector" }
            }
        }
        require(cap != "root.exec" || !template.containsMatchIn(args.optString("command"))) { "root 命令不支持直接插入动态参数；请使用固定工具脚本或其它参数化能力" }
        step.optJSONObject("before")?.let(::validateCondition)
        step.optJSONObject("after")?.let(::validateCondition)
    }

    private fun validateCondition(c: JSONObject) {
        when (c.optString("kind")) {
            "ui" -> require(c.optString("package").isNotBlank()) { "界面条件需要 package" }
            "file" -> require(c.optString("path").startsWith("/")) { "文件条件需要绝对路径" }
            "probe" -> require(c.optString("capability") in probes && c.optString("field").isNotBlank() && (c.has("equals") || c.has("contains"))) { "probe 仅支持读取能力及 field + equals/contains" }
            else -> error("条件 kind 应为 ui/file/probe")
        }
    }

    private fun check(c: JSONObject, host: OperationHost, timeout: Int): CapabilityResult {
        validateCondition(c)
        val deadline = now() + timeout
        var result: CapabilityResult
        do {
            result = when (c.getString("kind")) {
                "ui" -> {
                    val ui = host.inspect()
                    val selector = c.optJSONObject("selector")
                    val nodes = ui.optJSONArray("nodes") ?: JSONArray()
                    val matches = selector == null || (0 until nodes.length()).count { matches(nodes.getJSONObject(it), selector) } == 1
                    val ok = ui.optBoolean("active") && ui.optString("package") == c.getString("package") && matches &&
                        (!c.has("fingerprint") || fingerprint(ui) == c.getString("fingerprint"))
                    CapabilityResult(ok, if (ok) "界面条件符合" else "当前应用/控件/界面指纹不匹配", JSONObject().put("package", ui.optString("package")))
                }
                "file" -> {
                    val state = host.fileState(c.getString("path"))
                    CapabilityResult(state != null && state.optLong("size") >= c.optLong("minBytes", 1), "检查文件存在和大小", state ?: JSONObject())
                }
                else -> {
                    val read = host.call(c.getString("capability"), c.optJSONObject("arguments") ?: JSONObject())
                    var value: Any? = JSONObject().put("data", read.data).put("message", read.message)
                    for (key in c.getString("field").split('.')) value = (value as? JSONObject)?.opt(key)
                    val ok = read.ok && if (c.has("equals")) value?.toString() == c.get("equals").toString() else value?.toString()?.contains(c.getString("contains")) == true
                    CapabilityResult(ok, "读取结果条件${if (ok) "符合" else "不匹配"}", JSONObject().put("value", value ?: JSONObject.NULL))
                }
            }
            if (result.ok || now() >= deadline) return result
            Thread.sleep(180)
        } while (true)
    }

    @Synchronized private fun recipes(): JSONArray = read(file).optJSONArray("recipes") ?: JSONArray()
    @Synchronized private fun put(recipe: JSONObject) {
        val all = recipes()
        val output = JSONArray()
        for (i in 0 until all.length()) if (all.getJSONObject(i).optString("id") != recipe.getString("id")) output.put(all.getJSONObject(i))
        require(output.length() < 500) { "操作路径已达 500 条，请删除无用路径" }
        output.put(recipe)
        write(file, JSONObject().put("schema", 1).put("recipes", output))
    }

    private fun read(target: File): JSONObject {
        if (!target.exists()) return JSONObject()
        // Corrupt storage must never silently erase previously learned routes.
        return JSONObject(target.readText())
    }

    private fun write(target: File, value: JSONObject) {
        directory.mkdirs()
        val temp = File.createTempFile("${target.name}.", ".new", directory)
        try {
            temp.outputStream().use { out ->
                out.write(value.toString().toByteArray(Charsets.UTF_8))
                out.fd.sync()
            }
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (error: Exception) {
            temp.delete()
            throw IllegalStateException("操作记忆写入失败：${error.message}", error)
        }
    }

    companion object {
        private val instances = mutableMapOf<String, OperationMemory>()
        @Synchronized fun at(filesDir: File): OperationMemory = instances.getOrPut(filesDir.absolutePath) { OperationMemory(File(filesDir, "jarvis/operation-memory")) }
        private val template = Regex("\\{\\{([A-Za-z][A-Za-z0-9_]{0,39})\\}\\}")
        private val probes = setOf("device.info", "accessibility.inspect", "ocr.read", "notification.read")
        private val recordable = setOf("app.launch", "accessibility.click", "accessibility.input", "accessibility.global", "accessibility.perform", "accessibility.swipe", "root.exec", "camera.capture")

        fun fingerprint(ui: JSONObject): String {
            val parts = StringBuilder(ui.optString("package")).append(ui.optJSONObject("bounds"))
            val nodes = ui.optJSONArray("nodes") ?: JSONArray()
            for (i in 0 until nodes.length()) {
                val n = nodes.getJSONObject(i)
                parts.append(n.optString("viewId")).append('|').append(n.optString("text")).append('|')
                    .append(n.optString("description")).append('|').append(n.optJSONObject("bounds")).append('\n')
            }
            return MessageDigest.getInstance("SHA-256").digest(parts.toString().toByteArray()).joinToString("") { "%02x".format(it) }
        }

        fun matches(node: JSONObject, selector: JSONObject): Boolean {
            val keys = selector.keys().asSequence().toList()
            if (keys.isEmpty() || keys.any { it !in setOf("viewId", "text", "description", "class", "editable", "contains") }) return false
            if (keys.all { it == "contains" }) return false
            return keys.filter { it != "contains" }.all { key ->
                if (key == "editable") node.optBoolean(key) == selector.optBoolean(key)
                else if (selector.optBoolean("contains") && key in setOf("text", "description")) node.optString(key).contains(selector.getString(key))
                else node.optString(key) == selector.getString(key)
            }
        }

        private fun selectorAt(ui: JSONObject, x: Double, y: Double): JSONObject? {
            val nodes = ui.optJSONArray("nodes") ?: return null
            val candidates = (0 until nodes.length()).map { nodes.getJSONObject(it) }.filter { n ->
                val b = n.optJSONObject("bounds") ?: return@filter false
                x >= b.optInt("left") && x < b.optInt("right") && y >= b.optInt("top") && y < b.optInt("bottom")
            }.sortedBy { n -> val b = n.getJSONObject("bounds"); (b.getInt("right") - b.getInt("left")).toLong() * (b.getInt("bottom") - b.getInt("top")) }
            for (n in candidates) {
                val selector = JSONObject()
                for (key in listOf("viewId", "text", "description")) if (n.optString(key).isNotBlank()) selector.put(key, n.getString(key))
                if (selector.length() > 0 && (0 until nodes.length()).count { matches(nodes.getJSONObject(it), selector) } == 1) return selector
            }
            return null
        }

        private fun resolve(value: Any?, parameters: JSONObject): Any? = when (value) {
            is JSONObject -> JSONObject().also { out -> value.keys().forEach { out.put(it, resolve(value.get(it), parameters)) } }
            is JSONArray -> JSONArray().also { out -> for (i in 0 until value.length()) out.put(resolve(value.get(i), parameters)) }
            is String -> template.replace(value) { match ->
                val name = match.groupValues[1]
                require(parameters.has(name) && parameters.get(name) is String) { "缺少字符串参数 $name" }
                parameters.getString(name).also { require(it.length <= 4000) { "参数过长" } }
            }
            else -> value
        }

        val instructions = """
            操作记忆（所有应用和工具任务通用）：开始设备任务先调用 jarvis_operation_memory action=find，query 用简短意图，可加 package。
            只有目标和动作一致的 ready 路径才用 action=run,id,parameters 复用；把本次搜索词、联系人、消息、路径等填入命名参数，不能沿用上次对象。
            若 run 返回 replan=true，旧路径已停止；先看返回的当前 screen，从实际状态重新探索，不要重做已经发送/保存的动作。
            用返回的 recipe 或 action=get,id 读取原路径，保留有效步骤、修复失效部分，再核实并用同一 id 保存完整路径。
            首次或失败后通过普通设备工具探索，成功后 action=recording 读取自动记录，删去绕路和失败步骤，action=save 保存或以原 id 更新。
            recipe 格式：{title,intent,package,parameters:["query"],sampleParameters:{query:"本次值"},steps:[{capability,arguments,before?,after?}],verify}。
            文本动态值写成 {{query}}；sampleParameters 只用于本次核实，不存入路径。before/after/verify 条件为：
            {kind:"ui",package,selector?:{viewId?,text?,description?,class?,editable?,contains?},fingerprint?}；
            {kind:"file",path,minBytes:1}（必须是本次新增/更新文件，不能把截图当下载）；或 {kind:"probe",capability:"device.info",field:"data.model",equals:"实际值"}。
            选择器必须唯一。界面操作优先 accessibility.perform arguments={action:"click"或"input",package,selector,text?}，before 指明目标 package 和 selector。
            原始坐标、通用输入和滑动步骤必须使用最近 inspect 的 fingerprint 作为 before，界面变化会停止。root 命令每次仍检查完全模式，禁止直接拼接动态参数。
            只有工具独立核实 verify 成功才允许保存或报告完成；路径失败会停用，重新探索成功后必须更新路径。不要调用不存在的 write 工具。
        """.trimIndent()
    }
}
