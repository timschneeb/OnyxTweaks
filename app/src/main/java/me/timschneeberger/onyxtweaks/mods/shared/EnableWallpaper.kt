package me.timschneeberger.onyxtweaks.mods.shared

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.XResources
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.IEarlyZygoteHook
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethod
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.mods.utils.runSafely
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(LAUNCHER_PACKAGE)
class EnableWallpaper : ModPack(), IEarlyZygoteHook {
    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_launcher_desktop_wallpaper))
            return

        MethodFinder.fromClass("com.onyx.common.common.model.DeviceConfig")
            .firstByName("isWallpaperFeatureEnabled")
            .replaceWithConstant(true)

        MethodFinder.fromClass("com.onyx.reader.main.model.NormalUserDataProvider")
            .firstByName("getUserAppConfig")
            .createAfterHookCatching<EnableWallpaper> { param ->
                val config = param.invokeOriginalMethod()
                config?.objectHelper()?.setObjectUntilSuperclass("enableWallpaperFeature", true)
                param.result = config
            }

        @SuppressLint("MissingPermission")
        ConstructorFinder.fromClass("com.onyx.common.applications.view.Workspace")
            .filterByParamTypes(Context::class.java, AttributeSet::class.java)
            .first()
            .createAfterHookCatching<EnableWallpaper> { param ->
                fun setWallpaper() {
                        MethodFinder.fromClass(View::class)
                            .filterByName("setBackground")
                            .filterByParamTypes(Drawable::class.java)
                            .first()
                            .invoke(
                                param.thisObject,
                                WallpaperManager.getInstance(appContext).drawable
                            )
                }

                // Set wallpaper on creation
                setWallpaper()

                // Register WALLPAPER_CHANGED broadcast receiver
                ContextCompat.registerReceiver(
                    appContext,
                    object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            runSafely(EnableWallpaper::class) {
                                Log.dx("Received broadcast: ${intent.action}")
                                setWallpaper()
                            }
                        }
                    },
                    IntentFilter().apply {
                        addAction("com.onyx.action.WALLPAPER_CHANGED")
                        addAction("android.intent.action.WALLPAPER_CHANGED")
                    },
                    ContextCompat.RECEIVER_EXPORTED
                )
            }


        MethodFinder.fromClass("com.onyx.common.applications.action.DesktopOptionProcessImpl")
            .firstByName("onWallpaperSelection")
            .createReplaceHookCatching<EnableWallpaper> {
                Intent().apply {
                    action = "android.intent.action.MAIN"
                    setClassName("com.android.settings", "com.android.settings.Settings\$UserSettingsActivity")
                    setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }.let(appContext::startActivity)
                null
            }
    }

    override fun handleZygoteInit(param: IXposedHookZygoteInit.StartupParam) {
        if (!preferences.get<Boolean>(R.string.key_launcher_desktop_wallpaper))
            return

        XResources.setSystemWideReplacement(
            "android",
            "bool",
            "config_enableWallpaperService",
            true
        )
    }
}