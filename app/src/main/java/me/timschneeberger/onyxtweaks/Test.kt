package me.timschneeberger.onyxtweaks

import me.timschneeberger.onyxtweaks.BuildConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.lang.reflect.InvocationTargetException

class Test : IXposedHookLoadPackage {
    val prefs: XSharedPreferences = XSharedPreferences(BuildConfig.APPLICATION_ID, "conf")

    @Throws(
        IllegalAccessException::class,
        InvocationTargetException::class,
        InstantiationException::class
    )
    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        if (BuildConfig.DEBUG) {
            XposedBridge.log(
                "D/" + MainHook.Companion.TAG + " downgrade=" + prefs.getBoolean(
                    "downgrade",
                    true
                )
            )
            XposedBridge.log(
                "D/" + MainHook.Companion.TAG + " authcreak=" + prefs.getBoolean(
                    "authcreak",
                    false
                )
            )
            XposedBridge.log(
                "D/" + MainHook.Companion.TAG + " digestCreak=" + prefs.getBoolean(
                    "digestCreak",
                    true
                )
            )
            XposedBridge.log(
                "D/" + MainHook.Companion.TAG + " UsePreSig=" + prefs.getBoolean(
                    "UsePreSig",
                    false
                )
            )
            XposedBridge.log(
                "D/" + MainHook.Companion.TAG + " bypassBlock=" + prefs.getBoolean(
                    "bypassBlock",
                    true
                )
            )
            XposedBridge.log(
                "D/" + MainHook.Companion.TAG + " sharedUser=" + prefs.getBoolean(
                    "sharedUser",
                    false
                )
            )
            XposedBridge.log(
                "D/" + MainHook.Companion.TAG + " disableVerificationAgent=" + prefs.getBoolean(
                    "disableVerificationAgent",
                    true
                )
            )
        }
    }
}
