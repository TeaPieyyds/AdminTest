package com.demo.admintest

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * AdminTest —— 设备管理员 / 防卸载 安全测试工具。
 *
 * ⚠ 用途：验证在「应用已被激活为设备管理员」的前提下，
 *   `adb uninstall` / `pm suspend` / `dpm remove-active-admin` 各能做到什么程度。
 *
 * ⚠ 本应用不含任何窃密、远控、联网、自复活逻辑，纯粹是个测试壳。
 */
class MainActivity : Activity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ComponentName(this, TestDeviceAdminReceiver::class.java)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val title = TextView(this).apply {
            text = "AdminTest · 设备管理员测试"
            textSize = 20f
        }
        root.addView(title)

        status = TextView(this).apply {
            textSize = 14f
            setPadding(0, 32, 0, 32)
        }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "① 激活设备管理员"
            setOnClickListener { activateAdmin() }
        })

        root.addView(Button(this).apply {
            text = "② 取消激活设备管理员"
            setOnClickListener { deactivateAdmin() }
        })

        root.addView(Button(this).apply {
            text = "③ 启动前台保活服务"
            setOnClickListener {
                startForegroundService(Intent(this@MainActivity, KeepAliveService::class.java))
                refreshStatus()
            }
        })

        root.addView(Button(this).apply {
            text = "刷新状态"
            setOnClickListener { refreshStatus() }
        })

        val tip = TextView(this).apply {
            text = "\n测试步骤建议：\n" +
                "1. 点①激活设备管理员\n" +
                "2. 用 adb 执行：adb uninstall com.demo.admintest\n" +
                "   观察是否报 DELETE_FAILED_DEVICE_POLICY_MANAGER\n" +
                "3. 用 adb 执行：adb shell dpm remove-active-admin com.demo.admintest/com.demo.admintest.TestDeviceAdminReceiver\n" +
                "   观察是否报 non-test admin\n" +
                "4. 用 adb 执行：adb shell pm suspend --user 0 com.demo.admintest\n" +
                "   观察是否被冻结、能否防前台服务重启"
            textSize = 13f
        }
        root.addView(tip)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun activateAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "AdminTest 是安全测试应用，仅用于验证卸载防护机制。"
            )
        }
        startActivity(intent)
    }

    private fun deactivateAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
        }
        // 直接跳系统设置里的"取消激活"入口
        startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
    }

    private fun refreshStatus() {
        val active = dpm.isAdminActive(admin)
        val suspended = false // suspend 状态需 adb 查，这里仅备注
        status.text = buildString {
            append("设备管理员状态：").append(if (active) "已激活 ✅" else "未激活 ❌").append("\n")
            append("提示：激活后系统设置里的「卸载」会被拦截。")
            if (suspended) append("\n(已冻结)")
        }
    }
}