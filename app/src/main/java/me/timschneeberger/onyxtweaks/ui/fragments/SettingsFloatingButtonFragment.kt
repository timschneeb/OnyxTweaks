package me.timschneeberger.onyxtweaks.ui.fragments

import me.timschneeberger.onyxtweaks.mods.Constants.FLOATING_BUTTON_PACKAGE
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups


@PreferenceGroup(PreferenceGroups.FLOATING_BUTTON)
class SettingsFloatingButtonFragment : SettingsBaseFragment<SettingsActivity>() {
    override fun onPreferenceChanged(key: String) {
        super.onPreferenceChanged(key)
        requestPackageRestart(FLOATING_BUTTON_PACKAGE)
    }
}