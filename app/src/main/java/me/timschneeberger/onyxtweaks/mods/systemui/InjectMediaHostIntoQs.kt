package me.timschneeberger.onyxtweaks.mods.systemui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.mods.Constants
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.dpToPx
import me.timschneeberger.onyxtweaks.mods.utils.dumpIDs
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(Constants.SYSTEM_UI_PACKAGE)
class InjectMediaHostIntoQs : ModPack() {
    override val group = PreferenceGroups.STATUS_BAR

    fun moveToParent(view: View, newParent: ViewGroup?, position: Int) {
        if (newParent == null) {
            Log.ex("Trying to move view to null parent")
            return
        }
        val parent = view.parent as ViewGroup?
        if (parent != newParent) {
            parent?.removeView(view)
            newParent.addView(view, position)
        } else {
            if (newParent.indexOfChild(view) == position) {
                return
            }
            newParent.removeView(view)
            newParent.addView(view, position)
        }
    }

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        return // TODO remove

        // TODO: Fix seekbar not updating properly
        //       Fix QS layout getting messed up
        //       Better style for e-ink displays

        findClass("com.android.systemui.qs.QSPanel")
            .methodFinder()
            .firstByName("setUsingHorizontalLayout")
            .createAfterHookCatching<InjectMediaHostIntoQs> { param ->
                // unused: val shouldUseHorizontalLayout = param.args[0] as Boolean
                val viewGroup = param.args[1] as ViewGroup
                val force = param.args[2] as Boolean

                Log.ex("QSPanel - setUsingHorizontalLayout: force=$force")

                // Only move initially; we don't support the alternative horizontal layout,
                // so we don't need to check for
                if (!force)
                    return@createAfterHookCatching

                val qsPanel = param.thisObject as ViewGroup

                findClass("com.android.systemui.qs.QSPanel")
                    .methodFinder()
                    .firstByName("getFixedTileLayout")
                    .invoke(param.thisObject)
                    ?.let { tileLayout ->
                        moveToParent(tileLayout as ViewGroup, qsPanel, 0)
                    }

                val parent = viewGroup.parent as ViewGroup?
                if (parent != qsPanel) {
                    parent?.removeView(viewGroup)
                    qsPanel.addView(viewGroup)
                    (viewGroup.layoutParams as LinearLayout.LayoutParams).apply {
                        height = -2
                        width = -1
                        weight = 0.0f
                        bottomMargin = appContext.dpToPx(8)
                        marginStart = appContext.dpToPx(8)
                        marginEnd = appContext.dpToPx(8)
                    }
                }

                qsPanel.dumpIDs()
            }

        findClass("com.android.systemui.util.Utils")
            .methodFinder()
            .firstByName("useQsMediaPlayer")
            .replaceWithConstant(true)

        findClass("com.android.systemui.media.MediaViewController")
            .methodFinder()
            .firstByName("constraintSetForExpansion")
            .createAfterHookCatching<InjectMediaHostIntoQs> { param ->
                param.result = param.thisObject.objectHelper().getObjectOrNull("expandedLayout")
            }

        findClass("com.android.systemui.media.MediaHost")
            .methodFinder()
            .firstByName("getHostView")
            .createReplaceHookCatching<InjectMediaHostIntoQs> { param ->
                // Workaround: lateinit property hostView is sometimes not yet initialized,
                //             so we create a dummy object if called too early
                return@createReplaceHookCatching param.thisObject.objectHelper().getObjectOrNull("hostView")
                    ?: findClass("com.android.systemui.util.animation.UniqueObjectHostView")
                        .constructorFinder()
                        .filterByParamTypes(Context::class.java)
                        .first()
                        .newInstance(appContext)
            }
    }

    override fun handleInitPackageResources(param: XC_InitPackageResources.InitPackageResourcesParam) {
        param.res.setReplacement(
            "com.android.systemui",
            "bool",
            "config_use_split_notification_shade",
            true
        )
    }
}