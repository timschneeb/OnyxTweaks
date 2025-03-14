package me.timschneeberger.onyxtweaks.mods.systemui

import android.content.Intent
import android.provider.Settings
import android.view.View
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ISystemUiActivityStarter
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.castNonNull
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.getClass

@TargetPackages(SYSTEM_UI_PACKAGE)
class AddSettingsButtonToQs : ModPack(), ISystemUiActivityStarter {
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        getClass("com.android.systemui.qs.QSPanel").apply {
            // Set the settings button to visible
            methodFinder()
                .firstByName("initTabletTitleBar")
                .createAfterHook { param ->
                    param.thisObject
                        .objectHelper()
                        .getObjectOrNull("settings")
                        .castNonNull<View>()
                        .apply { visibility = View.VISIBLE }
                }

            // Override action
            methodFinder()
                .firstByName("startOnyxSettings")
                .createHook {
                    replace { param ->
                        // TODO Implement selection
                        startActivityDismissingKeyguard(Intent(Settings.ACTION_SETTINGS))
                        // Onyx: param.invokeOriginalMethod()
                    }
                }
        }
    }
}
