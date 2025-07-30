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
import me.timschneeberger.onyxtweaks.mods.utils.findViewByIdName
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.mods.utils.getDimensionPxByName
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Version.Companion.toVersion
import me.timschneeberger.onyxtweaks.utils.cast
import me.timschneeberger.onyxtweaks.utils.castNonNull
import me.timschneeberger.onyxtweaks.utils.onyxVersion

/**
 * This mod pack hides the volume slider, fixed tiles (wifi & bluetooth), tile grid,
 * brightness and temperature sliders, and the front-light label above the sliders.
 */
@TargetPackages(Constants.SYSTEM_UI_PACKAGE)
class HideQsComponents : ModPack() {
    override val group = PreferenceGroups.QS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        val hasNewBrightnessUi = onyxVersion >= "4.1".toVersion()

        val hideVolume = preferences.get<Boolean>(R.string.key_qs_sections_hide_volume)
        val hideFixedTiles = preferences.get<Boolean>(R.string.key_qs_sections_hide_fixed_tiles)
        val hideTiles = preferences.get<Boolean>(R.string.key_qs_sections_hide_tiles)
        val hideBrightness = preferences.get<Boolean>(R.string.key_qs_sections_hide_brightness_slider)
        val hideTemperature = preferences.get<Boolean>(R.string.key_qs_sections_hide_temperature_slider)
        val hideFrontLightLabel = preferences.get<Boolean>(R.string.key_qs_sections_hide_frontlight_label)
        // For v4.1+ only:
        val hidePenContainer = preferences.get<Boolean>(R.string.key_qs_sections_hide_pen_container)
        val hideFrontLightPresets = preferences.get<Boolean>(R.string.key_qs_sections_hide_frontlight_presets)

        if (!hideVolume && !hideFixedTiles && !hideTiles && !hideFrontLightPresets &&
            !hideBrightness && !hideTemperature && !hideFrontLightLabel && !hidePenContainer) {
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

                val shouldHideFrontLightLabel = hideFrontLightLabel ||
                            (hideBrightness && hideTemperature && (hideFrontLightPresets || !hasNewBrightnessUi))

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
                if (hideBrightness && hideTemperature && (hideFrontLightPresets || !hasNewBrightnessUi))
                    root.findViewByIdName("tablet_brightness_area")!!.isVisible = false

                if (shouldHideFrontLightLabel) {
                    val labelView = root.findViewByIdName("brightness_title_view")!!
                    // On v4.1+, the label is in a layout with a 'More settings' button which also needs to be hidden.
                    if (!hasNewBrightnessUi)
                        labelView.isVisible = false
                    else {
                        (labelView.parent as View).isVisible = false
                    }
                }

                if (hasNewBrightnessUi && hidePenContainer) {
                    // v4.1+ only. Hide the pen container.
                    // Causes excessive padding when the items inside are hidden, so hide entire the container.
                    root.findViewByIdName("pen_container")!!.isVisible = false
                }

                if (hasNewBrightnessUi && hideFrontLightPresets) {
                    // v4.1+ only. Hide the presets
                    root.findViewByIdName("brightness_style_area")!!.isVisible = false
                    // Remove top padding from the item below
                    val nextItem = if (!hideBrightness)
                        root.findViewByIdName("brightness_bright_panel")!!.layoutParams as ViewGroup.MarginLayoutParams
                    else if (!hideTemperature)
                        root.findViewByIdName<ViewGroup>("warm_panel")!!.layoutParams as ViewGroup.MarginLayoutParams
                    else null

                    nextItem?.topMargin = 0
                }

                // Hide the brightness and temperature sliders
                if (hideBrightness) {
                    // cold_panel was present in v4.0, brightness_bright_panel in v4.1+
                    val oldPanel = root.findViewByIdName("cold_panel")
                    val newPanel = root.findViewByIdName("brightness_bright_panel")

                    if(oldPanel == null && newPanel == null) {
                        throw IllegalStateException("Failed to find brightness panel view")
                    }

                    oldPanel?.isVisible = false
                    newPanel?.isVisible = false
                }
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