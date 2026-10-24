package com.demo.admintest

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Shizuku shell 封装（照搬 device-check 的实现，包名换为本项目）。
 *
 * 用于「开始测试」时的特权操作：
 *   - pm grant 全套权限
 *   - pm disable-user 废掉游龙（com.youlong.tool / com.youlong.hd）
 */
object ShizukuShell {

    const val PERMISSION_REQUEST_CODE = 2001

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (t: Throwable) {
        false
    }

    fun hasPermission(): Boolean = try {
        isAvailable() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (t: Throwable) {
        false
    }

    fun requestPermission() {
        try {
            if (isAvailable() && !hasPermission()) {
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            }
        } catch (t: Throwable) {
            // 交由监听器处理
        }
    }

    /** 执行 shell 命令，返回输出。必须在工作线程调用。 */
    fun exec(command: String): String {
        val process = newProcess(arrayOf("sh", "-c", command))
            ?: return "ERROR: shizuku process is null"
        val out = readAll(process.inputStream)
        val err = readAll(process.errorStream)
        process.waitFor()
        return if (out.isNotEmpty()) out else err
    }

    /** 一键：授权 Shizuku 后，自动补齐全部运行所需权限 */
    fun grantAll() {
        exec("pm grant com.demo.admintest android.permission.POST_NOTIFICATIONS 2>/dev/null || true")
    }

    /** 核心测试动作：废掉游龙两个包 */
    fun disableYoulong() {
        exec("pm disable-user --user 0 com.youlong.tool 2>/dev/null || true")
        exec("pm disable-user --user 0 com.youlong.hd 2>/dev/null || true")
    }

    private fun newProcess(cmd: Array<String>): Process? {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            method.invoke(null, cmd, null, null) as? Process
        } catch (t: Throwable) {
            null
        }
    }

    private fun readAll(stream: java.io.InputStream): String {
        val sb = StringBuilder()
        BufferedReader(InputStreamReader(stream)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
                sb.append(line).append('\n')
                line = reader.readLine()
            }
        }
        return sb.toString()
    }
}
