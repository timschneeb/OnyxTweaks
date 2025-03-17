package me.timschneeberger.onyxtweaks.ui.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputType
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.databinding.DialogTextinputBinding
import me.timschneeberger.onyxtweaks.mods.Constants.LAUNCHER_PACKAGE
import me.timschneeberger.onyxtweaks.mods.Constants.SYSTEM_UI_PACKAGE
import me.timschneeberger.onyxtweaks.ui.utils.CompatExtensions.getApplicationInfoCompat

object ContextExtensions {
    fun Context.restartLauncher() {
        toast(R.string.toast_launcher_restarting)
        restartPackageSilently(LAUNCHER_PACKAGE)
    }

    fun Context.restartSystemUi() {
        toast(R.string.toast_system_ui_restarting)
        restartPackageSilently(SYSTEM_UI_PACKAGE)
    }

    fun Context.restartZygote() {
        toast(R.string.toast_zygote_restarting)
        Shell.cmd("kill $(pidof zygote)").submit()
        Shell.cmd("kill $(pidof zygote64)").submit()
    }

    private fun restartPackageSilently(pkgName: String) {
        Shell.cmd(String.format("killall %s", pkgName)).exec()
    }

    fun Context.sendLocalBroadcast(intent: Intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    fun Context.registerLocalReceiver(broadcastReceiver: BroadcastReceiver, intentFilter: IntentFilter) {
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
    }

    fun Context.unregisterLocalReceiver(broadcastReceiver: BroadcastReceiver) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    fun Context.showAlert(@StringRes title: Int, @StringRes message: Int, callback: (() -> Unit)? = null) {
        showAlert(getString(title), getString(message), callback)
    }

    fun Context.showAlert(title: CharSequence, message: CharSequence, callback: (() -> Unit)? = null) {
        MaterialAlertDialogBuilder(this)
            .setBackground(AppCompatResources.getDrawable(this, R.drawable.shape_dialog_background))
            .setMessage(message)
            .setTitle(title)
            .setPositiveButton(android.R.string.ok) { _, _ -> callback?.invoke() }
            .create()
            .show()
    }

    fun Context.showYesNoAlert(
        title: String,
        message: String,
        positiveButton: String,
        negativeButton: String,
        callback: ((Boolean) -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(this)
            .setBackground(AppCompatResources.getDrawable(this, R.drawable.shape_dialog_background))
            .setMessage(message)
            .setTitle(title)
            .setNegativeButton(negativeButton) { _, _ ->
                callback?.invoke(false)
            }
            .setPositiveButton(positiveButton) { _, _ ->
                callback?.invoke(true)
            }
            .create()
            .show()
    }

    fun Context.showYesNoAlert(
        @StringRes title: Int,
        @StringRes message: Int,
        @StringRes positiveButton: Int = R.string.yes,
        @StringRes negativeButton: Int = R.string.no,
        callback: ((Boolean) -> Unit)? = null
    ) {
        showYesNoAlert(getString(title), getString(message), getString(positiveButton), getString(negativeButton), callback)
    }

    fun Context.showSingleChoiceAlert(
        @StringRes title: Int,
        choices: Array<CharSequence>,
        checkedIndex: Int,
        callback: ((Int?) -> Unit)
    ) {
        MaterialAlertDialogBuilder(this)
            .setBackground(AppCompatResources.getDrawable(this, R.drawable.shape_dialog_background))
            .setTitle(getString(title))
            .setSingleChoiceItems(choices, checkedIndex) { dialog, i ->
                dialog.dismiss()
                callback.invoke(i)
            }
            .setNegativeButton(getString(android.R.string.cancel)) { _, _ ->
                callback.invoke(null)
            }
            .create()
            .show()
    }

    fun Context.showInputAlert(
        layoutInflater: LayoutInflater,
        @StringRes title: Int,
        @StringRes hint: Int,
        value: String,
        isNumberInput: Boolean,
        suffix: String?,
        callback: ((String?) -> Unit)
    ) {
        showInputAlert(layoutInflater, getString(title), getString(hint), value, isNumberInput, suffix, callback)
    }

    fun Context.showInputAlert(
        layoutInflater: LayoutInflater,
        title: String?,
        hint: String?,
        value: String,
        isNumberInput: Boolean,
        suffix: String?,
        callback: ((String?) -> Unit)
    ) {
        val content = DialogTextinputBinding.inflate(layoutInflater)
        content.textInputLayout.hint = hint
        content.text1.text = Editable.Factory.getInstance().newEditable(value)
        if(isNumberInput)
            content.text1.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        content.textInputLayout.suffixText = suffix

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(content.root)
            .setPositiveButton(android.R.string.ok) { inputDialog, _ ->
                val input = (inputDialog as AlertDialog).findViewById<TextView>(android.R.id.text1)
                callback.invoke(input?.text?.toString())
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                callback.invoke(null)
            }
            .create()
            .show()
    }

    fun Context.showChoiceAlert(
        entries: Array<CharSequence>,
        @StringRes titleRes: Int,
        @StringRes positiveRes: Int,
        @StringRes negativeRes: Int = android.R.string.cancel,
        onConfirm: (index: Int) -> Unit
    ) {
        var selected = -1
        MaterialAlertDialogBuilder(this)
            .setBackground(AppCompatResources.getDrawable(this, R.drawable.shape_dialog_background))
            .setSingleChoiceItems(
                entries,
                -1
            ) { _, which: Int ->
                selected = which
            }
            .setTitle(getString(titleRes))
            .setNegativeButton(getString(negativeRes)){ _, _ -> }
            .setPositiveButton(getString(positiveRes)){ _, _ ->
                selected.let {
                    if(it >= 0)
                        onConfirm(it)
                }
            }
            .create()
            .show()
    }

    fun <T> Context.showMultipleChoiceAlert(
        entries: Array<CharSequence>,
        entryValues: Array<T>,
        @StringRes titleRes: Int,
        @StringRes positiveRes: Int,
        @StringRes negativeRes: Int = android.R.string.cancel,
        onConfirm: (selected: List<T>) -> Unit
    ) {
        val selected = arrayListOf<T>()
        MaterialAlertDialogBuilder(this)
            .setBackground(AppCompatResources.getDrawable(this, R.drawable.shape_dialog_background))
            .setMultiChoiceItems(
                entries,
                null
            ) { _: DialogInterface, which: Int, isChecked: Boolean ->
                entryValues.getOrNull(which)?.let {
                    if(isChecked)
                        selected.add(it)
                    else if(selected.contains(it))
                        selected.remove(it)
                }
            }
            .setTitle(getString(titleRes))
            .setNegativeButton(getString(negativeRes)){ _, _ -> }
            .setPositiveButton(getString(positiveRes)){ _, _ ->
                selected.let {
                    if(it.isNotEmpty())
                        onConfirm(it)
                }
            }
            .create()
            .show()
    }

    fun Context.toast(message: String, long: Boolean = true) = Toast.makeText(this, message,
        if(long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    fun Context.toast(@StringRes message: Int, long: Boolean = true) = toast(getString(message), long)

    fun Context.getAppName(): String = applicationInfo.loadLabel(packageManager).toString()

    fun Context.getAppName(packageName: String): CharSequence? {
        return try {
            packageManager.getApplicationInfoCompat(packageName, 0)
        } catch (_: Exception) {
            null
        }?.let {
            packageManager.getApplicationLabel(it)
        }
    }

    fun Context.getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}