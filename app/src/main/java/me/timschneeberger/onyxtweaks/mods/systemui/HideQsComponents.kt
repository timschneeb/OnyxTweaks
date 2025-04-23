package me.timschneeberger.onyxtweaks.mods.systemui

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.findViewByIdName
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mod_processor.TargetPackages
import me.timschneeberger.onyxtweaks.mods.Constants
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.dpToPx
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.getDimensionPxByName
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.cast
import me.timschneeberger.onyxtweaks.utils.castNonNull

/**
 * This mod pack hides the volume slider, fixed tiles (wifi & bluetooth), tile grid,
 * brightness and temperature sliders, and the front-light label above the sliders.
 */
@TargetPackages(Constants.SYSTEM_UI_PACKAGE)
class HideQsComponents : ModPack() {
    override val group = PreferenceGroups.QS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        val hideVolume = preferences.get<Boolean>(R.string.key_qs_sections_hide_volume)
        val hideFixedTiles = preferences.get<Boolean>(R.string.key_qs_sections_hide_fixed_tiles)
        val hideTiles = preferences.get<Boolean>(R.string.key_qs_sections_hide_tiles)
        val hideBrightness = preferences.get<Boolean>(R.string.key_qs_sections_hide_brightness_slider)
        val hideTemperature = preferences.get<Boolean>(R.string.key_qs_sections_hide_temperature_slider)
        val hideFrontLightLabel = preferences.get<Boolean>(R.string.key_qs_sections_hide_frontlight_label)
        if (!hideVolume && !hideFixedTiles && !hideTiles && !hideBrightness && !hideTemperature && !hideFrontLightLabel) {
            return
        }

        findClass("com.android.systemui.qs.QSPanel")
            .methodFinder()
            .firstByName("initialize")
            .createAfterHookCatching<HideQsComponents> { param ->
                val root = param.thisObject.objectHelper()
                    .getObjectOrNull("controlCenterView")
                    .castNonNull<View>()

                // Hide the volume slider
                if (hideVolume) {
                    param.thisObject.objectHelper().getObjectOrNull("mVolumeView")
                        .castNonNull<View>()
                        .isVisible = false
                }

                if (hideFixedTiles) {
                    param.thisObject.objectHelper().getObjectOrNull("mFixedTileLayout")
                        .castNonNull<View>()
                        .isVisible = false
                }

                val shouldHideFrontLightLabel = hideFrontLightLabel || (hideBrightness && hideTemperature)

                // Hide the tile grid when tiles are hidden or its container when volume is also hidden
                param.thisObject.objectHelper().getObjectOrNull("mTileLayout")
                    .castNonNull<View>()
                    .let { tileGrid ->
                        if (hideTiles)
                            tileGrid.isVisible = false
                        if (hideTiles && hideVolume) {
                            (tileGrid.parent as View).isVisible = false
                        }

                        // The label has a top margin. To compensate for this, we need to set the bottom margin of the tile grid
                        if (shouldHideFrontLightLabel) {
                            ((tileGrid.parent as View).layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                                appContext.resources.getDimensionPxByName("tablet_qs_panel_display_title_padding_top")
                                    ?: appContext.dpToPx(16)
                        }
                    }

                // Hide the entire container when both brightness and temperature are hidden
                if (hideBrightness && hideTemperature)
                    root.findViewByIdName("tablet_brightness_area")!!.isVisible = false

                if (shouldHideFrontLightLabel) {
                    root.findViewByIdName("brightness_title_view")!!.isVisible = false
                }

                // Hide the brightness and temperature sliders
                if (hideBrightness)
                    root.findViewByIdName("cold_panel")!!.isVisible = false
                if (hideTemperature)
                    root.findViewByIdName("warm_panel")!!.isVisible = false
            }

        // Hide the tile edit button when tiles are hidden
        if (hideTiles) {
            // Visibilities of header buttons are updated in requestLayout()
            findClass("com.android.systemui.qs.QSPanel")
                .methodFinder()
                .firstByName("requestLayout")
                .createAfterHookCatching<HideQsComponents> { param ->
                    // TileEdit may be null here
                    param.thisObject.objectHelper()
                        .getObjectOrNull("tileEdit")
                        .cast<View>()
                        ?.isVisible = false
                }
        }
    }
}