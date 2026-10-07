package com.demo.admintest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

/**
 * 测试用前台服务。
 *
 * ⚠ 只做「保活展示」：通知栏会明确显示本应用名，不做任何隐藏。
 * 目的是模拟真实病毒的前台服务保活行为，用来验证：
 *   - pm suspend 后服务能否被系统停掉
 *   - 强杀进程后服务会不会被拉起
 */
class KeepAliveService : Service() {

    companion object {
        const val CHANNEL_ID = "admintest_alive"
        const val NOTIF_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notif = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("AdminTest 前台服务运行中")
            .setContentText("这是安全测试用的保活服务，可随时关闭。")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notif)
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "AdminTest 保活",
                NotificationManager.IMPORTANCE_LOW // 明确低优先级，但通知可见
            )
            mgr.createNotificationChannel(ch)
        }
    }
}