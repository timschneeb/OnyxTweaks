package me.timschneeberger.onyxtweaks.ui.fragments

import androidx.annotation.StringRes
import androidx.preference.EditTextPreference
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.preferences.MaterialSwitchPreference
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Version.Companion.toVersion
import me.timschneeberger.onyxtweaks.utils.onyxVersion

@PreferenceGroup(PreferenceGroups.STATUS_BAR)
class SettingsStatusBarFragment : SettingsBaseFragment<SettingsActivity>() {
    private val maxNotificationIcons by lazy { findPreference<EditTextPreference>(getString(R.string.key_status_bar_status_icons_max_notification_icons)) }

    override fun onConfigurePreferences() {
        if (onyxVersion >= "4.1".toVersion()) {
            disableDeprecatedSwitchPreference(R.string.key_status_bar_status_icons_show_refresh_mode)
        }

        maxNotificationIcons?.configureAsNumberInput(0, 10, R.plurals.unit_icons)
    }

    override fun onPreferenceChanged(key: String) {
        super.onPreferenceChanged(key)
        requestPackageRestart(SYSTEM_UI_PACKAGE)
    }

    private fun disableDeprecatedSwitchPreference(@StringRes key: Int) {
        findPreference<MaterialSwitchPreference>(getString(key))?.apply {
            isEnabled = false
            isChecked = false
            summary = getString(R.string.deprecated_after_fw_4_0)
        }
    }
}