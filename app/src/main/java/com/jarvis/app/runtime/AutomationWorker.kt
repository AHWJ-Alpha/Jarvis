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
        val result = runCatching {
            DeviceTaskCoordinator.run("自动化：${rule.title}") { AutomationAgent(applicationContext).run(event) }
        }
            .getOrElse { "未完成：${it.message ?: it.javaClass.simpleName}" }
        manager.record(event, result)
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
                .addTag("jarvis-automation-${event.rule.id}")
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
