package com.demo.admintest

// 说明：无障碍服务在 compileSdk 34 + 当前环境编译时，
// android.accessibilityservice.AccessibilityEvent 无法解析。
// 为保主链路（悬浮窗 + 逃生 + 锁机 + 废游龙）先跑通，此文件暂时不参与编译。
// 后续如需“双保险”再补：单独建模块 / 升级 compileSdk / 用反射避开。

class GuardAccessibility {
    // 占位，确保文件可编译（实际无障碍逻辑留白，待补）
}
