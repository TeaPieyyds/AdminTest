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
 * AdminTest —— 病毒靶子（验证「文官(检测) + 武将(游龙处置)」能否清除它）。
 *
 * 流程：授权 → 点「开始测试」→ 自动 grant 权限 + 废游龙 + 拉悬浮窗 + 锁机。
 * 逃生：悬浮层里那颗红色「自毁」按钮 → 先解锁 → 再 killProcess。
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
        } catch (t: Throwable) {
            // Shizuku 未安装时忽略
        }

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

        root.addView(Button(this).apply {
            text = "▶ 开始测试（锁机 + 隐藏 + 废游龙）"
            textSize = 16f
            setBackgroundColor(0xFF2E7D32.toInt())
            setTextColor(Color.WHITE)
            setOnClickListener { startTest() }
        })

        root.addView(TextView(this).apply {
            text = "── 授权 ──"
            textSize = 14f
            setPadding(0, 16, 0, 4)
        })

        root.addView(Button(this).apply {
            text = "① 授权 Shizuku（关键，给全套权限）"
            setOnClickListener {
                ShizukuShell.requestPermission()
                refreshStatus()
            }
        })

        root.addView(Button(this).apply {
            text = "② 激活设备管理员（锁机靠它）"
            setOnClickListener { activateAdmin() }
        })

        root.addView(Button(this).apply {
            text = "③ 授权悬浮窗（隐藏双保险之一）"
            setOnClickListener { jumpToOverlaySettings() }
        })

        root.addView(Button(this).apply {
            text = "④ 授权无障碍（隐藏双保险之二）"
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
            text = "\n逃生方式：启动后屏幕角落会出现一颗红色「自毁」悬浮按钮，点它 = 解锁并关闭自己。"
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
        } catch (t: Throwable) {
            // 忽略
        }
        super.onDestroy()
    }

    private fun startTest() {
        if (!ShizukuShell.hasPermission()) {
            Toast.makeText(this, "请先点①授权 Shizuku", Toast.LENGTH_LONG).show()
            ShizukuShell.requestPermission()
            return
        }
        if (!dpm.isAdminActive(admin)) {
            Toast.makeText(this, "请先点②激活设备管理员", Toast.LENGTH_LONG).show()
            activateAdmin()
            return
        }
        if (!canDrawOverlays()) {
            Toast.makeText(this, "请先点③授权悬浮窗", Toast.LENGTH_LONG).show()
            jumpToOverlaySettings()
            return
        }

        status.text = "正在执行测试序列…"
        io.execute {
            Log.i("AdminTest", "startTest 开始")
            ShizukuShell.grantAll()
            ShizukuShell.disableYoulong()
            Log.i("AdminTest", "已尝试 disable com.youlong.tool / .hd")

            runOnUiThread {
                startForegroundService(Intent(this, OverlayService::class.java))
                Toast.makeText(this, "已启动，请留意屏幕角落的红色自毁按钮", Toast.LENGTH_LONG).show()
            }

            startAccessibilityIfGranted()

            runCatching {
                dpm.lockNow()
                Log.i("AdminTest", "lockNow 已执行")
            }.onFailure { t ->
                Log.w("AdminTest", "lockNow 失败: $t")
            }

            runOnUiThread { status.text = "测试序列执行完成（已锁机，自毁按钮在悬浮层）" }
        }
    }

    private fun startAccessibilityIfGranted() {
        try {
            val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (enabled != null && enabled.contains(packageName + "/")) {
                Toast.makeText(this, "无障碍已启用", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "无障碍未启用（请点④后手动开启）", Toast.LENGTH_LONG).show()
            }
        } catch (t: Throwable) {
            // 忽略
        }
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
                // 个别 ROM 打不开指定包页，退回应用管理页
                Toast.makeText(this, "请在设置里手动开启悬浮窗权限", Toast.LENGTH_LONG).show()
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

    private fun refreshStatus() {
        val shizukuOk = ShizukuShell.hasPermission()
        val adminOk = runCatching { dpm.isAdminActive(admin) }.getOrDefault(false)
        val overlayOk = canDrawOverlays()
        status.text = buildString {
            append("Shizuku：").append(if (shizukuOk) "已授权 ✅" else "未授权 ❌").append('\n')
            append("设备管理员：").append(if (adminOk) "已激活 ✅" else "未激活 ❌").append('\n')
            append("悬浮窗：").append(if (overlayOk) "可授权 ✅" else "未授权 ❌").append('\n')
            append("测试序列前置条件：").append(if (shizukuOk && adminOk && overlayOk) "就绪" else "未就绪")
        }
    }
}
