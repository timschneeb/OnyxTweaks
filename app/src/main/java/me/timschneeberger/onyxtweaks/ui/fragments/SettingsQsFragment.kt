package me.timschneeberger.onyxtweaks.ui.fragments

import android.os.Bundle
import androidx.preference.Preference
import me.timschneeberger.onyxtweaks.R


class SettingsQsFragment : SettingsBaseFragment() {

    private val version by lazy { findPreference<Preference>(getString(R.string.key_credits_version)) }
    private val buildInfo by lazy { findPreference<Preference>(getString(R.string.key_credits_build_info)) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_qs_preferences, rootKey)
    }

    companion object {
        fun newInstance(): SettingsQsFragment {
            return SettingsQsFragment()
        }
    }
}