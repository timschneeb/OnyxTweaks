package me.timschneeberger.onyxtweaks.ui.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.databinding.ActivitySettingsBinding
import me.timschneeberger.onyxtweaks.ui.fragments.SettingsFragment
import me.timschneeberger.onyxtweaks.utils.restartLauncher
import me.timschneeberger.onyxtweaks.utils.restartSystemUi
import me.timschneeberger.onyxtweaks.utils.restartZygote

class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.settingsToolbar)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.settings,
                    SettingsFragment().apply {
                        setTargetFragment(null, 0)
                    }
                )
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
            val isRoot = supportFragmentManager.backStackEntryCount == 0
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(!isRoot)

                if (isRoot) {
                    title = getString(R.string.app_name)
                    subtitle = getString(R.string.module_description)
                }
                else {
                    val topFragment = supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1)
                    title = topFragment.name
                    subtitle = topFragment.breadCrumbShortTitle
                }
            }
        }

        binding.settingsToolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_restart, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.item_restart_launcher -> {
            restartLauncher()
            true
        }
        R.id.item_restart_system_ui -> {
            restartSystemUi()
            true
        }
        R.id.item_restart_zygote -> {
            restartZygote()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(PERSIST_TITLE, supportActionBar?.title.toString())
        outState.putString(PERSIST_SUBTITLE, supportActionBar?.subtitle.toString())
        super.onSaveInstanceState(outState)
    }

    @Suppress("DEPRECATION")
    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        // Instantiate the new Fragment
        pref.fragment?.let {
            supportFragmentManager.fragmentFactory.instantiate(classLoader, it)
        }?.apply {
            arguments = pref.extras
            setTargetFragment(caller, 0)
        }?.let {
            // Replace the existing Fragment with the new Fragment
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.settings, it)
                .setBreadCrumbShortTitle(pref.summary)
                .addToBackStack(pref.title?.toString() ?: "")
                .commit()
        }

        return true
    }

    companion object {
        private const val PERSIST_TITLE = "title"
        private const val PERSIST_SUBTITLE = "subtitle"
    }
}

