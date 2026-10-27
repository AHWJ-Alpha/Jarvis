package com.jarvis.app.runtime

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OperationMemoryTest {
    @get:Rule val temp = TemporaryFolder()
    private var clock = 1_000_000L
    private fun memory() = OperationMemory(temp.root) { clock.also { clock += 1000 } }
    private val host = object : OperationHost {
        var appVersion = "1"
        var calls = mutableListOf<String>()
        var screen = ui()
        var file: JSONObject? = null
        var changeFile = false
        var denyRoot = false
        override fun inspect() = screen
        override fun version(packageName: String) = appVersion
        override fun fileState(path: String) = file
        override fun call(capability: String, arguments: JSONObject): CapabilityResult {
            calls.add(capability)
            if (capability == "device.info") return CapabilityResult(true, "device", JSONObject().put("model", "test"))
            if (capability == "accessibility.perform") screen = ui(arguments.optString("text"))
            if (capability == "root.exec" && denyRoot) return CapabilityResult(false, "完全模式已关闭")
            if (changeFile) file = JSONObject().put("path", "/output.png").put("size", 30).put("modified", clock + 1000)
            return CapabilityResult(true, "ok")
        }
    }

    private fun ui(text: String = "first") = JSONObject().put("active", true).put("package", "test.app").put("nodes", JSONArray()
        .put(JSONObject().put("viewId", "test:id/input").put("text", text).put("editable", true)))
    private fun probe() = JSONObject().put("kind", "probe").put("capability", "device.info").put("field", "data.model").put("equals", "test")
    private fun recipe(id: String = "test", dynamic: Boolean = false): JSONObject {
        val action = if (dynamic) JSONObject().put("capability", "accessibility.perform")
            .put("arguments", JSONObject().put("action", "input").put("package", "test.app")
                .put("selector", JSONObject().put("viewId", "test:id/input")).put("text", "{{query}}"))
            .put("before", JSONObject().put("kind", "ui").put("package", "test.app")
                .put("selector", JSONObject().put("viewId", "test:id/input")))
        else JSONObject().put("capability", "device.info")
        return JSONObject().put("id", id).put("title", "应用搜索").put("intent", "在应用搜索当前词语")
            .put("package", "test.app").put("parameters", if (dynamic) JSONArray().put("query") else JSONArray())
            .put("sampleParameters", if (dynamic) JSONObject().put("query", "first") else JSONObject())
            .put("steps", JSONArray().put(action)).put("verify", probe())
    }
    private fun save(m: OperationMemory, r: JSONObject = recipe()): CapabilityResult = m.execute(JSONObject().put("action", "save").put("recipe", r), host)
    private fun run(m: OperationMemory, params: JSONObject = JSONObject()) = m.execute(JSONObject().put("action", "run").put("id", "test").put("parameters", params), host)
    private fun find(m: OperationMemory): JSONObject = m.execute(JSONObject().put("action", "find").put("query", "搜索"), host).data.getJSONArray("recipes").getJSONObject(0)

    @Test fun verifiedRecipePersistsAcrossInstances() {
        assertTrue(save(memory()).ok)
        assertEquals("ready", find(memory()).getString("status"))
        assertEquals("test", find(memory()).getString("id"))
    }

    @Test fun currentParametersReplacePriorTextWithoutJsonInjection() {
        val m = memory()
        assertTrue(save(m, recipe(dynamic = true)).ok)
        val value = "新的\"搜索词\n{}"
        assertTrue(run(m, JSONObject().put("query", value)).ok)
        assertEquals(value, host.screen.getJSONArray("nodes").getJSONObject(0).getString("text"))
        assertFalse(temp.root.resolve("procedures.json").readText().contains("sampleParameters"))
    }

    @Test fun wrongParametersDoNotExecuteOrDisableRoute() {
        val m = memory()
        save(m, recipe(dynamic = true))
        host.calls.clear()
        assertFalse(run(m).ok)
        assertEquals(0, host.calls.size)
        assertEquals("ready", find(m).getString("status"))
    }

    @Test fun versionChangeStopsBeforeSideEffectsAndMarksStale() {
        val m = memory()
        save(m)
        host.calls.clear()
        host.appVersion = "2"
        val r = run(m)
        assertFalse(r.ok)
        assertTrue(r.data.getBoolean("replan"))
        assertEquals(0, host.calls.size)
        assertEquals("stale", find(m).getString("status"))
    }

    @Test fun uiMismatchStopsBeforeRemainingStepsAndCanBeRelearned() {
        val m = memory()
        val r = recipe(dynamic = true)
        r.getJSONArray("steps").getJSONObject(0).put("timeoutMs", 0)
        r.getJSONArray("steps").put(JSONObject().put("capability", "device.info"))
        assertTrue(save(m, r).ok)
        host.screen.put("package", "other.app")
        host.calls.clear()
        val failed = run(m, JSONObject().put("query", "new"))
        assertFalse(failed.ok)
        assertEquals(0, failed.data.getInt("completedSteps"))
        assertTrue(host.calls.isEmpty())
        assertEquals("stale", find(m).getString("status"))
        host.screen = ui()
        assertTrue(save(m, recipe(dynamic = true)).ok)
        assertTrue(run(m, JSONObject().put("query", "new")).ok)
        assertEquals(1, find(m).getInt("failures"))
    }

    @Test fun unsafeCoordinateRouteWithoutFingerprintIsRejected() {
        val r = recipe()
        r.put("steps", JSONArray().put(JSONObject().put("capability", "accessibility.click")
            .put("arguments", JSONObject().put("x", 10).put("y", 10))
            .put("before", JSONObject().put("kind", "ui").put("package", "test.app"))))
        assertFalse(save(memory(), r).ok)
    }

    @Test fun unverifiedSuccessIsNeverSaved() {
        val r = recipe().put("verify", probe().put("equals", "wrong"))
        assertFalse(save(memory(), r).ok)
        assertFalse(temp.root.resolve("procedures.json").exists())
    }

    @Test fun unchangedFileDoesNotCountAsDownload() {
        val m = memory()
        m.begin("download")
        host.file = JSONObject().put("path", "/output.png").put("size", 20).put("modified", clock + 1000)
        val r = recipe().put("verify", JSONObject().put("kind", "file").put("path", "/output.png"))
        assertTrue(save(m, r).ok)
        assertFalse(run(m).ok)
        assertEquals("stale", find(m).getString("status"))
    }

    @Test fun permissionsAreCheckedAgainOnEveryReplay() {
        val m = memory()
        val r = recipe().put("steps", JSONArray().put(JSONObject().put("capability", "root.exec")
            .put("arguments", JSONObject().put("command", "tool --fixed-option"))))
        assertTrue(save(m, r).ok)
        host.denyRoot = true
        assertFalse(run(m).ok)
        assertEquals("stale", find(m).getString("status"))
    }

    @Test fun shellParametersCannotBecomeCommands() {
        val r = recipe(dynamic = true)
        r.put("steps", JSONArray().put(JSONObject().put("capability", "root.exec").put("arguments", JSONObject().put("command", "tool {{query}}"))))
        assertFalse(save(memory(), r).ok)
    }

    @Test fun recordingConvertsCoordinatesToUniqueSelector() {
        val m = memory()
        m.begin("search")
        val screen = ui()
        screen.getJSONArray("nodes").getJSONObject(0).put("bounds", JSONObject().put("left", 0).put("top", 0).put("right", 100).put("bottom", 100))
        m.record("accessibility.click", JSONObject().put("x", 50).put("y", 50), screen, CapabilityResult(true, "ok"))
        val trace = m.execute(JSONObject().put("action", "recording"), host).data
        assertEquals("accessibility.perform", trace.getJSONArray("steps").getJSONObject(0).getJSONObject("step").getString("capability"))
    }

    @Test fun selectorRejectsAmbiguityAndEmptyCriteria() {
        assertFalse(OperationMemory.matches(JSONObject(), JSONObject()))
        assertFalse(OperationMemory.matches(JSONObject(), JSONObject().put("contains", true)))
        assertTrue(OperationMemory.matches(JSONObject().put("text", "Search all"), JSONObject().put("text", "Search").put("contains", true)))
    }

    @Test fun failureIncludesOriginalProcedureForRepair() {
        val m = memory()
        save(m)
        host.appVersion = "2"
        val failed = run(m)
        assertEquals("test", failed.data.getJSONObject("recipe").getString("id"))
        assertEquals("stale", failed.data.getJSONObject("recipe").getString("status"))
        assertEquals(1, m.execute(JSONObject().put("action", "get").put("id", "test"), host)
            .data.getJSONObject("recipe").getJSONArray("steps").length())
    }

    @Test fun corruptStoreIsReportedWithoutErasingLearnedData() {
        val m = memory()
        val path = temp.root.resolve("procedures.json")
        path.writeText("{broken")
        assertFalse(m.execute(JSONObject().put("action", "list"), host).ok)
        assertFalse(save(m).ok)
        assertEquals("{broken", path.readText())
    }
}
