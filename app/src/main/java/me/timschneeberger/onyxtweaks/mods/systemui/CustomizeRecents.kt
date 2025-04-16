package me.timschneeberger.onyxtweaks.mods.systemui

import android.view.View
import android.view.ViewGroup
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createBeforeHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.inflateLayoutByName
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.castNonNull
import java.lang.reflect.Method

@TargetPackages(SYSTEM_UI_PACKAGE)
class CustomizeRecents : ModPack() {
    override val group = PreferenceGroups.RECENTS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (preferences.get<Boolean>(R.string.key_recents_grid_custom_size)) {
            findClass("com.android.systemui.recents.OnyxRecentsActivity").apply {
                methodFinder()
                    .firstByName("getRow")
                    .replaceSizeByOrientation(
                        preferences.getStringAsInt(R.string.key_recents_grid_row_count_portrait),
                        preferences.getStringAsInt(R.string.key_recents_grid_row_count_landscape)
                    )

                methodFinder()
                    .firstByName("getColumn")
                    .replaceSizeByOrientation(
                        preferences.getStringAsInt(R.string.key_recents_grid_column_count_portrait),
                        preferences.getStringAsInt(R.string.key_recents_grid_column_count_landscape)
                    )
            }

            findClass("com.android.systemui.recents.views.SpaceItemDecoration")
                .constructorFinder()
                .filterByParamTypes(Int::class.java)
                .first()
                .createBeforeHookCatching<CustomizeRecents> { param ->
                    param.args[0] = preferences.getStringAsInt(R.string.key_recents_grid_spacing)
                }
        }

        if (preferences.get<Boolean>(R.string.key_recents_use_stock_header)) {
            MethodFinder.fromClass("com.android.systemui.recents.OnyxRecentsActivity\$TabletManagerAdapter")
                .firstByName("onPageCreateViewHolder")
                .createReplaceHookCatching<CustomizeRecents> hook@ { param ->
                    val view = param.args[0].castNonNull<ViewGroup>().let { root ->
                        root.context.inflateLayoutByName(
                            root,
                            "recents_task_view_phone"
                        )
                    }

                    return@hook ConstructorFinder
                        .fromClass("com.android.systemui.recents.OnyxRecentsActivity\$RecentItemViewHolder")
                        .filterByParamTypes(View::class.java)
                        .first()
                        .newInstance(view)
                }
        }
    }

    private fun Method.replaceSizeByOrientation(portraitValue: Int, landscapeValue: Int) {
        createReplaceHookCatching<CustomizeRecents> { param ->
            param
                .thisObject.javaClass
                .methodFinder()
                .firstByName("isPortrait")
                .invoke(param.thisObject)
                .castNonNull<Boolean>()
                .let { portrait -> if (portrait) portraitValue else landscapeValue }
        }
    }
}