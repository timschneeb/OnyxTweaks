package me.timschneeberger.onyxtweaks.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.databinding.ActivitySettingsBinding
import me.timschneeberger.onyxtweaks.ui.fragments.SettingsAboutFragment
import me.timschneeberger.onyxtweaks.ui.fragments.SettingsFragment

class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.settingsToolbar)

        if (savedInstanceState == null) {
            val fragment = SettingsFragment.newInstance()
            @Suppress("DEPRECATION")
            fragment.setTargetFragment(null, 0)

            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, fragment)
                .commit()
        }
        else {
            supportActionBar?.apply {
                title = savedInstanceState.getString(PERSIST_TITLE)
                subtitle = savedInstanceState.getString(PERSIST_SUBTITLE)
                setDisplayHomeAsUpEnabled(true)
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                supportActionBar?.apply {
                    title = getString(R.string.app_name)
                    subtitle = getString(R.string.module_description)
                }
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }
            else {
                supportActionBar?.apply {
                    title = supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1).name
                    subtitle = null
                }
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
        }

        binding.settingsToolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private inline fun<reified T> accessFragment(onAccess: T.() -> Unit) {
        val fragment = supportFragmentManager.findFragmentById(R.id.settings)
        if(fragment is T)
            onAccess(fragment)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(PERSIST_TITLE, supportActionBar?.title.toString())
        super.onSaveInstanceState(outState)
    }

     override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = pref.fragment?.let {
            supportFragmentManager.fragmentFactory.instantiate(classLoader, it)
        }
        fragment ?: return false

        fragment.arguments = args
        @Suppress("DEPRECATION")
        fragment.setTargetFragment(caller, 0)

        // Set the action bar title; the about page doesn't need one
        val title = if(fragment is SettingsAboutFragment)
            ""
        else
            pref.title.toString()

        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.settings, fragment)
            .addToBackStack(title)
            .commit()
        return true
    }

    companion object {
        private const val PERSIST_TITLE = "title"
        private const val PERSIST_SUBTITLE = "subtitle"
    }
}

