package me.timschneeberger.onyxtweaks.mods.global

import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.GLOBAL
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(GLOBAL)
class ShowRecyclerViewScrollBar : ModPack() {
    override val group = PreferenceGroups.MISC

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_framework_views_scrollbar_recyclerview))
            return

        findClass("com.android.internal.widget.LinearLayoutManager")
            .methodFinder()
            .firstByName("showScrollBar")
            .replaceWithConstant(true)

        findClass("com.android.internal.widget.RecyclerView")
            .methodFinder()
            .firstByName("showScrollBar")
            .replaceWithConstant(true)
    }
}