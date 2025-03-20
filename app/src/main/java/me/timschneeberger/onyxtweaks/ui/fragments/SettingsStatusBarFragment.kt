package me.timschneeberger.onyxtweaks.ui.fragments

import androidx.preference.EditTextPreference
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups

@PreferenceGroup(PreferenceGroups.STATUS_BAR)
class SettingsStatusBarFragment : SettingsBaseFragment<SettingsActivity>() {
    private val maxNotificationIcons by lazy { findPreference<EditTextPreference>(getString(R.string.key_status_bar_status_icons_max_notification_icons)) }

    override fun onConfigurePreferences() {
        maxNotificationIcons?.configureAsNumberInput(0, 10, R.plurals.unit_icons)
    }

    override fun onPreferenceChanged(key: String) {
        requestPackageRestart(SYSTEM_UI_PACKAGE)
    }
}