buildscript {
    val kotlinVersion by extra("2.1.10")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.8.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://api.xposed.info/")
        maven(url = "https://jitpack.io")
        maven(url = "https://repo.boox.com/repository/maven-public/")
    }
}

