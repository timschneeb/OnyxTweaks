package me.timschneeberger.onyxtweaks.utils

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

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