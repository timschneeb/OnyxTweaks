plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

val releaseStoreFile: String? by rootProject
val releaseStorePassword: String? by rootProject
val releaseKeyAlias: String? by rootProject
val releaseKeyPassword: String? by rootProject

android {
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "me.timschneeberger.onyxtweaks"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "COMMIT_COUNT", "\"${getCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getGitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime()}\"")
    }

    project.setProperty("archivesBaseName", "OnyxTweaks-v${defaultConfig.versionName}-${defaultConfig.versionCode}")

    namespace = "me.timschneeberger.onyxtweaks"

    signingConfigs {
        create("config") {
            releaseStoreFile?.also {
                storeFile = rootProject.file(it)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    packaging {
        jniLibs {
            excludes += "META-INF/**"
        }
        resources {
            excludes += "META-INF/**"
        }
    }

    androidResources {
        additionalParameters += arrayOf("--allow-reserved-package-id", "--package-id", "0x45")
    }

    buildTypes {
        all {
            signingConfig =
                if (releaseStoreFile.isNullOrEmpty()) signingConfigs.getByName("debug") else signingConfigs.getByName(
                    "config"
                )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        disable += "NonConstantResourceId"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.annotation:annotation-jvm:1.9.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.10")

    implementation("com.google.android.material:material:1.12.0")
    implementation("com.crossbowffs.remotepreferences:remotepreferences:0.8")
    implementation("com.github.kyuubiran:EzXHelper:2.2.1")
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")

    compileOnly("de.robv.android.xposed:api:82")
}