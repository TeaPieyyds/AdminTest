package com.demo.admintest

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 测试用设备管理员接收器。
 *
 * ⚠ 安全声明：本类只做日志记录，不执行任何锁屏/擦除/密码策略等破坏性动作。
 * 它的唯一目的，是让系统把本应用登记为「已激活的设备管理员」，
 * 以便测试 adb / Shizuku 能否移除它。
 */
class TestDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        const val TAG = "AdminTest"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        Log.i(TAG, "设备管理员已启用（测试用）")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.i(TAG, "设备管理员已被停用（测试用）")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // 系统在用户尝试取消激活时会弹这段文字
        return "AdminTest 是一个安全测试应用，取消激活后即可正常卸载。"
    }
}