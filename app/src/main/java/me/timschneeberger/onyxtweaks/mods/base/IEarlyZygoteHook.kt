package me.timschneeberger.onyxtweaks.mods.base

import de.robv.android.xposed.IXposedHookZygoteInit

/**
 * Interface for early Zygote hooks.
 * If a mod implements this interface, it will be instantiated during Zygote initialization.
 */
interface IEarlyZygoteHook {
    /**
     * Handle the initialization of the Zygote process.
     * This is called before any application code is loaded.
     *
     * @param param the startup parameters
     */
    fun handleZygoteInit(param: IXposedHookZygoteInit.StartupParam)
}
