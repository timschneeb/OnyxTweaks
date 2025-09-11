package me.timschneeberger.onyxtweaks.ui.fragments

import androidx.annotation.StringRes
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.activities.BasePreferenceActivity.Companion.ZYGOTE_MARKER
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.preferences.MaterialSwitchPreference
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Version.Companion.toVersion
import me.timschneeberger.onyxtweaks.utils.onyxVersion

@PreferenceGroup(PreferenceGroups.EINK)
class SettingsEinkFragment : SettingsBaseFragment<SettingsActivity>() {
    override fun onConfigurePreferences() {
        if (onyxVersion >= "4.1".toVersion()) {
            disableDeprecatedSwitchPreference(R.string.key_eink_center_always_allow_eac)
            disableDeprecatedSwitchPreference(R.string.key_eink_center_always_show_regal_mode)
        }
    }

    override fun onPreferenceChanged(key: String) {
        super.onPreferenceChanged(key)
        when (key) {
            getString(R.string.key_eink_center_always_allow_eac) -> {
                requestPackageRestart(ZYGOTE_MARKER)
            }
        }
    }

    private fun disableDeprecatedSwitchPreference(@StringRes key: Int) {
        findPreference<MaterialSwitchPreference>(getString(key))?.apply {
            isEnabled = false
            isChecked = false
            summary = getString(R.string.deprecated_after_fw_4_0)
        }
    }
}