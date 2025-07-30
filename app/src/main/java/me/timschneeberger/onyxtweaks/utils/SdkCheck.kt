package me.timschneeberger.onyxtweaks.utils

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import me.timschneeberger.onyxtweaks.utils.Version.Companion.toVersion

val onyxVersion by lazy {
    /*
     * Example build ids:
     *      2025-07-26_22-49_4.1-beta_0726_6aa3fa239
     *      2025-04-27_23-34_4.0.2_135ce530f
     *      2025-06-19_00-19_4.0.2-rel_0614
     *      2025-03-26_18-59_v4.0-rel_2aa96eb60
     */
    Build.ID
        .split('_')[2]
        .split('-').first()
        .replace("v", "")
        .toVersion()
}


class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
    override operator fun compareTo(other: Version): Int {
        return when {
            major != other.major -> major - other.major
            minor != other.minor -> minor - other.minor
            else -> patch - other.patch
        }
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        fun String.toVersion(): Version {
            val parts = split('.')
            return Version(
                major = parts.getOrNull(0)?.toIntOrNull() ?: 0,
                minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
                patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            )
        }
    }

}

class SdkCheckElseBranch<T>(private val result: T?) {
    fun valueOrNull(): T? = result
    fun below(onFailure: () -> T): T = result ?: onFailure()
}

@ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
inline fun <T> sdkAbove(sdk: Int, onSuccessful: () -> T): SdkCheckElseBranch<T> {
    (Build.VERSION.SDK_INT >= sdk).let {
        return SdkCheckElseBranch<T>(if(it) onSuccessful() else null)
    }
}

object SdkCheck {
    private val sdk = Build.VERSION.SDK_INT

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    val is9: Boolean get() = sdk >= Build.VERSION_CODES.P
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    val is10: Boolean get() = sdk >= Build.VERSION_CODES.Q
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    val is11: Boolean get() = sdk >= Build.VERSION_CODES.R
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val is12: Boolean get() = sdk >= Build.VERSION_CODES.TIRAMISU
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S_V2)
    val is12L: Boolean get() = sdk >= Build.VERSION_CODES.S_V2
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    val is13: Boolean get() = sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    val is14: Boolean get() = sdk >= Build.VERSION_CODES.VANILLA_ICE_CREAM
}