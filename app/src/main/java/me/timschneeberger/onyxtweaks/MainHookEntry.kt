package me.timschneeberger.onyxtweaks

import com.github.kyuubiran.ezxhelper.Config
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.ModManager
import me.timschneeberger.onyxtweaks.utils.CustomLogger

// TODO: set refresh mode based on activity

class MainHookEntry : IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
    private var modManager: ModManager = ModManager()

    override fun handleInitPackageResources(param: InitPackageResourcesParam) {
        ensureLoggerInitialized()
        modManager.handleInitPackageResources(param)
    }

    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        ensureLoggerInitialized()
        Config.enableFinderExceptionMessage = true

        // Only initialize using the first package for this process.
        // This is a workaround for the fact that the hook is called multiple times when an app is
        // running other packages within their process. (Example: com.google.android.webview)
        if (loadPackageParam.isFirstApplication) {
            EzXHelper.initHandleLoadPackage(loadPackageParam)
            EzXHelper.setLogTag("OT/${simplifyPackageName(loadPackageParam.packageName)}")
            Log.dx("Initializing mod packs")
        } else {
            Log.dx("Existing instance of also used for: ${loadPackageParam.packageName}")
        }

        modManager.handleLoadPackage(loadPackageParam)
    }

    override fun initZygote(startupParam: StartupParam) {
        ensureLoggerInitialized()
        EzXHelper.initZygote(startupParam)
        modManager.initZygote(startupParam)
    }

    private fun ensureLoggerInitialized() {
        if(Log.currentLogger != CustomLogger)
            Log.currentLogger = CustomLogger
    }

    private fun simplifyPackageName(packageName: String) = when (packageName) {
        BuildConfig.APPLICATION_ID -> "OnyxTweaks"
        SYSTEM_UI_PACKAGE -> "SystemUI"
        else -> packageName
    }
}
