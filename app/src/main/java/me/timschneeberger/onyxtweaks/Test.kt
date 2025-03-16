package me.timschneeberger.onyxtweaks

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class Test : IXposedHookLoadPackage {
    val prefs: XSharedPreferences = XSharedPreferences(BuildConfig.APPLICATION_ID, "conf")

    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        if (BuildConfig.DEBUG) {
            XposedBridge.log(
                "D/$TAG downgrade=" + prefs.getBoolean(
                    "downgrade",
                    true
                )
            )
            XposedBridge.log(
                "D/$TAG authcreak=" + prefs.getBoolean(
                    "authcreak",
                    false
                )
            )
            XposedBridge.log(
                "D/$TAG digestCreak=" + prefs.getBoolean(
                    "digestCreak",
                    true
                )
            )
            XposedBridge.log(
                "D/$TAG UsePreSig=" + prefs.getBoolean(
                    "UsePreSig",
                    false
                )
            )
            XposedBridge.log(
                "D/$TAG bypassBlock=" + prefs.getBoolean(
                    "bypassBlock",
                    true
                )
            )
            XposedBridge.log(
                "D/$TAG sharedUser=" + prefs.getBoolean(
                    "sharedUser",
                    false
                )
            )
            XposedBridge.log(
                "D/$TAG disableVerificationAgent=" + prefs.getBoolean(
                    "disableVerificationAgent",
                    true
                )
            )
        }
    }


    companion object {
        const val TAG: String = "OnyxTweaks"
    }
}
