package me.timschneeberger.onyxtweaks.utils

import android.content.Context
import android.util.TypedValue
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.toast


fun Context.dpToPx(dp: Int): Int {
    return dpToPx(dp.toFloat())
}

fun Context.dpToPx(dp: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        resources.displayMetrics
    ).toInt()
}

fun Context.restartLauncher() {
    toast(R.string.toast_launcher_restarting)
    restartPackageSilently(LAUNCHER_PACKAGE)
}

fun Context.restartSystemUi() {
    toast(R.string.toast_system_ui_restarting)
    restartPackageSilently(SYSTEM_UI_PACKAGE)
}