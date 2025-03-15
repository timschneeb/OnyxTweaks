package me.timschneeberger.onyxtweaks.ui.fragments

import android.os.Bundle
import me.timschneeberger.onyxtweaks.R


class SettingsFragment : SettingsBaseFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_preferences, rootKey)
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}