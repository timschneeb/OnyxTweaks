package me.timschneeberger.onyxtweaks.ui.activities

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.kyuubiran.ezxhelper.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.bridge.ModEventReceiver
import me.timschneeberger.onyxtweaks.bridge.OnModEventReceived
import me.timschneeberger.onyxtweaks.bridge.registerModEventReceiver
import me.timschneeberger.onyxtweaks.bridge.unregisterModEventReceiver
import me.timschneeberger.onyxtweaks.databinding.ActivitySettingsBinding
import me.timschneeberger.onyxtweaks.mods.Constants.FLOATING_BUTTON_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_SETTINGS_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.mods.ModRegistry
import me.timschneeberger.onyxtweaks.mods.ModRegistryExtensions.testedAndroidVersions
import me.timschneeberger.onyxtweaks.mods.ModRegistryExtensions.testedModels
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.getAppName
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.restartLauncher
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.restartPackage
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.restartSettings
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.restartSystemUi
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.restartZygote
import me.timschneeberger.onyxtweaks.ui.utils.getViewsByType
import me.timschneeberger.onyxtweaks.utils.cast

abstract class BasePreferenceActivity() : AppCompatActivity(), OnModEventReceived,
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var binding: ActivitySettingsBinding
    private val modifiedPackages = mutableSetOf<String>()

    override var modEventReceiver: ModEventReceiver? = null

    abstract fun createRootFragment(): Fragment
    @get:StringRes
    abstract val rootTitleRes: Int
    @get:StringRes
    abstract val rootSubtitleRes: Int
    abstract val rootIsSubActivity: Boolean

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerModEventReceiver(this)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.settingsToolbar)

        if (savedInstanceState == null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(rootIsSubActivity)
            updateBottomBarState(rootIsSubActivity)
            updateCompatibilityStatusBarState(!rootIsSubActivity)

            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.settings,
                    createRootFragment().apply {
                        setTargetFragment(null, 0)
                    }
                )
                .commit()
        } else {
            val isRoot = supportFragmentManager.backStackEntryCount == 0

            supportActionBar?.apply {
                title = savedInstanceState.getString(PERSIST_TITLE)
                subtitle = savedInstanceState.getString(PERSIST_SUBTITLE)
                val isRoot = isRoot || !rootIsSubActivity
                setDisplayHomeAsUpEnabled(!isRoot || rootIsSubActivity)
                updateBottomBarState(!isRoot || rootIsSubActivity)
                updateCompatibilityStatusBarState(isRoot && !rootIsSubActivity)
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val isRoot = supportFragmentManager.backStackEntryCount == 0
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(!isRoot || rootIsSubActivity)
                updateBottomBarState(!isRoot || rootIsSubActivity)
                updateCompatibilityStatusBarState(isRoot && !rootIsSubActivity)

                if (isRoot) {
                    title = getString(rootTitleRes)
                    subtitle = getString(rootSubtitleRes)
                } else {
                    val topFragment =
                        supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1)
                    title = topFragment.name
                    subtitle = topFragment.breadCrumbShortTitle
                }
            }
        }

        binding.settingsToolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.scrollUp.setOnClickListener(::onScrollButtonClicked)
        binding.scrollDown.setOnClickListener(::onScrollButtonClicked)
        binding.statusBar.setOnClickListener { onPackageRestartConfirmed() }
        binding.statusClose.setOnClickListener {
            modifiedPackages.clear()
            updateStatusPanel()
        }

        updateStatusPanel()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterModEventReceiver(this)
    }

    private fun onScrollButtonClicked(view: View) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            binding.scrollButtonGroup.clearChecked()
        }

        supportFragmentManager.findFragmentById(R.id.settings)?.let {
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
            modifiedPackages.remove(LAUNCHER_PACKAGE)
            updateStatusPanel()
            true
        }
        R.id.item_restart_system_ui -> {
            restartSystemUi()
            modifiedPackages.remove(SYSTEM_UI_PACKAGE)
            updateStatusPanel()
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
            navigateToFragment(it, pref.title.toString(), pref.summary.toString())
        }

        return true
    }

    @Suppress("DEPRECATION")
    fun navigateToFragment(fragment: Fragment, title: String, subtitle: String) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.settings, fragment)
            .setBreadCrumbShortTitle(subtitle)
            .addToBackStack(title)
            .commit()
    }

    override fun onHookLoaded(packageName: String) {
        modifiedPackages.remove(packageName)
        updateStatusPanel()
    }

    override fun onRestartRequested(packageName: String) {
        requestPackageRestart(packageName)
    }

    fun requestPackageRestart(packageName: String) {
        Log.i("Marking package $packageName as modified")
        modifiedPackages.add(packageName)
        updateStatusPanel()
    }

    private fun updateBottomBarState(isVisible: Boolean) {
        binding.bottomDivider.isVisible = isVisible
        binding.bottomAppBar.isVisible = isVisible
    }

    private fun updateCompatibilityStatusBarState(isVisible: Boolean) {
        val message = when {
            !ModRegistry.testedModels.contains(Build.MODEL) -> getString(R.string.compat_model_not_tested)
            !ModRegistry.testedAndroidVersions.contains(Build.VERSION.SDK_INT) -> getString(R.string.compat_android_version_not_tested)
            else -> null
        }

        binding.bottomStatusBar.isVisible = isVisible && !message.isNullOrEmpty()
        binding.bottomStatusText.text = message
    }

    private fun onPackageRestartConfirmed() {
        when {
            modifiedPackages.contains(ZYGOTE_MARKER) -> restartZygote()
            else -> {
                // TODO This needs some clean up
                if (modifiedPackages.contains(SYSTEM_UI_PACKAGE))
                    restartSystemUi()
                if (modifiedPackages.contains(LAUNCHER_PACKAGE))
                    restartLauncher()
                if (modifiedPackages.contains(SYSTEM_SETTINGS_PACKAGE))
                    restartSettings()
                if (modifiedPackages.contains(FLOATING_BUTTON_PACKAGE))
                    restartPackage(FLOATING_BUTTON_PACKAGE)
            }
        }
        modifiedPackages.clear()
        updateStatusPanel()
    }

    private fun updateStatusPanel() {
        val text = if (modifiedPackages.contains(ZYGOTE_MARKER)) {
            getString(R.string.status_needs_zygote_reset)
        } else {
            val friendlyNameMap = mapOf(
                SYSTEM_UI_PACKAGE to getString(R.string.system_ui),
                LAUNCHER_PACKAGE to getString(R.string.launcher),
                SYSTEM_SETTINGS_PACKAGE to getString(R.string.settings)
            )

            val friendlyNames = modifiedPackages.map { packageName ->
                friendlyNameMap[packageName] ?: getAppName(packageName) ?: packageName
            }

            val param = if (friendlyNames.size == 2)
                getString(R.string.and_conjunction, friendlyNames[0], friendlyNames[1])
            else
                friendlyNames.joinToString(", ")

            resources.getQuantityString(R.plurals.apps_restart_required, friendlyNames.size, param)
        }

        modifiedPackages.isNotEmpty().let {
            binding.statusDivider.isVisible = it
            binding.statusBar.isVisible = it
            binding.statusText.text = text
        }
    }

    companion object {
        private const val PERSIST_TITLE = "title"
        private const val PERSIST_SUBTITLE = "subtitle"

        const val ZYGOTE_MARKER = "zygote"
    }
}