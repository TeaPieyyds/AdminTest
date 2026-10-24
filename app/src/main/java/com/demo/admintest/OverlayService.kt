package com.demo.admintest

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.UserHandle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 悬浮窗（system alert window）——隐藏核心。
 *
 * 关键：TYPE_APPLICATION_OVERLAY + FLAG_NOT_FOCUSABLE
 *   → 窗口「可见但不抢焦点」，因此不会出现在游龙的 dumpsys 前台检测里
 *     （它读的是 mCurrentFocus / mFocusedApp，即当前获焦应用）。
 *
 * 承载「自毁」逃生按钮（最高优先级）：
 *   点一下 → 先解除锁机 → 再 killProcess 关自己（不卸载、不动系统）。
 */
class OverlayService : Service() {

    private var wm: WindowManager? = null
    private var view: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as? WindowManager
    }

    /** 显示悬浮窗（调用前需已有 SYSTEM_ALERT_WINDOW 权限） */
    fun show() {
        if (view != null) return
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        root.addView(TextView(this).apply {
            text = "AdminTest 运行中"
            textSize = 14f
        })

        val selfDestruct = Button(this).apply {
            text = "☠ 自毁：解锁并结束本进程"
            setBackgroundColor(0xFFCC0000.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { selfDestruct() }
        }
        root.addView(selfDestruct)

        val lp = buildParams()
        view = root
        wm?.addView(root, lp)
    }

    private fun buildParams() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // 关键三件套：不可获焦 + 不可点触摸外 + 布局不限位
                (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS),
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 60
            }
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS),
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 60
            }
        }

    /** 逃生：先解锁机，再杀自身进程（不卸载、不动系统） */
    fun selfDestruct() {
        // 先解锁机（用 shell 或 dpm 清 keyguard；设备管理员可强制清锁）
        runCatching { unlockKeyguard() }
        removeOverlay()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun unlockKeyguard() {
        val ctx = applicationContext
        val dpm = ctx.getSystemService(android.app.admin.DevicePolicyManager::class.java)
        val admin = ComponentName(ctx, TestDeviceAdminReceiver::class.java)
        // 若已激活管理员，可强制清除 keyguard（让手机不再锁着）
        if (dpm.isAdminActive(admin)) {
            runCatching {
                val user = UserHandle.of(0)
                // 不同 ROM 行为不一，失败可忽略（进程马上要死了）
                dpm.clearKeyguardSecurePin(user)
            }
        }
    }

    private fun removeOverlay() {
        view?.let { runCatching { wm?.removeView(it) } }
        view = null
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }
}
