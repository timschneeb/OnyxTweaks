package me.timschneeberger.onyxtweaks.mods.systemui

import android.view.View
import android.view.ViewGroup
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.firstByNameOrLog
import me.timschneeberger.onyxtweaks.mods.utils.getClass
import me.timschneeberger.onyxtweaks.mods.utils.inflateLayoutByName
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.castNonNull
import java.lang.reflect.Method

@TargetPackages(SYSTEM_UI_PACKAGE)
class CustomizeRecents : ModPack() {
    override val group = PreferenceGroups.RECENTS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (preferences.get<Boolean>(R.string.key_recents_grid_custom_size)) {
            getClass("com.android.systemui.recents.OnyxRecentsActivity").apply {
                methodFinder()
                    .firstByNameOrLog("getRow")
                    .replaceSizeByOrientation(
                        preferences.getStringAsInt(R.string.key_recents_grid_row_count_portrait),
                        preferences.getStringAsInt(R.string.key_recents_grid_row_count_landscape)
                    )

                methodFinder()
                    .firstByNameOrLog("getColumn")
                    .replaceSizeByOrientation(
                        preferences.getStringAsInt(R.string.key_recents_grid_column_count_portrait),
                        preferences.getStringAsInt(R.string.key_recents_grid_column_count_landscape)
                    )
            }
        }

        if (preferences.get<Boolean>(R.string.key_recents_use_stock_header)) {
            MethodFinder.fromClass("com.android.systemui.recents.OnyxRecentsActivity\$TabletManagerAdapter")
                .firstByNameOrLog("onPageCreateViewHolder")
                .createHook {
                    replace { param ->
                        val view = param.args[0].castNonNull<ViewGroup>().let { root ->
                            root.context.inflateLayoutByName(
                                root,
                                "recents_task_view_phone"
                            )
                        }

                        return@replace ConstructorFinder.fromClass("com.android.systemui.recents.OnyxRecentsActivity\$RecentItemViewHolder")
                            .filterByParamTypes(View::class.java)
                            .first()
                            .newInstance(view)
                    }
                }
        }
    }

    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        if (!preferences.get<Boolean>(R.string.key_recents_grid_custom_size))
            return

        // Set the space size between recent items
        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "integer",
            "onyx_recent_item_space_count",
            preferences.getStringAsInt(R.string.key_recents_grid_spacing)
        )
    }


    private fun Method.replaceSizeByOrientation(portraitValue: Int, landscapeValue: Int) {
        createHook {
            replace { param ->
                return@replace param.thisObject.javaClass
                    .methodFinder()
                    .firstByNameOrLog("isPortrait")
                    .invoke(param.thisObject)
                    .castNonNull<Boolean>()
                    .let { portrait -> if (portrait) portraitValue else landscapeValue }
            }
        }
    }
}