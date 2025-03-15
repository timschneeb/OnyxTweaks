package me.timschneeberger.onyxtweaks.ui.fragments

import android.os.Bundle
import androidx.preference.Preference
import me.timschneeberger.onyxtweaks.R


class SettingsEinkFragment : SettingsBaseFragment() {

    private val version by lazy { findPreference<Preference>(getString(R.string.key_credits_version)) }
    private val buildInfo by lazy { findPreference<Preference>(getString(R.string.key_credits_build_info)) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_eink_opt_preferences, rootKey)
    }

    companion object {
        fun newInstance(): SettingsEinkFragment {
            return SettingsEinkFragment()
        }
    }
}