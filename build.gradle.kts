plugins {
    id("plugins.token.parse")
}

buildscript {
    val kotlin_version = "1.5.20"
    repositories {
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.3.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}







tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
    print("WTF")
}









