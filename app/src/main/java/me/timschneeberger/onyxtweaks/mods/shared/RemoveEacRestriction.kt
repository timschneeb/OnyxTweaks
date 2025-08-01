package me.timschneeberger.onyxtweaks.mods.shared

import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_FRAMEWORK_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethodCatching
import me.timschneeberger.onyxtweaks.mods.utils.replaceCatchingWithExpression
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Version.Companion.toVersion
import me.timschneeberger.onyxtweaks.utils.onyxVersion
import java.lang.reflect.Method

/**
 * This mod pack removes the restriction of EAC in the Onyx EInk Center.
 *
 * Note: It also seems to force-enable the EAC configuration for all apps.
 *       This is problematic, as it causes the rotation to the forcefully fixed, unless
 *       the EAC rotation setting is manually changed.
 *
 * @deprecated This mod pack causes side-effects and has no real use case. Slated for removal.
 *             Only functional in firmware v4.0, dropped in v4.1.
 */
@TargetPackages(SYSTEM_FRAMEWORK_PACKAGE, SYSTEM_UI_PACKAGE, LAUNCHER_PACKAGE)
class RemoveEacRestriction : ModPack() {
    override val group = PreferenceGroups.EINK

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (onyxVersion >= "4.1".toVersion() || !preferences.get<Boolean>(R.string.key_eink_center_always_allow_eac))
            return

        if (lpParam.packageName == LAUNCHER_PACKAGE) {
            MethodFinder.fromClass("com.onyx.android.sdk.eac.data.v2.EACAppConfig")
                .firstByName("isSupportEAC")
                .replaceWithConstant(true)

            MethodFinder.fromClass("com.onyx.android.sdk.utils.ApplicationUtil")
                .firstByName("getApplicationType")
                .replaceCatchingWithExpression<RemoveEacRestriction> {
                    findClass("com.onyx.android.sdk.utils.ApplicationUtil\$AppType")
                        .objectHelper()
                        .getObjectOrNull("THIRD_PARTY")
                }
        }

        MethodFinder.fromClass("android.onyx.optimization.data.v2.EACAppConfig")
            .firstByName("isSupportEAC")
            .replaceWithConstant(true)

        MethodFinder.fromClass("android.onyx.utils.ActivityManagerHelper")
            .filterByParamTypes(String::class.java)
            .firstByName("isOnyxApp")
            .replaceSystemAppCheck()
        MethodFinder.fromClass("android.onyx.utils.ActivityManagerHelper")
            .firstByName("isSystemApp")
            .replaceSystemAppCheck()
    }
}

private fun Method.replaceSystemAppCheck() {
    val overriddenMethods = mapOf(
        ".EACRotationManager" to "getAppTargetRotation",
        ".EACSplashScreenManager" to "isPackageAllowSplashScreen",
        ".View" to "updateCanvasImpl",
        ".EInkHelper" to "getApplicationDPI",
    )

    createReplaceHookCatching<RemoveEacRestriction> { param ->
        val calledFromTarget = Throwable().stackTrace.any { element ->
            overriddenMethods.any { entry ->
                element.className.contains(entry.key) && element.methodName == entry.value
            }
        }

        return@createReplaceHookCatching when {
            calledFromTarget -> false
            else -> param.invokeOriginalMethodCatching()
        }
    }
}