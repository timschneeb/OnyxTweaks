buildscript {
    val kotlinVersion by extra("2.1.10")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.8.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
    }
}

apply(plugin = "io.gitlab.arturbosch.detekt")

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://api.xposed.info/")
        maven(url = "https://jitpack.io")
    }
}

