package me.timschneeberger.onyxtweaks.mods.shared

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.SystemClock
import android.util.Base64
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.hostPackageName
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_FRAMEWORK_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_SETTINGS_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethod
import me.timschneeberger.onyxtweaks.mods.utils.runSafely
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.cast

@TargetPackages(SYSTEM_FRAMEWORK_PACKAGE, SYSTEM_SETTINGS_PACKAGE)
class ResumeActivityAfterReboot : ModPack() {
    override val group = PreferenceGroups.RESUME_APP_SETTINGS

    companion object {
        // com.android.server.wm.WindowManagerService
        var windowManagerService: Any? = null
        var bootComplete = false

        const val ACTION_DONE_POLL_WAIT_MS = 100L
        const val MAX_BROADCAST_TIME_MS = 2000L
    }

    override fun handleEarlyLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (lpParam.packageName != SYSTEM_FRAMEWORK_PACKAGE)
            return

        // Obtain internal WindowManagerService instance
        MethodFinder.fromClass("com.android.server.am.ActivityManagerService")
            .firstByName("setWindowManager")
            .createAfterHookCatching<ResumeActivityAfterReboot> { param ->
                windowManagerService = param.args[0]
                Log.dx("WindowManagerService handle received: $windowManagerService")
            }
    }

    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalStdlibApi::class)
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        when (hostPackageName) {
            SYSTEM_FRAMEWORK_PACKAGE -> hookShutdownThread()
            SYSTEM_SETTINGS_PACKAGE -> hookFallbackHome()
        }
    }

    private fun hookFallbackHome() {
        MethodFinder.fromClass("android.app.Activity")
            .firstByName("finish")
            .createAfterHookCatching<ResumeActivityAfterReboot> { param ->
                if(param.thisObject::class.qualifiedName != "com.android.settings.FallbackHome")
                    return@createAfterHookCatching

                Log.dx("${param.thisObject::class.qualifiedName} finished")

                if (bootComplete) {
                    Log.dx("Boot complete, skipping activity resume")
                    return@createAfterHookCatching
                }
                bootComplete = true

                val allowedPackages = preferences.get<Set<String>>(R.string.key_resume_app_rules)
                if (allowedPackages.isEmpty()) {
                    Log.dx("Activity resume disabled")
                    return@createAfterHookCatching
                }

                val intent = preferences.get<String>(R.string.key_resume_app_persisted_intent)
                    .let { Base64.decode(it, Base64.DEFAULT) }
                    .let {
                        Parcel.obtain()
                            .apply { unmarshall(it, 0, it.size) }
                            .let { parcel ->
                                parcel.setDataPosition(0)
                                Intent.CREATOR.createFromParcel(parcel)
                                    .also { Log.dx("Saved intent: $it") }
                                    .apply { this?.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES) }
                                    .takeIf { intent ->
                                        allowedPackages.contains(intent?.component?.packageName) ||
                                                allowedPackages.contains(intent.`package`)
                                    }
                                    .also { parcel.recycle() }
                            }
                    }

                if (intent == null) {
                    Log.dx("No allowed intent found, skipping activity resume")
                    return@createAfterHookCatching
                }

                setPreference<String>(R.string.key_resume_app_persisted_intent, null)

                Handler(Looper.myLooper()!!).postDelayed(
                    Runnable {
                        intent.runSafely(this::class, "Resuming activity", block = EzXHelper.appContext::startActivity)
                    },
                    1000 * preferences
                        .get<Long>(R.string.key_resume_app_delay_seconds)
                        .coerceAtLeast(0)
                )
            }
    }

    private fun hookShutdownThread() {
        MethodFinder.fromClass("com.android.server.power.ShutdownThread")
            .firstByName("run")
            .createReplaceHookCatching<ResumeActivityAfterReboot> hook@ { param ->
                val rootWindow = windowManagerService?.objectHelper()
                    ?.getObjectOrNull("mRoot") // com.android.server.wm.RootWindowContainer

                if (rootWindow == null) {
                    Log.dx("RootWindowContainer is null")
                    return@hook param.invokeOriginalMethod()
                }

                // com.android.server.wm.ActivityRecord
                val topActivityRecord = MethodFinder.fromClass(rootWindow::class.java)
                    .firstByName("getTopResumedActivity")
                    .invoke(rootWindow)

                if (topActivityRecord == null)
                    return@hook param.invokeOriginalMethod()

                topActivityRecord
                    .objectHelper()
                    .getObjectOrNull("intent")
                    .cast<Intent>()
                    ?.let { intent ->
                        val parcel = Parcel.obtain()
                        intent.writeToParcel(parcel, 0)
                        val encodedIntent = Base64.encodeToString(parcel.marshall(), Base64.NO_WRAP)

                        var actionDone = false
                        val actionDoneSync = Object()

                        setPreference<String>(R.string.key_resume_app_persisted_intent, encodedIntent) {
                            Log.dx("Intent stored successfully")

                            synchronized (actionDoneSync) {
                                actionDone = true
                                actionDoneSync.notifyAll()
                            }
                        }

                        parcel.recycle()

                        // Wait for the action to be done before proceeding with the shutdown
                        val endTime = SystemClock.elapsedRealtime() + MAX_BROADCAST_TIME_MS
                        synchronized (actionDoneSync) {
                            while (!actionDone) {
                                val delay = endTime - SystemClock.elapsedRealtime()
                                if (delay <= 0) {
                                    Log.wx("Shutdown broadcast timed out");
                                    break
                                }

                                try {
                                    actionDoneSync.wait(delay.coerceAtMost(ACTION_DONE_POLL_WAIT_MS));
                                } catch (_: InterruptedException) {
                                }
                            }
                        }

                        // Continue with the shutdown
                        param.invokeOriginalMethod()
                    }
            }
    }
}