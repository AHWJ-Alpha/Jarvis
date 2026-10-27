package com.jarvis.app.runtime

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedDeque

object NotificationController {
    private val recent = ConcurrentLinkedDeque<JSONObject>()
    @Volatile var active = false
    fun list(): JSONArray = JSONArray(recent.toList())
    fun add(item: JSONObject) {
        recent.addFirst(item)
        while (recent.size > 100) recent.pollLast()
    }
}

class JarvisNotificationListenerService : NotificationListenerService() {
    private val automationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val automationMutex = Mutex()

    override fun onListenerConnected() {
        NotificationController.active = true
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        NotificationController.add(JSONObject().apply {
            put("package", sbn.packageName)
            put("title", title)
            put("text", text)
            put("timestamp", sbn.postTime)
        })

        val events = AutomationManager(this).acceptNotification(
            sbn.packageName, title, text, sbn.key
        )
        val openNotification = sbn.notification.contentIntent
        if (events.isEmpty()) return
        val notificationOpened = if (openNotification == null) false else runCatching { openNotification.send(); true }.getOrDefault(false)
        events.forEach { event ->
            automationScope.launch {
                automationMutex.withLock {
                    val manager = AutomationManager(this@JarvisNotificationListenerService)
                    if (!notificationOpened) {
                        manager.record(event, "未执行：目标通知没有可打开的页面入口")
                    } else {
                        delay(1400)
                        runCatching {
                            val openedEvent = event.copy(notificationOpened = true)
                            manager.markPending(openedEvent)
                            AutomationWorker.enqueue(this@JarvisNotificationListenerService, openedEvent)
                        }.onFailure {
                            manager.record(event, "未执行：无法排入后台任务：${it.message}")
                        }
                    }
                }
            }
        }
    }

    override fun onListenerDisconnected() {
        NotificationController.active = false
    }

    override fun onDestroy() {
        automationScope.cancel()
        super.onDestroy()
    }
}
