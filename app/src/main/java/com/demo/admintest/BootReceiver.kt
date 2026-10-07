package com.demo.admintest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 测试用开机自启接收器。
 *
 * ⚠ 仅用于验证「开机自启是否会被 pm suspend 阻止」，不做任何隐藏动作。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            context.startForegroundService(Intent(context, KeepAliveService::class.java))
        }
    }
}