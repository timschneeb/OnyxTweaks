package me.timschneeberger.onyxtweaks.mods.shared

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_FRAMEWORK_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.dpToPx
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.getClass
import me.timschneeberger.onyxtweaks.mods.utils.getDimensionPxByName
import me.timschneeberger.onyxtweaks.mods.utils.getDrawableByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.castNonNull

@TargetPackages(SYSTEM_UI_PACKAGE, SYSTEM_FRAMEWORK_PACKAGE)
class AddUserSwitcherToQs : ModPack() {
    override val group = PreferenceGroups.QS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_qs_header_show_user_switcher))
            return

        when (lpParam.packageName) {
            SYSTEM_UI_PACKAGE -> handleLoadSystemUi()
            SYSTEM_FRAMEWORK_PACKAGE -> handleLoadFramework()
        }
    }

    private fun handleLoadFramework() {
        getClass("android.os.UserManager").apply {
            methodFinder()
                .filterByName("isUserSwitcherEnabled")
                .first()
                .replaceWithConstant(true)

            methodFinder()
                .filterByName("supportsMultipleUsers")
                .first()
                .replaceWithConstant(true)
        }
    }

    private fun handleLoadSystemUi() {
        MethodFinder.fromClass("android.onyx.systemui.SystemUIConfig")
            .firstByName("isShowUserSwitch")
            .replaceWithConstant(true)

        MethodFinder.fromClass("com.android.systemui.qs.QSPanel")
            .firstByName("initTabletTitleBar")
            .createAfterHook { param ->
                // Obtain settings button and its context
                val settingsViewGroup = param.thisObject
                    .objectHelper()
                    .getObjectOrNull("settings")
                    .castNonNull<View>()

                val ctx = settingsViewGroup.context
                val userSwitcherLayout = LinearLayout(ctx).apply layout@ {
                    // Set layout params
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = ctx.dpToPx(12)
                    }.let { it -> layoutParams = it }

                    background = ctx.resources.getDrawableByName("button_focus_background_dot")

                    setOnClickListener { v ->
                        // Open switcher dialog if available, otherwise launch user settings
                        if(!showUserSwitchDialog(this@layout))
                            launchUserSettings()
                    }

                    // Add user switcher button to the QS panel
                    addView(
                        ImageView(ctx).apply {
                            val iconSize =
                                ctx.resources.getDimensionPxByName("ic_edit_qs_tile_icon_size") ?: ctx.dpToPx(26)
                            val padding =
                                ctx.resources.getDimensionPxByName("onyx_clock_row_button_padding") ?: ctx.dpToPx(6)

                            layoutParams =
                                LinearLayout.LayoutParams(iconSize, iconSize).apply {
                                    setMargins(padding, padding, padding, padding)
                                }
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            imageTintMode = PorterDuff.Mode.SRC_IN
                            imageTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(ctx, android.R.color.black)
                            )
                            setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    EzXHelper.moduleRes,
                                    R.drawable.ic_account,
                                    null
                                )
                            )
                        }
                    )
                }

                // Add the layout to the QS panel, next to the settings button
                settingsViewGroup.parent
                    .castNonNull<ViewGroup>()
                    .addView(userSwitcherLayout)
            }
    }

    private fun showUserSwitchDialog(hostLayout: ViewGroup) = try {
        val factory = getClass("com.android.systemui.SystemUIFactory")
            .methodFinder()
            .firstByName("getInstance")
            .invoke(null)

        val sysUiComp = factory::class.java
            .methodFinder()
            .firstByName("getSysUIComponent")
            .invoke(factory)

        // Retrieve UserSwitchDialogController instance to show dialog
        sysUiComp!!
            .objectHelper()
            .getObjectOrNull("userSwitchDialogControllerProvider")!!
            .let { provider ->

                provider::class.java
                    .methodFinder()
                    .firstByName("get")
                    .invoke(provider)
            }
            .also { ctrl ->
                ctrl::class.java
                    .methodFinder()
                    .firstByName("showDialog")
                    .invoke(ctrl, hostLayout)
            }

        true
    }
    catch (ex: Exception) {
        Log.wx("Failed to show user switch dialog, falling back to system settings", ex)
        false
    }

    private fun launchUserSettings() {
        try {
            Intent().apply {
                action = "android.intent.action.MAIN"
                setClassName(
                    "com.android.settings",
                    "com.android.settings.Settings\$UserSettingsActivity"
                )
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.let(appContext::startActivity)
        }
        catch (ex: Exception) {
            Log.ex("Failed to launch user settings", ex)

            Toast.makeText(
                appContext,
                "Failed to launch user settings. Not supported.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}