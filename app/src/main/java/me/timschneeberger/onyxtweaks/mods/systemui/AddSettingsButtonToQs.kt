package me.timschneeberger.onyxtweaks.mods.systemui

import android.content.Intent
import android.provider.Settings
import android.view.View
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ISystemUiActivityStarter
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.invokeOriginalMethodCatching
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.castNonNull

@TargetPackages(SYSTEM_UI_PACKAGE)
class AddSettingsButtonToQs : ModPack(), ISystemUiActivityStarter {
    override val group = PreferenceGroups.QS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_qs_header_show_settings_button))
            return

        findClass("com.android.systemui.qs.QSPanel").apply {
            // Set the settings button to visible
            methodFinder()
                .firstByName("initTabletTitleBar")
                .createAfterHookCatching<AddSettingsButtonToQs> { param ->
                    param.thisObject
                        .objectHelper()
                        .getObjectOrNull("settings")
                        .castNonNull<View>()
                        .apply { visibility = View.VISIBLE }
                }

            // Override action
            methodFinder()
                .firstByName("startOnyxSettings")
                .createReplaceHookCatching<AddSettingsButtonToQs> { param ->
                    val value = preferences.get<String>(R.string.key_qs_header_settings_button_action)
                    when (value) {
                        "onyx_settings" -> {
                            // Instead of calling the original method, which opens the settings as full-screen in SettingsActivity,
                            // we need to open the settings fragment within the MainActivity as a tab.
                            try {
                                startActivityDismissingKeyguard(
                                    Intent().apply {
                                        action = "com.onyx.intent.action.MAIN_ACTIVITY"
                                        `package` = LAUNCHER_PACKAGE
                                        putExtra("json", """{"action": "${TabAction.OPEN_SETTING}"}""")
                                    }
                                )
                            }
                            catch (ex: Exception) {
                                Log.ex("Failed to start Onyx settings activity", ex)
                                // Fall back to the original implementation
                                param.invokeOriginalMethodCatching()
                            }
                        }
                        "stock_settings" -> startActivityDismissingKeyguard(Intent(Settings.ACTION_SETTINGS))
                        else -> Log.ex("Unknown QS settings action '$value'")
                    }
                }
        }
    }

    private enum class TabAction {
        NOTHING, OPEN_HOME, OPEN_STORAGE, OPEN_APPLICATION_MANAGEMENT, OPEN_SETTING,
        OPEN_APPS, OPEN_NOTE, OPEN_ACCOUNT_MANAGER, OPEN_SHOP
    }
}
