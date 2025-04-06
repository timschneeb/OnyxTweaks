package me.timschneeberger.onyxtweaks.ui.fragments

import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_SETTINGS_PACKAGE
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups


@PreferenceGroup(PreferenceGroups.SYSTEM_SETTINGS)
class SettingsStockSettingsFragment : SettingsBaseFragment<SettingsActivity>() {
    override fun onPreferenceChanged(key: String) {
        super.onPreferenceChanged(key)
        requestPackageRestart(SYSTEM_SETTINGS_PACKAGE)
    }
}