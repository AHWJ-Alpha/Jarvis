package com.jarvis.app.runtime

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes agent runs that may drive the single Android accessibility session. */
object DeviceTaskCoordinator {
    const val CANCELLED_BY_CONVERSATION_DELETE = DeviceTaskRegistry.CANCELLED_BY_CONVERSATION_DELETE

    private val mutex = Mutex()
    private val registry = DeviceTaskRegistry()
    private val sequence = AtomicLong()

    fun activeTask(): String? = registry.running().firstOrNull()?.label

    /** 任务归属键：删除会话时只取消属于该会话的动作，其它会话与无障碍服务不受影响。 */
    fun conversationOwner(conversationId: String): String = "conversation:$conversationId"

    suspend fun <T> run(label: String, ownerKey: String? = null, block: suspend () -> T): T = mutex.withLock {
        val entry = registry.begin("task-${sequence.incrementAndGet()}", ownerKey.orEmpty(), label)
        FloatingAssistantController.taskStarted(label)
        try {
            val result = block()
            FloatingAssistantController.taskFinished(if (entry.cancelled) entry.result else result.toString())
            result
        } catch (error: Throwable) {
            FloatingAssistantController.taskFinished(
                if (entry.cancelled) entry.result else "任务结束：${error.message ?: error.javaClass.simpleName}"
            )
            throw error
        } finally {
            registry.finish(entry.id)
        }
    }

    /** 幂等取消：owner 匹配进行中的任务时写取消结果并执行关闭回调，重复调用不会重复取消。 */
    fun cancel(
        ownerKey: String,
        reason: String = CANCELLED_BY_CONVERSATION_DELETE,
        blockFutureRuns: Boolean = false
    ): List<DeviceTaskRegistry.Entry> = registry.cancel(ownerKey, reason, blockFutureRuns)

    /** 注册取消回调（例如断开阻塞中的 HTTP 请求），让被取消的任务尽快收敛。 */
    fun registerCancelHook(ownerKey: String, hook: () -> Unit) = registry.registerHook(ownerKey, hook)

    /** 能力调用闸门：被取消的任务收敛前拒绝一切新的设备操作，这是真正停住无障碍操作的入口。 */
    fun blockedReason(): String? = registry.blockedReason()

    /** 被取消任务在收敛后仍保留取消原因，调用方据此写终态结果。 */
    fun cancelledReasonFor(ownerKey: String): String? = registry.cancelledReasonFor(ownerKey)
}

/** A scoped marker lets the capability bridge enforce the separate remote-device permission. */
object RemoteExecutionGuard {
    @Volatile var active: Boolean = false
        private set
    @Volatile var deviceControlAllowed: Boolean = false
        private set

    suspend fun <T> run(allowDeviceControl: Boolean, block: suspend () -> T): T {
        active = true
        deviceControlAllowed = allowDeviceControl
        try {
            return block()
        } finally {
            deviceControlAllowed = false
            active = false
        }
    }
}
