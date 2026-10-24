package com.demo.admintest

import android.accessibilityservice.AccessibilityService

/**
 * 无障碍服务（隐藏双保险之二）。
 *
 * 悬浮窗被砍掉时，无障碍仍后台常驻，形成「双保险」。
 * 本类不做任何破坏性动作，只作为一个「后台常驻、系统级」的存在，
 * 增加对方一键清理的成本。
 */
class GuardAccessibility : AccessibilityService() {
    override fun onAccessibilityEvent(event: android.accessibilityservice.AccessibilityEvent?) {
        // 不做事，仅保持服务存活（用全限定名，绕开 import 解析问题）
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onInterrupt() {
        // 系统中断，忽略
    }
}
