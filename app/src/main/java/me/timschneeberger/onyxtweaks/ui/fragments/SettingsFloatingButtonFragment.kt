package me.timschneeberger.onyxtweaks.ui.fragments

import androidx.preference.Preference
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.FLOATING_BUTTON_PACKAGE
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Version.Companion.toVersion
import me.timschneeberger.onyxtweaks.utils.onyxVersion


@PreferenceGroup(PreferenceGroups.FLOATING_BUTTON)
class SettingsFloatingButtonFragment : SettingsBaseFragment<SettingsActivity>() {
    override fun onPreferenceChanged(key: String) {
        super.onPreferenceChanged(key)
        requestPackageRestart(FLOATING_BUTTON_PACKAGE)
    }

    override fun onConfigurePreferences() {
        if (onyxVersion >= "4.2".toVersion()) {
            // BW mode does not work anymore on FW 4.2
            findPreference<Preference>(getString(R.string.key_floating_button_show_bw_function))?.apply {
                isEnabled = false
                isVisible = false
            }
        }
    }
}