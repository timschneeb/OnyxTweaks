package me.timschneeberger.onyxtweaks.utils

import android.content.Context
import android.util.TypedValue
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.typeOf


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

@OptIn(ExperimentalContracts::class)
inline fun <reified T> Any?.cast(): T? {
    contract {
        returns() implies (this@cast is T?)
    }

    return when {
        this is T -> this
        this == null -> null
        else -> throw ClassCastException("Cannot cast $this to ${typeOf<T>()}")
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <reified T> Any?.castNonNull(): T {
    contract {
        returns() implies (this@castNonNull is T)
    }

    return cast<T>()!!
}