package me.timschneeberger.onyxtweaks.ui.utils

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Editable
import android.text.InputType
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.databinding.DialogTextinputBinding
import me.timschneeberger.onyxtweaks.ui.utils.CompatExtensions.getApplicationInfoCompat


object ContextExtensions {
    fun Context.openPlayStoreApp(pkgName:String?){
        if(!pkgName.isNullOrEmpty()) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkgName")))
            } catch (e: ActivityNotFoundException) {
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$pkgName")
                        )
                    )
                }
                catch (e: ActivityNotFoundException) {
                    toast(getString(R.string.no_activity_found))
                }
            }
        }
    }

    /** Open another app.
     * @param packageName the full package name of the app to open
     * @return true if likely successful, false if unsuccessful
     */
    fun Context.launchApp(packageName: String?): Boolean {
        val manager = this.packageManager
        return try {
            val i = manager.getLaunchIntentForPackage(packageName!!)
                ?: return false
            i.addCategory(Intent.CATEGORY_LAUNCHER)
            this.startActivity(i)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
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

    fun Context.showAlert(@StringRes title: Int, @StringRes message: Int) {
        showAlert(getString(title), getString(message))
    }

    fun Context.showAlert(title: CharSequence, message: CharSequence) {
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setTitle(title)
            .setNegativeButton(android.R.string.ok, null)
            .create()
            .show()
    }

    fun Context.showYesNoAlert(title: String, message: String, callback: ((Boolean) -> Unit)) {
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setTitle(title)
            .setNegativeButton(getString(R.string.no)) { _, _ ->
                callback.invoke(false)
            }
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                callback.invoke(true)
            }
            .create()
            .show()
    }

    fun Context.showYesNoAlert(@StringRes title: Int, @StringRes message: Int, callback: ((Boolean) -> Unit)) {
        showYesNoAlert(getString(title), getString(message), callback)
    }

    fun Context.showSingleChoiceAlert(
        @StringRes title: Int,
        choices: Array<CharSequence>,
        checkedIndex: Int,
        callback: ((Int?) -> Unit)
    ) {
        MaterialAlertDialogBuilder(this)
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
        } catch (e: Exception) {
            null
        }?.let {
            packageManager.getApplicationLabel(it)
        }
    }

    fun Context.getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}