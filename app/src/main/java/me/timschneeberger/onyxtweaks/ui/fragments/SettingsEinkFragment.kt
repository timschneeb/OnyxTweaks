package me.timschneeberger.onyxtweaks.ui.fragments

import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.activities.BasePreferenceActivity.Companion.ZYGOTE_MARKER
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@PreferenceGroup(PreferenceGroups.EINK)
class SettingsEinkFragment : SettingsBaseFragment<SettingsActivity>() {
    override fun onPreferenceChanged(key: String) {
        super.onPreferenceChanged(key)
        when (key) {
            getString(R.string.key_eink_center_always_allow_eac) -> {
                requestPackageRestart(ZYGOTE_MARKER)
            }
        }
    }
}