package me.timschneeberger.onyxtweaks.mods.base

import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import kotlin.reflect.KClass

abstract class ModPack {
    val targetPackages by lazy { getTargetPackages(this::class) }

    open fun updatePrefs(vararg key: String?) {}

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