plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.demo.admintest"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.demo.admintest"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            // 让 debug 也能打出可安装包
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // 刻意不引入任何 androidx 依赖：本项目只用 Android 原生 API，
    // 少一个 aar 就少一次 AAPT2 资源编译，能在 proot 环境下绕开 daemon 问题。
}