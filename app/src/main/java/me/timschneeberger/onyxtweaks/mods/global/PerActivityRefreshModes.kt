package me.timschneeberger.onyxtweaks.mods.global

import android.app.Activity
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.misc.AndroidUtils
import com.onyx.android.sdk.device.Device
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.GLOBAL
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.ui.model.ActivityRule
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

/**
 * This mod pack allows setting the refresh mode and update method for specific activities.
 *
 * Update methods are not supposed to be exposed to the user and should be used with caution.
 * Not all methods are supported by all refresh modes and devices.
 */
@TargetPackages(GLOBAL)
class PerActivityRefreshModes : ModPack() {
    override val group = PreferenceGroups.PER_ACTIVITY_SETTINGS

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
                    var rule = rules.firstOrNull { it.activityClass == param.thisObject::class.java.name }
                    rule = rule ?: rules.firstOrNull { it.activityClass == null } // fallback to default rule
                    rule?.let { rule ->
                        // Delay to allow Onyx's onResume hook to switch the currently cached component name,
                        // otherwise the previous component will be modified
                        AndroidUtils.mainHandler.postDelayed({
                            Log.dx("Setting refresh mode to ${rule.updateMethod}")

                            Device.currentDevice().clearAppScopeUpdate()
                            Device.currentDevice().applyAppScopeUpdate(
                                lpParam.packageName,
                                true,
                                false,
                                rule.updateMethod,
                                Int.MAX_VALUE
                            )
                        }, 500)
                    }
                }
            }
    }
}