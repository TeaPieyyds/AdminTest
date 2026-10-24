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
    // 核心：Shizuku 特权内核（shell 能力来源）。坐标照搬 device-check：
    //   dev.rikka.shizuku:api / :provider  v13.1.5
    // 注意：本项目原来刻意零依赖（连 androidx 都不引），现引入 Shizuku。
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
