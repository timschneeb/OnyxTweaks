package me.timschneeberger.onyxtweaks.mods.global

import android.app.Activity
import android.onyx.optimization.EInkHelper
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.misc.AndroidUtils
import com.onyx.android.sdk.device.Device
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants.GLOBAL
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(GLOBAL)
class PerActivityRefreshModes : ModPack() {
    override val group = PreferenceGroups.NONE

    enum class UpdateOption(val value: Int) {
        UNKNOWN(-1),
        DEFAULT(0),
        DU(1),
        A2(2),
        REGAL(3),
        X(4)
    }

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        MethodFinder.fromClass(Activity::class.java)
            .firstByName("onResume")
            .createAfterHookCatching { param ->
                Log.ex("Resumed: ${param.thisObject::class.java.name}")
                if(!lpParam.packageName.contains("komikku"))
                    return@createAfterHookCatching

                // Delay to allow Onyx's onResume hook to switch the currently cached component name,
                // otherwise the previous component will be modified
                AndroidUtils.mainHandler.postDelayed({
                    if (param.thisObject::class.java.simpleName == "ReaderActivity")
                        EInkHelper.setAppScopeRefreshMode(UpdateOption.REGAL.value)
                    else
                        EInkHelper.setAppScopeRefreshMode(UpdateOption.DU.value)

                    Log.ex("Refresh mode set to ${Device.currentDevice().appScopeRefreshMode}")
                }, 500)

                Log.ex("Refresh mode set to ${Device.currentDevice().appScopeRefreshMode}")
            }
    }
}