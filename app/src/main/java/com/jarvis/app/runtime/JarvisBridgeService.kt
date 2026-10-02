package com.jarvis.app.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.util.Log

class JarvisBridgeService : Service() {
    private var bridge: JarvisBridgeServer? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Jarvis 本地桥接", NotificationManager.IMPORTANCE_LOW))
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Jarvis 本地能力桥接")
            .setContentText("OpenClaw 可以通过本机安全接口请求已授权能力")
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        bridge = JarvisBridgeServer(this).also {
            if (!it.start()) Log.e("JarvisBridge", "Failed to bind 127.0.0.1:18791")
            else Log.i("JarvisBridge", "Listening on 127.0.0.1:18791")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        bridge?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "jarvis_bridge"
        private const val NOTIFICATION_ID = 18791
    }
}
