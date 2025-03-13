package me.timschneeberger.onyxtweaks.mods.base

import de.robv.android.xposed.IXposedHookZygoteInit

interface IEarlyZygoteHook {
    fun handleZygoteInit(param: IXposedHookZygoteInit.StartupParam)
}
