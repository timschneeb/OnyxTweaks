package me.timschneeberger.onyxtweaks.mods.utils

import android.content.Context
import android.util.TypedValue


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