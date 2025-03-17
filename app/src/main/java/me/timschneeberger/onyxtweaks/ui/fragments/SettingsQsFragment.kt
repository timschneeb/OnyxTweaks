package me.timschneeberger.onyxtweaks.ui.fragments

import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@PreferenceGroup(PreferenceGroups.QS)
class SettingsQsFragment : SettingsBaseFragment() {
    override fun onPreferenceChanged(key: String) {
        requestPackageRestart(SYSTEM_UI_PACKAGE)
    }
}