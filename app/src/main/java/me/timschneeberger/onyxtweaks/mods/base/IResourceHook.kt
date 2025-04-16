package me.timschneeberger.onyxtweaks.mods.base

import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam

/**
 * Interface for resource hooks.
 * Resource hooks are discouraged, as they are deprecated in LSPosed and will be removed in the future.
 * As these hooks also have a performance impact, it is often better to use a method hook instead.
 */
interface IResourceHook {
    /**
     * Handle the initialization of package resources.
     * Optional.
     *
     * @param param the initialization parameters
     */
    fun handleInitPackageResources(param: InitPackageResourcesParam) {}
}
