package me.timschneeberger.onyxtweaks.mods.systemui

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceCatchingWithExpression
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(SYSTEM_UI_PACKAGE)
class MoveNotificationHeaderToFooter : ModPack() {
    override val group = PreferenceGroups.STATUS_BAR

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        // Note: may cause minor visual issues
        MethodFinder.fromClass("android.onyx.systemui.SystemUIConfig")
            .firstByName("isNotificationManagerItemStayOnTop")
            .replaceCatchingWithExpression<MoveNotificationHeaderToFooter> {
                !preferences.get<Boolean>(R.string.key_status_bar_notifications_move_header_to_bottom)
            }
    }
}