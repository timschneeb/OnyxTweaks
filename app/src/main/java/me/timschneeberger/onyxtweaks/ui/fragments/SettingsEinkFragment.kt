package me.timschneeberger.onyxtweaks.ui.fragments

import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@PreferenceGroup(PreferenceGroups.EINK)
class SettingsEinkFragment : SettingsBaseFragment() {
    override fun onPreferenceChanged(key: String) {
        when (key) {
            getString(R.string.key_eink_center_always_show_regal_mode) -> {
                requestPackageRestart(SYSTEM_UI_PACKAGE)
            }
        }
    }
}