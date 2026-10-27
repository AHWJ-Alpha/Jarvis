package com.jarvis.app.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class JarvisBridgeService : Service() {
    private var bridge: JarvisBridgeServer? = null
    private var remoteServer: RemoteAccessServer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        synchronizeRemoteAccess()
        scope.launch {
            val result = OpenClawProcessManager.get(this@JarvisBridgeService).start()
            Log.i("JarvisGateway", "started=${result.started} ${result.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SYNC_REMOTE) synchronizeRemoteAccess()
        return START_STICKY
    }

    private fun synchronizeRemoteAccess() {
        if (RemoteAccessManager(this).enabled) {
            if (remoteServer?.isRunning() == true) return
            remoteServer?.stop()
            remoteServer = RemoteAccessServer(this, scope).also {
                if (!it.start()) Log.e("JarvisRemote", "Failed to bind remote access port ${RemoteAccessServer.PORT}")
                else Log.i("JarvisRemote", "Remote console listening on port ${RemoteAccessServer.PORT}")
            }
        } else {
            remoteServer?.stop()
            remoteServer = null
        }
    }

    override fun onDestroy() {
        remoteServer?.stop()
        remoteServer = null
        bridge?.stop()
        runBlocking { OpenClawProcessManager.get(this@JarvisBridgeService).stop() }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_SYNC_REMOTE = "com.jarvis.app.action.SYNC_REMOTE_ACCESS"
        private const val CHANNEL = "jarvis_bridge"
        private const val NOTIFICATION_ID = 18791
    }
}
