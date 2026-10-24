package com.demo.admintest

import android.accessibilityservice.AccessibilityEvent
import android.accessibilityservice.AccessibilityService

/**
 * 无障碍服务（隐藏双保险之二）。
 *
 * 悬浮窗被砍掉时，无障碍仍后台常驻，形成「双保险」。
 * 本类不做任何破坏性动作，只作为一个「后台常驻、系统级」的存在，
 * 增加对方一键清理的成本。
 */
class GuardAccessibility : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不做事，仅保持服务存活
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 服务已连接，保持存活即可
    }

    override fun onInterrupt() {
        // 系统中断，忽略
    }
}
