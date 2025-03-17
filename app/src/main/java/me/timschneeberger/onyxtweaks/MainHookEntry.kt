package me.timschneeberger.onyxtweaks

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import me.timschneeberger.onyxtweaks.mods.ModManager


// TODO: EAC activity based settings

class MainHookEntry : IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
    private var modManager: ModManager = ModManager()

    override fun handleInitPackageResources(param: InitPackageResourcesParam) {
        modManager.handleInitPackageResources(param)
    }

    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        // Only initialize using the first package for this process.
        // This is a workaround for the fact that the hook is called multiple times when an app is
        // running other packages within their process. (Example: com.google.android.webview)
        if (loadPackageParam.isFirstApplication) {
            EzXHelper.initHandleLoadPackage(loadPackageParam)
            EzXHelper.setLogTag("OnyxTweaks/${loadPackageParam.packageName}")
            Log.dx("Initializing mod packs for: ${loadPackageParam.packageName}")
        } else {
            val host = if(EzXHelper.isHostPackageNameInited) EzXHelper.hostPackageName else "<uninitialized>"
            Log.dx("Existing instance of $host also used for: ${loadPackageParam.packageName}")
        }

        modManager.handleLoadPackage(loadPackageParam)
    }

    override fun initZygote(startupParam: StartupParam) {
        EzXHelper.initZygote(startupParam)
        modManager.initZygote(startupParam)
    }
}
