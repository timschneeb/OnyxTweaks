package me.timschneeberger.onyxtweaks.mods.utils

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import com.github.kyuubiran.ezxhelper.EzXHelper.hostPackageName
import com.github.kyuubiran.ezxhelper.EzXHelper.isHostPackageNameInited
import me.timschneeberger.onyxtweaks.bridge.ModEventReceiver.Companion.sendEvent
import me.timschneeberger.onyxtweaks.bridge.ModEvents
import me.timschneeberger.onyxtweaks.bridge.ModEvents.Companion.ARG_EXCEPTION
import me.timschneeberger.onyxtweaks.bridge.ModEvents.Companion.ARG_HOOKED_CLASS
import me.timschneeberger.onyxtweaks.bridge.ModEvents.Companion.ARG_HOOKED_METHOD
import me.timschneeberger.onyxtweaks.bridge.ModEvents.Companion.ARG_HOOKED_PACKAGE
import me.timschneeberger.onyxtweaks.bridge.ModEvents.Companion.ARG_MESSAGE
import java.lang.reflect.Member
import kotlin.reflect.KClass

fun Context.dpToPx(dp: Number): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()
}

inline fun <reified TCaller> Context.sendHookExceptionEvent(
    e: Throwable,
    message: String?,
    method: Member? = null,
    isWarning: Boolean = false
) = sendHookExceptionEvent(e, message, method, isWarning, TCaller::class)

fun Context.sendHookExceptionEvent(
    e: Throwable,
    message: String?,
    method: Member?,
    isWarning: Boolean = false,
    callerClass: KClass<*>
) =
    sendEvent(
        if(isWarning) ModEvents.HOOK_WARNING else ModEvents.HOOK_EXCEPTION,
        callerClass,
        Bundle().apply {
            putSerializable(ARG_EXCEPTION, e)
            putString(ARG_MESSAGE, message)
            putString(ARG_HOOKED_CLASS, method?.declaringClass?.name)
            putString(ARG_HOOKED_METHOD, method?.name)
            putString(ARG_HOOKED_PACKAGE, if (isHostPackageNameInited) hostPackageName else null)
        }
    )