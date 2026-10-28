package com.jarvis.app.runtime

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchivedConversationMigrationTest {
    /** 可复现的假偏好源：模拟 SharedPreferences 里的 archivedConversationIds 键。 */
    private class FakeIdSource(private val initial: Set<String>) : ArchivedIdSource {
        var current: Set<String> = initial.toSet()
            private set
        var clearCalls = 0
            private set

        override fun read(): Set<String> = current

        override fun clear() {
            clearCalls++
            current = emptySet()
        }
    }

    private fun session(id: String, archivedAt: Long? = null) =
        ChatSession(id = id, title = id, workspace = "创作", archivedAt = archivedAt)

    @Test
    fun legacyJsonWithoutArchivedAtIsNotArchived() {
        val legacy = ChatSession(id = "a", title = "旧对话")
        val json = JSONObject(legacy.toJson().toString())
        json.remove("archivedAt")

        val restored = ChatSession.fromJson(json)
        assertFalse(restored!!.isArchived)
        assertNull(restored.archivedAt)
    }

    @Test
    fun archivedAtRoundTripsAndZeroMeansNotArchived() {
        val archived = session("a", archivedAt = 1_700_000_000_000L)
        assertEquals(archived, ChatSession.fromJson(JSONObject(archived.toJson().toString())))

        val zero = ChatSession.fromJson(JSONObject().put("id", "b").put("archivedAt", 0))
        assertFalse(zero!!.isArchived)
    }

    @Test
    fun migrationWritesLegacyIdsIntoArchivedAtAndClearsPrefs() {
        val legacy = FakeIdSource(setOf("a", "c", "gone"))
        val sessions = listOf(session("a"), session("b"), session("c"))
        val persisted = mutableListOf<List<ChatSession>>()

        val result = migrateArchivedConversations(legacy, sessions, 1_000L) { next ->
            persisted += next
            true
        }

        assertEquals(2, result.changed)
        assertTrue(result.prefsCleared)
        assertEquals(emptySet<String>(), legacy.current)
        assertEquals(1, legacy.clearCalls)
        assertEquals(listOf("a", "c"), result.sessions.filter { it.isArchived }.map { it.id })
        assertEquals(1, persisted.size)
    }

    @Test
    fun secondMigrationRunIsANoOp() {
        val legacy = FakeIdSource(setOf("a"))
        val sessions = listOf(session("a"), session("b"))
        val first = migrateArchivedConversations(legacy, sessions, 1_000L) { true }
        assertEquals(1, first.changed)

        // 第二次启动：偏好键已被清空，即便再调用也不会重复迁移。
        val second = migrateArchivedConversations(legacy, first.sessions, 2_000L) { error("不应再次写盘") }
        assertEquals(0, second.changed)
        assertFalse(second.prefsCleared)
        assertEquals(first.sessions, second.sessions)
        assertEquals(1, legacy.clearCalls)
    }

    @Test
    fun planIsIdempotentOnItsOwnOutput() {
        val sessions = listOf(session("a"), session("b", archivedAt = 500L))
        val once = planArchivedMigration(sessions, setOf("a", "b"), 1_000L)
        assertEquals(listOf("a", "b"), once.filter { it.isArchived }.map { it.id })
        // 已归档的 b 保留原时间戳，不被重写。
        assertEquals(500L, once.first { it.id == "b" }.archivedAt)
        assertEquals(once, planArchivedMigration(once, setOf("a", "b"), 2_000L))
    }

    @Test
    fun failedWriteKeepsPrefsSoTheNextLaunchRetries() {
        val legacy = FakeIdSource(setOf("a"))
        val sessions = listOf(session("a"))

        val failed = migrateArchivedConversations(legacy, sessions, 1_000L) { false }
        assertEquals(0, failed.changed)
        assertFalse(failed.prefsCleared)
        assertEquals(setOf("a"), legacy.current)
        assertEquals(0, legacy.clearCalls)
        assertEquals(sessions, failed.sessions)

        val retried = migrateArchivedConversations(legacy, sessions, 2_000L) { true }
        assertEquals(1, retried.changed)
        assertTrue(retried.prefsCleared)
    }

    @Test
    fun archiveQueriesDeriveFromArchivedAt() {
        val sessions = listOf(session("a", archivedAt = 900L), session("b"))
        assertEquals(listOf("b"), ConversationStore.visibleSessions(sessions).map { it.id })
        assertEquals(listOf("a", "b"), ConversationStore.visibleSessions(sessions, includeArchived = true).map { it.id })
        assertEquals(listOf("a"), ConversationStore.archivedIds(sessions))
    }
}
