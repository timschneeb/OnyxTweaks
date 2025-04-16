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
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.createReplaceHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.dpToPx
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.getDimensionPxByName
import me.timschneeberger.onyxtweaks.mods.utils.getDrawableByName
import me.timschneeberger.onyxtweaks.mods.utils.replaceWithConstant
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@TargetPackages(Constants.SYSTEM_UI_PACKAGE)
class InjectMediaHostIntoQs : ModPack() {
    override val group = PreferenceGroups.QS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        if (!preferences.get<Boolean>(R.string.key_qs_media_host_inject))
            return

        findClass("com.android.systemui.util.Utils")
            .methodFinder()
            .firstByName("useQsMediaPlayer")
            .replaceWithConstant(true)

        findClass("com.android.systemui.qs.QSPanel")
            .methodFinder()
            .firstByName("setUsingHorizontalLayout")
            .createAfterHookCatching<InjectMediaHostIntoQs> { param ->
                // unused: val shouldUseHorizontalLayout = param.args[0] as Boolean
                val viewGroup = param.args[1] as ViewGroup
                val force = param.args[2] as Boolean

                // Only run initially; we don't support the alternative horizontal layout,
                // so we don't need to check for any later configuration changes
                if (!force)
                    return@createAfterHookCatching

                val qsPanel = param.thisObject as ViewGroup
                val parent = viewGroup.parent as ViewGroup?

                // Don't move if the view is already in the correct parent
                if (parent == qsPanel)
                    return@createAfterHookCatching

                val marginHoriz = appContext.resources.getDimensionPxByName("control_center_margin_horizontal") ?: appContext.dpToPx(21)

                parent?.removeView(viewGroup)
                qsPanel.addView(viewGroup)
                (viewGroup.layoutParams as LinearLayout.LayoutParams).apply {
                    height = -2
                    width = -1
                    weight = 0.0f
                    bottomMargin = marginHoriz // intentionally re-used for bottom margin
                    marginStart = marginHoriz
                    marginEnd = marginHoriz
                }
            }

        // Replace background with Onyx section border & remove tint
        findClass("com.android.systemui.media.PlayerViewHolder")
            .constructorFinder()
            .filterByParamTypes(View::class.java)
            .first()
            .createAfterHookCatching<InjectMediaHostIntoQs> { param ->
                val player = param.thisObject.objectHelper().getObjectOrNull("player") as? View?
                if (player == null) {
                    Log.ex("Player is null")
                }
                else {
                    player.background = appContext.resources.getDrawableByName("tablet_rounded_border")
                    player.backgroundTintList = null
                }
            }

        if (preferences.get<String>(R.string.key_qs_media_host_state) != "default") {
            findClass("com.android.systemui.media.MediaViewController")
                .methodFinder()
                .firstByName("constraintSetForExpansion")
                .createAfterHookCatching<InjectMediaHostIntoQs> { param ->
                    when(preferences.get<String>(R.string.key_qs_media_host_state)) {
                        // Translate array value to field name containing the layout
                        "expanded" -> "expandedLayout"
                        "collapsed" -> "collapsedLayout"
                        else -> {
                            Log.ex("Unknown state")
                            return@createAfterHookCatching
                        }
                    }.let {
                        param.result = param.thisObject.objectHelper().getObjectOrNull(it)
                    }
                }
        }

        findClass("com.android.systemui.media.MediaHost")
            .methodFinder()
            .firstByName("getHostView")
            .createReplaceHookCatching<InjectMediaHostIntoQs> { param ->
                // Workaround: lateinit property hostView is sometimes not yet initialized,
                //             so we create a dummy object if it's called too early
                return@createReplaceHookCatching param.thisObject.objectHelper().getObjectOrNull("hostView")
                    ?: findClass("com.android.systemui.util.animation.UniqueObjectHostView")
                        .constructorFinder()
                        .filterByParamTypes(Context::class.java)
                        .first()
                        .newInstance(appContext)
            }
    }
}