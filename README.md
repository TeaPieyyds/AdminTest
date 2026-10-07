# AdminTest

> ⚠️ **安全测试专用工具** —— 用于验证「应用被激活为设备管理员后，adb / Shizuku 能否成功卸载它」。

## 这是什么

一个**无害的** Android 测试 APK（包名 `com.demo.admintest`），用来复现"设备管理员防卸载"这一机制的**行为**，从而实测各种 adb 手段的效果。

## 它有什么

| 组件 | 作用 | 对应真实病毒 |
|---|---|---|
| `TestDeviceAdminReceiver` | 注册设备管理员 | 病毒的防卸载机制 |
| `KeepAliveService` | 前台服务保活（通知栏**明文可见**） | 病毒的保活机制 |
| `BootReceiver` | 开机自启 | 病毒的自启动机制 |

## 它**没有**什么

- ❌ 没有任何窃密代码
- ❌ 没有任何远控/C2 通信
- ❌ 没有任何联网行为
- ❌ 没有隐藏通知栏（通知明确写着"安全测试")

**它就是个空壳测试机**，全部代码都可读（就 4 个 Kotlin 文件）。

## 怎么用

1. 安装 APK，点 **① 激活设备管理员**
2. 依次执行下面的命令，观察结果：

```bash
# ① 普通卸载 —— 预期被拦截
adb uninstall com.demo.admintest
#    → 预期：DELETE_FAILED_DEVICE_POLICY_MANAGER

# ② 尝试用 dpm 移除 —— 预期失败
adb shell dpm remove-active-admin com.demo.admintest/com.demo.admintest.TestDeviceAdminReceiver
#    → 预期：Attempt to remove non-test admin

# ③ 冻结 —— 预期成功
adb shell pm suspend --user 0 com.demo.admintest
#    → 预期：进程被冻结，前台服务被杀，开机不再自启

# ④ 解冻
adb shell pm unsuspend --user 0 com.demo.admintest
```

## 构建

推到 GitHub 后，Actions 会自动编译并发布 APK 到 Releases。

也可以本地：
```bash
gradle assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 为什么不用 AndroidX

刻意不引入任何 androidx 依赖 —— 只用系统原生 API。这样：
- 包体最小
- 少一层 AAPT2 资源编译，在 proot/Termux 等受限环境里更容易编译通过

## License

仅用于安全研究与自有设备测试。请勿用于未授权设备。
