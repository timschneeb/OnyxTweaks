package me.timschneeberger.onyxtweaks.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.preference.PreferenceFragmentCompat
import com.github.kyuubiran.ezxhelper.Log
import com.google.android.material.transition.MaterialSharedAxis
import me.timschneeberger.onyxtweaks.OnyxTweakApp
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.showAlert
import me.timschneeberger.onyxtweaks.ui.utils.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.utils.PublicDataStore
import me.timschneeberger.onyxtweaks.ui.utils.setBackgroundFromAttribute
import me.timschneeberger.onyxtweaks.utils.renderToLog
import kotlin.reflect.full.findAnnotations

abstract class SettingsBaseFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    protected val app
        get() = activity?.application as? OnyxTweakApp?

    private val group by lazy {
        this::class.findAnnotations(PreferenceGroup::class).firstOrNull()?.group
            ?: throw IllegalStateException("No IBaseSettingsComponent companion object found on ${this::class.simpleName}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return super.onCreateView(inflater, container, savedInstanceState).apply {
            setBackgroundFromAttribute(android.R.attr.windowBackground)
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("WorldReadableFiles")
    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Log.i("IsStorageDefault: " + preferenceManager.isStorageDefault)
        // Log.i("IsStorageDeviceProtected: " + preferenceManager.isStorageDeviceProtected)

        preferenceManager.preferenceDataStore = PublicDataStore(requireContext(), group)
        setPreferencesFromResource(group.xmlRes, rootKey)

        Log.e("--------> " + preferenceManager.sharedPreferencesName)
        preferenceManager.sharedPreferences.renderToLog("${this::class.simpleName}")
        Log.i("IsStorageDefault: " + preferenceManager.isStorageDefault)
        Log.i("IsStorageDeviceProtected: " + preferenceManager.isStorageDeviceProtected)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // checkXSharedPreferences(group.prefName)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("WorldReadableFiles")
    private fun checkXSharedPreferences(name: String) {
        try {
            // getSharedPreferences will hooked by LSPosed
            // will not throw SecurityException
            //noinspection deprecation
            requireContext()
                .getSharedPreferences(name, Context.MODE_WORLD_READABLE);
        } catch (exception: SecurityException) {
            Log.e(exception)
            requireContext().showAlert(R.string.xsp_init_failed, R.string.xsp_init_failed_summary) {
                requireActivity().finish()
            }
        }
    }

}