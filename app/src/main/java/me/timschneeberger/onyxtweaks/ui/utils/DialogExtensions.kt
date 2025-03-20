package me.timschneeberger.onyxtweaks.ui.utils

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.timschneeberger.onyxtweaks.R
import me.timschneeberger.onyxtweaks.databinding.DialogTextinputBinding

fun Context.showAlert(@StringRes title: Int, @StringRes message: Int, callback: (() -> Unit)? = null) =
    showAlert(getString(title), getString(message), callback)

fun Context.showAlert(title: CharSequence, message: CharSequence, callback: (() -> Unit)? = null) =
    MaterialAlertDialogBuilder(this)
        .setBackground(AppCompatResources.getDrawable(this, R.drawable.shape_dialog_background))
        .setMessage(message)
        .setTitle(title)
        .setPositiveButton(android.R.string.ok) { _, _ -> callback?.invoke() }
        .create()
        .show()

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
