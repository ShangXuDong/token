dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

pluginManagement {
    repositories {
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "buildSrc"

