package me.timschneeberger.onyxtweaks.mods.base

import com.github.kyuubiran.ezxhelper.EzXHelper
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Preferences
import kotlin.reflect.KClass

abstract class ModPack {
    abstract val group: PreferenceGroups

    val targetPackages by lazy { getTargetPackages(this::class) }
    val preferences = lazy { Preferences(group).also {
        it.onPreferencesChanged = ::onPreferencesChanged
    }}

    /**
     * Called when a preference is changed.
     *
     * @param key the key of the preference that was changed
     *            or null if all preferences were changed during initialization
     */
    open fun onPreferencesChanged(key: String?) {
        XposedBridge.log(EzXHelper.hostPackageName + ": Preference changed: $key")
    }

    /**
     * Handle the loading of a package.
     *
     * @param lpParam load package parameters
     */
    open fun handleLoadPackage(lpParam: LoadPackageParam) {}

    /**
     * Handle the initialization of package resources.
     * Optional.
     *
     * @param param the initialization parameters
     */
    open fun handleInitPackageResources(param: InitPackageResourcesParam) {}

    companion object {
        fun getTargetPackages(modPackCls: KClass<*>): Array<String> =
            modPackCls.annotations
                .mapNotNull { it as? TargetPackages }
                .fold(arrayOf<String>()) { acc, annotation -> acc + annotation.targets }
    }
}