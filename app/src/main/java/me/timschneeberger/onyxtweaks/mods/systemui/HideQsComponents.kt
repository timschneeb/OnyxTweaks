package me.timschneeberger.onyxtweaks.mods.systemui

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.github.kyuubiran.ezxhelper.misc.ViewUtils.findViewByIdName
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants
import me.timschneeberger.onyxtweaks.mods.base.ModPack
import me.timschneeberger.onyxtweaks.mods.base.TargetPackages
import me.timschneeberger.onyxtweaks.mods.utils.createAfterHookCatching
import me.timschneeberger.onyxtweaks.mods.utils.findClass
import me.timschneeberger.onyxtweaks.mods.utils.firstByName
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.castNonNull

@TargetPackages(Constants.SYSTEM_UI_PACKAGE)
class HideQsComponents : ModPack() {
    override val group = PreferenceGroups.QS

    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        val hideVolume = preferences.get<Boolean>(R.string.key_qs_sections_hide_volume)
        val hideTiles = preferences.get<Boolean>(R.string.key_qs_sections_hide_tiles)
        val hideBrightness = preferences.get<Boolean>(R.string.key_qs_sections_hide_brightness_slider)
        val hideTemperature = preferences.get<Boolean>(R.string.key_qs_sections_hide_temperature_slider)
        val hideFrontLightLabel = preferences.get<Boolean>(R.string.key_qs_sections_hide_frontlight_label)
        if (!hideVolume && !hideTiles && !hideBrightness && !hideTemperature && !hideFrontLightLabel) {
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

                // Hide the tile edit button when tiles are hidden
                if (hideTiles) {
                    param.thisObject.objectHelper().getObjectOrNull("tileEdit")
                        .castNonNull<View>()
                        .isVisible = false
                }

                // Hide the tile grid when tiles are hidden or its container when volume is also hidden
                if (hideTiles || hideVolume) {
                    param.thisObject.objectHelper().getObjectOrNull("mTileLayout")
                        .castNonNull<View>()
                        .let { tileGrid ->
                            if (hideTiles)
                                tileGrid.isVisible = false
                            if (hideTiles && hideVolume) {
                                (tileGrid.parent as View).isVisible = false
                            }
                        }
                }

                // Hide the entire container when both brightness and temperature are hidden
                if (hideBrightness && hideTemperature)
                    root.findViewByIdName("tablet_brightness_area")!!.isVisible = false
                // We need to preserve the label margins, so just set the height to 0
                if (hideFrontLightLabel || (hideBrightness && hideTemperature))
                    root.findViewByIdName("brightness_title_view")!!.castNonNull<TextView>().apply {
                        text = ""
                        layoutParams.height = 0
                    }

                // Hide the brightness and temperature sliders
                if (hideBrightness)
                    root.findViewByIdName("cold_panel")!!.isVisible = false
                if (hideTemperature)
                    root.findViewByIdName("warm_panel")!!.isVisible = false
            }
    }
}