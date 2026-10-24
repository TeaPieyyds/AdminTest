package com.demo.admintest

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors

/**
 * AdminTest —— 病毒靶子。
 * 流程：授权 → 点「开始测试」→ 先锁机 + 弹悬浮窗，最后才砍游龙（躲游龙反咬）。
 * 逃生：悬浮层里那颗红色「自毁」按钮 → killProcess 关自己。
 */
class MainActivity : Activity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private lateinit var status: TextView
    private val io = Executors.newSingleThreadExecutor()

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { code, result ->
            if (code == ShizukuShell.PERMISSION_REQUEST_CODE) {
                runOnUiThread { refreshStatus() }
            }
        }

    private val binderListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread { refreshStatus() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ComponentName(this, TestDeviceAdminReceiver::class.java)
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
            Shizuku.addBinderReceivedListener(binderListener)
        } catch (t: Throwable) { /* Shizuku 未装时忽略 */ }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        root.addView(TextView(this).apply {
            text = "AdminTest · 病毒靶子"
            textSize = 20f
            setPadding(0, 0, 0, 8)
        })
        status = TextView(this).apply {
            textSize = 13f
            setPadding(0, 8, 0, 16)
        }
        root.addView(status)

        // 核心测试序列：Shizuku 授权后自动跑
        root.addView(Button(this).apply {
            text = "▶ 开始测试（锁机 + 悬浮窗 + 砍游龙）"
            textSize = 16f
            setBackgroundColor(0xFF2E7D32.toInt())
            setTextColor(Color.WHITE)
            setOnClickListener { startTest() }
        })

        // 授权区（手动授权 Shizuku 后自动拿全套）
        root.addView(TextView(this).apply {
            text = "── 授权 ──"
            textSize = 14f
            setPadding(0, 16, 0, 4)
        })

        // ① Shizuku 授权（核心，提供 shell 权限 + 自动拿所有权限）
        root.addView(Button(this).apply {
            text = "① 授权 Shizuku"
            setOnClickListener {
                ShizukuShell.requestPermission()
                refreshStatus()
            }
        })

        // ② 设备管理员（lockNow 锁机需要）
        root.addView(Button(this).apply {
            text = "② 激活设备管理员（锁机用）"
            setOnClickListener { activateAdmin() }
        })

        // ③ 悬浮窗权限（隐藏核心 + 承载自毁按钮）
        root.addView(Button(this).apply {
            text = "③ 授权悬浮窗（隐藏核心 + 自毁按钮）"
            setOnClickListener { jumpToOverlaySettings() }
        })

        // ④ 无障碍（双保险之二，隐藏兜底）
        root.addView(Button(this).apply {
            text = "④ 授权无障碍（双保险）"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        root.addView(Button(this).apply {
            text = "刷新状态"
            setOnClickListener { refreshStatus() }
        })

        root.addView(TextView(this).apply {
            textSize = 11f
            text = "流程：先授权 Shizuku，再点「开始测试」→ 自动砍掉游龙双包 + 锁机 + 弹自毁悬浮窗。"
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    override fun onDestroy() {
        io.shutdownNow()
        try {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            Shizuku.removeBinderReceivedListener(binderListener)
        } catch (t: Throwable) { /* 忽略 */ }
        super.onDestroy()
    }

    /**
     * 测试序列：
     *   1. 先 lockNow（锁机，让机器进入"被管"状态）
     *   2. 启动 OverlayService（弹窗，含自毁按钮）
     *   3. 最后才砍游龙双包（避免先砍游龙被其反咬）
     * 顺序考量：lockNow 后机器已锁，悬浮窗已常驻，游龙砍不砍到都已被锁。
     */
    private fun startTest() {
        if (!ShizukuShell.hasPermission()) {
            Toast.makeText(this, "请先授权 Shizuku", Toast.LENGTH_LONG).show()
            ShizukuShell.requestPermission()
            return
        }
        if (!dpm.isAdminActive(admin)) {
            Toast.makeText(this, "请先激活设备管理员", Toast.LENGTH_LONG).show()
            activateAdmin()
            return
        }
        if (!canDrawOverlays()) {
            Toast.makeText(this, "请先授权悬浮窗", Toast.LENGTH_LONG).show()
            jumpToOverlaySettings()
            return
        }

        status.text = "正在执行测试序列…"
        io.execute {
            // 1. 先锁机（最高优先级，确保机器已被管）
            runCatching {
                dpm.lockNow()
                Log.i("AdminTest", "lockNow 已执行")
            }.onFailure { t -> Log.w("AdminTest", "lockNow 失败: $t") }

            // 2. 弹悬浮窗（NOT_FOCUSABLE，承载自毁按钮）
            runOnUiThread {
                startService(Intent(this, OverlayService::class.java))
                Toast.makeText(this, "测试已启动，自毁按钮在屏幕角落", Toast.LENGTH_LONG).show()
            }

            // 3. 最后才砍游龙双包（运行时拼包名，躲关键词检测）
            ShizukuShell.grantAll()
            ShizukuShell.disableYoulong()
            Log.i("AdminTest", "disable youlong 已尝试")

            runOnUiThread {
                status.text = "测试序列完成（已锁机 + 悬浮窗 + 砍游龙）"
            }
        }
    }

    private fun activateAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "AdminTest 是安全测试目标，激活后可锁机，用于验证清除能力。"
            )
        }
        startActivity(intent)
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun jumpToOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            try {
                startActivity(i)
            } catch (t: Throwable) {
                Toast.makeText(this, "请手动开启悬浮窗权限", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refreshStatus() {
        val shizukuOk = ShizukuShell.hasPermission()
        val adminOk = runCatching { dpm.isAdminActive(admin) }.getOrDefault(false)
        val overlayOk = canDrawOverlays()
        status.text = buildString {
            append("Shizuku: ").append(if (shizukuOk) "已授权" else "未授权").append('\n')
            append("管理员: ").append(if (adminOk) "已激活" else "未激活").append('\n')
            append("悬浮窗: ").append(if (overlayOk) "可授权" else "未授权").append('\n')
            append(if (shizukuOk && adminOk && overlayOk) "已就绪" else "未就绪")
        }
    }
}
