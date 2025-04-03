package me.timschneeberger.onyxtweaks.mods.global

import android.app.Activity
import android.onyx.optimization.EInkHelper
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.misc.AndroidUtils
import com.onyx.android.sdk.device.Device
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.GLOBAL
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.ui.model.ActivityRule
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(GLOBAL)
class PerActivityRefreshModes : ModPack() {
    override val group = PreferenceGroups.PER_ACTIVITY_SETTINGS

    // Note: Do not modify the enum value names, as they are serialized to preferences
    enum class UpdateOption(val value: Int) {
        UNKNOWN(-1),
        DEFAULT(0),
        DU(1),
        A2(2),
        REGAL(3),
        X(4)
    }

    private fun findRules(pkgName: String) =
        preferences.get<String>(R.string.key_per_activity_settings).run {
            try {
                Json.decodeFromString<List<ActivityRule>>(this)
                    .filter { it.packageName == pkgName }
            } catch (e: SerializationException) {
                Log.ex("Failed to load activity rules for $pkgName: $e")
                emptyList()
            }
        }

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        MethodFinder.fromClass(Activity::class.java)
            .firstByName("onResume")
            .createAfterHookCatching<PerActivityRefreshModes> { param ->
                findRules(lpParam.packageName).let { rules ->
                    var rule = rules.firstOrNull { it.activityClass.also { string -> Log.ex("Comparing $string to ${param.thisObject::class.java.name}") } == param.thisObject::class.java.name }
                    rule = rule ?: rules.firstOrNull { it.activityClass == null }
                    rule?.let { rule ->
                        // Delay to allow Onyx's onResume hook to switch the currently cached component name,
                        // otherwise the previous component will be modified
                        AndroidUtils.mainHandler.postDelayed({
                            EInkHelper.setAppScopeRefreshMode(rule.updateMode.value)
                            Log.dx("Refresh mode set to ${Device.currentDevice().appScopeRefreshMode}")
                        }, 500)
                    }
                }
            }
    }
}