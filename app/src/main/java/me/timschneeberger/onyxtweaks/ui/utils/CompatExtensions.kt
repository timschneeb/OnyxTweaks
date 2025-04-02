package me.timschneeberger.onyxtweaks.ui.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import me.timschneeberger.onyxtweaks.utils.sdkAbove
import java.io.Serializable

@Suppress("DEPRECATION")
object CompatExtensions {
    inline fun <reified T : Serializable> Bundle.getSerializableAs(key: String): T? {
        return sdkAbove(Build.VERSION_CODES.TIRAMISU) {
            this.getSerializable(key, T::class.java)
        }.below {
            this.getSerializable(key) as? T
        }
    }

    fun PackageManager.getApplicationInfoCompat(packageName: String, flags: Int = 0): ApplicationInfo {
        return sdkAbove(Build.VERSION_CODES.TIRAMISU) {
            getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        }.below {
            getApplicationInfo(packageName, flags)
        }
    }

    fun PackageManager.getInstalledApplicationsCompat(flags: Int = 0): List<ApplicationInfo> {
        return sdkAbove(Build.VERSION_CODES.TIRAMISU) {
            getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        }.below {
            getInstalledApplications(flags)
        }
    }
}