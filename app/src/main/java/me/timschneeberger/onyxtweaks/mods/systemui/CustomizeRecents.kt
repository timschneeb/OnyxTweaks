package me.timschneeberger.onyxtweaks.mods.systemui

import android.view.View
import android.view.ViewGroup
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.castNonNull
import me.timschneeberger.onyxtweaks.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.getClass
import me.timschneeberger.onyxtweaks.utils.inflateLayoutByName
import java.lang.reflect.Method

@TargetPackages(SYSTEM_UI_PACKAGE)
class CustomizeRecents : ModPack() {
    override val group = PreferenceGroups.RECENTS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {


        getClass("com.android.systemui.recents.OnyxRecentsActivity").apply {
            methodFinder()
                .firstByName("getRow")
                .replaceSizeByOrientation(5, 8)

            methodFinder()
                .firstByName("getColumn")
                .replaceSizeByOrientation(5, 8)
        }

        MethodFinder.fromClass("com.android.systemui.recents.OnyxRecentsActivity\$TabletManagerAdapter")
            .firstByName("onPageCreateViewHolder")
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

    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        // Set the space size between recent items
        param.res.setReplacement(
            SYSTEM_UI_PACKAGE,
            "integer",
            "onyx_recent_item_space_count",
            30
        )
    }


    private fun Method.replaceSizeByOrientation(portraitValue: Int, landscapeValue: Int) {
        createHook {
            replace { param ->
                return@replace param.thisObject.javaClass
                    .methodFinder()
                    .firstByName("isPortrait")
                    .invoke(param.thisObject)
                    .castNonNull<Boolean>()
                    .let { portrait -> if (portrait) portraitValue else landscapeValue }
            }
        }
    }
}