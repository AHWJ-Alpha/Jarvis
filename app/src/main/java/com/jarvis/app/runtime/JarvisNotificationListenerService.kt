package com.jarvis.app.runtime

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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
    override fun onListenerConnected() {
        NotificationController.active = true
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val extras = sbn.notification.extras
        NotificationController.add(JSONObject().apply {
            put("package", sbn.packageName)
            put("title", extras.getCharSequence("android.title")?.toString().orEmpty())
            put("text", extras.getCharSequence("android.text")?.toString().orEmpty())
            put("timestamp", sbn.postTime)
        })
    }

    override fun onListenerDisconnected() {
        NotificationController.active = false
    }
}
