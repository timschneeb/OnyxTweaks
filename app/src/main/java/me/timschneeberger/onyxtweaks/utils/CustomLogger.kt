package me.timschneeberger.onyxtweaks.utils

import android.util.Log
import com.github.kyuubiran.ezxhelper.Logger
import de.robv.android.xposed.XposedBridge
import me.timschneeberger.onyxtweaks.BuildConfig

object CustomLogger : Logger() {
    override fun i(msg: String, thr: Throwable?) {
        if (logLevelFilter > INFO) return
        Log.i(logTag, msg.decorate(), thr)
    }

    override fun d(msg: String, thr: Throwable?) {

        Log.d(logTag, msg.decorate(), thr)
        if (logLevelFilter > INFO) return
        Log.d(logTag, msg.decorate(), thr)
    }

    override fun w(msg: String, thr: Throwable?) {
        if (logLevelFilter > WARN) return
        Log.w(logTag, msg.decorate(), thr)
    }

    override fun e(msg: String, thr: Throwable?) {
        if (logLevelFilter > ERROR) return
        Log.e(logTag, msg.decorate(), thr)
    }

    private fun String.decorate(): String {
        val excludedClassPrefixes = arrayOf(
            "${BuildConfig.APPLICATION_ID}.mods.utils",
            "${BuildConfig.APPLICATION_ID}.ui.utils",
            "${BuildConfig.APPLICATION_ID}.utils.CustomLogger",
        )

        Throwable()
            .stackTrace
            .filter { it.className.startsWith(BuildConfig.APPLICATION_ID) }
            .filterNot { excludedClassPrefixes.any<String>(it.className::startsWith) }
            .firstOrNull()
            ?.let {
                return buildString {
                    append("[${it.className.substringAfterLast('.')}:${it.lineNumber}] ")
                    append(this@decorate)
                }
            }

        return "[Unknown] $this"
    }

    override fun px(levelFilter: Int, level: String, msg: String, thr: Throwable?) {
        if (logLevelFilter > levelFilter) return
        if (!isLogToXposed) return
        val logMessage = buildString {
            append("[$level/$logTag]")
            append(msg.decorate())
            if (thr != null) {
                append(": ${thr.stackTraceToString()}")
            }
        }
        XposedBridge.log(logMessage)
    }
}