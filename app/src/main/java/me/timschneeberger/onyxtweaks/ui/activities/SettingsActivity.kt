package me.timschneeberger.onyxtweaks.ui.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.databinding.ActivitySettingsBinding
import me.timschneeberger.onyxtweaks.ui.fragments.SettingsFragment
import me.timschneeberger.onyxtweaks.ui.utils.getViewsByType
import me.timschneeberger.onyxtweaks.utils.DumpTools
import me.timschneeberger.onyxtweaks.utils.cast
import me.timschneeberger.onyxtweaks.utils.restartLauncher
import me.timschneeberger.onyxtweaks.utils.restartSystemUi
import me.timschneeberger.onyxtweaks.utils.restartZygote

class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private lateinit var binding: ActivitySettingsBinding

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.settingsToolbar)

        if (savedInstanceState == null) {
            setBottomBarVisibility(false)
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
            val isRoot = supportFragmentManager.backStackEntryCount == 0

            supportActionBar?.apply {
                title = savedInstanceState.getString(PERSIST_TITLE)
                subtitle = savedInstanceState.getString(PERSIST_SUBTITLE)
                setDisplayHomeAsUpEnabled(!isRoot)
                setBottomBarVisibility(!isRoot)
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val isRoot = supportFragmentManager.backStackEntryCount == 0
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(!isRoot)
                setBottomBarVisibility(!isRoot)

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
        binding.scrollUp.setOnClickListener(::onScrollButtonClicked)
        binding.scrollDown.setOnClickListener(::onScrollButtonClicked)
    }

    private fun onScrollButtonClicked(view: View) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            binding.scrollButtonGroup.clearChecked()
        }

        supportFragmentManager.findFragmentById(R.id.settings)?.let {

            DumpTools.dumpIDs(it.requireView())
            it.requireView()
                .cast<ViewGroup>()
                ?.getViewsByType(RecyclerView::class)
                ?.firstOrNull()
                ?.let {
                    when (view.id) {
                        R.id.scrollUp -> it.scrollBy(0, -it.height)
                        R.id.scrollDown -> it.scrollBy(0, it.height)
                    }
                }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_global, menu)
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

    private fun setBottomBarVisibility(isVisible: Boolean) {
        binding.bottomDivider.isVisible = isVisible
        binding.bottomAppBar.isVisible = isVisible
    }

    companion object {
        private const val PERSIST_TITLE = "title"
        private const val PERSIST_SUBTITLE = "subtitle"
    }
}

