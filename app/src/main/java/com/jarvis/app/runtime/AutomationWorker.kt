package com.jarvis.app.runtime

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class AutomationWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.failure()
        val manager = AutomationManager(applicationContext)
        if (!manager.claim(eventId)) return Result.success()
        val ruleId = inputData.getString(KEY_RULE_ID) ?: return Result.failure()
        val rule = manager.findRule(ruleId) ?: return Result.success()

        val event = AutomationEvent(
            eventId = eventId,
            rule = rule,
            packageName = inputData.getString(KEY_PACKAGE).orEmpty(),
            title = inputData.getString(KEY_TITLE).orEmpty(),
            text = inputData.getString(KEY_TEXT).orEmpty(),
            notificationKey = inputData.getString(KEY_NOTIFICATION_KEY).orEmpty(),
            notificationOpened = inputData.getBoolean(KEY_OPENED, false)
        )
        if (!rule.enabled) {
            manager.record(event, "已取消：自动化在后台任务开始前被暂停")
            return Result.success()
        }
        val owner = ownerKey(rule.id)
        val outcome = runCatching {
            DeviceTaskCoordinator.run("自动化：${rule.title}", owner) { AutomationAgent(applicationContext).run(event) }
        }
            .getOrElse { "未完成：${it.message ?: it.javaClass.simpleName}" }
        // 会话被删除时这条后台任务会先被取消：结果如实写成取消，不留悬空运行态。
        manager.record(event, DeviceTaskCoordinator.cancelledReasonFor(owner) ?: outcome)
        return Result.success()
    }

    companion object {
        private const val KEY_EVENT_ID = "eventId"
        private const val KEY_RULE_ID = "ruleId"
        private const val KEY_PACKAGE = "packageName"
        private const val KEY_TITLE = "title"
        private const val KEY_TEXT = "text"
        private const val KEY_NOTIFICATION_KEY = "notificationKey"
        private const val KEY_OPENED = "notificationOpened"
        private const val WORK_NAME = "jarvis-automation-events"

        /** 任务归属键：删除会话时按规则取消已排队或在跑的后台任务。 */
        fun ownerKey(ruleId: String): String = "automation:$ruleId"

        private fun tag(ruleId: String): String = "jarvis-automation-$ruleId"

        /** 撤掉该规则已排队但还没开始的后台任务（删除会话、暂停规则时调用）。 */
        fun cancel(context: Context, ruleId: String) {
            WorkManager.getInstance(context.applicationContext).cancelAllWorkByTag(tag(ruleId))
        }

        fun enqueue(context: Context, event: AutomationEvent) {
            val data: Data = workDataOf(
                KEY_EVENT_ID to event.eventId,
                KEY_RULE_ID to event.rule.id,
                KEY_PACKAGE to event.packageName,
                KEY_TITLE to event.title,
                KEY_TEXT to event.text,
                KEY_NOTIFICATION_KEY to event.notificationKey,
                KEY_OPENED to event.notificationOpened
            )
            val request = OneTimeWorkRequestBuilder<AutomationWorker>()
                .setInputData(data)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(tag(event.rule.id))
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
