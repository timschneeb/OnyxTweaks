package me.timschneeberger.onyxtweaks.mods.systemui

import android.content.Intent
import android.provider.Settings
import android.view.View
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ISystemUiActivityStarter
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.getClass
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethod
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.castNonNull

@TargetPackages(SYSTEM_UI_PACKAGE)
class AddSettingsButtonToQs : ModPack(), ISystemUiActivityStarter {
    override val group = PreferenceGroups.QS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_qs_header_show_settings_button))
            return

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
                        val value = preferences.get<String>(R.string.key_qs_header_settings_button_action)
                        when (value) {
                            "onyx_settings" -> param.invokeOriginalMethod()
                            // TODO stock settings not getting called
                            "stock_settings" -> startActivityDismissingKeyguard(Intent(Settings.ACTION_SETTINGS))
                            else -> Log.ex("Unknown QS settings action '$value'")
                        }
                    }
                }
        }
    }
}
