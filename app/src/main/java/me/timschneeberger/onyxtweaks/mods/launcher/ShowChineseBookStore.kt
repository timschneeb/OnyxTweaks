package me.timschneeberger.onyxtweaks.mods.launcher

import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.mods.utils.firstByNameOrLog
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant

@TargetPackages(LAUNCHER_PACKAGE)
class ShowChineseBookStore : ModPack() {
    override val group = PreferenceGroups.LAUNCHER

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_launcher_store_show_chinese_store))
            return

        MethodFinder.fromClass("com.onyx.common.common.manager.BookshopManager")
            .firstByNameOrLog("isShowShopSelectUI")
            .replaceWithConstant(true)

        MethodFinder.fromClass("com.onyx.android.sdk.data.cluster.ClusterFeatures")
            .firstByNameOrLog("supportChinaBookShops")
            .replaceWithConstant(true)
    }
}