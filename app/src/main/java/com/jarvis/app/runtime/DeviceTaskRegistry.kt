package com.jarvis.app.runtime

/**
 * 设备动作的取消登记表：记录「哪个任务在跑、属于哪个 owner、是否已被取消」。
 *
 * 纯 Kotlin、不依赖 Android，删除会话时取消在跑任务的逻辑由单元测试直接覆盖。
 * owner 是任务归属键（会话 / 自动化规则），取消只作用于同一个 owner，避免牵连其它会话。
 */
class DeviceTaskRegistry {
    enum class State { RUNNING, CANCELED }

    class Entry internal constructor(
        val id: String,
        val ownerKey: String,
        val label: String
    ) {
        @Volatile var state: State = State.RUNNING
            internal set
        @Volatile var result: String = ""
            internal set

        val cancelled: Boolean get() = state == State.CANCELED
    }

    private val lock = Any()
    private val entries = LinkedHashMap<String, Entry>()
    private val ownerHooks = HashMap<String, MutableList<() -> Unit>>()
    private val cancelledReasons = HashMap<String, String>()
    private val blockedOwners = HashMap<String, String>()

    fun begin(id: String, ownerKey: String, label: String): Entry = synchronized(lock) {
        val blocked = blockedOwners.remove(ownerKey)
        val entry = Entry(id, ownerKey, label)
        if (blocked != null) {
            // 取消发生在任务启动之前（例如还排在排队锁上）：新任务直接以取消态开始，
            // 调用方随后注册的关闭回调会立即执行，请求不会真正发出。
            entry.state = State.CANCELED
            entry.result = blocked
            cancelledReasons[ownerKey] = blocked
        } else {
            cancelledReasons.remove(ownerKey)
        }
        entries[id] = entry
        entry
    }

    fun entry(id: String): Entry? = synchronized(lock) { entries[id] }

    fun running(): List<Entry> = synchronized(lock) { entries.values.toList() }

    fun finish(id: String) = synchronized(lock) {
        val removed = entries.remove(id) ?: return@synchronized
        // 任务正常收敛后丢弃它的关闭回调，避免同一会话多次请求累积无效回调。
        if (entries.values.none { it.ownerKey == removed.ownerKey }) ownerHooks.remove(removed.ownerKey)
    }

    /**
     * 幂等取消：同一 owner 只有第一次取消会改动任务；返回本次真正被取消的任务。
     * 已结束或已取消的任务不会重复取消，重复调用返回空列表。
     */
    fun cancel(ownerKey: String, reason: String, blockFutureRuns: Boolean = false): List<Entry> {
        val (matched, hooks) = synchronized(lock) {
            val hits = entries.values.filter { it.ownerKey == ownerKey && it.state == State.RUNNING }
            hits.forEach {
                it.state = State.CANCELED
                it.result = reason
            }
            cancelledReasons[ownerKey] = reason
            if (blockFutureRuns) blockedOwners[ownerKey] = reason
            hits to ownerHooks.remove(ownerKey).orEmpty()
        }
        hooks.forEach { hook -> runCatching { hook() } }
        return matched
    }

    /**
     * 注册取消回调（例如断开阻塞中的 HTTP 连接）；owner 已被取消时立即执行。
     */
    fun registerHook(ownerKey: String, hook: () -> Unit) {
        val cancelledAlready = synchronized(lock) {
            if (entries.values.any { it.ownerKey == ownerKey && it.state == State.CANCELED }) return@synchronized true
            if (entries.values.none { it.ownerKey == ownerKey }) return@synchronized false
            ownerHooks.getOrPut(ownerKey) { mutableListOf() }.add(hook)
            false
        }
        if (cancelledAlready) runCatching { hook() }
    }

    /** 能力调用闸门：还有未收敛的取消任务时返回取消原因，调用方必须拒绝继续操作。 */
    fun blockedReason(): String? = synchronized(lock) {
        entries.values.firstOrNull { it.state == State.CANCELED }?.let { entry ->
            entry.result.ifBlank { CANCELLED_BY_CONVERSATION_DELETE }
        }
    }

    /** 该 owner 最近一次的取消原因：任务收敛后仍可据此写终态结果，不留悬空状态。 */
    fun cancelledReasonFor(ownerKey: String): String? = synchronized(lock) { cancelledReasons[ownerKey] }

    fun reset() = synchronized(lock) {
        entries.clear()
        ownerHooks.clear()
        cancelledReasons.clear()
        blockedOwners.clear()
    }

    companion object {
        const val CANCELLED_BY_CONVERSATION_DELETE = "已取消：会话被删除"
    }
}
