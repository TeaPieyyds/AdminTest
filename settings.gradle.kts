pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://repo.huaweicloud.com/repository/maven/")
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Shizuku 官方 maven 仓库（dev.rikka.shizuku 的 api/provider 发布在这）
        maven("https://appdownload.shizuku.nickromi.app/maven-release")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://repo.huaweicloud.com/repository/maven/")
    }
}

rootProject.name = "AdminTest"
include(":app")
