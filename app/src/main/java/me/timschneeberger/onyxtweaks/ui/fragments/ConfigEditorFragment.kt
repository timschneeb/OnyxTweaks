package me.timschneeberger.onyxtweaks.ui.fragments

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.github.kyuubiran.ezxhelper.Log
import me.timschneeberger.onyxtweaks.IMMKVAccessService
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.ui.activities.ConfigEditorActivity
import me.timschneeberger.onyxtweaks.ui.activities.TextEditorActivity
import me.timschneeberger.onyxtweaks.ui.preferences.DeletablePreference
import me.timschneeberger.onyxtweaks.ui.preferences.PreferenceGroup
import me.timschneeberger.onyxtweaks.ui.services.MMKVAccessService.Companion.SYSTEM_HANDLE
import me.timschneeberger.onyxtweaks.ui.utils.ContextExtensions.restartZygote
import me.timschneeberger.onyxtweaks.ui.utils.MMKVUtils
import me.timschneeberger.onyxtweaks.ui.utils.MMKVUtils.resolveType
import me.timschneeberger.onyxtweaks.ui.utils.MMKVUtils.resolveValue
import me.timschneeberger.onyxtweaks.ui.utils.showAlert
import me.timschneeberger.onyxtweaks.ui.utils.showInputAlert
import me.timschneeberger.onyxtweaks.ui.utils.showSingleChoiceAlert
import me.timschneeberger.onyxtweaks.ui.utils.showYesNoAlert
import me.timschneeberger.onyxtweaks.utils.PreferenceGroups
import me.timschneeberger.onyxtweaks.utils.Preferences


@PreferenceGroup(PreferenceGroups.NONE)
class ConfigEditorFragment : SettingsBaseFragment<ConfigEditorActivity>() {
    private var handle: String? = null
    private var pkg: String? = null
    private var kvRootPreference: PreferenceCategory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        handle = requireArguments().getString(ARG_HANDLE)
        pkg = requireArguments().getString(ARG_PKG)
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        parentActivity?.mmkvService?.close(handle)
        super.onDestroy()
    }

    override fun onConfigurePreferences() {
        PreferenceCategory(requireContext()).apply {
            title = getString(R.string.mmkv_editor_special_actions)
            isIconSpaceReserved = false
        }.let { root ->
            preferenceScreen.addPreference(root)

            if (handle == SYSTEM_HANDLE) {
                root.addPreference(
                    Preference(requireContext()).apply {
                        setIcon(R.drawable.ic_twotone_info_24dp)
                        summary = getString(R.string.mmkv_editor_system_reboot_hint)
                        isSelectable = false
                    }
                )

                root.addPreference(
                    Preference(requireContext()).apply {
                        title = getString(R.string.mmkv_editor_system_reboot)
                        summary = getString(R.string.mmkv_editor_system_reboot_summary)
                        isIconSpaceReserved = false
                        onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            parentActivity?.restartZygote()
                            true
                        }
                    }
                )

                root.addPreference(
                    Preference(requireContext()).apply {
                        title = getString(R.string.mmkv_editor_remove_eac_keys)
                        summary = getString(R.string.mmkv_editor_remove_eac_keys_summary)
                        isIconSpaceReserved = false
                        onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            requireContext().showYesNoAlert(
                                R.string.mmkv_editor_remove_eac_keys,
                                R.string.mmkv_editor_remove_eac_keys_confirm,
                                R.string.continue_action,
                                R.string.cancel
                            ) { confirmed ->
                                val service = parentActivity?.mmkvService
                                if(confirmed && service != null) {
                                    service.allKeys(handle)
                                        .filter { it.startsWith("eac_app_") && it.contains(".") }
                                        .toMutableList()
                                        .also { Log.d("Keys to be removed: ${it.joinToString()}") }
                                        .forEach { key -> service.remove(handle, key) }
                                    service.sync(handle)
                                    refreshList()
                                }
                            }
                            true
                        }
                    }
                )
            }

            root.addPreference(
                Preference(requireContext()).apply {
                    title = getString(R.string.mmkv_editor_add)
                    summary = getString(R.string.mmkv_editor_add_summary)
                    isIconSpaceReserved = false
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        requireContext().showInputAlert(
                            layoutInflater,
                            getString(R.string.mmkv_editor_add_dialog),
                            getString(R.string.mmkv_editor_add_dialog_key_hint),
                            "",
                            false,
                            null
                        ) { key ->
                            if(key == null)
                                return@showInputAlert

                            onAddOrEdit(key, edit = false) { _ ->
                                refreshList()
                            }
                        }
                        true
                    }
                })
        }

        PreferenceCategory(requireContext()).apply {
            title = getString(R.string.mmkv_editor_kv_pairs)
            isIconSpaceReserved = false
        }.let { root ->
            preferenceScreen.addPreference(root)
            kvRootPreference = root

            if (handle != SYSTEM_HANDLE) {
                var info = getString(R.string.mmkv_editor_process_killed_hint, pkg)
                if (pkg == LAUNCHER_PACKAGE) {
                    info += getString(R.string.mmkv_editor_launcher_killed_hint)
                }

                root.addPreference(
                    Preference(requireContext()).apply {
                        setIcon(R.drawable.ic_twotone_info_24dp)
                        summary = info
                        isSelectable = false
                    }
                )
            }

            val service = parentActivity?.mmkvService
            if(handle == null || service == null)
                return

            service.allKeys(handle)
                .apply { sort() }
                .forEach { key ->
                    root.addPreference(
                        DeletablePreference(requireContext()).apply {
                            setKey(key)
                            title = key
                            summary = service.resolveValue(handle, key, true)?.toString()
                            isIconSpaceReserved = false
                            onPreferenceClickListener = Preference.OnPreferenceClickListener { pref ->
                                onAddOrEdit(key, edit = true) {
                                    pref.summary = it?.toString()
                                }
                                true
                            }
                            onDeleteClicked = { pref ->
                                requireContext().showYesNoAlert(
                                    getString(R.string.mmkv_editor_delete, key),
                                    getString(R.string.mmkv_editor_delete_key_message, key),
                                    getString(R.string.continue_action),
                                    getString(R.string.cancel)
                                ) { confirmed ->
                                    if(confirmed) {
                                        service.remove(handle, key)
                                        service.sync(handle)
                                        root.removePreference(pref)
                                    }
                                }
                            }
                        }
                    )
                }
        }
    }

    fun refreshList() {
        preferenceScreen.removeAll()
        onConfigurePreferences()
    }

    fun onAddOrEdit(key: String, edit: Boolean, onEdited: ((newValue: Any?) -> Unit)) {
        val service = parentActivity?.mmkvService
        if(handle == null || service == null)
            return

        val hasKnownType = MMKVUtils.isKnownType(key)
        val resolvedType = service.resolveType(handle, key)
        val currentValue by lazy {
            if (edit && resolvedType != null)
                service.resolveValue(handle, key, false, resolvedType)
            else
                null
        }

        val alwaysAutoDetect = Preferences(requireContext(), PreferenceGroups.MISC)
            .get<Boolean>(R.string.key_misc_mmkv_always_auto_detect_unknown_types)
        if((hasKnownType || alwaysAutoDetect) && resolvedType != null) {
            editItemAs(key, resolvedType, currentValue, onEdited)
        }
        else {
            promptForType(resolvedType, edit) { chosenType ->
                editItemAs(key, chosenType, currentValue, onEdited)
            }
        }
    }

    fun editItemAs(key: String, type: MMKVUtils.KnownTypes, currentValue: Any?, onEdited: ((newValue: Any?) -> Unit)) {
        val service = parentActivity?.mmkvService
        if(handle == null || service == null)
            return

        Log.i("Editing key '$key' as type ${type.name::class.simpleName}. Current value: $currentValue (isNull: ${currentValue == null})")

        // Check if full-screen editor is required
        if(type.editorMode != null) {
            var initialValue = currentValue?.toString() ?: requireContext().getString(R.string.mmkv_editor_new_value)
            if(currentValue is List<*>) {
                initialValue = currentValue.joinToString("\n")
            }

            parentActivity?.textEditorLauncher?.launch(
                TextEditorActivity.createIntent(
                    requireContext(),
                    type.editorMode,
                    handle!!,
                    key,
                    initialValue,
                )
            )
            return
        }

        // Otherwise use simple input dialog
        service.promptSimpleInput(key, type, currentValue, onEdited)
    }

    private fun IMMKVAccessService.promptSimpleInput(key: String, type: MMKVUtils.KnownTypes, currentValue: Any?, onEdited: ((newValue: Any?) -> Unit)) {
        requireContext().showInputAlert(
            layoutInflater,
            getString(R.string.mmkv_editor_edit_simple_title, key, type.description),
            getString(R.string.mmkv_editor_new_value),
            currentValue?.toString() ?: "",
            type.typeClass == Int::class || type.typeClass == Long::class || type.typeClass == Float::class,
            null
        ) { newValue ->
            if(newValue == null)
                return@showInputAlert

            try {
                when (type.typeClass) {
                    Int::class -> this.putInt(handle, key, newValue.toInt())
                    Long::class -> this.putLong(handle, key, newValue.toLong())
                    Float::class -> this.putFloat(handle, key, newValue.toFloat())
                    Boolean::class -> this.putBoolean(handle, key, newValue.toBoolean())
                }
                this.sync(handle)
                onEdited.invoke(newValue)
            }
            catch (e: NumberFormatException) {
                Log.e(e)
                requireContext().showAlert(
                    getString(R.string.mmkv_editor_edit_failed),
                    getString(
                        R.string.mmkv_editor_edit_failed_invalid_format,
                        type.description,
                        newValue
                    )
                )
            }
            catch (e: Exception) {
                Log.e(e)
                requireContext().showAlert(
                    getString(R.string.mmkv_editor_edit_failed),
                    getString(R.string.mmkv_editor_edit_failed_exception_message, e)
                )
            }
        }
    }

    fun promptForType(guessedType: MMKVUtils.KnownTypes?, edit: Boolean, onTypeSelected: ((MMKVUtils.KnownTypes) -> Unit)) {
        val availableTypes = MMKVUtils.KnownTypes.entries.map {it.description}.let {
            if (guessedType == null)
                it
            else
                it + arrayOf(
                    getString(
                        R.string.mmkv_editor_type_auto_detect,
                        guessedType.description
                    ))
        }
            .map { it as CharSequence }
            .toTypedArray()

        requireContext().showSingleChoiceAlert(
            title = if(edit) requireContext().getString(R.string.mmkv_editor_type_dialog_title)
            else requireContext().getString(R.string.mmkv_editor_new_type_dialog_title),
            choices = availableTypes,
            checkedIndex = -1
        ) { idx ->
            if(idx != null && idx >= 0 && idx < availableTypes.size) {
                // We add an additional item
                val chosenType =
                    if(idx < MMKVUtils.KnownTypes.entries.size)
                        MMKVUtils.KnownTypes.entries.elementAt(idx)
                    else
                        guessedType!!

                onTypeSelected.invoke(chosenType)
            }
        }
    }

    fun refreshByKey(key: String, newValuePreview: String) {
        kvRootPreference?.findPreference<Preference>(key)?.summary = newValuePreview
    }

    companion object {
        private const val ARG_HANDLE = "handle"
        private const val ARG_PKG = "pkg"

        fun newInstance(handle: String, pkg: String) =
            ConfigEditorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_HANDLE, handle)
                    putString(ARG_PKG, pkg)
                }
            }
    }
}