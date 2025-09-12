package me.timschneeberger.onyxtweaks.ui.fragments

import androidx.preference.Preference
import com.topjohnwu.superuser.Shell
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.OTA_SERVICE_PACKAGE
import me.timschneeberger.onyxtweaks.ui.activities.SettingsActivity
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.utils.showAlert
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups


@PreferenceGroup(PreferenceGroups.MISC)
class SettingsMiscFragment : SettingsBaseFragment<SettingsActivity>() {
    private val otaTips by lazy { findPreference<Preference>(getString(R.string.key_misc_ota_tips)) }
    private val shareLogs by lazy { findPreference<Preference>(getString(R.string.key_misc_share_debug_logs)) }

    override fun onConfigurePreferences() {
        shareLogs?.setOnPreferenceClickListener {
            Shell.getShell { shell ->
                shell.newJob().add(
                    "am start-activity -a android.intent.action.MAIN -p com.android.shell -n com.android.shell/.BugreportWarningActivity " +
                            "-c org.lsposed.manager.LAUNCH_MANAGER -d logs"
                ).exec()
            }
            true
        }

        otaTips?.setOnPreferenceClickListener {
            requireContext().showAlert(
                title = null,
                message = R.string.misc_ota_tips_text,
                html = true
            )
            true
        }
    }

    override fun onPreferenceChanged(key: String) {
        super.onPreferenceChanged(key)

        if (key == getString(R.string.key_misc_skip_reboot_after_ota)) {
            requestPackageRestart(OTA_SERVICE_PACKAGE)
        }
    }
}