package me.timschneeberger.onyxtweaks.mods.global

import android.app.Activity
import android.onyx.optimization.EInkHelper
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.misc.AndroidUtils
import com.onyx.android.sdk.api.device.epd.UpdateMode
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

    // Note: Do not modify the enum value names, as they are serialized to preferences
    enum class UpdateOption(val value: Int) {
        UNKNOWN(-1),
        DEFAULT(0),
        DU(1),
        A2(2),
        REGAL(3),
        X(4),
        REGAL_PLUS(5)
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
                    var rule = rules.firstOrNull { it.activityClass == param.thisObject::class.java.name }
                    rule = rule ?: rules.firstOrNull { it.activityClass == null } // fallback to default rule
                    rule?.let { rule ->
                        // Delay to allow Onyx's onResume hook to switch the currently cached component name,
                        // otherwise the previous component will be modified
                        AndroidUtils.mainHandler.postDelayed({

                            Log.dx("Setting refresh mode to ${rule.updateMode}")

                            Device.currentDevice().clearAppScopeUpdate()
                            EInkHelper.setAppScopeRefreshMode(rule.updateMode.value)

                            // TODO Allow user to select turbo level using EInkHelper.setTurbo

                            val method = rule.updateMethod
                            if (method != UpdateMode.None) {
                                Device.currentDevice().applyAppScopeUpdate(
                                    lpParam.packageName,
                                    true,
                                    false,
                                    method,
                                    Int.MAX_VALUE
                                )
                                Log.dx("Setting update mode to $method")
                            }
                        }, 500)
                    }
                }
            }
    }
}