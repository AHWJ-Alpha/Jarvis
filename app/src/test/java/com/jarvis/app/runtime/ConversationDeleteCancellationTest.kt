package com.jarvis.app.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

/** 删除会话 -> 进行中任务被取消且状态为 canceled。 */
class ConversationDeleteCancellationTest {
    private val reason = DeviceTaskRegistry.CANCELLED_BY_CONVERSATION_DELETE

    @Test fun deletingConversationCancelsRunningTaskAndMarksItCanceled() {
        val registry = DeviceTaskRegistry()
        val entry = registry.begin("task-1", "conversation:conv-1", "本机对话")
        val disconnected = AtomicBoolean(false)
        registry.registerHook("conversation:conv-1") { disconnected.set(true) }

        val cancelled = registry.cancel("conversation:conv-1", reason)

        assertEquals(1, cancelled.size)
        assertEquals(DeviceTaskRegistry.State.CANCELED, entry.state)
        assertEquals("已取消：会话被删除", entry.result)
        assertTrue("取消必须断开阻塞中的请求，任务才能尽快收敛", disconnected.get())
        // 收敛前闸门拒绝后续设备能力：这是真正停住无障碍操作的入口
        assertEquals(reason, registry.blockedReason())

        registry.finish(entry.id)

        assertNull("任务收敛后闸门必须放行，不能永久锁死设备操作", registry.blockedReason())
        assertEquals("结束的任务仍保留取消终态，不留悬空运行态", DeviceTaskRegistry.State.CANCELED, entry.state)
        assertEquals(reason, registry.cancelledReasonFor("conversation:conv-1"))
    }

    @Test fun cancelIsIdempotentAndOnlyTouchesTheDeletedConversation() {
        val registry = DeviceTaskRegistry()
        val deleted = registry.begin("task-1", "conversation:conv-1", "本机对话")
        val other = registry.begin("task-2", "conversation:conv-2", "本机对话")

        assertEquals(1, registry.cancel("conversation:conv-1", reason).size)
        assertTrue("重复取消不能再次改动任务", registry.cancel("conversation:conv-1", reason).isEmpty())
        assertTrue("重复取消不能重复触发关闭回调", registry.cancel("conversation:conv-1", reason).isEmpty())

        assertEquals(DeviceTaskRegistry.State.CANCELED, deleted.state)
        assertEquals(DeviceTaskRegistry.State.RUNNING, other.state)
        assertNull(registry.cancelledReasonFor("conversation:conv-2"))
    }

    @Test fun taskQueuedBeforeDeleteStartsAlreadyCanceledAndNeverRunsTheDeviceAction() {
        val registry = DeviceTaskRegistry()
        // 任务还在等协调器锁：删除会话时它尚未开始，取消只能登记在后来的任务上。
        assertEquals(0, registry.cancel("conversation:conv-1", reason, blockFutureRuns = true).size)

        val entry = registry.begin("task-1", "conversation:conv-1", "本机对话")

        assertEquals(DeviceTaskRegistry.State.CANCELED, entry.state)
        assertEquals(reason, entry.result)
        assertEquals(reason, registry.cancelledReasonFor("conversation:conv-1"))
        registry.finish(entry.id)
        assertNull(registry.blockedReason())
    }

    @Test fun otherConversationQueuedTaskIsNotBlockedByADelete() {
        val registry = DeviceTaskRegistry()
        registry.cancel("conversation:conv-1", reason, blockFutureRuns = true)

        val entry = registry.begin("task-2", "conversation:conv-2", "本机对话")

        assertEquals(DeviceTaskRegistry.State.RUNNING, entry.state)
    }

    @Test fun automationRuleIsPausedOnlyWhenItDependsOnTheDeletedConversation() {
        val rule = AutomationRule(
            id = "rule-1", kind = AutomationKind.BILIBILI_REPLY, title = "哔哩哔哩有人回复时自动回复",
            sourcePrompt = "当哔哩哔哩上有人回复我就立刻回复", enabled = true, sourceConversationId = "conv-1"
        )

        assertTrue(AutomationManager.dependsOnConversation(rule, "conv-1"))
        assertFalse("其它会话被删除不能暂停该规则", AutomationManager.dependsOnConversation(rule, "conv-2"))
        assertFalse("没有来源会话的规则不受删除影响", AutomationManager.dependsOnConversation(rule.copy(sourceConversationId = ""), "conv-1"))
        assertFalse(AutomationManager.dependsOnConversation(rule, ""))
    }

    @Test fun ruleJsonKeepsSourceConversationSoPauseSurvivesRestart() {
        val rule = AutomationRule(
            id = "rule-1", kind = AutomationKind.PIXIV_NEW_WORK, title = "Pixiv 画师发布新作品时自动下载",
            sourcePrompt = "当 Pixiv 画师 ID 38200001 发布新作品时就下载", artist = "38200001",
            enabled = true, sourceConversationId = "conv-9"
        )

        val restored = AutomationRule.fromJson(rule.toJson())

        assertEquals("conv-9", restored.sourceConversationId)
        assertEquals(AutomationManager.PAUSED_BY_CONVERSATION_DELETE, "已暂停：来源会话已删除")
    }
}
